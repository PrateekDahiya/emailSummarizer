package com.gmailreader.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Serve static resources from Next.js build
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(org.springframework.web.servlet.resource.PathResourceResolver().apply {
                addAllowedLocations("classpath:/static/")
            })

        // Next.js specific paths
        registry.addResourceHandler("/_next/**")
            .addResourceLocations("classpath:/static/_next/")
            .setCachePeriod(31536000) // 1 year for hashed assets
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        // Serve index.html for root
        registry.addViewController("/").setViewName("forward:/index.html")
        
        // Handle SPA routes - forward to index.html for client-side routing
        registry.addViewController("/dashboard/**").setViewName("forward:/index.html")
        registry.addViewController("/auth/**").setViewName("forward:/index.html")
        registry.addViewController("/settings/**").setViewName("forward:/index.html")
    }
}