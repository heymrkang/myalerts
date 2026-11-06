package com.mrkang.myalerts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 정적 자원 및 MVC 설정 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 1. templates 디렉토리 내 js/css 파일을 정적 자원으로 서빙하도록 핸들러를 등록한다.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/templates/", "classpath:/static/");
    }
}
