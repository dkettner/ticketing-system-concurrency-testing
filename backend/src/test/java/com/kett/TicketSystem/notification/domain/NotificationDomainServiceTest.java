package com.kett.TicketSystem.notification.domain;

import com.kett.TicketSystem.common.domainprimitives.EmailAddress;
import com.kett.TicketSystem.common.exceptions.IllegalStateUpdateException;
import com.kett.TicketSystem.common.exceptions.ImpossibleException;
import com.kett.TicketSystem.notification.domain.consumedData.UserDataOfNotification;
import com.kett.TicketSystem.notification.domain.exceptions.NoNotificationFoundException;
import com.kett.TicketSystem.notification.domain.exceptions.NotificationException;
import com.kett.TicketSystem.notification.repository.NotificationRepository;
import com.kett.TicketSystem.notification.repository.UserDataOfNotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

// write unit tests for notificationDomainService and use ProjectDomainServiceTest as a reference.
// use the structure of Arrange, Act, and Assert

@ExtendWith(MockitoExtension.class)
public class NotificationDomainServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserDataOfNotificationRepository userDataOfNotificationRepository;

    @InjectMocks
    private NotificationDomainService notificationDomainService;

    private UUID notificationId0;
    private String content0;
    private UUID recipientId0;
    private Notification notification0;

    private UUID notificationId1;
    private String content1;
    private Notification notification1;

    private EmailAddress emailAddress0;
    private UserDataOfNotification userDataOfNotification0;


    @BeforeEach
    public void buildUp() {
        recipientId0 = UUID.randomUUID();
        content0 = "content0";
        notification0 = new Notification(recipientId0, content0);
        notificationId0 = UUID.randomUUID();
        notification0.setId(notificationId0);

        content1 = "content1";
        notification1 = new Notification(recipientId0, content1);
        notificationId1 = UUID.randomUUID();
        notification1.setId(notificationId1);

        emailAddress0 = EmailAddress.fromString("email0@gmail.com");
        userDataOfNotification0 = new UserDataOfNotification(recipientId0, emailAddress0);
    }

    @AfterEach
    public void tearDown() {
        notificationId0 = null;
        content0 = null;
        recipientId0 = null;
        emailAddress0 = null;
        notification0 = null;

        userDataOfNotification0 = null;
    }

    @Test
    public void testGetNotificationById() {
        // Arrange
        when(notificationRepository.findById(notificationId0)).thenReturn(Optional.of(notification0));

        // Act
        Notification result = notificationDomainService.getNotificationById(notificationId0);

        // Assert
        assertEquals(notification0.getId(), result.getId());
        assertEquals(notification0.getRecipientId(), result.getRecipientId());
        assertEquals(notification0.getContent(), result.getContent());
        assertEquals(notification0.getCreationTime(), result.getCreationTime());
        assertEquals(notification0.getIsRead(), result.getIsRead());

        verify(notificationRepository).findById(notificationId0);

    }

    @Test
    public void testGetNotificationByIdNotFound() {
        // Arrange
        when(notificationRepository.findById(notificationId0)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoNotificationFoundException.class, () -> notificationDomainService.getNotificationById(notificationId0));

        verify(notificationRepository).findById(notificationId0);
    }

    @Test
    public void testGetNotificationsByRecipientId() {
        // Arrange
        when(notificationRepository.findByRecipientId(recipientId0)).thenReturn(java.util.List.of(notification0, notification1));

        // Act
        List<Notification> result = notificationDomainService.getNotificationsByRecipientId(recipientId0);

        // Assert
        assertEquals(2, result.size());
        assertEquals(notification0.getId(), result.get(0).getId());
        assertEquals(notification0.getRecipientId(), result.get(0).getRecipientId());
        assertEquals(notification0.getContent(), result.get(0).getContent());
        assertEquals(notification0.getCreationTime(), result.get(0).getCreationTime());
        assertEquals(notification0.getIsRead(), result.get(0).getIsRead());
        assertEquals(notification1.getId(), result.get(1).getId());
        assertEquals(notification1.getRecipientId(), result.get(1).getRecipientId());
        assertEquals(notification1.getContent(), result.get(1).getContent());
        assertEquals(notification1.getCreationTime(), result.get(1).getCreationTime());
        assertEquals(notification1.getIsRead(), result.get(1).getIsRead());

        verify(notificationRepository).findByRecipientId(recipientId0);
    }

    @Test
    public void testGetNotificationsByRecipientIdNoNotificationFound() {
        // Arrange
        when(notificationRepository.findByRecipientId(recipientId0)).thenReturn(java.util.List.of());

        // Act & Assert
        assertThrows(NoNotificationFoundException.class, () -> notificationDomainService.getNotificationsByRecipientId(recipientId0));

        verify(notificationRepository).findByRecipientId(recipientId0);
    }

    @Test
    public void testGetNotificationsByUserEmail() {
        // Arrange
        when(userDataOfNotificationRepository.findByUserEmailEquals(emailAddress0))
                .thenReturn(java.util.List.of(userDataOfNotification0));
        when(notificationRepository.findByRecipientId(recipientId0))
                .thenReturn(java.util.List.of(notification0, notification1));

        // Act
        List<Notification> result = notificationDomainService.getNotificationsByUserEmail(emailAddress0);

        // Assert
        assertEquals(2, result.size());
        assertEquals(notification0.getId(), result.get(0).getId());
        assertEquals(notification0.getRecipientId(), result.get(0).getRecipientId());
        assertEquals(notification0.getContent(), result.get(0).getContent());
        assertEquals(notification0.getCreationTime(), result.get(0).getCreationTime());
        assertEquals(notification0.getIsRead(), result.get(0).getIsRead());
        assertEquals(notification1.getId(), result.get(1).getId());
        assertEquals(notification1.getRecipientId(), result.get(1).getRecipientId());
        assertEquals(notification1.getContent(), result.get(1).getContent());
        assertEquals(notification1.getCreationTime(), result.get(1).getCreationTime());
        assertEquals(notification1.getIsRead(), result.get(1).getIsRead());

        verify(userDataOfNotificationRepository).findByUserEmailEquals(emailAddress0);
        verify(notificationRepository).findByRecipientId(recipientId0);
    }

    @Test
    public void testGetNotificationsByUserEmailNoUserDataFound() {
        // Arrange
        when(userDataOfNotificationRepository.findByUserEmailEquals(emailAddress0))
                .thenReturn(java.util.List.of());

        // Act & Assert
        assertThrows(ImpossibleException.class, () -> notificationDomainService.getNotificationsByUserEmail(emailAddress0));

        verify(userDataOfNotificationRepository).findByUserEmailEquals(emailAddress0);
    }

    @Test
    public void testGetNotificationsByUserEmailNoNotificationFound() {
        // Arrange
        when(userDataOfNotificationRepository.findByUserEmailEquals(emailAddress0))
                .thenReturn(java.util.List.of(userDataOfNotification0));
        when(notificationRepository.findByRecipientId(recipientId0))
                .thenReturn(java.util.List.of());

        // Act & Assert
        assertThrows(NoNotificationFoundException.class, () -> notificationDomainService.getNotificationsByUserEmail(emailAddress0));

        verify(userDataOfNotificationRepository).findByUserEmailEquals(emailAddress0);
        verify(notificationRepository).findByRecipientId(recipientId0);
    }

    @Test
    public void testGetRecipientIdByNotificationId() {
        // Arrange
        when(notificationRepository.findById(notificationId0)).thenReturn(Optional.of(notification0));

        // Act
        UUID result = notificationDomainService.getRecipientIdByNotificationId(notificationId0);

        // Assert
        assertEquals(recipientId0, result);

        verify(notificationRepository).findById(notificationId0);
    }

    @Test
    public void testGetRecipientIdByNotificationIdNotFound() {
        // Arrange
        when(notificationRepository.findById(notificationId0)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoNotificationFoundException.class, () -> notificationDomainService.getRecipientIdByNotificationId(notificationId0));

        verify(notificationRepository).findById(notificationId0);
    }

    @Test
    public void testGetUserIdByUserEmailAddress() {
        // Arrange
        when(userDataOfNotificationRepository.findByUserEmailEquals(emailAddress0))
                .thenReturn(java.util.List.of(userDataOfNotification0));

        // Act
        UUID result = notificationDomainService.getUserIdByUserEmailAddress(emailAddress0);

        // Assert
        assertEquals(recipientId0, result);

        verify(userDataOfNotificationRepository).findByUserEmailEquals(emailAddress0);
    }

    @Test
    public void testGetUserIdByUserEmailAddressNoUserDataFound() {
        // Arrange
        when(userDataOfNotificationRepository.findByUserEmailEquals(emailAddress0))
                .thenReturn(java.util.List.of());

        // Act & Assert
        assertThrows(ImpossibleException.class, () -> notificationDomainService.getUserIdByUserEmailAddress(emailAddress0));

        verify(userDataOfNotificationRepository).findByUserEmailEquals(emailAddress0);
    }

    @Test
    public void testPatchById() {
        // Arrange
        when(notificationRepository.findById(notificationId0)).thenReturn(Optional.of(notification0));

        // Act
        notificationDomainService.patchById(notificationId0, true);

        // Assert
        assertEquals(true, notification0.getIsRead());
        verify(notificationRepository).findById(notificationId0);
        verify(notificationRepository).save(notification0);
    }

    @Test
    public void testPatchByIdNotFound() {
        // Arrange
        when(notificationRepository.findById(notificationId0)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoNotificationFoundException.class, () -> notificationDomainService.patchById(notificationId0, true));

        verify(notificationRepository).findById(notificationId0);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    public void testPatchByIdIllegalStateUpdate() {
        // Arrange
        when(notificationRepository.findById(notificationId0)).thenReturn(Optional.of(notification0));
        notification0.setIsRead(true); // a read notification cannot be reset to unread

        // Act & Assert
        assertThrows(IllegalStateUpdateException.class, () -> notificationDomainService.patchById(notificationId0, false));

        verify(notificationRepository).findById(notificationId0);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    public void testPatchByIdNullIsRead() {
        // Arrange
        when(notificationRepository.findById(notificationId0)).thenReturn(Optional.of(notification0));

        // Act & Assert
        assertThrows(NotificationException.class, () -> notificationDomainService.patchById(notificationId0, null));

        verify(notificationRepository).findById(notificationId0);
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    public void testDeleteById() {
        // Arrange
        when(notificationRepository.removeById(notificationId0)).thenReturn(1L);

        // Act
        notificationDomainService.deleteById(notificationId0);

        // Assert
        verify(notificationRepository).removeById(notificationId0);
    }

    @Test
    public void testDeleteByIdNotFound() {
        // Arrange
        when(notificationRepository.removeById(notificationId0)).thenReturn(0L);

        // Act & Assert
        assertThrows(NoNotificationFoundException.class, () -> notificationDomainService.deleteById(notificationId0));

        verify(notificationRepository).removeById(notificationId0);
    }

    @Test
    public void testDeleteByRecipientId() {
        // Arrange
        // nothing to arrange

        // Act
        notificationDomainService.deleteByRecipientId(recipientId0);

        // Assert
        verify(notificationRepository).deleteByRecipientId(recipientId0);
    }


}