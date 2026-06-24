package com.k8s_troubleshooter.cli_service.llm.dto;

import com.k8s_troubleshooter.common.FailureCategory;

public record LLMResponse(FailureCategory failureCategory,String rootCause,String suggestedFix) {
}
