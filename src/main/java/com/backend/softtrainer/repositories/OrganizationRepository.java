package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  @Query("SELECT o FROM organizations o JOIN FETCH o.availableSkills WHERE o.name = :name")
  Optional<Organization> getFirstByName(@Param("name") final String name);

  Optional<Organization> findByName(String name);

  @Modifying
  @Query(value = "INSERT INTO organizations_skills (organization_id, skill_id) VALUES (:organizationId, :skillId) ON CONFLICT DO NOTHING", nativeQuery = true)
  void addSkillToOrganization(@Param("organizationId") Long organizationId, @Param("skillId") Long skillId);

}
