package com.kett.TicketSystem.phase.domain;

import com.kett.TicketSystem.common.exceptions.UnrelatedPhaseException;
import com.kett.TicketSystem.phase.domain.exceptions.PhaseException;
import com.kett.TicketSystem.phase.repository.PhaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class PhaseTests {
    private UUID projectId0;
    private UUID projectId1;

    private Phase phase0;
    private Phase phase1;
    private Phase phase2;
    private Phase phase3;
    private Phase phase4;
    private Phase phase5;
    private Phase phase6;
    private Phase phase7;

    private String phaseName0;
    private String phaseName1;
    private String phaseName2;
    private String phaseName3;
    private String phaseName4;
    private String phaseName5;
    private String phaseName6;
    private String phaseName7;

    @Mock
    private PhaseRepository phaseRepository;

    public static class TestablePhase extends Phase {
        public TestablePhase(UUID projectId, String name, Phase previousPhase, Phase nextPhase) {
            super(projectId, name, previousPhase, nextPhase);
        }

        public void publicSetProjectId(UUID projectId) {
            this.setProjectId(projectId);
        }
    }

    @BeforeEach
    public void buildUp() {
        lenient()
                .when(phaseRepository.save(ArgumentMatchers.any(Phase.class)))
                .thenAnswer(invocation -> {
                    Phase phase = invocation.getArgument(0);
                    if(phase.getId() == null) {
                        phase.setId(UUID.randomUUID());
                    }
                    return phase;
                });

        lenient()
                .when(phaseRepository.findById(ArgumentMatchers.any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    if(id.equals(phase0.getId())) {
                        return Optional.of(phase0);
                    } else if(id.equals(phase1.getId())) {
                        return Optional.of(phase1);
                    } else if(id.equals(phase2.getId())) {
                        return Optional.of(phase2);
                    } else if(id.equals(phase3.getId())) {
                        return Optional.of(phase3);
                    } else {
                        return Optional.empty();
                    }
                });

        projectId0 = UUID.randomUUID();
        projectId1 = UUID.randomUUID();

        phaseName0 = "Phase 0 Project 0";
        phaseName1 = "Phase 1 Project 0";
        phaseName2 = "Phase 2 Project 0";
        phaseName3 = "Phase 3 Project 0";
        phaseName4 = "Phase 0 Project 1";
        phaseName5 = "Phase 1 Project 1";
        phaseName6 = "Phase 2 Project 1";
        phaseName7 = "Phase 3 Project 1";

        phase0 = new Phase(projectId0, phaseName0, null, null);
        phase1 = new Phase(projectId0, phaseName1, null, null);
        phase2 = new Phase(projectId0, phaseName2, null, null);
        phase3 = new Phase(projectId0, phaseName3, null, null);
        phase4 = new Phase(projectId1, phaseName4, null, null);
        phase5 = new Phase(projectId1, phaseName5, null, null);
        phase6 = new Phase(projectId1, phaseName6, null, null);
        phase7 = new Phase(projectId1, phaseName7, null, null);
    }

    @AfterEach
    public void tearDown() {
        projectId0 = null;
        projectId1 = null;

        phase0 = null;
        phase1 = null;
        phase2 = null;
        phase3 = null;
        phase4 = null;
        phase5 = null;
        phase6 = null;
        phase7 = null;

        phaseName0 = null;
        phaseName1 = null;
        phaseName2 = null;
        phaseName3 = null;
        phaseName4 = null;
        phaseName5 = null;
        phaseName6 = null;
        phaseName7 = null;

        phaseRepository.deleteAll();
    }

    @Test
    public void checkConstructorValid() {
        assertNotNull(phase0);
        assertNotNull(phase1);
        assertNotNull(phase2);
        assertNotNull(phase3);
        assertNotNull(phase4);
        assertNotNull(phase5);
        assertNotNull(phase6);
        assertNotNull(phase7);

        // no related phases
        new Phase(projectId0, "Phase 0", null, null);

        // related phases
        new Phase(projectId0, "Phase 0", phase1, null);
        new Phase(projectId0, "Phase 0", null, phase2);
        new Phase(projectId0, "Phase 0", phase1, phase2);
    }

    @Test
    public void checkConstructorInvalidProjectId() {
        assertThrows(PhaseException.class, () -> new Phase(null, "Phase 1", null, null));

        assertNotEquals(projectId0, phase4.getProjectId()); // -> unrelated
        assertThrows(UnrelatedPhaseException.class, () -> new Phase (projectId0, "Phase XY", phase4, null));
        assertThrows(UnrelatedPhaseException.class, () -> new Phase (projectId0, "Phase XY", null, phase4));
    }

    @Test
    public void checkConstructorInvalidName() {
        UUID projectId = UUID.randomUUID();
        assertThrows(PhaseException.class, () -> new Phase(projectId, null, null, null));
        assertThrows(PhaseException.class, () -> new Phase(projectId, "", null, null));
    }

    @Test
    public void checkConstructorInvalidPreviousOrNextPhase() {
        assertThrows(UnrelatedPhaseException.class, () -> new Phase(projectId0, "Phase 1", phase4, null));
        assertThrows(UnrelatedPhaseException.class, () -> new Phase(projectId0, "Phase 1", null, phase5));
    }

    @Test
    public void checkEquals() {
        Phase phase0Copy = new Phase(phase0.getProjectId(), phase0.getName(), phase0.getPreviousPhase(), phase0.getNextPhase());
        Phase phase1Copy = new Phase(phase1.getProjectId(), phase1.getName(), phase1.getPreviousPhase(), phase1.getNextPhase());
        Phase phase2Copy = new Phase(phase2.getProjectId(), phase2.getName(), phase2.getPreviousPhase(), phase2.getNextPhase());
        Phase phase3Copy = new Phase(phase3.getProjectId(), phase3.getName(), phase3.getPreviousPhase(), phase3.getNextPhase());

        // without id, same parameters
        assertEquals(phase0, phase0Copy);
        assertEquals(phase1, phase1Copy);
        assertEquals(phase2, phase2Copy);
        assertEquals(phase3, phase3Copy);

        // without id, different parameters
        assertNotEquals(phase0, phase1);
        assertNotEquals(phase1, phase2);
        assertNotEquals(phase2, phase3);
        assertNotEquals(phase3, phase0);

        // add id
        phaseRepository.save(phase0);
        phaseRepository.save(phase1);
        phaseRepository.save(phase2);
        phaseRepository.save(phase3);
        phaseRepository.save(phase0Copy);
        phaseRepository.save(phase1Copy);
        phaseRepository.save(phase2Copy);
        phaseRepository.save(phase3Copy);

        // assigning relations only possible with IDs
        // 0 -> 1 -> 2
        phase0.setNextPhase(phase1);
        phase1.setPreviousPhase(phase0);
        phase1.setNextPhase(phase2);
        phase2.setPreviousPhase(phase1);

        phase0Copy.setNextPhase(phase1);
        phase1Copy.setPreviousPhase(phase0);
        phase1Copy.setNextPhase(phase2);
        phase2Copy.setPreviousPhase(phase1);

        // with id, compare with itself
        assertEquals(phase0, phaseRepository.findById(phase0.getId()).get());
        assertEquals(phase1, phaseRepository.findById(phase1.getId()).get());
        assertEquals(phase2, phaseRepository.findById(phase2.getId()).get());
        assertEquals(phase3, phaseRepository.findById(phase3.getId()).get());

        // with id, compare with different object
        assertNotEquals(phase0, phase1);
        assertNotEquals(phase1, phase2);
        assertNotEquals(phase2, phase3);
        assertNotEquals(phase3, phase0);

        // with id, compare with different object with same parameters
        assertNotEquals(phase0, phase0Copy);
        assertNotEquals(phase1, phase1Copy);
        assertNotEquals(phase2, phase2Copy);
        assertNotEquals(phase3, phase3Copy);
    }

    @Test
    public void checkHashCode() {
        Phase phase0Copy = new Phase(phase0.getProjectId(), phase0.getName(), phase0.getPreviousPhase(), phase0.getNextPhase());
        Phase phase1Copy = new Phase(phase1.getProjectId(), phase1.getName(), phase1.getPreviousPhase(), phase1.getNextPhase());
        Phase phase2Copy = new Phase(phase2.getProjectId(), phase2.getName(), phase2.getPreviousPhase(), phase2.getNextPhase());
        Phase phase3Copy = new Phase(phase3.getProjectId(), phase3.getName(), phase3.getPreviousPhase(), phase3.getNextPhase());

        // without id, same parameters
        assertEquals(phase0.hashCode(), phase0Copy.hashCode());
        assertEquals(phase1.hashCode(), phase1Copy.hashCode());
        assertEquals(phase2.hashCode(), phase2Copy.hashCode());
        assertEquals(phase3.hashCode(), phase3Copy.hashCode());

        // without id, different parameters
        assertNotEquals(phase0.hashCode(), phase1.hashCode());
        assertNotEquals(phase1.hashCode(), phase2.hashCode());
        assertNotEquals(phase2.hashCode(), phase3.hashCode());
        assertNotEquals(phase3.hashCode(), phase0.hashCode());

        // add id
        phaseRepository.save(phase0);
        phaseRepository.save(phase1);
        phaseRepository.save(phase2);
        phaseRepository.save(phase3);
        phaseRepository.save(phase0Copy);
        phaseRepository.save(phase1Copy);
        phaseRepository.save(phase2Copy);
        phaseRepository.save(phase3Copy);

        // assigning relations only possible with IDs
        // 0 -> 1 -> 2
        phase0.setNextPhase(phase1);
        phase1.setPreviousPhase(phase0);
        phase1.setNextPhase(phase2);
        phase2.setPreviousPhase(phase1);

        phase0Copy.setNextPhase(phase1);
        phase1Copy.setPreviousPhase(phase0);
        phase1Copy.setNextPhase(phase2);
        phase2Copy.setPreviousPhase(phase1);

        // with id, compare with itself
        assertEquals(phase0.hashCode(), phaseRepository.findById(phase0.getId()).get().hashCode());
        assertEquals(phase1.hashCode(), phaseRepository.findById(phase1.getId()).get().hashCode());
        assertEquals(phase2.hashCode(), phaseRepository.findById(phase2.getId()).get().hashCode());
        assertEquals(phase3.hashCode(), phaseRepository.findById(phase3.getId()).get().hashCode());

        // with id, compare with different object
        assertNotEquals(phase0.hashCode(), phase1.hashCode());
        assertNotEquals(phase1.hashCode(), phase2.hashCode());
        assertNotEquals(phase2.hashCode(), phase3.hashCode());
        assertNotEquals(phase3.hashCode(), phase0.hashCode());

        // with id, compare with different object with same parameters
        assertNotEquals(phase0.hashCode(), phase0Copy.hashCode());
        assertNotEquals(phase1.hashCode(), phase1Copy.hashCode());
        assertNotEquals(phase2.hashCode(), phase2Copy.hashCode());
        assertNotEquals(phase3.hashCode(), phase3Copy.hashCode());
    }

    @Test
    public void checkSetNameValid() {
        String newName = "New Phase Name";
        phase0.setName(newName);
        assertEquals(newName, phase0.getName());
    }

    @Test
    public void checkSetNameInvalid() {
        assertThrows(PhaseException.class, () -> phase0.setName(null));
        assertEquals(phaseName0, phase0.getName());

        assertThrows(PhaseException.class, () -> phase0.setName(""));
        assertEquals(phaseName0, phase0.getName());
    }

    @Test
    public void checkSetProjectId() {
        TestablePhase testablePhase = new TestablePhase(projectId0, phaseName0, null, null);

        testablePhase.publicSetProjectId(projectId1);
        assertEquals(projectId1, testablePhase.getProjectId());

        assertThrows(PhaseException.class, () -> testablePhase.publicSetProjectId(null));
        assertEquals(projectId1, testablePhase.getProjectId());
    }

    @Test
    public void checkSetPreviousPhase() {
        // same project: valid
        phase0.setPreviousPhase(phase1);
        assertEquals(phase1, phase0.getPreviousPhase());

        // different project: invalid
        assertNotEquals(phase0.getProjectId(), phase4.getProjectId()); // different projects -> unrelated
        assertThrows(UnrelatedPhaseException.class, () -> phase0.setPreviousPhase(phase4));
        assertNotEquals(phase4, phase0.getPreviousPhase());
        assertEquals(phase1, phase0.getPreviousPhase());

        // null: valid
        phase0.setPreviousPhase(null);
        assertNull(phase0.getPreviousPhase());
    }

    @Test
    public void checkSetNextPhase() {
        // same project: valid
        phase0.setNextPhase(phase1);
        assertEquals(phase1, phase0.getNextPhase());

        // different project: invalid
        assertNotEquals(phase0.getProjectId(), phase4.getProjectId()); // different projects -> unrelated
        assertThrows(UnrelatedPhaseException.class, () -> phase0.setNextPhase(phase4));
        assertEquals(phase1, phase0.getNextPhase());

        // null: valid
        phase0.setNextPhase(null);
        assertNull(phase0.getNextPhase());
    }

    @Test
    public void checkSetTicketCountValid() {
        phase0.setTicketCount(0);
        assertEquals(0, phase0.getTicketCount());

        phase1.setTicketCount(5);
        assertEquals(5, phase1.getTicketCount());

        assertThrows(PhaseException.class, () -> phase1.setTicketCount(-1));
        assertEquals(5, phase1.getTicketCount());
    }

    @Test
    public void checkIncreaseAndDecreaseTicketCount() {
        phase0.setTicketCount(3); // = 3

        // increase
        phase0.increaseTicketCount(); // -> 4
        assertEquals(4, phase0.getTicketCount());

        // decrease
        phase0.decreaseTicketCount(); // -> 3
        assertEquals(3, phase0.getTicketCount());

        // set to 0 and then decrease again
        phase0.setTicketCount(0); // -> 0
        assertEquals(0, phase0.getTicketCount()); // is 0 now
        assertThrows(PhaseException.class, () -> phase0.decreaseTicketCount()); // cannot be negative
        assertEquals(0, phase0.getTicketCount()); // is still 0
    }

    @Test
    public void checkIsFirstAndIsLast() {
        phase0.setPreviousPhase(null);
        assertTrue(phase0.isFirst());

        phase0.setPreviousPhase(phase1);
        assertFalse(phase0.isFirst());

        phase0.setPreviousPhase(null);
        assertTrue(phase0.isFirst());

        phase0.setNextPhase(null);
        assertTrue(phase0.isLast());

        phase0.setNextPhase(phase1);
        assertFalse(phase0.isLast());

        phase0.setNextPhase(null);
        assertTrue(phase0.isLast());
    }
}