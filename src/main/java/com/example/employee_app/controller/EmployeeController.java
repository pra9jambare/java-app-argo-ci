package com.example.employee_app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {

    @GetMapping("/")
    public String home() {
        return "Employee App Running";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
