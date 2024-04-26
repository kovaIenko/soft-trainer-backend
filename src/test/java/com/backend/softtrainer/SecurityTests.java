package com.backend.softtrainer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityTests {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser(username = "user")
  public void testSecuredEndpoint() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/health").with(csrf()))
      .andExpect(MockMvcResultMatchers.status().isOk());
  }
}

