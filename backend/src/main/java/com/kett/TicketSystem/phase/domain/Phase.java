package com.kett.TicketSystem.phase.domain;

import com.kett.TicketSystem.phase.domain.exceptions.PhaseException;
import com.kett.TicketSystem.common.exceptions.UnrelatedPhaseException;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Phase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Getter
    @Setter(AccessLevel.PROTECTED)
    @Column(length = 16)
    private UUID id;

    @Getter
    @Column(length = 16)
    private UUID projectId;

    @Getter
    private String name;

    @Getter
    @OneToOne(fetch = FetchType.LAZY)
    private Phase previousPhase;

    @Getter
    @OneToOne(fetch = FetchType.LAZY)
    private Phase nextPhase;

    @Getter
    private Integer ticketCount;

    protected void setProjectId(UUID projectId) {
        if (projectId == null) {
            throw new PhaseException("projectId must not be null");
        }
        this.projectId = projectId;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new PhaseException("name must not be null or empty");
        }
        this.name = name;
    }

    public void setPreviousPhase(Phase phase) {
        if (phase != null && phase.equals(this)) {
            throw new PhaseException("A phase cannot be its own previous phase");
        }
        if (phase != null && !this.projectId.equals(phase.getProjectId())) {
            throw new UnrelatedPhaseException(
                    "The new previousPhase with id: " + phase.getId() + " belongs to the project with id: " + phase.getProjectId() +
                    " but the phase with id: " + this.id + " does not. It belongs to the project with id: " + this.projectId + "."
            );
        }
        this.previousPhase = phase;
    }

    public void setNextPhase(Phase phase) {
        if (phase != null && phase.equals(this)) {
            throw new PhaseException("A phase cannot be its own next phase");
        }
        if (phase != null && !this.projectId.equals(phase.getProjectId())) {
            throw new UnrelatedPhaseException(
                    "The new nextPhase with id: " + phase.getId() + " belongs to the project with id: " + phase.getProjectId() +
                    " but the phase with id: " + this.id + " does not. It belongs to the project with id: " + this.projectId + "."
            );
        }
        this.nextPhase = phase;
    }

    public void setTicketCount(int ticketCount) throws PhaseException {
        if (ticketCount < 0) {
            throw new PhaseException("ticketCount cannot be negative");
        }
        this.ticketCount = ticketCount;
    }

    public void increaseTicketCount() throws PhaseException {
        this.setTicketCount(
                this.getTicketCount() + 1
        );
    }

    public void decreaseTicketCount() throws PhaseException {
        this.setTicketCount(
                this.getTicketCount() - 1
        );
    }

    public Boolean isFirst() {
        return previousPhase == null;
    }

    public Boolean isLast() {
        return nextPhase == null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Phase that = (Phase) o;
        if (getId() != null && that.getId() != null) {
            // Both IDs are not null, compare them
            return Objects.equals(getId(), that.getId());
        } else {
            // One or both IDs are null, compare name and other relevant fields

            // previousPhase and nextPhase are nullable
            // setting the IDs to null avoids NullPointerExceptions and prevents infinite recursion
            UUID thisPreviousPhaseId = this.getPreviousPhase() == null ? null : this.getPreviousPhase().getId();
            UUID thisNextPhaseId = this.getNextPhase() == null ? null : this.getNextPhase().getId();
            UUID thatPreviousPhaseId = that.getPreviousPhase() == null ? null : that.getPreviousPhase().getId();
            UUID thatNextPhaseId = that.getNextPhase() == null ? null : that.getNextPhase().getId();

            return Objects.equals(getName(), that.getName())
                    && Objects.equals(thisPreviousPhaseId, thatPreviousPhaseId)
                    && Objects.equals(thisNextPhaseId, thatNextPhaseId)
                    && Objects.equals(getTicketCount(), that.getTicketCount());
        }
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            // If ID is not null, use it for hash code
            return Objects.hash(getId());
        } else {
            // If ID is null, use name and other relevant fields for hash code

            // previousPhase and nextPhase are nullable
            // setting the IDs to null avoids NullPointerExceptions
            UUID thisPreviousPhaseId = this.getPreviousPhase() == null ? null : this.getPreviousPhase().getId();
            UUID thisNextPhaseId = this.getNextPhase() == null ? null : this.getNextPhase().getId();

            return Objects.hash(getName(), thisPreviousPhaseId, thisNextPhaseId, getTicketCount());
        }
    }

    public Phase(UUID projectId, String name, Phase previousPhase, Phase nextPhase) {
        this.setProjectId(projectId);
        this.setName(name);
        this.setPreviousPhase(previousPhase);
        this.setNextPhase(nextPhase);
        this.setTicketCount(0);
    }
}
