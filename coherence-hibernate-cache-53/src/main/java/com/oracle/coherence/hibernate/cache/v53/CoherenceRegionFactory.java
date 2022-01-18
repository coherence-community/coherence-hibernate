/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53;

import com.oracle.coherence.hibernate.cache.v53.access.CoherenceDomainDataRegionImpl;
import com.oracle.coherence.hibernate.cache.v53.access.CoherenceStorageAccessImpl;
import com.oracle.coherence.hibernate.cache.v53.configuration.session.SessionType;
import com.oracle.coherence.hibernate.cache.v53.configuration.support.Assert;
import com.oracle.coherence.hibernate.cache.v53.configuration.support.CoherenceHibernateProperties;
import com.oracle.coherence.hibernate.cache.v53.configuration.support.CoherenceHibernateSystemPropertyResolver;
import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegion;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
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

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A CoherenceRegionFactory is a factory for regions of Hibernate second-level cache implemented with Oracle Coherence.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 * @since 2.1
 */
public class CoherenceRegionFactory extends RegionFactoryTemplate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CoherenceRegionFactory.class);

    // ---- Constants

    private static final long serialVersionUID = -8434943540794407358L;

    protected CoherenceHibernateSystemPropertyResolver systemPropertyResolver;

    protected Session coherenceSession;
    private Coherence coherence;
    private final boolean requiresShutDown;

    // ---- Constructors

    /**
     * Default constructor. Any Coherence instances created will implicitly require a shutdown of Coherence when
     * {@link #stop()} is called via {@link #releaseFromUse()}.
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

    // ---- Fields

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
     * Returns the Coherence {@link Session} used by this {@link CoherenceRegionFactory}.
     *
     * @return the Coherence {@link Session}
     */
    protected Session getCoherenceSession()
    {
        return this.coherenceSession;
    }

    /**
     * Sets the Coherence {@link Session} used by this {@link CoherenceRegionFactory}.
     *
     * @param coherenceSession the Coherence {@link Session} used by this CoherenceRegionFactory. May be null.
     */
    protected void setCoherenceSession(Session coherenceSession)
    {
        this.coherenceSession = coherenceSession;
    }

    // ---- interface java.lang.Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass().getName() + "(" +
                "coherenceSession=" + (this.coherenceSession == null ? "N/A" : this.coherenceSession.getName()) +
                ", sessionFactoryOptions=" + sessionFactoryOptions +
                ")";
    }

    // ---- interface org.hibernate.cache.spi.RegionFactory

    @Override
    protected void prepareForUse(SessionFactoryOptions settings, Map configValues)
    {
        this.sessionFactoryOptions = settings;

        final CoherenceHibernateProperties coherenceHibernateProperties = new CoherenceHibernateProperties(configValues);

        final Map<String, Object> coherenceProperties = coherenceHibernateProperties.getCoherenceProperties();
        this.systemPropertyResolver = new CoherenceHibernateSystemPropertyResolver(coherenceProperties);

        if (this.systemPropertyResolver.getProperty(CoherenceHibernateProperties.COHERENCE_LOGGER_PROPERTY_NAME) == null) {
            this.systemPropertyResolver.addCoherenceProperty(CoherenceHibernateProperties.COHERENCE_LOGGER_PROPERTY_NAME, CoherenceHibernateProperties.COHERENCE_LOGGER_DEFAULT_VALUE);
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

        prepareCoherenceSessionIfNeeded(coherenceHibernateProperties);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("start({}, {})", settings, configValues);
        }
    }

    private void prepareCoherenceSessionIfNeeded(CoherenceHibernateProperties coherenceHibernateProperties) {
        if (this.coherenceSession == null) {

            final SessionConfiguration.Builder sessionConfigurationBuilder = SessionConfiguration.builder();

            if (coherenceHibernateProperties.getSessionName() != null) {
                sessionConfigurationBuilder.named(coherenceHibernateProperties.getSessionName());
            }

            sessionConfigurationBuilder.withConfigUri(coherenceHibernateProperties.getCacheConfigFilePath());

            final SessionConfiguration sessionConfiguration = sessionConfigurationBuilder.build();

            // GrpcSessionConfiguration.Builder

            final CoherenceConfiguration coherenceConfiguration = CoherenceConfiguration.builder()
                    .withSession(sessionConfiguration)
                    .build();

            final SessionType sessionType = coherenceHibernateProperties.getSessionType();

            this.coherence = this.createCoherenceInstance(sessionType, coherenceConfiguration);

            try {
                coherence.start().get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new IllegalStateException("Unable to start Coherence instance.", ex);
            }

            if (coherenceHibernateProperties.getSessionName() != null) {
                this.setCoherenceSession(coherence.getSession());
            }
            else {
                this.setCoherenceSession(coherence.getSession(coherenceHibernateProperties.getSessionName()));
            }
        }

        this.coherenceSession.activate();
    }

    @Override
    protected void releaseFromUse() {

        if (this.getCoherenceSession() != null) {
            try {
                this.coherenceSession.close();
            }
            catch (Exception ex) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Unable to close session '{}'.", this.coherenceSession.getName(), ex);
                }
            }
        }

        if (this.requiresShutDown) {
            this.coherence.getCluster().shutdown();
            this.coherence.close();

            try {
                this.coherence.whenClosed().get();
            }
            catch (InterruptedException | ExecutionException ex) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("An exception occurred while waiting for the Coherence instance to close.", ex);
                }
            }
        }
        else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skipping Coherence shutdown as requiresShutDown flag is false.");
            }
        }

        System.clearProperty("coherence.log");

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Shutdown of Coherence complete.");
        }

        this.setCoherenceSession(null);
        this.coherence = null;

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
        return this.coherenceSession.getCache(cacheName);
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
            SessionFactoryImplementor sessionFactory)
    {
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

    /**
     * Creates a {@link Coherence} instance with the {@link CoherenceConfiguration} provided. The created Coherence
     * instance may either be a client Coherence instance ({@link Coherence#client(CoherenceConfiguration)}) or a
     * cluster member instance ({@link Coherence#clusterMember(CoherenceConfiguration)}.
     * <p>
     * The rules for determining the instance type are as follows in descending priority:
     *
     * <ul>
     *    <li>Explicit configuration via parameter coherenceInstanceType.
     *    <li>Via the provided {@link SessionType}. As soon as {@link SessionType#SERVER} is provided,
     *        the Coherence instance is configured using {@link Coherence#clusterMember(CoherenceConfiguration)}.
     *    <li>If the provided {@link SessionType} is null,
     *        the Coherence instances is configured using {@link Coherence#clusterMember(CoherenceConfiguration)}.
     * </ul>
     * @param sessionType can be null
     * @param coherenceConfiguration must not be null
     * @return the Coherence instance
     */
    protected Coherence createCoherenceInstance(SessionType sessionType,
                                        CoherenceConfiguration coherenceConfiguration) {

        Assert.notNull(coherenceConfiguration, "coherenceConfiguration must not be null.");

        if (sessionType != null && sessionType.equals(SessionType.SERVER)) {
            return Coherence.clusterMember(coherenceConfiguration);
        }
        else {
            return Coherence.client(coherenceConfiguration);
        }
    }
}
