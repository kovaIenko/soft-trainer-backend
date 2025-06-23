package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.NewSkillPayload;
import com.backend.softtrainer.dtos.UpdateSkillVisibilityDto;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.enums.BehaviorType;
import com.backend.softtrainer.entities.enums.SkillType;
import com.backend.softtrainer.services.SkillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
public class AdminSkillControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SkillService skillService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateSkill() throws Exception {
        NewSkillPayload payload = new NewSkillPayload();
        payload.setName("Test Skill");
        payload.setDescription("Test Description");
        payload.setType(SkillType.DEVELOPMENT);
        payload.setBehavior(BehaviorType.STATIC);

        Skill skill = new Skill();
        skill.setId(1L);
        skill.setName("Test Skill");

        given(skillService.createSkill(any(NewSkillPayload.class))).willReturn(skill);

        mockMvc.perform(post("/api/admin/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("Test Skill"));
    }

    @Test
    public void testGetSkills() throws Exception {
        given(skillService.getAllSkill()).willReturn(Collections.emptySet());

        mockMvc.perform(get("/api/admin/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testGetSkillById() throws Exception {
        Skill skill = new Skill();
        skill.setId(1L);
        skill.setName("Test Skill");

        given(skillService.getSkillById(1L)).willReturn(skill);

        mockMvc.perform(get("/api/admin/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("Test Skill"));
    }

    @Test
    public void testUpdateSkillVisibility() throws Exception {
        UpdateSkillVisibilityDto payload = new UpdateSkillVisibilityDto();
        payload.setHidden(true);

        mockMvc.perform(patch("/api/admin/skills/1/visibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testDeleteSkill() throws Exception {
        mockMvc.perform(delete("/api/admin/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
} 