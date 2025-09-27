package nz.compliscan.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {
  @Bean
  public CorsFilter corsFilter(@Value("${app.cors.allowedOrigins:*}") String allowed) {
    CorsConfiguration cfg = new CorsConfiguration();
    if ("*".equals(allowed)) cfg.addAllowedOriginPattern("*");
    else for (String o : allowed.split(",")) cfg.addAllowedOrigin(o.trim());
    cfg.setAllowCredentials(true);
    cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Content-Type","Authorization","X-Requested-With","Accept","Origin","x-api-key"));
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return new CorsFilter(src);
  }
}