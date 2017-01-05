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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.quantic.sentry.SentryApp;
import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.repository.BotRepository;
import top.quantic.sentry.service.BotService;
import top.quantic.sentry.service.dto.BotDTO;
import top.quantic.sentry.service.mapper.BotMapper;

import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the BotResource REST controller.
 *
 * @see BotResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class BotResourceIntTest {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_TOKEN = "BBBBBBBBBB";

    private static final Boolean DEFAULT_AUTO_LOGIN = false;
    private static final Boolean UPDATED_AUTO_LOGIN = true;

    private static final Boolean DEFAULT_DAEMON = false;
    private static final Boolean UPDATED_DAEMON = true;

    private static final Integer DEFAULT_MAX_MISSED_PINGS = 1;
    private static final Integer UPDATED_MAX_MISSED_PINGS = 2;

    private static final Integer DEFAULT_MAX_RECONNECT_ATTEMPTS = 1;
    private static final Integer UPDATED_MAX_RECONNECT_ATTEMPTS = 2;

    private static final Integer DEFAULT_SHARD_COUNT = 1;
    private static final Integer UPDATED_SHARD_COUNT = 2;

    private static final Boolean DEFAULT_PRIMARY = false;
    private static final Boolean UPDATED_PRIMARY = true;

    private static final String DEFAULT_TAGS = "AAAAAAAAAA";
    private static final String UPDATED_TAGS = "BBBBBBBBBB";

    @Inject
    private BotRepository botRepository;

    @Inject
    private BotMapper botMapper;

    @Inject
    private BotService botService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restBotMockMvc;

    private Bot bot;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        BotResource botResource = new BotResource();
        ReflectionTestUtils.setField(botResource, "botService", botService);
        this.restBotMockMvc = MockMvcBuilders.standaloneSetup(botResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Bot createEntity() {
        Bot bot = new Bot()
                .name(DEFAULT_NAME)
                .token(DEFAULT_TOKEN)
                .autoLogin(DEFAULT_AUTO_LOGIN)
                .daemon(DEFAULT_DAEMON)
                .maxMissedPings(DEFAULT_MAX_MISSED_PINGS)
                .maxReconnectAttempts(DEFAULT_MAX_RECONNECT_ATTEMPTS)
                .shardCount(DEFAULT_SHARD_COUNT)
                .primary(DEFAULT_PRIMARY)
                .tags(DEFAULT_TAGS);
        return bot;
    }

    @Before
    public void initTest() {
        botRepository.deleteAll();
        bot = createEntity();
    }

    @Test
    public void createBot() throws Exception {
        int databaseSizeBeforeCreate = botRepository.findAll().size();

        // Create the Bot
        BotDTO botDTO = botMapper.botToBotDTO(bot);

        restBotMockMvc.perform(post("/api/bots")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(botDTO)))
            .andExpect(status().isCreated());

        // Validate the Bot in the database
        List<Bot> botList = botRepository.findAll();
        assertThat(botList).hasSize(databaseSizeBeforeCreate + 1);
        Bot testBot = botList.get(botList.size() - 1);
        assertThat(testBot.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testBot.getToken()).isEqualTo(DEFAULT_TOKEN);
        assertThat(testBot.isAutoLogin()).isEqualTo(DEFAULT_AUTO_LOGIN);
        assertThat(testBot.isDaemon()).isEqualTo(DEFAULT_DAEMON);
        assertThat(testBot.getMaxMissedPings()).isEqualTo(DEFAULT_MAX_MISSED_PINGS);
        assertThat(testBot.getMaxReconnectAttempts()).isEqualTo(DEFAULT_MAX_RECONNECT_ATTEMPTS);
        assertThat(testBot.getShardCount()).isEqualTo(DEFAULT_SHARD_COUNT);
        assertThat(testBot.isPrimary()).isEqualTo(DEFAULT_PRIMARY);
        assertThat(testBot.getTags()).isEqualTo(DEFAULT_TAGS);
    }

    @Test
    public void createBotWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = botRepository.findAll().size();

        // Create the Bot with an existing ID
        Bot existingBot = new Bot();
        existingBot.setId("existing_id");
        BotDTO existingBotDTO = botMapper.botToBotDTO(existingBot);

        // An entity with an existing ID cannot be created, so this API call must fail
        restBotMockMvc.perform(post("/api/bots")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingBotDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Bot> botList = botRepository.findAll();
        assertThat(botList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkTokenIsRequired() throws Exception {
        int databaseSizeBeforeTest = botRepository.findAll().size();
        // set the field null
        bot.setToken(null);

        // Create the Bot, which fails.
        BotDTO botDTO = botMapper.botToBotDTO(bot);

        restBotMockMvc.perform(post("/api/bots")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(botDTO)))
            .andExpect(status().isBadRequest());

        List<Bot> botList = botRepository.findAll();
        assertThat(botList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllBots() throws Exception {
        // Initialize the database
        botRepository.save(bot);

        // Get all the botList
        restBotMockMvc.perform(get("/api/bots?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(bot.getId())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].token").value(hasItem(DEFAULT_TOKEN.toString())))
            .andExpect(jsonPath("$.[*].autoLogin").value(hasItem(DEFAULT_AUTO_LOGIN.booleanValue())))
            .andExpect(jsonPath("$.[*].daemon").value(hasItem(DEFAULT_DAEMON.booleanValue())))
            .andExpect(jsonPath("$.[*].maxMissedPings").value(hasItem(DEFAULT_MAX_MISSED_PINGS)))
            .andExpect(jsonPath("$.[*].maxReconnectAttempts").value(hasItem(DEFAULT_MAX_RECONNECT_ATTEMPTS)))
            .andExpect(jsonPath("$.[*].shardCount").value(hasItem(DEFAULT_SHARD_COUNT)))
            .andExpect(jsonPath("$.[*].primary").value(hasItem(DEFAULT_PRIMARY.booleanValue())))
            .andExpect(jsonPath("$.[*].tags").value(hasItem(DEFAULT_TAGS.toString())));
    }

    @Test
    public void getBot() throws Exception {
        // Initialize the database
        botRepository.save(bot);

        // Get the bot
        restBotMockMvc.perform(get("/api/bots/{id}", bot.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(bot.getId()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.token").value(DEFAULT_TOKEN.toString()))
            .andExpect(jsonPath("$.autoLogin").value(DEFAULT_AUTO_LOGIN.booleanValue()))
            .andExpect(jsonPath("$.daemon").value(DEFAULT_DAEMON.booleanValue()))
            .andExpect(jsonPath("$.maxMissedPings").value(DEFAULT_MAX_MISSED_PINGS))
            .andExpect(jsonPath("$.maxReconnectAttempts").value(DEFAULT_MAX_RECONNECT_ATTEMPTS))
            .andExpect(jsonPath("$.shardCount").value(DEFAULT_SHARD_COUNT))
            .andExpect(jsonPath("$.primary").value(DEFAULT_PRIMARY.booleanValue()))
            .andExpect(jsonPath("$.tags").value(DEFAULT_TAGS.toString()));
    }

    // TODO: Test with injected MapStruct decorator
    //@Test
    public void getNonExistingBot() throws Exception {
        // Get the bot
        restBotMockMvc.perform(get("/api/bots/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateBot() throws Exception {
        // Initialize the database
        botRepository.save(bot);
        int databaseSizeBeforeUpdate = botRepository.findAll().size();

        // Update the bot
        Bot updatedBot = botRepository.findOne(bot.getId());
        updatedBot
                .name(UPDATED_NAME)
                .token(UPDATED_TOKEN)
                .autoLogin(UPDATED_AUTO_LOGIN)
                .daemon(UPDATED_DAEMON)
                .maxMissedPings(UPDATED_MAX_MISSED_PINGS)
                .maxReconnectAttempts(UPDATED_MAX_RECONNECT_ATTEMPTS)
                .shardCount(UPDATED_SHARD_COUNT)
                .primary(UPDATED_PRIMARY)
                .tags(UPDATED_TAGS);
        BotDTO botDTO = botMapper.botToBotDTO(updatedBot);

        restBotMockMvc.perform(put("/api/bots")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(botDTO)))
            .andExpect(status().isOk());

        // Validate the Bot in the database
        List<Bot> botList = botRepository.findAll();
        assertThat(botList).hasSize(databaseSizeBeforeUpdate);
        Bot testBot = botList.get(botList.size() - 1);
        assertThat(testBot.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testBot.getToken()).isEqualTo(UPDATED_TOKEN);
        assertThat(testBot.isAutoLogin()).isEqualTo(UPDATED_AUTO_LOGIN);
        assertThat(testBot.isDaemon()).isEqualTo(UPDATED_DAEMON);
        assertThat(testBot.getMaxMissedPings()).isEqualTo(UPDATED_MAX_MISSED_PINGS);
        assertThat(testBot.getMaxReconnectAttempts()).isEqualTo(UPDATED_MAX_RECONNECT_ATTEMPTS);
        assertThat(testBot.getShardCount()).isEqualTo(UPDATED_SHARD_COUNT);
        assertThat(testBot.isPrimary()).isEqualTo(UPDATED_PRIMARY);
        assertThat(testBot.getTags()).isEqualTo(UPDATED_TAGS);
    }

    @Test
    public void updateNonExistingBot() throws Exception {
        int databaseSizeBeforeUpdate = botRepository.findAll().size();

        // Create the Bot
        BotDTO botDTO = botMapper.botToBotDTO(bot);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restBotMockMvc.perform(put("/api/bots")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(botDTO)))
            .andExpect(status().isCreated());

        // Validate the Bot in the database
        List<Bot> botList = botRepository.findAll();
        assertThat(botList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deleteBot() throws Exception {
        // Initialize the database
        botRepository.save(bot);
        int databaseSizeBeforeDelete = botRepository.findAll().size();

        // Get the bot
        restBotMockMvc.perform(delete("/api/bots/{id}", bot.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Bot> botList = botRepository.findAll();
        assertThat(botList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
