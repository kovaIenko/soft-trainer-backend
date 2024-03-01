package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {

  @Query("SELECT c FROM Chat c JOIN FETCH c.messages WHERE c.id = :chatId")
  Optional<Chat> findByIdWithMessages(@Param("chatId") String chatId);

  boolean existsByOwnerIdAndFlowName(final String ownerId, final String flowName);

}
