package com.chess.auth.config;

import com.chess.auth.filter.RequestIdMdcFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestTracingConfig {

    @Bean
    public FilterRegistrationBean<RequestIdMdcFilter> requestIdMdcFilter() {
        FilterRegistrationBean<RequestIdMdcFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestIdMdcFilter());
        bean.setOrder(Integer.MIN_VALUE);
        return bean;
    }
}

