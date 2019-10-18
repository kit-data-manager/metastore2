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
package edu.kit.datamanager.metastore2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.RabbitMQMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 *
 * @author jejkal
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan({"edu.kit.datamanager", "edu.kit.datamanager.messaging.client"})
public class Application{

  @Bean
  @Scope("prototype")
  public Logger logger(InjectionPoint injectionPoint){
    Class<?> targetClass = injectionPoint.getMember().getDeclaringClass();
    return LoggerFactory.getLogger(targetClass.getCanonicalName());
  }

  @Bean(name = "OBJECT_MAPPER_BEAN")
  public ObjectMapper jsonObjectMapper(){
    return Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(JsonInclude.Include.NON_EMPTY) // Don’t include null values
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
            .modules(new JavaTimeModule())
            .build();
  }

//  @Bean
//  public WebMvcConfigurer corsConfigurer(){
//    return new WebMvcConfigurer(){
//      @Override
//      public void addCorsMappings(CorsRegistry registry){
//        registry.addMapping("/**").allowedOrigins("http://localhost:8090").exposedHeaders("Content-Length").allowedHeaders("Accept");
//      }
//    };
//  }
//  @Bean
//  @Primary
//  public RequestMappingHandlerAdapter adapter(){
//    return requestMappingHandlerAdapter;
//  }
  @Bean
  @ConfigurationProperties("repo")
  public ApplicationProperties metastoreProperties(){
    return new ApplicationProperties();
  }

  @Bean
  public IMessagingService messagingService(){
    return new RabbitMQMessagingService();
  }

  public static void main(String[] args){
    ApplicationContext ctx = SpringApplication.run(Application.class, args);
    System.out.println("Spring is running!");
  }

}
