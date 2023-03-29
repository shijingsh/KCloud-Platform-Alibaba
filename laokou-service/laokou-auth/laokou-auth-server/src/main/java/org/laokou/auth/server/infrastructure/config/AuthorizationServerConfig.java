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
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.SneakyThrows;
import org.laokou.auth.server.domain.sys.repository.service.*;
import org.laokou.auth.server.domain.sys.repository.service.impl.SysUserDetailServiceImpl;
import org.laokou.auth.server.domain.sys.repository.service.impl.SysUserServiceImpl;
import org.laokou.auth.server.infrastructure.authentication.*;
import org.laokou.common.core.utils.ResourceUtil;
import org.laokou.common.log.utils.LoginLogUtil;
import org.laokou.redis.utils.RedisUtil;
import org.laokou.tenant.service.SysSourceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
import org.springframework.security.oauth2.core.oidc.OidcScopes;
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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
/**
 * @author laokou
 */
@Configuration
public class AuthorizationServerConfig {

    /**
     *  授权端点过滤器链
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
    , SysAuthenticationService sysAuthenticationService
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
        http.authenticationProvider(new OAuth2PasswordAuthenticationProvider(sysUserService,sysMenuService,sysDeptService,loginLogUtil,passwordEncoder,sysCaptchaService,authorizationService,tokenGenerator, sysSourceService,sysAuthenticationService,redisUtil))
                .authenticationProvider(new OAuth2MobileAuthenticationProvider(sysUserService,sysMenuService,sysDeptService,loginLogUtil,passwordEncoder,sysCaptchaService,authorizationService,tokenGenerator, sysSourceService,sysAuthenticationService,redisUtil))
                .authenticationProvider(new OAuth2MailAuthenticationProvider(sysUserService,sysMenuService,sysDeptService,loginLogUtil,passwordEncoder,sysCaptchaService,authorizationService,tokenGenerator, sysSourceService,sysAuthenticationService,redisUtil));
        return defaultSecurityFilterChain;
    }

    /**
     * 注册客户端
     * @param jdbcTemplate
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        RegisteredClient registeredClient = RegisteredClient.withId("95TxSsTPFA3tF12TBSMmUVK0da")
                .clientId("95TxSsTPFA3tF12TBSMmUVK0da")
                .clientSecret("$2a$10$BDcxgmL3WYk7G.QEDTqlBeSudNlV3KUU/V6iC.hKlAbGAC.jbX2fO")
                // ClientAuthenticationMethod.CLIENT_SECRET_BASIC => client_id:client_secret => 95TxSsTPFA3tF12TBSMmUVK0da:FpHwIfw4wY92dO 进行Base64编码后的值
                // Headers Authorization Basic OTVUeFNzVFBGQTN0RjEyVEJTTW1VVkswZGE6RnBId0lmdzR3WTkyZE8=
                // http://localhost:1111/oauth2/authorize?client_id=auth-client&client_secret=secret&response_type=code&scope=password mail mobile&redirect_uri=https://www.baidu.com
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantTypes(authorizationGrantTypes -> authorizationGrantTypes.addAll(
                        List.of(AuthorizationGrantType.AUTHORIZATION_CODE
                                , AuthorizationGrantType.REFRESH_TOKEN
                                , new AuthorizationGrantType(OAuth2PasswordAuthenticationProvider.GRANT_TYPE)
                                , new AuthorizationGrantType(OAuth2MailAuthenticationProvider.GRANT_TYPE)
                                , new AuthorizationGrantType(OAuth2MobileAuthenticationProvider.GRANT_TYPE)
                                , AuthorizationGrantType.CLIENT_CREDENTIALS)))
                .scopes(scopes -> scopes.addAll(List.of(
                          OAuth2PasswordAuthenticationProvider.GRANT_TYPE
                        , OAuth2MailAuthenticationProvider.GRANT_TYPE
                        , OAuth2MobileAuthenticationProvider.GRANT_TYPE
                        , OidcScopes.OPENID
                        , OidcScopes.PROFILE
                        , OidcScopes.PHONE
                        , OidcScopes.ADDRESS
                        , OidcScopes.EMAIL
                )))
                .redirectUris(redirectUris -> redirectUris.addAll(List.of(
                          "https://spring.io"
                        , "https://github.com/KouShenhai"
                        , "https://www.baidu.com"
                )))
                .clientName("认证")
                // JWT配置
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofHours(6))
                        .build())
                // 客户端配置，包括验证密钥或需要授权页面
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .build();
        JdbcRegisteredClientRepository registeredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);
        registeredClientRepository.save(registeredClient);
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
     * @param sysUserService
     * @param sysMenuService
     * @param passwordEncoder
     * @param sysDeptService
     * @return
     */
    @Bean
    UserDetailsService userDetailsService(
            SysUserServiceImpl sysUserService
            , SysMenuService sysMenuService
            , PasswordEncoder passwordEncoder
            , SysDeptService sysDeptService) {
        return new SysUserDetailServiceImpl(sysUserService,sysMenuService
                , sysDeptService,passwordEncoder);
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
    JWKSource<SecurityContext> jwkSource() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, JOSEException {
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
