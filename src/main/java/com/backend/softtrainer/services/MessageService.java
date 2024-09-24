package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.messages.HintMessage;
import com.backend.softtrainer.entities.messages.LastSimulationMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.repositories.MessageRepository;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class MessageService {

  private final MessageRepository messageRepository;

  private final EntityManager entityManager;

  @Transactional(isolation = Isolation.READ_UNCOMMITTED)
  public Optional<Message> findMessageById(final String messageId) {
    //todo how to avoid that stupid request??
    entityManager.refresh(entityManager.find(Message.class, messageId));
    return entityManager.createQuery("SELECT m FROM messages m WHERE m.id = :id", Message.class)
      .setParameter("id", messageId)
      .setHint("org.hibernate.cacheable", false)
      .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
      .getResultStream()
      .findFirst();
  }

  @Transactional(isolation = Isolation.READ_UNCOMMITTED)
  public void updateOrCreateHintMessage(final String hintMessageId,
                                        final FlowNode hintNode,
                                        final Chat chat,
                                        final String content,
                                        final Map<String, String> hintCache) {

    HintMessage temp = null;

    var title = "Tip";
    try {
      log.info(
        "Updating or creation hint message for chat: {}, at {} with content {}",
        chat.getId(),
        LocalDateTime.now(),
        content
      );
      var msg = messageRepository.findById(hintMessageId);
      if (msg.isPresent()) {
        temp = (HintMessage) msg.get();
        temp.setTitle(title);
        temp.setContent(content);
        temp.setInteracted(true);
        log.info("The hint message is updated: {}, at {}, and version {}", temp, LocalDateTime.now(), temp.getVersion());
      } else {
        temp = HintMessage.builder()
          .id(hintMessageId)
          .chat(chat)
          .flowNode(hintNode)
          .messageType(MessageType.HINT_MESSAGE)
          .role(ChatRole.APP)
          .content(content)
          .title(title)
          .interacted(true)
          .build();
      }
      hintCache.put(temp.getId(), content);
      log.info("Save or update to store hint message with content: {}", temp);
      temp = entityManager.merge(temp);
      log.info("Hint message is stored: {}, at {}, version {}", temp, LocalDateTime.now(), temp.getVersion());

    } catch (Exception e) {
      log.error("Error while updating hint message: {}", temp, e);
    }
  }

  public Message save(final Message message) {
    return messageRepository.saveAndFlush(message);
  }

  @Transactional(isolation = Isolation.READ_UNCOMMITTED)
  public void updateResultSimulationMessage(final Message msg,
                                            final List<UserHyperParamResponseDto> params,
                                            final String title,
                                            final String content,
                                            final Map<String, String> resultCache) {
    try {
      var lastMsg = (LastSimulationMessage) findMessageById(msg.getId()).orElseThrow();
      lastMsg.setTitle(title);
      lastMsg.setContent(content);
      lastMsg.setHyperParams(params);
      lastMsg.setInteracted(true);

      lastMsg.setRole(ChatRole.APP);
      entityManager.persist(lastMsg);

      resultCache.put(lastMsg.getId(), content);
    } catch (Exception e) {
      log.error("Error while updating result message: {}", msg, e);
    }
  }

  @NotNull
  public Optional<Message> findQuestionUserMessageByOrderNumber(final Long chatId, final long orderNumber) {
    return messageRepository.findQuestionUserMessagesByOrderNumber(chatId, orderNumber);
  }

  @NotNull
  public List<Message> findMessagesByOrderNumber(final Long chatId, final long orderNumber) {
    return messageRepository.findMessagesByOrderNumber(chatId, orderNumber);
  }

}
