package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

  @Query("SELECT c FROM chats c JOIN FETCH c.messages WHERE c.id = :chatId")
  Optional<Chat> findByIdWithMessages(@Param("chatId") Long chatId);


  boolean existsByUserAndSimulationId(@Param("user") final User user,
                                      @Param("simulationId") final Long simulationId);

  boolean existsByIdAndUser(@Param("id") final Long id, @Param("user") final User user);


  @Query("SELECT c FROM chats c JOIN FETCH c.messages WHERE c.user = :user AND c.simulation = :simulation")
  List<Chat> findByUserAndSimulationWithMessages(@Param("user") final User user,
                                                 @Param("simulation") final Simulation simulation);

  @Query("select c from chats c join fetch c.skill s where s.id = :skillId AND c.user = :user")
  List<Chat> findAllBySkillId(@Param("user") final User user, @Param("skillId") final Long skillId);

  @Query("select c from chats c join fetch c.simulation s where s = :simulation AND c.user = :user")
  List<Chat> findAllBySimulationId(@Param("user") final User user, @Param("simulation") final Simulation simulation);


//  @Query("select c from chats c join fetch c.simulation s, c.messages m join fetch s.nodes n where s.id = skillId")
//  boolean chatIsCompletedBySimulation(@Param("chatId") final Long chatId);

  @Modifying
  @Transactional
  @Query("update chats c set c.isFinished = :isFinished where c.id = :chatId")
  void updateIsFinished(@Param("chatId") final Long chatId, @Param("isFinished") final boolean isFinished);

}
