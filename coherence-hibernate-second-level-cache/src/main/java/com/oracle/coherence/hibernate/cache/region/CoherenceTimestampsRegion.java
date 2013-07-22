/*
 * File: CoherenceTimestampsRegion.java
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

package com.oracle.coherence.hibernate.cache.region;

import com.tangosol.net.NamedCache;
import org.hibernate.cache.spi.TimestampsRegion;

import java.util.Properties;

/**
 * A CoherenceTimestampsRegion is a CoherenceGeneralDataRegion intended to cache Hibernate timestamps.
 *
 * @author Randy Stafford
 */
public class CoherenceTimestampsRegion
extends CoherenceGeneralDataRegion
implements TimestampsRegion
{

    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceTimestampsRegion
     * @param properties configuration properties for this CoherenceTimestampsRegion
     */
    public CoherenceTimestampsRegion(NamedCache namedCache, Properties properties)
    {
        super(namedCache, properties);
    }


}
