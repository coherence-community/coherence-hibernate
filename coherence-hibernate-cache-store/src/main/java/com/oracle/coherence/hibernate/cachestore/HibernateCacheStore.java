/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cachestore;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.tangosol.net.cache.CacheStore;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Data-driven CacheStore implementation for Hibernate tables.
 *
 * @author jp 2005.09.15
 * @author pp 2009.01.23
 * @author rs 2013.09.05
 * @author Gunnar Hillert
 */
public class HibernateCacheStore extends HibernateCacheLoader implements CacheStore {

    /**
     * Default constructor. If using this constructor, it is expected that
     * the {@code entityName} and {@code sessionFactory} attributes will
     * be set prior to usage.
     */
    public HibernateCacheStore() {
        super();
    }

    /**
     * Constructor which accepts an entityName.
     * @param entityName the Hibernate entity (the fully-qualified class name)
     */
    public HibernateCacheStore(String entityName) {
        super(entityName);
    }

    /**
     * Constructor which accepts an entityName and a hibernate configuration
     * resource. The current implementation instantiates a SessionFactory per
     * instance (implying one instance per CacheStore-backed NamedCache).
     * @param sEntityName the Hibernate entity (i.e. the HQL table name)
     * @param sResource the Hibernate config classpath resource (e.g. hibernate.cfg.xml)
     */
    public HibernateCacheStore(String sEntityName, String sResource) {
        super(sEntityName, sResource);
    }

    /**
     * Constructor which accepts an entityName and a hibernate configuration
     * resource. The current implementation instantiates a SessionFactory per
     * instance (implying one instance per CacheStore-backed NamedCache).
     * @param sEntityName the Hibernate entity (i.e. the HQL table name)
     * @param configurationFile the Hibernate config file (e.g. hibernate.cfg.xml)
     */
    public HibernateCacheStore(String sEntityName, File configurationFile) {
        super(sEntityName, configurationFile);
    }

    /**
     * Constructor which accepts an entityName and a Hibernate
     * {@code SessionFactory}. This allows for external configuration
     * of the SessionFactory (for instance using Spring.)
     * @param sEntityName the Hibernate entity (i.e. the HQL table name)
     * @param sessionFactory the Hibernate SessionFactory
     */
    public HibernateCacheStore(String sEntityName, SessionFactory sessionFactory) {
        super(sEntityName, sessionFactory);
    }


    // ----- CacheStore API methods -----------------------------------------

    /**
     * Store a Hibernate entity given an id (key) and entity (value).
     * <p>
     * The entity must have an identifier attribute, and it must be either
     * null (undefined) or equal to the cache key.
     * @param key   the cache key; specifically, the entity id
     * @param value the cache value; specifically, the entity
     */
    public void store(Object key, Object value) {
        ensureInitialized();

        Transaction tx = null;

        final Session session = openSession();

        try {
            tx = session.beginTransaction();

            validateIdentifier((Serializable) key, value, (SessionImplementor) session);

            // Save or Update (since we don't know if this is an insert or an
            // update)
            session.merge(getEntityName(), value);

            tx.commit();
        }
        catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }

            throw ensureRuntimeException(ex);
        }
        finally {
            closeSession(session);
        }
    }

    /**
     * Store a collection of Hibernate entities given a Map of ids (keys) and
     * entities (values).
     * @param entries   a mapping of ids (keys) to entities (values)
     */
    public void storeAll(Map entries) {
        ensureInitialized();

        Transaction tx = null;

        final Session session = openSession();

        try {
            tx = session.beginTransaction();

            // We just iterate through the incoming set and individually
            // save each one. Note that this is still part of a single
            // Hibernate transaction so it may batch them.
            for (Iterator iter = entries.entrySet().iterator(); iter.hasNext(); ) {
                final Map.Entry entry = (Map.Entry) iter.next();
                final Serializable id = (Serializable) entry.getKey();
                final Object entity = entry.getValue();
                validateIdentifier(id, entity, (SessionImplementor) session);
                session.merge(entity);
            }

            tx.commit();
        }
        catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ensureRuntimeException(ex);
        }
        finally {
            closeSession(session);
        }
    }

    /**
     * Erase a Hibernate entity given an id (key).
     * @param key   the cache key; specifically, the entity id
     */
    public void erase(Object key) {
        ensureInitialized();

        Transaction tx = null;

        final Session session = openSession();

        try {
            tx = session.beginTransaction();

            // Hibernate deletes objects ... it has no "delete by key".
            // So we need to load the objects before we delete them.
            // We may be able to use an HQL delete instead.
            final Object entity = createEntityFromId(key, (SessionImplementor) session);
            if (entity != null) {
                session.remove(entity);
            }

            tx.commit();
        }
        catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ensureRuntimeException(ex);
        }
        finally {
            closeSession(session);
        }
    }

    /**
     * Erase a set of Hibernate entities given an collection of ids (keys).
     * @param keys  the cache keys; specifically, the entity ids
     */
    public void eraseAll(Collection keys) {
        ensureInitialized();

        Transaction tx = null;

        final Session session = openSession();

        try {
            tx = session.beginTransaction();

            // We just iterate through the incoming set and individually
            // delete each one. Note that this is still part of a single
            // Hibernate transaction so it may batch them.
            for (Iterator iter = keys.iterator(); iter.hasNext();) {
                final Object key = iter.next();
                final Object entity = createEntityFromId(key, (SessionImplementor) session);
                if (entity != null) {
                    session.remove(entity);
                }
            }

            tx.commit();
        }
        catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ensureRuntimeException(ex);
        }
        finally {
            closeSession(session);
        }
    }
}
