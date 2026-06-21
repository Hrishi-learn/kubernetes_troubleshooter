package com.k8s_troubleshooter.cli_service.repository;

import com.k8s_troubleshooter.common.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

}
