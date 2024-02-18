package com.kett.TicketSystem.user.domain;

import com.kett.TicketSystem.common.domainprimitives.EmailAddress;
import com.kett.TicketSystem.common.exceptions.NoUserFoundException;
import com.kett.TicketSystem.membership.domain.MembershipDomainService;
import com.kett.TicketSystem.user.domain.events.UserCreatedEvent;
import com.kett.TicketSystem.user.domain.events.UserDeletedEvent;
import com.kett.TicketSystem.user.domain.events.UserPatchedEvent;
import com.kett.TicketSystem.user.domain.exceptions.EmailAlreadyInUseException;
import com.kett.TicketSystem.user.domain.exceptions.UserException;
import com.kett.TicketSystem.user.repository.UserRepository;
import com.kett.TicketSystem.util.EventCatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserDomainServiceTest {

    private String name0;
    private String email0;
    private String password0;
    private User user0;

    private String name1;
    private String email1;
    private String password1;
    private User userWithId;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipDomainService membershipDomainService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserDomainService userDomainService;

    @BeforeEach
    public void buildUp() {
        lenient()
                .when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User argument = (User) invocation.getArgument(0);
                    if (argument.getId() == null) {
                        argument.setId(UUID.randomUUID());
                    }
                    return argument;
                });

        lenient()
                .when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> {
                    String argument = (String) invocation.getArgument(0);
                    return "encoded" + argument;
                });

        name0 = "Bugs Bunny";
        email0 = "test@example.com";
        password0 = "password";
        user0 = new User(name0, email0, password0);

        name1 = "Daffy Duck";
        email1 = "loony@tuns.com";
        password1 = "password";
        userWithId = new User(name1, email1, password1);
        userWithId.setId(UUID.randomUUID());
    }

    @AfterEach
    public void tearDown() {
        name0 = null;
        email0 = null;
        password0 = null;
        user0 = null;

        name1 = null;
        email1 = null;
        password1 = null;
        userWithId = null;
    }

    @Test
    public void testAddUser() throws EmailAlreadyInUseException {
        // Arrange
        when(userRepository.existsByEmailEquals(user0.getEmail()))
                .thenReturn(false);

        // Act
        User result = userDomainService.addUser(user0);

        // Assert
        assertEquals(user0.getId(), result.getId());
        assertEquals(user0.getName(), result.getName());
        assertEquals(user0.getEmail(), result.getEmail());
        assertEquals("encoded" + password0, result.getPassword());

        verify(userRepository).existsByEmailEquals(user0.getEmail());
        verify(passwordEncoder).encode(password0);
        verify(userRepository).save(user0);

        // check if the constructor was called with the correct arguments
        ArgumentCaptor<UserCreatedEvent> argumentCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(eventPublisher).publishEvent(argumentCaptor.capture());
        assertEquals(user0.getId(), argumentCaptor.getValue().getUserId());
        assertEquals(user0.getName(), argumentCaptor.getValue().getName());
        assertEquals(user0.getEmail(), argumentCaptor.getValue().getEmailAddress());
    }

    @Test
    public void testAddUserEmailAlreadyInUse() {
        // Arrange
        when(userRepository.existsByEmailEquals(user0.getEmail()))
                .thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyInUseException.class, () -> userDomainService.addUser(user0));

        verify(userRepository).existsByEmailEquals(user0.getEmail());
        verify(userRepository, never()).save(user0);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testGetUserById() {
        // Arrange
        when(userRepository.findById(userWithId.getId()))
                .thenReturn(java.util.Optional.of(userWithId));

        // Act
        User result = userDomainService.getUserById(userWithId.getId());

        // Assert
        assertEquals(userWithId, result);
        verify(userRepository).findById(userWithId.getId());
    }

    @Test
    public void testGetUserByIdNoUserFound() {
        // Arrange
        when(userRepository.findById(userWithId.getId()))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoUserFoundException.class, () -> userDomainService.getUserById(userWithId.getId()));

        verify(userRepository).findById(userWithId.getId());
    }

    @Test
    public void testGetUserByEMailAddress() {
        // Arrange
        when(userRepository.findByEmailEquals(userWithId.getEmail()))
                .thenReturn(java.util.Optional.of(userWithId));

        // Act
        User result = userDomainService.getUserByEMailAddress(userWithId.getEmail());

        // Assert
        assertEquals(userWithId, result);
        verify(userRepository).findByEmailEquals(userWithId.getEmail());
    }

    @Test
    public void testGetUserByEMailAddressNoUserFound() {
        // Arrange
        when(userRepository.findByEmailEquals(userWithId.getEmail()))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoUserFoundException.class, () -> userDomainService.getUserByEMailAddress(userWithId.getEmail()));

        verify(userRepository).findByEmailEquals(userWithId.getEmail());
    }

    @Test
    public void testLoadUserByUsername() {
        // Arrange
        when(userRepository.findByEmailEquals(userWithId.getEmail()))
                .thenReturn(java.util.Optional.of(userWithId));
        when(membershipDomainService.getProjectAuthoritiesByUserId(userWithId.getId()))
                .thenReturn(new ArrayList<>());

        // Act
        UserDetails result = userDomainService.loadUserByUsername(userWithId.getEmail().toString());

        // Assert
        assertEquals(userWithId.getEmail().toString(), result.getUsername());
        assertEquals( password1, result.getPassword()); // password is not encoded in this test
        assertEquals(1, result.getAuthorities().size());
        assertEquals("ROLE_USER_" + userWithId.getId(), result.getAuthorities().iterator().next().getAuthority());

        verify(userRepository).findByEmailEquals(userWithId.getEmail());
        verify(membershipDomainService).getProjectAuthoritiesByUserId(userWithId.getId());
    }

    @Test
    public void testLoadUserByUsernameNoUserFound() {
        // Arrange
        when(userRepository.findByEmailEquals(userWithId.getEmail()))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoUserFoundException.class, () -> userDomainService.loadUserByUsername(userWithId.getEmail().toString()));

        verify(userRepository).findByEmailEquals(userWithId.getEmail());
        verifyNoInteractions(membershipDomainService);
    }

    @Test
    public void testPatchUserById() throws UserException, NoUserFoundException {
        // Arrange
        String newName = "Elmer Fudd";
        String newEmail = "elmer@fudd.net";

        when(userRepository.existsByEmailEquals(EmailAddress.fromString(newEmail)))
                .thenReturn(false);
        when(userRepository.findById(userWithId.getId()))
                .thenReturn(java.util.Optional.of(userWithId));

        // Act
        userDomainService.patchUserById(userWithId.getId(), newName, newEmail);

        // Assert
        assertEquals(newName, userWithId.getName());
        assertEquals(newEmail, userWithId.getEmail().toString());

        verify(userRepository).existsByEmailEquals(EmailAddress.fromString(newEmail));
        verify(userRepository).findById(userWithId.getId());
        verify(userRepository).save(userWithId);

        // check if the constructor was called with the correct arguments
        ArgumentCaptor<UserPatchedEvent> argumentCaptor = ArgumentCaptor.forClass(UserPatchedEvent.class);
        verify(eventPublisher).publishEvent(argumentCaptor.capture());
        assertEquals(userWithId.getId(), argumentCaptor.getValue().getUserId());
        assertEquals(userWithId.getName(), argumentCaptor.getValue().getName());
        assertEquals(userWithId.getEmail(), argumentCaptor.getValue().getEmailAddress());
    }

    @Test
    public void testPatchUserByIdNoUserFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        String newName = "Elmer Fudd";
        String newEmail = "elmer@fudd.net";

        when(userRepository.findById(id))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoUserFoundException.class, () -> userDomainService.patchUserById(id, newName, newEmail));

        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testPatchUserByIdEmailAlreadyInUse() {
        // Arrange
        String newName = "Elmer Fudd";
        String newEmail = "elmer@fudd.net";

        when(userRepository.existsByEmailEquals(EmailAddress.fromString(newEmail)))
                .thenReturn(true);
        when(userRepository.findById(userWithId.getId()))
                .thenReturn(java.util.Optional.of(userWithId));

        // Act & Assert
        assertThrows(EmailAlreadyInUseException.class, () -> userDomainService.patchUserById(userWithId.getId(), newName, newEmail));

        verify(userRepository).existsByEmailEquals(EmailAddress.fromString(newEmail));
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testDeleteById() {
        // Arrange
        when(userRepository.findById(userWithId.getId()))
                .thenReturn(java.util.Optional.of(userWithId));
        when(userRepository.removeById(userWithId.getId()))
                .thenReturn(1L);

        // Act
        userDomainService.deleteById(userWithId.getId());

        // Assert
        verify(userRepository).findById(userWithId.getId());
        verify(userRepository).removeById(userWithId.getId());

        // check if the constructor was called with the correct arguments
        ArgumentCaptor<UserDeletedEvent> argumentCaptor = ArgumentCaptor.forClass(UserDeletedEvent.class);
        verify(eventPublisher).publishEvent(argumentCaptor.capture());
        assertEquals(userWithId.getId(), argumentCaptor.getValue().getUserId());
        assertEquals(userWithId.getName(), argumentCaptor.getValue().getName());
        assertEquals(userWithId.getEmail(), argumentCaptor.getValue().getEmailAddress());
    }
}
