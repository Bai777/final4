package com.javarush.baymakov;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.baymakov.dao.CityDAO;
import com.javarush.baymakov.dao.CountryDAO;
import com.javarush.baymakov.domain.City;
import com.javarush.baymakov.domain.Country;
import com.javarush.baymakov.domain.CountryLanguage;
import com.javarush.baymakov.redis.CityCountry;
import com.javarush.baymakov.redis.RedisDataTransformer;
import com.javarush.baymakov.redis.RedisService;
import com.javarush.baymakov.service.DataValidationService;
import io.lettuce.core.RedisClient;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.List;

import static java.util.Objects.nonNull;

public class Main {
    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;
    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;

    public Main(SessionFactory sessionFactory, RedisClient redisClient,
                CityDAO cityDAO, CountryDAO countryDAO) {
        this.sessionFactory = sessionFactory;
        this.redisClient = redisClient;
        this.cityDAO = cityDAO;
        this.countryDAO = countryDAO;
    }

    public static void main(String[] args) {
        SessionFactory sessionFactory = new Configuration()
                .addAnnotatedClass(City.class)
                .addAnnotatedClass(Country.class)
                .addAnnotatedClass(CountryLanguage.class)
                .buildSessionFactory();

        RedisClient redisClient = RedisClient.create("redis://localhost:6379");
        ObjectMapper mapper = new ObjectMapper();
        RedisService redisService = new RedisService(redisClient, mapper);
        RedisDataTransformer transformer = new RedisDataTransformer();

        CityDAO cityDAO = new CityDAO(sessionFactory);
        CountryDAO countryDAO = new CountryDAO(sessionFactory);
        DataValidationService validationService = new DataValidationService(sessionFactory, cityDAO);

        Main main = new Main(sessionFactory, redisClient, cityDAO, countryDAO);

        try {
            List<City> allCities = main.cityDAO.getAllCities();
            System.out.println("Загружено городов: " + allCities.size());
            List<CityCountry> preparedData = transformer.transformData(allCities);
            redisService.pushToRedis(preparedData);
            System.out.println("Данные сохранены в Redis");
            List<Integer> ids = List.of(3, 2545, 123, 4, 189, 89, 3458, 1189, 10, 102);

            long startRedis = System.currentTimeMillis();
            redisService.testRedisData(ids);
            long stopRedis = System.currentTimeMillis();

            long startMysql = System.currentTimeMillis();
            validationService.testMysqlData(ids);
            long stopMysql = System.currentTimeMillis();

            System.out.printf("Redis:\t%d ms\n", (stopRedis - startRedis));
            System.out.printf("MySQL:\t%d ms\n", (stopMysql - startMysql));

        } finally {
            main.shutdown();
        }
    }

    public void shutdown() {
        if (nonNull(sessionFactory)) {
            sessionFactory.close();
        }
        if (nonNull(redisClient)) {
            redisClient.shutdown();
        }
    }
}