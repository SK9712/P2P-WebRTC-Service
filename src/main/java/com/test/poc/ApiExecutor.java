package com.test.poc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ApiExecutor {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Gson gson;

    public JsonObject processApiRequest(JsonObject request){
        return null;
    }
}
