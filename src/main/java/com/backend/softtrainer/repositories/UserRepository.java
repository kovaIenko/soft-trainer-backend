package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(final String email);

  @Query("SELECT u from users u join fetch u.organization where u.email = :email")
  Optional<User> findByEmailWithOrg(@Param("email") final String email);

}
