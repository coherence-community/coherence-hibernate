/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7.access.processor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.Comparator;

import com.oracle.coherence.hibernate.cache.v7.region.CoherenceRegionValue;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import org.hibernate.internal.util.compare.CalendarComparator;
import org.hibernate.internal.util.compare.RowVersionComparator;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;

/**
 * A AbstractReadWriteCoherenceEntityDataAccess.PutFromLoadProcessor is an EntryProcessor
 * responsible for putting a value in a second-level cache that was just loaded from database,
 * and returning a boolean indicating whether it did so, consistent with the expected behavior
 * of a cache access strategy's putFromLoad() method.
 *
 * We move this behavior into the grid for efficient concurrency control.
 *
 * @author Randy Stafford
 */
public class ReadWritePutFromLoadProcessor extends AbstractProcessor implements Serializable {

    /**
     * An identifier of this class's version for serialization purposes.
     */
    private static final long serialVersionUID = -3993308461928039511L;

    /**
     * A flag indicating whether "minimal puts" is in effect for Hibernate.
     */
    private boolean minimalPutsInEffect;

    /**
     * The replacement cache value in this ReadWritePutFromLoadProcessor.
     */
    private CoherenceRegionValue replacementValue;

    /**
     * From Hibernate javadoc, "a timestamp prior to the transaction start time"
     * [where "the transaction" loaded the potential replacement value from database].
     */
    private long txTimestamp;

    /**
     * A comparator for comparing actual value versions.
     */
    private Comparator versionComparator;

    /**
     * Complete constructor.
     * @param minimalPutsInEffect a flag indicating whether "minimal puts" is in effect for Hibernate
     * @param txTimestamp from Hibernate javadoc, "a timestamp prior to the transaction start time" [where "the transaction" loaded the potential replacement value from database]
     * @param replacementValue the replacement cache value in this ReadWritePutFromLoadProcessor
     * @param versionComparator a Comparator for comparing actual value versions
     */
    public ReadWritePutFromLoadProcessor(boolean minimalPutsInEffect, long txTimestamp, CoherenceRegionValue replacementValue, Comparator versionComparator) {
        this.minimalPutsInEffect = minimalPutsInEffect;
        this.txTimestamp = txTimestamp;
        this.replacementValue = replacementValue;
        // These built-in comparators are stateless but not Serializable. Resolve the exact
        // Hibernate ordering on storage; application-owned serializable comparators travel intact.
        if (versionComparator == RowVersionComparator.INSTANCE) {
            this.versionComparator = SerializableRowVersionComparator.INSTANCE;
        }
        else if (versionComparator == OffsetDateTimeJavaType.INSTANCE.getComparator()) {
            this.versionComparator = SerializableOffsetDateTimeComparator.INSTANCE;
        }
        else if (versionComparator == CalendarComparator.INSTANCE) {
            this.versionComparator = SerializableCalendarComparator.INSTANCE;
        }
        else {
            this.versionComparator = versionComparator;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(InvocableMap.Entry entry) {
        boolean isReplaceable = true;
        if (entry.isPresent()) {
            if (this.minimalPutsInEffect) {
                return false;
            }
            final CoherenceRegionValue presentValue = (CoherenceRegionValue) entry.getValue();
            isReplaceable = presentValue.isReplaceableFromLoad(this.txTimestamp, this.replacementValue.getVersion(), this.versionComparator);
        }
        if (isReplaceable) {
            entry.setValue(this.replacementValue);
        }
        return isReplaceable;
    }

    private enum SerializableCalendarComparator implements Comparator<Calendar> {
        INSTANCE;

        @Override
        public int compare(Calendar first, Calendar second) {
            return CalendarComparator.INSTANCE.compare(first, second);
        }
    }

    private enum SerializableOffsetDateTimeComparator implements Comparator<OffsetDateTime> {
        INSTANCE;

        @Override
        public int compare(OffsetDateTime first, OffsetDateTime second) {
            // Hibernate orders by instant, including nanoseconds, ignoring the offset for ties.
            return OffsetDateTimeJavaType.INSTANCE.getComparator().compare(first, second);
        }
    }

    private enum SerializableRowVersionComparator implements Comparator<byte[]> {
        INSTANCE;

        @Override
        public int compare(byte[] first, byte[] second) {
            return RowVersionComparator.INSTANCE.compare(first, second);
        }
    }
}
