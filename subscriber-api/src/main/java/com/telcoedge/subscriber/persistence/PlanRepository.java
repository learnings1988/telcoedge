package com.telcoedge.subscriber.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<PlanEntity, Long> {

    List<PlanEntity> findByOperatorIdAndActiveTrue(String operatorId);

    @Query("SELECT p FROM PlanEntity p JOIN FETCH p.tariffRates where p.id = :planId")
    Optional<PlanEntity> findByIdWithRates(@Param("planId") Long planId);

    @EntityGraph(attributePaths = {"tariffRates"})
    @Query("SELECT p from PlanEntity p where p.operatorId = :operatorId AND p.active= true")
    List<PlanEntity> findByOperatorIdWithRates(@Param("operatorId") String operatorId);
}
