package com.example.ApiGateway;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@OpenAPIDefinition(info = @Info(title = "API GATEWAY",description = "All api firstly coming in 8080 port"))
public class Config {
    @Bean
    public WebMvcConfigurer webMvcConfigurer(){
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000","http://localhost:5500","http://127.0.0.1:5500")
                        .allowedMethods("GET","POST","DELETE","PUT","PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        ;
            }
        };
    }
}
