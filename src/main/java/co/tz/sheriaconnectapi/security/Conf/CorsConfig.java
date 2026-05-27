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
                        "http://localhost:4001",
                        "http://127.0.0.1:4001",
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://sheriaconnect.co.tz",
                        "http://www.sheriaconnect.co.tz",
                        "http://api.sheriaconnect.co.tz",
                        "https://sheriaconnect.co.tz",
                        "https://www.sheriaconnect.co.tz",
                        "https://api.sheriaconnect.co.tz",
                        "10.0.2.2"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
