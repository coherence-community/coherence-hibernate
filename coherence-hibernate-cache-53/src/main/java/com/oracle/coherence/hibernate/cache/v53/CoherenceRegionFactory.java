/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53;

import com.oracle.coherence.hibernate.cache.v53.access.CoherenceDomainDataRegionImpl;
import com.oracle.coherence.hibernate.cache.v53.access.CoherenceStorageAccessImpl;
import com.oracle.coherence.hibernate.cache.v53.configuration.support.CoherenceHibernateSystemPropertyResolver;
import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegion;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;
import org.hibernate.cache.spi.support.StorageAccess;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A CoherenceRegionFactory is a factory for regions of Hibernate second-level cache implemented with Oracle Coherence.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceRegionFactory extends RegionFactoryTemplate
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CoherenceRegionFactory.class);

    // ---- Constants

    private static final long serialVersionUID = -8434943540794407358L;

    /**
     * The prefix of the names of all properties specific to this SPI implementation.
     */
    public static final String PROPERTY_NAME_PREFIX = "com.oracle.coherence.hibernate.cache.";

    /**
     * The name of the property specifying the path to the Coherence cache configuration file.
     */
    public static final String CACHE_CONFIG_FILE_PATH_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "cache_config_file_path";

    /**
     * The name of the property specifying the Coherence-specific logger. This will set the logger of the Coherence
     * sub-system.
     */
    public static final String COHERENCE_LOGGER_PROPERTY_NAME = "coherence.log";

    public static final String DEFAULT_COHERENCE_LOGGER = "slf4j";

    /**
     * The name of the property specifying whether to dump stack on debug messages.
     */
    public static final String DUMP_STACK_ON_DEBUG_MESSAGE_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "dump_stack_on_debug_message";

    /**
     * The default path to the cache configuration file.
     */
    protected static final String DEFAULT_CACHE_CONFIG_FILE_PATH = "hibernate-second-level-cache-config.xml";

    protected CoherenceHibernateSystemPropertyResolver systemPropertyResolver;

    // ---- Fields

    /**
     * The ConfigurableCacheFactory used by this CoherenceRegionFactory.
     */
    private ConfigurableCacheFactory cacheFactory;

    /**
     * The Hibernate settings object; may contain user-supplied "minimal puts" setting.
     */
    private SessionFactoryOptions sessionFactoryOptions;

    /**
     * The Hibernate {@link CacheKeysFactory} to use. Hibernate ships with 2 {@link CacheKeysFactory}
     * implementations:
     *
     * <ul>
     *   <li>{{@link org.hibernate.cache.internal.DefaultCacheKeysFactory}}
     *   <li>{@link org.hibernate.cache.internal.SimpleCacheKeysFactory}
     * </ul>
     *
     * If none is specified, then the {@link org.hibernate.cache.internal.DefaultCacheKeysFactory} is used.
     */
    private CacheKeysFactory cacheKeysFactory;

    // ---- Accessing

    @Override
    protected CacheKeysFactory getImplicitCacheKeysFactory() {
        return this.cacheKeysFactory;
    }

    /**
     * Returns the ConfigurableCacheFactory used by this CoherenceRegionFactory.
     *
     * @return the ConfigurableCacheFactory used by this CoherenceRegionFactory
     */
    protected ConfigurableCacheFactory getConfigurableCacheFactory()
    {
        return this.cacheFactory;
    }

    /**
     * Sets the ConfigurableCacheFactory used by this CoherenceRegionFactory.
     *
     * @param cacheFactory the ConfigurableCacheFactory used by this CoherenceRegionFactory
     */
    protected void setConfigurableCacheFactory(ConfigurableCacheFactory cacheFactory)
    {
        this.cacheFactory = cacheFactory;
    }


    // ---- interface java.lang.Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder(getClass().getName());
        stringBuilder.append("(");
        stringBuilder.append("cacheFactory=").append(cacheFactory);
        stringBuilder.append(", sessionFactoryOptions=").append(sessionFactoryOptions);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    // ---- interface org.hibernate.cache.spi.RegionFactory

    @SuppressWarnings("rawtypes")
    @Override
    protected void prepareForUse(SessionFactoryOptions settings, Map configValues)
    {
        this.sessionFactoryOptions = settings;
        this.systemPropertyResolver = new CoherenceHibernateSystemPropertyResolver(configValues); //TODO

        if (this.systemPropertyResolver.getProperty(COHERENCE_LOGGER_PROPERTY_NAME) == null) {
            this.systemPropertyResolver.addCoherenceProperty(COHERENCE_LOGGER_PROPERTY_NAME, DEFAULT_COHERENCE_LOGGER);
        }

        if (this.sessionFactoryOptions != null)
        {
            StrategySelector selector = this.sessionFactoryOptions.getServiceRegistry().getService(StrategySelector.class);
            this.cacheKeysFactory = selector.resolveDefaultableStrategy(CacheKeysFactory.class,
                    configValues.get(Environment.CACHE_KEYS_FACTORY), new DefaultCacheKeysFactory());
        }
        else
        {
            this.cacheKeysFactory = new DefaultCacheKeysFactory();
        }

        CacheFactory.ensureCluster();

        String cacheConfigFilePath = (configValues == null) ?
                null :
                (String) configValues.get(CACHE_CONFIG_FILE_PATH_PROPERTY_NAME);
        if (cacheConfigFilePath == null)
        {
            cacheConfigFilePath = System.getProperty(
                    CACHE_CONFIG_FILE_PATH_PROPERTY_NAME,
                    DEFAULT_CACHE_CONFIG_FILE_PATH);
        }

        final ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(
                cacheConfigFilePath,
                getClass().getClassLoader());
        setConfigurableCacheFactory(factory);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("start({}, {})", settings, configValues);
        }
    }

    @Override
    protected void releaseFromUse() {
        CacheFactory.getCacheFactoryBuilder().release(getConfigurableCacheFactory());
        setConfigurableCacheFactory(null);
    }

    /**
     * {@inheritDoc}
     *
     * see also https://stackoverflow.com/a/12389310/835934
     */
    @Override
    public boolean isMinimalPutsEnabledByDefault()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessType getDefaultAccessType()
    {
        return AccessType.READ_WRITE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextTimestamp()
    {
        return CacheFactory.ensureCluster().getTimeMillis();
    }

    // ---- Internal

    /**
     * Ensure the initialization of a NamedCache of the argument name.
     *
     * @param cacheName the name of the NamedCache whose initialization to ensure
     *
     * @return a NamedCache for the argument name
     */
    protected NamedCache<?, ?> ensureNamedCache(String cacheName)
    {
        // We intentionally avoid passing in a ClassLoader (as we can't do any
        // more than the default Coherence codepath does); however, this
        // method is provided so that customers may easily subclass.
        return getConfigurableCacheFactory().ensureCache(cacheName, null);
    }

    @Override
    protected DomainDataStorageAccess createDomainDataStorageAccess(DomainDataRegionConfig regionConfig,
        DomainDataRegionBuildingContext buildingContext)
    {
        return new CoherenceStorageAccessImpl(
            this.createCoherenceRegion(regionConfig.getRegionName(), buildingContext.getSessionFactory())
        );
    }

    @Override
    protected StorageAccess createTimestampsRegionStorageAccess(
            String regionName,
            SessionFactoryImplementor sessionFactory) {
        return new CoherenceStorageAccessImpl(this.createCoherenceRegion(regionName, sessionFactory));
    }

    @Override
    protected StorageAccess createQueryResultsRegionStorageAccess(String regionName, SessionFactoryImplementor sessionFactory)
    {
        return new CoherenceStorageAccessImpl(this.createCoherenceRegion(regionName, sessionFactory));
    }

    protected CoherenceRegion createCoherenceRegion(final String unqualifiedRegionName,
                                                    final SessionFactoryImplementor sessionFactory)
    {
        return new CoherenceRegion(this, this.ensureNamedCache(unqualifiedRegionName), sessionFactory.getProperties());
    }

    @Override
    public DomainDataRegion buildDomainDataRegion(final DomainDataRegionConfig regionConfig,
                                                  final DomainDataRegionBuildingContext buildingContext)
    {
        return new CoherenceDomainDataRegionImpl(
          regionConfig,
          this,
          createDomainDataStorageAccess(regionConfig, buildingContext),
          cacheKeysFactory,
          buildingContext
        );
    }
}
