package com.backend.softtrainer;

import com.backend.softtrainer.configs.RsaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(RsaProperties.class)
@EnableScheduling
public class SoftTrainerBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(SoftTrainerBackendApplication.class, args);
  }

}
