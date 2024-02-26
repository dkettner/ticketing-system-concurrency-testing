package com.kett.TicketSystem.phase.repository;

import com.kett.TicketSystem.phase.domain.consumedData.ProjectDataOfPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectDataOfPhaseRepository extends JpaRepository<ProjectDataOfPhase, UUID> {
    Integer deleteByProjectId(UUID projectId);

    Boolean existsByProjectId(UUID projectId);
}
