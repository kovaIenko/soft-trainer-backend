package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
