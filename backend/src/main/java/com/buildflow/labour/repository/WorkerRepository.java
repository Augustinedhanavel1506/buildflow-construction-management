package com.buildflow.labour.repository;

import com.buildflow.labour.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
    List<Worker> findByProjectIdOrderByNameAsc(Long projectId);

    Optional<Worker> findByIdAndProjectBusinessId(Long id, Long businessId);
}
