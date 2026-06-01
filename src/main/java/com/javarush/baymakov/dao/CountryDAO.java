package com.javarush.baymakov.dao;

import com.javarush.baymakov.domain.Country;
import org.hibernate.SessionFactory;

import java.util.List;

public class CountryDAO {
    private final SessionFactory sessionFactory;

    public CountryDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<Country> getAll() {
        return sessionFactory.getCurrentSession().createQuery("select c from Country c", Country.class).list();
    }
}
