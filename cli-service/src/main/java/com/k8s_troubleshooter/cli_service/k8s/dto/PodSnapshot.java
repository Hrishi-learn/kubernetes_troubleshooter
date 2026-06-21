package com.k8s_troubleshooter.cli_service.k8s.dto;

import java.util.List;

public record PodSnapshot(
        String podName,
        String namespace,
        String phase,
        String waitingReason,   // nullable - e.g. "CrashLoopBackOff", null if none
        int restartCount,
        List<String> eventMessages,
        String rawLogs
) {
}