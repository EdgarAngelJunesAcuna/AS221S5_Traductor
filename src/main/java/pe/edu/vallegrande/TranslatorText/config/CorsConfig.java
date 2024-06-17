package pe.edu.vallegrande.TranslatorText.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("https://glorious-broccoli-45r7vr7x46w2pp5-4200.app.github.dev"); // El frontend de Angular corre en este puerto
        corsConfig.addAllowedMethod("*"); // Permitir todos los métodos (GET, POST, etc.)
        corsConfig.addAllowedHeader("*"); // Permitir todas las cabeceras
        corsConfig.setAllowCredentials(true); // Permitir credenciales

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
