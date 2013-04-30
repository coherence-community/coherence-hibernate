package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A EntityReadOnlyCoherenceRegionAccessStrategyTest is a test of EntityReadOnlyCoherenceAccessStrategy behavior.
 *
 * @author Randy Stafford
 */
public class EntityReadOnlyCoherenceRegionAccessStrategyTest
extends AbstractCoherenceRegionAccessStrategyTest
{


    // ---- Subclass responsibility

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected CoherenceRegion newCoherenceRegion()
    {
        return newCoherenceEntityRegion();
    }

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected CoherenceRegionAccessStrategy newCoherenceRegionAccessStrategy()
    {
        return newEntityReadOnlyCoherenceRegionAccessStrategy();
    }


    // ---- Test cases

    /**
     * Tests EntityReadOnlyCoherenceRegionAccessStrategy.getRegion().
     */
    @Test
    public void testGetRegion()
    {
        EntityRegion entityRegion = getEntityRegionAccessStrategy().getRegion();
        assertTrue("Expect instance of EntityRegion", entityRegion instanceof EntityRegion);
    }

    /**
     * Tests EntityReadOnlyCoherenceRegionAccessStrategy.insert().
     */
    @Test
    public void testInsert()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testInsert";
        Object value = "testInsert";
        Object version = null;

        boolean cacheWasModified = accessStrategy.insert(key, value, version);
        assertFalse("Expect no cache modification from read-only access strategy insert", cacheWasModified);
    }

    /**
     * Tests EntityReadOnlyCoherenceRegionAccessStrategy.afterInsert().
     */
    @Test
    public void testAfterInsert()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testAfterInsert";
        Object value = "testAfterInsert";
        Object version = null;

        boolean cacheWasModified = accessStrategy.afterInsert(key, value, version);
        assertTrue("Expect cache modification from read-only access strategy afterInsert", cacheWasModified);
        assertTrue("Expect cache to contain key after afterInsert", accessStrategy.getRegion().contains(key));
        assertEquals("Expect to get same value put", value, accessStrategy.get(key, System.currentTimeMillis()));
    }

    /**
     * Tests EntityReadOnlyCoherenceRegionAccessStrategy.update().
     */
    @Test
    public void testUpdate()
    {
        try
        {
            EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

            Object key = "testUpdate";
            Object value = "testUpdate";
            Object currentVersion = null;
            Object previousVersion = null;

            accessStrategy.update(key, value, currentVersion, previousVersion);
            fail("Expect CacheException updating read-only access strategy");
        }
        catch (UnsupportedOperationException ex)
        {
            assertEquals("Expect writes not supported message", CoherenceRegionAccessStrategy.WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE, ex.getMessage());
        }
    }

    /**
     * Tests EntityReadOnlyCoherenceRegionAccessStrategy.afterUpdate().
     */
    @Test
    public void testAfterUpdate()
    {
        try
        {
            EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

            Object key = "testAfterUpdate";
            Object value = "testAfterUpdate";
            Object currentVersion = null;
            Object previousVersion = null;
            SoftLock softLock = null;

            accessStrategy.afterUpdate(key, value, currentVersion, previousVersion, softLock);
            fail("Expect CacheException updating read-only access strategy");
        }
        catch (UnsupportedOperationException ex)
        {
            assertEquals("Expect writes not supported message", CoherenceRegionAccessStrategy.WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE, ex.getMessage());
        }
    }


}
