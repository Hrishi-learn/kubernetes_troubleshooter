package com.k8s_troubleshooter.common;

public enum FailureCategory {
    OOMKilled,
    CrashLoopBackOff,
    ImagePullBackOff,
    ConfigError,
    NetworkError,
    ResourceConstraint,
    Unknown
}