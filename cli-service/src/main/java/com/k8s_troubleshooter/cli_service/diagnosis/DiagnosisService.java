package com.k8s_troubleshooter.cli_service.diagnosis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.k8s_troubleshooter.cli_service.k8s.K8sClient;
import com.k8s_troubleshooter.cli_service.k8s.PodHealthChecker;
import com.k8s_troubleshooter.cli_service.k8s.dto.PodSnapshot;
import com.k8s_troubleshooter.cli_service.llm.LLMClient;
import com.k8s_troubleshooter.cli_service.llm.PromptBuilder;
import com.k8s_troubleshooter.cli_service.diagnosis.dto.DiagnosisResult;
import com.k8s_troubleshooter.cli_service.llm.dto.LLMResponse;
import com.k8s_troubleshooter.cli_service.logs.LogTrimmer;
import com.k8s_troubleshooter.cli_service.repository.DiagnosisRepository;
import com.k8s_troubleshooter.common.FailureCategory;
import com.k8s_troubleshooter.common.entity.Diagnosis;
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
    private final DiagnosisRepository diagnosisRepository;

    public List<DiagnosisResult> diagnosisNamespace(String namespace){

        List<V1Pod>podList = Collections.emptyList();
        try{
            podList = k8sClient.getPods(namespace);
        }catch (ApiException e) {
            System.out.println("Unable to fetch the pods");
        }
        if(podList.isEmpty()){
            System.out.println("No pods found for namespace:" + namespace);
            return Collections.emptyList();
        }

        List<V1Pod>unhealthyPodsList = new ArrayList<>();

        for(V1Pod pod:podList){
            if(!podHealthChecker.checkPodHealth(pod)){
                unhealthyPodsList.add(pod);
            }
        }
        if(unhealthyPodsList.isEmpty()){
            System.out.println("No unhealthy pods found for namespace:" + namespace);
            return Collections.emptyList();
        }

        List<PodSnapshot>snapshots = new ArrayList<>();
    /*
        Todo: Currently fetching waiting reason and restartCount for one container, can be extended to multiple container inside a pod.
    */
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
        List<DiagnosisResult>diagnosisResults = new ArrayList<>();
        for(PodSnapshot snapshot:snapshots){
            String trimmedLogs = logTrimmer.trim(snapshot.rawLogs());
            String prompt = promptBuilder.buildPrompt(snapshot,trimmedLogs);

            try{
                LLMResponse response = llmClient.llmCall(prompt);
                DiagnosisResult result = new DiagnosisResult(snapshot.podName(),response.rootCause(),response.suggestedFix(),response.failureCategory());
                diagnosisResults.add(result);
                saveToDb(snapshot,result);
            }catch (JsonProcessingException e) {
                System.out.println("Failed to parse LLM response for pod: " + snapshot.podName());
            }
        }
        return diagnosisResults;
    }
    private void saveToDb(PodSnapshot snapshot, DiagnosisResult diagnosisResult){
        FailureCategory failureCategory = (diagnosisResult.failureCategory()==null)?FailureCategory.Unknown:diagnosisResult.failureCategory();
        Diagnosis diagnosis = new Diagnosis(snapshot.podName(),snapshot.namespace(),"",failureCategory,diagnosisResult.rootCause(),diagnosisResult.suggestedFix(), snapshot.rawLogs());
        diagnosisRepository.save(diagnosis);
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


}
