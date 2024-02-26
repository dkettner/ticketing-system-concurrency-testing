package com.kett.TicketSystem.phase.domain;

import com.kett.TicketSystem.common.domainprimitives.DomainEvent;
import com.kett.TicketSystem.common.domainprimitives.EmailAddress;
import com.kett.TicketSystem.common.exceptions.ImpossibleException;
import com.kett.TicketSystem.common.exceptions.NoProjectFoundException;
import com.kett.TicketSystem.common.exceptions.UnrelatedPhaseException;
import com.kett.TicketSystem.phase.domain.consumedData.ProjectDataOfPhase;
import com.kett.TicketSystem.phase.domain.events.PhasePositionUpdatedEvent;
import com.kett.TicketSystem.phase.domain.events.PhaseCreatedEvent;
import com.kett.TicketSystem.phase.domain.events.PhaseDeletedEvent;
import com.kett.TicketSystem.phase.domain.exceptions.LastPhaseException;
import com.kett.TicketSystem.phase.domain.exceptions.NoPhaseFoundException;
import com.kett.TicketSystem.phase.domain.exceptions.PhaseException;
import com.kett.TicketSystem.phase.domain.exceptions.PhaseIsNotEmptyException;
import com.kett.TicketSystem.phase.repository.PhaseRepository;
import com.kett.TicketSystem.phase.repository.ProjectDataOfPhaseRepository;
import com.kett.TicketSystem.project.domain.events.DefaultProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectDeletedEvent;
import com.kett.TicketSystem.ticket.domain.events.TicketCreatedEvent;
import com.kett.TicketSystem.ticket.domain.events.TicketDeletedEvent;
import com.kett.TicketSystem.ticket.domain.events.TicketPhaseUpdatedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PhaseDomainServiceTest {

    @Mock
    private PhaseRepository phaseRepository;

    @Mock
    private ProjectDataOfPhaseRepository projectDataOfPhaseRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PhaseDomainService phaseDomainService;

    private UUID projectId;
    private String phaseName0;
    private Phase phase0;

    private String phaseName1;
    private Phase phaseWithId;

    // for patchPhasePosition
    private UUID patchPhaseProjectId;
    private Phase firstPhase;
    private Phase secondPhase;
    private Phase thirdPhase;

    private EmailAddress testEmailAddress;
    private ProjectDataOfPhase projectDataOfPhase;

    @BeforeEach
    public void buildUp() {
        projectId = UUID.randomUUID();
        phaseName0 = "Phase 0";
        phase0 = new Phase(projectId, phaseName0, null, null);

        phaseName1 = "Phase 1";
        phaseWithId = new Phase(projectId, phaseName1, null, null);
        phaseWithId.setId(UUID.randomUUID());

        testEmailAddress = EmailAddress.fromString("some@test.com");
        projectDataOfPhase = new ProjectDataOfPhase(projectId);

        // for patchPhasePosition
        patchPhaseProjectId = UUID.randomUUID();

        firstPhase = new Phase(patchPhaseProjectId, "firstPhase", null, null);
        firstPhase.setId(UUID.randomUUID());
        secondPhase = new Phase(patchPhaseProjectId, "secondPhase", null, null);
        secondPhase.setId(UUID.randomUUID());
        thirdPhase = new Phase(patchPhaseProjectId, "thirdPhase", null, null);
        thirdPhase.setId(UUID.randomUUID());

        firstPhase.setNextPhase(secondPhase);
        secondPhase.setPreviousPhase(firstPhase);
        secondPhase.setNextPhase(thirdPhase);
        thirdPhase.setPreviousPhase(secondPhase);

        // stubbing
        lenient()
                .when(phaseRepository.save(any(Phase.class)))
                .thenAnswer(invocation -> {
                    Phase phase = invocation.getArgument(0);
                    if (phase.getId() == null) {
                        phase.setId(UUID.randomUUID());
                    }
                    return phase;
                });
        lenient()
                .when(phaseRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    if (phase0.getId() != null && phase0.getId().equals(id)) {
                        return Optional.of(phase0);
                    } else if (phaseWithId.getId().equals(id)) {
                        return Optional.of(phaseWithId);
                    } else if (firstPhase.getId().equals(id)) {
                        return Optional.of(firstPhase);
                    } else if (secondPhase.getId().equals(id)) {
                        return Optional.of(secondPhase);
                    } else if (thirdPhase.getId().equals(id)) {
                        return Optional.of(thirdPhase);
                    } else {
                        return Optional.empty();
                    }
                });

        lenient()
                .when(projectDataOfPhaseRepository.existsByProjectId(patchPhaseProjectId))
                .thenReturn(true);
        lenient()
                .when(projectDataOfPhaseRepository.existsByProjectId(projectId))
                .thenReturn(true);
    }

    @AfterEach
    public void tearDown() {
        projectId = null;
        phaseName0 = null;
        phase0 = null;
        phaseName1 = null;
        phaseWithId = null;
        testEmailAddress = null;
        projectDataOfPhase = null;

        // for patchPhasePosition
        patchPhaseProjectId = null;
        firstPhase = null;
        secondPhase = null;
        thirdPhase = null;
    }

    @Test
    public void testCreatePhaseAddSingleFirst() {
        // Arrange
        UUID localProjectId = UUID.randomUUID();
        Phase localPhase0 = new Phase(localProjectId, "Phase 0", null, null);

        when(projectDataOfPhaseRepository.existsByProjectId(localProjectId))
                .thenReturn(true);

        // Act
        Phase result = phaseDomainService.createPhase(localPhase0, null);

        // Assert
        assertEquals(localPhase0.getId(), result.getId());
        assertEquals(localPhase0.getName(), result.getName());
        assertEquals(localPhase0.getProjectId(), result.getProjectId());
        assertNull(result.getPreviousPhase());
        assertNull(result.getNextPhase());

        // addFirst saves twice to avoid transient phases being referenced in previousPhase etc.
        verify(phaseRepository, times(2)).save(localPhase0);

        // verify event
        ArgumentCaptor<PhaseCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PhaseCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(localPhase0.getId(), eventCaptor.getValue().getPhaseId());
        assertEquals(localPhase0.getProjectId(), eventCaptor.getValue().getProjectId());
        assertNull(eventCaptor.getValue().getPreviousPhaseId());
    }

    @Test
    public void testCreatePhaseAddAfterPrevious() {
        // Arrange
        // nothing to arrange

        // Act
        Phase result = phaseDomainService.createPhase(phase0, phaseWithId.getId());

        // Assert
        assertEquals(phase0.getId(), result.getId());
        assertEquals(phase0.getName(), result.getName());
        assertEquals(phase0.getProjectId(), result.getProjectId());
        assertEquals(phase0.getPreviousPhase(), result.getPreviousPhase());
        assertEquals(phase0.getNextPhase(), result.getNextPhase());
        assertEquals(phaseWithId.getProjectId(), result.getProjectId());
        assertEquals(phaseWithId.getId(), result.getPreviousPhase().getId());
        assertEquals(result.getId(), phaseWithId.getNextPhase().getId());

        verify(phaseRepository, times(2)).save(phase0);
        verify(phaseRepository, times(1)).save(phaseWithId);

        // verify event
        ArgumentCaptor<PhaseCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PhaseCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(phase0.getId(), eventCaptor.getValue().getPhaseId());
        assertEquals(phase0.getProjectId(), eventCaptor.getValue().getProjectId());
        assertEquals(phaseWithId.getId(), eventCaptor.getValue().getPreviousPhaseId());
    }

    @Test
    public void testCreatePhaseAddBeforeNext() {
        // Arrange
        when(phaseRepository.findByProjectIdAndPreviousPhaseIsNull(projectId))
                .thenReturn(Optional.of(phaseWithId));

        // Act
        Phase result = phaseDomainService.createPhase(phase0, null);

        // Assert
        assertEquals(phase0.getId(), result.getId());
        assertEquals(phase0.getName(), result.getName());
        assertEquals(phase0.getProjectId(), result.getProjectId());
        assertEquals(phase0.getPreviousPhase(), result.getPreviousPhase());
        assertEquals(phase0.getNextPhase(), result.getNextPhase());
        assertEquals(phaseWithId.getProjectId(), result.getProjectId());
        assertEquals(result.getId(), phaseWithId.getPreviousPhase().getId());
        assertEquals(phaseWithId.getId(), result.getNextPhase().getId());

        verify(phaseRepository, times(2)).save(phase0);
        verify(phaseRepository, times(1)).save(phaseWithId);

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        verifyNoMoreInteractions(eventPublisher);

        PhasePositionUpdatedEvent phasePositionUpdatedEvent = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(phaseWithId.getId(), phasePositionUpdatedEvent.getPhaseId());
        assertEquals(phaseWithId.getPreviousPhase().getId(), phasePositionUpdatedEvent.getPreviousPhaseId());
        assertEquals(phaseWithId.getProjectId(), phasePositionUpdatedEvent.getProjectId());

        PhaseCreatedEvent phaseCreatedEvent = (PhaseCreatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(phase0.getId(), phaseCreatedEvent.getPhaseId());
        assertEquals(phase0.getProjectId(), phaseCreatedEvent.getProjectId());
        assertNull(phaseCreatedEvent.getPreviousPhaseId());
    }

    @Test
    public void testAddPhaseBetweenPreviousAndNext() {
        // Arrange
        String localPhaseName = "localPhase";
        Phase localPhase = new Phase(projectId, localPhaseName, phaseWithId, null);
        localPhase.setId(UUID.randomUUID());
        phaseWithId.setNextPhase(localPhase);

        // Act
        Phase result = phaseDomainService.createPhase(phase0, phaseWithId.getId());

        // Assert
        assertEquals(phase0.getId(), result.getId());
        assertEquals(phase0.getName(), result.getName());
        assertEquals(phase0.getProjectId(), result.getProjectId());
        assertEquals(phase0.getPreviousPhase(), result.getPreviousPhase());
        assertEquals(phase0.getNextPhase(), result.getNextPhase());
        assertNull(phaseWithId.getPreviousPhase());
        assertEquals(phase0.getId(), phaseWithId.getNextPhase().getId());
        assertEquals(phaseWithId.getId(), phase0.getPreviousPhase().getId());
        assertEquals(localPhase.getId(), phase0.getNextPhase().getId());
        assertEquals(phase0.getId(), localPhase.getPreviousPhase().getId());
        assertNull(localPhase.getNextPhase());

        verify(phaseRepository, times(2)).save(phase0);
        verify(phaseRepository, times(1)).save(phaseWithId);
        verify(phaseRepository, times(1)).save(localPhase);

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        verifyNoMoreInteractions(eventPublisher);

        PhasePositionUpdatedEvent phasePositionUpdatedEvent = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(localPhase.getId(), phasePositionUpdatedEvent.getPhaseId());
        assertEquals(localPhase.getPreviousPhase().getId(), phasePositionUpdatedEvent.getPreviousPhaseId());
        assertEquals(localPhase.getProjectId(), phasePositionUpdatedEvent.getProjectId());

        PhaseCreatedEvent phaseCreatedEvent = (PhaseCreatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(phase0.getId(), phaseCreatedEvent.getPhaseId());
        assertEquals(phaseWithId.getId(), phaseCreatedEvent.getPreviousPhaseId());
        assertEquals(phase0.getProjectId(), phaseCreatedEvent.getProjectId());
    }

    @Test
    public void testAddPhaseNoProjectDataFound() {
        // Arrange
        when(projectDataOfPhaseRepository.existsByProjectId(projectId))
                .thenReturn(false);

        // Act
        assertThrows(NoProjectFoundException.class, () -> phaseDomainService.createPhase(phase0, null));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testAddPhaseUnrelated() {
        // Arrange
        UUID unrelatedProjectId = UUID.randomUUID();
        Phase unrelatedPhase = new Phase(unrelatedProjectId, "unrelatedPhase", null, null);

        when(projectDataOfPhaseRepository.existsByProjectId(unrelatedProjectId))
                .thenReturn(true);

        // Act
        assertThrows(UnrelatedPhaseException.class, () -> phaseDomainService.createPhase(unrelatedPhase, phaseWithId.getId()));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testAddPhaseNoPreviousPhaseFound() {
        // Arrange
        // nothing to arrange

        // Act
        assertThrows(NoPhaseFoundException.class, () -> phaseDomainService.createPhase(phase0, UUID.randomUUID()));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testAddPhaseWithIllegalNextPhase() {
        // Arrange
        phase0.setNextPhase(phaseWithId);

        // Act
        assertThrows(ImpossibleException.class, () -> phaseDomainService.createPhase(phase0, null));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }


    @Test
    public void testGetPhaseById() {
        // Arrange
        // nothing to arrange

        // Act
        Phase result = phaseDomainService.getPhaseById(phaseWithId.getId());

        // Assert
        assertEquals(phaseWithId.getId(), result.getId());
        assertEquals(phaseWithId.getName(), result.getName());
        assertEquals(phaseWithId.getProjectId(), result.getProjectId());
        assertEquals(phaseWithId.getPreviousPhase(), result.getPreviousPhase());
        assertEquals(phaseWithId.getNextPhase(), result.getNextPhase());

        verify(phaseRepository, times(1)).findById(phaseWithId.getId());
    }

    @Test
    public void testGetPhaseByIdNotFound() {
        // Arrange

        UUID randomId = UUID.randomUUID();
        when(phaseRepository.findById(randomId))
                .thenReturn(Optional.empty());

        // Act
        assertThrows(NoPhaseFoundException.class, () -> phaseDomainService.getPhaseById(randomId));

        // Assert
        verify(phaseRepository, times(1)).findById(randomId);
    }

    @Test
    public void testGetPhasesByProjectId() {
        // Arrange
        phase0.setId(UUID.randomUUID());
        phase0.setNextPhase(phaseWithId);
        phaseWithId.setPreviousPhase(phase0);
        List<Phase> phases = new ArrayList<>(Arrays.asList(phase0, phaseWithId));

        when(phaseRepository.findByProjectId(projectId))
                .thenReturn(phases);

        // Act
        List<Phase> result = phaseDomainService.getPhasesByProjectId(projectId);

        // Assert
        assertEquals(phases.size(), result.size());
        for (int i = 0; i < phases.size(); i++) {
            assertEquals(phases.get(i).getId(), result.get(i).getId());
            assertEquals(phases.get(i).getName(), result.get(i).getName());
            assertEquals(phases.get(i).getProjectId(), result.get(i).getProjectId());
            assertEquals(phases.get(i).getPreviousPhase(), result.get(i).getPreviousPhase());
            assertEquals(phases.get(i).getNextPhase(), result.get(i).getNextPhase());
        }

        verify(phaseRepository, times(1)).findByProjectId(projectId);
    }

    @Test
    public void testGetPhasesByProjectIdNotFound() {
        // Arrange
        when(phaseRepository.findByProjectId(projectId))
                .thenReturn(Collections.emptyList());

        // Act
        assertThrows(NoPhaseFoundException.class, () -> phaseDomainService.getPhasesByProjectId(projectId));

        // Assert
        verify(phaseRepository, times(1)).findByProjectId(projectId);
    }

    @Test
    public void testGetFirstPhaseByProjectId() {
        // Arrange
        phase0.setId(UUID.randomUUID());
        when(phaseRepository.findByProjectIdAndPreviousPhaseIsNull(projectId))
                .thenReturn(Optional.of(phase0));

        // Act
        Phase result = phaseDomainService.getFirstPhaseByProjectId(projectId).orElseThrow(RuntimeException::new);

        // Assert
        assertEquals(phase0.getId(), result.getId());
        assertEquals(phase0.getName(), result.getName());
        assertEquals(phase0.getProjectId(), result.getProjectId());
        assertNull(result.getPreviousPhase());
        assertEquals(phase0.getNextPhase(), result.getNextPhase());

        verify(phaseRepository, times(1)).findByProjectIdAndPreviousPhaseIsNull(projectId);
    }

    @Test
    public void testGetFirstPhaseByProjectIdNotFound() {
        // Arrange
        when(phaseRepository.findByProjectIdAndPreviousPhaseIsNull(projectId))
                .thenReturn(Optional.empty());

        // Act
        Optional<Phase> result = phaseDomainService.getFirstPhaseByProjectId(projectId);

        // Assert
        assertTrue(result.isEmpty());
        verify(phaseRepository, times(1)).findByProjectIdAndPreviousPhaseIsNull(projectId);
    }

    @Test
    public void testGetProjectIdByPhaseId() {
        // Arrange
        // nothing to arrange

        // Act
        UUID result = phaseDomainService.getProjectIdByPhaseId(phaseWithId.getId());

        // Assert
        assertEquals(phaseWithId.getProjectId(), result);
    }

    @Test
    public void testGetProjectIdByPhaseIdNotFound() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(phaseRepository.findById(randomId))
                .thenReturn(Optional.empty());

        // Act
        assertThrows(NoPhaseFoundException.class, () -> phaseDomainService.getProjectIdByPhaseId(randomId));

        // Assert
        verify(phaseRepository, times(1)).findById(randomId);
    }

    @Test
    public void testPatchPhaseName() {
        // Arrange
        String newName = "newName";
        phase0.setId(UUID.randomUUID());
        when(phaseRepository.findById(phase0.getId()))
                .thenReturn(Optional.of(phase0));

        // Act
        phaseDomainService.patchPhaseName(phase0.getId(), newName);

        // Assert
        assertEquals(newName, phase0.getName());

        verify(phaseRepository, times(1)).save(phase0);
    }

    @Test
    public void testPatchPhaseNameNotFound() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(phaseRepository.findById(randomId))
                .thenReturn(Optional.empty());

        // Act
        assertThrows(NoPhaseFoundException.class, () -> phaseDomainService.patchPhaseName(randomId, "newName"));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
    }

    @Test
    public void testPatchPhaseNameNull() {
        // Arrange
        phase0.setId(UUID.randomUUID());
        when(phaseRepository.findById(phase0.getId()))
                .thenReturn(Optional.of(phase0));

        // Act
        assertThrows(PhaseException.class, () -> phaseDomainService.patchPhaseName(phase0.getId(), null));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
    }

    @Test
    public void testPatchPhaseNameEmpty() {
        // Arrange
        phase0.setId(UUID.randomUUID());
        when(phaseRepository.findById(phase0.getId()))
                .thenReturn(Optional.of(phase0));

        // Act
        assertThrows(PhaseException.class, () -> phaseDomainService.patchPhaseName(phase0.getId(), ""));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
    }

    @Test
    public void testPatchPhasePositionFirstToFirst() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.patchPhasePosition(firstPhase.getId(), null);

        // Assert
        assertNull(firstPhase.getPreviousPhase());
        assertEquals(secondPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, secondPhase.getPreviousPhase());
        assertEquals(thirdPhase, secondPhase.getNextPhase());
        assertEquals(secondPhase, thirdPhase.getPreviousPhase());
        assertNull(thirdPhase.getNextPhase());

        verify(phaseRepository, times(0)).save(any(Phase.class));

        // verify events
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testPatchPhasePositionFirstToSecond() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.patchPhasePosition(firstPhase.getId(), secondPhase.getId());

        // Assert
        assertNull(secondPhase.getPreviousPhase());
        assertEquals(firstPhase, secondPhase.getNextPhase());
        assertEquals(secondPhase, firstPhase.getPreviousPhase());
        assertEquals(thirdPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, thirdPhase.getPreviousPhase());
        assertNull(thirdPhase.getNextPhase());

        verify(phaseRepository, times(6)).save(any(Phase.class));

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(3)).publishEvent(eventCaptor.capture());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent0 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent0.getPhaseId());
        assertEquals(firstPhase.getId(), phasePositionUpdatedEvent0.getPreviousPhaseId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent1 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(firstPhase.getId(), phasePositionUpdatedEvent1.getPhaseId());
        assertEquals(secondPhase.getId(), phasePositionUpdatedEvent1.getPreviousPhaseId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent2 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(2);
        assertEquals(secondPhase.getId(), phasePositionUpdatedEvent2.getPhaseId());
        assertNull(phasePositionUpdatedEvent2.getPreviousPhaseId());
    }

    @Test
    public void testPatchPhasePositionFirstToLast() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.patchPhasePosition(firstPhase.getId(), thirdPhase.getId());

        // Assert
        assertNull(secondPhase.getPreviousPhase());
        assertEquals(thirdPhase, secondPhase.getNextPhase());
        assertEquals(secondPhase, thirdPhase.getPreviousPhase());
        assertEquals(firstPhase, thirdPhase.getNextPhase());
        assertEquals(thirdPhase, firstPhase.getPreviousPhase());
        assertNull(firstPhase.getNextPhase());

        verify(phaseRepository, times(5)).save(any(Phase.class));

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent0 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(firstPhase.getId(), phasePositionUpdatedEvent0.getPhaseId());
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent0.getPreviousPhaseId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent1 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(secondPhase.getId(), phasePositionUpdatedEvent1.getPhaseId());
        assertNull(phasePositionUpdatedEvent1.getPreviousPhaseId());
    }

    @Test
    public void testPatchPhasePositionSecondToFirst() {
        // Arrange
        when(phaseRepository.findByProjectIdAndPreviousPhaseIsNull(patchPhaseProjectId))
                .thenReturn(Optional.of(firstPhase));

        // Act
        phaseDomainService.patchPhasePosition(secondPhase.getId(), null);

        // Assert
        assertNull(secondPhase.getPreviousPhase());
        assertEquals(firstPhase, secondPhase.getNextPhase());
        assertEquals(secondPhase, firstPhase.getPreviousPhase());
        assertEquals(thirdPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, thirdPhase.getPreviousPhase());
        assertNull(thirdPhase.getNextPhase());

        verify(phaseRepository, times(6)).save(any(Phase.class));

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(3)).publishEvent(eventCaptor.capture());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent0 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(firstPhase.getId(), phasePositionUpdatedEvent0.getPhaseId());
        assertEquals(secondPhase.getId(), phasePositionUpdatedEvent0.getPreviousPhaseId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent1 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(secondPhase.getId(), phasePositionUpdatedEvent1.getPhaseId());
        assertNull(phasePositionUpdatedEvent1.getPreviousPhaseId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent2 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(2);
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent2.getPhaseId());
        assertEquals(firstPhase.getId(), phasePositionUpdatedEvent2.getPreviousPhaseId());
    }

    @Test
    public void testPatchPhasePositionSecondToSecond() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.patchPhasePosition(secondPhase.getId(), firstPhase.getId());

        // Assert
        assertNull(firstPhase.getPreviousPhase());
        assertEquals(secondPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, secondPhase.getPreviousPhase());
        assertEquals(thirdPhase, secondPhase.getNextPhase());
        assertEquals(secondPhase, thirdPhase.getPreviousPhase());
        assertNull(thirdPhase.getNextPhase());

        verify(phaseRepository, times(0)).save(any(Phase.class));

        // verify events
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testPatchPhasePositionSecondToThird() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.patchPhasePosition(secondPhase.getId(), thirdPhase.getId());

        // Assert
        assertNull(firstPhase.getPreviousPhase());
        assertEquals(thirdPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, thirdPhase.getPreviousPhase());
        assertEquals(secondPhase, thirdPhase.getNextPhase());
        assertEquals(thirdPhase, secondPhase.getPreviousPhase());
        assertNull(secondPhase.getNextPhase());

        verify(phaseRepository, times(6)).save(any(Phase.class));

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent0 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(secondPhase.getId(), phasePositionUpdatedEvent0.getPhaseId());
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent0.getPreviousPhaseId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent1 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent1.getPhaseId());
        assertEquals(firstPhase.getId(), phasePositionUpdatedEvent1.getPreviousPhaseId());
    }

    @Test
    public void testPatchPhasePositionLastToFirst() {
        // Arrange
        when(phaseRepository.findByProjectIdAndPreviousPhaseIsNull(patchPhaseProjectId))
                .thenReturn(Optional.of(firstPhase));

        // Act
        phaseDomainService.patchPhasePosition(thirdPhase.getId(), null);

        // Assert
        assertNull(thirdPhase.getPreviousPhase());
        assertEquals(firstPhase, thirdPhase.getNextPhase());
        assertEquals(thirdPhase, firstPhase.getPreviousPhase());
        assertEquals(secondPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, secondPhase.getPreviousPhase());
        assertNull(secondPhase.getNextPhase());

        verify(phaseRepository, times(5)).save(any(Phase.class));

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent0 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(firstPhase.getId(), phasePositionUpdatedEvent0.getPhaseId());
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent0.getPreviousPhaseId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent1 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent1.getPhaseId());
        assertNull(phasePositionUpdatedEvent1.getPreviousPhaseId());
    }

    @Test
    public void testPatchPhasePositionThirdToSecond() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.patchPhasePosition(thirdPhase.getId(), firstPhase.getId());

        // Assert
        assertNull(firstPhase.getPreviousPhase());
        assertEquals(thirdPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, thirdPhase.getPreviousPhase());
        assertEquals(secondPhase, thirdPhase.getNextPhase());
        assertEquals(thirdPhase, secondPhase.getPreviousPhase());
        assertNull(secondPhase.getNextPhase());

        verify(phaseRepository, times(6)).save(any(Phase.class));

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent0 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(secondPhase.getId(), phasePositionUpdatedEvent0.getPhaseId());
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent0.getPreviousPhaseId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent1 = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent1.getPhaseId());
        assertEquals(firstPhase.getId(), phasePositionUpdatedEvent1.getPreviousPhaseId());
    }

    @Test
    public void testPatchPhasePositionThirdToThird() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.patchPhasePosition(thirdPhase.getId(), secondPhase.getId());

        // Assert
        assertNull(firstPhase.getPreviousPhase());
        assertEquals(secondPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, secondPhase.getPreviousPhase());
        assertEquals(thirdPhase, secondPhase.getNextPhase());
        assertEquals(secondPhase, thirdPhase.getPreviousPhase());
        assertNull(thirdPhase.getNextPhase());

        verify(phaseRepository, times(0)).save(any(Phase.class));

        // verify events
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testPatchPhasePositionNotFound() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(phaseRepository.findById(randomId))
                .thenReturn(Optional.empty());

        // Act
        assertThrows(NoPhaseFoundException.class, () -> phaseDomainService.patchPhasePosition(randomId, null));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testPatchPhasePositionUnrelated() {
        // Arrange
        UUID unrelatedProjectId = UUID.randomUUID();
        Phase unrelatedPhase = new Phase(unrelatedProjectId, "unrelatedPhase", null, null);

        when(projectDataOfPhaseRepository.existsByProjectId(unrelatedProjectId))
                .thenReturn(true);
        when(phaseRepository.findById(unrelatedPhase.getId()))
                .thenReturn(Optional.of(unrelatedPhase));

        // Act
        assertThrows(UnrelatedPhaseException.class, () -> phaseDomainService.patchPhasePosition(unrelatedPhase.getId(), firstPhase.getId()));

        // Assert
        verify(phaseRepository, times(1)).save(any(Phase.class)); // save is redundant, commits no changes here
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testDeleteByIdFirstPhase() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.deleteById(firstPhase.getId());

        // Assert
        assertNull(secondPhase.getPreviousPhase());
        assertEquals(thirdPhase, secondPhase.getNextPhase());
        assertEquals(secondPhase, thirdPhase.getPreviousPhase());
        assertNull(thirdPhase.getNextPhase());

        verify(phaseRepository, times(1)).removeById(firstPhase.getId());
        verify(phaseRepository, times(2)).save(any(Phase.class));

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        PhaseDeletedEvent phaseDeletedEvent = (PhaseDeletedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(firstPhase.getId(), phaseDeletedEvent.getPhaseId());
        assertEquals(firstPhase.getProjectId(), phaseDeletedEvent.getProjectId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(secondPhase.getId(), phasePositionUpdatedEvent.getPhaseId());
        assertNull(phasePositionUpdatedEvent.getPreviousPhaseId());
    }

    @Test
    public void testDeleteByIdMiddlePhase() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.deleteById(secondPhase.getId());

        // Assert
        assertNull(firstPhase.getPreviousPhase());
        assertEquals(thirdPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, thirdPhase.getPreviousPhase());
        assertNull(thirdPhase.getNextPhase());

        verify(phaseRepository, times(1)).removeById(secondPhase.getId());
        verify(phaseRepository, times(3)).save(any(Phase.class));

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        PhaseDeletedEvent phaseDeletedEvent = (PhaseDeletedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(secondPhase.getId(), phaseDeletedEvent.getPhaseId());
        assertEquals(secondPhase.getProjectId(), phaseDeletedEvent.getProjectId());

        PhasePositionUpdatedEvent phasePositionUpdatedEvent = (PhasePositionUpdatedEvent) eventCaptor.getAllValues().get(1);
        assertEquals(thirdPhase.getId(), phasePositionUpdatedEvent.getPhaseId());
        assertNull(phasePositionUpdatedEvent.getPreviousPhaseId());
    }

    @Test
    public void testDeleteByIdLastPhase() {
        // Arrange
        // nothing to arrange

        // Act
        phaseDomainService.deleteById(thirdPhase.getId());

        // Assert
        assertNull(firstPhase.getPreviousPhase());
        assertEquals(secondPhase, firstPhase.getNextPhase());
        assertEquals(firstPhase, secondPhase.getPreviousPhase());
        assertNull(secondPhase.getNextPhase());

        verify(phaseRepository, times(1)).removeById(thirdPhase.getId());
        verify(phaseRepository, times(2)).save(any(Phase.class));

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        PhaseDeletedEvent phaseDeletedEvent = (PhaseDeletedEvent) eventCaptor.getAllValues().get(0);
        assertEquals(thirdPhase.getId(), phaseDeletedEvent.getPhaseId());
        assertEquals(thirdPhase.getProjectId(), phaseDeletedEvent.getProjectId());
    }

    @Test
    public void testDeleteByIdNotFound() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(phaseRepository.findById(randomId))
                .thenReturn(Optional.empty());

        // Act
        assertThrows(NoPhaseFoundException.class, () -> phaseDomainService.deleteById(randomId));

        // Assert
        verify(phaseRepository, never()).removeById(randomId);
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testDeleteByIdPhaseIsNotEmpty() {
        // Arrange
        firstPhase.setTicketCount(1);

        // Act
        assertThrows(PhaseIsNotEmptyException.class, () -> phaseDomainService.deleteById(firstPhase.getId()));

        // Assert
        verify(phaseRepository, never()).removeById(firstPhase.getId());
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testDeleteByIdLastPhaseOfProject() {
        // Arrange
        UUID lastPhaseProjectId = UUID.randomUUID();
        Phase lastPhase = new Phase(lastPhaseProjectId, "lastPhase", null, null);
        lastPhase.setId(UUID.randomUUID());
        when(phaseRepository.findById(lastPhase.getId()))
                .thenReturn(Optional.of(lastPhase));

        // Act
        assertThrows(LastPhaseException.class, () -> phaseDomainService.deleteById(lastPhase.getId()));

        // Assert
        verify(phaseRepository, never()).removeById(lastPhase.getId());
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testHandleDefaultProjectCreatedEvent() {
        // Arrange
        UUID localProjectId = UUID.randomUUID();
        ProjectDataOfPhase localProjectDataOfPhase = new ProjectDataOfPhase(localProjectId);
        DefaultProjectCreatedEvent event = new DefaultProjectCreatedEvent(localProjectId, UUID.randomUUID());
        List<Phase> createdPhases = new ArrayList<>();

        when(projectDataOfPhaseRepository.existsByProjectId(localProjectId))
                .thenReturn(true);
        when(projectDataOfPhaseRepository.save(any(ProjectDataOfPhase.class)))
                .thenReturn(localProjectDataOfPhase);

        AtomicReference<Phase> currentFirstPhaseOfProject = new AtomicReference<>(null);
        when(phaseRepository.save(any(Phase.class)))
                .thenAnswer(invocation -> {
                    Phase phase = invocation.getArgument(0);
                    if (phase.getId() == null) {
                        phase.setId(UUID.randomUUID());
                        currentFirstPhaseOfProject.set(phase);
                        createdPhases.add(phase);
                    }
                    return phase;
                });

        when(phaseRepository.findByProjectIdAndPreviousPhaseIsNull(localProjectId))
                .thenAnswer(invocation -> {
                    if (currentFirstPhaseOfProject.get() != null) {
                        return Optional.of(currentFirstPhaseOfProject.get());
                    } else {
                        return Optional.empty();
                    }
                });

        // Act
        phaseDomainService.handleDefaultProjectCreated(event);

        // Assert
        ArgumentCaptor<ProjectDataOfPhase> projectDataOfPhaseArgumentCaptor = ArgumentCaptor.forClass(ProjectDataOfPhase.class);
        verify(projectDataOfPhaseRepository, times(1)).save(projectDataOfPhaseArgumentCaptor.capture());
        assertEquals(localProjectId, projectDataOfPhaseArgumentCaptor.getValue().getProjectId());

        ArgumentCaptor<Phase> phaseArgumentCaptor = ArgumentCaptor.forClass(Phase.class);
        verify(phaseRepository, times(11)).save(phaseArgumentCaptor.capture());
        List<Phase> capturedPhases = phaseArgumentCaptor.getAllValues();
        assertEquals("DONE", capturedPhases.get(0).getName());
        assertEquals(localProjectId, capturedPhases.get(0).getProjectId());
        assertEquals("REVIEW", capturedPhases.get(2).getName());
        assertEquals(localProjectId, capturedPhases.get(2).getProjectId());
        assertEquals("DOING", capturedPhases.get(5).getName());
        assertEquals(localProjectId, capturedPhases.get(5).getProjectId());
        assertEquals("BACKLOG", capturedPhases.get(8).getName());
        assertEquals(localProjectId, capturedPhases.get(8).getProjectId());

        // verify events
        ArgumentCaptor<DomainEvent> eventsCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(7)).publishEvent(eventsCaptor.capture());
        List<DomainEvent> capturedEvents = eventsCaptor.getAllValues();

        // TODO: clean up this mess
        for (DomainEvent capturedEvent : capturedEvents) {
            if (capturedEvent instanceof PhaseCreatedEvent phaseCreatedEvent) {
                assertEquals(localProjectId, phaseCreatedEvent.getProjectId());
                assertNull(phaseCreatedEvent.getPreviousPhaseId());
                Long numberOfCreatedPhasesWithThisId = createdPhases.stream()
                        .filter(p -> p.getId().equals(phaseCreatedEvent.getPhaseId()))
                        .count();
                assertEquals(1, numberOfCreatedPhasesWithThisId);
            } else if (capturedEvent instanceof PhasePositionUpdatedEvent phasePositionUpdatedEvent) {
                Phase phase = createdPhases.stream()
                        .filter(p -> p.getId().equals(phasePositionUpdatedEvent.getPhaseId()))
                        .findFirst()
                        .orElseThrow(RuntimeException::new);
                assertEquals(phase.getPreviousPhase().getId(), phasePositionUpdatedEvent.getPreviousPhaseId());
                assertEquals(localProjectId, phasePositionUpdatedEvent.getProjectId());
            }
        }
    }

    @Test
    public void testHandleProjectDeletedEvent() {
        // Arrange
        ProjectDeletedEvent event = new ProjectDeletedEvent(patchPhaseProjectId);
        List<Phase> phases = new ArrayList<>(Arrays.asList(firstPhase, secondPhase, thirdPhase));

        when(phaseRepository.deleteByProjectId(patchPhaseProjectId))
                .thenReturn(phases);

        // Act
        phaseDomainService.handleProjectDeletedEvent(event);

        // Assert
        verify(phaseRepository, times(1)).deleteByProjectId(patchPhaseProjectId);
        verify(projectDataOfPhaseRepository, times(1)).deleteByProjectId(patchPhaseProjectId);

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(3)).publishEvent(eventCaptor.capture());

        Iterator<DomainEvent> eventIterator = eventCaptor.getAllValues().iterator();
        phases.forEach(phase -> {
            PhaseDeletedEvent phaseDeletedEvent = (PhaseDeletedEvent) eventIterator.next();
            assertEquals(phase.getId(), phaseDeletedEvent.getPhaseId());
            assertEquals(phase.getProjectId(), phaseDeletedEvent.getProjectId());
        });
    }

    @Test
    public void testHandleProjectCreatedEvent() {
        // Arrange
        UUID localProjectId = UUID.randomUUID();
        ProjectCreatedEvent event = new ProjectCreatedEvent(localProjectId, UUID.randomUUID());

        when(projectDataOfPhaseRepository.existsByProjectId(localProjectId))
                .thenReturn(true);

        // Act
        phaseDomainService.handleProjectCreatedEvent(event);

        // Assert
        ArgumentCaptor<ProjectDataOfPhase> projectDataOfPhaseArgumentCaptor = ArgumentCaptor.forClass(ProjectDataOfPhase.class);
        verify(projectDataOfPhaseRepository, times(1)).save(projectDataOfPhaseArgumentCaptor.capture());
        assertEquals(localProjectId, projectDataOfPhaseArgumentCaptor.getValue().getProjectId());

        ArgumentCaptor<Phase> phaseArgumentCaptor = ArgumentCaptor.forClass(Phase.class);
        verify(phaseRepository, times(2)).save(phaseArgumentCaptor.capture());
        assertEquals(localProjectId, phaseArgumentCaptor.getAllValues().get(0).getProjectId());
        assertEquals("BACKLOG", phaseArgumentCaptor.getAllValues().get(0).getName());
        assertNull(phaseArgumentCaptor.getAllValues().get(0).getPreviousPhase());
        assertNull(phaseArgumentCaptor.getAllValues().get(0).getNextPhase());

        // verify events
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        PhaseCreatedEvent phaseCreatedEvent = (PhaseCreatedEvent) eventCaptor.getValue();
        assertEquals(localProjectId, phaseCreatedEvent.getProjectId());
        assertNull(phaseCreatedEvent.getPreviousPhaseId());
    }

    @Test
    public void testHandleTicketCreatedEvent() {
        // Arrange
        UUID localProjectId = UUID.randomUUID();
        UUID localPhaseId = UUID.randomUUID();
        Phase localPhase = new Phase(localProjectId, "localPhase", null, null);
        localPhase.setId(localPhaseId);
        TicketCreatedEvent event = new TicketCreatedEvent(UUID.randomUUID(), localProjectId, UUID.randomUUID());

        when(phaseRepository.findByProjectIdAndPreviousPhaseIsNull(localProjectId))
                .thenReturn(Optional.of(localPhase));

        // Act
        phaseDomainService.handleTicketCreatedEvent(event);

        // Assert
        assertEquals(1, localPhase.getTicketCount());

        verify(phaseRepository, times(1)).save(localPhase);
    }

    @Test
    public void testHandleTicketCreatedEventNoPhasesInProject() {
        // Arrange
        UUID localProjectId = UUID.randomUUID();
        TicketCreatedEvent event = new TicketCreatedEvent(UUID.randomUUID(), localProjectId, UUID.randomUUID());

        when(phaseRepository.findByProjectIdAndPreviousPhaseIsNull(localProjectId))
                .thenReturn(Optional.empty());

        // Act
        assertThrows(ImpossibleException.class, () -> phaseDomainService.handleTicketCreatedEvent(event));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
    }

    @Test
    public void testHandleTicketPhaseUpdatedEvent() {
        // Arrange
        firstPhase.increaseTicketCount();
        TicketPhaseUpdatedEvent event =
                new TicketPhaseUpdatedEvent(
                        UUID.randomUUID(),
                        patchPhaseProjectId,
                        firstPhase.getId(),
                        secondPhase.getId()
                );

        // Act
        phaseDomainService.handleTicketPhaseUpdatedEvent(event);

        // Assert
        assertEquals(0, firstPhase.getTicketCount());
        assertEquals(1, secondPhase.getTicketCount());
        assertEquals(0, thirdPhase.getTicketCount());

        ArgumentCaptor<Phase> phaseArgumentCaptor = ArgumentCaptor.forClass(Phase.class);
        verify(phaseRepository, times(2)).save(phaseArgumentCaptor.capture());
        List<Phase> capturedPhases = phaseArgumentCaptor.getAllValues();
        assertEquals(firstPhase.getId(), capturedPhases.get(0).getId());
        assertEquals(0, capturedPhases.get(0).getTicketCount());
        assertEquals(secondPhase.getId(), capturedPhases.get(1).getId());
        assertEquals(1, capturedPhases.get(1).getTicketCount());

        // verify events
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void handleTicketPhaseUpdatedEventPhaseNotFound() {
        // Arrange
        TicketPhaseUpdatedEvent event =
                new TicketPhaseUpdatedEvent(
                        UUID.randomUUID(),
                        patchPhaseProjectId,
                        UUID.randomUUID(),
                        secondPhase.getId()
                );

        // Act
        assertThrows(NoPhaseFoundException.class, () -> phaseDomainService.handleTicketPhaseUpdatedEvent(event));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void handleTicketPhaseUpdatedEventUnrelatedPhases() {
        // Arrange
        UUID unrelatedProjectId = UUID.randomUUID();
        Phase unrelatedPhase = new Phase(unrelatedProjectId, "unrelatedPhase", null, null);
        TicketPhaseUpdatedEvent event =
                new TicketPhaseUpdatedEvent(
                        UUID.randomUUID(),
                        patchPhaseProjectId,
                        firstPhase.getId(),
                        unrelatedPhase.getId()
                );

        when(phaseRepository.findById(unrelatedPhase.getId()))
                .thenReturn(Optional.of(unrelatedPhase));

        // Act
        assertThrows(UnrelatedPhaseException.class, () -> phaseDomainService.handleTicketPhaseUpdatedEvent(event));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
        verify(eventPublisher, never()).publishEvent(any(DomainEvent.class));
    }

    @Test
    public void testHandleTicketDeletedEvent() {
        // Arrange
        firstPhase.increaseTicketCount();
        TicketDeletedEvent event = new TicketDeletedEvent(UUID.randomUUID(), patchPhaseProjectId, firstPhase.getId());

        // Act
        phaseDomainService.handleTicketDeletedEvent(event);

        // Assert
        assertEquals(0, firstPhase.getTicketCount());

        verify(phaseRepository, times(1)).save(firstPhase);
    }

    @Test
    public void testHandleTicketDeletedEventPhaseNotFound() {
        // Arrange
        TicketDeletedEvent event = new TicketDeletedEvent(UUID.randomUUID(), patchPhaseProjectId, UUID.randomUUID());

        // Act
        assertThrows(NoPhaseFoundException.class, () -> phaseDomainService.handleTicketDeletedEvent(event));

        // Assert
        verify(phaseRepository, never()).save(any(Phase.class));
    }
}
