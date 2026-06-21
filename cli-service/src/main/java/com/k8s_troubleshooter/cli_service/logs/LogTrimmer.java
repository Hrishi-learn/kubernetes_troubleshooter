package com.k8s_troubleshooter.cli_service.logs;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LogTrimmer {
    private static final List<String> ERROR_KEYWORDS = List.of(
            "ERROR",
            "FATAL",
            "EXCEPTION",
            "FAILED",
            "FAILURE",
            "PANIC",
            "CRITICAL",
            "SEVERE",
            "NullPointerException",
            "OutOfMemoryError",
            "Connection refused",
            "Timeout",
            "SQLException",
            "Unauthorized",
            "Forbidden"
    );

    public String trim(String logs){
        if(logs==null)return "";
        String[] lines = logs.split("\\R");
        int n = lines.length;

        TreeMap<Integer,String>map = new TreeMap<>();

        for(String error:ERROR_KEYWORDS){

            for(int i=0;i<n;i++){
                if(lines[i].toLowerCase().contains(error.toLowerCase())){
                    for(int j=-2;j<=2;j++){
                        if(i+j<n && i+j>=0){
                            map.put(i+j,lines[i+j]);
                        }
                    }
                }
            }
        }

        StringBuilder sb =  new StringBuilder();
        map.forEach((key,value)->{
            sb.append(value).append("\n");
        });

        return sb.toString();
    }

}
