/*
 * Copyright 2025 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.test;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for the Prometheus actuator endpoint.
 * This test checks that the Prometheus endpoint is correctly exposed and that it contains the expected metrics.
 * Also checks that other actuator endpoints are not exposed.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT) //RANDOM_PORT)
@EntityScan("edu.kit.datamanager")
@EnableJpaRepositories("edu.kit.datamanager")
@ComponentScan({"edu.kit.datamanager"})
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        WithSecurityContextTestExecutionListener.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"server.port=41426"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/prometheus/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/prometheus/metadata"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_prometheus;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@TestPropertySource(properties = {"spring.jpa.defer-datasource-initialization=true"})
@TestPropertySource(properties = {"repo.monitoring.enabled=true"})
@TestPropertySource(properties = {"repo.monitoring.serviceName=metastore"})
@TestPropertySource(properties = {"management.endpoint.prometheus.enabled=true"})
@TestPropertySource(properties = {"management.endpoints.web.exposure.include=info,health,prometheus"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureObservability
public class ActuatorPrometheusTest {


  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  @Before
  public void setUp() {
    // setup mockMvc
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(springSecurity())
            .apply(documentationConfiguration(this.restDocumentation).uris()
                    .withPort(41416))
            .build();
  }

  @Test
  public void testForNotExposedActuators() throws Exception {
    this.mockMvc.perform(get("/actuator/beans")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/caches")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/conditions")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/configprops")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/env")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/loggers")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/heapdump")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/threaddump")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/metrics")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/scheduledtasks")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/actuator/mappings")).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testActuator() throws Exception {
    // /actuator/info
    MvcResult result = this.mockMvc.perform(get("/actuator/prometheus")).andDo(print()).andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("# TYPE metastore_metadata_documents")))
            .andExpect(content().string(Matchers.containsString("# TYPE metastore_metadata_schemas")))
            .andExpect(content().string(Matchers.containsString("# TYPE metastore_requests_served_total")))
            .andExpect(content().string(Matchers.containsString("# TYPE metastore_unique_users")))
            .andReturn();
  }
}
