package com.saiko.bidmarket.common.config;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.saiko.bidmarket.jwt.Jwt;
import com.saiko.bidmarket.jwt.JwtAuthenticationFilter;
import com.saiko.bidmarket.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.saiko.bidmarket.oauth2.OAuth2AuthenticationSuccessHandler;
import com.saiko.bidmarket.user.service.UserService;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final JwtConfig jwtConfigure;

  public WebSecurityConfig(JwtConfig jwtConfigure) {

    this.jwtConfigure = jwtConfigure;
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler() {

    return (request, response, e) -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      Object principal = authentication != null ? authentication.getPrincipal() : null;
      log.warn("{} is denied", principal, e);
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("text/plain;charset=UTF-8");
      response.getWriter().write("ACCESS DENIED");
      response.getWriter().flush();
      response.getWriter().close();
    };
  }

  @Bean
  public Jwt jwt() {

    return new Jwt(
        jwtConfigure.getIssuer(),
        jwtConfigure.getClientSecret(),
        jwtConfigure.getExpirySeconds()
    );
  }

  public JwtAuthenticationFilter jwtAuthenticationFilter(Jwt jwt) {

    return new JwtAuthenticationFilter(jwtConfigure.getHeader(), jwt);
  }

  @Bean
  public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {

    return new HttpCookieOAuth2AuthorizationRequestRepository();
  }

  @Bean
  public OAuth2AuthorizedClientService authorizedClientService(
      JdbcOperations jdbcOperations,
      ClientRegistrationRepository clientRegistrationRepository
  ) {

    return new JdbcOAuth2AuthorizedClientService(jdbcOperations, clientRegistrationRepository);
  }

  @Bean
  public OAuth2AuthorizedClientRepository authorizedClientRepository(
      OAuth2AuthorizedClientService authorizedClientService) {

    return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
  }

  @Bean
  public OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler(Jwt jwt,
                                                                               UserService userService) {

    return new OAuth2AuthenticationSuccessHandler(jwt, userService);
  }

  @Bean
  public SecurityFilterChain filterChain(Jwt jwt,
                                         HttpSecurity http,
                                         OAuth2AuthorizedClientRepository repository,
                                         OAuth2AuthenticationSuccessHandler handler
  ) throws Exception {

    http.authorizeRequests()
        .antMatchers("/api/v1/products/**").hasAnyRole("USER", "ADMIN")
        .anyRequest().permitAll()
        .and()
        /**
         * formLogin, csrf, headers, http-basic, rememberMe, logout filter 비활성화
         */
        .formLogin()
        .disable()
        .csrf()
        .disable()
        .headers()
        .disable()
        .httpBasic()
        .disable()
        .rememberMe()
        .disable()
        .logout()
        .disable()
        /**
         * Session 사용하지 않음
         */
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        /**
         * OAuth2 설정
         */
        .oauth2Login()
        .authorizationEndpoint()
        .authorizationRequestRepository(authorizationRequestRepository())
        .and()
        .successHandler(handler)
        .authorizedClientRepository(repository)
        .and()
        /**
         * 예외처리 핸들러
         */
        .exceptionHandling()
        .accessDeniedHandler(accessDeniedHandler())
        .and()
        /**
         * Jwt 필터
         */
        .addFilterBefore(jwtAuthenticationFilter(jwt), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
