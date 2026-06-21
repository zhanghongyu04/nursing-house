package com.zhiling.gateway.config;

import com.zhiling.common.properties.SecurityConfigProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 权限核心配置类
 *
 * @author zhanghongyu
 */
@Configuration
@EnableConfigurationProperties(SecurityConfigProperties.class)
public class SecurityConfig {

    private final SecurityConfigProperties securityConfigProperties;
    private final AuthorizationManager<RequestAuthorizationContext> jwtAuthorizationManager;
    private final OncePerRequestFilter jwtAuthenticationFilter;

    public SecurityConfig(SecurityConfigProperties securityConfigProperties,
                          @Qualifier("jwtAuthorizationManager") AuthorizationManager<RequestAuthorizationContext> jwtAuthorizationManager,
                          @Qualifier("jwtAuthenticationFilter") OncePerRequestFilter jwtAuthenticationFilter) {
        this.securityConfigProperties = securityConfigProperties;
        this.jwtAuthorizationManager = jwtAuthorizationManager;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DaoAuthenticationProvider daoAuthenticationProvider) throws Exception {
        List<String> ignoreUrl = securityConfigProperties.getIgnoreUrl();

        http
                .securityMatcher("/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(ignoreUrl.toArray(new String[0]))
                        .permitAll()
                        .anyRequest()
                        .access(jwtAuthorizationManager)
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(daoAuthenticationProvider);

        return http.build();
    }

    /**
     * 配置 DaoAuthenticationProvider，用于用户名密码认证。
     * 通过 @Bean 方法参数注入 UserDetailsService，避免构造器循环依赖。
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(bcryptPasswordEncoder());
        return provider;
    }

    /**
     * 方法：corsConfigurationSource
     *
     * @author zhanghongyu
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of(
                "Captcha-Key",
                "X-RAG-HIT",
                "X-RAG-HIT-COUNT",
                "X-RAG-SOURCES"
        ));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * BCrypt密码编码
     */
    @Bean
    public BCryptPasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}