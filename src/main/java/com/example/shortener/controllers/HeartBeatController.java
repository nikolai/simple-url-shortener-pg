package com.example.shortener.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class HeartBeatController {

    @GetMapping("/heartbeat")
    public String heartbeat() {
        return new Date().toString();
    }
}
