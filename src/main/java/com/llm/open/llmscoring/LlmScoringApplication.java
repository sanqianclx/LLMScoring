package com.llm.open.llmscoring;

import com.llm.open.llmscoring.config.LlmProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableConfigurationProperties(LlmProperties.class)
public class LlmScoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmScoringApplication.class, args);
    }

}
