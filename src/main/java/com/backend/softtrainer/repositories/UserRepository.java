package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(final String email);

  @Query("SELECT u from users u join fetch u.organization where u.email = :email")
  Optional<User> findByEmailWithOrg(@Param("email") final String email);

  @Query("SELECT u FROM users u WHERE u.organization = :organization")
  List<User> findAllByOrganization(@Param("organization") Organization organization);

  @Modifying
  @Transactional
  @Query("update users as u set u.name = :name where u = :user")
  void updateName(@Param("user") final User user, @Param("name") final String name);

}
