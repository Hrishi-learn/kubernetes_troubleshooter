package com.k8s_troubleshooter.cli_service.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(String id, String model, List<Choice> choices) {
}
