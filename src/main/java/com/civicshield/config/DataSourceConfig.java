package com.civicshield.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${MYSQLHOST:}")
    private String mysqlHost;

    @Value("${MYSQLPORT:3306}")
    private String mysqlPort;

    @Value("${MYSQLDATABASE:railway}")
    private String mysqlDatabase;

    @Value("${MYSQLUSER:root}")
    private String mysqlUser;

    @Value("${MYSQLPASSWORD:}")
    private String mysqlPassword;

    @Value("${SPRING_DATASOURCE_URL:}")
    private String springDatasourceUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setConnectionTimeout(30000);
        ds.setMaximumPoolSize(5);

        if (springDatasourceUrl != null && !springDatasourceUrl.isBlank()) {
            log.info("Using configured SPRING_DATASOURCE_URL");
            ds.setJdbcUrl(springDatasourceUrl);
            ds.setUsername(mysqlUser);
            ds.setPassword(mysqlPassword);
        } else if (mysqlHost != null && !mysqlHost.isBlank() && !mysqlHost.equalsIgnoreCase("localhost")) {
            log.info("Connecting to Railway MySQL database at {}:{}", mysqlHost, mysqlPort);
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setJdbcUrl("jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase
                    + "?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
            ds.setUsername(mysqlUser);
            ds.setPassword(mysqlPassword);
        } else {
            log.info("No external MySQL database detected. Running with embedded database fallback.");
            ds.setDriverClassName("org.h2.Driver");
            ds.setJdbcUrl("jdbc:h2:mem:civicshield_db;MODE=MySQL;DB_CLOSE_DELAY=-1");
            ds.setUsername("sa");
            ds.setPassword("");
        }
        return ds;
    }
}
