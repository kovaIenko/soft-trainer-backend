package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<Auth, Long> {
}
