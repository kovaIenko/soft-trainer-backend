package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.messages.Message;
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

  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.messageType in :actionableMessageTypes AND m.chatId = :chatId ORDER BY m.timestamp DESC")
  List<Message> getActionableMessage(final List<String> actionableMessageTypes, @Param("chatId") final Long chatId);

  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.role = 'USER' and f.orderNumber = :orderNumber and m.chatId = :chatId")
  Optional<Message> findAllUserMessagesByOrderNumber(@Param("chatId") final Long chatId, @Param("orderNumber") final long orderNumber);

  @Query("SELECT m FROM messages m JOIN FETCH m.flowNode f WHERE m.role = 'USER' and f.orderNumber = :orderNumber and m.chatId = :chatId")
  Optional<Message> findAllUserMessagesByOrderNumber(@Param("chatId") final String chatId, @Param("orderNumber") final long orderNumber);

}
