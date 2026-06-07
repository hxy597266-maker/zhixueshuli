package com.zhixue.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "欢迎来到智学数理平台！";
    }

    // 处理 Chrome DevTools 的探测请求，直接返回 404，避免控制台报错
    @GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void chromeDevToolsJson() {
        // 无需任何逻辑，只需存在这个映射即可
    }
}