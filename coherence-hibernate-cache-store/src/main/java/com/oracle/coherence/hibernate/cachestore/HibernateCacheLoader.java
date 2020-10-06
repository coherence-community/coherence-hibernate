/*
 * File: HibernateCacheLoader.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.hibernate.cachestore;

import com.tangosol.net.cache.CacheLoader;
import com.tangosol.util.Base;
import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metadata.ClassMetadata;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Data-driven CacheLoader implementation for Hibernate tables
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
 */
public class HibernateCacheLoader
        extends Base
        implements CacheLoader
{
    // ----- Constructor(s) -------------------------------------------------

    /**
     * Default constructor.  If using this constructor, it is expected that
     * the {@code entityName} and {@code sessionFactory} attributes will
     * be set prior to usage.
     */
    public HibernateCacheLoader()
    {
    }

    /**
     * Constructor which accepts an entityName. Configures Hibernate using
     * the default Hibernate configuration. The current implementation parses
     * this file once-per-instance (there is typically a single instance per).
     *
     * @param sEntityName    the Hibernate entity (i.e., the HQL table name)
     */
    public HibernateCacheLoader(String sEntityName)
    {
        m_sEntityName = sEntityName;

        // Configure using the default Hibernate configuration.
        Configuration configuration = new Configuration();
        configuration.configure();

        m_sessionFactory = configuration.buildSessionFactory();
    }

    /**
     * Constructor which accepts an entityName and a Hibernate configuration
     * resource. The current implementation instantiates a SessionFactory per
     * instance (implying one instance per CacheStore-backed NamedCache).
     *
     * @param sEntityName   Hibernate entity (i.e. the HQL table name)
     * @param sResource     Hibernate config classpath resource (e.g. hibernate.cfg.xml)
     */
    public HibernateCacheLoader(String sEntityName, String sResource)
    {
        m_sEntityName = sEntityName;

        /*
        If we start caching these we need to be aware that the resource may
        be relative (and so we should not key the cache by resource name).
        */
        Configuration configuration = new Configuration();
        configuration.configure(sResource);

        m_sessionFactory = configuration.buildSessionFactory();
    }

    /**
     * Constructor which accepts an entityName and a Hibernate configuration
     * resource. The current implementation instantiates a SessionFactory per
     * instance (implying one instance per CacheStore-backed NamedCache).
     *
     * @param sEntityName       Hibernate entity (i.e. the HQL table name)
     * @param configurationFile Hibernate config file (e.g. hibernate.cfg.xml)
     */
    public HibernateCacheLoader(String sEntityName, File configurationFile)
    {
        m_sEntityName = sEntityName;

        /*
        If we start caching these we should cache by canonical file name.
        */
        Configuration configuration = new Configuration();
        configuration.configure(configurationFile);

        m_sessionFactory = configuration.buildSessionFactory();
    }

    /**
     * Constructor which accepts an entityName and a Hibernate
     * {@code SessionFactory}.  This allows for external configuration
     * of the SessionFactory (for instance using Spring.)
     *
     * @param sEntityName       Hibernate entity (i.e. the HQL table name)
     * @param sessionFactory    Hibernate SessionFactory
     */
    public HibernateCacheLoader(String sEntityName,
                                SessionFactory sessionFactory)
    {
        m_sEntityName    = sEntityName;
        m_sessionFactory = sessionFactory;
    }


    // ----- accessors -----------------------------------------------------

    /**
     * Get the Hibernate SessionFactory.
     *
     * @return  the Hibernate SessionFactory
     */
    public SessionFactory getSessionFactory()
    {
        return m_sessionFactory;
    }

    /**
     * Set the Hibernate SessionFactory to be used by this CacheLoader.  This
     * attribute can only be set once during the lifecycle of an instance.
     *
     * @param sessionFactory the Hibernate SessionFactory
     *
     * @throws IllegalStateException  if the session factory has already
     *                                been set
     */
    public synchronized void setSessionFactory(SessionFactory sessionFactory)
    {
        if (m_sessionFactory != null)
        {
            throw new IllegalStateException("SessionFactory has already been set");
        }
        m_sessionFactory = sessionFactory;
    }

    /**
     * Get the Hibernate entity name
     *
     * @return the entity name
     */
    protected String getEntityName()
    {
        return m_sEntityName;
    }

    /**
     * Set the Hibernate entity name.  This attribute can only be set once
     * during the lifecycle of an instance.
     *
     * @param sEntityName the entity name
     *
     * @throws IllegalStateException  if the entity name has already been set
     */
    public synchronized void setEntityName(String sEntityName)
    {
        if (m_sEntityName != null)
        {
            throw new IllegalStateException("Entity name has already been set");
        }
        m_sEntityName = sEntityName;
    }


    // ----- Initialization methods ----------------------------------------

    /**
     * Initializer (must be called post-constructor)
     * <p>
     * We do this specifically so that derived classes can safely create
     * override methods that depend on a fully constructed object state.
     * Will only be called once per instance and prior to the main body
     * of any API methods. This should not be directly called by derived
     * classes. If this method is overridden, super must be called at the
     * end of the overriding method.
     */
    protected void initialize()
    {
        String sEntityName = m_sEntityName;

        if (sEntityName == null)
        {
            throw new IllegalStateException("Entity name attribute was not set");
        }

        SessionFactory sessionFactory = getSessionFactory();
        if (sessionFactory == null)
        {
            // Can only occur with derived classes
            throw new IllegalStateException("No session factory was specified, " +
                    "and a hibernate configuration file was not provided.");
        }

        // Look up the Hibernate metadata for the entity
        ClassMetadata entityClassMetadata =
                sessionFactory.getClassMetadata(sEntityName);
        if (entityClassMetadata == null)
        {
            throw new RuntimeException("Unable to find ClassMetadata" +
                    " for Hibernate entity " + sEntityName + ".");
        }
        setEntityClassMetadata(entityClassMetadata);

        // Create the loadAll query (it requires an identifier property).
        // Use "fetch all properties" to force eager loading.
        String sIdName;
        if (entityClassMetadata.hasIdentifierProperty())
        {
            sIdName = entityClassMetadata.getIdentifierPropertyName();
            azzert(sIdName != null);
        }
        else
        {
            throw new RuntimeException("Hibernate entity " + sEntityName +
                    " does not have an ID column associated with it.");
        }

        String sLoadAllQuery = "from " + sEntityName + " fetch all properties where "
                + sIdName + " in (:" + PARAM_IDS + ") ";
        setLoadAllQuery(sLoadAllQuery);
    }

    /**
     * Called by all API-implementing methods for lazy initialization. This
     * should never be called from a constructor.
     */
    protected synchronized void ensureInitialized()
    {
        if (!m_fInitialized)
        {
            initialize();
            m_fInitialized = true;
        }
    }


    // ----- CacheLoader API methods ----------------------------------------

    /**
     * Load a Hibernate entity given an id (key)
     *
     * @param key   the cache key; specifically, the entity id
     *
     * @return      the corresponding Hibernate entity instance
     */
    public Object load(Object key)
    {
        ensureInitialized();

        Transaction tx = null;

        Object value = null;

        Session session = openSession();

        try
        {
            tx = session.beginTransaction();

            // The Hibernate docs indicate that the returned value is
            // sufficiently "detached" for our purposes (without explicitly
            // converting the state to transient).
            value = session.get(getEntityName(), (Serializable)key);

            tx.commit();
        }
        catch (Exception e)
        {
            if (tx != null)
            {
                tx.rollback();
            }

            throw ensureRuntimeException(e);
        }
        finally
        {
            closeSession(session);
        }

        return value;
    }

    /**
     * Load a collection of Hibernate entities given a set of ids (keys)
     *
     * @param keys  the cache keys; specifically, the entity ids
     *
     * @return      the corresponding Hibernate entity instances
     */
    public Map loadAll(Collection keys)
    {
        ensureInitialized();

        Map results = new HashMap();

        Transaction tx = null;

        Session session = openSession();
        SessionImplementor sessionImplementor = (SessionImplementor) session;

        try
        {
            tx = session.beginTransaction();

            // Create the query
            String sQuery = getLoadAllQuery();
            Query query = session.createQuery(sQuery);

            // Prevent Hibernate from caching the results
            query.setCacheMode(CacheMode.IGNORE);
            query.setCacheable(false);
            query.setReadOnly(true);

            // Parameterize the query (where :keys = keys)
            query.setParameterList(PARAM_IDS, keys);

            // Need a way to extract the key from an entity that we know
            // nothing about.
            ClassMetadata classMetaData = getEntityClassMetadata();

            // Iterate through the results and place into the return map
            for (Iterator iter = query.list().iterator(); iter.hasNext(); )
            {
                Object entity = iter.next();
                Object id = classMetaData.getIdentifier(entity, sessionImplementor);
                results.put(id, entity);
            }

            tx.commit();
        }
        catch (Exception e)
        {
            if (tx != null)
            {
                tx.rollback();
            }

            throw ensureRuntimeException(e);
        }
        finally
        {
            closeSession(session);
        }

        return results;
    }


    // ----- Helper methods -------------------------------------------------

    /**
     * Open a Hibernate Session.
     *
     * @return  the Hibernate Session object
     */
    protected Session openSession()
    {
        Session session = getSessionFactory().openSession();
        return session;
    }

    /**
     * Close a Hibernate Session.
     *
     * @param session   the Hibernate Session object
     */
    protected void closeSession(Session session)
    {
        azzert(session != null, "Attempted to close a null session.");
        session.close();
    }

    /**
     * Get the Hibernate ClassMetadata for the Hibernate entity
     *
     * @return  the ClassMetadata object
     */
    protected ClassMetadata getEntityClassMetadata()
    {
        return m_entityClassMetadata;
    }

    /**
     * Get the Hibernate ClassMetadata for the Hibernate entity
     *
     * @param   entityClassMetadata     the ClassMetadata object
     */
    protected void setEntityClassMetadata(ClassMetadata entityClassMetadata)
    {
        m_entityClassMetadata = entityClassMetadata;
    }

    /**
     * Get the parameterized loadAll HQL query string
     *
     * @return  a parameterized HQL query string
     */
    protected String getLoadAllQuery()
    {
        return m_sLoadAllQuery;
    }

    /**
     * Get the parameterized loadAll HQL query string
     *
     * @param   sLoadAllQuery   a parameterized HQL query string
     */
    protected void setLoadAllQuery(String sLoadAllQuery)
    {
        m_sLoadAllQuery = sLoadAllQuery;
    }

    /**
     * Create a transient entity instance given an entity id
     *
     * @param id the Hibernate entity id
     * @param sessionImplementor the Hibernate SessionImplementor
     *
     * @return the Hibernate entity (may return null)
     */
    protected Object createEntityFromId(Object id, SessionImplementor sessionImplementor)
    {
        ClassMetadata cmd = getEntityClassMetadata();
        Object o = cmd.instantiate((Serializable)id, sessionImplementor);
        return o;
    }

    /**
     * Ensure that there are no conflicts between an explicit and implicit key.
     *
     * @param id     the explicit key
     * @param entity an entity (containing an implicit key)
     * @param sessionImplementor the Hibernate SessionImplementor
     */
    protected void validateIdentifier(Serializable id, Object entity, SessionImplementor sessionImplementor)
    {
        ClassMetadata classMetaData = getEntityClassMetadata();

        Serializable intrinsicIdentifier =
                classMetaData.getIdentifier(entity, sessionImplementor);

        if (intrinsicIdentifier == null)
        {
            classMetaData
                    .setIdentifier(entity, (Serializable)id, sessionImplementor);
        }
        else
        {
            if (!intrinsicIdentifier.equals(id))
            {
                throw new IllegalArgumentException("Conflicting identifier " +
                        "information between entity " + entity + " and id " + id);
            }
        }
    }


    // ----- constants ------------------------------------------------------

    /**
     * Name of the "ids" named parameter in HQL bulk queries
     */
    protected static final String PARAM_IDS = "ids";


    // ----- private fields --------------------------------------------------

    /**
     * Has this instance been initialized?
     */
    private boolean m_fInitialized;

    /**
     * ClassMetadata object for this CacheLoader's entity type
     * (has pseudo-final semantics, and is guarded by ensureInitialized())
     */
    private volatile ClassMetadata m_entityClassMetadata;

    /**
     * An HQL query string for loadAll
     * (has pseudo-final semantics, and is guarded by ensureInitialized())
     */
    private volatile String m_sLoadAllQuery;

    /**
     * The entity name
     */
    private volatile String m_sEntityName;

    /**
     * The Hibernate SessionFactory (the instance's copy)
     */
    private volatile SessionFactory m_sessionFactory;
}
