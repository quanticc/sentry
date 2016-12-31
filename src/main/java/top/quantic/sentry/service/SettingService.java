package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.repository.SettingRepository;
import top.quantic.sentry.service.dto.SettingDTO;
import top.quantic.sentry.service.mapper.SettingMapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing Setting.
 */
@Service
public class SettingService {

    private final Logger log = LoggerFactory.getLogger(SettingService.class);

    private final SettingRepository settingRepository;
    private final SettingMapper settingMapper;

    @Autowired
    public SettingService(SettingRepository settingRepository, SettingMapper settingMapper) {
        this.settingRepository = settingRepository;
        this.settingMapper = settingMapper;
    }

    @Cacheable("prefixes")
    public List<String> getPrefixes(String guild) {
        List<Setting> settings = settingRepository.findByGuildAndKey(guild, Constants.KEY_PREFIX);
        if (settings.isEmpty()) {
            // try with "*"
            settings = settingRepository.findByGuildAndKey(Constants.ANY, Constants.KEY_PREFIX);
            if (settings.isEmpty()) {
                // fallback to '!'
                return Collections.singletonList(Constants.DEFAULT_PREFIX);
            }
        }
        return extractValues(settings);
    }

    /**
     * Save a setting.
     *
     * @param settingDTO the entity to save
     * @return the persisted entity
     */
    @CacheEvict(cacheNames = "prefixes", allEntries = true)
    public SettingDTO save(SettingDTO settingDTO) {
        log.debug("Request to save Setting : {}", settingDTO);
        Setting setting = settingMapper.settingDTOToSetting(settingDTO);
        setting = settingRepository.save(setting);
        SettingDTO result = settingMapper.settingToSettingDTO(setting);
        return result;
    }

    /**
     * Get all the settings.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<SettingDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Settings");
        Page<Setting> result = settingRepository.findAll(pageable);
        return result.map(setting -> settingMapper.settingToSettingDTO(setting));
    }

    /**
     * Get one setting by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public SettingDTO findOne(String id) {
        log.debug("Request to get Setting : {}", id);
        Setting setting = settingRepository.findOne(id);
        SettingDTO settingDTO = settingMapper.settingToSettingDTO(setting);
        return settingDTO;
    }

    /**
     * Delete the  setting by id.
     *
     * @param id the id of the entity
     */
    @CacheEvict(cacheNames = "prefixes", allEntries = true)
    public void delete(String id) {
        log.debug("Request to delete Setting : {}", id);
        settingRepository.delete(id);
    }

    private List<String> extractValues(List<Setting> settingList) {
        return settingList.stream().map(Setting::getValue).collect(Collectors.toList());
    }
}
