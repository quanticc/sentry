package top.quantic.sentry.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.quantic.sentry.domain.GameServer;
import top.quantic.sentry.service.dto.GameServerDTO;

import java.util.List;

/**
 * Mapper for the entity GameServer and its DTO GameServerDTO.
 */
@Mapper(componentModel = "spring",
    uses = {},
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GameServerMapper {

    GameServerDTO gameServerToGameServerDTO(GameServer gameServer);

    List<GameServerDTO> gameServersToGameServerDTOs(List<GameServer> gameServers);

    GameServer gameServerDTOToGameServer(GameServerDTO gameServerDTO);

    List<GameServer> gameServerDTOsToGameServers(List<GameServerDTO> gameServerDTOs);
}
