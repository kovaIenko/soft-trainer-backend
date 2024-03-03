package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.messages.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

  Optional<Message> getFirstByChatIdOrderByTimestampDesc(final String chatId);

}
