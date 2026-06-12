package com.kabooz.backend.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-level bean configuration.
 */
@Configuration
public class AppConfig {

    /**
     * ModelMapper with strict matching strategy to prevent accidental field mapping.
     *
     * @return ModelMapper bean
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper;
    }
}
