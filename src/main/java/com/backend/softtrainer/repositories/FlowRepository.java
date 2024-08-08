package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.flow.FlowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlowRepository extends JpaRepository<FlowNode, Long> {

  @Query("SELECT f FROM nodes f WHERE f.simulation.id = :simulationId ORDER BY f.orderNumber LIMIT 10")
  List<FlowNode> findFirst10QuestionsBySimulation(@Param("simulationId") final Long simulationId);

  @Query("SELECT f FROM nodes f WHERE f.simulation.id = :simulationId and f.previousOrderNumber = :previousOrderNumber ORDER BY" +
    " f.orderNumber LIMIT 10")
  List<FlowNode> findAllBySimulationIdAndPreviousOrderNumber(@Param("simulationId") final Long simulationId, @Param(
    "previousOrderNumber") final long previousOrderNumber);

//  List<FlowNode> findAllByOrderNumber(@Param("orderNumber") final long orderNumber);

  Optional<FlowNode> findTopBySimulationOrderByOrderNumberDesc(@Param("simulation") final Simulation simulation);

}
