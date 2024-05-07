package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.messages.Message;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

  @Deprecated
  Optional<Message> getFirstByChatIdOrderByTimestampDesc(final Long chatId);

  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.messageType in :actionableMessageTypes AND m.chat.id = :chatId ORDER BY m.timestamp DESC")
  List<Message> getActionableMessage(final List<String> actionableMessageTypes, @Param("chatId") final Long chatId);

  @Deprecated
  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.role = 'USER' and f.orderNumber = :orderNumber and m.chat.id = :chatId")
  List<Message> findAllUserMessagesByOrderNumber(@Param("chatId") final Long chatId, @Param("orderNumber") final long orderNumber);

  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.role = 'USER' and f.orderNumber = :orderNumber and m.chat.id = :chatId")
  Optional<Message> findQuestionUserMessagesByOrderNumber(@Param("chatId") final Long chatId, @Param("orderNumber") final long orderNumber);

  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.chat = :chat and f.orderNumber = :orderNumber")
  List<Message> existsByOrderNumberAndChatId(@Param("chat") final Chat chat, @Param("orderNumber") long orderNumber);

}
