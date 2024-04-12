package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.flow.FlowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FlowRepository extends JpaRepository<FlowNode, Long> {

  Optional<FlowNode> findFlowTaskByPreviousOrderNumberAndName(final long parentOrderNumber, final String name);

  boolean existsByName(final String name);

  @Query("SELECT DISTINCT f.name FROM flows f")
  Set<String> findAllNameFlows();

  @Query("SELECT f FROM flows f WHERE f.name = :name ORDER BY f.orderNumber LIMIT 10")
  List<FlowNode> findFirst10QuestionsByName(@Param("name") final String name);

  List<FlowNode> findAllByNameAndPreviousOrderNumber(@Param("name") final String name, @Param("previousOrderNumber") final long previousOrderNumber);

  List<FlowNode> findAllByOrderNumber(@Param("orderNumber") final long orderNumber);

}
