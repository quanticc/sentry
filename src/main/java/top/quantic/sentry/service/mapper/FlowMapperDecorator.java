package top.quantic.sentry.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.quantic.sentry.domain.Flow;
import top.quantic.sentry.service.dto.FlowDTO;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class FlowMapperDecorator implements FlowMapper {

    private static final Logger log = LoggerFactory.getLogger(FlowMapperDecorator.class);

    @Autowired
    @Qualifier("delegate")
    private FlowMapper delegate;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FlowDTO flowToFlowDTO(Flow flow) {
        FlowDTO dto = delegate.flowToFlowDTO(flow);
        Map<String, Object> variables = flow.getVariables();
        if (variables != null && !variables.isEmpty()) {
            try {
                dto.setInputParameters(objectMapper.writeValueAsString(variables));
            } catch (JsonProcessingException e) {
                log.warn("Could not map variables field", e);
            }
        }
        return dto;
    }

    @Override
    public Flow flowDTOToFlow(FlowDTO flowDTO) {
        Flow flow = delegate.flowDTOToFlow(flowDTO);
        String input = flowDTO.getInputParameters();
        if (isBlank(input)) {
            flow.setVariables(new HashMap<>());
        } else {
            try {
                flow.setVariables(objectMapper.readValue(input, new TypeReference<Map<String, Object>>() {
                }));
            } catch (IOException e) {
                log.warn("Could not map flow parameters", e);
                flow.setVariables(new HashMap<>());
            }
        }
        return flow;
    }
}
