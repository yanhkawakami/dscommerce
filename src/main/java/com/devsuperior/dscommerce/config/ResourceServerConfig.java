package com.devsuperior.dscommerce.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

    // Busca da application.properties
    // CORS é um recurso que os navegadores tem que não permitem que um backend seja acessado por um host que não esteja
    // autorizado. Nesse caso, são os hosts definidos no properties
	@Value("${cors.origins}")
	private String corsOrigins;

    // Libera o H2 no modo teste para que funcione sem problemas e possamos visualizar o Console do H2
	@Bean
	@Profile("test")
	@Order(1)
	public SecurityFilterChain h2SecurityFilterChain(HttpSecurity http) throws Exception {

        // frameOptions libera a interface gráfica do H2
		http.securityMatcher(PathRequest.toH2Console()).csrf(csrf -> csrf.disable())
				.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
		return http.build();
	}

    // Chama o CORS
	@Bean
	@Order(3)
	public SecurityFilterChain rsSecurityFilterChain(HttpSecurity http) throws Exception {

        // Não é necessário, pois nossa API é REST (stateless) e não corre risco de sofrer ataque CRSF
		http.csrf(csrf -> csrf.disable());
        // Tudo liberado, sem restrições. Aqui vamos restringir a nível de rota, mas é possível restringir tudo,
        // exigindo login e autenticação para tudo. A nível de rota, facilita o desacoplamento
		http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
		// Configura que o Resource Server recebe o Token JWT
        http.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()));
		http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
		return http.build();
	}

    // Customização do Token JWT para funcionar no Resource Server
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
		grantedAuthoritiesConverter.setAuthorityPrefix("");

		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
		return jwtAuthenticationConverter;
	}

    // Busca os hosts e configura para que eles possam ter acesso no ResourceServer
	@Bean
	CorsConfigurationSource corsConfigurationSource() {

		String[] origins = corsOrigins.split(",");

		CorsConfiguration corsConfig = new CorsConfiguration();
        // Define os hosts como origens permitidas
		corsConfig.setAllowedOriginPatterns(Arrays.asList(origins));
        // Define quais instruções podem ser passadas
		corsConfig.setAllowedMethods(Arrays.asList("POST", "GET", "PUT", "DELETE", "PATCH"));
		corsConfig.setAllowCredentials(true);
        // Autoriza os cabeçalhos
		corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfig);
		return source;
	}

    // Define como precedência máxima os cors definidos
	@Bean
	FilterRegistrationBean<CorsFilter> filterRegistrationBeanCorsFilter() {
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(
				new CorsFilter(corsConfigurationSource()));
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return bean;
	}
}
