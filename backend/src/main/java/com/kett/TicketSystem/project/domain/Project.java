package com.kett.TicketSystem.project.domain;

import com.kett.TicketSystem.project.domain.exceptions.ProjectException;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Getter
    @Setter(AccessLevel.PROTECTED)
    @Column(length = 16)
    private UUID id;

    @Getter
    private String name;

    @Getter
    @Column(length = 1000)
    private String description;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private LocalDateTime creationTime;

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new ProjectException("name must not be null or empty");
        }

        this.name = name;
    }

    public void setDescription(String description) {
        if (description == null) {
            description = "";
        }

        this.description = description;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Project that = (Project) o;
        if (getId() != null && that.getId() != null) {
            // Both IDs are not null, compare them
            return Objects.equals(getId(), that.getId());
        } else {
            // One or both IDs are null, compare name, description and creationTime
            return Objects.equals(getName(), that.getName())
                    && Objects.equals(getDescription(), that.getDescription())
                    && Objects.equals(getCreationTime(), that.getCreationTime());
        }
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            // If ID is not null, use it for hash code
            return Objects.hash(getId());
        } else {
            // If ID is null, use name, description and creationTime for hash code
            return Objects.hash(getName(), getDescription(), getCreationTime());
        }
    }

    public Project(String name, String description) {
        this.setName(name);
        this.setDescription(description);
        this.creationTime = LocalDateTime.now();
    }
}
