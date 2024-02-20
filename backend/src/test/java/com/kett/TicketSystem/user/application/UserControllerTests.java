package com.kett.TicketSystem.user.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.kett.TicketSystem.authentication.application.dto.AuthenticationPostDto;
import com.kett.TicketSystem.common.domainprimitives.EmailAddress;
import com.kett.TicketSystem.common.exceptions.NoUserFoundException;
import com.kett.TicketSystem.user.application.dto.UserPatchDto;
import com.kett.TicketSystem.user.application.dto.UserPostDto;
import com.kett.TicketSystem.user.domain.User;
import com.kett.TicketSystem.user.domain.UserDomainService;
import com.kett.TicketSystem.user.domain.events.UserCreatedEvent;
import com.kett.TicketSystem.user.domain.events.UserDeletedEvent;
import com.kett.TicketSystem.user.domain.events.UserPatchedEvent;
import com.kett.TicketSystem.user.repository.UserRepository;
import com.kett.TicketSystem.util.EventCatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.Cookie;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({ "test" })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerTests {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    private String name0;
    private String email0;
    private String password0;

    private String name1;
    private String email1;
    private String password1;

    private String name2;
    private String email2;
    private String password2;

    private String name3;
    private String email3;
    private String password3;

    private String id4;
    private String name4;
    private String email4;
    private String password4;
    private String jwt4;


    @Autowired
    public UserControllerTests(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    @BeforeEach
    public void buildUp() throws Exception {

        // validUser0
        name0 = "Bill Gates";
        email0 = "dollar.bill@microsoft.com";
        password0 = "hereComesThe_Money1337";

        // validUser1
        name1 = "Jeff Bezos";
        email1 = "say_my_name@amazon.com";
        password1 = "ioiisdfoipsd0203kf0k";

        // invalidUser0
        name2 = "Elon Mosquito";
        email2 = "say_my_nameamazon.com"; // missing @
        password2 = "ioiisdfoipsd0203kf0k";

        // invalidUser1
        name3 = "Jeff Beach";
        email3 = "say_my_name@amazon"; // no top level domain
        password3 = "ioiisdfoipsd0203kf0k";

        // post and authenticate user for get, patch, delete tests
        name4 = "WallE";
        email4 = "shy_robot@ai.net";
        password4 = "9d9d8f0s0dfmsmdfpsdopASFASD)0a9s";

        UserPostDto dummyUserPostDto = new UserPostDto(name4, email4, password4);
        MvcResult dummyResult =
                mockMvc.perform(
                                post("/users")
                                        .content(objectMapper.writeValueAsString(dummyUserPostDto))
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andReturn();
        String dummyResponse = dummyResult.getResponse().getContentAsString();
        id4 = JsonPath.parse(dummyResponse).read("$.id");

        AuthenticationPostDto authenticationPostDto4 = new AuthenticationPostDto(email4, password4);
        MvcResult postAuthenticationResult4 =
                mockMvc.perform(
                                post("/authentications")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(authenticationPostDto4)))
                        .andExpect(status().isOk())
                        .andReturn();
        jwt4 = "Bearer " + Objects.requireNonNull(postAuthenticationResult4.getResponse().getContentAsString());
    }

    @AfterEach
    public void tearDown() {
        name0 = null;
        email0 = null;
        password0 = null;

        name1 = null;
        email1 = null;
        password1 = null;

        name2 = null;
        email2 = null;
        password2 = null;

        name3 = null;
        email3 = null;
        password3 = null;

        id4 = null;
        name4 = null;
        email4 = null;
        password4 = null;
        jwt4 = null;

        userRepository.deleteAll();
    }

    @Test
    public void testPostUser() throws Exception {
        // Arrange
        String name = "Default User";
        String email = "default.user@mail.com";
        String password = "SWORDFISH";
        UserPostDto userPostDto = new UserPostDto(name, email, password);
        String postContent = objectMapper.writeValueAsString(userPostDto);

        // Act
        mockMvc.perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(postContent))
                .andExpect(status().isCreated())                         // Assert
                .andExpect(jsonPath("$.id").value(              //check if id is valid UUID
                        UUID.fromString(jsonPath("$.id").toString())
                                .toString())
                )
                .andExpect(jsonPath("$.name").value(userPostDto.getName()))
                .andExpect(jsonPath("$.email").value(userPostDto.getEmail()));
    }


    @Test
    public void postInvalidUserTests() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert stage 1
        // invalidUser0
        UserPostDto invalidUser0PostDto = new UserPostDto(name2, email2, password2);
        mockMvc.perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidUser0PostDto)))
                .andExpect(status().isBadRequest());

        // Act & Assert stage 2
        // invalidUser1
        UserPostDto invalidUser1PostDto = new UserPostDto(name3, email3, password3);
        mockMvc.perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidUser1PostDto)))
                .andExpect(status().isBadRequest());

        // Act & Assert stage 3
        // invalidUser2
        UserPostDto invalidUser2PostDto = new UserPostDto(null, email0, password0);
        mockMvc.perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidUser2PostDto)))
                .andExpect(status().isBadRequest());

        // Act & Assert stage 4
        // invalidUser3
        UserPostDto invalidUser3PostDto = new UserPostDto(name0, null, password0);
        mockMvc.perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidUser3PostDto)))
                .andExpect(status().isBadRequest());

        // Act & Assert stage 5
        // invalidUser4
        UserPostDto invalidUser4PostDto = new UserPostDto(name0, email0, null);
        mockMvc.perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidUser4PostDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void postDuplicateUserTest() throws Exception {
        // Arrange
        // duplicate email, email4 already exists because of buildUp()
        UserPostDto duplicatePostDto = new UserPostDto(name0, email4, password0);

        // Act & Assert
        mockMvc.perform(
                        post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicatePostDto)))
                .andExpect(status().isConflict());
    }

    @Test
    public void getUserByIdTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                get("/users/" + id4)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt4))
                        .andExpect(status().isOk())
                        .andReturn();
        String response = result.getResponse().getContentAsString();

        assertEquals(id4, JsonPath.parse(response).read("$.id"));
        assertEquals(name4, JsonPath.parse(response).read("$.name"));
        assertEquals(email4, JsonPath.parse(response).read("$.email"));
    }

    @Test
    public void getUserByWrongIdTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                get("/users/" + UUID.randomUUID())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt4))
                        .andExpect(status().isNotFound())
                        .andReturn();
    }

    @Test
    public void getUserByEmailQueryTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                get("/users" )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt4)
                                        .queryParam("email", email4))
                        .andExpect(status().isOk())
                        .andReturn();
        String response = result.getResponse().getContentAsString();

        assertEquals(id4, JsonPath.parse(response).read("$.id"));
        assertEquals(name4, JsonPath.parse(response).read("$.name"));
        assertEquals(email4, JsonPath.parse(response).read("$.email"));
    }

    @Test
    public void getUserByWrongEmailQueryTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                get("/users" )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt4)
                                        .queryParam("email", "hola.quetal@espanol.com"))
                        .andExpect(status().isNotFound())
                        .andReturn();
    }

    @Test
    public void getOtherUserByEmailQueryTest() throws Exception {
        // Arrange
        UserPostDto validUser0PostDto = new UserPostDto(name0, email0, password0);

        // Act & Assert
        MvcResult result0 =
                mockMvc.perform(
                                post("/users")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(validUser0PostDto)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.name").value(validUser0PostDto.getName()))
                        .andExpect(jsonPath("$.email").value(validUser0PostDto.getEmail()))
                        .andReturn();
        String response0 = result0.getResponse().getContentAsString();
        String id0 = JsonPath.parse(response0).read("$.id");

        // stage 2

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                get("/users" )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt4)
                                        .queryParam("email", email0))
                        .andExpect(status().isOk())
                        .andReturn();
        String response = result.getResponse().getContentAsString();

        assertEquals(id0, JsonPath.parse(response).read("$.id"));
        assertEquals(name0, JsonPath.parse(response).read("$.name"));
        assertEquals(email0, JsonPath.parse(response).read("$.email"));
    }

    @Test
    public void patchUserNameTest() throws Exception {
        // Arrange
        UserPatchDto userPatchDto = new UserPatchDto("Donald Trump", null);

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                patch("/users/" + id4 )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(userPatchDto))
                                        .header("Authorization", jwt4))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    @Test
    public void patchUserEmailTest() throws Exception {
        // Arrange
        UserPatchDto userPatchDto = new UserPatchDto(null, "agent.orange@truth.net");

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                patch("/users/" + id4 )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(userPatchDto))
                                        .header("Authorization", jwt4))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    @Test
    public void patchUserNameAndEmailTest() throws Exception {
        // Arrange
        UserPatchDto userPatchDto = new UserPatchDto("Donald Trump", "agent.orange@truth.net");

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                patch("/users/" + id4 )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(userPatchDto))
                                        .header("Authorization", jwt4))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    @Test
    public void patchUserWithNullDtoEmailTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                patch("/users/" + id4)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt4))
                        .andExpect(status().isBadRequest())
                        .andReturn();
    }

    @Test
    public void patchUserWithFaultyEmailTest() throws Exception {
        // Arrange
        UserPatchDto userPatchDto = new UserPatchDto("Donald Trump", "agent.orangetruth.net"); // no @

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                patch("/users/" + id4)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(userPatchDto))
                                        .header("Authorization", jwt4))
                        .andExpect(status().isBadRequest())
                        .andReturn();
    }

    @Test
    public void patchWrongUserTest() throws Exception {
        // Arrange
        UserPostDto validUser0PostDto = new UserPostDto(name0, email0, password0);

        // Act & Assert
        MvcResult result0 =
                mockMvc.perform(
                                post("/users")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(validUser0PostDto)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.name").value(validUser0PostDto.getName()))
                        .andExpect(jsonPath("$.email").value(validUser0PostDto.getEmail()))
                        .andReturn();

        // stage 2

        // Arrange
        String response0 = result0.getResponse().getContentAsString();
        String id0 = JsonPath.parse(response0).read("$.id");
        UserPatchDto userPatchDto = new UserPatchDto("Donald Trump", "agent.orange@truth.net");

        // Act & Assert
        MvcResult result1 =
                mockMvc.perform(
                                patch("/users/" + id0) // wrong but existing user
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(userPatchDto))
                                        .header("Authorization", jwt4))
                        .andExpect(status().isForbidden())
                        .andReturn();

        // stage 3

        // Arrange
        // nothing to arrange

        // Act & Assert
        MvcResult result2 =
                mockMvc.perform(
                                patch("/users/" + UUID.randomUUID()) // non-existing user
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(userPatchDto))
                                        .header("Authorization", jwt4))
                        .andExpect(status().isForbidden())
                        .andReturn();
    }

    @Test
    public void patchUserUnauthorizedTest() throws Exception {
        // Arrange
        UserPatchDto userPatchDto = new UserPatchDto("Donald Trump", "agent.orange@truth.net");

        // Act & Assert
        MvcResult result =
                mockMvc.perform(
                                patch("/users/" + id4)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(userPatchDto))) // no jwt
                        .andExpect(status().isUnauthorized())
                        .andReturn();
    }

    @Test
    public void deleteUserTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        // validUser1
        MvcResult result1 =
                mockMvc.perform(
                                delete("/users/" + id4)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt4))
                        .andExpect(status().isNoContent())
                        .andReturn();
    }

    @Test
    public void deleteUserUnauthorizedTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        // validUser1
        MvcResult result1 =
                mockMvc.perform(
                                delete("/users/" + id4)
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isUnauthorized())
                        .andReturn();
    }

    @Test
    public void deleteNonExistingUserTest() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        // validUser1
        MvcResult result1 =
                mockMvc.perform(
                                delete("/users/" + UUID.randomUUID())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt4))
                        .andExpect(status().isForbidden())
                        .andReturn();
    }

    @Test
    public void deleteOtherExistingUserTest() throws Exception {
        // Arrange
        // post other user
        UserPostDto dummyUserPostDto = new UserPostDto(name0, email0, password0);
        MvcResult dummyResult =
                mockMvc.perform(
                                post("/users")
                                        .content(objectMapper.writeValueAsString(dummyUserPostDto))
                                        .contentType(MediaType.APPLICATION_JSON))
                        .andReturn();
        String dummyResponse = dummyResult.getResponse().getContentAsString();
        String id0 = JsonPath.parse(dummyResponse).read("$.id");

        // Act & Assert
        // but use jwt from the wrong user
        MvcResult result1 =
                mockMvc.perform(
                                delete("/users/" + id0)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .header("Authorization", jwt4))
                        .andExpect(status().isForbidden())
                        .andReturn();
    }

    // new tests

    @Test
    public void testPatchUserWithAlreadyUsedEmail() throws Exception {
        // Arrange
        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserPostDto(name0, email0, password0))));
        UserPatchDto userPatchDto = new UserPatchDto(null, email0);

        // Act & Assert
        mockMvc.perform(
                        patch("/users/" + id4)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userPatchDto))
                                .header("Authorization", jwt4))
                .andExpect(status().isConflict());
    }

    @Test
    public void testGetUserByQueryNoParameters() throws Exception {
        // Arrange
        // nothing to arrange

        // Act & Assert
        mockMvc.perform(
                        get("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", jwt4))
                .andExpect(status().isBadRequest());
    }
}
