package com.javarush.baymakov;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.baymakov.dao.CityDAO;
import com.javarush.baymakov.dao.CountryDAO;
import com.javarush.baymakov.domain.City;
import com.javarush.baymakov.domain.Country;
import com.javarush.baymakov.domain.CountryLanguage;
import io.lettuce.core.RedisClient;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.List;

import static java.util.Objects.nonNull;

public class Main {
    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;
    private final ObjectMapper mapper;
    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;

    public Main(SessionFactory sessionFactory, RedisClient redisClient, ObjectMapper mapper,
                CityDAO cityDAO, CountryDAO countryDAO) {
        this.sessionFactory = sessionFactory;
        this.redisClient = redisClient;
        this.mapper = mapper;
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

        CityDAO cityDAO = new CityDAO(sessionFactory);
        CountryDAO countryDAO = new CountryDAO(sessionFactory);

        Main main = new Main(sessionFactory, redisClient, mapper, cityDAO, countryDAO);

        try {
            List<City> allCities = main.cityDAO.getAllCities();
            System.out.println("Загружено городов: " + allCities.size());

            List<Country> countries = main.countryDAO.getAll();
            System.out.println("Стран загружено: " + countries.size());
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