package com.backend.softtrainer.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
      .allowedOriginPatterns(
        "http://localhost:3000"
//        "https://app.thesofttrainer.com",
//        "https://thesofttrainer.com",
//        "https://app1.thesofttrainer.com",
//        "https://*.thesofttrainer.com",
//        "https://admin.thesofttrainer.com"
      )
      .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true);
  }

}

