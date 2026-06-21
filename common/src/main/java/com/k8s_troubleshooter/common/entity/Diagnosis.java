package com.k8s_troubleshooter.common.entity;

import com.k8s_troubleshooter.common.FailureCategory;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "diagnosis")
@Getter
public class Diagnosis {

    protected Diagnosis() {
    }

    public Diagnosis(String podName, String namespace, String failureSignatureHash,
                     FailureCategory category, String rootCause,
                     String suggestedFix, String rawLogsExcerpt) {
        this.podName = podName;
        this.namespace = namespace;
        this.failureSignatureHash = failureSignatureHash;
        this.category = category;
        this.rootCause = rootCause;
        this.suggestedFix = suggestedFix;
        this.rawLogsExcerpt = rawLogsExcerpt;
        this.createdAt = Instant.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pod_name", nullable = false)
    private String podName;

    @Column(name = "namespace", nullable = false)
    private String namespace;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private FailureCategory category;

    @Column(name = "root_cause", nullable = false, columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "suggested_fix", nullable = false, columnDefinition = "TEXT")
    private String suggestedFix;

    @Column(name = "raw_logs_excerpt", columnDefinition = "TEXT")
    private String rawLogsExcerpt;

    @Column(name = "failure_signature_hash", nullable = false)
    private String failureSignatureHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}