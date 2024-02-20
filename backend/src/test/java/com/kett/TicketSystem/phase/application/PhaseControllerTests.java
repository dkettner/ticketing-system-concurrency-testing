package com.kett.TicketSystem.phase.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.kett.TicketSystem.phase.application.dto.PhasePutNameDto;
import com.kett.TicketSystem.phase.application.dto.PhasePutPositionDto;
import com.kett.TicketSystem.phase.application.dto.PhasePostDto;
import com.kett.TicketSystem.phase.domain.Phase;
import com.kett.TicketSystem.phase.domain.PhaseDomainService;
import com.kett.TicketSystem.phase.domain.events.PhaseCreatedEvent;
import com.kett.TicketSystem.phase.domain.events.PhaseDeletedEvent;
import com.kett.TicketSystem.phase.domain.exceptions.NoPhaseFoundException;
import com.kett.TicketSystem.phase.repository.PhaseRepository;
import com.kett.TicketSystem.project.domain.events.DefaultProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectDeletedEvent;
import com.kett.TicketSystem.project.repository.ProjectRepository;
import com.kett.TicketSystem.ticket.domain.events.TicketCreatedEvent;
import com.kett.TicketSystem.ticket.domain.events.TicketDeletedEvent;
import com.kett.TicketSystem.ticket.domain.events.TicketPhaseUpdatedEvent;
import com.kett.TicketSystem.user.repository.UserRepository;
import com.kett.TicketSystem.util.EventCatcher;
import com.kett.TicketSystem.util.RestRequestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({ "test" })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class PhaseControllerTests {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final RestRequestHelper restMinion;
    private final PhaseDomainService phaseDomainService;
    private final PhaseRepository phaseRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private String userName;
    private String userEmail;
    private String userPassword;
    private UUID userId;
    private String jwt;

    private String buildUpProjectName;
    private String buildUpProjectDescription;
    private UUID buildUpProjectId;

    private String differentProjectDescription;
    private String differentProjectName;

    private String phaseName0;
    private String phaseName1;
    private String phaseName2;
    private String phaseName3;

    @Autowired
    public PhaseControllerTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            PhaseDomainService phaseDomainService,
            PhaseRepository phaseRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.restMinion = new RestRequestHelper(mockMvc, objectMapper);
        this.phaseDomainService = phaseDomainService;
        this.phaseRepository = phaseRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @BeforeEach
    public void buildUp() throws Exception {
        userName = "Geralt";
        userEmail = "il.brucho@netflix.com";
        userPassword = "DiesDasAnanasiospjefosias9999023";
        userId = restMinion.postUser(userName, userEmail, userPassword);
        jwt = restMinion.authenticateUser(userEmail, userPassword);

        buildUpProjectName = "toss a coin to your witcher";
        buildUpProjectDescription = "50ct please";
        buildUpProjectId = restMinion.postProject(jwt, buildUpProjectName, buildUpProjectDescription);

        differentProjectName = "Stormcloaks";
        differentProjectDescription = "Not the Imperial Legion.";

        phaseName0 = "phaseName0";
        phaseName1 = "phaseName1";
        phaseName2 = "phaseName2";
        phaseName3 = "phaseName3";
    }

    @AfterEach
    public void tearDown() {
        userName = null;
        userEmail = null;
        userPassword = null;
        userId = null;
        jwt = null;

        buildUpProjectName = null;
        buildUpProjectDescription = null;
        buildUpProjectId = null;

        differentProjectName = null;
        differentProjectDescription = null;

        phaseName0 = null;
        phaseName1 = null;
        phaseName2 = null;
        phaseName3 = null;

        phaseRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void getPhaseByIdTest() throws Exception {
        // Arrange
        UUID phaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);
        Phase phase = phaseDomainService.getPhaseById(phaseId);

        // Act & Assert
        MvcResult getResult =
                mockMvc.perform(
                                get("/phases/" + phaseId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(phaseId.toString()))
                        .andExpect(jsonPath("$.projectId").value(buildUpProjectId.toString()))
                        .andExpect(jsonPath("$.name").value(phaseName0))
                        .andExpect(jsonPath("$.previousPhaseId").isEmpty())
                        .andExpect(jsonPath("$.nextPhaseId").exists())
                        .andExpect(jsonPath("$.ticketCount").value(0))
                        .andReturn();
    }

    @Test
    public void getPhasesByQueryTest() throws Exception {
        // Arrange
        UUID phaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);
        List<Phase> phases = phaseDomainService.getPhasesByProjectId(buildUpProjectId);

        // Act & Assert
        // defaultPhase + new phase = 2
        MvcResult getResult =
                mockMvc.perform(
                                get("/phases")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .queryParam("project-id", buildUpProjectId.toString())
                                        .header("Authorization", jwt))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").isArray())
                        .andExpect(jsonPath("$[0].id").value(phases.get(0).getId().toString()))
                        .andExpect(jsonPath("$[0].projectId").value(phases.get(0).getProjectId().toString()))
                        .andExpect(jsonPath("$[0].name").value(phases.get(0).getName()))
                        .andExpect(jsonPath("$[0].previousPhaseId").value(phases.get(0).getPreviousPhase().getId().toString()))
                        .andExpect(jsonPath("$[0].nextPhaseId").isEmpty())
                        .andExpect(jsonPath("$[0].ticketCount").value(phases.get(0).getTicketCount()))
                        .andExpect(jsonPath("$[1].id").value(phases.get(1).getId().toString()))
                        .andExpect(jsonPath("$[1].projectId").value(phases.get(1).getProjectId().toString()))
                        .andExpect(jsonPath("$[1].name").value(phases.get(1).getName()))
                        .andExpect(jsonPath("$[1].previousPhaseId").isEmpty())
                        .andExpect(jsonPath("$[1].nextPhaseId").value(phases.get(1).getNextPhase().getId().toString()))
                        .andExpect(jsonPath("$[1].ticketCount").value(phases.get(1).getTicketCount()))
                        .andReturn();
    }

    @Test
    public void postPhaseToNewProjectTest() throws Exception {
        // Arrange
        // post to first place
        PhasePostDto phasePostDto0 = new PhasePostDto(buildUpProjectId, phaseName0, null);

        // Act & Assert
        MvcResult postResult0 =
                mockMvc.perform(
                                post("/phases")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(phasePostDto0))
                                        .header("Authorization", jwt))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.projectId").value(buildUpProjectId.toString()))
                        .andExpect(jsonPath("$.name").value(phaseName0))
                        .andExpect(jsonPath("$.previousPhaseId").isEmpty())
                        .andExpect(jsonPath("$.nextPhaseId").exists())
                        .andExpect(jsonPath("$.ticketCount").value(0))
                        .andReturn();

        // second stage

        // Arrange
        String postResponse0 = postResult0.getResponse().getContentAsString();
        UUID phaseId0 = UUID.fromString(JsonPath.parse(postResponse0).read("$.id"));
        UUID nextPhaseId = UUID.fromString(JsonPath.parse(postResponse0).read("$.nextPhaseId"));
        // post to second place
        PhasePostDto phasePostDto1 = new PhasePostDto(buildUpProjectId, phaseName1, phaseId0);

        // Act & Assert
        MvcResult postResult1 =
                mockMvc.perform(
                                post("/phases")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(phasePostDto1))
                                        .header("Authorization", jwt))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.projectId").value(buildUpProjectId.toString()))
                        .andExpect(jsonPath("$.name").value(phaseName1))
                        .andExpect(jsonPath("$.previousPhaseId").value(phaseId0.toString()))
                        .andExpect(jsonPath("$.nextPhaseId").value(nextPhaseId.toString()))
                        .andExpect(jsonPath("$.ticketCount").value(0))
                        .andReturn();
    }

    @Test
    public void deletePhaseTest() throws Exception {
        // Arrange
        restMinion.postPhase(jwt, buildUpProjectId, phaseName1, null);
        restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);

        // test initial state
        List<Phase> initialPhases = phaseDomainService.getPhasesByProjectId(buildUpProjectId);
        assertEquals(3, initialPhases.size());

        assertEquals("BACKLOG", initialPhases.get(0).getName());
        UUID backlogId = initialPhases.get(0).getId();
        assertFalse(initialPhases.get(0).isFirst());
        assertTrue(initialPhases.get(0).isLast());
        assertEquals(initialPhases.get(1).getId(), initialPhases.get(0).getPreviousPhase().getId());
        assertNull(initialPhases.get(0).getNextPhase());

        assertEquals(phaseName1, initialPhases.get(1).getName());
        UUID phaseId1 = initialPhases.get(1).getId();
        assertFalse(initialPhases.get(1).isFirst());
        assertFalse(initialPhases.get(1).isLast());
        assertEquals(initialPhases.get(2).getId(), initialPhases.get(1).getPreviousPhase().getId());
        assertEquals(initialPhases.get(0).getId(), initialPhases.get(1).getNextPhase().getId());

        assertEquals(phaseName0, initialPhases.get(2).getName());
        UUID phaseId0 = initialPhases.get(2).getId();
        assertTrue(initialPhases.get(2).isFirst());
        assertFalse(initialPhases.get(2).isLast());
        assertNull(initialPhases.get(2).getPreviousPhase());
        assertEquals(initialPhases.get(1).getId(), initialPhases.get(2).getNextPhase().getId());

        // Act & Assert
        // delete middle -> phaseId1
        MvcResult deleteResult0 =
                mockMvc.perform(
                                delete("/phases/" + phaseId1)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt))
                        .andExpect(status().isNoContent())
                        .andReturn();

        // test other phases after delete
        List<Phase> phasesAfterFirstDelete = phaseDomainService.getPhasesByProjectId(buildUpProjectId);
        assertEquals(2, phasesAfterFirstDelete.size());
        assertEquals(backlogId, phasesAfterFirstDelete.get(0).getId());
        assertTrue(phasesAfterFirstDelete.get(0).isLast());
        assertEquals(phasesAfterFirstDelete.get(0).getPreviousPhase().getId(), phasesAfterFirstDelete.get(1).getId());
        assertNull(phasesAfterFirstDelete.get(0).getNextPhase());
        assertEquals(phaseId0, phasesAfterFirstDelete.get(1).getId());
        assertTrue(phasesAfterFirstDelete.get(1).isFirst());
        assertEquals(phasesAfterFirstDelete.get(1).getNextPhase().getId(), phasesAfterFirstDelete.get(0).getId());
        assertNull(phasesAfterFirstDelete.get(1).getPreviousPhase());

        // second stage
        // Arrange
        // nothing to arrange

        // Act & Assert
        // delete last -> backlogId
        MvcResult deleteResult1 =
                mockMvc.perform(
                                delete("/phases/" + backlogId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt))
                        .andExpect(status().isNoContent())
                        .andReturn();

        // test other phases after delete
        List<Phase> phasesAfterSecondDelete = phaseDomainService.getPhasesByProjectId(buildUpProjectId);
        assertEquals(1, phasesAfterSecondDelete.size());
        assertEquals(phaseId0, phasesAfterSecondDelete.get(0).getId());
        assertTrue(phasesAfterSecondDelete.get(0).isFirst());
        assertTrue(phasesAfterSecondDelete.get(0).isLast());
        assertNull(phasesAfterSecondDelete.get(0).getPreviousPhase());
        assertNull(phasesAfterSecondDelete.get(0).getNextPhase());

        // third stage
        // Arrange
        // nothing to arrange

        // Act & Assert
        // delete remaining phase -> phaseId0
        MvcResult deleteResult2 =
                mockMvc.perform(
                                delete("/phases/" + phaseId0)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt))
                        .andExpect(status().isConflict())
                        .andReturn();
    }

    @Test
    public void putPhaseNameTest() throws Exception {
        // Arrange
        List<Phase> initialPhases = phaseDomainService.getPhasesByProjectId(buildUpProjectId);
        assertEquals(1, initialPhases.size());
        assertEquals("BACKLOG", initialPhases.get(0).getName());
        UUID backlogId = initialPhases.get(0).getId();
        String newName = "Hola que tal";
        PhasePutNameDto phasePutNameDto = new PhasePutNameDto(newName);

        // Act & Assert
        MvcResult putResult =
                mockMvc.perform(
                                put("/phases/" + backlogId + "/name")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(phasePutNameDto))
                                        .header("Authorization", jwt))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    @Test
    public void putPhasePositionFirstToLastTest() throws Exception {
        // Arrange
        restMinion.postPhase(jwt, buildUpProjectId, phaseName1, null);
        restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);

        // test initial state
        List<Phase> initialPhases = phaseDomainService.getPhasesByProjectId(buildUpProjectId);
        assertEquals(3, initialPhases.size());

        assertEquals("BACKLOG", initialPhases.get(0).getName());
        UUID backlogId = initialPhases.get(0).getId();
        assertFalse(initialPhases.get(0).isFirst());
        assertTrue(initialPhases.get(0).isLast());
        assertEquals(initialPhases.get(1).getId(), initialPhases.get(0).getPreviousPhase().getId());
        assertNull(initialPhases.get(0).getNextPhase());

        assertEquals(phaseName1, initialPhases.get(1).getName());
        UUID phaseId1 = initialPhases.get(1).getId();
        assertFalse(initialPhases.get(1).isFirst());
        assertFalse(initialPhases.get(1).isLast());
        assertEquals(initialPhases.get(2).getId(), initialPhases.get(1).getPreviousPhase().getId());
        assertEquals(initialPhases.get(0).getId(), initialPhases.get(1).getNextPhase().getId());

        assertEquals(phaseName0, initialPhases.get(2).getName());
        UUID phaseId0 = initialPhases.get(2).getId();
        assertTrue(initialPhases.get(2).isFirst());
        assertFalse(initialPhases.get(2).isLast());
        assertNull(initialPhases.get(2).getPreviousPhase());
        assertEquals(initialPhases.get(1).getId(), initialPhases.get(2).getNextPhase().getId());

        // Act & Assert
        // move first to last
        PhasePutPositionDto phasePutPositionDto = new PhasePutPositionDto(backlogId);
        MvcResult putResult =
                mockMvc.perform(
                                put("/phases/" + phaseId0 + "/position")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(phasePutPositionDto))
                                        .header("Authorization", jwt))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    // new tests

    @Test
    public void testGetPhaseByIdNotFound() throws Exception {
        // Arrange
        UUID phaseId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(
                get("/phases/" + phaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetPhaseByIdWithInvalidJWT() throws Exception {
        // Arrange
        UUID phaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);
        String wrongJwt = "Bearer wrongJwt";

        // Act & Assert
        mockMvc.perform(
                        get("/phases/" + phaseId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", wrongJwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetPhaseByIdUnauthorized() throws Exception {
        // Arrange
        String differentUserName = "Different User";
        String differentUserEmail = "differen@user.com";
        String differentUserPassword = "DifferentPassword";
        UUID differentUserId = restMinion.postUser(differentUserName, differentUserEmail, differentUserPassword);
        String differentUserJwt = restMinion.authenticateUser(differentUserEmail, differentUserPassword);
        UUID phaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);

        // Act & Assert
        mockMvc.perform(
                        get("/phases/" + phaseId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", differentUserJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetPhasesByQueryNoProjectId() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        mockMvc.perform(
                get("/phases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", jwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetPhasesByQueryNotFound() throws Exception {
        // Arrange
        UUID projectId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(
                get("/phases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("project-id", projectId.toString())
                        .header("Authorization", jwt))
                .andExpect(status().isForbidden()); // fails early in security
    }

    @Test
    public void testPostPhaseToNonExistentProject() throws Exception {
        // Arrange
        UUID projectId = UUID.randomUUID();
        PhasePostDto phasePostDto = new PhasePostDto(projectId, phaseName0, null);

        // Act & Assert
        mockMvc.perform(
                post("/phases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phasePostDto))
                        .header("Authorization", jwt))
                .andExpect(status().isForbidden()); // fails early in security
    }

    @Test
    public void testPostPhaseUnauthorized() throws Exception {
        // Arrange
        String differentUserName = "Different User";
        String differentUserEmail = "differen@user.com";
        String differentUserPassword = "DifferentPassword";
        UUID differentUserId = restMinion.postUser(differentUserName, differentUserEmail, differentUserPassword);
        String differentUserJwt = restMinion.authenticateUser(differentUserEmail, differentUserPassword);
        UUID phaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);
        PhasePostDto phasePostDto = new PhasePostDto(buildUpProjectId, phaseName0, null);

        // Act & Assert
        mockMvc.perform(
                        post("/phases")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(phasePostDto))
                                .header("Authorization", differentUserJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testPostPhaseWithNotExistingPreviousPhase() throws Exception {
        // Arrange
        UUID unrelatedPhaseId = UUID.randomUUID();
        PhasePostDto phasePostDto = new PhasePostDto(buildUpProjectId, "somePhaseName", unrelatedPhaseId);

        // Act & Assert
        mockMvc.perform(
                post("/phases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phasePostDto))
                        .header("Authorization", jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testPostPhaseWithUnrelatedPreviousPhase() throws Exception {
        // Arrange
        UUID otherProjectId = restMinion.postProject(jwt, differentProjectName, differentProjectDescription);
        UUID unrelatedPhaseId = restMinion.postPhase(jwt, otherProjectId, "someUnrelatedPhaseName", null);
        PhasePostDto phasePostDto = new PhasePostDto(buildUpProjectId, "somePhaseName", unrelatedPhaseId);

        // Act & Assert
        mockMvc.perform(
                post("/phases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phasePostDto))
                        .header("Authorization", jwt))
                .andExpect(status().isConflict());
    }

    @Test
    public void testPutPhaseNameUnauthorized() throws Exception {
        // Arrange
        String differentUserName = "Different User";
        String differentUserEmail = "differen@user.com";
        String differentUserPassword = "DifferentPassword";
        UUID differentUserId = restMinion.postUser(differentUserName, differentUserEmail, differentUserPassword);
        String differentUserJwt = restMinion.authenticateUser(differentUserEmail, differentUserPassword);
        UUID phaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);
        PhasePostDto phasePatchNameDto = new PhasePostDto(buildUpProjectId, "newPhaseName", null);

        // Act & Assert
        mockMvc.perform(
                        put("/phases/" + phaseId + "/name")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(phasePatchNameDto))
                                .header("Authorization", differentUserJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testPutPhaseNameNotFound() throws Exception {
        // Arrange
        UUID phaseId = UUID.randomUUID();
        PhasePostDto phasePatchNameDto = new PhasePostDto(buildUpProjectId, "newPhaseName", null);

        // Act & Assert
        mockMvc.perform(
                put("/phases/" + phaseId + "/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phasePatchNameDto))
                        .header("Authorization", jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testPutPhaseNameEmptyName() throws Exception {
        // Arrange
        UUID phaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);
        PhasePostDto phasePatchNameDto = new PhasePostDto(buildUpProjectId, "", null);

        // Act & Assert
        mockMvc.perform(
                put("/phases/" + phaseId + "/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phasePatchNameDto))
                        .header("Authorization", jwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPutPhasePositionUnauthorized() throws Exception {
        // Arrange
        String differentUserName = "Different User";
        String differentUserEmail = "differen@user.com";
        String differentUserPassword = "DifferentPassword";
        UUID differentUserId = restMinion.postUser(differentUserName, differentUserEmail, differentUserPassword);
        String differentUserJwt = restMinion.authenticateUser(differentUserEmail, differentUserPassword);
        UUID phaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);
        PhasePostDto phasePatchNameDto = new PhasePostDto(buildUpProjectId, "newPhaseName", null);

        // Act & Assert
        mockMvc.perform(
                        put("/phases/" + phaseId + "/position")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(phasePatchNameDto))
                                .header("Authorization", differentUserJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testPutPhasePositionNotFound() throws Exception {
        // Arrange
        UUID phaseId = UUID.randomUUID();
        PhasePostDto phasePatchNameDto = new PhasePostDto(buildUpProjectId, "newPhaseName", null);

        // Act & Assert
        mockMvc.perform(
                put("/phases/" + phaseId + "/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phasePatchNameDto))
                        .header("Authorization", jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testPutPhasePositionWithNotExistingPreviousPhase() throws Exception {
        // Arrange
        UUID unrelatedPhaseId = UUID.randomUUID();
        PhasePostDto phasePatchNameDto = new PhasePostDto(buildUpProjectId, "newPhaseName", unrelatedPhaseId);

        // Act & Assert
        mockMvc.perform(
                put("/phases/" + unrelatedPhaseId + "/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phasePatchNameDto))
                        .header("Authorization", jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testPutPhasePositionWithUnrelatedPreviousPhase() throws Exception {
        // Arrange
        UUID newPhaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);
        UUID otherProjectId = restMinion.postProject(jwt, differentProjectName, differentProjectDescription);
        UUID unrelatedPhaseId = restMinion.postPhase(jwt, otherProjectId, "someUnrelatedPhaseName", null);
        PhasePutPositionDto phasePutPositionDto = new PhasePutPositionDto(unrelatedPhaseId);

        // Act & Assert
        mockMvc.perform(
                put("/phases/" + newPhaseId + "/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phasePutPositionDto))
                        .header("Authorization", jwt))
                .andExpect(status().isConflict());
    }

    @Test
    public void testDeletePhaseUnauthorized() throws Exception {
        // Arrange
        String differentUserName = "Different User";
        String differentUserEmail = "differen@user.com";
        String differentUserPassword = "DifferentPassword";
        UUID differentUserId = restMinion.postUser(differentUserName, differentUserEmail, differentUserPassword);
        String differentUserJwt = restMinion.authenticateUser(differentUserEmail, differentUserPassword);
        UUID phaseId = restMinion.postPhase(jwt, buildUpProjectId, phaseName0, null);

        // Act & Assert
        mockMvc.perform(
                        delete("/phases/" + phaseId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", differentUserJwt))
                .andExpect(status().isForbidden());
    }
}
