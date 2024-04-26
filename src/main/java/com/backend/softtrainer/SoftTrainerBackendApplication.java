package com.backend.softtrainer;

import com.backend.softtrainer.configs.RsaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RsaProperties.class)
public class SoftTrainerBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(SoftTrainerBackendApplication.class, args);
  }

}
