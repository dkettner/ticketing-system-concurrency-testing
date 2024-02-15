package com.kett.TicketSystem.membership.domain;

import com.kett.TicketSystem.common.exceptions.IllegalStateUpdateException;
import com.kett.TicketSystem.membership.domain.exceptions.MembershipException;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership implements GrantedAuthority {
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
    @Column(length = 16)
    private UUID userId;

    @Getter
    @Enumerated(EnumType.STRING)
    private Role role;

    @Getter
    @Enumerated(EnumType.STRING)
    private State state;

    public Boolean isAccepted() {
        return this.state.equals(State.ACCEPTED);
    }

    protected void setProjectId(UUID projectId) {
        if (projectId == null) {
            throw new MembershipException("projectId cannot be null");
        }
        if (this.getProjectId() != null && !this.getProjectId().equals(projectId)) {
            throw new IllegalStateUpdateException("projectId cannot be changed");
        }
        this.projectId = projectId;
    }

    protected void setUserId(UUID userId) {
        if (userId == null) {
            throw new MembershipException("userId cannot be null");
        }
        if (this.getUserId() != null && !this.getUserId().equals(userId)) {
            throw new IllegalStateUpdateException("userId cannot be changed");
        }
        this.userId = userId;
    }

    public void setState(State state) {
        if (state == null) {
            throw new MembershipException("state must not be null");
        }

        if (this.state != null) {
            if (this.state.equals(state)) {
                throw new IllegalStateUpdateException("state of membership with id: " + this.id + " is already " + this.state);
            }

            if (this.state.equals(State.ACCEPTED) && state.equals(State.OPEN)) {
                throw new IllegalStateUpdateException(
                        "Once state has been changed to ACCEPTED, it cannot go back to OPEN. " +
                        "To revoke Membership, use delete."
                );
            }
        }


        this.state = state;
    }

    public void setRole(Role role) {
        if (role == null) {
            throw new MembershipException("role cannot be null");
        }
        this.role = role;
    }

    @Override
    public String getAuthority() {
        return  "ROLE_" +
                "PROJECT_" +
                this.role.toString() + "_" +
                this.projectId.toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Membership that = (Membership) o;
        if (getId() != null && that.getId() != null) {
            // Both IDs are not null, compare them
            return Objects.equals(getId(), that.getId());
        } else {
            // One or both IDs are null, compare projectId, userId, role, and state
            return Objects.equals(getProjectId(), that.getProjectId())
                    && Objects.equals(getUserId(), that.getUserId())
                    && Objects.equals(getRole(), that.getRole())
                    && Objects.equals(getState(), that.getState());
        }
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            // If ID is not null, use it for hash code
            return Objects.hash(getId());
        } else {
            // If ID is null, use projectId, userId, role, and state for hash code
            return Objects.hash(getProjectId(), getUserId(), getRole(), getState());
        }
    }

    public Membership(UUID projectId, UUID userId, Role role) {
        this.setProjectId(projectId);
        this.setUserId(userId);
        this.setRole(role);
        this.setState(State.OPEN);
    }
}
