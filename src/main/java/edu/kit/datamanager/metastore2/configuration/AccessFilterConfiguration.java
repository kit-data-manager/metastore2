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
package edu.kit.datamanager.metastore2.configuration;

import edu.kit.datamanager.metastore2.filter.AccessLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessFilterConfiguration {

    @Bean
    public FilterRegistrationBean<AccessLoggingFilter> loggingFilter() {
        FilterRegistrationBean<AccessLoggingFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new AccessLoggingFilter());

        registrationBean.addUrlPatterns("/api/v1/*");
        registrationBean.setOrder(Integer.MAX_VALUE);

        return registrationBean;

    }

}