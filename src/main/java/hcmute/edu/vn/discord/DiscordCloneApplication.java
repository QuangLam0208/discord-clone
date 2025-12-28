package hcmute.edu.vn.discord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "hcmute.edu.vn.discord.repository")
public class DiscordCloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscordCloneApplication.class, args);
    }

}
