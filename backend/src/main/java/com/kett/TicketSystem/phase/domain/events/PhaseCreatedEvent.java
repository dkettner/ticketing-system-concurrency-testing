package com.kett.TicketSystem.phase.domain.events;

import com.kett.TicketSystem.common.domainprimitives.DomainEvent;
import com.kett.TicketSystem.phase.domain.Phase;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PhaseCreatedEvent extends DomainEvent {
    private final UUID phaseId;
    private final UUID previousPhaseId;
    private final UUID projectId;

    public PhaseCreatedEvent(UUID phaseId, Phase previousPhase, UUID projectId) {
        super();
        this.phaseId = phaseId;
        this.previousPhaseId = previousPhase == null ? null: previousPhase.getId();
        this.projectId = projectId;
    }
}
