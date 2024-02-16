package com.kett.TicketSystem.user.domain;

import com.kett.TicketSystem.common.domainprimitives.EmailAddress;
import com.kett.TicketSystem.common.domainprimitives.EmailAddressException;
import com.kett.TicketSystem.user.domain.exceptions.UserException;
import com.kett.TicketSystem.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({ "test" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserTests {
    private final UserRepository userRepository;

    private User user0;
    private User user1;
    private User user2;
    private User user3;

    @Autowired
    public UserTests(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @BeforeEach
    public void buildUp() {
        user0 = new User("User0", "user0@example.com", "password0");
        user1 = new User("User1", "user1@example.com", "password1");
        user2 = new User("User2", "user2@example.com", "password2");
        user3 = new User("User3", "user3@example.com", "password3");
    }

    @AfterEach
    public void tearDown() {
        user0 = null;
        user1 = null;
        user2 = null;
        user3 = null;

        userRepository.deleteAll();
    }

    @Test
    public void checkValidConstructorParameters() {
        // constructor with email as string
        new User("User0", "user0@example.com", "password0");
        new User("User1", "user1@example.com", "password1");
        new User("User2", "user2@example.com", "password2");
        new User("User3", "user3@example.com", "password3");

        // constructor with email as EmailAddress
        new User("User0", EmailAddress.fromString("user0@example.com"), "password0");
        new User("User1", EmailAddress.fromString("user1@example.com"), "password1");
        new User("User2", EmailAddress.fromString("user2@example.com"), "password1");
        new User("User3", EmailAddress.fromString("user3@example.com"), "password1");
    }

    @Test
    public void checkNullConstructorParameters() {
        // null has to be cast to the correct type to avoid ambiguity in the overloaded constructor

        // one parameter is null
        assertThrows(UserException.class, () -> new User(null, "user0@example.com", "password0"));
        assertThrows(UserException.class, () -> new User(null, EmailAddress.fromString("user0@example.com"), "password0"));

        assertThrows(EmailAddressException.class, () -> new User("User1", (String) null, "password1"));
        assertThrows(UserException.class, () -> new User("User1", (EmailAddress) null, "password1"));

        assertThrows(UserException.class, () -> new User("User2", "user2@example.com", null));
        assertThrows(UserException.class, () -> new User("User2", EmailAddress.fromString("user2@example.com"), null));

        // two parameters are null
        assertThrows(EmailAddressException.class, () -> new User(null, (String) null, "password0"));
        assertThrows(UserException.class, () -> new User(null, (EmailAddress) null, "password0"));

        assertThrows(UserException.class, () -> new User(null, "user0@example.com", null));
        assertThrows(UserException.class, () -> new User(null, EmailAddress.fromString("user0@example.com"), null));

        assertThrows(EmailAddressException.class, () -> new User("User0", (String) null, null));
        assertThrows(UserException.class, () -> new User("User0", (EmailAddress) null, null));

        // three parameters are null
        assertThrows(EmailAddressException.class, () -> new User(null, (String) null, null));
        assertThrows(UserException.class, () -> new User(null, (EmailAddress) null, null));
    }

    @Test
    public void checkEmptyStringConstructorParameters() {
        // one parameter is empty string
        assertThrows(UserException.class, () -> new User("", "user0@example.com", "password0"));
        assertThrows(UserException.class, () -> new User("", EmailAddress.fromString("user0@example.com"), "password0"));

        assertThrows(EmailAddressException.class, () -> new User("User1", "", "password1"));
        assertThrows(EmailAddressException.class, () -> new User("User1", EmailAddress.fromString(""), "password1"));

        assertThrows(UserException.class, () -> new User("User2", "user2@example.com", ""));
        assertThrows(UserException.class, () -> new User("User2", EmailAddress.fromString("user2@example.com"), ""));

        // two parameters are empty string
        assertThrows(EmailAddressException.class, () -> new User("", "", "password0"));
        assertThrows(EmailAddressException.class, () -> new User("", EmailAddress.fromString(""), "password0"));

        assertThrows(UserException.class, () -> new User("", "user0@example.com", ""));
        assertThrows(UserException.class, () -> new User("", EmailAddress.fromString("user0@example.com"), ""));

        assertThrows(EmailAddressException.class, () -> new User("User0", "", ""));
        assertThrows(EmailAddressException.class, () -> new User("User0", EmailAddress.fromString(""), ""));

        // three parameters are empty string
        assertThrows(EmailAddressException.class, () -> new User("", "", ""));
        assertThrows(EmailAddressException.class, () -> new User("", EmailAddress.fromString(""), ""));
    }

    @Test
    public void checkEquals() {
        User user0Copy = new User("User0", "user0@example.com", "password0");
        User user1Copy = new User("User1", "user1@example.com", "password1");
        User user2Copy = new User("User2", "user2@example.com", "password2");
        User user3Copy = new User("User3", "user3@example.com", "password3");

        // without id, same parameters
        assertEquals(user0, user0Copy);
        assertEquals(user1, user1Copy);
        assertEquals(user2, user2Copy);
        assertEquals(user3, user3Copy);

        // without id, different parameters
        assertNotEquals(user0, user1);
        assertNotEquals(user1, user2);
        assertNotEquals(user2, user3);
        assertNotEquals(user3, user0);

        // add id
        userRepository.save(user0);
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user0Copy);
        userRepository.save(user1Copy);
        userRepository.save(user2Copy);
        userRepository.save(user3Copy);

        // with id, compare with itself
        assertEquals(user0, userRepository.findById(user0.getId()).get());
        assertEquals(user1, userRepository.findById(user1.getId()).get());
        assertEquals(user2, userRepository.findById(user2.getId()).get());
        assertEquals(user3, userRepository.findById(user3.getId()).get());

        // with id, compare with different object
        assertNotEquals(user0, user1);
        assertNotEquals(user1, user2);
        assertNotEquals(user2, user3);
        assertNotEquals(user3, user0);

        // with id, compare with different object with same parameters
        assertNotEquals(user0, user0Copy);
        assertNotEquals(user1, user1Copy);
        assertNotEquals(user2, user2Copy);
        assertNotEquals(user3, user3Copy);
    }

    @Test
    public void checkHashCode() {
        User user0Copy = new User("User0", "user0@example.com", "password0");
        User user1Copy = new User("User1", "user1@example.com", "password1");
        User user2Copy = new User("User2", "user2@example.com", "password2");
        User user3Copy = new User("User3", "user3@example.com", "password3");

        // without id, same parameters
        assertEquals(user0.hashCode(), user0Copy.hashCode());
        assertEquals(user1.hashCode(), user1Copy.hashCode());
        assertEquals(user2.hashCode(), user2Copy.hashCode());
        assertEquals(user3.hashCode(), user3Copy.hashCode());

        // without id, different parameters
        assertNotEquals(user0.hashCode(), user1Copy.hashCode());
        assertNotEquals(user1.hashCode(), user2Copy.hashCode());
        assertNotEquals(user2.hashCode(), user3Copy.hashCode());
        assertNotEquals(user3.hashCode(), user0Copy.hashCode());

        // add id
        userRepository.save(user0);
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user0Copy);
        userRepository.save(user1Copy);
        userRepository.save(user2Copy);
        userRepository.save(user3Copy);

        // with id, compare with itself
        assertEquals(user0.hashCode(), userRepository.findById(user0.getId()).get().hashCode());
        assertEquals(user1.hashCode(), userRepository.findById(user1.getId()).get().hashCode());
        assertEquals(user2.hashCode(), userRepository.findById(user2.getId()).get().hashCode());
        assertEquals(user3.hashCode(), userRepository.findById(user3.getId()).get().hashCode());

        // with id, compare with different object
        assertNotEquals(user0.hashCode(), user1.hashCode());
        assertNotEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user2.hashCode(), user3.hashCode());
        assertNotEquals(user3.hashCode(), user0.hashCode());

        // with id, compare with different object with same parameters
        assertNotEquals(user0.hashCode(), user0Copy.hashCode());
        assertNotEquals(user1.hashCode(), user1Copy.hashCode());
        assertNotEquals(user2.hashCode(), user2Copy.hashCode());
        assertNotEquals(user3.hashCode(), user3Copy.hashCode());
    }

    @Test
    public void checkSetName() {
        // valid name
        user0.setName("User1");
        assertEquals("User1", user0.getName());

        // null name
        assertThrows(UserException.class, () -> user0.setName(null));
        assertEquals("User1", user0.getName());

        // empty name
        assertThrows(UserException.class, () -> user0.setName(""));
        assertEquals("User1", user0.getName());
    }

    @Test
    public void checkSetEmail() {
        // valid email
        user0.setEmail(EmailAddress.fromString("user1@example.com"));
        assertEquals(EmailAddress.fromString("user1@example.com"), user0.getEmail());

        // null email
        assertThrows(UserException.class, () -> user0.setEmail(null));
        assertEquals(EmailAddress.fromString("user1@example.com"), user0.getEmail());

        // empty email
        assertThrows(EmailAddressException.class, () -> user0.setEmail(EmailAddress.fromString("")));
        assertEquals(EmailAddress.fromString("user1@example.com"), user0.getEmail());
    }

    @Test
    public void checkSetPassword() {
        // valid password
        user0.setPassword("password1");
        assertEquals("password1", user0.getPassword());

        // null password
        assertThrows(UserException.class, () -> user0.setPassword(null));
        assertEquals("password1", user0.getPassword());

        // empty password
        assertThrows(UserException.class, () -> user0.setPassword(""));
        assertEquals("password1", user0.getPassword());
    }
}