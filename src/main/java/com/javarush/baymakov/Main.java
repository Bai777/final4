package com.javarush.baymakov;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.baymakov.dao.CityDAO;
import com.javarush.baymakov.dao.CountryDAO;
import io.lettuce.core.RedisClient;
import org.hibernate.SessionFactory;

public class Main {
    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;

    private final ObjectMapper mapper;

    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;

    public Main(SessionFactory sessionFactory, RedisClient redisClient, ObjectMapper mapper, CityDAO cityDAO, CountryDAO countryDAO) {
        this.sessionFactory = sessionFactory;
        this.redisClient = redisClient;
        this.mapper = mapper;
        this.cityDAO = cityDAO;
        this.countryDAO = countryDAO;
    }

    public static void main(String[] args) {
        sessionFactory = prepareRelationalDb();
        cityDAO = new CityDAO(sessionFactory);
        countryDAO = new CountryDAO(sessionFactory);

        redisClient = prepareRedisClient();
        mapper = new ObjectMapper();
    }
}