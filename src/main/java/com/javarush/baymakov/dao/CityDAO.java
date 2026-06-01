package com.javarush.baymakov.dao;

import com.javarush.baymakov.domain.City;
import org.hibernate.SessionFactory;

import java.util.List;

public class CityDAO {
    private final SessionFactory sessionFactory;

    public CityDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<City> getItems(int offset, int limit) {
        return sessionFactory.getCurrentSession().createQuery("select c from City c", City.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .list();
    }

    public int getTotalCount() {
        return Math.toIntExact(sessionFactory.getCurrentSession().createQuery("select count(c) from City c", Long.class).uniqueResult());
    }
}
