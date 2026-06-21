package com.k8s_troubleshooter.cli_service.llm;

import com.k8s_troubleshooter.cli_service.k8s.dto.PodSnapshot;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    private final static String BASE = "You are a Kubernetes troubleshooting assistant. " +
            "Analyze the following pod failure data and respond with ONLY a JSON object, no markdown formatting, no code fences, no explanation outside the JSON. " +
            "The JSON must match this exact shape: {\"rootCause\": string, \"category\": string, \"suggestedFix\": string}. " +
            "Valid values for category are: OOMKilled, CrashLoopBackOff, ImagePullBackOff, ConfigError, NetworkError, ResourceConstraint, Unknown. " +
            "The category should reflect the underlying root cause type, not simply repeat the Kubernetes-reported status.";

    public String buildPrompt(PodSnapshot podSnapshot, String trimmedLogs) {
        StringBuilder sb = new StringBuilder();

        sb.append(BASE).append("\n\n");

        sb.append("Pod: ").append(podSnapshot.podName()).append("\n");
        sb.append("Namespace: ").append(podSnapshot.namespace()).append("\n");
        sb.append("Phase: ").append(podSnapshot.phase()).append("\n");
        sb.append("Waiting Reason: ")
                .append(podSnapshot.waitingReason() != null ? podSnapshot.waitingReason() : "None")
                .append("\n");
        sb.append("Restart Count: ").append(podSnapshot.restartCount()).append("\n\n");

        sb.append("Recent Events:\n");
        if (podSnapshot.eventMessages() != null && !podSnapshot.eventMessages().isEmpty()) {
            podSnapshot.eventMessages()
                    .forEach(event -> sb.append(event).append("\n"));
        } else {
            sb.append("No recent events\n");
        }

        sb.append("\n");
        sb.append("Log excerpt (grepped around error keywords):\n");
        sb.append(trimmedLogs != null && !trimmedLogs.isBlank()
                ? trimmedLogs
                : "No relevant logs found");

        sb.append("\n\n");
        sb.append("What is wrong and how do I fix it?");

        return sb.toString();
    }
}
