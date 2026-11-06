package com.sya.ragnote.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.sya.ragnote.mapper")
public class MybatisConfig {}
