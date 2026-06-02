package com.javarush.baymakov.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;

import java.util.List;

public class RedisService {
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;

    public RedisService(RedisClient redisClient, ObjectMapper objectMapper) {
        this.redisClient = redisClient;
        this.objectMapper = objectMapper;
    }

    public void pushToRedis(List<CityCountry> data) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (CityCountry cityCountry : data) {
                try {
                    sync.set(String.valueOf(cityCountry.getId()), objectMapper.writeValueAsString(cityCountry));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void testRedisData(List<Integer> ids) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : ids) {
                String value = sync.get(String.valueOf(id));
                try {
                    objectMapper.readValue(value, CityCountry.class);
                    System.out.println("Успешно прочитан и десериализован объект с id=" + id);
                } catch (JsonProcessingException e) {
                    System.err.println("Ошибка десериализации для id=" + id);
                    e.printStackTrace();
                }
            }
        }
    }
}