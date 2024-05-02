package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.HyperParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface HyperParameterRepository extends JpaRepository<HyperParameter, String> {

  @Query("SELECT hp.key FROM hyperparams hp WHERE hp.simulationName = :flowName")
  Set<String> getAllKeysByFlowName(@Param("flowName") final String flowName);

}
