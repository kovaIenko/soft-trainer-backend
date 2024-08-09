package com.backend.softtrainer.repositories;

import com.backend.softtrainer.dtos.SumHyperParamDto;
import com.backend.softtrainer.entities.UserHyperParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
