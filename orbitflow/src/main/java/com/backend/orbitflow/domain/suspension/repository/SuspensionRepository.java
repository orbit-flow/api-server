package com.backend.orbitflow.domain.suspension.repository;

import com.backend.orbitflow.domain.suspension.entity.Suspension;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuspensionRepository extends JpaRepository<Suspension, Long> {
}
