package com.king.learn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@RestController
public class LearnApplication {
    private static final Logger log = LoggerFactory.getLogger(LearnApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(LearnApplication.class, args);
	}

//    @GetMapping("/hello")
//    public String hello(@RequestParam(value="name", defaultValue ="world") String name) {
//        return String.format("Hello %s", name);
//    }

    @GetMapping("/api/random")
    public Quote quote() {
        return new Quote("test", new Value(100l, "hello"));
    }

    @Bean
    @Profile("!test")
    public ApplicationRunner run(RestClient.Builder builder, Environment environment) {
        return args -> {
            try {
                String port = environment.getProperty("local.server.port",
                        environment.getProperty("server.port", "8080"));
                RestClient restClient = builder.baseUrl("http://localhost:" + port).build();
                Quote quote = restClient
                        .get().uri("/api/random")
                        .retrieve()
                        .body(Quote.class);
                log.info("Quote: {}", quote);
            } catch (Exception e) {
                log.warn("自动请求 /api/random 失败: {}", e.getMessage());
            }
        };
    }
}
