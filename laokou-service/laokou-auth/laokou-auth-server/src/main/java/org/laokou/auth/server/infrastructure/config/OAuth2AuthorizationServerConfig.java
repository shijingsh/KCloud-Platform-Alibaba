/**
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.laokou.auth.server.infrastructure.config;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.SneakyThrows;
import org.laokou.auth.server.domain.sys.repository.service.*;
import org.laokou.auth.server.infrastructure.authentication.*;
import org.laokou.common.core.utils.ResourceUtil;
import org.laokou.common.easy.captcha.service.SysCaptchaService;
import org.laokou.common.log.utils.LoginLogUtil;
import org.laokou.common.redis.utils.RedisUtil;
import org.laokou.common.tenant.service.SysSourceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.*;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
/**
 * @author laokou
 */
@Configuration
@ConditionalOnProperty(havingValue = "true",matchIfMissing = true,prefix = OAuth2AuthorizationServerProperties.PREFIX,name = "enabled")
public class OAuth2AuthorizationServerConfig {

    /**
     *
     * @param http
     * @param authorizationServerSettings
     * @param authorizationService
     * @param sysUserService
     * @param sysMenuService
     * @param sysDeptService
     * @param loginLogUtil
     * @param passwordEncoder
     * @param sysCaptchaService
     * @param tokenGenerator
     * @param sysSourceService
     * @param sysAuthenticationService
     * @param redisUtil
     * @return
     * @throws Exception
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http
    , AuthorizationServerSettings authorizationServerSettings
    , OAuth2AuthorizationService authorizationService
    , SysUserService sysUserService
    , SysMenuService sysMenuService
    , SysDeptService sysDeptService
    , LoginLogUtil loginLogUtil
    , PasswordEncoder passwordEncoder
    , SysCaptchaService sysCaptchaService
    , OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator
    , SysSourceService sysSourceService
    , RedisUtil redisUtil) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        authorizationServerConfigurer.oidc(Customizer.withDefaults());
        http.exceptionHandling(configurer -> configurer.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .apply(authorizationServerConfigurer.tokenEndpoint((tokenEndpoint) -> tokenEndpoint.accessTokenRequestConverter(new DelegatingAuthenticationConverter(
                List.of(new OAuth2PasswordAuthenticationConverter()
                        , new OAuth2MobileAuthenticationConverter()
                        , new OAuth2MailAuthenticationConverter()
                        , new OAuth2AuthorizationCodeAuthenticationConverter()
                        , new OAuth2ClientCredentialsAuthenticationConverter()
                        , new OAuth2RefreshTokenAuthenticationConverter()
                        , new OAuth2AuthorizationCodeRequestAuthenticationConverter())))));
        DefaultSecurityFilterChain defaultSecurityFilterChain = http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .authorizeHttpRequests(authorizeRequests -> {
                    // 忽略error
                    authorizeRequests.requestMatchers("/error").permitAll();
                    authorizeRequests.anyRequest().authenticated();
                })
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
                .apply(authorizationServerConfigurer
                        .authorizationService(authorizationService)
                        .authorizationServerSettings(authorizationServerSettings))
                .and()
                .build();
        http.authenticationProvider(new OAuth2PasswordAuthenticationProvider(sysUserService,sysMenuService,sysDeptService,loginLogUtil,passwordEncoder,sysCaptchaService,authorizationService,tokenGenerator, sysSourceService,redisUtil))
                .authenticationProvider(new OAuth2MobileAuthenticationProvider(sysUserService,sysMenuService,sysDeptService,loginLogUtil,passwordEncoder,sysCaptchaService,authorizationService,tokenGenerator, sysSourceService,redisUtil))
                .authenticationProvider(new OAuth2MailAuthenticationProvider(sysUserService,sysMenuService,sysDeptService,loginLogUtil,passwordEncoder,sysCaptchaService,authorizationService,tokenGenerator, sysSourceService,redisUtil));
        return defaultSecurityFilterChain;
    }

    /**
     * @param jdbcTemplate
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate,OAuth2AuthorizationServerProperties properties) {
        OAuth2AuthorizationServerProperties.Client client = properties.getClient();
        OAuth2AuthorizationServerProperties.Token token = properties.getToken();
        OAuth2AuthorizationServerProperties.Registration registration = properties.getRegistration();
        RegisteredClient.Builder registrationBuilder = RegisteredClient.withId(registration.getId());
        TokenSettings.Builder tokenBuilder = TokenSettings.builder();
        ClientSettings.Builder clientBuilder = ClientSettings.builder();
        PropertyMapper map = PropertyMapper.get();
        // 令牌 => JWT配置
        map.from(token::getAccessTokenTimeToLive).to(tokenBuilder::accessTokenTimeToLive);
        map.from(token::getRefreshTokenTimeToLive).to(tokenBuilder::refreshTokenTimeToLive);
        // 客户端配置，包括验证密钥或需要授权页面
        map.from(client::isRequireAuthorizationConsent).to(clientBuilder::requireAuthorizationConsent);
        // 注册
        // ClientAuthenticationMethod.CLIENT_SECRET_BASIC => client_id:client_secret => 95TxSsTPFA3tF12TBSMmUVK0da:FpHwIfw4wY92dO 进行Base64编码后的值
        // Headers Authorization Basic OTVUeFNzVFBGQTN0RjEyVEJTTW1VVkswZGE6RnBId0lmdzR3WTkyZE8=
        // http://localhost:1111/oauth2/authorize?client_id=auth-client&client_secret=secret&response_type=code&scope=password mail mobile&redirect_uri=http://127.0.0.1:8000
        map.from(registration::getClientId).to(registrationBuilder::clientId);
        map.from(registration::getClientName).to(registrationBuilder::clientName);
        map.from(registration::getClientSecret).to(registrationBuilder::clientSecret);
        registration.getClientAuthenticationMethods().forEach(clientAuthenticationMethod -> map.from(clientAuthenticationMethod)
                .whenNonNull()
                .as(ClientAuthenticationMethod::new)
                .to(registrationBuilder::clientAuthenticationMethod));
        registration.getAuthorizationGrantTypes().forEach(authorizationGrantType -> map.from(authorizationGrantType)
                .whenNonNull()
                .as(AuthorizationGrantType::new)
                .to(registrationBuilder::authorizationGrantType));
        registration.getScopes().forEach(scope -> map.from(scope)
                .whenNonNull()
                .to(registrationBuilder::scope));
        registration.getRedirectUris().forEach(redirectUri -> map.from(redirectUri)
                .whenNonNull()
                .to(registrationBuilder::redirectUri));
        registrationBuilder.tokenSettings(tokenBuilder.build());
        registrationBuilder.clientSettings(clientBuilder.build());
        JdbcRegisteredClientRepository registeredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);
        registeredClientRepository.save(registrationBuilder.build());
        return registeredClientRepository;
    }

    /**
     *
     * @param jwtEncoder
     * @return
     */
    @Bean
    OAuth2TokenGenerator<OAuth2Token> oAuth2TokenGenerator(JwtEncoder jwtEncoder) {
        JwtGenerator generator = new JwtGenerator(jwtEncoder);
        return new DelegatingOAuth2TokenGenerator(generator, new OAuth2RefreshTokenGenerator());
    }

    /**
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(AuthorizationServerSettings.class)
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    /**
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     *
     * @param jdbcTemplate
     * @param registeredClientRepository
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizationService.class)
    OAuth2AuthorizationService auth2AuthorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    /**
     *
     * @param passwordEncoder
     * @param userDetailsService
     * @return
     */
    @Bean
    AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder
            , UserDetailsService userDetailsService) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        return daoAuthenticationProvider;
    }

    /**
     * JWK资源
     * @return
     */
    @Bean
    JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = getRSAKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    /**
     * JWT解码器
     * 客户端认证授权后，需要访问用户信息，解码器可以从令牌中解析用户信息
     * @return
     * @throws CertificateException
     */
    @Bean
    JwtDecoder jwtDecoder() throws CertificateException, IOException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("x.509");
        // 读取cer公钥证书来配置解码器
        InputStream inputStream = ResourceUtil.getResource("auth.cer").getInputStream();
        Certificate certificate = certificateFactory.generateCertificate(inputStream);
        RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    /**
     *
     * @param jwkSource
     * @return
     */
    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     *
     * @param jdbcTemplate
     * @param registeredClientRepository
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizationConsentService.class)
    OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * 获取RSA密钥
     * @return
     */
    @SneakyThrows
    private RSAKey getRSAKey() {
        String alias = "auth";
        String password = "koushenhai";
        String path = "auth.jks";
        InputStream inputStream = ResourceUtil.getResource(path).getInputStream();
        KeyStore jks = KeyStore.getInstance("jks");
        char[] pwd = password.toCharArray();
        jks.load(inputStream,pwd);
        return RSAKey.load(jks, alias, pwd);
    }

}
