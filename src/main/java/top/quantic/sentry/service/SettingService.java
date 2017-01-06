package top.quantic.sentry.service;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sx.blah.discord.handle.obj.IMessage;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.repository.SettingRepository;
import top.quantic.sentry.service.dto.SettingDTO;
import top.quantic.sentry.service.mapper.SettingMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing Setting.
 */
@Service
public class SettingService {

    private final Logger log = LoggerFactory.getLogger(SettingService.class);

    private final SettingRepository settingRepository;
    private final SettingMapper settingMapper;
    private final SentryProperties sentryProperties;

    @Autowired
    public SettingService(SettingRepository settingRepository, SettingMapper settingMapper,
                          SentryProperties sentryProperties) {
        this.settingRepository = settingRepository;
        this.settingMapper = settingMapper;
        this.sentryProperties = sentryProperties;
    }

    public Set<String> getPrefixes(IMessage message) {
        if (message.getChannel().isPrivate()) {
            return getPrefixes("*");
        } else {
            return getPrefixes(message.getGuild().getID());
        }
    }

    public Set<String> getPrefixes(String guild) {
        List<Setting> settings = settingRepository.findByGuildAndKey(guild, Constants.KEY_PREFIX);
        if (settings.isEmpty()) {
            settings = settingRepository.findByGuildAndKey(Constants.ANY, Constants.KEY_PREFIX);
            if (settings.isEmpty()) {
                return Sets.newHashSet(sentryProperties.getDiscord().getDefaultPrefixes());
            }
        }
        return extractValues(settings);
    }

    public List<Setting> findByKey(String key) {
        return settingRepository.findByKey(key);
    }

    public List<Setting> findByGuildAndKey(String guild, String key) {
        return settingRepository.findByGuildAndKey(guild, key);
    }

    public List<Setting> findByGlobalKey(String key) {
        return settingRepository.findByGuildAndKey(Constants.ANY, key);
    }

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
    public void delete(String id) {
        log.debug("Request to delete Setting : {}", id);
        settingRepository.delete(id);
    }

    private Set<String> extractValues(List<Setting> settingList) {
        return settingList.stream().map(Setting::getValue).collect(Collectors.toSet());
    }
}
