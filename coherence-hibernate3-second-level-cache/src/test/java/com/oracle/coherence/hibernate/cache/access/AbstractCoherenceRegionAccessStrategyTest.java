/*
 * File: AbstractCoherenceRegionAccessStrategyTest.java
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

package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.AbstractCoherenceRegionTest;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.junit.After;
import org.junit.Before;

/**
 * AbstractCoherenceRegionAccessStrategyTest is an abstract superclass for tests with a CoherenceRegionAccessStrategy
 * in the fixture.  It abstracts state for holding an instance of CoherenceRegion and behavior for setting up and
 * tearing down that instance.
 *
 * @author Randy Stafford
 */
public abstract class AbstractCoherenceRegionAccessStrategyTest
extends AbstractCoherenceRegionTest
{


    // ---- Fields

    /**
     * The CoherenceRegionAccessStrategy in the test fixture.
     */
    private CoherenceRegionAccessStrategy coherenceRegionAccessStrategy;


    // ---- Accessing

    /**
     * Returns the CoherenceRegionAccessStrategy in the fixture.
     *
     * @return the CoherenceRegionAccessStrategy in the fixture
     */
    protected CoherenceRegionAccessStrategy getCoherenceRegionAccessStrategy()
    {
        return coherenceRegionAccessStrategy;
    }

    /**
     * Returns the CollectionRegionAccessStrategy in the fixture.
     *
     * @return the CollectionRegionAccessStrategy in the fixture
     */
    protected CollectionRegionAccessStrategy getCollectionRegionAccessStrategy()
    {
        return (CollectionRegionAccessStrategy) coherenceRegionAccessStrategy;
    }

    /**
     * Returns the EntityRegionAccessStrategy in the fixture.
     *
     * @return the EntityRegionAccessStrategy in the fixture
     */
    protected EntityRegionAccessStrategy getEntityRegionAccessStrategy()
    {
        return (EntityRegionAccessStrategy) coherenceRegionAccessStrategy;
    }

    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @Before
    public void setUpAbstractCoherenceRegionAccessStrategyTest()
    {
        coherenceRegionAccessStrategy = newCoherenceRegionAccessStrategy();
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDownAbstractCoherenceRegionAccessStrategyTest()
    {
        coherenceRegionAccessStrategy = null;
    }


    // ---- Subclass responsibility

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected abstract CoherenceRegionAccessStrategy newCoherenceRegionAccessStrategy();


    // ---- Factory

    /**
     * Returns a new CollectionReadOnlyCoherenceRegionAccessStrategy.
     *
     * @return an CollectionReadOnlyCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newCollectionReadOnlyCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceCollectionRegion().buildAccessStrategy(AccessType.READ_ONLY);
    }

    /**
     * Returns a new CollectionNonstrictReadWriteCoherenceRegionAccessStrategy.
     *
     * @return an CollectionNonstrictReadWriteCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newCollectionNonstrictReadWriteCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceCollectionRegion().buildAccessStrategy(AccessType.NONSTRICT_READ_WRITE);
    }

    /**
     * Returns a new CollectionReadWriteCoherenceRegionAccessStrategy.
     *
     * @return an CollectionReadWriteCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newCollectionReadWriteCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceCollectionRegion().buildAccessStrategy(AccessType.READ_WRITE);
    }

    /**
     * Returns a new EntityReadOnlyCoherenceRegionAccessStrategy.
     *
     * @return an EntityReadOnlyCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newEntityReadOnlyCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceEntityRegion().buildAccessStrategy(AccessType.READ_ONLY);
    }

    /**
     * Returns a new EntityNonstrictReadWriteCoherenceRegionAccessStrategy.
     *
     * @return an EntityNonstrictReadWriteCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newEntityNonstrictReadWriteCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceEntityRegion().buildAccessStrategy(AccessType.NONSTRICT_READ_WRITE);
    }

    /**
     * Returns a new EntityReadWriteCoherenceRegionAccessStrategy.
     *
     * @return an EntityReadWriteCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newEntityReadWriteCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceEntityRegion().buildAccessStrategy(AccessType.READ_WRITE);
    }

}
