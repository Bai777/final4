package com.javarush.baymakov.dao;

import com.javarush.baymakov.domain.Country;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class CountryDAO {
    private final SessionFactory sessionFactory;

    public CountryDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<Country> getAll() {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            List<Country> countries = session
                    .createQuery("select c from Country c join fetch c.languages", Country.class)
                    .list();
            session.getTransaction().commit();
            return countries;
        }
    }
}
