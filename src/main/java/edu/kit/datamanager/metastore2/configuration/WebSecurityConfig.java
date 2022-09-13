/*
 * Copyright 2018 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.metastore2.configuration;

import edu.kit.datamanager.security.filter.KeycloakTokenFilter;
import edu.kit.datamanager.security.filter.NoAuthenticationFilter;
import java.util.Optional;
import javax.servlet.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 *
 * @author jejkal
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  private Optional<KeycloakTokenFilter> keycloaktokenFilterBean;

  @Value("${metastore.security.enable-csrf:true}")
  private boolean enableCsrf;
  @Value("${metastore.security.allowedOriginPattern:http*://localhost:*}")
  private String allowedOriginPattern;

  public WebSecurityConfig() {
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    HttpSecurity httpSecurity = http.authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**").
            permitAll().
            and().
            sessionManagement().
            sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();
    if (!enableCsrf) {
      httpSecurity = httpSecurity.csrf().disable();
    }
    if (keycloaktokenFilterBean.isPresent()) {
      httpSecurity.addFilterAfter(keycloaktokenFilterBean.get(), BasicAuthenticationFilter.class);
    }
    if (!applicationProperties.isAuthEnabled()) {
      logger.info("Authentication is DISABLED. Adding 'NoAuthenticationFilter' to authentication chain.");
      httpSecurity = httpSecurity.addFilterAfter(new NoAuthenticationFilter(applicationProperties.getJwtSecret(), authenticationManager()), BasicAuthenticationFilter.class);

      logger.info("Authentication is ENABLED.");
    }

    httpSecurity.
            authorizeRequests().
            antMatchers("/api/v1").authenticated();

    http.headers().cacheControl().disable();
  }

  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
    DefaultHttpFirewall firewall = new DefaultHttpFirewall();
    firewall.setAllowUrlEncodedSlash(true);
    return firewall;
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
  }

  @Bean
  public FilterRegistrationBean corsFilter() {
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    String[] allOrigins = allowedOriginPattern.split("[ ]*,[ ]*");
    for (String origin : allOrigins) {
      logger.info("Add origin pattern: '{}'", origin);
      config.addAllowedOriginPattern(origin); // @Value: http://localhost:8080
    }
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    config.addExposedHeader("Content-Range");
    config.addExposedHeader("ETag");

    source.registerCorsConfiguration("/**", config);
    FilterRegistrationBean<Filter> bean;
    bean = new FilterRegistrationBean<>(new CorsFilter(source));
    bean.setOrder(0);
    return bean;
  }

}
