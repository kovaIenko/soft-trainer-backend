package com.backend.softtrainer.controllers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@AllArgsConstructor
@Slf4j
public class HomeController {

  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Hello, Misha");
  }

}
