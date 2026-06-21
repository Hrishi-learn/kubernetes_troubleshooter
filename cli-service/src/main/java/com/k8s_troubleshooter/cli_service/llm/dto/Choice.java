package com.k8s_troubleshooter.cli_service.llm.dto;

public record Choice(int index,Message message,String finish_reason) {
}
