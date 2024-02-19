package com.kett.TicketSystem.project.domain;

import com.kett.TicketSystem.common.domainprimitives.EmailAddress;
import com.kett.TicketSystem.common.exceptions.ImpossibleException;
import com.kett.TicketSystem.common.exceptions.NoProjectFoundException;
import com.kett.TicketSystem.membership.domain.events.LastProjectMemberDeletedEvent;
import com.kett.TicketSystem.project.domain.consumedData.UserDataOfProject;
import com.kett.TicketSystem.project.domain.events.DefaultProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectCreatedEvent;
import com.kett.TicketSystem.project.domain.events.ProjectDeletedEvent;
import com.kett.TicketSystem.project.repository.ProjectRepository;
import com.kett.TicketSystem.project.repository.UserDataOfProjectRepository;
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
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ProjectDomainServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserDataOfProjectRepository userDataOfProjectRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProjectDomainService projectDomainService;

    private String projectName0;
    private String projectDescription0;
    private Project project0;

    private String projectName1;
    private String projectDescription1;
    private Project projectWithId;

    private EmailAddress testEmailAddress;
    private UserDataOfProject testUserDataOfProject;

    @BeforeEach
    public void buildUp() {

        lenient()
                .when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> {
                    Project argument = invocation.getArgument(0);
                    if (argument.getId() == null) {
                        argument.setId(UUID.randomUUID());
                    }
                    return argument;
                });

        projectName0 = "projectName0";
        projectDescription0 = "projectDescription0";
        project0 = new Project(projectName0, projectDescription0);

        projectName1 = "projectName1";
        projectDescription1 = "projectDescription1";
        projectWithId = new Project(projectName1, projectDescription1);
        projectWithId.setId(UUID.randomUUID());

        testEmailAddress = EmailAddress.fromString("test@example.com");
        testUserDataOfProject = new UserDataOfProject(UUID.randomUUID(), testEmailAddress);
    }

    @AfterEach
    public void tearDown() {
        projectName0 = null;
        projectDescription0 = null;
        project0 = null;

        projectName1 = null;
        projectDescription1 = null;
        projectWithId = null;

        testEmailAddress = null;
        testUserDataOfProject = null;
    }

    @Test
    public void testAddProject() {
        // Arrange
        when(userDataOfProjectRepository.findByUserEmailEquals(testEmailAddress))
                .thenReturn(Collections.singletonList(testUserDataOfProject));

        // Act
        Project result = projectDomainService.addProject(project0, testEmailAddress);

        // Assert
        assertEquals(project0.getId(), result.getId());
        assertEquals(project0.getName(), result.getName());
        assertEquals(project0.getDescription(), result.getDescription());
        assertTrue(result.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));

        verify(userDataOfProjectRepository).findByUserEmailEquals(testEmailAddress);
        verify(projectRepository).save(project0);

        // verify event
        ArgumentCaptor<ProjectCreatedEvent> argumentCaptor = ArgumentCaptor.forClass(ProjectCreatedEvent.class);
        verify(eventPublisher).publishEvent(argumentCaptor.capture());
        assertEquals(project0.getId(), argumentCaptor.getValue().getProjectId());
        assertEquals(testUserDataOfProject.getUserId(), argumentCaptor.getValue().getUserId());
    }

    @Test
    public void testAddProjectNoUserDataFound() {
        // Arrange
        when(userDataOfProjectRepository.findByUserEmailEquals(testEmailAddress))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(ImpossibleException.class, () -> projectDomainService.addProject(project0, testEmailAddress));

        verify(userDataOfProjectRepository).findByUserEmailEquals(testEmailAddress);
        verifyNoInteractions(projectRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testGetProjectById() {
        // Arrange
        when(projectRepository.findById(projectWithId.getId()))
                .thenReturn(Optional.of(projectWithId));

        // Act
        Project result = projectDomainService.getProjectById(projectWithId.getId());

        // Assert
        assertEquals(projectWithId.getId(), result.getId());
        assertEquals(projectWithId.getName(), result.getName());
        assertEquals(projectWithId.getDescription(), result.getDescription());
        assertEquals(projectWithId.getCreationTime(), result.getCreationTime());

        verify(projectRepository).findById(projectWithId.getId());
    }

    @Test
    public void testGetProjectByIdNotFound() {
        // Arrange
        when(projectRepository.findById(projectWithId.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoProjectFoundException.class, () -> projectDomainService.getProjectById(projectWithId.getId()));

        verify(projectRepository).findById(projectWithId.getId());
    }

    @Test
    public void testPatchProjectById() {
        // Arrange
        String newName = "newName";
        String newDescription = "newDescription";

        when(projectRepository.findById(projectWithId.getId()))
                .thenReturn(Optional.of(projectWithId));

        // Act
        projectDomainService.patchProjectById(projectWithId.getId(), newName, newDescription);

        // Assert
        assertEquals(newName, projectWithId.getName());
        assertEquals(newDescription, projectWithId.getDescription());

        verify(projectRepository).findById(projectWithId.getId());
        verify(projectRepository).save(projectWithId);
    }

    @Test
    public void testPatchProjectByIdNotFound() {
        // Arrange
        String newName = "newName";
        String newDescription = "newDescription";

        when(projectRepository.findById(projectWithId.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoProjectFoundException.class, () -> projectDomainService.patchProjectById(projectWithId.getId(), newName, newDescription));

        verify(projectRepository).findById(projectWithId.getId());
        verifyNoMoreInteractions(projectRepository);
    }

    @Test
    public void testPatchProjectByIdNameNull() {
        // Arrange
        String newDescription = "newDescription";

        when(projectRepository.findById(projectWithId.getId()))
                .thenReturn(Optional.of(projectWithId));

        // Act
        projectDomainService.patchProjectById(projectWithId.getId(), null, newDescription);

        // Assert
        assertEquals(projectName1, projectWithId.getName());
        assertEquals(newDescription, projectWithId.getDescription());

        verify(projectRepository).findById(projectWithId.getId());
        verify(projectRepository).save(projectWithId);
    }

    @Test
    public void testPatchProjectByIdDescriptionNull() {
        // Arrange
        String newName = "newName";

        when(projectRepository.findById(projectWithId.getId()))
                .thenReturn(Optional.of(projectWithId));

        // Act
        projectDomainService.patchProjectById(projectWithId.getId(), newName, null);

        // Assert
        assertEquals(newName, projectWithId.getName());
        assertEquals(projectDescription1, projectWithId.getDescription());

        verify(projectRepository).findById(projectWithId.getId());
        verify(projectRepository).save(projectWithId);
    }

    @Test
    public void testPatchProjectByIdNameAndDescriptionNull() {
        // Arrange
        when(projectRepository.findById(projectWithId.getId()))
                .thenReturn(Optional.of(projectWithId));

        // Act
        projectDomainService.patchProjectById(projectWithId.getId(), null, null);

        // Assert
        assertEquals(projectName1, projectWithId.getName());
        assertEquals(projectDescription1, projectWithId.getDescription());

        verify(projectRepository).findById(projectWithId.getId());
        verify(projectRepository).save(projectWithId);
    }

    @Test
    public void testDeleteProjectById() {
        // Arrange
        UUID projectId = UUID.randomUUID();

        when(projectRepository.removeById(projectId))
                .thenReturn(1L);

        // Act
        projectDomainService.deleteProjectById(projectId);

        // Assert
        verify(projectRepository).removeById(projectId);

        // verify event
        ArgumentCaptor<ProjectDeletedEvent> argumentCaptor = ArgumentCaptor.forClass(ProjectDeletedEvent.class);
        verify(eventPublisher).publishEvent(argumentCaptor.capture());
        assertEquals(projectId, argumentCaptor.getValue().getProjectId());
    }

    @Test
    public void testDeleteProjectByIdNotFound() {
        // Arrange
        UUID projectId = UUID.randomUUID();

        when(projectRepository.removeById(projectId))
                .thenReturn(0L);

        // Act & Assert
        assertThrows(NoProjectFoundException.class, () -> projectDomainService.deleteProjectById(projectId));

        verify(projectRepository).removeById(projectId);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testDeleteProjectByIdMultipleProjectsDeleted() {
        // Arrange
        UUID projectId = UUID.randomUUID();

        when(projectRepository.removeById(projectId))
                .thenReturn(2L);

        // Act & Assert
        assertThrows(ImpossibleException.class, () -> projectDomainService.deleteProjectById(projectId));

        verify(projectRepository).removeById(projectId);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(eventPublisher);
    }


    // event listeners

    @Test
    public void testHandleUserCreated() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        projectDomainService.handleUserCreated(new UserCreatedEvent(userId, "name", testEmailAddress));

        // Assert
        ArgumentCaptor<UserDataOfProject> userDataArgumentCaptor = ArgumentCaptor.forClass(UserDataOfProject.class);
        verify(userDataOfProjectRepository).save(userDataArgumentCaptor.capture());
        assertEquals(userId, userDataArgumentCaptor.getValue().getUserId());
        assertEquals(testEmailAddress, userDataArgumentCaptor.getValue().getUserEmail());


        // verify event
        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectArgumentCaptor.capture());
        verify(projectRepository).save(any(Project.class));

        ArgumentCaptor<DefaultProjectCreatedEvent> eventArgumentCaptor = ArgumentCaptor.forClass(DefaultProjectCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventArgumentCaptor.capture());
        assertEquals(projectArgumentCaptor.getValue().getId(), eventArgumentCaptor.getValue().getProjectId());
        assertEquals(userId, eventArgumentCaptor.getValue().getUserId());
    }

    @Test
    public void testHandleLastProjectMemberDeletedEvent() {
        // Arrange
        when(projectRepository.removeById(projectWithId.getId()))
                .thenReturn(1L);

        // Act
        projectDomainService.handleLastProjectMemberDeletedEvent(new LastProjectMemberDeletedEvent(UUID.randomUUID(), UUID.randomUUID(), projectWithId.getId()));

        // Assert
        verify(projectRepository).removeById(projectWithId.getId());

        // verify event
        ArgumentCaptor<ProjectDeletedEvent> argumentCaptor = ArgumentCaptor.forClass(ProjectDeletedEvent.class);
        verify(eventPublisher).publishEvent(argumentCaptor.capture());
        assertEquals(projectWithId.getId(), argumentCaptor.getValue().getProjectId());
    }

    @Test
    public void testHandleLastProjectMemberDeletedEventNotFound() {
        // Arrange
        when(projectRepository.removeById(projectWithId.getId()))
                .thenReturn(0L);

        // Act & Assert
        assertThrows(NoProjectFoundException.class, () -> projectDomainService.handleLastProjectMemberDeletedEvent(new LastProjectMemberDeletedEvent(UUID.randomUUID(), UUID.randomUUID(), projectWithId.getId())));

        verify(projectRepository).removeById(projectWithId.getId());
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testHandleLastProjectMemberDeletedEventMultipleProjectsDeleted() {
        // Arrange
        when(projectRepository.removeById(projectWithId.getId()))
                .thenReturn(2L);

        // Act & Assert
        assertThrows(ImpossibleException.class, () -> projectDomainService.handleLastProjectMemberDeletedEvent(new LastProjectMemberDeletedEvent(UUID.randomUUID(), UUID.randomUUID(), projectWithId.getId())));

        verify(projectRepository).removeById(projectWithId.getId());
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void testHandleUserPatchedEvent() {
        // Arrange
        String newName = "newName";
        EmailAddress newEmailAddress = EmailAddress.fromString("new@mail.com");

        when(userDataOfProjectRepository.findByUserId(testUserDataOfProject.getUserId()))
                .thenReturn(new ArrayList<>(Collections.singletonList(testUserDataOfProject)));

        // Act
        projectDomainService.handleUserPatchedEvent(new UserPatchedEvent(testUserDataOfProject.getUserId(), newName, newEmailAddress));

        // Assert
        verify(userDataOfProjectRepository).findByUserId(testUserDataOfProject.getUserId());

        ArgumentCaptor<UserDataOfProject> userDataArgumentCaptor = ArgumentCaptor.forClass(UserDataOfProject.class);
        verify(userDataOfProjectRepository).save(userDataArgumentCaptor.capture());
        assertEquals(testUserDataOfProject.getId(), userDataArgumentCaptor.getValue().getId());
        assertEquals(testUserDataOfProject.getUserId(), userDataArgumentCaptor.getValue().getUserId());
        assertEquals(newEmailAddress, userDataArgumentCaptor.getValue().getUserEmail());
    }

    @Test
    public void testHandleUserDeletedEvent() {
        // Arrange
        when(userDataOfProjectRepository.deleteByUserId(testUserDataOfProject.getUserId()))
                .thenReturn(1);

        // Act
        projectDomainService.handleUserDeletedEvent(
                new UserDeletedEvent(
                        testUserDataOfProject.getUserId(),
                        "name",
                        testUserDataOfProject.getUserEmail()
                )
        );

        // Assert
        verify(userDataOfProjectRepository).deleteByUserId(testUserDataOfProject.getUserId());
    }
}
