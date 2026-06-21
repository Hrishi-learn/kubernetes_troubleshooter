package com.k8s_troubleshooter.cli_service.llm.dto;

import java.util.List;

public record ChatRequest(List<Message>messages,String model){

}
