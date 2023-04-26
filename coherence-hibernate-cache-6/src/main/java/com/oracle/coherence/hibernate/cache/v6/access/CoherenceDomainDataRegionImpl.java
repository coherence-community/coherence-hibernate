/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6.access;

import java.util.Comparator;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;

/**
 *
 * @author Gunnar Hillert
 * @since 2.1
 *
 */
public class CoherenceDomainDataRegionImpl extends DomainDataRegionImpl
{

    public CoherenceDomainDataRegionImpl(DomainDataRegionConfig regionConfig, RegionFactoryTemplate regionFactory,
            DomainDataStorageAccess domainDataStorageAccess, CacheKeysFactory defaultKeysFactory,
            DomainDataRegionBuildingContext buildingContext)
    {
        super(regionConfig, regionFactory, domainDataStorageAccess, defaultKeysFactory, buildingContext);
    }

    @Override
    protected EntityDataAccess generateTransactionalEntityDataAccess(EntityDataCachingConfig entityAccessConfig)
    {
        throw new CacheException(AbstractCoherenceEntityDataAccess.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    protected NaturalIdDataAccess generateTransactionalNaturalIdDataAccess(NaturalIdDataCachingConfig accessConfig)
    {
        throw new CacheException(AbstractCoherenceEntityDataAccess.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    protected CollectionDataAccess generateTransactionalCollectionDataAccess(CollectionDataCachingConfig accessConfig)
    {
        throw new CacheException(AbstractCoherenceEntityDataAccess.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
    }

    // ---- Entity Access

    @Override
    protected EntityDataAccess generateReadOnlyEntityAccess(EntityDataCachingConfig accessConfig)
    {
        final Comparator<?> versionComparator = accessConfig.getVersionComparatorAccess() == null
                ? null
                : accessConfig.getVersionComparatorAccess().get();

        return new CoherenceReadOnlyEntityAccess(this, this.getCacheStorageAccess(), versionComparator);
    }

    @Override
    protected EntityDataAccess generateReadWriteEntityAccess(EntityDataCachingConfig accessConfig)
    {
        final Comparator<?> versionComparator = accessConfig.getVersionComparatorAccess() == null
                ? null
                : accessConfig.getVersionComparatorAccess().get();

        return new CoherenceReadWriteEntityAccess(this, this.getCacheStorageAccess(), versionComparator);
    }

    @Override
    protected EntityDataAccess generateNonStrictReadWriteEntityAccess(EntityDataCachingConfig accessConfig)
    {
        final Comparator<?> versionComparator = accessConfig.getVersionComparatorAccess() == null
                ? null
                : accessConfig.getVersionComparatorAccess().get();

        return new CoherenceNonstrictReadWriteEntityAccess(this, getCacheStorageAccess(), versionComparator);
    }

    // ---- Natural Id Access

    @Override
    protected NaturalIdDataAccess generateReadOnlyNaturalIdAccess(NaturalIdDataCachingConfig accessConfig)
    {
        return new CoherenceReadOnlyNaturalIdAccess(this, getCacheStorageAccess());
    }

    @Override
    protected NaturalIdDataAccess generateReadWriteNaturalIdAccess(NaturalIdDataCachingConfig accessConfig)
    {
        return new CoherenceReadWriteNaturalIdAccess(this, getCacheStorageAccess());
    }

    @Override
    protected NaturalIdDataAccess generateNonStrictReadWriteNaturalIdAccess(NaturalIdDataCachingConfig accessConfig)
    {
        return new CoherenceNonstrictReadWriteNaturalIdAccess(this, getCacheStorageAccess());
    }

    // ---- Collection Data Access

    @Override
    public CollectionDataAccess generateCollectionAccess(CollectionDataCachingConfig accessConfig) {
        final Comparator<?> versionComparator = accessConfig.getOwnerVersionComparator() == null
                ? null
                : accessConfig.getOwnerVersionComparator();

        switch ( accessConfig.getAccessType() ) {
            case READ_ONLY: {
                return new CoherenceReadOnlyCollectionAccess( this, getCacheStorageAccess(), versionComparator );
            }
            case READ_WRITE: {
                return new CoherenceReadWriteCollectionAccess( this, getCacheStorageAccess(), versionComparator );
            }
            case NONSTRICT_READ_WRITE: {
                return new CoherenceNonstrictReadWriteCollectionAccess( this, getCacheStorageAccess(), versionComparator );
            }
            case TRANSACTIONAL: {
                return generateTransactionalCollectionDataAccess( accessConfig );
            }
            default: {
                throw new IllegalArgumentException( "Unrecognized cache AccessType - " + accessConfig.getAccessType() );
            }
        }
    }

}
