package com.backend.softtrainer.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SkillControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser
  public void testGetAllSkillNamesSuccess() throws Exception {
    mockMvc.perform(get("/skills/names")
                      .param("org", "SoftTrainer")
                      .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @WithMockUser
  public void testGetAllSimulationsSuccess() throws Exception {
    mockMvc.perform(get("/skills/simulation/names")
                      .param("skillId", "1")
                      .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true));
  }

}
