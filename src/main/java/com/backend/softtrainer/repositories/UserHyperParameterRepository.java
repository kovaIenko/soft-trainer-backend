package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.UserHyperParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserHyperParameterRepository extends JpaRepository<UserHyperParameter, Long> {

  Optional<UserHyperParameter> findUserHyperParameterByChatIdAndKey(@Param("chatId") final Long chatId, @Param("key") final String key);

}
