package top.quantic.sentry.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.quantic.sentry.domain.Streamer;
import top.quantic.sentry.service.dto.StreamerDTO;

import java.util.List;

/**
 * Mapper for the entity Streamer and its DTO StreamerDTO.
 */
@Mapper(componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StreamerMapper {

    StreamerDTO streamerToStreamerDTO(Streamer streamer);

    List<StreamerDTO> streamersToStreamerDTOs(List<Streamer> streamers);

    Streamer streamerDTOToStreamer(StreamerDTO streamerDTO);

    List<Streamer> streamerDTOsToStreamers(List<StreamerDTO> streamerDTOs);
}
