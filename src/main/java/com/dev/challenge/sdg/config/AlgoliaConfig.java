package com.dev.challenge.sdg.config;

import com.algolia.api.SearchClient;
import com.algolia.api.AnalyticsClient;
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
        return new SearchClient(applicationId, adminApiKey);
    }
    
    @Bean
    public AnalyticsClient algoliaAnalyticsClient() {
        log.info("Initializing Algolia Analytics Client with Application ID: {}", applicationId);
        return new AnalyticsClient(applicationId, adminApiKey);
    }
}
