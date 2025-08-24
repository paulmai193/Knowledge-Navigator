package com.knowledgenavigator.config;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeaviateConfig {

    @Value("${weaviate.url:http://localhost:8080}")
    private String weaviateUrl;

    @Bean
    public WeaviateClient weaviateClient() {
        Config config = new Config("http", weaviateUrl.replace("http://", ""));
        return new WeaviateClient(config);
    }
}