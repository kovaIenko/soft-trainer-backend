package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.messages.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

  Optional<Message> getFirstByChatIdOrderByTimestampDesc(final String chatId);

  @Query("SELECT m FROM messages m JOIN FETCH m.flowQuestion f WHERE m.role = 'USER' and f.orderNumber = :orderNumber and m.chatId = :chatId")
  Optional<Message> findAllUserMessagesByOrderNumber(@Param("chatId") final String chatId, @Param("orderNumber") final long orderNumber);

}
