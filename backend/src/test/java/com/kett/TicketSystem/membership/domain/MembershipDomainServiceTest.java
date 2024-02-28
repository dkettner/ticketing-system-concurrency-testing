package com.kett.TicketSystem.membership.domain;

import com.kett.TicketSystem.common.domainprimitives.DomainEvent;
import com.kett.TicketSystem.common.domainprimitives.EmailAddress;
import com.kett.TicketSystem.common.exceptions.IllegalStateUpdateException;
import com.kett.TicketSystem.common.exceptions.ImpossibleException;
import com.kett.TicketSystem.common.exceptions.NoProjectFoundException;
import com.kett.TicketSystem.common.exceptions.NoUserFoundException;
import com.kett.TicketSystem.membership.domain.consumedData.ProjectDataOfMembership;
import com.kett.TicketSystem.membership.domain.consumedData.UserDataOfMembership;
import com.kett.TicketSystem.membership.domain.events.LastProjectMemberDeletedEvent;
import com.kett.TicketSystem.membership.domain.events.MembershipAcceptedEvent;
import com.kett.TicketSystem.membership.domain.events.MembershipDeletedEvent;
import com.kett.TicketSystem.membership.domain.events.UnacceptedProjectMembershipCreatedEvent;
import com.kett.TicketSystem.membership.domain.exceptions.AlreadyLastAdminException;
import com.kett.TicketSystem.membership.domain.exceptions.MembershipAlreadyExistsException;
import com.kett.TicketSystem.membership.domain.exceptions.NoMembershipFoundException;
import com.kett.TicketSystem.membership.repository.MembershipRepository;
import com.kett.TicketSystem.membership.repository.ProjectDataOfMembershipRepository;
import com.kett.TicketSystem.membership.repository.UserDataOfMembershipRepository;
import com.kett.TicketSystem.project.domain.events.DefaultProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectDeletedEvent;
import com.kett.TicketSystem.user.domain.events.UserCreatedEvent;
import com.kett.TicketSystem.user.domain.events.UserDeletedEvent;
import com.kett.TicketSystem.user.domain.events.UserPatchedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MembershipDomainServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserDataOfMembershipRepository userDataOfMembershipRepository;

    @Mock
    private ProjectDataOfMembershipRepository projectDataOfMembershipRepository;

    @InjectMocks
    private MembershipDomainService membershipDomainService;

    @BeforeEach
    public void buildUp() {
        lenient()
                .when(membershipRepository.save(any(Membership.class)))
                .thenAnswer(invocation -> {
                    Membership argument = invocation.getArgument(0);
                    if (argument.getId() == null) {
                        argument.setId(UUID.randomUUID());
                    }
                    return argument;
                });
    }

    @Test
    public void testAddNewMembership() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Membership membership = new Membership(projectId, userId, Role.MEMBER);

        when(userDataOfMembershipRepository.existsByUserId(userId))
                .thenReturn(true);
        when(projectDataOfMembershipRepository.existsByProjectId(projectId))
                .thenReturn(true);
        when(membershipRepository.existsByUserIdAndProjectId(userId, projectId))
                .thenReturn(false);

        // Act
        membershipDomainService.addNewMembership(membership);

        // Assert
        verify(membershipRepository, times(1)).save(membership);

        // verify event
        ArgumentCaptor<UnacceptedProjectMembershipCreatedEvent> argumentCaptor =
                ArgumentCaptor.forClass(UnacceptedProjectMembershipCreatedEvent.class);

        verify(eventPublisher, times(1)).publishEvent(argumentCaptor.capture());
        assertEquals(membership.getId(), argumentCaptor.getValue().getMembershipId());
        assertEquals(userId, argumentCaptor.getValue().getInviteeId());
        assertEquals(projectId, argumentCaptor.getValue().getProjectId());
    }

    @Test
    public void testAddNewMembershipWithUserNotExisting() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Membership membership = new Membership(projectId, userId, Role.MEMBER);

        when(userDataOfMembershipRepository.existsByUserId(userId))
                .thenReturn(false);

        // Act & Assert
        assertThrows(NoUserFoundException.class, () -> membershipDomainService.addNewMembership(membership));
        verify(membershipRepository, times(0)).save(membership);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testAddNewMembershipWithProjectNotExisting() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Membership membership = new Membership(projectId, userId, Role.MEMBER);

        when(userDataOfMembershipRepository.existsByUserId(userId))
                .thenReturn(true);
        when(projectDataOfMembershipRepository.existsByProjectId(projectId))
                .thenReturn(false);

        // Act & Assert
        assertThrows(NoProjectFoundException.class, () -> membershipDomainService.addNewMembership(membership));
        verify(membershipRepository, times(0)).save(membership);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testAddNewMembershipWithMembershipExisting() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Membership membership = new Membership(projectId, userId, Role.MEMBER);

        when(userDataOfMembershipRepository.existsByUserId(userId))
                .thenReturn(true);
        when(projectDataOfMembershipRepository.existsByProjectId(projectId))
                .thenReturn(true);
        when(membershipRepository.existsByUserIdAndProjectId(userId, projectId))
                .thenReturn(true);

        // Act & Assert
        assertThrows(MembershipAlreadyExistsException.class, () -> membershipDomainService.addNewMembership(membership));
        verify(membershipRepository, times(0)).save(membership);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testGetMembershipById() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        Membership membership = new Membership(UUID.randomUUID(), UUID.randomUUID(), Role.MEMBER);
        membership.setId(membershipId);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.of(membership));

        // Act
        Membership result = membershipDomainService.getMembershipById(membershipId);

        // Assert
        assertEquals(membership, result);
        verify(membershipRepository, times(1)).findById(membershipId);
    }

    @Test
    public void testGetMembershipByIdWithNoMembershipFound() {
        // Arrange
        UUID membershipId = UUID.randomUUID();

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoMembershipFoundException.class, () -> membershipDomainService.getMembershipById(membershipId));
        verify(membershipRepository, times(1)).findById(membershipId);
    }

    @Test
    public void testGetMembershipsByUserId() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Membership membership1 = new Membership(UUID.randomUUID(), userId, Role.MEMBER);
        Membership membership2 = new Membership(UUID.randomUUID(), userId, Role.MEMBER);

        when(membershipRepository.findByUserId(userId))
                .thenReturn(List.of(membership1, membership2));

        // Act
        List<Membership> result = membershipDomainService.getMembershipsByUserId(userId);

        // Assert
        assertEquals(List.of(membership1, membership2), result);
        verify(membershipRepository, times(1)).findByUserId(userId);
    }

    @Test
    public void testGetMembershipsByUserIdNoMembershipFound() {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(membershipRepository.findByUserId(userId))
                .thenReturn(List.of());

        // Act & Assert
        assertThrows(NoMembershipFoundException.class, () ->  membershipDomainService.getMembershipsByUserId(userId));
        verify(membershipRepository, times(1)).findByUserId(userId);
    }

    @Test
    public void testGetMembershipsByUserEmail() {
        // Arrange
        EmailAddress emailAddress = EmailAddress.fromString("some@mail.com");
        UUID userId = UUID.randomUUID();
        Membership membership1 = new Membership(UUID.randomUUID(), userId, Role.MEMBER);
        Membership membership2 = new Membership(UUID.randomUUID(), userId, Role.MEMBER);

        when(userDataOfMembershipRepository.findByUserEmailEquals(emailAddress))
                .thenReturn(List.of(new UserDataOfMembership(userId, emailAddress)));
        when(membershipRepository.findByUserId(userId))
                .thenReturn(List.of(membership1, membership2));

        // Act
        List<Membership> result = membershipDomainService.getMembershipsByUserEmail(emailAddress);

        // Assert
        assertEquals(List.of(membership1, membership2), result);
        verify(userDataOfMembershipRepository, times(1)).findByUserEmailEquals(emailAddress);
        verify(membershipRepository, times(1)).findByUserId(userId);
    }

    @Test
    public void testGetMembershipsByUserEmailNoMembershipFound() {
        // Arrange
        EmailAddress emailAddress = EmailAddress.fromString("some@mail.com");
        UserDataOfMembership userDataOfMembership = new UserDataOfMembership(UUID.randomUUID(), emailAddress);

        when(userDataOfMembershipRepository.findByUserEmailEquals(emailAddress))
                .thenReturn(List.of(userDataOfMembership));

        // Act & Assert
        assertThrows(NoMembershipFoundException.class,
                () ->  membershipDomainService.getMembershipsByUserEmail(emailAddress));
        verify(userDataOfMembershipRepository, times(1)).findByUserEmailEquals(emailAddress);
    }

    @Test
    public void testGetProjectAuthoritiesByUserId() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Membership membership1 = new Membership(UUID.randomUUID(), userId, Role.MEMBER);
        Membership membership2 = new Membership(UUID.randomUUID(), userId, Role.MEMBER);

        when(membershipRepository.findByUserIdAndStateEquals(userId, State.ACCEPTED))
                .thenReturn(List.of(membership1, membership2));

        // Act
        List<GrantedAuthority> result = membershipDomainService.getProjectAuthoritiesByUserId(userId);

        // Assert
        assertEquals(List.of(membership1, membership2), result);
        verify(membershipRepository, times(1)).findByUserIdAndStateEquals(userId, State.ACCEPTED);
    }

    @Test
    public void testGetProjectAuthoritiesByUserIdNoMembershipFound() {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(membershipRepository.findByUserIdAndStateEquals(userId, State.ACCEPTED))
                .thenReturn(List.of());

        // Act
        List<GrantedAuthority> result = membershipDomainService.getProjectAuthoritiesByUserId(userId);

        // Assert
        assertEquals(List.of(), result);
        verify(membershipRepository, times(1)).findByUserIdAndStateEquals(userId, State.ACCEPTED);
    }

    @Test
    public void testGetProjectAuthoritiesByUserIdOnlyUnacceptedMemberships() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Membership membership1 = new Membership(UUID.randomUUID(), userId, Role.MEMBER);
        Membership membership2 = new Membership(UUID.randomUUID(), userId, Role.MEMBER);

        when(membershipRepository.findByUserIdAndStateEquals(userId, State.ACCEPTED))
                .thenReturn(List.of(membership1, membership2));

        // Act
        List<GrantedAuthority> result = membershipDomainService.getProjectAuthoritiesByUserId(userId);

        // Assert
        assertEquals(List.of(membership1, membership2), result);
        verify(membershipRepository, times(1)).findByUserIdAndStateEquals(userId, State.ACCEPTED);
    }

    @Test
    public void testGetMembershipsByProjectId() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        Membership membership1 = new Membership(projectId, UUID.randomUUID(), Role.MEMBER);
        Membership membership2 = new Membership(projectId, UUID.randomUUID(), Role.MEMBER);

        when(membershipRepository.findByProjectId(projectId))
                .thenReturn(List.of(membership1, membership2));

        // Act
        List<Membership> result = membershipDomainService.getMembershipsByProjectId(projectId);

        // Assert
        assertEquals(List.of(membership1, membership2), result);
        verify(membershipRepository, times(1)).findByProjectId(projectId);
    }

    @Test
    public void testGetMembershipsByProjectIdNoMembershipFound() {
        // Arrange
        UUID projectId = UUID.randomUUID();

        when(membershipRepository.findByProjectId(projectId))
                .thenReturn(List.of());

        // Act & Assert
        assertThrows(NoMembershipFoundException.class,
                () ->  membershipDomainService.getMembershipsByProjectId(projectId));
        verify(membershipRepository, times(1)).findByProjectId(projectId);
    }

    @Test
    public void testGetUserIdByMembershipId() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Membership membership = new Membership(UUID.randomUUID(), userId, Role.MEMBER);
        membership.setId(membershipId);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.of(membership));

        // Act
        UUID result = membershipDomainService.getUserIdByMembershipId(membershipId);

        // Assert
        assertEquals(userId, result);
        verify(membershipRepository, times(1)).findById(membershipId);
    }

    @Test
    public void testGetUserIdByMembershipIdWithNoMembershipFound() {
        // Arrange
        UUID membershipId = UUID.randomUUID();

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoMembershipFoundException.class,
                () -> membershipDomainService.getUserIdByMembershipId(membershipId));
        verify(membershipRepository, times(1)).findById(membershipId);
    }

    @Test
    public void testGetProjectIdByMembershipId() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Membership membership = new Membership(projectId, UUID.randomUUID(), Role.MEMBER);
        membership.setId(membershipId);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.of(membership));

        // Act
        UUID result = membershipDomainService.getProjectIdByMembershipId(membershipId);

        // Assert
        assertEquals(projectId, result);
        verify(membershipRepository, times(1)).findById(membershipId);
    }

    @Test
    public void testGetProjectIdByMembershipIdWithNoMembershipFound() {
        // Arrange
        UUID membershipId = UUID.randomUUID();

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoMembershipFoundException.class,
                () -> membershipDomainService.getProjectIdByMembershipId(membershipId));
        verify(membershipRepository, times(1)).findById(membershipId);
    }

    @Test
    public void testGetUserIdByUserEmailAddress() {
        // Arrange
        EmailAddress emailAddress = EmailAddress.fromString("some@mail.com");
        UUID userId = UUID.randomUUID();

        when(userDataOfMembershipRepository.findByUserEmailEquals(emailAddress))
                .thenReturn(List.of(new UserDataOfMembership(userId, emailAddress)));

        // Act
        UUID result = membershipDomainService.getUserIdByUserEmailAddress(emailAddress);

        // Assert
        assertEquals(userId, result);
        verify(userDataOfMembershipRepository, times(1)).findByUserEmailEquals(emailAddress);
    }

    @Test
    public void testGetUserIdByUserEmailAddressWithNoUserFound() {
        // Arrange
        EmailAddress emailAddress = EmailAddress.fromString("some@mail.com");

        when(userDataOfMembershipRepository.findByUserEmailEquals(emailAddress))
                .thenReturn(List.of());

        // Act & Assert
        assertThrows(ImpossibleException.class,
                () -> membershipDomainService.getUserIdByUserEmailAddress(emailAddress));
        verify(userDataOfMembershipRepository, times(1)).findByUserEmailEquals(emailAddress);
    }

    @Test
    public void testGetUserIdByUserEmailNoUserFound() {
        // Arrange
        EmailAddress emailAddress = EmailAddress.fromString("some@mail.com");

        when(userDataOfMembershipRepository.findByUserEmailEquals(emailAddress))
                .thenReturn(List.of());

        // Act & Assert
        assertThrows(ImpossibleException.class,
                () -> membershipDomainService.getUserIdByUserEmailAddress(emailAddress));
        verify(userDataOfMembershipRepository, times(1)).findByUserEmailEquals(emailAddress);
    }

    @Test
    public void testUpdateMemberShipState() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        Membership membership = new Membership(UUID.randomUUID(), UUID.randomUUID(), Role.MEMBER);
        membership.setId(membershipId);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.of(membership));

        // Act
        membershipDomainService.updateMemberShipState(membershipId, State.ACCEPTED);

        // Assert
        verify(membershipRepository, times(1)).save(membership);
        assertEquals(State.ACCEPTED, membership.getState());

        // verify event
        ArgumentCaptor<MembershipAcceptedEvent> argumentCaptor =
                ArgumentCaptor.forClass(MembershipAcceptedEvent.class);

        verify(eventPublisher, times(1)).publishEvent(argumentCaptor.capture());
        assertEquals(membership.getId(), argumentCaptor.getValue().getMembershipId());
        assertEquals(membership.getProjectId(), argumentCaptor.getValue().getProjectId());
        assertEquals(membership.getUserId(), argumentCaptor.getValue().getUserId());
    }

    @Test
    public void testUpdateMemberShipStateWithNoMembershipFound() {
        // Arrange
        UUID membershipId = UUID.randomUUID();

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoMembershipFoundException.class,
                () -> membershipDomainService.updateMemberShipState(membershipId, State.ACCEPTED));
        verify(membershipRepository, times(0)).save(any(Membership.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testUpdateMemberShipStateWithMembershipAlreadyAccepted() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        Membership membership = new Membership(UUID.randomUUID(), UUID.randomUUID(), Role.MEMBER);
        membership.setId(membershipId);
        membership.setState(State.ACCEPTED);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.of(membership));

        // Act & Assert
        assertThrows(IllegalStateUpdateException.class,
                () -> membershipDomainService.updateMemberShipState(membershipId, State.ACCEPTED));
        verify(membershipRepository, times(0)).save(membership);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testUpdateMembershipRole() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        Membership membership = new Membership(UUID.randomUUID(), UUID.randomUUID(), Role.MEMBER);
        membership.setId(membershipId);
        membership.setState(State.ACCEPTED);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.of(membership));
        when(membershipRepository.countMembershipByProjectIdAndStateEqualsAndRoleEquals(membership.getProjectId(), State.ACCEPTED, Role.ADMIN))
                .thenReturn(1);

        // Act
        membershipDomainService.updateMembershipRole(membershipId, Role.ADMIN);

        // Assert
        assertEquals(Role.ADMIN, membership.getRole());
        verify(membershipRepository, times(1)).save(membership);
        verify(membershipRepository, times(1)).countMembershipByProjectIdAndStateEqualsAndRoleEquals(membership.getProjectId(), State.ACCEPTED, Role.ADMIN);
    }

    @Test
    public void testUpdateMembershipRoleNoUserFound() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        Membership membership = new Membership(UUID.randomUUID(), UUID.randomUUID(), Role.MEMBER);
        membership.setId(membershipId);
        membership.setState(State.ACCEPTED);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoMembershipFoundException.class,
                () -> membershipDomainService.updateMembershipRole(membershipId, Role.ADMIN));
        verify(membershipRepository, times(0)).save(membership);
        verify(membershipRepository, times(0)).countMembershipByProjectIdAndStateEqualsAndRoleEquals(membership.getProjectId(), State.ACCEPTED, Role.ADMIN);
    }

    @Test
    public void testUpdateMembershipRoleToMemberWithNoAdminsLeft() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        Membership membership = new Membership(UUID.randomUUID(), UUID.randomUUID(), Role.ADMIN);
        membership.setId(membershipId);
        membership.setState(State.ACCEPTED);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.of(membership));
        when(membershipRepository.countMembershipByProjectIdAndStateEqualsAndRoleEquals(membership.getProjectId(), State.ACCEPTED, Role.ADMIN))
                .thenReturn(1);

        // Act & Assert
        assertThrows(AlreadyLastAdminException.class, () -> membershipDomainService.updateMembershipRole(membershipId, Role.MEMBER));
        verify(membershipRepository, times(0)).save(membership);
        verify(membershipRepository, times(1)).countMembershipByProjectIdAndStateEqualsAndRoleEquals(membership.getProjectId(), State.ACCEPTED, Role.ADMIN);
    }

    @Test
    public void testDeleteMembership() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        Membership membership = new Membership(UUID.randomUUID(), UUID.randomUUID(), Role.MEMBER);
        membership.setId(membershipId);
        membership.setState(State.ACCEPTED);

        Membership remainingAdmin = new Membership(membership.getProjectId(), UUID.randomUUID(), Role.ADMIN);
        remainingAdmin.setState(State.ACCEPTED);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.of(membership));
        when(membershipRepository.removeById(membershipId))
                .thenReturn(1L);
        when(membershipRepository.findByProjectIdAndStateEquals(membership.getProjectId(), State.ACCEPTED))
                .thenReturn(List.of(membership, remainingAdmin));

        // Act
        membershipDomainService.deleteMembershipById(membershipId);

        // Assert
        verify(membershipRepository, times(1)).removeById(membershipId);

        // verify event
        ArgumentCaptor<MembershipDeletedEvent> argumentCaptor =
                ArgumentCaptor.forClass(MembershipDeletedEvent.class);

        verify(eventPublisher, times(1)).publishEvent(argumentCaptor.capture());
        assertEquals(membership.getId(), argumentCaptor.getValue().getMembershipId());
        assertEquals(membership.getProjectId(), argumentCaptor.getValue().getProjectId());
        assertEquals(membership.getUserId(), argumentCaptor.getValue().getUserId());
    }

    @Test
    public void testDeleteMembershipWithNoMembershipFound() {
        // Arrange
        UUID membershipId = UUID.randomUUID();

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(NoMembershipFoundException.class, () -> membershipDomainService.deleteMembershipById(membershipId));
        verify(membershipRepository, times(0)).removeById(membershipId);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testDeleteMembershipWithNoMembersLeft() {
        // Arrange
        UUID membershipId = UUID.randomUUID();
        Membership membership = new Membership(UUID.randomUUID(), UUID.randomUUID(), Role.MEMBER);
        membership.setId(membershipId);
        membership.setState(State.ACCEPTED);

        when(membershipRepository.findById(membershipId))
                .thenReturn(java.util.Optional.of(membership));
        when(membershipRepository.removeById(membershipId))
                .thenReturn(1L);
        when(membershipRepository.findByProjectIdAndStateEquals(membership.getProjectId(), State.ACCEPTED))
                .thenReturn(List.of());

        // Act
        membershipDomainService.deleteMembershipById(membershipId);

        // Assert
        verify(membershipRepository, times(1)).removeById(membershipId);
        verify(membershipRepository, times(1)).findByProjectIdAndStateEquals(membership.getProjectId(), State.ACCEPTED);

        // verify event
        ArgumentCaptor<DomainEvent> argumentCaptor =
                ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(argumentCaptor.capture());

        assertEquals(membership.getId(), ((MembershipDeletedEvent) argumentCaptor.getAllValues().get(0)).getMembershipId());
        assertEquals(membership.getProjectId(), ((MembershipDeletedEvent) argumentCaptor.getAllValues().get(0)).getProjectId());
        assertEquals(membership.getUserId(), ((MembershipDeletedEvent) argumentCaptor.getAllValues().get(0)).getUserId());

        assertEquals(membership.getId(), ((LastProjectMemberDeletedEvent) argumentCaptor.getAllValues().get(1)).getMembershipId());
        assertEquals(membership.getProjectId(), ((LastProjectMemberDeletedEvent) argumentCaptor.getAllValues().get(1)).getProjectId());
        assertEquals(membership.getUserId(), ((LastProjectMemberDeletedEvent) argumentCaptor.getAllValues().get(1)).getUserId());
    }

    @Test
    public void testDeleteMembershipWithNoAdminsLeft() {
        // Arrange
        UUID membershipIdOfLastAdmin = UUID.randomUUID();
        Membership membershipOfLastAdmin = new Membership(UUID.randomUUID(), UUID.randomUUID(), Role.ADMIN);
        membershipOfLastAdmin.setId(membershipIdOfLastAdmin);
        membershipOfLastAdmin.setState(State.ACCEPTED);

        UUID membershipIdOfMember = UUID.randomUUID();
        Membership membershipOfMember = new Membership(membershipOfLastAdmin.getProjectId(), UUID.randomUUID(), Role.MEMBER);
        membershipOfMember.setId(membershipIdOfMember);
        membershipOfMember.setState(State.ACCEPTED);

        when(membershipRepository.findById(membershipIdOfLastAdmin))
                .thenReturn(java.util.Optional.of(membershipOfLastAdmin));
        when(membershipRepository.findByProjectId(membershipOfLastAdmin.getProjectId()))
                .thenReturn(List.of(membershipOfMember));
        when(membershipRepository.removeById(membershipIdOfLastAdmin))
                .thenReturn(1L);
        when(membershipRepository.findByProjectIdAndStateEquals(membershipOfLastAdmin.getProjectId(), State.ACCEPTED))
                .thenReturn(List.of(membershipOfMember));

        // Act
        membershipDomainService.deleteMembershipById(membershipIdOfLastAdmin);

        // Assert
        assertEquals(Role.ADMIN, membershipOfLastAdmin.getRole());
        verify(membershipRepository, times(1)).save(membershipOfMember);
        verify(membershipRepository, times(1)).removeById(membershipIdOfLastAdmin);
        verify(membershipRepository, times(2)).findByProjectIdAndStateEquals(membershipOfLastAdmin.getProjectId(), State.ACCEPTED);

        // verify events
        ArgumentCaptor<MembershipDeletedEvent> argumentCaptor =
                ArgumentCaptor.forClass(MembershipDeletedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(argumentCaptor.capture());

        assertEquals(membershipOfLastAdmin.getId(), argumentCaptor.getValue().getMembershipId());
        assertEquals(membershipOfLastAdmin.getProjectId(), argumentCaptor.getValue().getProjectId());
        assertEquals(membershipOfLastAdmin.getUserId(), argumentCaptor.getValue().getUserId());
    }

    // event listeners

    @Test
    public void testHandleProjectCreatedEvent() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectCreatedEvent projectCreatedEvent = new ProjectCreatedEvent(projectId, userId);

        when(projectDataOfMembershipRepository.existsByProjectId(projectId))
                .thenReturn(true);

        // Act
        membershipDomainService.handleProjectCreatedEvent(projectCreatedEvent);

        // Assert
        ArgumentCaptor<ProjectDataOfMembership> argumentCaptor =
                ArgumentCaptor.forClass(ProjectDataOfMembership.class);
        verify(projectDataOfMembershipRepository, times(1)).save(argumentCaptor.capture());
        assertEquals(projectId, argumentCaptor.getValue().getProjectId());
    }

    @Test
    public void testHandleDefaultProjectCreatedEvent() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DefaultProjectCreatedEvent defaultProjectCreatedEvent = new DefaultProjectCreatedEvent(projectId, userId);

        when(projectDataOfMembershipRepository.existsByProjectId(projectId))
                .thenReturn(true);

        // Act
        membershipDomainService.handleDefaultProjectCreatedEvent(defaultProjectCreatedEvent);

        // Assert
        ArgumentCaptor<ProjectDataOfMembership> argumentCaptor =
                ArgumentCaptor.forClass(ProjectDataOfMembership.class);
        verify(projectDataOfMembershipRepository, times(1)).save(argumentCaptor.capture());
        assertEquals(projectId, argumentCaptor.getValue().getProjectId());
    }

    @Test
    public void testHandleProjectDeletedEvent() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        Membership membership1 = new Membership(projectId, UUID.randomUUID(), Role.MEMBER);
        Membership membership2 = new Membership(projectId, UUID.randomUUID(), Role.MEMBER);
        ProjectDeletedEvent projectDeletedEvent = new ProjectDeletedEvent(projectId);

        when(membershipRepository.deleteByProjectId(projectId))
                .thenReturn(List.of(membership1, membership2));

        // Act
        membershipDomainService.handleProjectDeletedEvent(projectDeletedEvent);

        // Assert
        verify(membershipRepository, times(1)).deleteByProjectId(projectId);
        verify(projectDataOfMembershipRepository, times(1)).deleteByProjectId(projectId);

        // verify events
        ArgumentCaptor<MembershipDeletedEvent> argumentCaptor =
                ArgumentCaptor.forClass(MembershipDeletedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(argumentCaptor.capture());

        assertEquals(membership1.getId(), argumentCaptor.getAllValues().get(0).getMembershipId());
        assertEquals(membership1.getProjectId(), argumentCaptor.getAllValues().get(0).getProjectId());
        assertEquals(membership1.getUserId(), argumentCaptor.getAllValues().get(0).getUserId());

        assertEquals(membership2.getId(), argumentCaptor.getAllValues().get(1).getMembershipId());
        assertEquals(membership2.getProjectId(), argumentCaptor.getAllValues().get(1).getProjectId());
        assertEquals(membership2.getUserId(), argumentCaptor.getAllValues().get(1).getUserId());
    }

    @Test
    public void testHandleUserCreatedEvent() {
        // Arrange
        UUID userId = UUID.randomUUID();
        EmailAddress emailAddress = EmailAddress.fromString("some@mail.com");
        String username = "someUsername";

        when(userDataOfMembershipRepository.existsByUserId(userId))
                .thenReturn(false);

        // Act
        membershipDomainService.handleUserCreatedEvent(new UserCreatedEvent(userId, username, emailAddress));

        // Assert
        ArgumentCaptor<UserDataOfMembership> argumentCaptor =
                ArgumentCaptor.forClass(UserDataOfMembership.class);
        verify(userDataOfMembershipRepository, times(1)).save(argumentCaptor.capture());
        assertEquals(userId, argumentCaptor.getValue().getUserId());
        assertEquals(emailAddress, argumentCaptor.getValue().getUserEmail());
    }

    @Test
    public void testHandleUserPatchedEvent() {
        // Arrange
        UUID userId = UUID.randomUUID();
        EmailAddress emailAddress = EmailAddress.fromString("some@mail.com");
        EmailAddress newEmailAddress = EmailAddress.fromString("new@mail.com");
        UserDataOfMembership userDataOfMembership = new UserDataOfMembership(userId, emailAddress);
        UserPatchedEvent userPatchedEvent = new UserPatchedEvent(userId, "someName", newEmailAddress);

        when(userDataOfMembershipRepository.findByUserId(userId))
                .thenReturn(List.of(userDataOfMembership));

        // Act
        membershipDomainService.handleUserPatchedEvent(userPatchedEvent);

        // Assert
        ArgumentCaptor<UserDataOfMembership> argumentCaptor =
                ArgumentCaptor.forClass(UserDataOfMembership.class);
        verify(userDataOfMembershipRepository, times(1)).save(argumentCaptor.capture());
        assertEquals(userId, argumentCaptor.getValue().getUserId());
        assertEquals(newEmailAddress, argumentCaptor.getValue().getUserEmail());
    }

    @Test
    public void testHandleUserDeletedEvent() {
        // Arrange
        UUID userId = UUID.randomUUID();
        EmailAddress emailAddress = EmailAddress.fromString("some@mail.com");
        Membership membership1 = new Membership(UUID.randomUUID(), userId, Role.MEMBER);
        UserDataOfMembership userDataOfMembership = new UserDataOfMembership(userId, emailAddress);
        UserDeletedEvent userDeletedEvent = new UserDeletedEvent(userId, "someName", emailAddress);

        when(membershipRepository.findByUserId(userId))
                .thenReturn(List.of(membership1));
        when(membershipRepository.findById(membership1.getId()))
                .thenReturn(java.util.Optional.of(membership1));
        when(membershipRepository.removeById(membership1.getId()))
                .thenReturn(1L);

        // Act
        membershipDomainService.handleUserDeletedEvent(userDeletedEvent);

        // Assert
        verify(membershipRepository, times(1)).removeById(membership1.getId());
        verify(userDataOfMembershipRepository, times(1)).deleteByUserId(userId);

        // verify event
        ArgumentCaptor<DomainEvent> argumentCaptor =
                ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(3)).publishEvent(argumentCaptor.capture());

        assertEquals(membership1.getId(), ((MembershipDeletedEvent) argumentCaptor.getAllValues().get(0)).getMembershipId());
        assertEquals(membership1.getProjectId(), ((MembershipDeletedEvent) argumentCaptor.getAllValues().get(0)).getProjectId());
        assertEquals(membership1.getUserId(), ((MembershipDeletedEvent) argumentCaptor.getAllValues().get(0)).getUserId());

        assertEquals(membership1.getId(), ((LastProjectMemberDeletedEvent) argumentCaptor.getAllValues().get(1)).getMembershipId());
        assertEquals(membership1.getProjectId(), ((LastProjectMemberDeletedEvent) argumentCaptor.getAllValues().get(1)).getProjectId());
        assertEquals(membership1.getUserId(), ((LastProjectMemberDeletedEvent) argumentCaptor.getAllValues().get(1)).getUserId());
    }
}
