package top.quantic.sentry.service.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.quantic.sentry.domain.TimeFrame;
import top.quantic.sentry.service.dto.TimeFrameDTO;

import java.time.Period;
import java.time.format.DateTimeParseException;

public abstract class TimeFrameMapperDecorator implements TimeFrameMapper {

    private static final Logger log = LoggerFactory.getLogger(TimeFrameMapperDecorator.class);

    @Autowired
    @Qualifier("delegate")
    private TimeFrameMapper delegate;

    @Override
    public TimeFrameDTO timeFrameToTimeFrameDTO(TimeFrame timeFrame) {
        TimeFrameDTO dto = delegate.timeFrameToTimeFrameDTO(timeFrame);
        dto.setRecurrenceValue(timeFrame.getRecurrence().toString());
        return dto;
    }

    @Override
    public TimeFrame timeFrameDTOToTimeFrame(TimeFrameDTO timeFrameDTO) {
        TimeFrame timeFrame = delegate.timeFrameDTOToTimeFrame(timeFrameDTO);
        try {
            timeFrame.setRecurrence(Period.parse(timeFrameDTO.getRecurrenceValue()));
        } catch (DateTimeParseException e) {
            log.warn("Could not parse recurrence - Using default of P1D", e);
            timeFrame.setRecurrence(Period.ofDays(1));
        }
        return timeFrame;
    }
}
