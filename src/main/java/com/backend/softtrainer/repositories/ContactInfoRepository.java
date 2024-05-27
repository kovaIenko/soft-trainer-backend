package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.ContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
}
