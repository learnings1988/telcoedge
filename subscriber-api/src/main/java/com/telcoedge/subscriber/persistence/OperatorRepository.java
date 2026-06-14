package com.telcoedge.subscriber.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorRepository extends JpaRepository<OperatorEntity, Long> {

    Optional<OperatorEntity> findByCode(String code);
    boolean existsByCode(String code);
}
