package com.backend.softtrainer.entities;

import com.backend.softtrainer.entities.messages.Message;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "chats")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"messages", "user", "simulation", "skill"})
public class Chat {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

//  @OrderBy("flowNode.orderNumber ASC")
  @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private List<Message> messages;

  @ManyToOne
  private User user;

  @ManyToOne(fetch = FetchType.EAGER)
  private Simulation simulation;

  @Column(name = "timestamp", insertable = false, updatable = false)
  @CreationTimestamp(source = SourceType.DB)
  private LocalDateTime timestamp;

  @ManyToOne(fetch = FetchType.EAGER)
  private Skill skill;

  private boolean isFinished = false;

  private Double hearts;

}
