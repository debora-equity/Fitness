package com.softgenia.playlist.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

        @Value("${upload.path}")
        private String uploadPath;

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String absoluteUploadPath = java.nio.file.Paths.get(uploadPath).toAbsolutePath().normalize().toString();

                registry.addResourceHandler("/uploads/**")
                                .addResourceLocations("file:" + absoluteUploadPath + "/");
                registry.addResourceHandler("/thumbnails/**")
                                .addResourceLocations("file:" + absoluteUploadPath + "/thumbnails/");
                registry.addResourceHandler("/documents/**")
                                .addResourceLocations("file:" + absoluteUploadPath + "/documents/");
                registry.addResourceHandler("/videos/**")
                                .addResourceLocations("file:" + absoluteUploadPath + "/videos/");
        }
}
