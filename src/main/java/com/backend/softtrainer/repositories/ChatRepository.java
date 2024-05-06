package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

  @Query("SELECT c FROM chats c JOIN FETCH c.messages WHERE c.id = :chatId")
  Optional<Chat> findByIdWithMessages(@Param("chatId") Long chatId);


  boolean existsByUserAndSimulationId(@Param("user") final User user,
                                      @Param("simulationId") final Long simulationId);

  boolean existsByIdAndUser(@Param("id") final Long id, @Param("user") final User user);


  @Query("SELECT c FROM chats c JOIN FETCH c.messages WHERE c.user = :user AND c.simulation = :simulation")
  Optional<Chat> findByUserAndSimulationWithMessages(@Param("user") final User user,
                                                     @Param("simulation") final Simulation simulation);

}
