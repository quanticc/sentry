package top.quantic.sentry.web.rest;

import top.quantic.sentry.SentryApp;

import top.quantic.sentry.domain.Flow;
import top.quantic.sentry.repository.FlowRepository;
import top.quantic.sentry.service.FlowService;
import top.quantic.sentry.service.dto.FlowDTO;
import top.quantic.sentry.service.mapper.FlowMapper;

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
 * Test class for the FlowResource REST controller.
 *
 * @see FlowResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class FlowResourceIntTest {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_INPUT = "AAAAAAAAAA";
    private static final String UPDATED_INPUT = "BBBBBBBBBB";

    private static final String DEFAULT_INPUT_PARAMETERS = "AAAAAAAAAA";
    private static final String UPDATED_INPUT_PARAMETERS = "BBBBBBBBBB";

    private static final String DEFAULT_MESSAGE = "AAAAAAAAAA";
    private static final String UPDATED_MESSAGE = "BBBBBBBBBB";

    private static final String DEFAULT_TRANSLATOR = "AAAAAAAAAA";
    private static final String UPDATED_TRANSLATOR = "BBBBBBBBBB";

    private static final String DEFAULT_OUTPUT = "AAAAAAAAAA";
    private static final String UPDATED_OUTPUT = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ENABLED = false;
    private static final Boolean UPDATED_ENABLED = true;

    @Inject
    private FlowRepository flowRepository;

    @Inject
    private FlowMapper flowMapper;

    @Inject
    private FlowService flowService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restFlowMockMvc;

    private Flow flow;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        FlowResource flowResource = new FlowResource();
        ReflectionTestUtils.setField(flowResource, "flowService", flowService);
        this.restFlowMockMvc = MockMvcBuilders.standaloneSetup(flowResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Flow createEntity() {
        Flow flow = new Flow()
                .name(DEFAULT_NAME)
                .input(DEFAULT_INPUT)
//                .inputParameters(DEFAULT_INPUT_PARAMETERS)
                .message(DEFAULT_MESSAGE)
                .translator(DEFAULT_TRANSLATOR)
                .output(DEFAULT_OUTPUT)
                .enabled(DEFAULT_ENABLED);
        return flow;
    }

    @Before
    public void initTest() {
        flowRepository.deleteAll();
        flow = createEntity();
    }

    @Test
    public void createFlow() throws Exception {
        int databaseSizeBeforeCreate = flowRepository.findAll().size();

        // Create the Flow
        FlowDTO flowDTO = flowMapper.flowToFlowDTO(flow);

        restFlowMockMvc.perform(post("/api/flows")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(flowDTO)))
            .andExpect(status().isCreated());

        // Validate the Flow in the database
        List<Flow> flowList = flowRepository.findAll();
        assertThat(flowList).hasSize(databaseSizeBeforeCreate + 1);
        Flow testFlow = flowList.get(flowList.size() - 1);
        assertThat(testFlow.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testFlow.getInput()).isEqualTo(DEFAULT_INPUT);
//        assertThat(testFlow.getInputParameters()).isEqualTo(DEFAULT_INPUT_PARAMETERS);
        assertThat(testFlow.getMessage()).isEqualTo(DEFAULT_MESSAGE);
        assertThat(testFlow.getTranslator()).isEqualTo(DEFAULT_TRANSLATOR);
        assertThat(testFlow.getOutput()).isEqualTo(DEFAULT_OUTPUT);
        assertThat(testFlow.isEnabled()).isEqualTo(DEFAULT_ENABLED);
    }

    @Test
    public void createFlowWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = flowRepository.findAll().size();

        // Create the Flow with an existing ID
        Flow existingFlow = new Flow();
        existingFlow.setId("existing_id");
        FlowDTO existingFlowDTO = flowMapper.flowToFlowDTO(existingFlow);

        // An entity with an existing ID cannot be created, so this API call must fail
        restFlowMockMvc.perform(post("/api/flows")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingFlowDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Flow> flowList = flowRepository.findAll();
        assertThat(flowList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkInputIsRequired() throws Exception {
        int databaseSizeBeforeTest = flowRepository.findAll().size();
        // set the field null
        flow.setInput(null);

        // Create the Flow, which fails.
        FlowDTO flowDTO = flowMapper.flowToFlowDTO(flow);

        restFlowMockMvc.perform(post("/api/flows")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(flowDTO)))
            .andExpect(status().isBadRequest());

        List<Flow> flowList = flowRepository.findAll();
        assertThat(flowList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkMessageIsRequired() throws Exception {
        int databaseSizeBeforeTest = flowRepository.findAll().size();
        // set the field null
        flow.setMessage(null);

        // Create the Flow, which fails.
        FlowDTO flowDTO = flowMapper.flowToFlowDTO(flow);

        restFlowMockMvc.perform(post("/api/flows")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(flowDTO)))
            .andExpect(status().isBadRequest());

        List<Flow> flowList = flowRepository.findAll();
        assertThat(flowList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkTranslatorIsRequired() throws Exception {
        int databaseSizeBeforeTest = flowRepository.findAll().size();
        // set the field null
        flow.setTranslator(null);

        // Create the Flow, which fails.
        FlowDTO flowDTO = flowMapper.flowToFlowDTO(flow);

        restFlowMockMvc.perform(post("/api/flows")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(flowDTO)))
            .andExpect(status().isBadRequest());

        List<Flow> flowList = flowRepository.findAll();
        assertThat(flowList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkOutputIsRequired() throws Exception {
        int databaseSizeBeforeTest = flowRepository.findAll().size();
        // set the field null
        flow.setOutput(null);

        // Create the Flow, which fails.
        FlowDTO flowDTO = flowMapper.flowToFlowDTO(flow);

        restFlowMockMvc.perform(post("/api/flows")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(flowDTO)))
            .andExpect(status().isBadRequest());

        List<Flow> flowList = flowRepository.findAll();
        assertThat(flowList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllFlows() throws Exception {
        // Initialize the database
        flowRepository.save(flow);

        // Get all the flowList
        restFlowMockMvc.perform(get("/api/flows?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(flow.getId())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].input").value(hasItem(DEFAULT_INPUT.toString())))
            .andExpect(jsonPath("$.[*].inputParameters").value(hasItem(DEFAULT_INPUT_PARAMETERS.toString())))
            .andExpect(jsonPath("$.[*].message").value(hasItem(DEFAULT_MESSAGE.toString())))
            .andExpect(jsonPath("$.[*].translator").value(hasItem(DEFAULT_TRANSLATOR.toString())))
            .andExpect(jsonPath("$.[*].output").value(hasItem(DEFAULT_OUTPUT.toString())))
            .andExpect(jsonPath("$.[*].enabled").value(hasItem(DEFAULT_ENABLED.booleanValue())));
    }

    @Test
    public void getFlow() throws Exception {
        // Initialize the database
        flowRepository.save(flow);

        // Get the flow
        restFlowMockMvc.perform(get("/api/flows/{id}", flow.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(flow.getId()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.input").value(DEFAULT_INPUT.toString()))
            .andExpect(jsonPath("$.inputParameters").value(DEFAULT_INPUT_PARAMETERS.toString()))
            .andExpect(jsonPath("$.message").value(DEFAULT_MESSAGE.toString()))
            .andExpect(jsonPath("$.translator").value(DEFAULT_TRANSLATOR.toString()))
            .andExpect(jsonPath("$.output").value(DEFAULT_OUTPUT.toString()))
            .andExpect(jsonPath("$.enabled").value(DEFAULT_ENABLED.booleanValue()));
    }

    @Test
    public void getNonExistingFlow() throws Exception {
        // Get the flow
        restFlowMockMvc.perform(get("/api/flows/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateFlow() throws Exception {
        // Initialize the database
        flowRepository.save(flow);
        int databaseSizeBeforeUpdate = flowRepository.findAll().size();

        // Update the flow
        Flow updatedFlow = flowRepository.findOne(flow.getId());
        updatedFlow
                .name(UPDATED_NAME)
                .input(UPDATED_INPUT)
//                .inputParameters(UPDATED_INPUT_PARAMETERS)
                .message(UPDATED_MESSAGE)
                .translator(UPDATED_TRANSLATOR)
                .output(UPDATED_OUTPUT)
                .enabled(UPDATED_ENABLED);
        FlowDTO flowDTO = flowMapper.flowToFlowDTO(updatedFlow);

        restFlowMockMvc.perform(put("/api/flows")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(flowDTO)))
            .andExpect(status().isOk());

        // Validate the Flow in the database
        List<Flow> flowList = flowRepository.findAll();
        assertThat(flowList).hasSize(databaseSizeBeforeUpdate);
        Flow testFlow = flowList.get(flowList.size() - 1);
        assertThat(testFlow.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testFlow.getInput()).isEqualTo(UPDATED_INPUT);
//        assertThat(testFlow.getInputParameters()).isEqualTo(UPDATED_INPUT_PARAMETERS);
        assertThat(testFlow.getMessage()).isEqualTo(UPDATED_MESSAGE);
        assertThat(testFlow.getTranslator()).isEqualTo(UPDATED_TRANSLATOR);
        assertThat(testFlow.getOutput()).isEqualTo(UPDATED_OUTPUT);
        assertThat(testFlow.isEnabled()).isEqualTo(UPDATED_ENABLED);
    }

    @Test
    public void updateNonExistingFlow() throws Exception {
        int databaseSizeBeforeUpdate = flowRepository.findAll().size();

        // Create the Flow
        FlowDTO flowDTO = flowMapper.flowToFlowDTO(flow);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restFlowMockMvc.perform(put("/api/flows")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(flowDTO)))
            .andExpect(status().isCreated());

        // Validate the Flow in the database
        List<Flow> flowList = flowRepository.findAll();
        assertThat(flowList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deleteFlow() throws Exception {
        // Initialize the database
        flowRepository.save(flow);
        int databaseSizeBeforeDelete = flowRepository.findAll().size();

        // Get the flow
        restFlowMockMvc.perform(delete("/api/flows/{id}", flow.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Flow> flowList = flowRepository.findAll();
        assertThat(flowList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
