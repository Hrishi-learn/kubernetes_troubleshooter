package com.k8s_troubleshooter.cli_service.diagnosis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.k8s_troubleshooter.cli_service.k8s.K8sClient;
import com.k8s_troubleshooter.cli_service.k8s.PodHealthChecker;
import com.k8s_troubleshooter.cli_service.k8s.dto.PodSnapshot;
import com.k8s_troubleshooter.cli_service.llm.LLMClient;
import com.k8s_troubleshooter.cli_service.llm.PromptBuilder;
import com.k8s_troubleshooter.cli_service.llm.dto.DiagnosisResult;
import com.k8s_troubleshooter.cli_service.logs.LogTrimmer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final K8sClient k8sClient;
    private final PodHealthChecker podHealthChecker;
    private final LLMClient llmClient;
    private final PromptBuilder promptBuilder;
    private final LogTrimmer logTrimmer;

    public void diagnosisNamespace(String namespace){

        List<V1Pod>podList = Collections.emptyList();
        try{
            podList = k8sClient.getPods(namespace);
        }catch (ApiException e) {
            System.out.println("Unable to fetch the pods");
        }
        List<V1Pod>unhealthyPodsList = new ArrayList<>();

        for(V1Pod pod:podList){
            if(!podHealthChecker.checkPodHealth(pod)){
                unhealthyPodsList.add(pod);
            }
        }

        List<PodSnapshot>snapshots = new ArrayList<>();

        for(V1Pod pod: unhealthyPodsList){
            List<CoreV1Event>events = Collections.emptyList();
            String podName = "";
            String phase = "";
            String logs = "";
            String waitingReason = "";
            int restartCount = 0;

            if(pod.getMetadata()!=null){
                podName = pod.getMetadata().getName();
            }
            boolean isCrashOrImageLoop = podHealthChecker.isCrashOrImageLooping(pod);

            if(pod.getStatus()!=null){
                phase = pod.getStatus().getPhase();
            }
            try{
                events = k8sClient.getEvents(namespace,podName);
            }catch (ApiException e){
                System.out.println("No events found for pod " + pod);
            }
            try{
                logs = k8sClient.getLogs(namespace,podName,isCrashOrImageLoop);
            }catch(ApiException e){
                System.out.println("No Logs found for pod " + pod);
            }

            List<String>eventMessages = new ArrayList<>();
            for(CoreV1Event event:events){
                eventMessages.add(event.getMessage());
            }
            waitingReason = getWaitingReason(pod);
            restartCount = getRestartCount(pod);

            PodSnapshot podSnapshot = new PodSnapshot(podName,namespace,phase,waitingReason,restartCount,eventMessages,logs);
            snapshots.add(podSnapshot);
        }

        for(PodSnapshot snapshot:snapshots){
            String trimmedLogs = logTrimmer.trim(snapshot.rawLogs());
            String prompt = promptBuilder.buildPrompt(snapshot,trimmedLogs);

            try{
                DiagnosisResult result = llmClient.llmCall(prompt);
                print(snapshot,result);
            }catch (JsonProcessingException e) {
                System.out.println("Failed to parse LLM response for pod: " + snapshot.podName());
            }
        }

    }
    private String getWaitingReason(V1Pod pod){
        if (pod.getStatus()==null || pod.getStatus().getContainerStatuses() == null){
            return "";
        }
        V1PodStatus status = pod.getStatus();
        if (status == null) {
            return "";
        }
        List<V1ContainerStatus> containerStatuses = status.getContainerStatuses();
        if (containerStatuses == null || containerStatuses.isEmpty()) {
            return "";
        }
        V1ContainerStateWaiting waiting =
                containerStatuses.get(0).getState() != null
                        ? containerStatuses.get(0).getState().getWaiting()
                        : null;

        return waiting != null ? waiting.getReason() : "";
    }
    private int getRestartCount(V1Pod pod){
        if (pod.getStatus()==null || pod.getStatus().getContainerStatuses() == null){
            return 0;
        }
        V1PodStatus status = pod.getStatus();
        if (status == null) {
            return 0;
        }
        List<V1ContainerStatus> containerStatuses = status.getContainerStatuses();
        if (containerStatuses == null || containerStatuses.isEmpty()) {
            return 0;
        }
        return  containerStatuses.get(0).getRestartCount();
    }

    private void print(PodSnapshot snapshot,DiagnosisResult result){
        System.out.println("PodName: " + snapshot.podName() + "\n");
        System.out.println("Failure_Category: "+ result.failureCategory() + "\n");
        System.out.println("Root Cause: "+ result.rootCause() + "\n");
        System.out.println("Suggested Fix: "+ result.suggestedFix() + "\n");
    }

}
