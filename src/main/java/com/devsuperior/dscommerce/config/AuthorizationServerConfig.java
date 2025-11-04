package com.devsuperior.dscommerce.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;

import com.devsuperior.dscommerce.config.customgrant.CustomPasswordAuthenticationConverter;
import com.devsuperior.dscommerce.config.customgrant.CustomPasswordAuthenticationProvider;
import com.devsuperior.dscommerce.config.customgrant.CustomUserAuthorities;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class AuthorizationServerConfig {

    // Busca da application.properties
	@Value("${security.client-id}")
	private String clientId;

    // Busca da application.properties
	@Value("${security.client-secret}")
	private String clientSecret;

    // Busca da application.properties
	@Value("${security.jwt.duration}")
	private Integer jwtDurationSeconds;

	@Autowired
	private UserDetailsService userDetailsService;

    // O order 1 e o 3 está no Resource Server
	@Bean
	@Order(2)
	public SecurityFilterChain asSecurityFilterChain(HttpSecurity http) throws Exception {

        // Estamos configurando o Authorization Server para funcionar com o SpringSecurity
		http.securityMatcher("/oauth2/**", "/.well-known/**").with(OAuth2AuthorizationServerConfigurer.authorizationServer(), Customizer.withDefaults());

		// @formatter:off
		// Configura todo o token, a autenticação
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
			.tokenEndpoint(tokenEndpoint -> tokenEndpoint
				.accessTokenRequestConverter(new CustomPasswordAuthenticationConverter())
                    // Usa o UserDetailsService para buscar os dados de usuário no Spring Security
				.authenticationProvider(new CustomPasswordAuthenticationProvider(authorizationService(), tokenGenerator(), userDetailsService, passwordEncoder())));

        // Configura a autenticação para gerar o token como JWT - Esse token é assinado, usando assimetria
        // A chave privada, só o Authorization Server conhece
		http.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()));
		// @formatter:on

		return http.build();
	}

	@Bean
	public OAuth2AuthorizationService authorizationService() {
		return new InMemoryOAuth2AuthorizationService();
	}

	@Bean
	public OAuth2AuthorizationConsentService oAuth2AuthorizationConsentService() {
		return new InMemoryOAuth2AuthorizationConsentService();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

    // Registra o nosso cliente. Isso é gerado em memória, não é salvo em nenhum banco de dados. Enquanto
    // o token dele não expirar e a aplicação ficar ativa, o token fica ativo
	@Bean
	public RegisteredClientRepository registeredClientRepository() {
		// @formatter:off
		RegisteredClient registeredClient = RegisteredClient
			.withId(UUID.randomUUID().toString())
			.clientId(clientId)
			.clientSecret(passwordEncoder().encode(clientSecret))
			.scope("read")
			.scope("write")
			.authorizationGrantType(new AuthorizationGrantType("password"))
			.tokenSettings(tokenSettings())
			.clientSettings(clientSettings())
			.build();
		// @formatter:on

		return new InMemoryRegisteredClientRepository(registeredClient);
	}

    // Faz configurações do token, principalmente a duração
	@Bean
	public TokenSettings tokenSettings() {
		// @formatter:off
		return TokenSettings.builder()
			.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
			.accessTokenTimeToLive(Duration.ofSeconds(jwtDurationSeconds))
			.build();
		// @formatter:on
	}

	@Bean
	public ClientSettings clientSettings() {
		return ClientSettings.builder().build();
	}


	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().build();
	}

    // Faz algumas configurações do token também
	@Bean
	public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator() {
		NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource());
		JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
		jwtGenerator.setJwtCustomizer(tokenCustomizer());
		OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
		return new DelegatingOAuth2TokenGenerator(jwtGenerator, accessTokenGenerator);
	}

    // Essa função customiza o ‘token’, colocando as reivindicações necessárias
	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
		return context -> {
			OAuth2ClientAuthenticationToken principal = context.getPrincipal();
			CustomUserAuthorities user = (CustomUserAuthorities) principal.getDetails();
			List<String> authorities = user.getAuthorities().stream().map(x -> x.getAuthority()).toList();
			if (context.getTokenType().getValue().equals("access_token")) {
				// @formatter:off
                // Podemos passar outros valores além dos que já estão aqui, vai depender da
                // necessidade da aplicação
				context.getClaims()
					.claim("authorities", authorities)
					.claim("username", user.getUsername());
				// @formatter:on
			}
		};
	}

	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}

	@Bean
	public JWKSource<SecurityContext> jwkSource() {
		RSAKey rsaKey = generateRsa();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
	}

    // Esses três métodos abaixo são responsáveis por gerar as chaves que apenas o Authorization Server
    // vai reconhecer como chave
	private static RSAKey generateRsa() {
		KeyPair keyPair = generateRsaKey();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(UUID.randomUUID().toString()).build();
	}

	private static KeyPair generateRsaKey() {
		KeyPair keyPair;
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			keyPair = keyPairGenerator.generateKeyPair();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		return keyPair;
	}
}
