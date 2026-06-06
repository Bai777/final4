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
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

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

    public static void main(String[] args) throws Exception {
        String dbHost = getEnv("DB_HOST", "localhost");
        String dbPort = getEnv("DB_PORT", "3306");
        String dbName = getEnv("DB_NAME", "world");
        String dbUser = getEnv("DB_USER", "root");
        String dbPass = getEnv("DB_PASSWORD", "admin");
        String redisHost = getEnv("REDIS_HOST", "localhost");
        String redisPort = getEnv("REDIS_PORT", "6379");

        String cleanUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", dbHost, dbPort, dbName);
        String jdbcUrl = String.format("jdbc:p6spy:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", dbHost, dbPort, dbName);
        String redisUrl = String.format("redis://%s:%s", redisHost, redisPort);

        try (Connection connection = DriverManager.getConnection(cleanUrl, dbUser, dbPass)) {
            Liquibase liquibase = new Liquibase(
                    "db/changelog/changelog.xml",
                    new ClassLoaderResourceAccessor(),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))
            );
            liquibase.update("");
            System.out.println("Liquibase миграции выполнены");
        } catch (Exception e) {
            System.err.println("Ошибка выполнения Liquibase: " + e.getMessage());
            throw e;
        }

        Configuration cfg = new Configuration()
                .addAnnotatedClass(City.class)
                .addAnnotatedClass(Country.class)
                .addAnnotatedClass(CountryLanguage.class);
        cfg.setProperty("hibernate.connection.url", jdbcUrl);
        cfg.setProperty("hibernate.connection.username", dbUser);
        cfg.setProperty("hibernate.connection.password", dbPass);
        cfg.setProperty("hibernate.connection.driver_class", "com.p6spy.engine.spy.P6SpyDriver");

        SessionFactory sessionFactory = cfg.buildSessionFactory();

        RedisClient redisClient = RedisClient.create(redisUrl);
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

            List<Long> redisTimes = new ArrayList<>();
            List<Long> mysqlTimes = new ArrayList<>();

            for (int i = 1; i <= 10; i++) {

                long startRedis = System.nanoTime();
                redisService.testRedisData(ids);
                long redisDelta = System.nanoTime() - startRedis;

                long startMysql = System.nanoTime();
                validationService.testMysqlData(ids);
                long mysqlDelta = System.nanoTime() - startMysql;

                redisTimes.add(redisDelta / 1_000_000);
                mysqlTimes.add(mysqlDelta / 1_000_000);

                System.out.printf("Итерация %2d: Redis = %4d ms, MySQL = %4d ms%n",
                        i, redisTimes.get(i - 1), mysqlTimes.get(i - 1));
            }

            System.out.println("\n========= Сводная таблица времени (в миллисекундах) =========");
            System.out.printf("%-10s | %-10s | %-10s%n", "Итерация", "Redis", "MySQL");
            System.out.println("-----------------------------------------");
            for (int i = 0; i < redisTimes.size(); i++) {
                System.out.printf("%-10d | %-10d | %-10d%n", i + 1, redisTimes.get(i), mysqlTimes.get(i));
            }
        } finally {
            main.shutdown();
        }
    }

    private static String getEnv(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    public void shutdown() {
        if (sessionFactory != null) sessionFactory.close();
        if (redisClient != null) redisClient.shutdown();
    }
}