package com.oracle.coherence.hibernate.cache;

import com.oracle.coherence.hibernate.cache.region.CoherenceCollectionRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceEntityRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceNaturalIdRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceQueryResultsRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceTimestampsRegion;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Settings;

import java.util.Properties;

/**
 * A CoherenceRegionFactory is a factory for regions of Hibernate second-level cache implemented with Oracle Coherence.
 *
 * @author Randy Stafford
 */
public class CoherenceRegionFactory
implements RegionFactory
{


    // ---- Constants

    /**
     * The prefix of the names of all properties specific to this SPI implementation.
     */
    public static final String PROPERTY_NAME_PREFIX = "com.oracle.coherence.hibernate.cache.";

    /**
     * The name of the property specifying the path to the Coherence cache configuration file.
     */
    public static final String CACHE_CONFIG_FILE_PATH_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "cache_config_file_path";

    /**
     * The name of the property specifying whether to dump stack on debug messages.
     */
    public static final String DUMP_STACK_ON_DEBUG_MESSAGE_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "dump_stack_on_debug_message";

    /**
     * The default path to the cache configuration file.
     */
    protected static final String DEFAULT_CACHE_CONFIG_FILE_PATH = "config/hibernate-cache-config.xml";


    // ---- Fields

    /**
     * A flag indicating whether to dump stack on debug messages.
     */
    private static boolean dumpStackOnDebugMessage = false;

    /**
     * The ConfigurableCacheFactory used by this CoherenceRegionFactory.
     */
    private ConfigurableCacheFactory cacheFactory;

    /**
     * The Hibernate settings object; may contain user-supplied "minimal puts" setting.
     */
    private Settings settings;


    // ---- Accessing

    /**
     * Returns the ConfigurableCacheFactory used by this CoherenceRegionFactory.
     *
     * @return the ConfigurableCacheFactory used by this CoherenceRegionFactory
     */
    protected ConfigurableCacheFactory getConfigurableCacheFactory()
    {
        return cacheFactory;
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


    // ---- API: logging support

    /**
     * Log the argument message with formatted arguments at debug severity.
     *
     * @param message the message to log
     * @param arguments the arguments to the format specifiers in message
     */
    public static void debugf(String message, Object ... arguments)
    {
        CacheFactory.log(String.format(message, arguments), 9);
        if (dumpStackOnDebugMessage) Thread.dumpStack();
    }


    // ---- interface org.hibernate.cache.spi.RegionFactory

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(Settings settings, Properties properties) throws CacheException
    {
        this.settings = settings;

        CacheFactory.ensureCluster();

        String cacheConfigFilePath = (properties == null) ?
                null :
                properties.getProperty(CACHE_CONFIG_FILE_PATH_PROPERTY_NAME);
        if (cacheConfigFilePath == null)
        {
            cacheConfigFilePath = System.getProperty(
                    CACHE_CONFIG_FILE_PATH_PROPERTY_NAME,
                    DEFAULT_CACHE_CONFIG_FILE_PATH);
        }

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(
                cacheConfigFilePath,
                getClass().getClassLoader());
        setConfigurableCacheFactory(factory);

        dumpStackOnDebugMessage = Boolean.getBoolean(DUMP_STACK_ON_DEBUG_MESSAGE_PROPERTY_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
    {
        // Note: Gavin's original CacheProvider called
        // CacheFactory.shutdown() but this causes problems when
        // multiple Hibernate SessionFactory instances are used in an application.
        // It also causes problems if the application is using
        // Coherence directly and has dependencies on it beyond
        // the lifespan of the Hibernate SessionFactory.
        //
        // In particular, it is difficult to determine which resources
        // (caches, cache services, cluster service) are shared.
        //
        // Since Coherence 3.5.1, we should release the factory
        CacheFactory.getCacheFactoryBuilder().release(getConfigurableCacheFactory());
        setConfigurableCacheFactory(null);
    }

    /**
     * {@inheritDoc}
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
        // See also related methods in CoherenceRegion ... there are fundamental
        // flaws with this approach (in the Hibernate code) no matter what we
        // do, so the goal is to minimize problems; note that we use the static
        // CacheFactory for this particular call rather than our own since
        // they both share a cluster service (and all we care about is time).
        return CacheFactory.ensureCluster().getTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata)
    throws CacheException
    {
        return new CoherenceEntityRegion(ensureNamedCache(regionName), settings, properties, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata)
    throws CacheException
    {
        return new CoherenceNaturalIdRegion(ensureNamedCache(regionName), settings, properties, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
    throws CacheException
    {
        return new CoherenceCollectionRegion(ensureNamedCache(regionName), settings, properties, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException
    {
        return new CoherenceQueryResultsRegion(ensureNamedCache(regionName), properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException
    {
        return new CoherenceTimestampsRegion(ensureNamedCache(regionName), properties);
    }


    // ---- Internal

    /**
     * Ensure the initialization of a NamedCache of the argument name.
     *
     * @param cacheName the name of the NamedCache whose initialization to ensure
     *
     * @return a NamedCache for the argument name
     */
    protected NamedCache ensureNamedCache(String cacheName)
    {
        // We intentionally avoid passing in a ClassLoader (as we can't do any
        // more than the default Coherence codepath does); however, this
        // method is provided so that customers may easily subclass.
        return getConfigurableCacheFactory().ensureCache(cacheName, null);
    }


}
