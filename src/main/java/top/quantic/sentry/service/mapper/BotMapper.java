package top.quantic.sentry.service.mapper;

import top.quantic.sentry.domain.*;
import top.quantic.sentry.service.dto.BotDTO;

import org.mapstruct.*;
import java.util.List;

/**
 * Mapper for the entity Bot and its DTO BotDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface BotMapper {

    BotDTO botToBotDTO(Bot bot);

    List<BotDTO> botsToBotDTOs(List<Bot> bots);

    Bot botDTOToBot(BotDTO botDTO);

    List<Bot> botDTOsToBots(List<BotDTO> botDTOs);
}
