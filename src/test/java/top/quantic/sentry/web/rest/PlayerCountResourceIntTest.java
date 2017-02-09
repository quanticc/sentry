package top.quantic.sentry.web.rest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.quantic.sentry.SentryApp;
import top.quantic.sentry.domain.PlayerCount;
import top.quantic.sentry.repository.PlayerCountRepository;
import top.quantic.sentry.service.PlayerCountService;

import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static top.quantic.sentry.web.rest.TestUtil.sameInstant;

/**
 * Test class for the PlayerCountResource REST controller.
 *
 * @see PlayerCountResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class PlayerCountResourceIntTest {

    private static final String DEFAULT_REGION = "AAAAAAAAAA";
    private static final String UPDATED_REGION = "BBBBBBBBBB";

    private static final Long DEFAULT_VALUE = 0L;
    private static final Long UPDATED_VALUE = 1L;

    private static final ZonedDateTime DEFAULT_TIMESTAMP = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_TIMESTAMP = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    @Inject
    private PlayerCountRepository playerCountRepository;

    @Inject
    private PlayerCountService playerCountService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restPlayerCountMockMvc;

    private PlayerCount playerCount;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PlayerCountResource playerCountResource = new PlayerCountResource(playerCountService);
        this.restPlayerCountMockMvc = MockMvcBuilders.standaloneSetup(playerCountResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PlayerCount createEntity() {
        PlayerCount playerCount = new PlayerCount()
                .region(DEFAULT_REGION)
                .value(DEFAULT_VALUE)
                .timestamp(DEFAULT_TIMESTAMP);
        return playerCount;
    }

    @Before
    public void initTest() {
        playerCountRepository.deleteAll();
        playerCount = createEntity();
    }

    @Test
    public void createPlayerCount() throws Exception {
        int databaseSizeBeforeCreate = playerCountRepository.findAll().size();

        // Create the PlayerCount

        restPlayerCountMockMvc.perform(post("/api/player-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(playerCount)))
            .andExpect(status().isCreated());

        // Validate the PlayerCount in the database
        List<PlayerCount> playerCountList = playerCountRepository.findAll();
        assertThat(playerCountList).hasSize(databaseSizeBeforeCreate + 1);
        PlayerCount testPlayerCount = playerCountList.get(playerCountList.size() - 1);
        assertThat(testPlayerCount.getRegion()).isEqualTo(DEFAULT_REGION);
        assertThat(testPlayerCount.getValue()).isEqualTo(DEFAULT_VALUE);
        assertThat(testPlayerCount.getTimestamp()).isEqualTo(DEFAULT_TIMESTAMP);
    }

    @Test
    public void createPlayerCountWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = playerCountRepository.findAll().size();

        // Create the PlayerCount with an existing ID
        PlayerCount existingPlayerCount = new PlayerCount();
        existingPlayerCount.setId("existing_id");

        // An entity with an existing ID cannot be created, so this API call must fail
        restPlayerCountMockMvc.perform(post("/api/player-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingPlayerCount)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<PlayerCount> playerCountList = playerCountRepository.findAll();
        assertThat(playerCountList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkRegionIsRequired() throws Exception {
        int databaseSizeBeforeTest = playerCountRepository.findAll().size();
        // set the field null
        playerCount.setRegion(null);

        // Create the PlayerCount, which fails.

        restPlayerCountMockMvc.perform(post("/api/player-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(playerCount)))
            .andExpect(status().isBadRequest());

        List<PlayerCount> playerCountList = playerCountRepository.findAll();
        assertThat(playerCountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkValueIsRequired() throws Exception {
        int databaseSizeBeforeTest = playerCountRepository.findAll().size();
        // set the field null
        playerCount.setValue(null);

        // Create the PlayerCount, which fails.

        restPlayerCountMockMvc.perform(post("/api/player-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(playerCount)))
            .andExpect(status().isBadRequest());

        List<PlayerCount> playerCountList = playerCountRepository.findAll();
        assertThat(playerCountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkTimestampIsRequired() throws Exception {
        int databaseSizeBeforeTest = playerCountRepository.findAll().size();
        // set the field null
        playerCount.setTimestamp(null);

        // Create the PlayerCount, which fails.

        restPlayerCountMockMvc.perform(post("/api/player-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(playerCount)))
            .andExpect(status().isBadRequest());

        List<PlayerCount> playerCountList = playerCountRepository.findAll();
        assertThat(playerCountList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllPlayerCounts() throws Exception {
        // Initialize the database
        playerCountRepository.save(playerCount);

        // Get all the playerCountList
        restPlayerCountMockMvc.perform(get("/api/player-counts?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(playerCount.getId())))
            .andExpect(jsonPath("$.[*].region").value(hasItem(DEFAULT_REGION)))
            .andExpect(jsonPath("$.[*].value").value(hasItem(DEFAULT_VALUE.intValue())))
            .andExpect(jsonPath("$.[*].timestamp").value(hasItem(sameInstant(DEFAULT_TIMESTAMP))));
    }

    @Test
    public void getPlayerCount() throws Exception {
        // Initialize the database
        playerCountRepository.save(playerCount);

        // Get the playerCount
        restPlayerCountMockMvc.perform(get("/api/player-counts/{id}", playerCount.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(playerCount.getId()))
            .andExpect(jsonPath("$.region").value(DEFAULT_REGION))
            .andExpect(jsonPath("$.value").value(DEFAULT_VALUE.intValue()))
            .andExpect(jsonPath("$.timestamp").value(sameInstant(DEFAULT_TIMESTAMP)));
    }

    @Test
    public void getNonExistingPlayerCount() throws Exception {
        // Get the playerCount
        restPlayerCountMockMvc.perform(get("/api/player-counts/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updatePlayerCount() throws Exception {
        // Initialize the database
        playerCountService.save(playerCount);

        int databaseSizeBeforeUpdate = playerCountRepository.findAll().size();

        // Update the playerCount
        PlayerCount updatedPlayerCount = playerCountRepository.findOne(playerCount.getId());
        updatedPlayerCount
                .region(UPDATED_REGION)
                .value(UPDATED_VALUE)
                .timestamp(UPDATED_TIMESTAMP);

        restPlayerCountMockMvc.perform(put("/api/player-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedPlayerCount)))
            .andExpect(status().isOk());

        // Validate the PlayerCount in the database
        List<PlayerCount> playerCountList = playerCountRepository.findAll();
        assertThat(playerCountList).hasSize(databaseSizeBeforeUpdate);
        PlayerCount testPlayerCount = playerCountList.get(playerCountList.size() - 1);
        assertThat(testPlayerCount.getRegion()).isEqualTo(UPDATED_REGION);
        assertThat(testPlayerCount.getValue()).isEqualTo(UPDATED_VALUE);
        assertThat(testPlayerCount.getTimestamp()).isEqualTo(UPDATED_TIMESTAMP);
    }

    @Test
    public void updateNonExistingPlayerCount() throws Exception {
        int databaseSizeBeforeUpdate = playerCountRepository.findAll().size();

        // Create the PlayerCount

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restPlayerCountMockMvc.perform(put("/api/player-counts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(playerCount)))
            .andExpect(status().isCreated());

        // Validate the PlayerCount in the database
        List<PlayerCount> playerCountList = playerCountRepository.findAll();
        assertThat(playerCountList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deletePlayerCount() throws Exception {
        // Initialize the database
        playerCountService.save(playerCount);

        int databaseSizeBeforeDelete = playerCountRepository.findAll().size();

        // Get the playerCount
        restPlayerCountMockMvc.perform(delete("/api/player-counts/{id}", playerCount.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<PlayerCount> playerCountList = playerCountRepository.findAll();
        assertThat(playerCountList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
