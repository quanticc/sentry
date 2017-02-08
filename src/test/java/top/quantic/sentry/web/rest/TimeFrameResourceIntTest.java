package top.quantic.sentry.web.rest;

import top.quantic.sentry.SentryApp;

import top.quantic.sentry.domain.TimeFrame;
import top.quantic.sentry.repository.TimeFrameRepository;
import top.quantic.sentry.service.TimeFrameService;
import top.quantic.sentry.service.dto.TimeFrameDTO;
import top.quantic.sentry.service.mapper.TimeFrameMapper;

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
 * Test class for the TimeFrameResource REST controller.
 *
 * @see TimeFrameResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class TimeFrameResourceIntTest {

    private static final String DEFAULT_SUBSCRIBER = "AAAAAAAAAA";
    private static final String UPDATED_SUBSCRIBER = "BBBBBBBBBB";

    private static final ZonedDateTime DEFAULT_START = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_START = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final ZonedDateTime DEFAULT_END = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_END = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final Boolean DEFAULT_INCLUSIVE = false;
    private static final Boolean UPDATED_INCLUSIVE = true;

    private static final String DEFAULT_RECURRENCE_VALUE = "AAAAAAAAAA";
    private static final String UPDATED_RECURRENCE_VALUE = "BBBBBBBBBB";

    @Inject
    private TimeFrameRepository timeFrameRepository;

    @Inject
    private TimeFrameMapper timeFrameMapper;

    @Inject
    private TimeFrameService timeFrameService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restTimeFrameMockMvc;

    private TimeFrame timeFrame;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TimeFrameResource timeFrameResource = new TimeFrameResource();
        ReflectionTestUtils.setField(timeFrameResource, "timeFrameService", timeFrameService);
        this.restTimeFrameMockMvc = MockMvcBuilders.standaloneSetup(timeFrameResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TimeFrame createEntity() {
        TimeFrame timeFrame = new TimeFrame()
                .subscriber(DEFAULT_SUBSCRIBER)
                .start(DEFAULT_START)
                .end(DEFAULT_END)
                .inclusive(DEFAULT_INCLUSIVE);
//                .recurrenceValue(DEFAULT_RECURRENCE_VALUE);
        return timeFrame;
    }

    @Before
    public void initTest() {
        timeFrameRepository.deleteAll();
        timeFrame = createEntity();
    }

    @Test
    public void createTimeFrame() throws Exception {
        int databaseSizeBeforeCreate = timeFrameRepository.findAll().size();

        // Create the TimeFrame
        TimeFrameDTO timeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame);

        restTimeFrameMockMvc.perform(post("/api/time-frames")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(timeFrameDTO)))
            .andExpect(status().isCreated());

        // Validate the TimeFrame in the database
        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeCreate + 1);
        TimeFrame testTimeFrame = timeFrameList.get(timeFrameList.size() - 1);
        assertThat(testTimeFrame.getSubscriber()).isEqualTo(DEFAULT_SUBSCRIBER);
        assertThat(testTimeFrame.getStart()).isEqualTo(DEFAULT_START);
        assertThat(testTimeFrame.getEnd()).isEqualTo(DEFAULT_END);
        assertThat(testTimeFrame.isInclusive()).isEqualTo(DEFAULT_INCLUSIVE);
//        assertThat(testTimeFrame.getRecurrenceValue()).isEqualTo(DEFAULT_RECURRENCE_VALUE);
    }

    @Test
    public void createTimeFrameWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = timeFrameRepository.findAll().size();

        // Create the TimeFrame with an existing ID
        TimeFrame existingTimeFrame = new TimeFrame();
        existingTimeFrame.setId("existing_id");
        TimeFrameDTO existingTimeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(existingTimeFrame);

        // An entity with an existing ID cannot be created, so this API call must fail
        restTimeFrameMockMvc.perform(post("/api/time-frames")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingTimeFrameDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkSubscriberIsRequired() throws Exception {
        int databaseSizeBeforeTest = timeFrameRepository.findAll().size();
        // set the field null
        timeFrame.setSubscriber(null);

        // Create the TimeFrame, which fails.
        TimeFrameDTO timeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame);

        restTimeFrameMockMvc.perform(post("/api/time-frames")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(timeFrameDTO)))
            .andExpect(status().isBadRequest());

        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkStartIsRequired() throws Exception {
        int databaseSizeBeforeTest = timeFrameRepository.findAll().size();
        // set the field null
        timeFrame.setStart(null);

        // Create the TimeFrame, which fails.
        TimeFrameDTO timeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame);

        restTimeFrameMockMvc.perform(post("/api/time-frames")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(timeFrameDTO)))
            .andExpect(status().isBadRequest());

        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkEndIsRequired() throws Exception {
        int databaseSizeBeforeTest = timeFrameRepository.findAll().size();
        // set the field null
        timeFrame.setEnd(null);

        // Create the TimeFrame, which fails.
        TimeFrameDTO timeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame);

        restTimeFrameMockMvc.perform(post("/api/time-frames")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(timeFrameDTO)))
            .andExpect(status().isBadRequest());

        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkInclusiveIsRequired() throws Exception {
        int databaseSizeBeforeTest = timeFrameRepository.findAll().size();
        // set the field null
        timeFrame.setInclusive(null);

        // Create the TimeFrame, which fails.
        TimeFrameDTO timeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame);

        restTimeFrameMockMvc.perform(post("/api/time-frames")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(timeFrameDTO)))
            .andExpect(status().isBadRequest());

        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkRecurrenceValueIsRequired() throws Exception {
        int databaseSizeBeforeTest = timeFrameRepository.findAll().size();
        // set the field null
//        timeFrame.setRecurrenceValue(null);

        // Create the TimeFrame, which fails.
        TimeFrameDTO timeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame);

        restTimeFrameMockMvc.perform(post("/api/time-frames")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(timeFrameDTO)))
            .andExpect(status().isBadRequest());

        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllTimeFrames() throws Exception {
        // Initialize the database
        timeFrameRepository.save(timeFrame);

        // Get all the timeFrameList
        restTimeFrameMockMvc.perform(get("/api/time-frames?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(timeFrame.getId())))
            .andExpect(jsonPath("$.[*].subscriber").value(hasItem(DEFAULT_SUBSCRIBER)))
            .andExpect(jsonPath("$.[*].start").value(hasItem(sameInstant(DEFAULT_START))))
            .andExpect(jsonPath("$.[*].end").value(hasItem(sameInstant(DEFAULT_END))))
            .andExpect(jsonPath("$.[*].inclusive").value(hasItem(DEFAULT_INCLUSIVE)))
            .andExpect(jsonPath("$.[*].recurrenceValue").value(hasItem(DEFAULT_RECURRENCE_VALUE)));
    }

    @Test
    public void getTimeFrame() throws Exception {
        // Initialize the database
        timeFrameRepository.save(timeFrame);

        // Get the timeFrame
        restTimeFrameMockMvc.perform(get("/api/time-frames/{id}", timeFrame.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(timeFrame.getId()))
            .andExpect(jsonPath("$.subscriber").value(DEFAULT_SUBSCRIBER))
            .andExpect(jsonPath("$.start").value(sameInstant(DEFAULT_START)))
            .andExpect(jsonPath("$.end").value(sameInstant(DEFAULT_END)))
            .andExpect(jsonPath("$.inclusive").value(DEFAULT_INCLUSIVE))
            .andExpect(jsonPath("$.recurrenceValue").value(DEFAULT_RECURRENCE_VALUE));
    }

    @Test
    public void getNonExistingTimeFrame() throws Exception {
        // Get the timeFrame
        restTimeFrameMockMvc.perform(get("/api/time-frames/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateTimeFrame() throws Exception {
        // Initialize the database
        timeFrameRepository.save(timeFrame);
        int databaseSizeBeforeUpdate = timeFrameRepository.findAll().size();

        // Update the timeFrame
        TimeFrame updatedTimeFrame = timeFrameRepository.findOne(timeFrame.getId());
        updatedTimeFrame
                .subscriber(UPDATED_SUBSCRIBER)
                .start(UPDATED_START)
                .end(UPDATED_END)
                .inclusive(UPDATED_INCLUSIVE);
//                .recurrenceValue(UPDATED_RECURRENCE_VALUE);
        TimeFrameDTO timeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(updatedTimeFrame);

        restTimeFrameMockMvc.perform(put("/api/time-frames")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(timeFrameDTO)))
            .andExpect(status().isOk());

        // Validate the TimeFrame in the database
        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeUpdate);
        TimeFrame testTimeFrame = timeFrameList.get(timeFrameList.size() - 1);
        assertThat(testTimeFrame.getSubscriber()).isEqualTo(UPDATED_SUBSCRIBER);
        assertThat(testTimeFrame.getStart()).isEqualTo(UPDATED_START);
        assertThat(testTimeFrame.getEnd()).isEqualTo(UPDATED_END);
        assertThat(testTimeFrame.isInclusive()).isEqualTo(UPDATED_INCLUSIVE);
//        assertThat(testTimeFrame.getRecurrenceValue()).isEqualTo(UPDATED_RECURRENCE_VALUE);
    }

    @Test
    public void updateNonExistingTimeFrame() throws Exception {
        int databaseSizeBeforeUpdate = timeFrameRepository.findAll().size();

        // Create the TimeFrame
        TimeFrameDTO timeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restTimeFrameMockMvc.perform(put("/api/time-frames")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(timeFrameDTO)))
            .andExpect(status().isCreated());

        // Validate the TimeFrame in the database
        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deleteTimeFrame() throws Exception {
        // Initialize the database
        timeFrameRepository.save(timeFrame);
        int databaseSizeBeforeDelete = timeFrameRepository.findAll().size();

        // Get the timeFrame
        restTimeFrameMockMvc.perform(delete("/api/time-frames/{id}", timeFrame.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<TimeFrame> timeFrameList = timeFrameRepository.findAll();
        assertThat(timeFrameList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
