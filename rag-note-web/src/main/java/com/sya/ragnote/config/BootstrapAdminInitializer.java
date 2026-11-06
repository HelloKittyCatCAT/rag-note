package com.sya.ragnote.config;

import com.sya.ragnote.service.user.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final UserService users;
    private final RagProperties properties;

    public void run(ApplicationArguments args) {
        users.initializeAdmin(
                properties.getBootstrap().getUsername(), properties.getBootstrap().getPassword());
    }
}
