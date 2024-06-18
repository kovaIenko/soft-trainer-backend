package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.HyperParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface HyperParameterRepository extends JpaRepository<HyperParameter, Long> {

  @Query("SELECT hp.key FROM hyperparams hp WHERE hp.simulationId = :simulationId")
  Set<String> getAllKeysBySimulationId(@Param("simulationId") final Long simulationId);

}
