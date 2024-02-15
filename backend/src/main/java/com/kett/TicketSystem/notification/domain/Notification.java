package com.kett.TicketSystem.notification.domain;

import com.kett.TicketSystem.common.exceptions.IllegalStateUpdateException;
import com.kett.TicketSystem.notification.domain.exceptions.NotificationException;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Getter
    @Setter(AccessLevel.PROTECTED)
    @Column(length = 16)
    private UUID id;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private LocalDateTime creationTime;

    @Getter
    @Column(length = 16)
    private UUID recipientId;

    @Getter
    private Boolean isRead;

    @Getter
    @Column(length = 1000)
    private String content; // TODO: exchange for different types of events (invitation, ticketAssigned etc.)

    protected void setRecipientId(UUID recipientId) {
        if (recipientId == null) {
            throw new NotificationException("recipientId must not be null");
        }
        this.recipientId = recipientId;
    }

    protected void setContent(String content) {
        if (content == null || content.isEmpty()) {
            throw new NotificationException("content must not be null or empty");
        }
        this.content = content;
    }

    public void setIsRead(Boolean isReadStatus) {
        if (isReadStatus == null) {
            throw new NotificationException("isRead must not be null");
        }
        if (this.isRead) {
            throw new IllegalStateUpdateException(
                    "The notification with id: " + this.id + " has already been read " +
                    "but you provided \"" + isReadStatus + "\" ");
        }

        this.isRead = isReadStatus;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Notification that = (Notification) o;
        if (getId() != null && that.getId() != null) {
            // Both IDs are not null, compare them
            return Objects.equals(getId(), that.getId());
        } else {
            // One or both IDs are null, compare recipientId and content
            return Objects.equals(getRecipientId(), that.getRecipientId())
                    && Objects.equals(getContent(), that.getContent());
        }
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            // If ID is not null, use it for hash code
            return Objects.hash(getId());
        } else {
            // If ID is null, use recipientId and content for hash code
            return Objects.hash(getRecipientId(), getContent());
        }
    }

    public Notification(UUID recipientId, String content) {
        this.creationTime = LocalDateTime.now();
        this.setRecipientId(recipientId);
        this.isRead = Boolean.FALSE;
        this.setContent(content);
    }
}
