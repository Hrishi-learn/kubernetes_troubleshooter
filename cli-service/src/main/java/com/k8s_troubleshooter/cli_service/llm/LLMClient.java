package com.k8s_troubleshooter.cli_service.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.k8s_troubleshooter.cli_service.config.LLMConfigProperties;
import com.k8s_troubleshooter.cli_service.llm.dto.ChatRequest;
import com.k8s_troubleshooter.cli_service.llm.dto.ChatResponse;
import com.k8s_troubleshooter.cli_service.llm.dto.LLMResponse;
import com.k8s_troubleshooter.cli_service.llm.dto.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LLMClient{

    private final RestClient restClient;
    private final static String role = "user";
    private final LLMConfigProperties props;

    public LLMResponse llmCall(String prompt) throws JsonProcessingException {
        ChatRequest request = new ChatRequest(List.of(new Message(role,prompt)),props.model());

        ChatResponse response = restClient.post().uri(props.baseUrl()).body(request).header("content-type", "application/json")
                .header("authorization","bearer "+props.key())
                .retrieve()
                .body(ChatResponse.class);

        String rawContent = response.choices().get(0).message().content();

        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        LLMResponse result = objectMapper.readValue(rawContent,LLMResponse.class);

        return result;
    }
}
