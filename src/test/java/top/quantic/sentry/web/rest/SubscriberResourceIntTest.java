package top.quantic.sentry.web.rest;

import top.quantic.sentry.SentryApp;

import top.quantic.sentry.domain.Subscriber;
import top.quantic.sentry.repository.SubscriberRepository;
import top.quantic.sentry.service.SubscriberService;
import top.quantic.sentry.service.dto.SubscriberDTO;
import top.quantic.sentry.service.mapper.SubscriberMapper;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the SubscriberResource REST controller.
 *
 * @see SubscriberResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class SubscriberResourceIntTest {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_CHANNEL = "AAAAAAAAAA";
    private static final String UPDATED_CHANNEL = "BBBBBBBBBB";

    private static final String DEFAULT_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_TYPE = "BBBBBBBBBB";

    private static final String DEFAULT_TYPE_PARAMETERS = "AAAAAAAAAA";
    private static final String UPDATED_TYPE_PARAMETERS = "BBBBBBBBBB";

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private SubscriberMapper subscriberMapper;

    @Inject
    private SubscriberService subscriberService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restSubscriberMockMvc;

    private Subscriber subscriber;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SubscriberResource subscriberResource = new SubscriberResource();
        ReflectionTestUtils.setField(subscriberResource, "subscriberService", subscriberService);
        this.restSubscriberMockMvc = MockMvcBuilders.standaloneSetup(subscriberResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Subscriber createEntity() {
        Subscriber subscriber = new Subscriber()
                .name(DEFAULT_NAME)
                .channel(DEFAULT_CHANNEL)
                .type(DEFAULT_TYPE);
//                .typeParameters(DEFAULT_TYPE_PARAMETERS);
        return subscriber;
    }

    @Before
    public void initTest() {
        subscriberRepository.deleteAll();
        subscriber = createEntity();
    }

    @Test
    public void createSubscriber() throws Exception {
        int databaseSizeBeforeCreate = subscriberRepository.findAll().size();

        // Create the Subscriber
        SubscriberDTO subscriberDTO = subscriberMapper.subscriberToSubscriberDTO(subscriber);

        restSubscriberMockMvc.perform(post("/api/subscribers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subscriberDTO)))
            .andExpect(status().isCreated());

        // Validate the Subscriber in the database
        List<Subscriber> subscriberList = subscriberRepository.findAll();
        assertThat(subscriberList).hasSize(databaseSizeBeforeCreate + 1);
        Subscriber testSubscriber = subscriberList.get(subscriberList.size() - 1);
        assertThat(testSubscriber.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testSubscriber.getChannel()).isEqualTo(DEFAULT_CHANNEL);
        assertThat(testSubscriber.getType()).isEqualTo(DEFAULT_TYPE);
//        assertThat(testSubscriber.getTypeParameters()).isEqualTo(DEFAULT_TYPE_PARAMETERS);
    }

    @Test
    public void createSubscriberWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = subscriberRepository.findAll().size();

        // Create the Subscriber with an existing ID
        Subscriber existingSubscriber = new Subscriber();
        existingSubscriber.setId("existing_id");
        SubscriberDTO existingSubscriberDTO = subscriberMapper.subscriberToSubscriberDTO(existingSubscriber);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSubscriberMockMvc.perform(post("/api/subscribers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingSubscriberDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Subscriber> subscriberList = subscriberRepository.findAll();
        assertThat(subscriberList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkChannelIsRequired() throws Exception {
        int databaseSizeBeforeTest = subscriberRepository.findAll().size();
        // set the field null
        subscriber.setChannel(null);

        // Create the Subscriber, which fails.
        SubscriberDTO subscriberDTO = subscriberMapper.subscriberToSubscriberDTO(subscriber);

        restSubscriberMockMvc.perform(post("/api/subscribers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subscriberDTO)))
            .andExpect(status().isBadRequest());

        List<Subscriber> subscriberList = subscriberRepository.findAll();
        assertThat(subscriberList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkTypeIsRequired() throws Exception {
        int databaseSizeBeforeTest = subscriberRepository.findAll().size();
        // set the field null
        subscriber.setType(null);

        // Create the Subscriber, which fails.
        SubscriberDTO subscriberDTO = subscriberMapper.subscriberToSubscriberDTO(subscriber);

        restSubscriberMockMvc.perform(post("/api/subscribers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subscriberDTO)))
            .andExpect(status().isBadRequest());

        List<Subscriber> subscriberList = subscriberRepository.findAll();
        assertThat(subscriberList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkTypeParametersIsRequired() throws Exception {
        int databaseSizeBeforeTest = subscriberRepository.findAll().size();
        // set the field null
//        subscriber.setTypeParameters(null);

        // Create the Subscriber, which fails.
        SubscriberDTO subscriberDTO = subscriberMapper.subscriberToSubscriberDTO(subscriber);

        restSubscriberMockMvc.perform(post("/api/subscribers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subscriberDTO)))
            .andExpect(status().isBadRequest());

        List<Subscriber> subscriberList = subscriberRepository.findAll();
        assertThat(subscriberList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllSubscribers() throws Exception {
        // Initialize the database
        subscriberRepository.save(subscriber);

        // Get all the subscriberList
        restSubscriberMockMvc.perform(get("/api/subscribers?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(subscriber.getId())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].channel").value(hasItem(DEFAULT_CHANNEL)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE)))
            .andExpect(jsonPath("$.[*].typeParameters").value(hasItem(DEFAULT_TYPE_PARAMETERS)));
    }

    @Test
    public void getSubscriber() throws Exception {
        // Initialize the database
        subscriberRepository.save(subscriber);

        // Get the subscriber
        restSubscriberMockMvc.perform(get("/api/subscribers/{id}", subscriber.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(subscriber.getId()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.channel").value(DEFAULT_CHANNEL))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE))
            .andExpect(jsonPath("$.typeParameters").value(DEFAULT_TYPE_PARAMETERS));
    }

    @Test
    public void getNonExistingSubscriber() throws Exception {
        // Get the subscriber
        restSubscriberMockMvc.perform(get("/api/subscribers/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateSubscriber() throws Exception {
        // Initialize the database
        subscriberRepository.save(subscriber);
        int databaseSizeBeforeUpdate = subscriberRepository.findAll().size();

        // Update the subscriber
        Subscriber updatedSubscriber = subscriberRepository.findOne(subscriber.getId());
        updatedSubscriber
                .name(UPDATED_NAME)
                .channel(UPDATED_CHANNEL)
                .type(UPDATED_TYPE);
//                .typeParameters(UPDATED_TYPE_PARAMETERS);
        SubscriberDTO subscriberDTO = subscriberMapper.subscriberToSubscriberDTO(updatedSubscriber);

        restSubscriberMockMvc.perform(put("/api/subscribers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subscriberDTO)))
            .andExpect(status().isOk());

        // Validate the Subscriber in the database
        List<Subscriber> subscriberList = subscriberRepository.findAll();
        assertThat(subscriberList).hasSize(databaseSizeBeforeUpdate);
        Subscriber testSubscriber = subscriberList.get(subscriberList.size() - 1);
        assertThat(testSubscriber.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testSubscriber.getChannel()).isEqualTo(UPDATED_CHANNEL);
        assertThat(testSubscriber.getType()).isEqualTo(UPDATED_TYPE);
//        assertThat(testSubscriber.getTypeParameters()).isEqualTo(UPDATED_TYPE_PARAMETERS);
    }

    @Test
    public void updateNonExistingSubscriber() throws Exception {
        int databaseSizeBeforeUpdate = subscriberRepository.findAll().size();

        // Create the Subscriber
        SubscriberDTO subscriberDTO = subscriberMapper.subscriberToSubscriberDTO(subscriber);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restSubscriberMockMvc.perform(put("/api/subscribers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subscriberDTO)))
            .andExpect(status().isCreated());

        // Validate the Subscriber in the database
        List<Subscriber> subscriberList = subscriberRepository.findAll();
        assertThat(subscriberList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deleteSubscriber() throws Exception {
        // Initialize the database
        subscriberRepository.save(subscriber);
        int databaseSizeBeforeDelete = subscriberRepository.findAll().size();

        // Get the subscriber
        restSubscriberMockMvc.perform(delete("/api/subscribers/{id}", subscriber.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Subscriber> subscriberList = subscriberRepository.findAll();
        assertThat(subscriberList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
