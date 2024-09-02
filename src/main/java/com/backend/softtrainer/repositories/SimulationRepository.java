package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {

  @Query("SELECT s from simulations s join fetch s.nodes where s.isOpen = true")
  List<Simulation> findAllSimulationsWithFlowNodes();

}
