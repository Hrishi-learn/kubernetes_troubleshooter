package com.k8s_troubleshooter.common;

/**
 * Classification of the underlying root cause, as determined by the LLM.
 *
 * Note: this is meant to capture the CAUSE, not just echo Kubernetes'
 * own status/reason fields (e.g. CrashLoopBackOff is itself a K8s-level
 * symptom we already know before calling the LLM - OOMKilled,
 * ConfigError, NetworkError, ResourceConstraint are meant to be the
 * more specific "why" behind that symptom). Worth revisiting the
 * prompt if diagnoses keep just echoing the input status instead of
 * adding real classification value.
 */
public enum FailureCategory {
    OOMKilled,
    CrashLoopBackOff,
    ImagePullBackOff,
    ConfigError,
    NetworkError,
    ResourceConstraint,
    Unknown
}