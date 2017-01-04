package top.quantic.sentry.service.mapper;

import top.quantic.sentry.domain.*;
import top.quantic.sentry.service.dto.SettingDTO;

import org.mapstruct.*;
import java.util.List;

/**
 * Mapper for the entity Setting and its DTO SettingDTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SettingMapper {

    SettingDTO settingToSettingDTO(Setting setting);

    List<SettingDTO> settingsToSettingDTOs(List<Setting> settings);

    Setting settingDTOToSetting(SettingDTO settingDTO);

    List<Setting> settingDTOsToSettings(List<SettingDTO> settingDTOs);
}
