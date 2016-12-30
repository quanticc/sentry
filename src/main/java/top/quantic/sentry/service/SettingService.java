package top.quantic.sentry.service;

import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.repository.SettingRepository;
import top.quantic.sentry.service.dto.SettingDTO;
import top.quantic.sentry.service.mapper.SettingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing Setting.
 */
@Service
public class SettingService {

    private final Logger log = LoggerFactory.getLogger(SettingService.class);
    
    @Inject
    private SettingRepository settingRepository;

    @Inject
    private SettingMapper settingMapper;

    /**
     * Save a setting.
     *
     * @param settingDTO the entity to save
     * @return the persisted entity
     */
    public SettingDTO save(SettingDTO settingDTO) {
        log.debug("Request to save Setting : {}", settingDTO);
        Setting setting = settingMapper.settingDTOToSetting(settingDTO);
        setting = settingRepository.save(setting);
        SettingDTO result = settingMapper.settingToSettingDTO(setting);
        return result;
    }

    /**
     *  Get all the settings.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    public Page<SettingDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Settings");
        Page<Setting> result = settingRepository.findAll(pageable);
        return result.map(setting -> settingMapper.settingToSettingDTO(setting));
    }

    /**
     *  Get one setting by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    public SettingDTO findOne(String id) {
        log.debug("Request to get Setting : {}", id);
        Setting setting = settingRepository.findOne(id);
        SettingDTO settingDTO = settingMapper.settingToSettingDTO(setting);
        return settingDTO;
    }

    /**
     *  Delete the  setting by id.
     *
     *  @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete Setting : {}", id);
        settingRepository.delete(id);
    }
}
