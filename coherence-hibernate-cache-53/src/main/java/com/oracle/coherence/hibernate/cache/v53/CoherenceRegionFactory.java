/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53;

import java.util.Map;

import com.oracle.coherence.hibernate.cache.v53.access.CoherenceDomainDataRegionImpl;
import com.oracle.coherence.hibernate.cache.v53.access.CoherenceStorageAccessImpl;
import com.oracle.coherence.hibernate.cache.v53.configuration.session.SessionType;
import com.oracle.coherence.hibernate.cache.v53.configuration.support.Assert;
import com.oracle.coherence.hibernate.cache.v53.configuration.support.CoherenceHibernateProperties;
import com.oracle.coherence.hibernate.cache.v53.configuration.support.CoherenceHibernateSystemPropertyResolver;
import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegion;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
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

/**
 * A CoherenceRegionFactory is a factory for regions of Hibernate second-level cache implemented with Oracle Coherence.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 * @since 2.1
 */
public class CoherenceRegionFactory extends RegionFactoryTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoherenceRegionFactory.class);

    private static final long serialVersionUID = -8434943540794407358L;

    protected transient CoherenceHibernateSystemPropertyResolver systemPropertyResolver;

    protected transient Session coherenceSession;

    private final boolean requiresShutDown;

    private transient DefaultCacheServer defaultCacheServer;

    private Cluster cluster = null;

    /**
     * Default constructor. Any Coherence instances created will implicitly require a shutdown of Coherence when
     * {@link #stop()} is called via {@link #releaseFromUse()}. This option will by default start Coherence as a
     * Cache client. This means that Coherence services are disabled by default (e.g. Local Storage). You can
     * start Coherence as a CacheServer and local storage will be enabled by default and all default services will be
     * started as well. Please provide property {@link CoherenceHibernateProperties#START_CACHE_SERVER_PROPERTY_NAME}
     */
    public CoherenceRegionFactory() {
        this.coherenceSession = null;
        this.requiresShutDown = true;
    }

    /**
     * Constructor that allows to pass-in an externally created Coherence {@link Session}. In this case the external
     * caller is responsible for any needed Coherence shut-downs when {@link #stop()} is called via {@link #releaseFromUse()}.
     * This means that call to {@link #stop()} will NOT result in a shutdown of Coherence; only the {@link Session} is
     * closed.
     *
     * @param coherenceSession must not be null
     */
    public CoherenceRegionFactory(Session coherenceSession) {
        Assert.notNull(coherenceSession, "The passed-in coherenceSession must not be null.");
        this.coherenceSession = coherenceSession;
        this.requiresShutDown = false;
    }

    /**
     * The Hibernate settings object; may contain user-supplied "minimal puts" setting.
     */
    private transient SessionFactoryOptions sessionFactoryOptions;

    /**
     * The Hibernate {@link CacheKeysFactory} to use. Hibernate ships with 2 {@link CacheKeysFactory}
     * implementations:
     *
     * <ul>
     *   <li>{{@link org.hibernate.cache.internal.DefaultCacheKeysFactory}}
     *   <li>{@link org.hibernate.cache.internal.SimpleCacheKeysFactory}
     * </ul>
     * <p>
     * If none is specified, then the {@link org.hibernate.cache.internal.DefaultCacheKeysFactory} is used.
     */
    private transient CacheKeysFactory cacheKeysFactory;

    @Override
    protected CacheKeysFactory getImplicitCacheKeysFactory() {
        return this.cacheKeysFactory;
    }

    /**
     * Returns the Coherence {@link Session} used by this {@link CoherenceRegionFactory}.
     * @return the Coherence {@link Session}
     */
    public Session getCoherenceSession() {
        return this.coherenceSession;
    }

    /**
     * Sets the Coherence {@link Session} used by this {@link CoherenceRegionFactory}.
     * @param coherenceSession the Coherence {@link Session} used by this CoherenceRegionFactory. May be null.
     */
    protected void setCoherenceSession(Session coherenceSession) {
        this.coherenceSession = coherenceSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + "(" +
                "coherenceSession=" + ((this.coherenceSession != null) ? this.coherenceSession.toString() : "N/A") +
                ", sessionFactoryOptions=" + this.sessionFactoryOptions +
                ")";
    }

    @Override
    protected void prepareForUse(SessionFactoryOptions settings, Map configValues) {
        this.sessionFactoryOptions = settings;

        final CoherenceHibernateProperties coherenceHibernateProperties = new CoherenceHibernateProperties(configValues);

        final Map<String, Object> coherenceProperties = coherenceHibernateProperties.getCoherenceProperties();
        this.systemPropertyResolver = new CoherenceHibernateSystemPropertyResolver(coherenceProperties);

        if (this.systemPropertyResolver.getProperty(CoherenceHibernateProperties.COHERENCE_LOGGER_PROPERTY_NAME) == null) {
            this.systemPropertyResolver.addCoherenceProperty(CoherenceHibernateProperties.COHERENCE_LOGGER_PROPERTY_NAME, CoherenceHibernateProperties.COHERENCE_LOGGER_DEFAULT_VALUE);
        }

        if (this.sessionFactoryOptions != null) {
            final StrategySelector selector = this.sessionFactoryOptions.getServiceRegistry().getService(StrategySelector.class);
            this.cacheKeysFactory = selector.resolveDefaultableStrategy(CacheKeysFactory.class,
                    configValues.get(Environment.CACHE_KEYS_FACTORY), new DefaultCacheKeysFactory());
        }
        else {
            this.cacheKeysFactory = new DefaultCacheKeysFactory();
        }

        this.systemPropertyResolver.initialize();

        prepareCoherenceSessionIfNeeded(coherenceHibernateProperties);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("start({}, {})", settings, configValues);
        }
    }

    private void prepareCoherenceSessionIfNeeded(CoherenceHibernateProperties coherenceHibernateProperties) {
        if (this.coherenceSession == null) {
            if (coherenceHibernateProperties.getSessionType() == null || SessionType.SERVER.equals(coherenceHibernateProperties.getSessionType())) {
                if (coherenceHibernateProperties.isStartCacheServer()) {
                    final ExtensibleConfigurableCacheFactory.Dependencies deps =
                            ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(coherenceHibernateProperties.getCacheConfigFilePath());

                    final ExtensibleConfigurableCacheFactory cacheFactory = new ExtensibleConfigurableCacheFactory(deps);
                    this.defaultCacheServer = new DefaultCacheServer(cacheFactory);
                    this.defaultCacheServer.startDaemon(5000);
                }

                CacheFactory.ensureCluster();

            }

            final SessionConfiguration.Builder sessionConfigBuild = SessionConfiguration.builder();
            if (coherenceHibernateProperties.getSessionName() != null) {
                sessionConfigBuild.named(coherenceHibernateProperties.getSessionName());
            }

            sessionConfigBuild.withConfigUri(coherenceHibernateProperties.getCacheConfigFilePath());
            sessionConfigBuild.withClassLoader(getClass().getClassLoader());

            final Session sessionToSet = Session.create(sessionConfigBuild.build()).get();  //TODO
            this.setCoherenceSession(sessionToSet);
        }
    }

    @Override
    protected void releaseFromUse() {
        if (this.getCoherenceSession() != null) {
            try {
                this.coherenceSession.close();
            }
            catch (Exception ex) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Unable to close session '{}'.", this.coherenceSession, ex);
                }
            }
        }

        if (this.requiresShutDown) {
            CacheFactory.getCluster().shutdown();
            CacheFactory.shutdown();

            if (this.defaultCacheServer != null) {
                this.defaultCacheServer.shutdownServer();
            }
        }
        else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skipping Coherence shutdown as requiresShutDown flag is false.");
            }
        }

        System.clearProperty("coherence.log");
        this.systemPropertyResolver.unset();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Shutdown of Coherence complete.");
        }

        this.setCoherenceSession(null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * see also https://stackoverflow.com/a/12389310/835934
     */
    @Override
    public boolean isMinimalPutsEnabledByDefault() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessType getDefaultAccessType() {
        return AccessType.READ_WRITE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextTimestamp() {
        if (this.cluster == null) {
            return System.currentTimeMillis();
        }
        else {
            return CacheFactory.ensureCluster().getTimeMillis();
        }
    }

    // ---- Internal

    /**
     * Ensure the initialization of a NamedCache of the argument name.
     * @param cacheName the name of the NamedCache whose initialization to ensure
     * @return a NamedCache for the argument name
     */
    protected NamedCache<?, ?> ensureNamedCache(String cacheName) {
        return this.coherenceSession.getCache(cacheName);
    }

    @Override
    protected DomainDataStorageAccess createDomainDataStorageAccess(DomainDataRegionConfig regionConfig,
        DomainDataRegionBuildingContext buildingContext) {
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
    protected StorageAccess createQueryResultsRegionStorageAccess(String regionName, SessionFactoryImplementor sessionFactory) {
        return new CoherenceStorageAccessImpl(this.createCoherenceRegion(regionName, sessionFactory));
    }

    protected CoherenceRegion createCoherenceRegion(final String unqualifiedRegionName,
                                                    final SessionFactoryImplementor sessionFactory) {
        return new CoherenceRegion(this, this.ensureNamedCache(unqualifiedRegionName), sessionFactory.getProperties());
    }

    @Override
    public DomainDataRegion buildDomainDataRegion(final DomainDataRegionConfig regionConfig,
                                                  final DomainDataRegionBuildingContext buildingContext) {
        return new CoherenceDomainDataRegionImpl(
          regionConfig,
          this,
          createDomainDataStorageAccess(regionConfig, buildingContext),
          this.cacheKeysFactory,
          buildingContext
        );
    }
}
