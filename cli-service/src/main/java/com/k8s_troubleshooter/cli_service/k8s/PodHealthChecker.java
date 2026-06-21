package com.k8s_troubleshooter.cli_service.k8s;

import io.kubernetes.client.openapi.models.V1ContainerState;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class PodHealthChecker {

    private static final int RESTART_COUNT = 5;

    public boolean checkPodHealth(V1Pod pod){
        V1PodStatus status = pod.getStatus();
        if(status==null)return false;

        String phase = status.getPhase();
        if(isCrashOrImageLooping(pod))return false;

        if("Pending".equals(phase)){
            if(isStuckPending(pod)){
                return false;
            }
            return true;
        }
        if("Running".equals(phase)){
            boolean ready = status.getConditions() != null && status.getConditions()
                    .stream()
                    .anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));
            if(ready)return true;
        }
        if("Succeeded".equals(phase)){
            return true;
        }
        return false;
    }

    private boolean isStuckPending(V1Pod pod){
        boolean unscheduled = pod.getStatus().getConditions()!=null && pod.getStatus()
                .getConditions().stream().anyMatch(c -> "PodScheduled".equals(c.getType())
                && "False".equals(c.getStatus()) && "Unschedulable".equals(c.getReason()));

        if(unscheduled)return true;

        if(pod.getMetadata()==null)return  false;

        OffsetDateTime createdAt = pod.getMetadata().getCreationTimestamp();

        if(createdAt!=null){
            Duration age = Duration.between(createdAt,OffsetDateTime.now());
            if(age.toMinutes()>5)
                return true;
        }
        return false;
    }

    private boolean isCrashOrImageLooping(V1Pod pod){
        if (pod.getStatus().getContainerStatuses() == null) return false;
        return pod.getStatus().getContainerStatuses().stream().anyMatch(cs -> {
            V1ContainerState state = cs.getState();
            boolean waitingCrashLoop = state.getWaiting() != null
                    && ("CrashLoopBackOff".equals(state.getWaiting().getReason()) || "ImagePullBackOff".equals(state.getWaiting().getReason()));
            boolean highRestarts = cs.getRestartCount() != null && cs.getRestartCount() > RESTART_COUNT;
            return waitingCrashLoop || highRestarts;
        });
    }

}
