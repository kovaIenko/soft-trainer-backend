package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.flow.FlowQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlowRepository extends JpaRepository<FlowQuestion, Long> {

  Optional<FlowQuestion> findFlowTaskByPreviousOrderNumberAndName(final long parentOrderNumber, final String name);

  boolean existsByName(final String name);
}
