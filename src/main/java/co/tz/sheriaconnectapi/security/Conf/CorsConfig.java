package co.tz.sheriaconnectapi.security.Conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://76.13.14.56:8091",
                        "http://76.13.14.56:4001",
                        "http://76.13.14.56:6001",
                        "http://localhost:4001",
                        "http://127.0.0.1:4001",
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://sheria-connect.co.tz",
                        "http://www.sheria-connect.co.tz",
                        "https://sheria-connect.co.tz",
                        "https://www.sheria-connect.co.tz",
                        "10.0.2.2"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
