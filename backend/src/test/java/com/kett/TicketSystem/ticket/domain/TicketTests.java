package com.kett.TicketSystem.ticket.domain;

import com.kett.TicketSystem.ticket.domain.exceptions.TicketException;
import com.kett.TicketSystem.ticket.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class TicketTests {

    @Mock
    private TicketRepository ticketRepository;

    private UUID assigneeId0;
    private UUID assigneeId1;
    private UUID assigneeId2;
    private UUID assigneeId3;

    private List<UUID> assigneeIds0;
    private List<UUID> assigneeIds1;
    private List<UUID> assigneeIds2;
    private List<UUID> assigneeIds3;

    private UUID projectId0;
    private UUID projectId1;
    private UUID projectId2;
    private UUID projectId3;

    private UUID phaseId0;
    private UUID phaseId1;
    private UUID phaseId2;
    private UUID phaseId3;

    private String title0;
    private String title1;
    private String title2;
    private String title3;

    private String description0;
    private String description1;
    private String description2;
    private String description3;

    private LocalDateTime dueTime0;
    private LocalDateTime dueTime1;
    private LocalDateTime dueTime2;
    private LocalDateTime dueTime3;

    private Ticket ticket0;
    private Ticket ticket1;
    private Ticket ticket2;
    private Ticket ticket3;

    public static class TestableTicket extends Ticket {
        public TestableTicket(String title, String description, LocalDateTime dueTime, UUID projectId, UUID phaseId, List<UUID> assigneeIds) {
            super(title, description, dueTime, projectId, phaseId, assigneeIds);
        }

        public void publicSetProjectId(UUID projectId) {
            this.setProjectId(projectId);
        }
    }

    @BeforeEach
    public void buildUp() {
        lenient()
                .when(ticketRepository.save(ArgumentMatchers.any(Ticket.class)))
                .thenAnswer(invocation -> {
                    Ticket ticket = invocation.getArgument(0);
                    if(ticket.getId() == null) {
                        ticket.setId(UUID.randomUUID());
                    }
                    return ticket;
                });

        lenient()
                .when(ticketRepository.findById(ArgumentMatchers.any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    if(id.equals(ticket0.getId())) {
                        return Optional.of(ticket0);
                    } else if(id.equals(ticket1.getId())) {
                        return Optional.of(ticket1);
                    } else if(id.equals(ticket2.getId())) {
                        return Optional.of(ticket2);
                    } else if(id.equals(ticket3.getId())) {
                        return Optional.of(ticket3);
                    } else {
                        return Optional.empty();
                    }
                });

        assigneeId0 = UUID.randomUUID();
        assigneeId1 = UUID.randomUUID();
        assigneeId2 = UUID.randomUUID();
        assigneeId3 = UUID.randomUUID();

        projectId0 = UUID.randomUUID();
        projectId1 = UUID.randomUUID();
        projectId2 = UUID.randomUUID();
        projectId3 = UUID.randomUUID();

        phaseId0 = UUID.randomUUID();
        phaseId1 = UUID.randomUUID();
        phaseId2 = UUID.randomUUID();
        phaseId3 = UUID.randomUUID();

        title0 = "title0";
        title1 = "title1";
        title2 = "title2";
        title3 = "title3";

        description0 = "description0";
        description1 = "description1";
        description2 = "description2";
        description3 = "description3";

        dueTime0 = LocalDateTime.of(2050, 1, 1, 0, 0);
        dueTime1 = LocalDateTime.of(2051, 1, 1, 0, 0);
        dueTime2 = LocalDateTime.of(2052, 1, 1, 0, 0);
        dueTime3 = LocalDateTime.of(2053, 1, 1, 0, 0);

        assigneeIds0 = new ArrayList<>();
        assigneeIds0.add(assigneeId0);
        assigneeIds0.add(assigneeId1);

        assigneeIds1 = new ArrayList<>();
        assigneeIds1.add(assigneeId1);
        assigneeIds1.add(assigneeId2);

        assigneeIds2 = new ArrayList<>();
        assigneeIds2.add(assigneeId2);
        assigneeIds2.add(assigneeId3);

        assigneeIds3 = new ArrayList<>();
        assigneeIds3.add(assigneeId3);
        assigneeIds3.add(assigneeId0);

        ticket0 = new Ticket(title0, description0, dueTime0, projectId0, phaseId0, assigneeIds0);
        ticket1 = new Ticket(title1, description1, dueTime1, projectId1, phaseId1, assigneeIds1);
        ticket2 = new Ticket(title2, description2, dueTime2, projectId2, phaseId2, assigneeIds2);
        ticket3 = new Ticket(title3, description3, dueTime3, projectId3, phaseId3, assigneeIds3);
    }

    @AfterEach
    public void tearDown() {
        assigneeId0 = null;
        assigneeId1 = null;
        assigneeId2 = null;
        assigneeId3 = null;

        assigneeIds0 = null;
        assigneeIds1 = null;
        assigneeIds2 = null;
        assigneeIds3 = null;

        projectId0 = null;
        projectId1 = null;
        projectId2 = null;
        projectId3 = null;

        phaseId0 = null;
        phaseId1 = null;
        phaseId2 = null;
        phaseId3 = null;

        title0 = null;
        title1 = null;
        title2 = null;
        title3 = null;

        description0 = null;
        description1 = null;
        description2 = null;
        description3 = null;

        dueTime0 = null;
        dueTime1 = null;
        dueTime2 = null;
        dueTime3 = null;

        ticket0 = null;
        ticket1 = null;
        ticket2 = null;
        ticket3 = null;
    }

    @Test
    public void checkValidConstructorParameters() {
        assertNotNull(ticket0);
        assertNotNull(ticket1);
        assertNotNull(ticket2);
        assertNotNull(ticket3);

        new Ticket(title0, description0, dueTime0, projectId0, phaseId0, assigneeIds0);
        new Ticket(title1, description1, dueTime1, projectId1, phaseId1, assigneeIds1);
        new Ticket(title2, description2, dueTime2, projectId2, phaseId2, assigneeIds2);
        new Ticket(title3, description3, dueTime3, projectId3, phaseId3, assigneeIds3);

        new Ticket(title0, description0, dueTime0, projectId0, phaseId0, new ArrayList<>());
        new Ticket(title1, description1, dueTime1, projectId1, phaseId1, new ArrayList<>());
        new Ticket(title2, description2, dueTime2, projectId2, phaseId2, new ArrayList<>());
        new Ticket(title3, description3, dueTime3, projectId3, phaseId3, new ArrayList<>());

        new Ticket(title0, "", dueTime0, projectId0, phaseId0, assigneeIds0);
        new Ticket(title1, "", dueTime1, projectId1, phaseId1, assigneeIds1);
        new Ticket(title2, "", dueTime2, projectId2, phaseId2, assigneeIds2);
        new Ticket(title3, "", dueTime3, projectId3, phaseId3, assigneeIds3);

        new Ticket(title0, null, dueTime0, projectId0, phaseId0, assigneeIds0);
        new Ticket(title1, null, dueTime1, projectId1, phaseId1, assigneeIds1);
        new Ticket(title2, null, dueTime2, projectId2, phaseId2, assigneeIds2);
        new Ticket(title3, null, dueTime3, projectId3, phaseId3, assigneeIds3);

        new Ticket(title0, description0, null, projectId0, phaseId0, assigneeIds0);
        new Ticket(title1, description1, null, projectId1, phaseId1, assigneeIds1);
        new Ticket(title2, description2, null, projectId2, phaseId2, assigneeIds2);
        new Ticket(title3, description3, null, projectId3, phaseId3, assigneeIds3);

        new Ticket(title0, description0, dueTime0, projectId0, null, assigneeIds0);
        new Ticket(title1, description1, dueTime1, projectId1, null, assigneeIds1);
        new Ticket(title2, description2, dueTime2, projectId2, null, assigneeIds2);
        new Ticket(title3, description3, dueTime3, projectId3, null, assigneeIds3);

        new Ticket(title0, null, null, projectId0, null, assigneeIds0);
        new Ticket(title1, "", null, projectId1, phaseId1, assigneeIds1);
        new Ticket(title2, null, dueTime2, projectId2, null, new ArrayList<>());
        new Ticket(title3, "", dueTime3, projectId3, phaseId3, new ArrayList<>());
    }

    @Test
    public void checkNullConstructorParameters() {
        assertThrows(TicketException.class, () -> new Ticket(null, description0, dueTime0, projectId0, phaseId0, assigneeIds0));
        assertThrows(TicketException.class, () -> new Ticket(title0, description0, dueTime0, null, phaseId0, assigneeIds0));
        assertThrows(TicketException.class, () -> new Ticket(title0, description0, dueTime0, projectId0, phaseId0, null));
    }

    @Test
    public void checkEmptyTitleParameter() {
        assertThrows(TicketException.class, () -> new Ticket("", description0, dueTime0, projectId0, phaseId0, assigneeIds0));
    }

    @Test
    public void checkEquals() {
        Ticket ticket0Copy = new Ticket(title0, description0, dueTime0, projectId0, phaseId0, assigneeIds0);
        Ticket ticket1Copy = new Ticket(title1, description1, dueTime1, projectId1, phaseId1, assigneeIds1);

        // only possible in tests because creationTime is private and should not be settable
        ticket0Copy.setCreationTime(ticket0.getCreationTime());
        ticket1Copy.setCreationTime(ticket1.getCreationTime());

        // without id, same parameters
        assertEquals(ticket0, ticket0Copy);
        assertEquals(ticket1, ticket1Copy);

        // without id, different parameters
        assertNotEquals(ticket0, ticket1);
        assertNotEquals(ticket1, ticket0Copy);

        // add id
        ticketRepository.save(ticket0);
        ticketRepository.save(ticket1);
        ticketRepository.save(ticket0Copy);
        ticketRepository.save(ticket1Copy);

        // with id, compare with itself
        assertEquals(ticket0, ticketRepository.findById(ticket0.getId()).get());
        assertEquals(ticket1, ticketRepository.findById(ticket1.getId()).get());

        // with id, compare with different object
        assertNotEquals(ticket0, ticket1);
        assertNotEquals(ticket1, ticket0Copy);

        // with id, compare with different object with same parameters
        assertNotEquals(ticket0, ticket0Copy);
        assertNotEquals(ticket1, ticket1Copy);
    }

    @Test
    public void checkHashCode() {
        Ticket ticket0Copy = new Ticket(title0, description0, dueTime0, projectId0, phaseId0, assigneeIds0);
        Ticket ticket1Copy = new Ticket(title1, description1, dueTime1, projectId1, phaseId1, assigneeIds1);

        // only possible in tests because creationTime is private and should not be settable
        ticket0Copy.setCreationTime(ticket0.getCreationTime());
        ticket1Copy.setCreationTime(ticket1.getCreationTime());

        // without id, same parameters
        assertEquals(ticket0.hashCode(), ticket0Copy.hashCode());
        assertEquals(ticket1.hashCode(), ticket1Copy.hashCode());

        // without id, different parameters
        assertNotEquals(ticket0.hashCode(), ticket1.hashCode());
        assertNotEquals(ticket1.hashCode(), ticket0Copy.hashCode());

        // add id
        ticketRepository.save(ticket0);
        ticketRepository.save(ticket1);
        ticketRepository.save(ticket0Copy);
        ticketRepository.save(ticket1Copy);

        // with id, compare with itself
        assertEquals(ticket0.hashCode(), ticketRepository.findById(ticket0.getId()).get().hashCode());
        assertEquals(ticket1.hashCode(), ticketRepository.findById(ticket1.getId()).get().hashCode());

        // with id, compare with different object
        assertNotEquals(ticket0.hashCode(), ticket1.hashCode());
        assertNotEquals(ticket1.hashCode(), ticket0Copy.hashCode());

        // with id, compare with different object with same parameters
        assertNotEquals(ticket0.hashCode(), ticket0Copy.hashCode());
        assertNotEquals(ticket1.hashCode(), ticket1Copy.hashCode());
    }

    @Test
    public void checkTitle() {
        assertEquals(title0, ticket0.getTitle());
        assertEquals(title1, ticket1.getTitle());
        assertEquals(title2, ticket2.getTitle());
        assertEquals(title3, ticket3.getTitle());
    }

    @Test
    public void checkDescription() {
        assertEquals(description0, ticket0.getDescription());
        assertEquals(description1, ticket1.getDescription());
        assertEquals(description2, ticket2.getDescription());
        assertEquals(description3, ticket3.getDescription());
    }

    @Test
    public void checkCreationTime() {
        assertNotNull(ticket0.getCreationTime());
        assertNotNull(ticket1.getCreationTime());
        assertNotNull(ticket2.getCreationTime());
        assertNotNull(ticket3.getCreationTime());

        assertTrue(ticket0.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(ticket1.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(ticket2.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(ticket3.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    public void checkDueTime() {
        assertEquals(dueTime0, ticket0.getDueTime());
        assertEquals(dueTime1, ticket1.getDueTime());
        assertEquals(dueTime2, ticket2.getDueTime());
        assertEquals(dueTime3, ticket3.getDueTime());
    }

    @Test
    public void checkProjectId() {
        assertEquals(projectId0, ticket0.getProjectId());
        assertEquals(projectId1, ticket1.getProjectId());
        assertEquals(projectId2, ticket2.getProjectId());
        assertEquals(projectId3, ticket3.getProjectId());
    }

    @Test
    public void checkPhaseId() {
        assertEquals(phaseId0, ticket0.getPhaseId());
        assertEquals(phaseId1, ticket1.getPhaseId());
        assertEquals(phaseId2, ticket2.getPhaseId());
        assertEquals(phaseId3, ticket3.getPhaseId());
    }

    @Test
    public void checkSetTitle() {
        ticket0.setTitle(title1);
        assertEquals(title1, ticket0.getTitle());
        assertThrows(TicketException.class, () -> ticket0.setTitle(null));
        assertThrows(TicketException.class, () -> ticket0.setTitle(""));
    }

    @Test
    public void checkSetProjectId() {
        TestableTicket testableTicket = new TestableTicket(title0, description0, dueTime0, projectId0, phaseId0, assigneeIds0);
        testableTicket.publicSetProjectId(projectId1);
        assertEquals(projectId1, testableTicket.getProjectId());
        assertThrows(TicketException.class, () -> testableTicket.publicSetProjectId(null));
    }

    @Test
    public void checkSetDueTime() {
        ticket0.setDueTime(dueTime1);
        assertEquals(dueTime1, ticket0.getDueTime());

        assertThrows(TicketException.class, () -> ticket0.setDueTime(LocalDateTime.now().minusDays(1)));
        assertEquals(dueTime1, ticket0.getDueTime());

        ticket0.setDueTime(null);
        assertNull(ticket0.getDueTime());
    }

    @Test
    public void checkSetPhaseId() {
        ticket0.setPhaseId(phaseId1);
        assertEquals(phaseId1, ticket0.getPhaseId());
    }

    @Test
    public void checkSetAssigneeIds() {
        ticket0.setAssigneeIds(assigneeIds1);
        assertEquals(assigneeIds1, ticket0.getAssigneeIds());

        assertThrows(TicketException.class, () -> ticket0.setAssigneeIds(null));
        assertEquals(assigneeIds1, ticket0.getAssigneeIds());

        ticket0.setAssigneeIds(new ArrayList<>());
        assertTrue(ticket0.getAssigneeIds().isEmpty());
    }

    @Test
    public void checkIsAssignee() {
        UUID assigneeIdToCheck = assigneeIds0.get(0);
        assertTrue(ticket0.isAssignee(assigneeIdToCheck));

        UUID nonAssigneeId = UUID.randomUUID();
        assertFalse(ticket0.isAssignee(nonAssigneeId));
    }

    @Test
    public void checkRemoveAssignee() {
        UUID assigneeIdToRemove = assigneeIds0.get(0);
        ticket0.removeAssignee(assigneeIdToRemove);
        assertFalse(ticket0.getAssigneeIds().contains(assigneeIdToRemove));
        assertFalse(ticket0.isAssignee(assigneeIdToRemove));
    }
}
