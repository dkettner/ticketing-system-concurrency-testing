package com.kett.TicketSystem.project.domain;

import com.kett.TicketSystem.project.domain.exceptions.ProjectException;
import com.kett.TicketSystem.project.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({ "test" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProjectTests {
    private final ProjectRepository projectRepository;

    private String validName0;
    private String validName1;
    private String validName2;
    private String validName3;

    private String emptyName;
    private String nullName;

    private String description0;
    private String description1;
    private String description2;
    private String description3;

    private String emptyDescription;
    private String nullDescription;

    private Project project0;
    private Project project1;
    private Project project2;
    private Project project3;
    private Project emptyDescriptionProject;
    private Project nullDescriptionProject;

    @Autowired
    public ProjectTests(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }


    @BeforeEach
    public void buildUp() {
        validName0 = "Develop new Javascript Framework";
        validName1 = "Calculate TSM in O(n)";
        validName2 = "Do Stuff.";
        validName3 = "Get some coffee";

        emptyName = "";
        nullName = null;

        description0 = "Something better than Angular and React.";
        description1 = "I mean, it can't be too hard right?";
        description2 = "The same procedure as every year James.";
        description3 = "Black, not from Starbucks.";

        emptyDescription = "";
        nullDescription = null;

        project0 = new Project(validName0, description0);
        project1 = new Project(validName1, description1);
        project2 = new Project(validName2, description2);
        project3 = new Project(validName3, description3);

        emptyDescriptionProject = new Project(validName0, emptyDescription);
        nullDescriptionProject = new Project(validName1, nullDescription);
    }

    @AfterEach
    public void tearDown() {
        validName0 = null;
        validName1 = null;
        validName2 = null;
        validName3 = null;

        emptyName = null;
        nullName = null;

        description0 = null;
        description1 = null;
        description2 = null;
        description3 = null;

        project0 = null;
        project1 = null;
        project2 = null;
        project3 = null;
        emptyDescriptionProject = null;
        nullDescriptionProject = null;

        projectRepository.deleteAll();
    }

    @Test
    public void checkValidConstructorParameters() {
        new Project(validName0, description0);
        new Project(validName1, description1);
        new Project(validName2, description2);
        new Project(validName3, description3);

        new Project(validName0, emptyDescription);
        new Project(validName1, emptyDescription);
        new Project(validName2, emptyDescription);
        new Project(validName3, emptyDescription);

        new Project(validName0, nullDescription);
        new Project(validName1, nullDescription);
        new Project(validName2, nullDescription);
        new Project(validName3, nullDescription);
    }

    @Test
    public void checkInvalidConstructorParameters() {
        assertThrows(ProjectException.class, () -> new Project(emptyName, description0));
        assertThrows(ProjectException.class, () -> new Project(emptyName, description1));
        assertThrows(ProjectException.class, () -> new Project(emptyName, description2));
        assertThrows(ProjectException.class, () -> new Project(emptyName, description3));

        assertThrows(ProjectException.class, () -> new Project(nullName, description0));
        assertThrows(ProjectException.class, () -> new Project(nullName, description1));
        assertThrows(ProjectException.class, () -> new Project(nullName, description2));
        assertThrows(ProjectException.class, () -> new Project(nullName, description3));
    }

    @Test
    public void checkEquals() {
        Project project0Copy = new Project(validName0, description0);
        Project project1Copy = new Project(validName1, description1);
        Project emptyDescriptionProjectCopy = new Project(validName0, emptyDescription);
        Project nullDescriptionProjectCopy = new Project(validName1, nullDescription);

        // only possible in tests because creationTime is private and should not be settable
        project0Copy.setCreationTime(project0.getCreationTime());
        project1Copy.setCreationTime(project1.getCreationTime());
        emptyDescriptionProjectCopy.setCreationTime(emptyDescriptionProject.getCreationTime());
        nullDescriptionProjectCopy.setCreationTime(nullDescriptionProject.getCreationTime());

        // without id, same parameters
        assertEquals(project0, project0Copy);
        assertEquals(project1, project1Copy);
        assertEquals(emptyDescriptionProject, emptyDescriptionProjectCopy);
        assertEquals(nullDescriptionProject, nullDescriptionProjectCopy);

        // without id, different parameters
        assertNotEquals(project0, project1);
        assertNotEquals(project1, emptyDescriptionProject);
        assertNotEquals(project2, nullDescriptionProject);
        assertNotEquals(emptyDescriptionProject, nullDescriptionProject);

        // add id
        projectRepository.save(project0);
        projectRepository.save(project1);
        projectRepository.save(emptyDescriptionProject);
        projectRepository.save(nullDescriptionProject);

        projectRepository.save(project0Copy);
        projectRepository.save(project1Copy);
        projectRepository.save(emptyDescriptionProjectCopy);
        projectRepository.save(nullDescriptionProjectCopy);

        // with id, compare with itself
        assertEquals(project0, projectRepository.findById(project0.getId()).get());
        assertEquals(project1, projectRepository.findById(project1.getId()).get());
        assertEquals(emptyDescriptionProject, projectRepository.findById(emptyDescriptionProject.getId()).get());
        assertEquals(nullDescriptionProject, projectRepository.findById(nullDescriptionProject.getId()).get());

        // with id, compare with different object
        assertNotEquals(project0, project1);
        assertNotEquals(project1, emptyDescriptionProject);
        assertNotEquals(project2, nullDescriptionProject);
        assertNotEquals(emptyDescriptionProject, nullDescriptionProject);

        // with id, compare with different object with same parameters
        assertNotEquals(project0, project0Copy);
        assertNotEquals(project1, project1Copy);
        assertNotEquals(emptyDescriptionProject, emptyDescriptionProjectCopy);
        assertNotEquals(nullDescriptionProject, nullDescriptionProjectCopy);
    }

    @Test
    public void checkHashCode() {
        Project project0Copy = new Project(project0.getName(), project0.getDescription());
        Project project1Copy = new Project(project1.getName(), project1.getDescription());
        Project emptyDescriptionProjectCopy = new Project(emptyDescriptionProject.getName(), emptyDescriptionProject.getDescription());
        Project nullDescriptionProjectCopy = new Project(nullDescriptionProject.getName(), nullDescriptionProject.getDescription());

        // only possible in tests because creationTime is private and should not be settable
        project0Copy.setCreationTime(project0.getCreationTime());
        project1Copy.setCreationTime(project1.getCreationTime());
        emptyDescriptionProjectCopy.setCreationTime(emptyDescriptionProject.getCreationTime());
        nullDescriptionProjectCopy.setCreationTime(nullDescriptionProject.getCreationTime());

        // without id, same parameters
        assertEquals(project0.hashCode(), project0Copy.hashCode());
        assertEquals(project1.hashCode(), project1Copy.hashCode());
        assertEquals(emptyDescriptionProject.hashCode(), emptyDescriptionProjectCopy.hashCode());
        assertEquals(nullDescriptionProject.hashCode(), nullDescriptionProjectCopy.hashCode());

        // without id, different parameters
        assertNotEquals(project0.hashCode(), project1.hashCode());
        assertNotEquals(project1.hashCode(), nullDescriptionProjectCopy.hashCode());
        assertNotEquals(project2.hashCode(), emptyDescriptionProject.hashCode());
        assertNotEquals(emptyDescriptionProject.hashCode(), nullDescriptionProject.hashCode());

        // add id
        projectRepository.save(project0);
        projectRepository.save(project1);
        projectRepository.save(emptyDescriptionProject);
        projectRepository.save(nullDescriptionProject);

        projectRepository.save(project0Copy);
        projectRepository.save(project1Copy);
        projectRepository.save(emptyDescriptionProjectCopy);
        projectRepository.save(nullDescriptionProjectCopy);

        // with id, compare with itself
        assertEquals(project0.hashCode(), projectRepository.findById(project0.getId()).get().hashCode());
        assertEquals(project1.hashCode(), projectRepository.findById(project1.getId()).get().hashCode());
        assertEquals(emptyDescriptionProject.hashCode(), projectRepository.findById(emptyDescriptionProject.getId()).get().hashCode());
        assertEquals(nullDescriptionProject.hashCode(), projectRepository.findById(nullDescriptionProject.getId()).get().hashCode());

        // with id, compare with different object
        assertNotEquals(project0.hashCode(), project1.hashCode());
        assertNotEquals(project1.hashCode(), nullDescriptionProjectCopy.hashCode());
        assertNotEquals(project2.hashCode(), emptyDescriptionProject.hashCode());
        assertNotEquals(emptyDescriptionProject.hashCode(), nullDescriptionProject.hashCode());

        // with id, compare with different object with same parameters
        assertNotEquals(project0.hashCode(), project0Copy.hashCode());
        assertNotEquals(project1.hashCode(), project1Copy.hashCode());
        assertNotEquals(emptyDescriptionProject.hashCode(), emptyDescriptionProjectCopy.hashCode());
        assertNotEquals(nullDescriptionProject.hashCode(), nullDescriptionProjectCopy.hashCode());
    }

    @Test
    public void checkName() {
        assertEquals(validName0, project0.getName());
        assertEquals(validName1, project1.getName());
        assertEquals(validName2, project2.getName());
        assertEquals(validName3, project3.getName());

        assertNotEquals(validName1, project0.getName());
        assertNotEquals(validName2, project1.getName());
        assertNotEquals(validName3, project2.getName());
        assertNotEquals(validName0, project3.getName());
    }

    @Test
    public void checkSetNameValid() {
        String newName = "New Project Name";
        project0.setName(newName);
        assertEquals(newName, project0.getName());
    }

    @Test
    public void checkSetNameEmpty() {
        assertThrows(ProjectException.class, () -> project0.setName(""));
        assertEquals(validName0, project0.getName());

        assertThrows(ProjectException.class, () -> project1.setName(""));
        assertEquals(validName1, project1.getName());

        assertThrows(ProjectException.class, () -> project2.setName(""));
        assertEquals(validName2, project2.getName());

        assertThrows(ProjectException.class, () -> project3.setName(""));
        assertEquals(validName3, project3.getName());

        assertThrows(ProjectException.class, () -> emptyDescriptionProject.setName(""));
        assertEquals(validName0, emptyDescriptionProject.getName());

        assertThrows(ProjectException.class, () -> nullDescriptionProject.setName(""));
        assertEquals(validName1, nullDescriptionProject.getName());
    }

    @Test
    public void checkSetNameNull() {
        assertThrows(ProjectException.class, () -> project0.setName(null));
        assertEquals(validName0, project0.getName());

        assertThrows(ProjectException.class, () -> project1.setName(null));
        assertEquals(validName1, project1.getName());

        assertThrows(ProjectException.class, () -> project2.setName(null));
        assertEquals(validName2, project2.getName());

        assertThrows(ProjectException.class, () -> project3.setName(null));
        assertEquals(validName3, project3.getName());

        assertThrows(ProjectException.class, () -> emptyDescriptionProject.setName(null));
        assertEquals(validName0, emptyDescriptionProject.getName());

        assertThrows(ProjectException.class, () -> nullDescriptionProject.setName(null));
        assertEquals(validName1, nullDescriptionProject.getName());
    }

    @Test
    public void checkDescription() {
        assertEquals(description0, project0.getDescription());
        assertEquals(description1, project1.getDescription());
        assertEquals(description2, project2.getDescription());
        assertEquals(description3, project3.getDescription());

        assertEquals(emptyDescription, emptyDescriptionProject.getDescription());
        assertEquals(emptyDescription, nullDescriptionProject.getDescription()); // null is treated as empty string

        assertNotEquals(description1, project0.getDescription());
        assertNotEquals(description2, project1.getDescription());
        assertNotEquals(description3, project2.getDescription());
        assertNotEquals(description0, project3.getDescription());

        assertNotEquals(emptyDescription, project0.getDescription());
        assertNotEquals(emptyDescription, project1.getDescription());
        assertNotEquals(emptyDescription, project2.getDescription());
        assertNotEquals(emptyDescription, project3.getDescription());

        assertNotEquals(nullDescription, project0.getDescription());
        assertNotEquals(nullDescription, project1.getDescription());
        assertNotEquals(nullDescription, project2.getDescription());
        assertNotEquals(nullDescription, project3.getDescription());

        assertNotEquals(description0, emptyDescriptionProject.getDescription());
        assertNotEquals(description1, emptyDescriptionProject.getDescription());
        assertNotEquals(description2, nullDescriptionProject.getDescription());
        assertNotEquals(description3, nullDescriptionProject.getDescription());
    }

    @Test
    public void checkSetDescriptionValid() {
        String newDescription = "New Project Description";
        project0.setDescription(newDescription);
        assertEquals(newDescription, project0.getDescription());
    }

    @Test
    public void checkSetDescriptionEmpty() {
        project0.setDescription("");
        assertEquals("", project0.getDescription());
    }

    @Test
    public void checkSetDescriptionNull() {
        project0.setDescription(null);
        assertEquals("", project0.getDescription()); // null is treated as empty string
    }

    @Test
    public void checkCreationTime() {
        assertNotNull(project0.getCreationTime());
        assertNotNull(project1.getCreationTime());
        assertNotNull(project2.getCreationTime());
        assertNotNull(project3.getCreationTime());
        assertNotNull(emptyDescriptionProject.getCreationTime());
        assertNotNull(nullDescriptionProject.getCreationTime());

        assertTrue(project0.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(project1.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(project2.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(project3.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(emptyDescriptionProject.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(nullDescriptionProject.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
    }
}
