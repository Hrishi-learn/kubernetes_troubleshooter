package com.k8s_troubleshooter.cli_service.k8s;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Service
public class K8sClient {

    private ApiClient apiClient;
    private CoreV1Api api;

    @PostConstruct
    public void init(){
        try{
            apiClient = ClientBuilder.standard().build();
            api = new CoreV1Api(apiClient);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public List<V1Pod> getPods(String namespace) throws ApiException{

        return api.listNamespacedPod(namespace).execute().getItems();
    }
    public List<CoreV1Event> getEvents(String namespace,String podname) throws ApiException {

        List<CoreV1Event>events = api.listNamespacedEvent(namespace).execute().getItems();

        return events.stream().filter(event-> event.getInvolvedObject()!=null && podname.equals(event.getInvolvedObject().getName()))
                .toList();
    }
    public String getLogs(String namespace,String pod, boolean previous) throws ApiException {

        return api.readNamespacedPodLog(pod,namespace).previous(previous).execute();
    }

}
