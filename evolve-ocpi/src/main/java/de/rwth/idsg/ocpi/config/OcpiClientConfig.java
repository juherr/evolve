package de.rwth.idsg.ocpi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OcpiClientConfig {

    @Bean
    public RestTemplate ocpiRestTemplate() {
        return new RestTemplate();
    }
}
