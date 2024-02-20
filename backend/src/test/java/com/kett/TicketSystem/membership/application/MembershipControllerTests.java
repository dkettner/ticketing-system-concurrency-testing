package com.kett.TicketSystem.membership.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.kett.TicketSystem.membership.application.dto.MembershipPostDto;
import com.kett.TicketSystem.membership.application.dto.MembershipPutRoleDto;
import com.kett.TicketSystem.membership.application.dto.MembershipPutStateDto;
import com.kett.TicketSystem.membership.domain.Membership;
import com.kett.TicketSystem.membership.domain.MembershipDomainService;
import com.kett.TicketSystem.membership.domain.Role;
import com.kett.TicketSystem.membership.domain.State;
import com.kett.TicketSystem.membership.domain.events.MembershipAcceptedEvent;
import com.kett.TicketSystem.membership.domain.events.MembershipDeletedEvent;
import com.kett.TicketSystem.membership.domain.events.UnacceptedProjectMembershipCreatedEvent;
import com.kett.TicketSystem.membership.domain.exceptions.NoMembershipFoundException;
import com.kett.TicketSystem.membership.repository.MembershipRepository;
import com.kett.TicketSystem.project.domain.events.DefaultProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectDeletedEvent;
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
import java.util.UUID;

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
public class MembershipControllerTests {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final EventCatcher eventCatcher;
    private final RestRequestHelper restMinion;
    private final UserRepository userRepository;
    private final MembershipDomainService membershipDomainService;
    private final MembershipRepository membershipRepository;

    private UUID userId0;
    private String userName0;
    private String userEmail0;
    private String userPassword0;
    private String jwt0;
    private UUID defaultProjectId0;

    private UUID userId1;
    private String userName1;
    private String userEmail1;
    private String userPassword1;
    private String jwt1;
    private UUID defaultProjectId1;

    private UUID randomProjectId;

    @Autowired
    public MembershipControllerTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            EventCatcher eventCatcher,
            UserRepository userRepository,
            MembershipDomainService membershipDomainService,
            MembershipRepository membershipRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.eventCatcher = eventCatcher;
        this.restMinion = new RestRequestHelper(mockMvc, objectMapper);
        this.userRepository = userRepository;
        this.membershipDomainService = membershipDomainService;
        this.membershipRepository = membershipRepository;
    }

    @BeforeEach
    public void buildUp() throws Exception {
        userName0 = "Harry Potter";
        userEmail0 = "harry.potter@hw.uk";
        userPassword0 = "snapeShape88";

        eventCatcher.catchEventOfType(DefaultProjectCreatedEvent.class);
        userId0 = restMinion.postUser(userName0, userEmail0, userPassword0);
        await().until(eventCatcher::hasCaughtEvent);
        defaultProjectId0 = ((DefaultProjectCreatedEvent) eventCatcher.getEvent()).getProjectId();

        jwt0 = restMinion.authenticateUser(userEmail0, userPassword0);

        userName1 = "Ronald Weasley";
        userEmail1 = "RonRonRonWeasley@hw.uk";
        userPassword1 = "lkasjdfoijwaefo8238298";

        eventCatcher.catchEventOfType(DefaultProjectCreatedEvent.class);
        userId1 = restMinion.postUser(userName1, userEmail1, userPassword1);
        await().until(eventCatcher::hasCaughtEvent);
        defaultProjectId1 = ((DefaultProjectCreatedEvent) eventCatcher.getEvent()).getProjectId();

        jwt1 = restMinion.authenticateUser(userEmail1, userPassword1);

        randomProjectId = UUID.randomUUID();

        //dummyEventListener.deleteAllEvents();
    }

    @AfterEach
    public void tearDown() {
        userName0 = null;
        userEmail0 = null;
        userPassword0 = null;
        userId0 = null;
        jwt0 = null;
        defaultProjectId0 = null;

        userName1 = null;
        userEmail1 = null;
        userPassword1 = null;
        userId1 = null;
        jwt1 = null;
        defaultProjectId1 = null;

        randomProjectId = null;

        membershipRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void getMembershipByIdTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);

        // Act & Assert
        MvcResult getResult =
                mockMvc.perform(
                                get("/memberships/" + membershipId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt1))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(membershipId.toString()))
                        .andExpect(jsonPath("$.projectId").value(projectId0.toString()))
                        .andExpect(jsonPath("$.userId").value(userId1.toString()))
                        .andExpect(jsonPath("$.role").value(Role.MEMBER.toString()))
                        .andExpect(jsonPath("$.state").value(State.OPEN.toString()))
                        .andReturn();
    }

    @Test
    public void getMembershipByIdAsAdminTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);

        // Act & Assert
        MvcResult getResult =
                mockMvc.perform(
                                get("/memberships/" + membershipId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt0))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(membershipId.toString()))
                        .andExpect(jsonPath("$.projectId").value(projectId0.toString()))
                        .andExpect(jsonPath("$.userId").value(userId1.toString()))
                        .andExpect(jsonPath("$.role").value(Role.MEMBER.toString()))
                        .andExpect(jsonPath("$.state").value(State.OPEN.toString()))
                        .andReturn();
    }

    @Test
    public void getMembershipsByUserIdAndEmailQueryTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);

        // Act & Assert
        MvcResult getByUserIdResult =
                mockMvc.perform(
                                get("/memberships")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .queryParam("user-id", userId0.toString())
                                        .header("Authorization", jwt0))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].id").exists())
                        .andExpect(jsonPath("$[0].userId").value(userId0.toString()))
                        .andExpect(jsonPath("$[0].projectId").value(defaultProjectId0.toString()))
                        .andExpect(jsonPath("$[0].role").value(Role.ADMIN.toString()))
                        .andExpect(jsonPath("$[0].state").value(State.ACCEPTED.toString()))
                        .andExpect(jsonPath("$[1].id").exists())
                        .andExpect(jsonPath("$[1].projectId").value(projectId0.toString()))
                        .andExpect(jsonPath("$[1].userId").value(userId0.toString()))
                        .andExpect(jsonPath("$[1].role").value(Role.ADMIN.toString()))
                        .andExpect(jsonPath("$[1].state").value(State.ACCEPTED.toString()))
                        .andReturn();

        // stage 2

        // Arrange
        // nothing to arrange

        // Act & Assert
        MvcResult getByEmailResult =
                mockMvc.perform(
                                get("/memberships")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .queryParam("email", userEmail0)
                                        .header("Authorization", jwt0))
                        .andExpect(status().isOk())
                        .andReturn();
        assertEquals(getByUserIdResult.getResponse().getContentAsString(), getByEmailResult.getResponse().getContentAsString());
    }

    @Test
    public void getMembershipsByProjectIdQueryTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);

        // Act & Assert
        MvcResult getResult =
                mockMvc.perform(
                                get("/memberships")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .queryParam("project-id", projectId0.toString())
                                        .header("Authorization", jwt0))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].id").exists())
                        .andExpect(jsonPath("$[0].userId").value(userId0.toString()))
                        .andExpect(jsonPath("$[0].projectId").value(projectId0.toString()))
                        .andExpect(jsonPath("$[0].role").value(Role.ADMIN.toString()))
                        .andExpect(jsonPath("$[0].state").value(State.ACCEPTED.toString()))
                        .andExpect(jsonPath("$[1].id").value(membershipId.toString()))
                        .andExpect(jsonPath("$[1].projectId").value(projectId0.toString()))
                        .andExpect(jsonPath("$[1].userId").value(userId1.toString()))
                        .andExpect(jsonPath("$[1].role").value(Role.MEMBER.toString()))
                        .andExpect(jsonPath("$[1].state").value(State.OPEN.toString()))
                        .andReturn();
    }

    @Test
    public void postMembershipTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        MembershipPostDto membershipPostDto = new MembershipPostDto(projectId0, userId1, Role.MEMBER);

        // Act & Assert
        MvcResult postResult =
                mockMvc.perform(
                                post("/memberships")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(membershipPostDto))
                                        .header("Authorization", jwt0))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.projectId").value(membershipPostDto.getProjectId().toString()))
                        .andExpect(jsonPath("$.userId").value(membershipPostDto.getUserId().toString()))
                        .andExpect(jsonPath("$.role").value(membershipPostDto.getRole().toString()))
                        .andExpect(jsonPath("$.state").value(State.OPEN.toString()))
                        .andReturn();
    }

    @Test
    public void putMembershipStateTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);
        MembershipPutStateDto membershipPutStateDto = new MembershipPutStateDto(State.ACCEPTED);

        // Act & Assert
        MvcResult putResult =
                mockMvc.perform(
                                put("/memberships/" + membershipId + "/state")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(membershipPutStateDto))
                                        .header("Authorization", jwt1))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    @Test
    public void putMembershipStateBackToOpenTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);
        MembershipPutStateDto membershipPutStateDto0 = new MembershipPutStateDto(State.ACCEPTED);

        // Act & Assert
        MvcResult putResult0 =
                mockMvc.perform(
                                put("/memberships/" + membershipId + "/state")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(membershipPutStateDto0))
                                        .header("Authorization", jwt1))
                        .andExpect(status().isNoContent())
                        .andReturn();

        // stage 2

        // Arrange
        MembershipPutStateDto membershipPutStateDto1 = new MembershipPutStateDto(State.OPEN);

        // Act & Assert
        // try to go back to open
        MvcResult putResult1 =
                mockMvc.perform(
                                put("/memberships/" + membershipId + "/state")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(membershipPutStateDto1))
                                        .header("Authorization", jwt1))
                        .andExpect(status().isConflict())
                        .andReturn();
    }

    @Test
    public void putMembershipStateBackToOpenAsAdminTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);
        MembershipPutStateDto membershipPutStateDto0 = new MembershipPutStateDto(State.ACCEPTED);

        // Act & Assert
        // accept
        MvcResult putResult0 =
                mockMvc.perform(
                                put("/memberships/" + membershipId + "/state")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(membershipPutStateDto0))
                                        .header("Authorization", jwt1))
                        .andExpect(status().isNoContent())
                        .andReturn();

        // stage 2

        // Arrange
        MembershipPutStateDto membershipPutStateDto1 = new MembershipPutStateDto(State.OPEN);

        // Act & Assert
        // try to go back to open
        MvcResult putResult1 =
                mockMvc.perform(
                                put("/memberships/" + membershipId + "/state")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(membershipPutStateDto1))
                                        .header("Authorization", jwt0))
                        .andExpect(status().isForbidden())
                        .andReturn();
    }

    @Test
    public void putMembershipStateAsAdminTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);
        MembershipPutStateDto membershipPutStateDto = new MembershipPutStateDto(State.ACCEPTED);

        // Act & Assert
        MvcResult putResult =
                mockMvc.perform(
                                put("/memberships/" + membershipId + "/state")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(membershipPutStateDto))
                                        .header("Authorization", jwt0))
                        .andExpect(status().isForbidden())
                        .andReturn();
    }

    @Test
    public void putMembershipRoleAsAdminTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);
        MembershipPutRoleDto membershipPutStateDto = new MembershipPutRoleDto(Role.ADMIN);

        // Act & Assert
        MvcResult putResult =
                mockMvc.perform(
                                put("/memberships/" + membershipId + "/role")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(membershipPutStateDto))
                                        .header("Authorization", jwt0))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    @Test
    public void putMembershipRoleTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);
        MembershipPutRoleDto membershipPutStateDto = new MembershipPutRoleDto(Role.ADMIN);

        // Act & Assert
        MvcResult putResult =
                mockMvc.perform(
                                put("/memberships/" + membershipId + "/role")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(membershipPutStateDto))
                                        .header("Authorization", jwt1))
                        .andExpect(status().isForbidden())
                        .andReturn();
    }

    @Test
    public void deleteOtherMembershipAsAdminTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);

        // Act & Assert
        MvcResult deleteResult =
                mockMvc.perform(
                                delete("/memberships/" + membershipId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt0))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    @Test
    public void deleteOwnMembershipAsMemberTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);

        // Act & Assert
        MvcResult deleteResult =
                mockMvc.perform(
                                delete("/memberships/" + membershipId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt1))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    // new tests

    @Test
    public void testGetMembershipByIdNotFoundTest() throws Exception {
        // Arrange
        UUID membershipId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(
                get("/memberships/" + membershipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", jwt1))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetMembershipsByQueryTooManyParametersTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        mockMvc.perform(
                get("/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("user-id", userId0.toString())
                        .queryParam("project-id", defaultProjectId0.toString())
                        .header("Authorization", jwt0))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testPostMembershipProjectNotFoundTest() throws Exception {
        // Arrange
        UUID randomProjectId = UUID.randomUUID();
        MembershipPostDto membershipPostDto = new MembershipPostDto(randomProjectId, userId1, Role.MEMBER);

        // Act & Assert
        mockMvc.perform(
                post("/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(membershipPostDto))
                        .header("Authorization", jwt0))
                .andExpect(status().isForbidden()); // fail early in security
    }

    @Test
    public void testPostMembershipUserNotFoundTest() throws Exception {
        // Arrange
        UUID randomUserId = UUID.randomUUID();
        MembershipPostDto membershipPostDto = new MembershipPostDto(defaultProjectId0, randomUserId, Role.MEMBER);

        // Act & Assert
        mockMvc.perform(
                post("/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(membershipPostDto))
                        .header("Authorization", jwt0))
                .andExpect(status().isNotFound()); // fail early in security
    }

    @Test
    public void testPutMembershipRoleToMemberAsLastAdminTest() throws Exception {
        // Arrange
        MvcResult result = mockMvc.perform(get("/memberships")
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam("project-id", defaultProjectId0.toString())
                .header("Authorization", jwt0))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        String defaultProjectMembershipId = JsonPath.read(content, "$[0].id");

        // Act & Assert
        mockMvc.perform(
                put("/memberships/" + defaultProjectMembershipId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MembershipPutRoleDto(Role.MEMBER)))
                        .header("Authorization", jwt0))
                .andExpect(status().isConflict());
    }

    @Test
    public void testDeleteMembershipByIdNotFoundTest() throws Exception {
        // Arrange
        UUID membershipId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(
                delete("/memberships/" + membershipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", jwt1))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetMembershipsByQueryNoParametersTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        mockMvc.perform(
                get("/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", jwt0))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetMembershipByIdWithInvalidJwtTest() throws Exception {
        // Arrange
        String projectName0 = "Project 0";
        String projectDescription0 = "Description 0";
        UUID projectId0 = restMinion.postProject(jwt0, projectName0, projectDescription0);
        UUID membershipId = restMinion.postMembership(jwt0, projectId0, userId1, Role.MEMBER);

        // Act & Assert
        mockMvc.perform(
                get("/memberships/" + membershipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt0 + "invalid"))
                .andExpect(status().isUnauthorized());
    }
}
