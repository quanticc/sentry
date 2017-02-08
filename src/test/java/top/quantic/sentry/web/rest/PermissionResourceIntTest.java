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
import top.quantic.sentry.domain.Permission;
import top.quantic.sentry.domain.enumeration.PermissionType;
import top.quantic.sentry.repository.PermissionRepository;
import top.quantic.sentry.service.PermissionService;

import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * Test class for the PermissionResource REST controller.
 *
 * @see PermissionResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class PermissionResourceIntTest {

    private static final PermissionType DEFAULT_TYPE = PermissionType.ALLOW;
    private static final PermissionType UPDATED_TYPE = PermissionType.DENY;

    private static final String DEFAULT_ROLE = "AAAAAAAAAA";
    private static final String UPDATED_ROLE = "BBBBBBBBBB";

    private static final String DEFAULT_OPERATION = "AAAAAAAAAA";
    private static final String UPDATED_OPERATION = "BBBBBBBBBB";

    private static final String DEFAULT_RESOURCE = "AAAAAAAAAA";
    private static final String UPDATED_RESOURCE = "BBBBBBBBBB";

    @Inject
    private PermissionRepository permissionRepository;

    @Inject
    private PermissionService permissionService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restPermissionMockMvc;

    private Permission permission;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PermissionResource permissionResource = new PermissionResource();
        ReflectionTestUtils.setField(permissionResource, "permissionService", permissionService);
        this.restPermissionMockMvc = MockMvcBuilders.standaloneSetup(permissionResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Permission createEntity() {
        Permission permission = new Permission()
                .type(DEFAULT_TYPE)
                .role(DEFAULT_ROLE)
                .operation(DEFAULT_OPERATION)
                .resource(DEFAULT_RESOURCE);
        return permission;
    }

    @Before
    public void initTest() {
        permissionRepository.deleteAll();
        permission = createEntity();
    }

    @Test
    public void createPermission() throws Exception {
        int databaseSizeBeforeCreate = permissionRepository.findAll().size();

        // Create the Permission

        restPermissionMockMvc.perform(post("/api/permissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(permission)))
            .andExpect(status().isCreated());

        // Validate the Permission in the database
        List<Permission> permissionList = permissionRepository.findAll();
        assertThat(permissionList).hasSize(databaseSizeBeforeCreate + 1);
        Permission testPermission = permissionList.get(permissionList.size() - 1);
        assertThat(testPermission.getType()).isEqualTo(DEFAULT_TYPE);
        assertThat(testPermission.getRole()).isEqualTo(DEFAULT_ROLE);
        assertThat(testPermission.getOperation()).isEqualTo(DEFAULT_OPERATION);
        assertThat(testPermission.getResource()).isEqualTo(DEFAULT_RESOURCE);
    }

    @Test
    public void createPermissionWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = permissionRepository.findAll().size();

        // Create the Permission with an existing ID
        Permission existingPermission = new Permission();
        existingPermission.setId("existing_id");

        // An entity with an existing ID cannot be created, so this API call must fail
        restPermissionMockMvc.perform(post("/api/permissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingPermission)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Permission> permissionList = permissionRepository.findAll();
        assertThat(permissionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkTypeIsRequired() throws Exception {
        int databaseSizeBeforeTest = permissionRepository.findAll().size();
        // set the field null
        permission.setType(null);

        // Create the Permission, which fails.

        restPermissionMockMvc.perform(post("/api/permissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(permission)))
            .andExpect(status().isBadRequest());

        List<Permission> permissionList = permissionRepository.findAll();
        assertThat(permissionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkRoleIsRequired() throws Exception {
        int databaseSizeBeforeTest = permissionRepository.findAll().size();
        // set the field null
        permission.setRole(null);

        // Create the Permission, which fails.

        restPermissionMockMvc.perform(post("/api/permissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(permission)))
            .andExpect(status().isBadRequest());

        List<Permission> permissionList = permissionRepository.findAll();
        assertThat(permissionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkOperationIsRequired() throws Exception {
        int databaseSizeBeforeTest = permissionRepository.findAll().size();
        // set the field null
        permission.setOperation(null);

        // Create the Permission, which fails.

        restPermissionMockMvc.perform(post("/api/permissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(permission)))
            .andExpect(status().isBadRequest());

        List<Permission> permissionList = permissionRepository.findAll();
        assertThat(permissionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllPermissions() throws Exception {
        // Initialize the database
        permissionRepository.save(permission);

        // Get all the permissionList
        restPermissionMockMvc.perform(get("/api/permissions?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(permission.getId())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE)))
            .andExpect(jsonPath("$.[*].operation").value(hasItem(DEFAULT_OPERATION)))
            .andExpect(jsonPath("$.[*].resource").value(hasItem(DEFAULT_RESOURCE)));
    }

    @Test
    public void getPermission() throws Exception {
        // Initialize the database
        permissionRepository.save(permission);

        // Get the permission
        restPermissionMockMvc.perform(get("/api/permissions/{id}", permission.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(permission.getId()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.role").value(DEFAULT_ROLE))
            .andExpect(jsonPath("$.operation").value(DEFAULT_OPERATION))
            .andExpect(jsonPath("$.resource").value(DEFAULT_RESOURCE));
    }

    @Test
    public void getNonExistingPermission() throws Exception {
        // Get the permission
        restPermissionMockMvc.perform(get("/api/permissions/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updatePermission() throws Exception {
        // Initialize the database
        permissionService.save(permission);

        int databaseSizeBeforeUpdate = permissionRepository.findAll().size();

        // Update the permission
        Permission updatedPermission = permissionRepository.findOne(permission.getId());
        updatedPermission
                .type(UPDATED_TYPE)
                .role(UPDATED_ROLE)
                .operation(UPDATED_OPERATION)
                .resource(UPDATED_RESOURCE);

        restPermissionMockMvc.perform(put("/api/permissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedPermission)))
            .andExpect(status().isOk());

        // Validate the Permission in the database
        List<Permission> permissionList = permissionRepository.findAll();
        assertThat(permissionList).hasSize(databaseSizeBeforeUpdate);
        Permission testPermission = permissionList.get(permissionList.size() - 1);
        assertThat(testPermission.getType()).isEqualTo(UPDATED_TYPE);
        assertThat(testPermission.getRole()).isEqualTo(UPDATED_ROLE);
        assertThat(testPermission.getOperation()).isEqualTo(UPDATED_OPERATION);
        assertThat(testPermission.getResource()).isEqualTo(UPDATED_RESOURCE);
    }

    @Test
    public void updateNonExistingPermission() throws Exception {
        int databaseSizeBeforeUpdate = permissionRepository.findAll().size();

        // Create the Permission

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restPermissionMockMvc.perform(put("/api/permissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(permission)))
            .andExpect(status().isCreated());

        // Validate the Permission in the database
        List<Permission> permissionList = permissionRepository.findAll();
        assertThat(permissionList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deletePermission() throws Exception {
        // Initialize the database
        permissionService.save(permission);

        int databaseSizeBeforeDelete = permissionRepository.findAll().size();

        // Get the permission
        restPermissionMockMvc.perform(delete("/api/permissions/{id}", permission.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Permission> permissionList = permissionRepository.findAll();
        assertThat(permissionList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
