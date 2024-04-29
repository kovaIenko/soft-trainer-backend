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

  boolean existsByOwnerIdAndSimulationNameAndSkillId(@Param("ownerId") final Long ownerId,
                                                     @Param("simulationName") final String simulationName,
                                                     @Param("skillId") final Long skillId);

  @Query("SELECT c FROM Chat c JOIN FETCH c.messages WHERE c.ownerId = :ownerId AND c.simulationName = :simulationName")
  Optional<Chat> findByOwnerIdAndFlowNameWithMessages(@Param("ownerId") final Long ownerId,
                                                      @Param("simulationName") final String simulationName);

  List<Chat> findAllByOwnerId(@Param("ownerId") final Long ownerId);

}
