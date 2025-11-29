package edu.eci.arsw;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StudentTutorSchedulerApplicationTest {

    @Test
    void mainMethodShouldRunWithoutErrors() {
        StudentTutorSchedulerApplication.main(new String[]{
                "--spring.main.web-application-type=none"
        });
        assertNotNull(StudentTutorSchedulerApplication.class);
    }

    @Test
    void corsConfigurerShouldCreateConfigurerAndConfigureCors() {
        StudentTutorSchedulerApplication app = new StudentTutorSchedulerApplication();

        WebMvcConfigurer configurer =
                app.corsConfigurer("http://localhost:3000,http://localhost:5173");

        assertNotNull(configurer);

        configurer.addCorsMappings(new CorsRegistry());
    }
}
