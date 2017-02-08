package top.quantic.sentry.web.rest;

import top.quantic.sentry.SentryApp;

import top.quantic.sentry.domain.GameServer;
import top.quantic.sentry.repository.GameServerRepository;
import top.quantic.sentry.service.GameServerService;
import top.quantic.sentry.service.dto.GameServerDTO;
import top.quantic.sentry.service.mapper.GameServerMapper;

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
 * Test class for the GameServerResource REST controller.
 *
 * @see GameServerResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class GameServerResourceIntTest {

    private static final String DEFAULT_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_ADDRESS = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Integer DEFAULT_PING = 1;
    private static final Integer UPDATED_PING = 2;

    private static final Integer DEFAULT_PLAYERS = 1;
    private static final Integer UPDATED_PLAYERS = 2;

    private static final Integer DEFAULT_MAX_PLAYERS = 1;
    private static final Integer UPDATED_MAX_PLAYERS = 2;

    private static final String DEFAULT_MAP = "AAAAAAAAAA";
    private static final String UPDATED_MAP = "BBBBBBBBBB";

    private static final Integer DEFAULT_VERSION = 1;
    private static final Integer UPDATED_VERSION = 2;

    private static final String DEFAULT_RCON_PASSWORD = "AAAAAAAAAA";
    private static final String UPDATED_RCON_PASSWORD = "BBBBBBBBBB";

    private static final String DEFAULT_SV_PASSWORD = "AAAAAAAAAA";
    private static final String UPDATED_SV_PASSWORD = "BBBBBBBBBB";

    private static final Integer DEFAULT_TV_PORT = 1;
    private static final Integer UPDATED_TV_PORT = 2;

    private static final Boolean DEFAULT_EXPIRES = false;
    private static final Boolean UPDATED_EXPIRES = true;

    private static final ZonedDateTime DEFAULT_EXPIRATION_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_EXPIRATION_DATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final ZonedDateTime DEFAULT_EXPIRATION_CHECK_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_EXPIRATION_CHECK_DATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final ZonedDateTime DEFAULT_STATUS_CHECK_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_STATUS_CHECK_DATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final ZonedDateTime DEFAULT_LAST_VALID_PING = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_LAST_VALID_PING = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final ZonedDateTime DEFAULT_LAST_RCON_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_LAST_RCON_DATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final ZonedDateTime DEFAULT_LAST_GAME_UPDATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_LAST_GAME_UPDATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final Boolean DEFAULT_UPDATING = false;
    private static final Boolean UPDATED_UPDATING = true;

    private static final Integer DEFAULT_UPDATE_ATTEMPTS = 1;
    private static final Integer UPDATED_UPDATE_ATTEMPTS = 2;

    private static final ZonedDateTime DEFAULT_LAST_UPDATE_START = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_LAST_UPDATE_START = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    private static final ZonedDateTime DEFAULT_LAST_RCON_ANNOUNCE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_LAST_RCON_ANNOUNCE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    @Inject
    private GameServerRepository gameServerRepository;

    @Inject
    private GameServerMapper gameServerMapper;

    @Inject
    private GameServerService gameServerService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restGameServerMockMvc;

    private GameServer gameServer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        GameServerResource gameServerResource = new GameServerResource();
        ReflectionTestUtils.setField(gameServerResource, "gameServerService", gameServerService);
        this.restGameServerMockMvc = MockMvcBuilders.standaloneSetup(gameServerResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GameServer createEntity() {
        GameServer gameServer = new GameServer()
                .address(DEFAULT_ADDRESS)
                .name(DEFAULT_NAME)
                .ping(DEFAULT_PING)
                .players(DEFAULT_PLAYERS)
                .maxPlayers(DEFAULT_MAX_PLAYERS)
                .map(DEFAULT_MAP)
                .version(DEFAULT_VERSION)
                .rconPassword(DEFAULT_RCON_PASSWORD)
                .svPassword(DEFAULT_SV_PASSWORD)
                .tvPort(DEFAULT_TV_PORT)
                .expires(DEFAULT_EXPIRES)
                .expirationDate(DEFAULT_EXPIRATION_DATE)
                .expirationCheckDate(DEFAULT_EXPIRATION_CHECK_DATE)
                .statusCheckDate(DEFAULT_STATUS_CHECK_DATE)
                .lastValidPing(DEFAULT_LAST_VALID_PING)
                .lastRconDate(DEFAULT_LAST_RCON_DATE)
                .lastGameUpdate(DEFAULT_LAST_GAME_UPDATE)
                .updating(DEFAULT_UPDATING)
                .updateAttempts(DEFAULT_UPDATE_ATTEMPTS)
                .lastUpdateStart(DEFAULT_LAST_UPDATE_START)
                .lastRconAnnounce(DEFAULT_LAST_RCON_ANNOUNCE);
        return gameServer;
    }

    @Before
    public void initTest() {
        gameServerRepository.deleteAll();
        gameServer = createEntity();
    }

    @Test
    public void createGameServer() throws Exception {
        int databaseSizeBeforeCreate = gameServerRepository.findAll().size();

        // Create the GameServer
        GameServerDTO gameServerDTO = gameServerMapper.gameServerToGameServerDTO(gameServer);

        restGameServerMockMvc.perform(post("/api/game-servers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(gameServerDTO)))
            .andExpect(status().isCreated());

        // Validate the GameServer in the database
        List<GameServer> gameServerList = gameServerRepository.findAll();
        assertThat(gameServerList).hasSize(databaseSizeBeforeCreate + 1);
        GameServer testGameServer = gameServerList.get(gameServerList.size() - 1);
        assertThat(testGameServer.getAddress()).isEqualTo(DEFAULT_ADDRESS);
        assertThat(testGameServer.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testGameServer.getPing()).isEqualTo(DEFAULT_PING);
        assertThat(testGameServer.getPlayers()).isEqualTo(DEFAULT_PLAYERS);
        assertThat(testGameServer.getMaxPlayers()).isEqualTo(DEFAULT_MAX_PLAYERS);
        assertThat(testGameServer.getMap()).isEqualTo(DEFAULT_MAP);
        assertThat(testGameServer.getVersion()).isEqualTo(DEFAULT_VERSION);
        assertThat(testGameServer.getRconPassword()).isEqualTo(DEFAULT_RCON_PASSWORD);
        assertThat(testGameServer.getSvPassword()).isEqualTo(DEFAULT_SV_PASSWORD);
        assertThat(testGameServer.getTvPort()).isEqualTo(DEFAULT_TV_PORT);
        assertThat(testGameServer.isExpires()).isEqualTo(DEFAULT_EXPIRES);
        assertThat(testGameServer.getExpirationDate()).isEqualTo(DEFAULT_EXPIRATION_DATE);
        assertThat(testGameServer.getExpirationCheckDate()).isEqualTo(DEFAULT_EXPIRATION_CHECK_DATE);
        assertThat(testGameServer.getStatusCheckDate()).isEqualTo(DEFAULT_STATUS_CHECK_DATE);
        assertThat(testGameServer.getLastValidPing()).isEqualTo(DEFAULT_LAST_VALID_PING);
        assertThat(testGameServer.getLastRconDate()).isEqualTo(DEFAULT_LAST_RCON_DATE);
        assertThat(testGameServer.getLastGameUpdate()).isEqualTo(DEFAULT_LAST_GAME_UPDATE);
        assertThat(testGameServer.isUpdating()).isEqualTo(DEFAULT_UPDATING);
        assertThat(testGameServer.getUpdateAttempts()).isEqualTo(DEFAULT_UPDATE_ATTEMPTS);
        assertThat(testGameServer.getLastUpdateStart()).isEqualTo(DEFAULT_LAST_UPDATE_START);
        assertThat(testGameServer.getLastRconAnnounce()).isEqualTo(DEFAULT_LAST_RCON_ANNOUNCE);
    }

    @Test
    public void createGameServerWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = gameServerRepository.findAll().size();

        // Create the GameServer with an existing ID
        GameServer existingGameServer = new GameServer();
        existingGameServer.setId("existing_id");
        GameServerDTO existingGameServerDTO = gameServerMapper.gameServerToGameServerDTO(existingGameServer);

        // An entity with an existing ID cannot be created, so this API call must fail
        restGameServerMockMvc.perform(post("/api/game-servers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingGameServerDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<GameServer> gameServerList = gameServerRepository.findAll();
        assertThat(gameServerList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkAddressIsRequired() throws Exception {
        int databaseSizeBeforeTest = gameServerRepository.findAll().size();
        // set the field null
        gameServer.setAddress(null);

        // Create the GameServer, which fails.
        GameServerDTO gameServerDTO = gameServerMapper.gameServerToGameServerDTO(gameServer);

        restGameServerMockMvc.perform(post("/api/game-servers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(gameServerDTO)))
            .andExpect(status().isBadRequest());

        List<GameServer> gameServerList = gameServerRepository.findAll();
        assertThat(gameServerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = gameServerRepository.findAll().size();
        // set the field null
        gameServer.setName(null);

        // Create the GameServer, which fails.
        GameServerDTO gameServerDTO = gameServerMapper.gameServerToGameServerDTO(gameServer);

        restGameServerMockMvc.perform(post("/api/game-servers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(gameServerDTO)))
            .andExpect(status().isBadRequest());

        List<GameServer> gameServerList = gameServerRepository.findAll();
        assertThat(gameServerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkExpiresIsRequired() throws Exception {
        int databaseSizeBeforeTest = gameServerRepository.findAll().size();
        // set the field null
        gameServer.setExpires(null);

        // Create the GameServer, which fails.
        GameServerDTO gameServerDTO = gameServerMapper.gameServerToGameServerDTO(gameServer);

        restGameServerMockMvc.perform(post("/api/game-servers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(gameServerDTO)))
            .andExpect(status().isBadRequest());

        List<GameServer> gameServerList = gameServerRepository.findAll();
        assertThat(gameServerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllGameServers() throws Exception {
        // Initialize the database
        gameServerRepository.save(gameServer);

        // Get all the gameServerList
        restGameServerMockMvc.perform(get("/api/game-servers?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(gameServer.getId())))
            .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].ping").value(hasItem(DEFAULT_PING)))
            .andExpect(jsonPath("$.[*].players").value(hasItem(DEFAULT_PLAYERS)))
            .andExpect(jsonPath("$.[*].maxPlayers").value(hasItem(DEFAULT_MAX_PLAYERS)))
            .andExpect(jsonPath("$.[*].map").value(hasItem(DEFAULT_MAP)))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION)))
            .andExpect(jsonPath("$.[*].rconPassword").value(hasItem(DEFAULT_RCON_PASSWORD)))
            .andExpect(jsonPath("$.[*].svPassword").value(hasItem(DEFAULT_SV_PASSWORD)))
            .andExpect(jsonPath("$.[*].tvPort").value(hasItem(DEFAULT_TV_PORT)))
            .andExpect(jsonPath("$.[*].expires").value(hasItem(DEFAULT_EXPIRES)))
            .andExpect(jsonPath("$.[*].expirationDate").value(hasItem(sameInstant(DEFAULT_EXPIRATION_DATE))))
            .andExpect(jsonPath("$.[*].expirationCheckDate").value(hasItem(sameInstant(DEFAULT_EXPIRATION_CHECK_DATE))))
            .andExpect(jsonPath("$.[*].statusCheckDate").value(hasItem(sameInstant(DEFAULT_STATUS_CHECK_DATE))))
            .andExpect(jsonPath("$.[*].lastValidPing").value(hasItem(sameInstant(DEFAULT_LAST_VALID_PING))))
            .andExpect(jsonPath("$.[*].lastRconDate").value(hasItem(sameInstant(DEFAULT_LAST_RCON_DATE))))
            .andExpect(jsonPath("$.[*].lastGameUpdate").value(hasItem(sameInstant(DEFAULT_LAST_GAME_UPDATE))))
            .andExpect(jsonPath("$.[*].updating").value(hasItem(DEFAULT_UPDATING)))
            .andExpect(jsonPath("$.[*].updateAttempts").value(hasItem(DEFAULT_UPDATE_ATTEMPTS)))
            .andExpect(jsonPath("$.[*].lastUpdateStart").value(hasItem(sameInstant(DEFAULT_LAST_UPDATE_START))))
            .andExpect(jsonPath("$.[*].lastRconAnnounce").value(hasItem(sameInstant(DEFAULT_LAST_RCON_ANNOUNCE))));
    }

    @Test
    public void getGameServer() throws Exception {
        // Initialize the database
        gameServerRepository.save(gameServer);

        // Get the gameServer
        restGameServerMockMvc.perform(get("/api/game-servers/{id}", gameServer.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(gameServer.getId()))
            .andExpect(jsonPath("$.address").value(DEFAULT_ADDRESS))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.ping").value(DEFAULT_PING))
            .andExpect(jsonPath("$.players").value(DEFAULT_PLAYERS))
            .andExpect(jsonPath("$.maxPlayers").value(DEFAULT_MAX_PLAYERS))
            .andExpect(jsonPath("$.map").value(DEFAULT_MAP))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION))
            .andExpect(jsonPath("$.rconPassword").value(DEFAULT_RCON_PASSWORD))
            .andExpect(jsonPath("$.svPassword").value(DEFAULT_SV_PASSWORD))
            .andExpect(jsonPath("$.tvPort").value(DEFAULT_TV_PORT))
            .andExpect(jsonPath("$.expires").value(DEFAULT_EXPIRES))
            .andExpect(jsonPath("$.expirationDate").value(sameInstant(DEFAULT_EXPIRATION_DATE)))
            .andExpect(jsonPath("$.expirationCheckDate").value(sameInstant(DEFAULT_EXPIRATION_CHECK_DATE)))
            .andExpect(jsonPath("$.statusCheckDate").value(sameInstant(DEFAULT_STATUS_CHECK_DATE)))
            .andExpect(jsonPath("$.lastValidPing").value(sameInstant(DEFAULT_LAST_VALID_PING)))
            .andExpect(jsonPath("$.lastRconDate").value(sameInstant(DEFAULT_LAST_RCON_DATE)))
            .andExpect(jsonPath("$.lastGameUpdate").value(sameInstant(DEFAULT_LAST_GAME_UPDATE)))
            .andExpect(jsonPath("$.updating").value(DEFAULT_UPDATING))
            .andExpect(jsonPath("$.updateAttempts").value(DEFAULT_UPDATE_ATTEMPTS))
            .andExpect(jsonPath("$.lastUpdateStart").value(sameInstant(DEFAULT_LAST_UPDATE_START)))
            .andExpect(jsonPath("$.lastRconAnnounce").value(sameInstant(DEFAULT_LAST_RCON_ANNOUNCE)));
    }

    @Test
    public void getNonExistingGameServer() throws Exception {
        // Get the gameServer
        restGameServerMockMvc.perform(get("/api/game-servers/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateGameServer() throws Exception {
        // Initialize the database
        gameServerRepository.save(gameServer);
        int databaseSizeBeforeUpdate = gameServerRepository.findAll().size();

        // Update the gameServer
        GameServer updatedGameServer = gameServerRepository.findOne(gameServer.getId());
        updatedGameServer
                .address(UPDATED_ADDRESS)
                .name(UPDATED_NAME)
                .ping(UPDATED_PING)
                .players(UPDATED_PLAYERS)
                .maxPlayers(UPDATED_MAX_PLAYERS)
                .map(UPDATED_MAP)
                .version(UPDATED_VERSION)
                .rconPassword(UPDATED_RCON_PASSWORD)
                .svPassword(UPDATED_SV_PASSWORD)
                .tvPort(UPDATED_TV_PORT)
                .expires(UPDATED_EXPIRES)
                .expirationDate(UPDATED_EXPIRATION_DATE)
                .expirationCheckDate(UPDATED_EXPIRATION_CHECK_DATE)
                .statusCheckDate(UPDATED_STATUS_CHECK_DATE)
                .lastValidPing(UPDATED_LAST_VALID_PING)
                .lastRconDate(UPDATED_LAST_RCON_DATE)
                .lastGameUpdate(UPDATED_LAST_GAME_UPDATE)
                .updating(UPDATED_UPDATING)
                .updateAttempts(UPDATED_UPDATE_ATTEMPTS)
                .lastUpdateStart(UPDATED_LAST_UPDATE_START)
                .lastRconAnnounce(UPDATED_LAST_RCON_ANNOUNCE);
        GameServerDTO gameServerDTO = gameServerMapper.gameServerToGameServerDTO(updatedGameServer);

        restGameServerMockMvc.perform(put("/api/game-servers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(gameServerDTO)))
            .andExpect(status().isOk());

        // Validate the GameServer in the database
        List<GameServer> gameServerList = gameServerRepository.findAll();
        assertThat(gameServerList).hasSize(databaseSizeBeforeUpdate);
        GameServer testGameServer = gameServerList.get(gameServerList.size() - 1);
        assertThat(testGameServer.getAddress()).isEqualTo(UPDATED_ADDRESS);
        assertThat(testGameServer.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testGameServer.getPing()).isEqualTo(UPDATED_PING);
        assertThat(testGameServer.getPlayers()).isEqualTo(UPDATED_PLAYERS);
        assertThat(testGameServer.getMaxPlayers()).isEqualTo(UPDATED_MAX_PLAYERS);
        assertThat(testGameServer.getMap()).isEqualTo(UPDATED_MAP);
        assertThat(testGameServer.getVersion()).isEqualTo(UPDATED_VERSION);
        assertThat(testGameServer.getRconPassword()).isEqualTo(UPDATED_RCON_PASSWORD);
        assertThat(testGameServer.getSvPassword()).isEqualTo(UPDATED_SV_PASSWORD);
        assertThat(testGameServer.getTvPort()).isEqualTo(UPDATED_TV_PORT);
        assertThat(testGameServer.isExpires()).isEqualTo(UPDATED_EXPIRES);
        assertThat(testGameServer.getExpirationDate()).isEqualTo(UPDATED_EXPIRATION_DATE);
        assertThat(testGameServer.getExpirationCheckDate()).isEqualTo(UPDATED_EXPIRATION_CHECK_DATE);
        assertThat(testGameServer.getStatusCheckDate()).isEqualTo(UPDATED_STATUS_CHECK_DATE);
        assertThat(testGameServer.getLastValidPing()).isEqualTo(UPDATED_LAST_VALID_PING);
        assertThat(testGameServer.getLastRconDate()).isEqualTo(UPDATED_LAST_RCON_DATE);
        assertThat(testGameServer.getLastGameUpdate()).isEqualTo(UPDATED_LAST_GAME_UPDATE);
        assertThat(testGameServer.isUpdating()).isEqualTo(UPDATED_UPDATING);
        assertThat(testGameServer.getUpdateAttempts()).isEqualTo(UPDATED_UPDATE_ATTEMPTS);
        assertThat(testGameServer.getLastUpdateStart()).isEqualTo(UPDATED_LAST_UPDATE_START);
        assertThat(testGameServer.getLastRconAnnounce()).isEqualTo(UPDATED_LAST_RCON_ANNOUNCE);
    }

    @Test
    public void updateNonExistingGameServer() throws Exception {
        int databaseSizeBeforeUpdate = gameServerRepository.findAll().size();

        // Create the GameServer
        GameServerDTO gameServerDTO = gameServerMapper.gameServerToGameServerDTO(gameServer);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restGameServerMockMvc.perform(put("/api/game-servers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(gameServerDTO)))
            .andExpect(status().isCreated());

        // Validate the GameServer in the database
        List<GameServer> gameServerList = gameServerRepository.findAll();
        assertThat(gameServerList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deleteGameServer() throws Exception {
        // Initialize the database
        gameServerRepository.save(gameServer);
        int databaseSizeBeforeDelete = gameServerRepository.findAll().size();

        // Get the gameServer
        restGameServerMockMvc.perform(delete("/api/game-servers/{id}", gameServer.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<GameServer> gameServerList = gameServerRepository.findAll();
        assertThat(gameServerList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
