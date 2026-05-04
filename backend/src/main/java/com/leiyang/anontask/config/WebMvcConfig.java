package com.leiyang.anontask.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  private final AppProperties props;

  public WebMvcConfig(AppProperties props) {
    this.props = props;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Serve local uploaded files in dev. In prod, it's expected to use OSS/COS and store URLs.
    Path uploadDir = Path.of(props.upload().dir()).toAbsolutePath().normalize();
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations(uploadDir.toUri().toString());
  }
}

