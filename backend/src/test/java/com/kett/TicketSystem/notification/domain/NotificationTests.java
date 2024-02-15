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

        notification1.setIsRead(true);
        assertTrue(notification1.getIsRead());

        notification2.setIsRead(true);
        assertTrue(notification2.getIsRead());

        notification3.setIsRead(true);
        assertTrue(notification3.getIsRead());
    }

    @Test
    public void checkSetIsReadWithNullParameters() {
        assertThrows(NotificationException.class, () -> notification0.setIsRead(null));
        assertThrows(NotificationException.class, () -> notification1.setIsRead(null));
        assertThrows(NotificationException.class, () -> notification2.setIsRead(null));
        assertThrows(NotificationException.class, () -> notification3.setIsRead(null));
    }

    @Test
    public void checkSetIsReadWhenNotificationIsAlreadyRead() {
        notification0.setIsRead(true);
        notification1.setIsRead(true);
        notification2.setIsRead(true);
        notification3.setIsRead(true);

        // set true again
        assertThrows(IllegalStateUpdateException.class, () -> notification0.setIsRead(true));
        assertThrows(IllegalStateUpdateException.class, () -> notification1.setIsRead(true));
        assertThrows(IllegalStateUpdateException.class, () -> notification2.setIsRead(true));
        assertThrows(IllegalStateUpdateException.class, () -> notification3.setIsRead(true));

        // set false when is already true
        assertThrows(IllegalStateUpdateException.class, () -> notification0.setIsRead(false));
        assertThrows(IllegalStateUpdateException.class, () -> notification1.setIsRead(false));
        assertThrows(IllegalStateUpdateException.class, () -> notification2.setIsRead(false));
        assertThrows(IllegalStateUpdateException.class, () -> notification3.setIsRead(false));
    }

    @Test
    public void checkEquals() {
        Notification notification0Copy = new Notification(uuid0, message0);
        Notification notification1Copy = new Notification(uuid1, message1);
        Notification notification2Copy = new Notification(uuid2, message2);
        Notification notification3Copy = new Notification(uuid3, message3);

        // without id, same parameters
        assertEquals(notification0, new Notification(uuid0, message0));
        assertEquals(notification1, new Notification(uuid1, message1));
        assertEquals(notification2, new Notification(uuid2, message2));
        assertEquals(notification3, new Notification(uuid3, message3));

        // without id, different parameters
        assertNotEquals(notification0, new Notification(uuid1, message1));
        assertNotEquals(notification1, new Notification(uuid2, message2));
        assertNotEquals(notification2, new Notification(uuid3, message3));
        assertNotEquals(notification3, new Notification(uuid0, message0));

        // add id
        notificationRepository.save(notification0);
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);
        notificationRepository.save(notification0Copy);
        notificationRepository.save(notification1Copy);
        notificationRepository.save(notification2Copy);
        notificationRepository.save(notification3Copy);

        // with id, compare with itself
        assertEquals(notification0, notificationRepository.findById(notification0.getId()).get());
        assertEquals(notification1, notificationRepository.findById(notification1.getId()).get());
        assertEquals(notification2, notificationRepository.findById(notification2.getId()).get());
        assertEquals(notification3, notificationRepository.findById(notification3.getId()).get());

        // with id, compare with different object
        assertNotEquals(notification0, notification1);
        assertNotEquals(notification1, notification2);
        assertNotEquals(notification2, notification3);
        assertNotEquals(notification3, notification0);

        // with id, compare with different object with same parameters
        assertNotEquals(notification0, notification0Copy);
        assertNotEquals(notification1, notification1Copy);
        assertNotEquals(notification2, notification2Copy);
        assertNotEquals(notification3, notification3Copy);
    }

    @Test
    public void checkHashCode() {
        Notification notification0Copy = new Notification(uuid0, message0);
        Notification notification1Copy = new Notification(uuid1, message1);
        Notification notification2Copy = new Notification(uuid2, message2);
        Notification notification3Copy = new Notification(uuid3, message3);

        // without id, same parameters
        assertEquals(notification0.hashCode(), notification0Copy.hashCode());
        assertEquals(notification1.hashCode(), notification1Copy.hashCode());
        assertEquals(notification2.hashCode(), notification2Copy.hashCode());
        assertEquals(notification3.hashCode(), notification3Copy.hashCode());

        // without id, different parameters
        assertNotEquals(notification0.hashCode(), notification1.hashCode());
        assertNotEquals(notification1.hashCode(), notification2.hashCode());
        assertNotEquals(notification2.hashCode(), notification3.hashCode());
        assertNotEquals(notification3.hashCode(), notification0.hashCode());

        // add id
        notificationRepository.save(notification0);
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);
        notificationRepository.save(notification0Copy);
        notificationRepository.save(notification1Copy);
        notificationRepository.save(notification2Copy);
        notificationRepository.save(notification3Copy);

        // with id, compare with itself
        assertEquals(notification0.hashCode(), notificationRepository.findById(notification0.getId()).get().hashCode());
        assertEquals(notification1.hashCode(), notificationRepository.findById(notification1.getId()).get().hashCode());
        assertEquals(notification2.hashCode(), notificationRepository.findById(notification2.getId()).get().hashCode());
        assertEquals(notification3.hashCode(), notificationRepository.findById(notification3.getId()).get().hashCode());

        // with id, compare with different object
        assertNotEquals(notification0.hashCode(), notification1.hashCode());
        assertNotEquals(notification1.hashCode(), notification2.hashCode());
        assertNotEquals(notification2.hashCode(), notification3.hashCode());
        assertNotEquals(notification3.hashCode(), notification0.hashCode());

        // with id, compare with different object with same parameters
        assertNotEquals(notification0.hashCode(), notification0Copy.hashCode());
        assertNotEquals(notification1.hashCode(), notification1Copy.hashCode());
        assertNotEquals(notification2.hashCode(), notification2Copy.hashCode());
        assertNotEquals(notification3.hashCode(), notification3Copy.hashCode());
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
