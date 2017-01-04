package top.quantic.sentry.service.mapper;

import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.service.dto.BotDTO;

import java.util.List;

/**
 * Mapper for the entity Bot and its DTO BotDTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(BotMapperDecorator.class)
public interface BotMapper {

    BotDTO botToBotDTO(Bot bot);

    List<BotDTO> botsToBotDTOs(List<Bot> bots);

    Bot botDTOToBot(BotDTO botDTO);

    List<Bot> botDTOsToBots(List<BotDTO> botDTOs);
}
