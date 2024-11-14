package com.github.fractalo.streaming_settlement.config.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public DataSource transactionRoutingDataSource() {
        TransactionRoutingDataSource routingDataSource = new TransactionRoutingDataSource();

        Map<Object, Object> dataSourceMap = Map.of(
                DataSourceRole.READ_WRITE, primaryDataSource(),
                DataSourceRole.READ_ONLY, replicaDataSource()
        );

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource());

        return routingDataSource;
    }

}
