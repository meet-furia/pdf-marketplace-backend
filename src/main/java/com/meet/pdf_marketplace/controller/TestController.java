package com.meet.pdf_marketplace.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/health")
    public String health() {
        return "PDF Marketplace Backend is running";
    }

    @GetMapping("/api/test")
    public String test() {
        return "Test API is working";
    }
}