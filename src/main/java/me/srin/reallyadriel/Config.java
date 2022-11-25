package me.srin.reallyadriel;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties
@ConfigurationProperties("bot")
@Getter @Setter
public class Config {
    private String token;
    private int xpCoolDownInSeconds;
    private int deLevelingCountdownInSeconds;
    private String rankCardBackgroundUrl;
    private String rankCardColor;
    private List<String> mediaOnlyChannels;
    private Map<String, String> levelRoleMap;
}
