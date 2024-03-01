package com.backend.softtrainer.entities;

import com.backend.softtrainer.entities.messages.Message;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Chat {

  @Id
  private String id;

  @OneToMany(mappedBy = "chatId", fetch = FetchType.LAZY)
  private Set<Message> messages;

  private String ownerId;

  private String flowName;

}
