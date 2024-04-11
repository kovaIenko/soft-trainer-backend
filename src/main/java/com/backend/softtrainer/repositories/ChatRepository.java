package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

  @Query("SELECT c FROM Chat c JOIN FETCH c.messages WHERE c.id = :chatId")
  Optional<Chat> findByIdWithMessages(@Param("chatId") Long chatId);

  boolean existsByOwnerIdAndFlowName(final Long ownerId, final String flowName);

  @Query("SELECT c FROM Chat c JOIN FETCH c.messages WHERE c.ownerId = :ownerId AND c.flowName = :flowName")
  Optional<Chat> findByOwnerIdAndFlowNameWithMessages(@Param("ownerId") final Long ownerId, @Param("flowName") final String flowName);

  List<Chat> findAllByOwnerId(@Param("ownerId") final Long ownerId);

}
