package top.quantic.sentry.service;

import org.ocpsoft.prettytime.shade.edu.emory.mathcs.backport.java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sx.blah.discord.handle.obj.IMessage;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.domain.AbstractAuditingEntity;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.repository.SettingRepository;
import top.quantic.sentry.service.dto.SettingDTO;
import top.quantic.sentry.service.mapper.SettingMapper;
import top.quantic.sentry.service.util.Key;

import java.util.*;
import java.util.stream.Collectors;

import static top.quantic.sentry.config.Constants.ANY;
import static top.quantic.sentry.config.Constants.KEY_PREFIX;

/**
 * Service Implementation for managing Setting.
 */
@Service
public class SettingService {

    private final Logger log = LoggerFactory.getLogger(SettingService.class);

    private final SettingRepository settingRepository;
    private final SettingMapper settingMapper;
    private final SentryProperties sentryProperties;

    private long lastUpdate = 0L;

    @Autowired
    public SettingService(SettingRepository settingRepository, SettingMapper settingMapper,
                          SentryProperties sentryProperties) {
        this.settingRepository = settingRepository;
        this.settingMapper = settingMapper;
        this.sentryProperties = sentryProperties;
    }

    public boolean isInvalidated(long lastCheck) {
        return lastUpdate != lastCheck;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public Setting getSettingFromKey(Key<?> key) {
        return findOneByGuildAndKey(key.getGroup(), key.getName())
            .orElse(null);
    }

    public <T> T getValueFromKey(Key<T> key) {
        return findOneByGuildAndKey(key.getGroup(), key.getName())
            .map(key::fromSetting)
            .orElse(key.getDefaultValue());
    }

    public Set<String> getPrefixes(IMessage message) {
        if (message.getChannel().isPrivate()) {
            return getPrefixes("*");
        } else {
            return getPrefixes(message.getGuild().getID());
        }
    }

    public Set<String> getPrefixes(String guild) {
        List<Setting> settings = (guild == null ? Collections.emptyList() :
            settingRepository.findByGuildAndKey(guild, KEY_PREFIX));
        if (settings.isEmpty()) {
            settings = settingRepository.findByGuildAndKey(ANY, KEY_PREFIX);
            if (settings.isEmpty()) {
                return new HashSet<>(sentryProperties.getDiscord().getDefaultPrefixes());
            }
        }
        return extractValues(settings);
    }

    public void setPrefixes(IMessage message, Set<String> prefixes, boolean append) {
        if (message.getChannel().isPrivate()) {
            setPrefixes("*", prefixes, append);
        } else {
            setPrefixes(message.getGuild().getID(), prefixes, append);
        }
    }

    public void setPrefixes(String guild, Set<String> prefixes, boolean append) {
        List<Setting> current = settingRepository.findByGuildAndKey(guild, KEY_PREFIX);
        if (append) {
            log.info("<{}> Appending prefixes {} to: {}", guild, prefixes, current.stream()
                .map(Setting::getValue).collect(Collectors.joining(" ")));
        } else {
            log.info("<{}> Setting new prefixes: {}", guild, prefixes, current.stream()
                .map(Setting::getValue).collect(Collectors.joining(" ")));
            current.stream()
                .filter(setting -> !prefixes.contains(setting.getValue()))
                .forEach(settingRepository::delete);
        }
        prefixes.stream()
            .filter(prefix -> current.stream().noneMatch(setting -> setting.getValue().equals(prefix)))
            .map(prefix -> prefixToSetting(guild, prefix))
            .forEach(settingRepository::save);
    }

    private Setting prefixToSetting(String guild, String prefix) {
        Setting setting = new Setting();
        setting.setGuild(guild);
        setting.setKey(KEY_PREFIX);
        setting.setValue(prefix);
        return setting;
    }

    public List<Setting> findByKey(String key) {
        return settingRepository.findByKey(key);
    }

    public List<Setting> findByGuild(String guild) {
        return settingRepository.findByGuild(guild);
    }

    public List<Setting> findByGuildAndKey(String guild, String key) {
        return settingRepository.findByGuildAndKey(guild, key);
    }

    public List<Setting> findByGuildAndKeyStartingWith(String guild, String key) {
        return settingRepository.findByGuildAndKeyStartingWith(guild, key);
    }

    public List<Setting> findByGlobalKey(String key) {
        return settingRepository.findByGuildAndKey(ANY, key);
    }

    public Optional<Setting> findOneByGuildAndKey(String guild, String key) {
        return settingRepository.findByGuildAndKey(guild, key).stream().findAny();
    }

    public SettingDTO mappedFindOneByGuildAndKey(String guild, String key) {
        return settingMapper.settingToSettingDTO(
            settingRepository.findByGuildAndKey(guild, key).stream()
                .findAny()
                .orElse(null));
    }

    public Optional<Setting> findMostRecentByGuildAndKey(String guild, String key) {
        return settingRepository.findByGuildAndKey(guild, key).stream()
            .sorted(Comparator.comparing(AbstractAuditingEntity::getLastModifiedDate).reversed())
            .findFirst();
    }

    public Optional<Setting> findOneByGlobalKey(String key) {
        return settingRepository.findByGuildAndKey(ANY, key).stream().findAny();
    }

    public List<Setting> findByKeyStartingWith(String key) {
        return settingRepository.findByKeyStartingWith(key);
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
        lastUpdate = System.currentTimeMillis();
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

    public void updateSetting(Setting setting, String group, String key, String value) {
        log.debug("Request to update Setting : {} with ({}, {}, {})", setting, group, key, value);
        setting.setGuild(group);
        setting.setKey(key);
        setting.setValue(value);
        settingRepository.save(setting);
        lastUpdate = System.currentTimeMillis();
    }

    public void updateValue(Setting setting, String value) {
        log.debug("Request to update Setting : {} with value: {}", setting, value);
        setting.setValue(value);
        settingRepository.save(setting);
        lastUpdate = System.currentTimeMillis();
    }

    public void createSetting(String group, String key, String value) {
        log.debug("Request to create Setting with ({}, {}, {})", group, key, value);
        updateSetting(new Setting(), group, key, value);
    }
}
