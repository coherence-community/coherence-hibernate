/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cachestore;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tangosol.net.cache.CacheLoader;
import com.tangosol.util.Base;
import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;

/**
 * Data-driven CacheLoader implementation for Hibernate tables.
 * <p>
 * These methods all follow the pattern of:
 * <ol>
 *   <li>open session</li>
 *   <li>begin transaction</li>
 *   <li>do work</li>
 *   <li>commit transaction (or rollback on exception and rethrow)</li>
 *   <li>close session</li>
 * </ol>
 *
 * @author jp 2005.09.15
 * @author pp 2009.01.23
 * @author rs 2013.09.05
 * @author Gunnar Hillert
 */
public class HibernateCacheLoader extends Base implements CacheLoader {

    /**
     * Name of the "ids" named parameter in HQL bulk queries.
     */
    protected static final String PARAM_IDS = "ids";

    /**
     * Has this instance been initialized?
     */
    private boolean initialized;

    /**
     * ClassMetadata object for this CacheLoader's entity type (has pseudo-final semantics, and is guarded by
     * ensureInitialized()).
     */
    private EntityPersister entityClassMetadata;

    /**
     * An HQL query string for loadAll (has pseudo-final semantics, and is guarded by ensureInitialized()).
     */
    private volatile String loadAllQuery;

    /**
     * The entity name.
     */
    private volatile String entityName;

    /**
     * The Hibernate SessionFactory (the instance's copy).
     */
    private SessionFactory sessionFactory;

    /**
     * Default constructor. If using this constructor, it is expected that
     * the {@code entityName} and {@code sessionFactory} attributes will
     * be set prior to usage.
     */
    public HibernateCacheLoader() {
    }

    /**
     * Constructor which accepts an entityName. Configures Hibernate using
     * the default Hibernate configuration. The current implementation parses
     * this file once-per-instance (there is typically a single instance per).
     * @param entityName the Hibernate entity (i.e., the HQL table name)
     */
    public HibernateCacheLoader(String entityName) {
        this.entityName = entityName;

        // Configure using the default Hibernate configuration.
        final Configuration configuration = new Configuration();
        configuration.configure();

        this.sessionFactory = configuration.buildSessionFactory();
    }

    /**
     * Constructor which accepts an entityName and a Hibernate configuration
     * resource. The current implementation instantiates a SessionFactory per
     * instance (implying one instance per CacheStore-backed NamedCache).
     * @param entityName the Hibernate entity (i.e. the HQL table name)
     * @param resource the Hibernate config classpath resource (e.g. hibernate.cfg.xml)
     */
    public HibernateCacheLoader(String entityName, String resource) {
        this.entityName = entityName;

        /*
        If we start caching these we need to be aware that the resource may
        be relative (and so we should not key the cache by resource name).
        */
        final Configuration configuration = new Configuration();
        configuration.configure(resource);

        this.sessionFactory = configuration.buildSessionFactory();
    }

    /**
     * Constructor which accepts an entityName and a Hibernate configuration
     * resource. The current implementation instantiates a SessionFactory per
     * instance (implying one instance per CacheStore-backed NamedCache).
     * @param entityName the Hibernate entity (i.e. the HQL table name)
     * @param configurationFile the Hibernate config file (e.g. hibernate.cfg.xml)
     */
    public HibernateCacheLoader(String entityName, File configurationFile) {
        this.entityName = entityName;

        /*
        If we start caching these we should cache by canonical file name.
        */
        final Configuration configuration = new Configuration();
        configuration.configure(configurationFile);

        this.sessionFactory = configuration.buildSessionFactory();
    }

    /**
     * Constructor which accepts an entityName and a Hibernate {@code SessionFactory}.
     * This allows for external configuration of the SessionFactory (for instance using Spring).
     * @param entityName the Hibernate entity (i.e. the HQL table name)
     * @param sessionFactory the Hibernate SessionFactory
     */
    public HibernateCacheLoader(String entityName, SessionFactory sessionFactory) {
        this.entityName = entityName;
        this.sessionFactory = sessionFactory;
    }


    // ----- accessors -----------------------------------------------------

    /**
     * Get the Hibernate SessionFactory.
     * @return the Hibernate SessionFactory
     */
    public synchronized SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    /**
     * Set the Hibernate SessionFactory to be used by this CacheLoader.  This
     * attribute can only be set once during the lifecycle of an instance.
     * @param sessionFactory the Hibernate SessionFactory
     * @throws IllegalStateException  if the session factory has already been set
     */
    public synchronized void setSessionFactory(SessionFactory sessionFactory) {
        if (this.sessionFactory != null) {
            throw new IllegalStateException("SessionFactory has already been set");
        }
        this.sessionFactory = sessionFactory;
    }

    /**
     * Get the Hibernate entity name.
     * @return the entity name
     */
    protected synchronized String getEntityName() {
        return this.entityName;
    }

    /**
     * Set the Hibernate entity name. This attribute can only be set once
     * during the lifecycle of an instance.
     * @param entityName the entity name
     * @throws IllegalStateException if the entity name has already been set
     */
    public synchronized void setEntityName(String entityName) {
        if (this.entityName != null) {
            throw new IllegalStateException("Entity name has already been set");
        }
        this.entityName = entityName;
    }

    // ----- Initialization methods ----------------------------------------

    /**
     * Initializer (must be called post-constructor).
     * <p>
     * We do this specifically so that derived classes can safely create
     * override methods that depend on a fully constructed object state.
     * Will only be called once per instance and prior to the main body
     * of any API methods. This should not be directly called by derived
     * classes. If this method is overridden, super must be called at the
     * end of the overriding method.
     */
    protected void initialize() {

        if (this.entityName == null) {
            throw new IllegalStateException("Entity name attribute was not set");
        }

        final SessionFactory sessionFactory = getSessionFactory();
        if (sessionFactory == null) {
            // Can only occur with derived classes
            throw new IllegalStateException("No session factory was specified, " +
                    "and a hibernate configuration file was not provided.");
        }

        // Look up the Hibernate metadata for the entity
        final MappingMetamodel metamodel = (MappingMetamodel) sessionFactory.getMetamodel();
        try {
            final EntityPersister entityPersister = metamodel.getEntityDescriptor(this.entityName);
            setEntityClassMetadata(entityPersister);
        }
        catch (UnknownEntityTypeException ex) {
            throw new RuntimeException("Unable to find ClassMetadata" +
                    " for Hibernate entity " + this.entityName + ".");
        }
    }

    /**
     * Called by all API-implementing methods for lazy initialization. This
     * should never be called from a constructor.
     */
    protected synchronized void ensureInitialized() {
        if (!this.initialized) {
            initialize();
            this.initialized = true;
        }
    }


    // ----- CacheLoader API methods ----------------------------------------

    /**
     * Load a Hibernate entity given an id (key).
     * @param key the cache key; specifically, the entity id
     * @return the corresponding Hibernate entity instance
     */
    public Object load(Object key) {
        ensureInitialized();

        Transaction transaction = null;

        Object value = null;

        final Session session = openSession();

        try {
            transaction = session.beginTransaction();

            // The Hibernate docs indicate that the returned value is
            // sufficiently "detached" for our purposes (without explicitly
            // converting the state to transient).
            value = session.get(getEntityName(), (Serializable) key);

            transaction.commit();
        }
        catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }

            throw ensureRuntimeException(ex);
        }
        finally {
            closeSession(session);
        }

        return value;
    }

    /**
     * Load a collection of Hibernate entities given a set of ids (keys).
     * @param keys the cache keys; specifically, the entity ids. By default, entities will be returned in the order of the
     *              provided List of keys
     * @return the corresponding Hibernate entity instances
     */
    public Map loadAll(List keys) {
        ensureInitialized();

        final Map results = new HashMap();

        Transaction transaction = null;

        final Session session = openSession();
        final SessionImplementor sessionImplementor = (SessionImplementor) session;

        try {
            transaction = session.beginTransaction();

            final List<?> result;
            if (this.getLoadAllQuery() != null) {
                // Create the query
                final String sQuery = getLoadAllQuery();
                final Query query = session.createQuery(sQuery);

                // Prevent Hibernate from caching the results
                query.setCacheMode(CacheMode.IGNORE);
                query.setCacheable(false);
                query.setReadOnly(true);

                // Parameterize the query (where :keys = keys)
                query.setParameterList(PARAM_IDS, keys);
                result = query.list();
            }
            else {
                result = session.byMultipleIds(this.entityName)
                        .with(CacheMode.IGNORE)
                        .multiLoad(keys);
            }

            // Need a way to extract the key from an entity that we know
            // nothing about.
            final EntityPersister entityPersister = getEntityClassMetadata();

            for (Object entity : result) {
                final Object[] propertyValues = entityPersister.getValues(entity);
                for (Object propertyValue : propertyValues) {
                    Hibernate.initialize(propertyValue);
                }
            }

            // Iterate through the results and place into the return map
            for (Object entity : result) {
                final Object id = entityPersister.getIdentifier(entity, sessionImplementor);
                results.put(id, entity);
            }
            transaction.commit();
        }
        catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw ensureRuntimeException(ex);
        }
        finally {
            closeSession(session);
        }

        return results;
    }

    // ----- Helper methods -------------------------------------------------

    /**
     * Open a Hibernate Session.
     * @return the Hibernate Session object
     */
    protected Session openSession() {
        return getSessionFactory().openSession();
    }

    /**
     * Close a Hibernate Session.
     * @param session the Hibernate Session object
     */
    protected void closeSession(Session session) {
        azzert(session != null, "Attempted to close a null session.");
        session.close();
    }

    /**
     * Get the Hibernate ClassMetadata for the Hibernate entity.
     * @return the ClassMetadata object
     */
    protected EntityPersister getEntityClassMetadata() {
        return this.entityClassMetadata;
    }

    /**
     * Get the Hibernate EntityPersister for the Hibernate entity.
     * @param entityPersister the EntityPersister object
     */
    protected void setEntityClassMetadata(EntityPersister entityPersister) {
        this.entityClassMetadata = entityPersister;
    }

    /**
     * Get the parameterized loadAll HQL query string.
     * @return  a parameterized HQL query string
     */
    protected String getLoadAllQuery() {
        return this.loadAllQuery;
    }

    /**
     * Get the parameterized loadAll HQL query string.
     * @param sLoadAllQuery a parameterized HQL query string
     */
    protected void setLoadAllQuery(String sLoadAllQuery) {
        this.loadAllQuery = sLoadAllQuery;
    }

    /**
     * Create a transient entity instance given an entity id.
     * @param id the Hibernate entity id
     * @param sessionImplementor the Hibernate SessionImplementor
     * @return the Hibernate entity (may return null)
     */
    protected Object createEntityFromId(Object id, SharedSessionContractImplementor sessionImplementor) {
        final EntityPersister cmd = getEntityClassMetadata();
        final Object o = cmd.instantiate(id, sessionImplementor);
        return o;
    }

    /**
     * Ensure that there are no conflicts between an explicit and implicit key.
     * @param id the explicit key
     * @param entity an entity (containing an implicit key)
     * @param sessionImplementor the Hibernate SessionImplementor
     */
    protected void validateIdentifier(Serializable id, Object entity, SharedSessionContractImplementor sessionImplementor) {
        final EntityPersister classMetaData = getEntityClassMetadata();

        final Object intrinsicIdentifier =
                classMetaData.getIdentifier(entity, sessionImplementor);

        if (intrinsicIdentifier == null) {
            classMetaData
                    .setIdentifier(entity, id, sessionImplementor);
        }
        else {
            if (!intrinsicIdentifier.equals(id)) {
                throw new IllegalArgumentException("Conflicting identifier " +
                        "information between entity " + entity + " and id " + id);
            }
        }
    }
}
