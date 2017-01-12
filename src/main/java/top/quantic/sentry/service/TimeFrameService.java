package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import top.quantic.sentry.domain.TimeFrame;
import top.quantic.sentry.repository.TimeFrameRepository;
import top.quantic.sentry.service.dto.TimeFrameDTO;
import top.quantic.sentry.service.mapper.TimeFrameMapper;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import static java.time.LocalDateTime.now;

/**
 * Service Implementation for managing TimeFrame.
 */
@Service
public class TimeFrameService {

    private final Logger log = LoggerFactory.getLogger(TimeFrameService.class);

    private final TimeFrameRepository timeFrameRepository;
    private final TimeFrameMapper timeFrameMapper;

    @Autowired
    public TimeFrameService(TimeFrameRepository timeFrameRepository, TimeFrameMapper timeFrameMapper) {
        this.timeFrameRepository = timeFrameRepository;
        this.timeFrameMapper = timeFrameMapper;
    }

    public boolean included(String subscriberId) {
        return timeFrameRepository.findBySubscriber(subscriberId).stream()
            .allMatch(frame -> !includes(frame, now()) || frame.isInclusive());
    }

    private boolean includes(TimeFrame frame, LocalDateTime dateTime) {
        LocalDateTime start = LocalDateTime.from(frame.getStart());
        LocalDateTime end = LocalDateTime.from(frame.getEnd());
        int distance = (int) (ChronoUnit.DAYS.between(start, dateTime) - 1);
        if (distance > 0) {
            int factor = distance / frame.getRecurrence().getDays();
            if (factor > 0) {
                Period advance = frame.getRecurrence().multipliedBy(factor);
                start.plus(advance);
                end.plus(advance);
            }
        }
        while (!start.isAfter(dateTime)) {
            if (end.isAfter(dateTime)) {
                return true;
            }
            start = start.plus(frame.getRecurrence());
            end = end.plus(frame.getRecurrence());
        }
        return false;
    }

    /**
     * Save a timeFrame.
     *
     * @param timeFrameDTO the entity to save
     * @return the persisted entity
     */
    public TimeFrameDTO save(TimeFrameDTO timeFrameDTO) {
        log.debug("Request to save TimeFrame : {}", timeFrameDTO);
        TimeFrame timeFrame = timeFrameMapper.timeFrameDTOToTimeFrame(timeFrameDTO);
        timeFrame = timeFrameRepository.save(timeFrame);
        TimeFrameDTO result = timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame);
        return result;
    }

    /**
     *  Get all the timeFrames.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    public Page<TimeFrameDTO> findAll(Pageable pageable) {
        log.debug("Request to get all TimeFrames");
        Page<TimeFrame> result = timeFrameRepository.findAll(pageable);
        return result.map(timeFrame -> timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame));
    }

    /**
     *  Get one timeFrame by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    public TimeFrameDTO findOne(String id) {
        log.debug("Request to get TimeFrame : {}", id);
        TimeFrame timeFrame = timeFrameRepository.findOne(id);
        TimeFrameDTO timeFrameDTO = timeFrameMapper.timeFrameToTimeFrameDTO(timeFrame);
        return timeFrameDTO;
    }

    /**
     *  Delete the  timeFrame by id.
     *
     *  @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete TimeFrame : {}", id);
        timeFrameRepository.delete(id);
    }
}
