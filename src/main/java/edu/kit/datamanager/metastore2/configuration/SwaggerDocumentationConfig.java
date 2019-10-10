package edu.kit.datamanager.metastore2.configuration;

import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-07-09T15:21:24.632+02:00")

@Configuration
public class SwaggerDocumentationConfig{

  ApiInfo apiInfo(){
    return new ApiInfoBuilder()
            .title("Metastore2 Service")
            .description("The KIT DM 2.0 Metastore2 Service is a generic metadata repository and metadata schema registry.")
            .license("APACHE LICENSE, VERSION 2.0")
            .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
            .termsOfServiceUrl("")
            .version("0.1.0")
            .contact(new Contact("Thomas Jejkal", "", "thomas.jejkal@kit.edu"))
            .build();
  }

  @Bean
  public Docket customImplementation(){
    return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("edu.kit.datamanager.metastore2.web"))
            .build()
            .ignoredParameterTypes(Pageable.class, WebRequest.class, HttpServletResponse.class, UriComponentsBuilder.class)
            .apiInfo(apiInfo());
  }

}
