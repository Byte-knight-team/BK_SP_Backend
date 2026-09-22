package com.ByteKnights.com.resturarent_system.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class WebCacheConfig {

    /**
     * Registers Spring's ShallowEtagHeaderFilter for all /api/* routes.
     * Generates MD5 ETag response headers for GET/HEAD requests.
     * If the payload hasn't changed, returns HTTP 304 Not Modified with 0 bytes
     * payload,
     * saving network bandwidth and client-side processing time.
     */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean = new FilterRegistrationBean<>(
                new ShallowEtagHeaderFilter());
        filterRegistrationBean.addUrlPatterns("/api/*");
        filterRegistrationBean.setName("shallowEtagHeaderFilter");
        filterRegistrationBean.setOrder(1);
        return filterRegistrationBean;
    }
}
