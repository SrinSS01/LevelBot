package me.srin.reallyadriel;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@SpringBootApplication
public class ReallyadrielApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReallyadrielApplication.class);
    public static void main(String[] args) {
        val config = new File("config");
        if (!config.exists()) {
            val result = config.mkdirs();
            if (result) {
                LOGGER.info("Created config directory");
                val properties = new File("config/application.yml");
                if (!properties.exists()) {
                    try(val writer = new FileWriter(properties)) {
                        writer.write("""
                                bot:
                                  token: null
                                  xp-cooldown-in-seconds: 0
                                  de-leveling-countdown-in-seconds: 0
                                  #rank-card-background-url: null
                                  rank-card-color: yellow
                                media-only-channels:
                                  - channel id 1
                                  - channel id 2
                                  - channel id 3
                                levelRoleMap:
                                  level1: roleId 1
                                  level2: roleId 2
                                  level3: roleId 3
                                                               
                                database:
                                  host: null
                                  name: null
                                  user: null
                                  password: null
                                """);
                        writer.flush();
                    } catch (IOException e) {
                        LOGGER.error("Failed to create config/application.yml");
                        return;
                    }
                    LOGGER.info("Created application.yml file");
                }
            } else {
                LOGGER.error("Failed to create config directory");
            }
            return;
        }
        SpringApplication.run(ReallyadrielApplication.class, args);
    }

}
