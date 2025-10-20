package edu.eci.arsw;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/** Aplicación principal de Spring Boot para el programador de tutorías estudiantiles */
@SpringBootApplication
public class StudentTutorSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentTutorSchedulerApplication.class, args);
    }
    /** Configuración de CORS para permitir solicitudes desde orígenes específicos */
    @Bean
    public WebMvcConfigurer corsConfigurer(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins.split(","))
                        .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                        .allowCredentials(true);
            }
        };
    }
}
