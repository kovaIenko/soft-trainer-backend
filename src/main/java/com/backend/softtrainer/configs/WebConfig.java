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
      .allowedOrigins(
        "https://app.thesofttrainer.com",
        "https://test-web-flutter-427fd.web.app/",
        "https://thesofttrainer.com",
        "https://thesofttrainerdev.web.app",
        "http://localhost:3000",
        "https://master.dpkezokj5u56u.amplifyapp.com"
      )
      .allowedMethods("GET", "POST", "PUT", "DELETE")
      .allowedHeaders("*")
      .allowCredentials(true);
  }

}
