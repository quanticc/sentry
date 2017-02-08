package top.quantic.sentry.web.rest;

import top.quantic.sentry.SentryApp;

import top.quantic.sentry.domain.Privilege;
import top.quantic.sentry.repository.PrivilegeRepository;

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
 * Test class for the PrivilegeResource REST controller.
 *
 * @see PrivilegeResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class PrivilegeResourceIntTest {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_ROLE = "AAAAAAAAAA";
    private static final String UPDATED_ROLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    @Inject
    private PrivilegeRepository privilegeRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restPrivilegeMockMvc;

    private Privilege privilege;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PrivilegeResource privilegeResource = new PrivilegeResource();
        ReflectionTestUtils.setField(privilegeResource, "privilegeRepository", privilegeRepository);
        this.restPrivilegeMockMvc = MockMvcBuilders.standaloneSetup(privilegeResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Privilege createEntity() {
        Privilege privilege = new Privilege()
                .key(DEFAULT_KEY)
                .role(DEFAULT_ROLE)
                .description(DEFAULT_DESCRIPTION);
        return privilege;
    }

    @Before
    public void initTest() {
        privilegeRepository.deleteAll();
        privilege = createEntity();
    }

    @Test
    public void createPrivilege() throws Exception {
        int databaseSizeBeforeCreate = privilegeRepository.findAll().size();

        // Create the Privilege

        restPrivilegeMockMvc.perform(post("/api/privileges")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(privilege)))
            .andExpect(status().isCreated());

        // Validate the Privilege in the database
        List<Privilege> privilegeList = privilegeRepository.findAll();
        assertThat(privilegeList).hasSize(databaseSizeBeforeCreate + 1);
        Privilege testPrivilege = privilegeList.get(privilegeList.size() - 1);
        assertThat(testPrivilege.getKey()).isEqualTo(DEFAULT_KEY);
        assertThat(testPrivilege.getRole()).isEqualTo(DEFAULT_ROLE);
        assertThat(testPrivilege.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    public void createPrivilegeWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = privilegeRepository.findAll().size();

        // Create the Privilege with an existing ID
        Privilege existingPrivilege = new Privilege();
        existingPrivilege.setId("existing_id");

        // An entity with an existing ID cannot be created, so this API call must fail
        restPrivilegeMockMvc.perform(post("/api/privileges")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingPrivilege)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Privilege> privilegeList = privilegeRepository.findAll();
        assertThat(privilegeList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = privilegeRepository.findAll().size();
        // set the field null
        privilege.setKey(null);

        // Create the Privilege, which fails.

        restPrivilegeMockMvc.perform(post("/api/privileges")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(privilege)))
            .andExpect(status().isBadRequest());

        List<Privilege> privilegeList = privilegeRepository.findAll();
        assertThat(privilegeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkRoleIsRequired() throws Exception {
        int databaseSizeBeforeTest = privilegeRepository.findAll().size();
        // set the field null
        privilege.setRole(null);

        // Create the Privilege, which fails.

        restPrivilegeMockMvc.perform(post("/api/privileges")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(privilege)))
            .andExpect(status().isBadRequest());

        List<Privilege> privilegeList = privilegeRepository.findAll();
        assertThat(privilegeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllPrivileges() throws Exception {
        // Initialize the database
        privilegeRepository.save(privilege);

        // Get all the privilegeList
        restPrivilegeMockMvc.perform(get("/api/privileges?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(privilege.getId())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY)))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)));
    }

    @Test
    public void getPrivilege() throws Exception {
        // Initialize the database
        privilegeRepository.save(privilege);

        // Get the privilege
        restPrivilegeMockMvc.perform(get("/api/privileges/{id}", privilege.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(privilege.getId()))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY))
            .andExpect(jsonPath("$.role").value(DEFAULT_ROLE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION));
    }

    @Test
    public void getNonExistingPrivilege() throws Exception {
        // Get the privilege
        restPrivilegeMockMvc.perform(get("/api/privileges/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updatePrivilege() throws Exception {
        // Initialize the database
        privilegeRepository.save(privilege);
        int databaseSizeBeforeUpdate = privilegeRepository.findAll().size();

        // Update the privilege
        Privilege updatedPrivilege = privilegeRepository.findOne(privilege.getId());
        updatedPrivilege
                .key(UPDATED_KEY)
                .role(UPDATED_ROLE)
                .description(UPDATED_DESCRIPTION);

        restPrivilegeMockMvc.perform(put("/api/privileges")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedPrivilege)))
            .andExpect(status().isOk());

        // Validate the Privilege in the database
        List<Privilege> privilegeList = privilegeRepository.findAll();
        assertThat(privilegeList).hasSize(databaseSizeBeforeUpdate);
        Privilege testPrivilege = privilegeList.get(privilegeList.size() - 1);
        assertThat(testPrivilege.getKey()).isEqualTo(UPDATED_KEY);
        assertThat(testPrivilege.getRole()).isEqualTo(UPDATED_ROLE);
        assertThat(testPrivilege.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
    }

    @Test
    public void updateNonExistingPrivilege() throws Exception {
        int databaseSizeBeforeUpdate = privilegeRepository.findAll().size();

        // Create the Privilege

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restPrivilegeMockMvc.perform(put("/api/privileges")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(privilege)))
            .andExpect(status().isCreated());

        // Validate the Privilege in the database
        List<Privilege> privilegeList = privilegeRepository.findAll();
        assertThat(privilegeList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deletePrivilege() throws Exception {
        // Initialize the database
        privilegeRepository.save(privilege);
        int databaseSizeBeforeDelete = privilegeRepository.findAll().size();

        // Get the privilege
        restPrivilegeMockMvc.perform(delete("/api/privileges/{id}", privilege.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Privilege> privilegeList = privilegeRepository.findAll();
        assertThat(privilegeList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
