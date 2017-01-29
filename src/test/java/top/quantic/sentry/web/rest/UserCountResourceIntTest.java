package top.quantic.sentry.web.rest;

import top.quantic.sentry.SentryApp;

import top.quantic.sentry.domain.UserCount;
import top.quantic.sentry.repository.UserCountRepository;
import top.quantic.sentry.service.UserCountService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.inject.Inject;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.List;

import static top.quantic.sentry.web.rest.TestUtil.sameInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the UserCountResource REST controller.
 *
 * @see UserCountResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class UserCountResourceIntTest {

    private static final String DEFAULT_BOT = "AAAAAAAAAA";
    private static final String UPDATED_BOT = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final Long DEFAULT_VALUE = 0L;
    private static final Long UPDATED_VALUE = 1L;

    private static final String DEFAULT_GUILD = "AAAAAAAAAA";
    private static final String UPDATED_GUILD = "BBBBBBBBBB";

    private static final ZonedDateTime DEFAULT_TIMESTAMP = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_TIMESTAMP = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    @Inject
    private UserCountRepository userCountRepository;

    @Inject
    private UserCountService userCountService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restUserCountMockMvc;

    private UserCount userCount;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        UserCountResource userCountResource = new UserCountResource();
        ReflectionTestUtils.setField(userCountResource, "userCountService", userCountService);
        this.restUserCountMockMvc = MockMvcBuilders.standaloneSetup(userCountResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserCount createEntity() {
        UserCount userCount = new UserCount()
                .bot(DEFAULT_BOT)
                .status(DEFAULT_STATUS)
                .value(DEFAULT_VALUE)
                .guild(DEFAULT_GUILD)
                .timestamp(DEFAULT_TIMESTAMP);
        return userCount;
    }

    @Before
    public void initTest() {
        userCountRepository.deleteAll();
        userCount = createEntity();
    }

    @Test
    public void createUserCount() throws Exception {
        int databaseSizeBeforeCreate = userCountRepository.findAll().size();

        // Create the UserCount

        restUserCountMockMvc.perform(post("/api/user-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userCount)))
            .andExpect(status().isCreated());

        // Validate the UserCount in the database
        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeCreate + 1);
        UserCount testUserCount = userCountList.get(userCountList.size() - 1);
        assertThat(testUserCount.getBot()).isEqualTo(DEFAULT_BOT);
        assertThat(testUserCount.getStatus()).isEqualTo(DEFAULT_STATUS);
        assertThat(testUserCount.getValue()).isEqualTo(DEFAULT_VALUE);
        assertThat(testUserCount.getGuild()).isEqualTo(DEFAULT_GUILD);
        assertThat(testUserCount.getTimestamp()).isEqualTo(DEFAULT_TIMESTAMP);
    }

    @Test
    public void createUserCountWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userCountRepository.findAll().size();

        // Create the UserCount with an existing ID
        UserCount existingUserCount = new UserCount();
        existingUserCount.setId("existing_id");

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserCountMockMvc.perform(post("/api/user-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingUserCount)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkBotIsRequired() throws Exception {
        int databaseSizeBeforeTest = userCountRepository.findAll().size();
        // set the field null
        userCount.setBot(null);

        // Create the UserCount, which fails.

        restUserCountMockMvc.perform(post("/api/user-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userCount)))
            .andExpect(status().isBadRequest());

        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkStatusIsRequired() throws Exception {
        int databaseSizeBeforeTest = userCountRepository.findAll().size();
        // set the field null
        userCount.setStatus(null);

        // Create the UserCount, which fails.

        restUserCountMockMvc.perform(post("/api/user-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userCount)))
            .andExpect(status().isBadRequest());

        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkValueIsRequired() throws Exception {
        int databaseSizeBeforeTest = userCountRepository.findAll().size();
        // set the field null
        userCount.setValue(null);

        // Create the UserCount, which fails.

        restUserCountMockMvc.perform(post("/api/user-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userCount)))
            .andExpect(status().isBadRequest());

        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkGuildIsRequired() throws Exception {
        int databaseSizeBeforeTest = userCountRepository.findAll().size();
        // set the field null
        userCount.setGuild(null);

        // Create the UserCount, which fails.

        restUserCountMockMvc.perform(post("/api/user-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userCount)))
            .andExpect(status().isBadRequest());

        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkTimestampIsRequired() throws Exception {
        int databaseSizeBeforeTest = userCountRepository.findAll().size();
        // set the field null
        userCount.setTimestamp(null);

        // Create the UserCount, which fails.

        restUserCountMockMvc.perform(post("/api/user-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userCount)))
            .andExpect(status().isBadRequest());

        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllUserCounts() throws Exception {
        // Initialize the database
        userCountRepository.save(userCount);

        // Get all the userCountList
        restUserCountMockMvc.perform(get("/api/user-counts?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userCount.getId())))
            .andExpect(jsonPath("$.[*].bot").value(hasItem(DEFAULT_BOT.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].value").value(hasItem(DEFAULT_VALUE.intValue())))
            .andExpect(jsonPath("$.[*].guild").value(hasItem(DEFAULT_GUILD.toString())))
            .andExpect(jsonPath("$.[*].timestamp").value(hasItem(sameInstant(DEFAULT_TIMESTAMP))));
    }

    @Test
    public void getUserCount() throws Exception {
        // Initialize the database
        userCountRepository.save(userCount);

        // Get the userCount
        restUserCountMockMvc.perform(get("/api/user-counts/{id}", userCount.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(userCount.getId()))
            .andExpect(jsonPath("$.bot").value(DEFAULT_BOT.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.value").value(DEFAULT_VALUE.intValue()))
            .andExpect(jsonPath("$.guild").value(DEFAULT_GUILD.toString()))
            .andExpect(jsonPath("$.timestamp").value(sameInstant(DEFAULT_TIMESTAMP)));
    }

    @Test
    public void getNonExistingUserCount() throws Exception {
        // Get the userCount
        restUserCountMockMvc.perform(get("/api/user-counts/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateUserCount() throws Exception {
        // Initialize the database
        userCountService.save(userCount);

        int databaseSizeBeforeUpdate = userCountRepository.findAll().size();

        // Update the userCount
        UserCount updatedUserCount = userCountRepository.findOne(userCount.getId());
        updatedUserCount
                .bot(UPDATED_BOT)
                .status(UPDATED_STATUS)
                .value(UPDATED_VALUE)
                .guild(UPDATED_GUILD)
                .timestamp(UPDATED_TIMESTAMP);

        restUserCountMockMvc.perform(put("/api/user-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedUserCount)))
            .andExpect(status().isOk());

        // Validate the UserCount in the database
        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeUpdate);
        UserCount testUserCount = userCountList.get(userCountList.size() - 1);
        assertThat(testUserCount.getBot()).isEqualTo(UPDATED_BOT);
        assertThat(testUserCount.getStatus()).isEqualTo(UPDATED_STATUS);
        assertThat(testUserCount.getValue()).isEqualTo(UPDATED_VALUE);
        assertThat(testUserCount.getGuild()).isEqualTo(UPDATED_GUILD);
        assertThat(testUserCount.getTimestamp()).isEqualTo(UPDATED_TIMESTAMP);
    }

    @Test
    public void updateNonExistingUserCount() throws Exception {
        int databaseSizeBeforeUpdate = userCountRepository.findAll().size();

        // Create the UserCount

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restUserCountMockMvc.perform(put("/api/user-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userCount)))
            .andExpect(status().isCreated());

        // Validate the UserCount in the database
        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deleteUserCount() throws Exception {
        // Initialize the database
        userCountService.save(userCount);

        int databaseSizeBeforeDelete = userCountRepository.findAll().size();

        // Get the userCount
        restUserCountMockMvc.perform(delete("/api/user-counts/{id}", userCount.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<UserCount> userCountList = userCountRepository.findAll();
        assertThat(userCountList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
