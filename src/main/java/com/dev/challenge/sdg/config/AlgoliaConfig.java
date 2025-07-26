package com.dev.challenge.sdg.config;

import com.algolia.search.DefaultSearchClient;
import com.algolia.search.SearchClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AlgoliaConfig {
    
    @Value("${algolia.application-id}")
    private String applicationId;
    
    @Value("${algolia.admin-api-key}")
    private String adminApiKey;
    
    @Bean
    public SearchClient algoliaSearchClient() {
        log.info("Initializing Algolia Search Client with Application ID: {}", applicationId);
        return DefaultSearchClient.create(applicationId, adminApiKey);
    }
}
