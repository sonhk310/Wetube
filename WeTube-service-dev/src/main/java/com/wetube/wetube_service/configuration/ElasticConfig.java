package com.wetube.wetube_service.configuration;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration

public class ElasticConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticUrl;

    @Value("${elasticsearch.api-key}")
    private String apiKey;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(HttpHost.create(elasticUrl))
            .setDefaultHeaders(new BasicHeader[]{
                new BasicHeader("Authorization", "ApiKey " + apiKey)
            })
            .build();

        RestClientTransport transport =
            new RestClientTransport(restClient, new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }
}
