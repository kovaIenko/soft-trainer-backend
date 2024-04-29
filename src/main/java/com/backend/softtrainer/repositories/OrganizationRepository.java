package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  @Query("SELECT o FROM organizations o JOIN FETCH o.availableSkills WHERE o.name = :name")
  Optional<Organization> getFirstByName(@Param("name") final String name);

}
