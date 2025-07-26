package com.dev.challenge.sdg.config;

import com.dev.challenge.sdg.service.AlgoliaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationInitializer implements CommandLineRunner {
    
    private final AlgoliaService algoliaService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing Smart Discount Generator application...");
        
        try {
            // Initialize Algolia indexes with sample data
            algoliaService.initializeIndexes().get();
            log.info("Application initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize application", e);
            // Don't throw exception to prevent application startup failure
        }
    }
}
