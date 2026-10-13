package com.cozyfireplace.server;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.auth.UserDetailsImpl;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileRepository;
import com.cozyfireplace.server.notifications.firebase.FirebaseConfig;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleRepository;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.tags.Tag;
import com.cozyfireplace.server.tags.TagRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared base class for all integration tests (IT) of the Cozy Fireplace backend.
 *
 * <p><b>What it provides:</p>
 * <ul>
 *   <li>A real Spring application context ({@code @SpringBootTest}) with the full
 *       MVC stack including the Spring Security filter chain ({@code @AutoConfigureMockMvc}).</li>
 *   <li>A dedicated PostgreSQL 18 database running in <b>Testcontainers</b>. The container
 *       uses the <em>singleton pattern</em>: it is started once for the whole test JVM and
 *       never stopped, so the cached Spring context remains valid across every IT class.</li>
 *   <li>Liquibase migrations executed against the container on first context start
 *       ({@code spring.jpa.hibernate.ddl-auto=validate} guarantees schema/entity drift fails fast).</li>
 *   <li>Transactional test methods: every test runs inside a transaction that is rolled back
 *       afterwards, keeping the shared database clean between tests.</li>
 *   <li>External infrastructure mocked out: Firebase ({@link FirebaseConfig}) and the
 *       S3-compatible object storage client ({@link S3Client}) — ITs never touch real cloud services.</li>
 *   <li>Ready-made fixture helpers: {@link #newProfile(String)}, {@link #newRoom},
 *       {@link #newAccount}, {@link #newRole}, {@link #newTag}.</li>
 *   <li>Authentication helpers: {@link #authenticateAs(Profile)} installs a real
 *       {@code UsernamePasswordAuthenticationToken} with a {@link UserDetailsImpl} principal
 *       into the {@code SecurityContextHolder}, which is exactly what
 *       {@code @CurrentAccount}-based controllers resolve against. The authentication is
 *       cleared automatically after each test.</li>
 * </ul>
 *
 * <p><b>How to write a new IT:</b> extend this class, name the class {@code *IT},
 * annotate it with {@code @DisplayName}, organise endpoints with {@code @Nested}
 * classes and cover at minimum: happy path, not-found, forbidden and validation cases.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Integration test base")
public abstract class BaseIntegrationTest {

    /** Singleton PostgreSQL container shared by every IT class for the whole JVM run. */
    static final PostgreSQL18Container POSTGRES = PostgreSQL18Container.create();

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /**
     * Starts a PostgreSQL 18 (alpine) container once and keeps it alive.
     * Kept in a nested class so the container is only lazily initialised
     * when the first IT actually needs it.
     */
    static final class PostgreSQL18Container extends org.testcontainers.postgresql.PostgreSQLContainer {
        PostgreSQL18Container(String dockerImageName) {
            super(dockerImageName);
        }

        static PostgreSQL18Container create() {
            PostgreSQL18Container c = new PostgreSQL18Container("postgres:18-alpine");
            c.withDatabaseName("testdb");
            c.withUsername("test");
            c.withPassword("test");
            c.withReuse(true);
            c.start();
            return c;
        }
    }

    // ====== Spring infrastructure ======

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ====== Repositories used for fixture setup ======

    @Autowired
    protected ProfileRepository profileRepository;
    @Autowired
    protected AccountRepository accountRepository;
    @Autowired
    protected RoomRepository roomRepository;
    @Autowired
    protected RoleRepository roleRepository;
    @Autowired
    protected TagRepository tagRepository;

    // ====== External services mocked globally ======

    /** Prevents Firebase initialisation (requires real service-account credentials). */
    @MockitoBean
    protected FirebaseConfig firebaseConfig;

    /** Prevents any real network call to the RustFS/S3 object storage. */
    @MockitoBean
    protected S3Client s3Client;

    /** Unique-number generator so fixture usernames/emails never collide within one transaction. */
    private final AtomicInteger sequence = new AtomicInteger();

    // ====== Authentication helpers ======

    /**
     * Installs an authentication for the given profile into the {@code SecurityContextHolder}
     * <b>and</b> into the {@code TestSecurityContextHolder}.
     * The principal is a {@link UserDetailsImpl}, matching what {@code CurrentAccountResolver}
     * and {@code CustomUserDetailsService} consumers expect.
     *
     * <p>Both holders are needed: Spring Boot applies {@code springSecurity()} to the
     * auto-configured {@code MockMvc}, so the security filter chain loads the context through
     * {@code TestSecurityContextRepository}. If only the plain {@code SecurityContextHolder} is
     * written, the context captured by the <em>first</em> request of a test keeps winning and a
     * later {@code authenticateAs(otherProfile)} is silently ignored — the request then runs as
     * the previously authenticated user (which made role-restricted content look visible to a
     * plain member).</p>
     */
    protected void authenticateAs(Profile profile) {
        UserDetailsImpl principal = UserDetailsImpl.build(profile);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, principal.getPassword(), principal.getAuthorities());
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(auth);
        TestSecurityContextHolder.setAuthentication(auth);
    }

    /** Removes the current authentication (e.g. to test 401 responses). */
    protected void clearAuthentication() {
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDownAuthentication() {
        clearAuthentication();
    }

    // ====== Fixture helpers ======

    /** Persists a minimal profile with a unique username/email (password: {@code "password"}). */
    protected Profile newProfile(String roleHint) {
        int n = sequence.incrementAndGet();
        Profile p = new Profile();
        p.setUsername(roleHint + n);
        p.setEmail(roleHint + n + "@it.cozyfireplace.ru");
        p.setPassword("password");
        return profileRepository.save(p);
    }

    /**
     * Persists a room owned by a freshly created creator profile.
     * Returns the saved room ({@code room.getCreator()} is the creator's account in this room).
     */
    protected Room newRoom(String name) {
        return newRoomOwnedBy(newProfile("creator-"), name);
    }

    /**
     * Persists a room (slug-style unique url) whose creator account belongs to
     * the given profile. Also exposes the creator's account via {@code room.getCreator()}.
     */
    protected Room newRoomOwnedBy(Profile creatorProfile, String name) {
        String slug = "it-" + name.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + System.nanoTime();
        Room room = Room.builder()
                .name(name)
                .url(slug)
                .build();
        Account creatorAccount = newAccount(creatorProfile, room, "Creator");
        room.setCreator(creatorAccount);
        return roomRepository.save(room);
    }

    /** Persists an account binding a profile into a room. Room must already be saved. */
    protected Account newAccount(Profile profile, Room room, String name) {
        Account account = Account.builder()
                .name(name)
                .profile(profile)
                .room(room)
                .roles(new java.util.HashSet<>())
                .build();
        return accountRepository.save(account);
    }

    /** Persists a role inside the given room. */
    protected Role newRole(Room room, String name) {
        Role role = new Role();
        role.setName(name);
        role.setRoom(room);
        return roleRepository.save(role);
    }

    /** Persists a tag inside the given room. */
    protected Tag newTag(Room room, String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setRoom(room);
        return tagRepository.save(tag);
    }

    /** Serialises an object to JSON for request bodies. */
    protected String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialise test payload: " + value, e);
        }
    }
}
