package top.quantic.sentry.web.rest;

import top.quantic.sentry.SentryApp;

import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.repository.SettingRepository;
import top.quantic.sentry.service.SettingService;
import top.quantic.sentry.service.dto.SettingDTO;
import top.quantic.sentry.service.mapper.SettingMapper;

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
 * Test class for the SettingResource REST controller.
 *
 * @see SettingResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class SettingResourceIntTest {

    private static final String DEFAULT_GUILD = "AAAAAAAAAA";
    private static final String UPDATED_GUILD = "BBBBBBBBBB";

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_VALUE = "AAAAAAAAAA";
    private static final String UPDATED_VALUE = "BBBBBBBBBB";

    private static final String DEFAULT_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_TYPE = "BBBBBBBBBB";

    @Inject
    private SettingRepository settingRepository;

    @Inject
    private SettingMapper settingMapper;

    @Inject
    private SettingService settingService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restSettingMockMvc;

    private Setting setting;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SettingResource settingResource = new SettingResource();
        ReflectionTestUtils.setField(settingResource, "settingService", settingService);
        this.restSettingMockMvc = MockMvcBuilders.standaloneSetup(settingResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Setting createEntity() {
        Setting setting = new Setting()
                .guild(DEFAULT_GUILD)
                .key(DEFAULT_KEY)
                .value(DEFAULT_VALUE)
                .type(DEFAULT_TYPE);
        return setting;
    }

    @Before
    public void initTest() {
        settingRepository.deleteAll();
        setting = createEntity();
    }

    @Test
    public void createSetting() throws Exception {
        int databaseSizeBeforeCreate = settingRepository.findAll().size();

        // Create the Setting
        SettingDTO settingDTO = settingMapper.settingToSettingDTO(setting);

        restSettingMockMvc.perform(post("/api/settings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(settingDTO)))
            .andExpect(status().isCreated());

        // Validate the Setting in the database
        List<Setting> settingList = settingRepository.findAll();
        assertThat(settingList).hasSize(databaseSizeBeforeCreate + 1);
        Setting testSetting = settingList.get(settingList.size() - 1);
        assertThat(testSetting.getGuild()).isEqualTo(DEFAULT_GUILD);
        assertThat(testSetting.getKey()).isEqualTo(DEFAULT_KEY);
        assertThat(testSetting.getValue()).isEqualTo(DEFAULT_VALUE);
        assertThat(testSetting.getType()).isEqualTo(DEFAULT_TYPE);
    }

    @Test
    public void createSettingWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = settingRepository.findAll().size();

        // Create the Setting with an existing ID
        Setting existingSetting = new Setting();
        existingSetting.setId("existing_id");
        SettingDTO existingSettingDTO = settingMapper.settingToSettingDTO(existingSetting);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSettingMockMvc.perform(post("/api/settings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingSettingDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Setting> settingList = settingRepository.findAll();
        assertThat(settingList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkGuildIsRequired() throws Exception {
        int databaseSizeBeforeTest = settingRepository.findAll().size();
        // set the field null
        setting.setGuild(null);

        // Create the Setting, which fails.
        SettingDTO settingDTO = settingMapper.settingToSettingDTO(setting);

        restSettingMockMvc.perform(post("/api/settings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(settingDTO)))
            .andExpect(status().isBadRequest());

        List<Setting> settingList = settingRepository.findAll();
        assertThat(settingList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = settingRepository.findAll().size();
        // set the field null
        setting.setKey(null);

        // Create the Setting, which fails.
        SettingDTO settingDTO = settingMapper.settingToSettingDTO(setting);

        restSettingMockMvc.perform(post("/api/settings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(settingDTO)))
            .andExpect(status().isBadRequest());

        List<Setting> settingList = settingRepository.findAll();
        assertThat(settingList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkValueIsRequired() throws Exception {
        int databaseSizeBeforeTest = settingRepository.findAll().size();
        // set the field null
        setting.setValue(null);

        // Create the Setting, which fails.
        SettingDTO settingDTO = settingMapper.settingToSettingDTO(setting);

        restSettingMockMvc.perform(post("/api/settings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(settingDTO)))
            .andExpect(status().isBadRequest());

        List<Setting> settingList = settingRepository.findAll();
        assertThat(settingList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllSettings() throws Exception {
        // Initialize the database
        settingRepository.save(setting);

        // Get all the settingList
        restSettingMockMvc.perform(get("/api/settings?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(setting.getId())))
            .andExpect(jsonPath("$.[*].guild").value(hasItem(DEFAULT_GUILD)))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY)))
            .andExpect(jsonPath("$.[*].value").value(hasItem(DEFAULT_VALUE)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE)));
    }

    @Test
    public void getSetting() throws Exception {
        // Initialize the database
        settingRepository.save(setting);

        // Get the setting
        restSettingMockMvc.perform(get("/api/settings/{id}", setting.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(setting.getId()))
            .andExpect(jsonPath("$.guild").value(DEFAULT_GUILD))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.value").value(DEFAULT_VALUE))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE));
    }

    @Test
    public void getNonExistingSetting() throws Exception {
        // Get the setting
        restSettingMockMvc.perform(get("/api/settings/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateSetting() throws Exception {
        // Initialize the database
        settingRepository.save(setting);
        int databaseSizeBeforeUpdate = settingRepository.findAll().size();

        // Update the setting
        Setting updatedSetting = settingRepository.findOne(setting.getId());
        updatedSetting
                .guild(UPDATED_GUILD)
                .key(UPDATED_KEY)
                .value(UPDATED_VALUE)
                .type(UPDATED_TYPE);
        SettingDTO settingDTO = settingMapper.settingToSettingDTO(updatedSetting);

        restSettingMockMvc.perform(put("/api/settings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(settingDTO)))
            .andExpect(status().isOk());

        // Validate the Setting in the database
        List<Setting> settingList = settingRepository.findAll();
        assertThat(settingList).hasSize(databaseSizeBeforeUpdate);
        Setting testSetting = settingList.get(settingList.size() - 1);
        assertThat(testSetting.getGuild()).isEqualTo(UPDATED_GUILD);
        assertThat(testSetting.getKey()).isEqualTo(UPDATED_KEY);
        assertThat(testSetting.getValue()).isEqualTo(UPDATED_VALUE);
        assertThat(testSetting.getType()).isEqualTo(UPDATED_TYPE);
    }

    @Test
    public void updateNonExistingSetting() throws Exception {
        int databaseSizeBeforeUpdate = settingRepository.findAll().size();

        // Create the Setting
        SettingDTO settingDTO = settingMapper.settingToSettingDTO(setting);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restSettingMockMvc.perform(put("/api/settings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(settingDTO)))
            .andExpect(status().isCreated());

        // Validate the Setting in the database
        List<Setting> settingList = settingRepository.findAll();
        assertThat(settingList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deleteSetting() throws Exception {
        // Initialize the database
        settingRepository.save(setting);
        int databaseSizeBeforeDelete = settingRepository.findAll().size();

        // Get the setting
        restSettingMockMvc.perform(delete("/api/settings/{id}", setting.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Setting> settingList = settingRepository.findAll();
        assertThat(settingList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
