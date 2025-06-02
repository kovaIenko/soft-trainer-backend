package com.backend.softtrainer.repositories;

import com.backend.softtrainer.dtos.SumHyperParamDto;
import com.backend.softtrainer.dtos.UserHyperParamMaxValueDto;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.UserHyperParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserHyperParameterRepository extends JpaRepository<UserHyperParameter, Long> {

  Optional<UserHyperParameter> findUserHyperParameterByChatIdAndKey(@Param("chatId") final Long chatId,
                                                                    @Param("key") final String key);

  List<UserHyperParameter> findAllByChatId(@Param("chatId") final Long chatId);

  @Query("SELECT new com.backend.softtrainer.dtos.SumHyperParamDto(u.key, sum(u.value)) " +
          "FROM user_hyperparams u " +
          "WHERE u.ownerId = :ownerId " +
          "GROUP BY u.key")
  List<SumHyperParamDto> sumUpByUser(@Param("ownerId") final Long ownerId);


  @Query("SELECT distinct new com.backend.softtrainer.dtos.UserHyperParamMaxValueDto(u.key, u.value, h.maxValue) " +
          "FROM user_hyperparams u " +
          "JOIN FETCH hyperparams h on h.simulationId = u.simulationId and u.key = h.key " +
          "WHERE u.chatId = :chatId ")
  List<UserHyperParamMaxValueDto> findHyperParamsWithMaxValues(@Param("chatId") final Long chatId);

  @Query("SELECT u FROM user_hyperparams u WHERE u.ownerId = :ownerId AND u.simulationId = :simulationId")
  List<UserHyperParameter> findAllByOwnerIdAndSimulationId(@Param("ownerId") Long ownerId, @Param("simulationId") Long simulationId);

  @Query("SELECT u FROM user_hyperparams u WHERE u.ownerId = :ownerId")
  List<UserHyperParameter> findAllByOwnerId(@Param("ownerId") Long ownerId);

  @Query("SELECT u.email FROM users u WHERE u.id = :userId")
  String findUserEmailById(@Param("userId") Long userId);

  @Query("""
    SELECT DISTINCT u
    FROM user_hyperparams uhp
    JOIN FETCH users u
    on u.id = uhp.ownerId
    WHERE uhp.updatedAt >= :since
  """)
  List<User> findUsersWithRecentHyperparamUpdates(@Param("since") LocalDateTime since);

}
