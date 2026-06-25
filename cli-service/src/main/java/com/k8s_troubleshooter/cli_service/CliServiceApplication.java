package com.k8s_troubleshooter.cli_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@EntityScan("com.k8s_troubleshooter.common.entity")
public class CliServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CliServiceApplication.class, args);
	}

}
