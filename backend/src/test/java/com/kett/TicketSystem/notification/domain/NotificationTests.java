package com.kett.TicketSystem.notification.domain;

import com.kett.TicketSystem.common.exceptions.IllegalStateUpdateException;
import com.kett.TicketSystem.notification.domain.exceptions.NotificationException;
import com.kett.TicketSystem.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class contains tests for the Notification class.
 * It tests all methods and fields of the Notification class.
 */

@SpringBootTest
@ActiveProfiles({ "test" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NotificationTests {
    private final NotificationRepository notificationRepository;

    private UUID uuid0;
    private UUID uuid1;
    private UUID uuid2;
    private UUID uuid3;

    private String message0;
    private String message1;
    private String message2;
    private String message3;

    private Notification notification0;
    private Notification notification1;
    private Notification notification2;
    private Notification notification3;

    private static class TestableNotification extends Notification {
        public TestableNotification(UUID recipientId, String content) {
            super(recipientId, content);
        }

        public void callSetRecipientId(UUID recipientId) {
            setRecipientId(recipientId);
        }

        public void callSetContent(String content) {
            setContent(content);
        }
    }

    @Autowired
    public NotificationTests(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @BeforeEach
    public void buildUp() {
        uuid0 = UUID.randomUUID();
        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
        uuid3 = UUID.randomUUID();

        message0 = "message0";
        message1 = "message1";
        message2 = "message2";
        message3 = "message3";

        notification0 = new Notification(uuid0, message0);
        notification1 = new Notification(uuid1, message1);
        notification2 = new Notification(uuid2, message2);
        notification3 = new Notification(uuid3, message3);
    }

    @AfterEach
    public void tearDown() {
        uuid0 = null;
        uuid1 = null;
        uuid2 = null;
        uuid3 = null;

        message0 = null;
        message1 = null;
        message2 = null;
        message3 = null;

        notification0 = null;
        notification1 = null;
        notification2 = null;
        notification3 = null;

        notificationRepository.deleteAll();
    }

    @Test
    public void checkValidConstructorParameters() {
        new Notification(uuid0, message0);
        new Notification(uuid1, message1);
        new Notification(uuid2, message2);
        new Notification(uuid3, message3);
    }

    @Test
    public void checkNullConstructorParameters() {
        assertThrows(NotificationException.class, () -> new Notification(null, message0));
        assertThrows(NotificationException.class, () -> new Notification(uuid0, null));
        assertThrows(NotificationException.class, () -> new Notification(null, null));
    }

    @Test
    public void checkInvalidConstructorParameters() {
        // Test with null recipientId
        assertThrows(NotificationException.class, () -> new Notification(null, "Test content"));

        // Test with null content
        assertThrows(NotificationException.class, () -> new Notification(UUID.randomUUID(), null));

        // Test with empty content
        assertThrows(NotificationException.class, () -> new Notification(UUID.randomUUID(), ""));

        // Test with null recipientId and null content
        assertThrows(NotificationException.class, () -> new Notification(null, null));

        // Test with null recipientId and empty content
        assertThrows(NotificationException.class, () -> new Notification(null, ""));

        // Test with empty content and null recipientId
        assertThrows(NotificationException.class, () -> new Notification(UUID.randomUUID(), null));
    }

    @Test
    public void checkSetIsReadWithValidParameters() {
        notification0.setIsRead(true);
        assertTrue(notification0.getIsRead());
    }

    @Test
    public void checkSetIsReadWithNullParameters() {
        assertThrows(NotificationException.class, () -> notification0.setIsRead(null));
    }

    @Test
    public void checkSetIsReadWhenNotificationIsAlreadyRead() {
        notification0.setIsRead(true);

        // set true again
        assertThrows(IllegalStateUpdateException.class, () -> notification0.setIsRead(true));

        // set false when is already true
        assertThrows(IllegalStateUpdateException.class, () -> notification0.setIsRead(false));
    }

    // testing the protected methods
    @Test
    public void checkSetRecipientIdWithNullParameter() {
        TestableNotification notification = new TestableNotification(UUID.randomUUID(), "Test content");
        assertThrows(NotificationException.class, () -> notification.callSetRecipientId(null));
    }

    @Test
    public void checkSetContentWithNullParameter() {
        TestableNotification notification = new TestableNotification(UUID.randomUUID(), "Test content");
        assertThrows(NotificationException.class, () -> notification.callSetContent(null));
    }

    @Test
    public void checkSetContentWithEmptyParameter() {
        TestableNotification notification = new TestableNotification(UUID.randomUUID(), "Test content");
        assertThrows(NotificationException.class, () -> notification.callSetContent(""));
    }
}
