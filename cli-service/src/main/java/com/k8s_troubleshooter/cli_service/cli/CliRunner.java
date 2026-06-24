package com.k8s_troubleshooter.cli_service.cli;

import com.k8s_troubleshooter.cli_service.diagnosis.DiagnosisService;
import com.k8s_troubleshooter.cli_service.diagnosis.dto.DiagnosisResult;
import com.k8s_troubleshooter.cli_service.k8s.dto.PodSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Component
@RequiredArgsConstructor
public class CliRunner implements CommandLineRunner {

    private final DiagnosisService diagnosisService;

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Please enter the namespace: ");

            String prompt = scanner.nextLine();

            if ("exit".equalsIgnoreCase(prompt)) {
                break;
            }
            List<DiagnosisResult>results = diagnosisService.diagnosisNamespace(prompt);

            for(DiagnosisResult result:results){
                print(result);
            }
        }
    }

    private void print(DiagnosisResult result){
        System.out.println("PodName: " + result.podName() + "\n");
        System.out.println("Failure_Category: "+ result.failureCategory() + "\n");
        System.out.println("Root Cause: "+ result.rootCause() + "\n");
        System.out.println("Suggested Fix: "+ result.suggestedFix() + "\n");
    }
}
