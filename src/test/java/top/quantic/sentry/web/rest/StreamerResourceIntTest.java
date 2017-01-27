package top.quantic.sentry.web.rest;

import top.quantic.sentry.SentryApp;

import top.quantic.sentry.domain.Streamer;
import top.quantic.sentry.repository.StreamerRepository;
import top.quantic.sentry.service.StreamerService;
import top.quantic.sentry.service.dto.StreamerDTO;
import top.quantic.sentry.service.mapper.StreamerMapper;

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
 * Test class for the StreamerResource REST controller.
 *
 * @see StreamerResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class StreamerResourceIntTest {

    private static final String DEFAULT_PROVIDER = "AAAAAAAAAA";
    private static final String UPDATED_PROVIDER = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_LEAGUE = "AAAAAAAAAA";
    private static final String UPDATED_LEAGUE = "BBBBBBBBBB";

    private static final String DEFAULT_DIVISION = "AAAAAAAAAA";
    private static final String UPDATED_DIVISION = "BBBBBBBBBB";

    private static final String DEFAULT_TITLE_FILTER = "AAAAAAAAAA";
    private static final String UPDATED_TITLE_FILTER = "BBBBBBBBBB";

    private static final String DEFAULT_ANNOUNCEMENT = "AAAAAAAAAA";
    private static final String UPDATED_ANNOUNCEMENT = "BBBBBBBBBB";

    private static final ZonedDateTime DEFAULT_LAST_ANNOUNCEMENT = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_LAST_ANNOUNCEMENT = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final Boolean DEFAULT_ENABLED = false;
    private static final Boolean UPDATED_ENABLED = true;

    private static final Long DEFAULT_LAST_STREAM_ID = 1L;
    private static final Long UPDATED_LAST_STREAM_ID = 2L;

    @Inject
    private StreamerRepository streamerRepository;

    @Inject
    private StreamerMapper streamerMapper;

    @Inject
    private StreamerService streamerService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restStreamerMockMvc;

    private Streamer streamer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        StreamerResource streamerResource = new StreamerResource();
        ReflectionTestUtils.setField(streamerResource, "streamerService", streamerService);
        this.restStreamerMockMvc = MockMvcBuilders.standaloneSetup(streamerResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Streamer createEntity() {
        Streamer streamer = new Streamer()
                .provider(DEFAULT_PROVIDER)
                .name(DEFAULT_NAME)
                .league(DEFAULT_LEAGUE)
                .division(DEFAULT_DIVISION)
                .titleFilter(DEFAULT_TITLE_FILTER)
                .announcement(DEFAULT_ANNOUNCEMENT)
                .lastAnnouncement(DEFAULT_LAST_ANNOUNCEMENT)
                .enabled(DEFAULT_ENABLED)
                .lastStreamId(DEFAULT_LAST_STREAM_ID);
        return streamer;
    }

    @Before
    public void initTest() {
        streamerRepository.deleteAll();
        streamer = createEntity();
    }

    @Test
    public void createStreamer() throws Exception {
        int databaseSizeBeforeCreate = streamerRepository.findAll().size();

        // Create the Streamer
        StreamerDTO streamerDTO = streamerMapper.streamerToStreamerDTO(streamer);

        restStreamerMockMvc.perform(post("/api/streamers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(streamerDTO)))
            .andExpect(status().isCreated());

        // Validate the Streamer in the database
        List<Streamer> streamerList = streamerRepository.findAll();
        assertThat(streamerList).hasSize(databaseSizeBeforeCreate + 1);
        Streamer testStreamer = streamerList.get(streamerList.size() - 1);
        assertThat(testStreamer.getProvider()).isEqualTo(DEFAULT_PROVIDER);
        assertThat(testStreamer.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testStreamer.getLeague()).isEqualTo(DEFAULT_LEAGUE);
        assertThat(testStreamer.getDivision()).isEqualTo(DEFAULT_DIVISION);
        assertThat(testStreamer.getTitleFilter()).isEqualTo(DEFAULT_TITLE_FILTER);
        assertThat(testStreamer.getAnnouncement()).isEqualTo(DEFAULT_ANNOUNCEMENT);
        assertThat(testStreamer.getLastAnnouncement()).isEqualTo(DEFAULT_LAST_ANNOUNCEMENT);
        assertThat(testStreamer.isEnabled()).isEqualTo(DEFAULT_ENABLED);
        assertThat(testStreamer.getLastStreamId()).isEqualTo(DEFAULT_LAST_STREAM_ID);
    }

    @Test
    public void createStreamerWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = streamerRepository.findAll().size();

        // Create the Streamer with an existing ID
        Streamer existingStreamer = new Streamer();
        existingStreamer.setId("existing_id");
        StreamerDTO existingStreamerDTO = streamerMapper.streamerToStreamerDTO(existingStreamer);

        // An entity with an existing ID cannot be created, so this API call must fail
        restStreamerMockMvc.perform(post("/api/streamers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingStreamerDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Streamer> streamerList = streamerRepository.findAll();
        assertThat(streamerList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkProviderIsRequired() throws Exception {
        int databaseSizeBeforeTest = streamerRepository.findAll().size();
        // set the field null
        streamer.setProvider(null);

        // Create the Streamer, which fails.
        StreamerDTO streamerDTO = streamerMapper.streamerToStreamerDTO(streamer);

        restStreamerMockMvc.perform(post("/api/streamers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(streamerDTO)))
            .andExpect(status().isBadRequest());

        List<Streamer> streamerList = streamerRepository.findAll();
        assertThat(streamerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = streamerRepository.findAll().size();
        // set the field null
        streamer.setName(null);

        // Create the Streamer, which fails.
        StreamerDTO streamerDTO = streamerMapper.streamerToStreamerDTO(streamer);

        restStreamerMockMvc.perform(post("/api/streamers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(streamerDTO)))
            .andExpect(status().isBadRequest());

        List<Streamer> streamerList = streamerRepository.findAll();
        assertThat(streamerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkEnabledIsRequired() throws Exception {
        int databaseSizeBeforeTest = streamerRepository.findAll().size();
        // set the field null
        streamer.setEnabled(null);

        // Create the Streamer, which fails.
        StreamerDTO streamerDTO = streamerMapper.streamerToStreamerDTO(streamer);

        restStreamerMockMvc.perform(post("/api/streamers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(streamerDTO)))
            .andExpect(status().isBadRequest());

        List<Streamer> streamerList = streamerRepository.findAll();
        assertThat(streamerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllStreamers() throws Exception {
        // Initialize the database
        streamerRepository.save(streamer);

        // Get all the streamerList
        restStreamerMockMvc.perform(get("/api/streamers?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(streamer.getId())))
            .andExpect(jsonPath("$.[*].provider").value(hasItem(DEFAULT_PROVIDER.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].league").value(hasItem(DEFAULT_LEAGUE.toString())))
            .andExpect(jsonPath("$.[*].division").value(hasItem(DEFAULT_DIVISION.toString())))
            .andExpect(jsonPath("$.[*].titleFilter").value(hasItem(DEFAULT_TITLE_FILTER.toString())))
            .andExpect(jsonPath("$.[*].announcement").value(hasItem(DEFAULT_ANNOUNCEMENT.toString())))
            .andExpect(jsonPath("$.[*].lastAnnouncement").value(hasItem(sameInstant(DEFAULT_LAST_ANNOUNCEMENT))))
            .andExpect(jsonPath("$.[*].enabled").value(hasItem(DEFAULT_ENABLED.booleanValue())))
            .andExpect(jsonPath("$.[*].lastStreamId").value(hasItem(DEFAULT_LAST_STREAM_ID.intValue())));
    }

    @Test
    public void getStreamer() throws Exception {
        // Initialize the database
        streamerRepository.save(streamer);

        // Get the streamer
        restStreamerMockMvc.perform(get("/api/streamers/{id}", streamer.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(streamer.getId()))
            .andExpect(jsonPath("$.provider").value(DEFAULT_PROVIDER.toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.league").value(DEFAULT_LEAGUE.toString()))
            .andExpect(jsonPath("$.division").value(DEFAULT_DIVISION.toString()))
            .andExpect(jsonPath("$.titleFilter").value(DEFAULT_TITLE_FILTER.toString()))
            .andExpect(jsonPath("$.announcement").value(DEFAULT_ANNOUNCEMENT.toString()))
            .andExpect(jsonPath("$.lastAnnouncement").value(sameInstant(DEFAULT_LAST_ANNOUNCEMENT)))
            .andExpect(jsonPath("$.enabled").value(DEFAULT_ENABLED.booleanValue()))
            .andExpect(jsonPath("$.lastStreamId").value(DEFAULT_LAST_STREAM_ID.intValue()));
    }

    @Test
    public void getNonExistingStreamer() throws Exception {
        // Get the streamer
        restStreamerMockMvc.perform(get("/api/streamers/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateStreamer() throws Exception {
        // Initialize the database
        streamerRepository.save(streamer);
        int databaseSizeBeforeUpdate = streamerRepository.findAll().size();

        // Update the streamer
        Streamer updatedStreamer = streamerRepository.findOne(streamer.getId());
        updatedStreamer
                .provider(UPDATED_PROVIDER)
                .name(UPDATED_NAME)
                .league(UPDATED_LEAGUE)
                .division(UPDATED_DIVISION)
                .titleFilter(UPDATED_TITLE_FILTER)
                .announcement(UPDATED_ANNOUNCEMENT)
                .lastAnnouncement(UPDATED_LAST_ANNOUNCEMENT)
                .enabled(UPDATED_ENABLED)
                .lastStreamId(UPDATED_LAST_STREAM_ID);
        StreamerDTO streamerDTO = streamerMapper.streamerToStreamerDTO(updatedStreamer);

        restStreamerMockMvc.perform(put("/api/streamers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(streamerDTO)))
            .andExpect(status().isOk());

        // Validate the Streamer in the database
        List<Streamer> streamerList = streamerRepository.findAll();
        assertThat(streamerList).hasSize(databaseSizeBeforeUpdate);
        Streamer testStreamer = streamerList.get(streamerList.size() - 1);
        assertThat(testStreamer.getProvider()).isEqualTo(UPDATED_PROVIDER);
        assertThat(testStreamer.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testStreamer.getLeague()).isEqualTo(UPDATED_LEAGUE);
        assertThat(testStreamer.getDivision()).isEqualTo(UPDATED_DIVISION);
        assertThat(testStreamer.getTitleFilter()).isEqualTo(UPDATED_TITLE_FILTER);
        assertThat(testStreamer.getAnnouncement()).isEqualTo(UPDATED_ANNOUNCEMENT);
        assertThat(testStreamer.getLastAnnouncement()).isEqualTo(UPDATED_LAST_ANNOUNCEMENT);
        assertThat(testStreamer.isEnabled()).isEqualTo(UPDATED_ENABLED);
        assertThat(testStreamer.getLastStreamId()).isEqualTo(UPDATED_LAST_STREAM_ID);
    }

    @Test
    public void updateNonExistingStreamer() throws Exception {
        int databaseSizeBeforeUpdate = streamerRepository.findAll().size();

        // Create the Streamer
        StreamerDTO streamerDTO = streamerMapper.streamerToStreamerDTO(streamer);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restStreamerMockMvc.perform(put("/api/streamers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(streamerDTO)))
            .andExpect(status().isCreated());

        // Validate the Streamer in the database
        List<Streamer> streamerList = streamerRepository.findAll();
        assertThat(streamerList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deleteStreamer() throws Exception {
        // Initialize the database
        streamerRepository.save(streamer);
        int databaseSizeBeforeDelete = streamerRepository.findAll().size();

        // Get the streamer
        restStreamerMockMvc.perform(delete("/api/streamers/{id}", streamer.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Streamer> streamerList = streamerRepository.findAll();
        assertThat(streamerList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
