package top.quantic.sentry.service;

import com.ibasco.agql.protocols.valve.steam.webapi.SteamWebApiClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import top.quantic.sentry.SentryApp;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class GameQueryIntTest {

    @Inject
    private SteamWebApiClient steamWebApiClient;

    @Inject
    private SettingService settingService;

    @Inject
    private RestTemplate restTemplate;

    private GameQueryService gameQueryService;

    @Before
    public void setup() {
        gameQueryService = new GameQueryService(steamWebApiClient, settingService, restTemplate);
    }

    @Test
    public void testSteamUrlResolution() {
        assertThat(gameQueryService.getSteamId64("http://steamcommunity.com/profiles/76561198012092861/").join()).isEqualTo(76561198012092861L);
        assertThat(gameQueryService.getSteamId64("http://steamcommunity.com/id/thepropane/").join()).isEqualTo(76561198012092861L);
        assertThat(gameQueryService.getSteamId64("76561198012092861").join()).isEqualTo(76561198012092861L);
        assertThat(gameQueryService.getSteamId64("thepropane").join()).isEqualTo(76561198012092861L);
        assertThat(gameQueryService.getSteamId64("[U:1:51827133]").join()).isEqualTo(76561198012092861L);
        assertThat(gameQueryService.getSteamId64("STEAM_0:1:25913566").join()).isEqualTo(76561198012092861L);

        assertThat(gameQueryService.getSteamId64("http://steamcommunity.com/groups/restaurante/").join()).isNull();
        assertThatExceptionOfType(NumberFormatException.class)
            .isThrownBy(() -> gameQueryService.getSteamId64(Long.MAX_VALUE + "1").join());
        assertThat(gameQueryService.getSteamId64("nope://example.org/profiles/vanity/").join()).isNull();
    }
}
