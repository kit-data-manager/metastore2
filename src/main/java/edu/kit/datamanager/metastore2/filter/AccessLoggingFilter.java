/*
 * Copyright 2022 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.filter;

import edu.kit.datamanager.security.filter.KeycloakTokenFilter;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter for logging access by users
 */
public class AccessLoggingFilter implements Filter {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AccessLoggingFilter.class);
  /**
   * Prefix of authorization header.
   */
  private static final String BEARER = "Bearer ";

  @Override
  public void doFilter(
          ServletRequest request,
          ServletResponse response,
          FilterChain chain) throws IOException, ServletException {
    String authToken;
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    authToken = req.getHeader(KeycloakTokenFilter.AUTHORIZATION_HEADER);
    if ((authToken != null) && (authToken.length() > BEARER.length())) {
      authToken = authToken.substring(BEARER.length());
    }
    chain.doFilter(request, response);

    // authToken may be null if authentication is disabled.
    LOGGER.trace("'{}' access to '{}' -> Status: '{}'", req.getMethod(), req.getRequestURI(), resp.getStatus(), authToken);
  }
}
