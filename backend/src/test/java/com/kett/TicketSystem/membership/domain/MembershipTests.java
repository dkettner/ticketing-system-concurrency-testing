package com.kett.TicketSystem.membership.domain;

import com.kett.TicketSystem.common.exceptions.IllegalStateUpdateException;
import com.kett.TicketSystem.membership.domain.exceptions.MembershipException;
import com.kett.TicketSystem.membership.repository.MembershipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({ "test" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MembershipTests {
    private final MembershipRepository membershipRepository;

    private UUID uuid0;
    private UUID uuid1;
    private UUID uuid2;
    private UUID uuid3;

    private Role standardRole;
    private Role adminRole;

    private Membership membership0;
    private Membership membership1;
    private Membership membership2;
    private Membership membership3;

    private static class TestableMembership extends Membership {
        public TestableMembership(UUID projectId, UUID userId, Role role) {
            super(projectId, userId, role);
        }

        public void publicSetProjectId(UUID projectId) {
            this.setProjectId(projectId);
        }

        public void publicSetUserId(UUID userId) {
            this.setUserId(userId);
        }
    }

    @Autowired
    public MembershipTests(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @BeforeEach
    public void buildUp() {
        uuid0 = UUID.randomUUID();
        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
        uuid3 = UUID.randomUUID();

        standardRole = Role.MEMBER;
        adminRole = Role.ADMIN;

        membership0 = new Membership(uuid0, uuid1, standardRole);
        membership1 = new Membership(uuid1, uuid2, standardRole);
        membership2 = new Membership(uuid2, uuid3, adminRole);
        membership3 = new Membership(uuid3, uuid0, adminRole);
    }

    @AfterEach
    public void tearDown() {
        uuid0 = null;
        uuid1 = null;
        uuid2 = null;
        uuid3 = null;

        standardRole = null;
        adminRole = null;

        membership0 = null;
        membership1 = null;
        membership2 = null;
        membership3 = null;

        membershipRepository.deleteAll();
    }

    @Test
    public void checkValidConstructorParameters() {
        new Membership(uuid0, uuid1, standardRole);
        new Membership(uuid1, uuid2, standardRole);
        new Membership(uuid2, uuid3, adminRole);
        new Membership(uuid3, uuid0, adminRole);
    }

    @Test
    public void checkNullConstructorParameters() {
        assertThrows(MembershipException.class, () -> new Membership(null, null, null));
        assertThrows(MembershipException.class, () -> new Membership(null, null, standardRole));
        assertThrows(MembershipException.class, () -> new Membership(null, uuid0, null));
        assertThrows(MembershipException.class, () -> new Membership(null, uuid1, adminRole));
        assertThrows(MembershipException.class, () -> new Membership(uuid2, null, null));
        assertThrows(MembershipException.class, () -> new Membership(uuid3, null, standardRole));
        assertThrows(MembershipException.class, () -> new Membership(uuid0, uuid1, null));
    }

    @Test
    public void checkEquals() {
        Membership membership0Copy = new Membership(uuid0, uuid1, standardRole);
        Membership membership1Copy = new Membership(uuid1, uuid2, standardRole);
        Membership membership2Copy = new Membership(uuid2, uuid3, adminRole);
        Membership membership3Copy = new Membership(uuid3, uuid0, adminRole);

        // without id, same parameters
        assertEquals(membership0, membership0Copy);
        assertEquals(membership1, membership1Copy);
        assertEquals(membership2, membership2Copy);
        assertEquals(membership3, membership3Copy);

        // without id, different parameters
        assertNotEquals(membership0, membership1);
        assertNotEquals(membership1, membership2);
        assertNotEquals(membership2, membership3);
        assertNotEquals(membership3, membership0);

        // add id
        membershipRepository.save(membership0);
        membershipRepository.save(membership1);
        membershipRepository.save(membership2);
        membershipRepository.save(membership3);
        membershipRepository.save(membership0Copy);
        membershipRepository.save(membership1Copy);
        membershipRepository.save(membership2Copy);
        membershipRepository.save(membership3Copy);

        // with id, compare with itself
        assertEquals(membership0, membershipRepository.findById(membership0.getId()).get());
        assertEquals(membership1, membershipRepository.findById(membership1.getId()).get());
        assertEquals(membership2, membershipRepository.findById(membership2.getId()).get());
        assertEquals(membership3, membershipRepository.findById(membership3.getId()).get());

        // with id, compare with different object
        assertNotEquals(membership0, membership1);
        assertNotEquals(membership1, membership2);
        assertNotEquals(membership2, membership3);
        assertNotEquals(membership3, membership0);

        // with id, compare with different object with same parameters
        assertNotEquals(membership0, membership0Copy);
        assertNotEquals(membership1, membership1Copy);
        assertNotEquals(membership2, membership2Copy);
        assertNotEquals(membership3, membership3Copy);
    }

    @Test
    public void checkHashCode() {
        Membership membership0Copy = new Membership(uuid0, uuid1, standardRole);
        Membership membership1Copy = new Membership(uuid1, uuid2, standardRole);
        Membership membership2Copy = new Membership(uuid2, uuid3, adminRole);
        Membership membership3Copy = new Membership(uuid3, uuid0, adminRole);

        // without id, same parameters
        assertEquals(membership0.hashCode(), membership0Copy.hashCode());
        assertEquals(membership1.hashCode(), membership1Copy.hashCode());
        assertEquals(membership2.hashCode(), membership2Copy.hashCode());
        assertEquals(membership3.hashCode(), membership3Copy.hashCode());

        // without id, different parameters
        assertNotEquals(membership0.hashCode(), membership1Copy.hashCode());
        assertNotEquals(membership1.hashCode(), membership2Copy.hashCode());
        assertNotEquals(membership2.hashCode(), membership3Copy.hashCode());
        assertNotEquals(membership3.hashCode(), membership0Copy.hashCode());

        // add id
        membershipRepository.save(membership0);
        membershipRepository.save(membership1);
        membershipRepository.save(membership2);
        membershipRepository.save(membership3);
        membershipRepository.save(membership0Copy);
        membershipRepository.save(membership1Copy);
        membershipRepository.save(membership2Copy);
        membershipRepository.save(membership3Copy);

        // with id, compare with itself
        assertEquals(membership0.hashCode(), membershipRepository.findById(membership0.getId()).get().hashCode());
        assertEquals(membership1.hashCode(), membershipRepository.findById(membership1.getId()).get().hashCode());
        assertEquals(membership2.hashCode(), membershipRepository.findById(membership2.getId()).get().hashCode());
        assertEquals(membership3.hashCode(), membershipRepository.findById(membership3.getId()).get().hashCode());

        // with id, compare with different object
        assertNotEquals(membership0.hashCode(), membership1.hashCode());
        assertNotEquals(membership1.hashCode(), membership2.hashCode());
        assertNotEquals(membership2.hashCode(), membership3.hashCode());
        assertNotEquals(membership3.hashCode(), membership0.hashCode());

        // with id, compare with different object with same parameters
        assertNotEquals(membership0.hashCode(), membership0Copy.hashCode());
        assertNotEquals(membership1.hashCode(), membership1Copy.hashCode());
        assertNotEquals(membership2.hashCode(), membership2Copy.hashCode());
        assertNotEquals(membership3.hashCode(), membership3Copy.hashCode());
    }

    @Test
    public void checkProjectId() {
        assertEquals(uuid0, membership0.getProjectId());
        assertEquals(uuid1, membership1.getProjectId());
        assertEquals(uuid2, membership2.getProjectId());
        assertEquals(uuid3, membership3.getProjectId());

        assertNotEquals(uuid1, membership0.getProjectId());
        assertNotEquals(uuid2, membership1.getProjectId());
        assertNotEquals(uuid3, membership2.getProjectId());
        assertNotEquals(uuid0, membership3.getProjectId());
    }

    @Test
    public void checkUserId() {
        assertEquals(uuid1, membership0.getUserId());
        assertEquals(uuid2, membership1.getUserId());
        assertEquals(uuid3, membership2.getUserId());
        assertEquals(uuid0, membership3.getUserId());

        assertNotEquals(uuid0, membership0.getUserId());
        assertNotEquals(uuid1, membership1.getUserId());
        assertNotEquals(uuid2, membership2.getUserId());
        assertNotEquals(uuid3, membership3.getUserId());
    }

    @Test
    public void checkRole() {
        // Role gets set in buildUp()
        assertEquals(Role.MEMBER, membership0.getRole());
        assertEquals(Role.MEMBER, membership1.getRole());
        assertEquals(Role.ADMIN, membership2.getRole());
        assertEquals(Role.ADMIN, membership3.getRole());

        assertNotEquals(Role.ADMIN, membership0.getRole());
        assertNotEquals(Role.ADMIN, membership1.getRole());
        assertNotEquals(Role.MEMBER, membership2.getRole());
        assertNotEquals(Role.MEMBER, membership3.getRole());
    }

    @Test
    public void checkSetRoleNull() {
        assertThrows(MembershipException.class, () -> membership0.setRole(null));
        assertEquals(membership0.getRole(), standardRole);

        assertThrows(MembershipException.class, () -> membership1.setRole(null));
        assertEquals(membership1.getRole(), standardRole);

        assertThrows(MembershipException.class, () -> membership2.setRole(null));
        assertEquals(membership2.getRole(), adminRole);

        assertThrows(MembershipException.class, () -> membership3.setRole(null));
        assertEquals(membership3.getRole(), adminRole);
    }

    @Test
    public void checkDefaultState() {
        assertEquals(State.OPEN, membership0.getState());
        assertEquals(State.OPEN, membership1.getState());
        assertEquals(State.OPEN, membership2.getState());
        assertEquals(State.OPEN, membership3.getState());
    }

    @Test
    public void checkSetStateFromOpenToAccepted() {
        membership0.setState(State.ACCEPTED);
        assertEquals(State.ACCEPTED, membership0.getState());

        membership1.setState(State.ACCEPTED);
        assertEquals(State.ACCEPTED, membership1.getState());

        membership2.setState(State.ACCEPTED);
        assertEquals(State.ACCEPTED, membership2.getState());

        membership3.setState(State.ACCEPTED);
        assertEquals(State.ACCEPTED, membership3.getState());
    }

    @Test
    public void checkSetStateOpenAfterAccepted() {
        membership0.setState(State.ACCEPTED);
        assertThrows(IllegalStateUpdateException.class, () -> membership0.setState(State.OPEN));
        assertEquals(State.ACCEPTED, membership0.getState());

        membership1.setState(State.ACCEPTED);
        assertThrows(IllegalStateUpdateException.class, () -> membership1.setState(State.OPEN));
        assertEquals(State.ACCEPTED, membership1.getState());

        membership2.setState(State.ACCEPTED);
        assertThrows(IllegalStateUpdateException.class, () -> membership2.setState(State.OPEN));
        assertEquals(State.ACCEPTED, membership2.getState());

        membership3.setState(State.ACCEPTED);
        assertThrows(IllegalStateUpdateException.class, () -> membership3.setState(State.OPEN));
        assertEquals(State.ACCEPTED, membership3.getState());
    }

    @Test
    public void checkSetStateSame() {
        // Membership is OPEN by default
        assertThrows(IllegalStateUpdateException.class, () -> membership0.setState(State.OPEN));
        assertEquals(State.OPEN, membership0.getState());

        assertThrows(IllegalStateUpdateException.class, () -> membership1.setState(State.OPEN));
        assertEquals(State.OPEN, membership1.getState());

        assertThrows(IllegalStateUpdateException.class, () -> membership2.setState(State.OPEN));
        assertEquals(State.OPEN, membership2.getState());

        assertThrows(IllegalStateUpdateException.class, () -> membership3.setState(State.OPEN));
        assertEquals(State.OPEN, membership3.getState());

        membership0.setState(State.ACCEPTED);
        membership1.setState(State.ACCEPTED);
        membership2.setState(State.ACCEPTED);
        membership3.setState(State.ACCEPTED);

        assertThrows(IllegalStateUpdateException.class, () -> membership0.setState(State.ACCEPTED));
        assertEquals(State.ACCEPTED, membership0.getState());

        assertThrows(IllegalStateUpdateException.class, () -> membership1.setState(State.ACCEPTED));
        assertEquals(State.ACCEPTED, membership1.getState());

        assertThrows(IllegalStateUpdateException.class, () -> membership2.setState(State.ACCEPTED));
        assertEquals(State.ACCEPTED, membership2.getState());

        assertThrows(IllegalStateUpdateException.class, () -> membership3.setState(State.ACCEPTED));
        assertEquals(State.ACCEPTED, membership3.getState());
    }

    @Test
    public void checkSetStateNull() {
        assertThrows(MembershipException.class, () -> membership0.setState(null));
        assertThrows(MembershipException.class, () -> membership1.setState(null));
        assertThrows(MembershipException.class, () -> membership2.setState(null));
        assertThrows(MembershipException.class, () -> membership3.setState(null));
    }

    @Test
    public void checkSetStateFromAcceptedToOpen() {
        membership0.setState(State.ACCEPTED);
        assertThrows(IllegalStateUpdateException.class, () -> membership0.setState(State.OPEN));
        assertEquals(State.ACCEPTED, membership0.getState());

        membership1.setState(State.ACCEPTED);
        assertThrows(IllegalStateUpdateException.class, () -> membership1.setState(State.OPEN));
        assertEquals(State.ACCEPTED, membership1.getState());

        membership2.setState(State.ACCEPTED);
        assertThrows(IllegalStateUpdateException.class, () -> membership2.setState(State.OPEN));
        assertEquals(State.ACCEPTED, membership2.getState());

        membership3.setState(State.ACCEPTED);
        assertThrows(IllegalStateUpdateException.class, () -> membership3.setState(State.OPEN));
        assertEquals(State.ACCEPTED, membership3.getState());
    }

    @Test
    public void testSetProjectId() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TestableMembership membership = new TestableMembership(projectId, userId, Role.MEMBER);

        // Test setting the project ID to a new value
        UUID newProjectId = UUID.randomUUID();
        assertThrows(IllegalStateUpdateException.class, () -> membership.publicSetProjectId(newProjectId));

        // Test setting the project ID to null
        assertThrows(MembershipException.class, () -> membership.publicSetProjectId(null));
    }

    @Test
    public void testSetUserId() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TestableMembership membership = new TestableMembership(projectId, userId, Role.MEMBER);

        // Test setting the user ID to a new value
        UUID newUserId = UUID.randomUUID();
        assertThrows(IllegalStateUpdateException.class, () -> membership.publicSetUserId(newUserId));

        // Test setting the user ID to null
        assertThrows(MembershipException.class, () -> membership.publicSetUserId(null));
    }

    @Test
    public void testGetAuthority() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Test the authority for a member
        TestableMembership membership = new TestableMembership(projectId, userId, Role.MEMBER);
        String expectedAuthority = "ROLE_PROJECT_MEMBER_" + projectId.toString();
        assertEquals(expectedAuthority, membership.getAuthority());

        // Test the authority for an admin
        TestableMembership adminMembership = new TestableMembership(projectId, userId, Role.ADMIN);
        expectedAuthority = "ROLE_PROJECT_ADMIN_" + projectId.toString();
        assertEquals(expectedAuthority, adminMembership.getAuthority());
    }
}
