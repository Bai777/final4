package com.javarush.baymakov.service;

import com.javarush.baymakov.dao.CityDAO;
import com.javarush.baymakov.domain.City;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class DataValidationService {
    private final SessionFactory sessionFactory;
    private final CityDAO cityDAO;

    public DataValidationService(SessionFactory sessionFactory, CityDAO cityDAO) {
        this.sessionFactory = sessionFactory;
        this.cityDAO = cityDAO;
    }

    public void testMysqlData(List<Integer> ids) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : ids) {
                City city = cityDAO.getById(id);
                city.getCountry().getLanguages().size();
            }
            session.getTransaction().commit();
        }
    }
}
