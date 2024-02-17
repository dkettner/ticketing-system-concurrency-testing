package com.kett.TicketSystem.ticket.domain;

import com.kett.TicketSystem.ticket.domain.exceptions.TicketException;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Getter
    @Setter(AccessLevel.PROTECTED)
    @Column(length = 16)
    private UUID id;

    @Getter
    private String title;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private LocalDateTime creationTime; // make creationTime final?

    @Getter
    private LocalDateTime dueTime;

    @Getter
    @Column(length = 16)
    private UUID projectId;

    @Getter
    @Setter
    @Column(length = 16)
    private UUID phaseId;

    @Getter
    @Type(type="uuid-char")
    @ElementCollection(targetClass = UUID.class, fetch = FetchType.EAGER)
    private List<UUID> assigneeIds = new ArrayList<>();

    public void setTitle(String title) {
        if (title == null || title.isEmpty()) {
            throw new TicketException("title must not be null or empty");
        }
        this.title = title;
    }

    public void setDueTime(LocalDateTime newDueTime) {
        if (newDueTime != null && newDueTime.isBefore(LocalDateTime.now())) {
            throw new TicketException("dueTime cannot be in the past");
        }
        this.dueTime = newDueTime;
    }

    protected void setProjectId(UUID projectId) {
        if (projectId == null) {
            throw new TicketException("projectId must not be null");
        }
        this.projectId = projectId;
    }

    public void setAssigneeIds(List<UUID> assigneeIds) {
        if (assigneeIds == null) {
            throw new TicketException("assigneeIds must not be null but it may be empty");
        }
        this.assigneeIds.clear();
        this.assigneeIds.addAll(assigneeIds);
    }

    public void removeAssignee(UUID userId) {
        assigneeIds.remove(userId);
    }

    public Boolean isAssignee(UUID assigneeId) {
        return this.assigneeIds.contains(assigneeId);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Ticket that = (Ticket) o;
        if (getId() != null && that.getId() != null) {
            // Both IDs are not null, compare them
            return Objects.equals(getId(), that.getId());
        } else {
            // One or both IDs are null, compare all other fields
            return Objects.equals(getTitle(), that.getTitle())
                    && Objects.equals(getDescription(), that.getDescription())
                    && Objects.equals(getCreationTime(), that.getCreationTime())
                    && Objects.equals(getDueTime(), that.getDueTime())
                    && Objects.equals(getProjectId(), that.getProjectId())
                    && Objects.equals(getPhaseId(), that.getPhaseId())
                    && Objects.equals(getAssigneeIds(), that.getAssigneeIds());
        }
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            // If ID is not null, use it for hash code
            return Objects.hash(getId());
        } else {
            // If ID is null, use all other fields for hash code
            return Objects.hash(getTitle(), getDescription(), getCreationTime(), getDueTime(), getProjectId(), getPhaseId(), getAssigneeIds());
        }
    }

    public Ticket(String title, String description, LocalDateTime dueTime, UUID projectId, UUID phaseId, List<UUID> assigneeIds) {
        this.setTitle(title);
        this.description = description;
        this.creationTime = LocalDateTime.now();
        this.setDueTime(dueTime);
        this.setPhaseId(phaseId);
        this.setProjectId(projectId);
        this.setAssigneeIds(assigneeIds);
    }
}
