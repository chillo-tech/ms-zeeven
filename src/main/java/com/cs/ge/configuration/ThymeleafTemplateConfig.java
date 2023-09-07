package com.cs.ge.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ThymeleafTemplateConfig {
    @Bean
    ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    /*
        @Bean
        public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
            final Jackson2ObjectMapperBuilder builder =
                    new Jackson2ObjectMapperBuilder()
                            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .serializers(
                                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")))
                            .serializationInclusion(JsonInclude.Include.NON_NULL);
            return new MappingJackson2HttpMessageConverter(builder.build());
        }
    */
    /*
        @Bean
        public TemplateEngine templateEngine() {
            final TemplateEngine templateEngine = new TemplateEngine();
            templateEngine.setTemplateResolver(this.thymeleafTemplateResolver());
            return templateEngine;
        }
    */
    @Bean
    public ClassLoaderTemplateResolver thymeleafTemplateResolver() {
        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        return templateResolver;
    }

}
