package com.kett.TicketSystem.ticket.domain;

import com.kett.TicketSystem.common.domainprimitives.DomainEvent;
import com.kett.TicketSystem.common.domainprimitives.EmailAddress;
import com.kett.TicketSystem.common.exceptions.ImpossibleException;
import com.kett.TicketSystem.common.exceptions.InvalidProjectMembersException;
import com.kett.TicketSystem.common.exceptions.NoProjectFoundException;
import com.kett.TicketSystem.common.exceptions.UnrelatedPhaseException;
import com.kett.TicketSystem.membership.domain.events.MembershipAcceptedEvent;
import com.kett.TicketSystem.membership.domain.events.MembershipDeletedEvent;
import com.kett.TicketSystem.phase.domain.events.PhaseCreatedEvent;
import com.kett.TicketSystem.phase.domain.events.PhaseDeletedEvent;
import com.kett.TicketSystem.phase.domain.events.PhasePositionUpdatedEvent;
import com.kett.TicketSystem.project.domain.events.DefaultProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectDeletedEvent;
import com.kett.TicketSystem.ticket.domain.consumedData.MembershipDataOfTicket;
import com.kett.TicketSystem.ticket.domain.consumedData.PhaseDataOfTicket;
import com.kett.TicketSystem.ticket.domain.consumedData.ProjectDataOfTicket;
import com.kett.TicketSystem.ticket.domain.consumedData.UserDataOfTicket;
import com.kett.TicketSystem.ticket.domain.events.*;
import com.kett.TicketSystem.ticket.domain.exceptions.NoTicketFoundException;
import com.kett.TicketSystem.ticket.domain.exceptions.TicketException;
import com.kett.TicketSystem.ticket.repository.*;
import com.kett.TicketSystem.user.domain.events.UserCreatedEvent;
import com.kett.TicketSystem.user.domain.events.UserDeletedEvent;
import com.kett.TicketSystem.user.domain.events.UserPatchedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class TicketDomainServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ProjectDataOfTicketRepository projectDataOfTicketRepository;

    @Mock
    private MembershipDataOfTicketRepository membershipDataOfTicketRepository;

    @Mock
    private PhaseDataOfTicketRepository phaseDataOfTicketRepository;

    @Mock
    private UserDataOfTicketRepository userDataOfTicketRepository;

    @InjectMocks
    private TicketDomainService ticketDomainService;

    private UUID user0Id;
    private EmailAddress user0Email;
    private UserDataOfTicket userDataOfTicket0;

    private String title0;
    private String description0;
    private LocalDateTime dueTime0;
    private UUID projectId0;
    private Ticket ticket0;

    private UUID phaseId0;
    private PhaseDataOfTicket phaseDataOfTicket0;
    private UUID phaseId1;
    private PhaseDataOfTicket phaseDataOfTicket1;

    @BeforeEach
    public void buildUp() {
        user0Id = UUID.randomUUID();
        user0Email = EmailAddress.fromString("user0@mail.com");
        userDataOfTicket0 = new UserDataOfTicket(user0Id, user0Email);

        title0 = "Test Ticket";
        description0 = "Test Description";
        dueTime0 = LocalDateTime.now().plusDays(7);
        projectId0 = UUID.randomUUID();
        ticket0 = new Ticket(title0, description0, dueTime0, projectId0, null, List.of(user0Id));

        phaseId0 = UUID.randomUUID();
        phaseDataOfTicket0 = new PhaseDataOfTicket(phaseId0, null, projectId0);
        phaseId1 = UUID.randomUUID();
        phaseDataOfTicket1 = new PhaseDataOfTicket(phaseId1, phaseId0, projectId0);

        lenient()
                .when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> {
                    Ticket ticket = invocation.getArgument(0);
                    if (ticket.getId() == null) {
                        ticket.setId(UUID.randomUUID());
                    }
                    return ticket;
                });
        lenient()
                .when(ticketRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    if (id.equals(ticket0.getId())) {
                        return java.util.Optional.of(ticket0);
                    }
                    return java.util.Optional.empty();
                });
        lenient()
                .when(projectDataOfTicketRepository.existsByProjectId(projectId0))
                .thenReturn(true);
        lenient()
                .when(membershipDataOfTicketRepository.existsByUserIdAndProjectId(user0Id, projectId0))
                .thenReturn(true);
        lenient()
                .when(userDataOfTicketRepository.findByUserEmailEquals(user0Email))
                .thenReturn(List.of(userDataOfTicket0));
        lenient()
                .when(phaseDataOfTicketRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    if (id.equals(phaseId0)) {
                        return java.util.Optional.of(phaseDataOfTicket0);
                    }
                    if (id.equals(phaseId1)) {
                        return java.util.Optional.of(phaseDataOfTicket1);
                    }
                    return java.util.Optional.empty();
                });
        lenient()
                .when(phaseDataOfTicketRepository.findByPhaseId(phaseId0))
                .thenReturn(List.of(phaseDataOfTicket0));
        lenient()
                .when(phaseDataOfTicketRepository.findByPhaseId(phaseId1))
                .thenReturn(List.of(phaseDataOfTicket1));
        lenient()
                .when(phaseDataOfTicketRepository.findByProjectIdAndPreviousPhaseIdIsNull(projectId0))
                .thenReturn(List.of(phaseDataOfTicket0));
    }

    @AfterEach
    public void tearDown() {
        user0Id = null;
        user0Email = null;
        userDataOfTicket0 = null;

        phaseId0 = null;
        phaseDataOfTicket0 = null;
        phaseId1 = null;
        phaseDataOfTicket1 = null;

        title0 = null;
        description0 = null;
        dueTime0 = null;
        projectId0 = null;
        ticket0 = null;
    }

    @Test
    public void testAddTicket() {
        // Arrange
        // nothing to arrange

        // Act
        ticketDomainService.addTicket(ticket0, user0Email);

        // Assert
        assertEquals(projectId0, ticket0.getProjectId());
        assertEquals(phaseId0, ticket0.getPhaseId());
        assertTrue(ticket0.isAssignee(user0Id));

        verify(ticketRepository, times(1)).save(ticket0);

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<DomainEvent> events = eventCaptor.getAllValues();

        TicketCreatedEvent ticketCreatedEvent = (TicketCreatedEvent) events.get(0);
        assertEquals(ticket0.getId(), ticketCreatedEvent.getTicketId());
        assertEquals(ticket0.getProjectId(), ticketCreatedEvent.getProjectId());
        assertEquals(user0Id, ticketCreatedEvent.getUserId());

        TicketAssignedEvent ticketAssignedEvent = (TicketAssignedEvent) events.get(1);
        assertEquals(ticket0.getId(), ticketAssignedEvent.getTicketId());
        assertEquals(user0Id, ticketAssignedEvent.getAssigneeId());
        assertEquals(user0Id, ticketAssignedEvent.getAssigneeId());
    }

    @Test
    public void testAddTicketWithInvalidUser() {
        // Arrange
        when(userDataOfTicketRepository.findByUserEmailEquals(user0Email))
                .thenReturn(new ArrayList<>());

        // Act
        assertThrows(ImpossibleException.class, () -> ticketDomainService.addTicket(ticket0, user0Email));

        // Assert
        verify(ticketRepository, times(0)).save(ticket0);
        verify(eventPublisher, times(0)).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testAddTicketProjectNotFound() {
        // Arrange
        when(projectDataOfTicketRepository.existsByProjectId(projectId0))
                .thenReturn(false);

        // Act
        assertThrows(NoProjectFoundException.class, () -> ticketDomainService.addTicket(ticket0, user0Email));

        // Assert
        verify(ticketRepository, times(0)).save(ticket0);
        verify(eventPublisher, times(0)).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testAddTicketInvalidProjectMember() {
        // Arrange
        when(membershipDataOfTicketRepository.existsByUserIdAndProjectId(user0Id, projectId0))
                .thenReturn(false);

        // Act
        assertThrows(InvalidProjectMembersException.class, () -> ticketDomainService.addTicket(ticket0, user0Email));

        // Assert
        verify(ticketRepository, times(0)).save(ticket0);
        verify(eventPublisher, times(0)).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testGetTicketById() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        ticket0.setId(ticketId);

        // Act
        Ticket result = ticketDomainService.getTicketById(ticket0.getId());

        // Assert
        assertEquals(ticket0, result);

        verify(ticketRepository, times(1)).findById(ticket0.getId());
    }

    @Test
    public void testGetTicketByIdNotFound() {
        // Arrange
        UUID ticketId = UUID.randomUUID();

        // Act
        assertThrows(NoTicketFoundException.class, () -> ticketDomainService.getTicketById(ticketId));

        // Assert
        verify(ticketRepository, times(1)).findById(ticketId);
    }

    @Test
    public void testGetTicketsByPhaseId() {
        // Arrange
        Ticket anotherTicket =
                new Ticket(
                        "Another Ticket",
                        "",
                        LocalDateTime.now().plusDays(127),
                        projectId0,
                        phaseId0,
                        new ArrayList<>()
                );
        anotherTicket.setId(UUID.randomUUID());
        List<Ticket> tickets = List.of(ticket0, anotherTicket);

        when(ticketRepository.findByPhaseId(phaseId0))
                .thenReturn(tickets);

        // Act
        List<Ticket> result = ticketDomainService.getTicketsByPhaseId(phaseId0);

        // Assert
        assertEquals(tickets, result);

        verify(ticketRepository, times(1)).findByPhaseId(phaseId0);
    }

    @Test
    public void testGetTicketsByPhaseIdNotFound() {
        // Arrange
        UUID phaseId = UUID.randomUUID();

        // Act
        assertThrows(NoTicketFoundException.class, () -> ticketDomainService.getTicketsByPhaseId(phaseId));

        // Assert
        verify(ticketRepository, times(1)).findByPhaseId(phaseId);
    }

    @Test
    public void testGetTicketsByProjectId() {
        // Arrange
        Ticket anotherTicket =
                new Ticket(
                        "Another Ticket",
                        "",
                        LocalDateTime.now().plusDays(127),
                        projectId0,
                        phaseId0,
                        new ArrayList<>()
                );
        anotherTicket.setId(UUID.randomUUID());
        List<Ticket> tickets = List.of(ticket0, anotherTicket);

        when(ticketRepository.findByProjectId(projectId0))
                .thenReturn(tickets);

        // Act
        List<Ticket> result = ticketDomainService.getTicketsByProjectId(projectId0);

        // Assert
        assertEquals(tickets, result);

        verify(ticketRepository, times(1)).findByProjectId(projectId0);
    }

    @Test
    public void testGetTicketsByProjectIdNotFound() {
        // Arrange
        UUID projectId = UUID.randomUUID();

        // Act
        assertThrows(NoTicketFoundException.class, () -> ticketDomainService.getTicketsByProjectId(projectId));

        // Assert
        verify(ticketRepository, times(1)).findByProjectId(projectId);
    }

    @Test
    public void testGetTicketsByAssigneeId() {
        // Arrange
        Ticket anotherTicket =
                new Ticket(
                        "Another Ticket",
                        "",
                        LocalDateTime.now().plusDays(127),
                        projectId0,
                        phaseId0,
                        List.of(user0Id)
                );
        anotherTicket.setId(UUID.randomUUID());
        List<Ticket> tickets = List.of(ticket0, anotherTicket);

        when(ticketRepository.findByAssigneeIdsContaining(user0Id))
                .thenReturn(tickets);

        // Act
        List<Ticket> result = ticketDomainService.getTicketsByAssigneeId(user0Id);

        // Assert
        assertEquals(tickets, result);

        verify(ticketRepository, times(1)).findByAssigneeIdsContaining(user0Id);
    }

    @Test
    public void testGetTicketsByAssigneeIdNotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        assertThrows(NoTicketFoundException.class, () -> ticketDomainService.getTicketsByAssigneeId(userId));

        // Assert
        verify(ticketRepository, times(1)).findByAssigneeIdsContaining(userId);
    }

    @Test
    public void testGetProjectIdByTicketId() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        ticket0.setId(ticketId);

        // Act
        UUID result = ticketDomainService.getProjectIdByTicketId(ticket0.getId());

        // Assert
        assertEquals(projectId0, result);

        verify(ticketRepository, times(1)).findById(ticket0.getId());
    }

    @Test
    public void testGetProjectIdByTicketIdNotFound() {
        // Arrange
        UUID ticketId = UUID.randomUUID();

        // Act
        assertThrows(NoTicketFoundException.class, () -> ticketDomainService.getProjectIdByTicketId(ticketId));

        // Assert
        verify(ticketRepository, times(1)).findById(ticketId);
    }

    @Test
    public void testGetProjectIdByPhaseIdOfTicket() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        ticket0.setId(ticketId);
        ticket0.setPhaseId(phaseId0);

        // Act
        UUID result = ticketDomainService.getProjectIdByPhaseIdOfTicket(ticket0.getPhaseId());

        // Assert
        assertEquals(projectId0, result);

        verify(phaseDataOfTicketRepository, times(1)).findByPhaseId(ticket0.getPhaseId());
    }

    @Test
    public void testGetProjectIdByPhaseIdOfTicketNoPhaseData() {
        // Arrange
        UUID phaseId = UUID.randomUUID();

        // Act
        assertThrows(TicketException.class, () -> ticketDomainService.getProjectIdByPhaseIdOfTicket(phaseId));

        // Assert
        verify(phaseDataOfTicketRepository, times(1)).findByPhaseId(phaseId);
    }

    @Test
    public void testPatchTicket() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        ticket0.setId(ticketId);
        ticket0.setPhaseId(phaseId0);
        String newTitle = "New Title";
        String newDescription = "New Description";
        LocalDateTime newDueTime = LocalDateTime.now().plusDays(123);

        when(phaseDataOfTicketRepository.existsByPhaseIdAndProjectId(phaseId1, projectId0))
                .thenReturn(true);

        // Act
        ticketDomainService.patchTicket(
                ticket0.getId(),
                newTitle,
                newDescription,
                newDueTime,
                phaseId1,
                new ArrayList<>()
        );

        // Assert
        assertEquals(newTitle, ticket0.getTitle());
        assertEquals(newDescription, ticket0.getDescription());
        assertEquals(newDueTime, ticket0.getDueTime());
        assertEquals(phaseId1, ticket0.getPhaseId());
        assertTrue(ticket0.getAssigneeIds().isEmpty());

        verify(ticketRepository, times(1)).findById(ticket0.getId());
        verify(ticketRepository, times(1)).save(ticket0);
        verify(phaseDataOfTicketRepository, times(1)).existsByPhaseIdAndProjectId(phaseId1, projectId0);

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<DomainEvent> events = eventCaptor.getAllValues();

        TicketUnassignedEvent ticketUnassignedEvent = (TicketUnassignedEvent) events.get(0);
        assertEquals(ticket0.getId(), ticketUnassignedEvent.getTicketId());
        assertEquals(user0Id, ticketUnassignedEvent.getAssigneeId());
        assertEquals(projectId0, ticketUnassignedEvent.getProjectId());

        TicketPhaseUpdatedEvent ticketPhaseUpdatedEvent = (TicketPhaseUpdatedEvent) events.get(1);
        assertEquals(ticket0.getId(), ticketPhaseUpdatedEvent.getTicketId());
        assertEquals(phaseId0, ticketPhaseUpdatedEvent.getOldPhaseId());
        assertEquals(phaseId1, ticketPhaseUpdatedEvent.getNewPhaseId());
    }

    @Test
    public void testPatchTicketNoTicketFound() {
        // Arrange
        UUID ticketId = UUID.randomUUID();

        // Act
        assertThrows(NoTicketFoundException.class, () -> ticketDomainService.patchTicket(
                ticketId,
                "New Title",
                "New Description",
                LocalDateTime.now().plusDays(123),
                phaseId1,
                new ArrayList<>()
        ));

        // Assert
        verify(ticketRepository, times(1)).findById(ticketId);
        verify(ticketRepository, times(0)).save(any(Ticket.class));
        verify(eventPublisher, times(0)).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testPatchTicketUnrelatedPhase() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        ticket0.setId(ticketId);

        when(phaseDataOfTicketRepository.existsByPhaseIdAndProjectId(phaseId1, projectId0))
                .thenReturn(false);

        // Act
        assertThrows(UnrelatedPhaseException.class, () -> ticketDomainService.patchTicket(
                ticket0.getId(),
                "New Title",
                "New Description",
                LocalDateTime.now().plusDays(123),
                phaseId1,
                new ArrayList<>()
        ));

        // Assert
        verify(ticketRepository, times(1)).findById(ticket0.getId());
        verify(ticketRepository, times(0)).save(any(Ticket.class));
        verify(eventPublisher, times(0)).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testPatchTicketInvalidProjectMembers() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        ticket0.setId(ticketId);
        ticket0.setPhaseId(phaseId0);
        ticket0.removeAssignee(user0Id);

        when(phaseDataOfTicketRepository.existsByPhaseIdAndProjectId(phaseId1, projectId0))
                .thenReturn(true);
        when(membershipDataOfTicketRepository.existsByUserIdAndProjectId(user0Id, projectId0))
                .thenReturn(false);

        // Act
        assertThrows(InvalidProjectMembersException.class, () -> ticketDomainService.patchTicket(
                ticket0.getId(),
                "New Title",
                "New Description",
                LocalDateTime.now().plusDays(123),
                phaseId1,
                List.of(user0Id)
        ));

        // Assert
        verify(ticketRepository, times(1)).findById(ticket0.getId());
        verify(ticketRepository, times(0)).save(any(Ticket.class));
        verify(eventPublisher, times(0)).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testPatchTicketAddAssignee() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        ticket0.setId(ticketId);
        ticket0.setPhaseId(phaseId0);
        ticket0.removeAssignee(user0Id);

        when(membershipDataOfTicketRepository.existsByUserIdAndProjectId(user0Id, projectId0))
                .thenReturn(true);

        // Act
        ticketDomainService.patchTicket(
                ticket0.getId(),
                null,
                null,
                null,
                null,
                List.of(user0Id)
        );

        // Assert
        assertTrue(ticket0.isAssignee(user0Id));

        verify(ticketRepository, times(1)).findById(ticket0.getId());
        verify(ticketRepository, times(1)).save(ticket0);

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        TicketAssignedEvent ticketAssignedEvent = (TicketAssignedEvent) eventCaptor.getValue();
        assertEquals(ticket0.getId(), ticketAssignedEvent.getTicketId());
        assertEquals(projectId0, ticketAssignedEvent.getProjectId());
        assertEquals(user0Id, ticketAssignedEvent.getAssigneeId());
    }

    @Test
    public void testDeleteTicketById() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        ticket0.setId(ticketId);
        ticket0.setPhaseId(phaseId0);

        when(ticketRepository.findById(ticket0.getId()))
                .thenReturn(java.util.Optional.of(ticket0));

        // Act
        ticketDomainService.deleteTicketById(ticket0.getId());

        // Assert
        verify(ticketRepository, times(1)).findById(ticket0.getId());
        verify(ticketRepository, times(1)).removeById(ticket0.getId());

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        TicketDeletedEvent ticketDeletedEvent = (TicketDeletedEvent) eventCaptor.getValue();
        assertEquals(ticket0.getId(), ticketDeletedEvent.getTicketId());
        assertEquals(projectId0, ticketDeletedEvent.getProjectId());
        assertEquals(phaseId0, ticketDeletedEvent.getPhaseId());
    }

    @Test
    public void testDeleteTicketByIdNotFound() {
        // Arrange
        UUID ticketId = UUID.randomUUID();

        // Act
        assertThrows(NoTicketFoundException.class, () -> ticketDomainService.deleteTicketById(ticketId));

        // Assert
        verify(ticketRepository, times(1)).findById(ticketId);
        verify(ticketRepository, times(0)).removeById(ticketId);
        verify(eventPublisher, times(0)).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testHandleMembershipDeletedEvent() {
        // Arrange
        UUID userIdOfAnotherUser = UUID.randomUUID();
        Ticket anotherTicket =
                new Ticket(
                        "Another Ticket",
                        "",
                        LocalDateTime.now().plusDays(127),
                        projectId0,
                        phaseId0,
                        List.of(user0Id, userIdOfAnotherUser)
                );
        anotherTicket.setId(UUID.randomUUID());
        ticket0.setId(UUID.randomUUID());
        ticket0.setAssigneeIds(List.of(user0Id));
        List<Ticket> tickets = List.of(ticket0, anotherTicket);

        MembershipDeletedEvent membershipDeletedEvent =
                new MembershipDeletedEvent(UUID.randomUUID(), projectId0, user0Id);

        when(ticketRepository.findByProjectId(projectId0))
                .thenReturn(tickets);

        // Act
        ticketDomainService.handleMembershipDeletedEvent(membershipDeletedEvent);

        // Assert
        assertFalse(ticket0.isAssignee(user0Id));
        assertFalse(anotherTicket.isAssignee(user0Id));
        assertTrue(anotherTicket.isAssignee(userIdOfAnotherUser));

        verify(ticketRepository, times(1)).findByProjectId(projectId0);
        verify(membershipDataOfTicketRepository).deleteByMembershipId(membershipDeletedEvent.getMembershipId());
        verify(ticketRepository).saveAll(tickets);
    }

    @Test
    public void testHandleMembershipAcceptedEvent() {
        // Arrange
        MembershipAcceptedEvent membershipAcceptedEvent =
                new MembershipAcceptedEvent(UUID.randomUUID(), projectId0, user0Id);

        // Act
        ticketDomainService.handleMembershipAcceptedEvent(membershipAcceptedEvent);

        // Assert
        ArgumentCaptor<MembershipDataOfTicket> membershipDataOfTicketCaptor = ArgumentCaptor.forClass(MembershipDataOfTicket.class);
        verify(membershipDataOfTicketRepository).save(membershipDataOfTicketCaptor.capture());

        MembershipDataOfTicket membershipDataOfTicket = membershipDataOfTicketCaptor.getValue();
        assertEquals(user0Id, membershipDataOfTicket.getUserId());
        assertEquals(projectId0, membershipDataOfTicket.getProjectId());
    }

    @Test
    public void testHandleProjectCreatedEvent() {
        // Arrange
        UUID localProjectId = UUID.randomUUID();

        ProjectCreatedEvent projectCreatedEvent = new ProjectCreatedEvent(localProjectId, user0Id);

        // Act
        ticketDomainService.handleProjectCreatedEvent(projectCreatedEvent);

        // Assert
        ArgumentCaptor<ProjectDataOfTicket> projectDataOfTicketCaptor = ArgumentCaptor.forClass(ProjectDataOfTicket.class);
        verify(projectDataOfTicketRepository).save(projectDataOfTicketCaptor.capture());

        ProjectDataOfTicket projectDataOfTicket = projectDataOfTicketCaptor.getValue();
        assertEquals(localProjectId, projectDataOfTicket.getProjectId());
    }

    @Test
    public void testHandleDefaultProjectCreatedEvent() {
        // Arrange
        UUID localProjectId = UUID.randomUUID();

        DefaultProjectCreatedEvent defaultProjectCreatedEvent = new DefaultProjectCreatedEvent(localProjectId, user0Id);

        // Act
        ticketDomainService.handleDefaultProjectCreatedEvent(defaultProjectCreatedEvent);

        // Assert
        ArgumentCaptor<ProjectDataOfTicket> projectDataOfTicketCaptor = ArgumentCaptor.forClass(ProjectDataOfTicket.class);
        verify(projectDataOfTicketRepository).save(projectDataOfTicketCaptor.capture());

        ProjectDataOfTicket projectDataOfTicket = projectDataOfTicketCaptor.getValue();
        assertEquals(localProjectId, projectDataOfTicket.getProjectId());
    }

    @Test
    public void testHandlePhaseDeletedEvent() {
        // Arrange
        UUID phaseId = UUID.randomUUID();
        PhaseDeletedEvent phaseDeletedEvent = new PhaseDeletedEvent(phaseId, projectId0);

        // Act
        ticketDomainService.handlePhaseDeletedEvent(phaseDeletedEvent);

        // Assert
        verify(phaseDataOfTicketRepository).deleteByPhaseId(phaseId);
    }

    @Test
    public void testHandleProjectDeletedEvent() {
        // Arrange
        UUID localProjectId = UUID.randomUUID();
        ProjectDeletedEvent event = new ProjectDeletedEvent(localProjectId);

        // Act
        ticketDomainService.handleProjectDeletedEvent(event);

        // Assert
        verify(ticketRepository).deleteByProjectId(localProjectId);
        verify(projectDataOfTicketRepository).deleteByProjectId(localProjectId);
    }

    @Test
    public void testHandlePhaseCreatedEvent() {
        // Arrange
        UUID localPhaseId = UUID.randomUUID();
        UUID localProjectId = UUID.randomUUID();
        PhaseCreatedEvent event = new PhaseCreatedEvent(localPhaseId, null, localProjectId);

        // Act
        ticketDomainService.handlePhaseCreatedEvent(event);

        // Assert
        ArgumentCaptor<PhaseDataOfTicket> phaseDataOfTicketCaptor = ArgumentCaptor.forClass(PhaseDataOfTicket.class);
        verify(phaseDataOfTicketRepository).save(phaseDataOfTicketCaptor.capture());

        PhaseDataOfTicket phaseDataOfTicket = phaseDataOfTicketCaptor.getValue();
        assertEquals(localPhaseId, phaseDataOfTicket.getPhaseId());
        assertNull(phaseDataOfTicket.getPreviousPhaseId());
        assertEquals(localProjectId, phaseDataOfTicket.getProjectId());
    }

    @Test
    public void testHandleUserCreatedEvent() {
        // Arrange
        UUID localUserId = UUID.randomUUID();
        String localUserName = "Hans Hermann Hase der Dritte";
        EmailAddress localUserEmail = EmailAddress.fromString("local@user.net");
        UserCreatedEvent event = new UserCreatedEvent(localUserId, localUserName, localUserEmail);

        // Act
        ticketDomainService.handleUserCreatedEvent(event);

        // Assert
        ArgumentCaptor<UserDataOfTicket> userDataOfTicketCaptor = ArgumentCaptor.forClass(UserDataOfTicket.class);
        verify(userDataOfTicketRepository).save(userDataOfTicketCaptor.capture());

        UserDataOfTicket userDataOfTicket = userDataOfTicketCaptor.getValue();
        assertEquals(localUserId, userDataOfTicket.getUserId());
        assertEquals(localUserEmail, userDataOfTicket.getUserEmail());
    }

    @Test
    public void testHandleUserDeletedEvent() {
        // Arrange
        UUID localUserId = UUID.randomUUID();
        String localUserName = "Hans Hermann Hase der Dritte";
        EmailAddress localUserEmail = EmailAddress.fromString("local@user.net");
        UserDeletedEvent event = new UserDeletedEvent(localUserId, localUserName, localUserEmail);

        // Act
        ticketDomainService.handleUserDeletedEvent(event);

        // Assert
        verify(userDataOfTicketRepository).deleteByUserId(localUserId);
    }

    @Test
    public void testHandlePhasePositionUpdatedEvent() {
        // Arrange
        PhasePositionUpdatedEvent event =
                new PhasePositionUpdatedEvent(
                        phaseDataOfTicket0.getPhaseId(),
                        phaseDataOfTicket1.getId(),
                        projectId0
                );

        when(phaseDataOfTicketRepository.findByPhaseId(phaseDataOfTicket0.getPhaseId()))
                .thenReturn(List.of(phaseDataOfTicket0));

        // Act
        ticketDomainService.handlePhasePositionUpdatedEvent(event);

        // Assert
        assertEquals(phaseDataOfTicket1.getId(), phaseDataOfTicket0.getPreviousPhaseId());

        verify(phaseDataOfTicketRepository).save(phaseDataOfTicket0);
    }

    @Test
    public void testHandleUserPatchedEvent() {
        // Arrange
        UUID localUserId = UUID.randomUUID();
        String localUserName = "Hans Hermann Hase der Dritte";
        EmailAddress localUserEmail = EmailAddress.fromString("hans@mail.uk");

        String newLocalUserName = "Hans Hermann Hase der Vierte";
        EmailAddress newLocalUserEmail = EmailAddress.fromString("hermann@post.de");
        UserPatchedEvent event = new UserPatchedEvent(localUserId, newLocalUserName, newLocalUserEmail);

        when(userDataOfTicketRepository.findByUserId(localUserId))
                .thenReturn(List.of(new UserDataOfTicket(localUserId, localUserEmail)));

        // Act
        ticketDomainService.handleUserPatchedEvent(event);

        // Assert
        ArgumentCaptor<UserDataOfTicket> userDataOfTicketCaptor = ArgumentCaptor.forClass(UserDataOfTicket.class);
        verify(userDataOfTicketRepository).save(userDataOfTicketCaptor.capture());

        UserDataOfTicket userDataOfTicket = userDataOfTicketCaptor.getValue();
        assertEquals(localUserId, userDataOfTicket.getUserId());
        assertEquals(newLocalUserEmail, userDataOfTicket.getUserEmail());

        verify(userDataOfTicketRepository).findByUserId(localUserId);
    }
}
