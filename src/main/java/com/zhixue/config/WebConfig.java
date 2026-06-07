package com.zhixue.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {

        // 头像上传路径
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(
                        "file:" +
                                System.getProperty("user.dir")
                                + "/upload/"
                );

        // 👇 新增：帖子图片上传路径（如果单独存放）
        // 如果帖子图片和头像都放在 /upload/ 下，上面的配置已经覆盖了，不需要额外添加
    }
}
