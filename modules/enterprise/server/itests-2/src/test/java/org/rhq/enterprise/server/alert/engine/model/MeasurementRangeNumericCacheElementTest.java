/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.alert.engine.model;

import org.testng.annotations.Test;

import org.rhq.core.domain.alert.AlertConditionOperator;

/**
 * there are two ways we can check the value compared to the range, each with either inclusivness or exclusivness.
 * inclusive means if value equals either the lo or hi, it is considered inside the range.
 * exclusive means if value equals either the lo or hi, it is NOT considered inside the range, it is outside the range
 *    <  - if the value is inside the range (i.e. between the low and high values), exclusive
 *    >  - if the value is outside the range (i.e. lower than the low value or higher than the high value), exclusive
 *    <= - if the value is inside the range (i.e. between the low and high values), inclusive
 *    >= - if the value is outside the range (i.e. lower than the low value or higher than the high value), inclusive
 *
 * Example:
 *   Given a value of 20, with a low-high range of 20...50.
 *   <  (inside,  exclusive) - NO match - 20 is not considered inside the range but we are looking for values inside the range
 *   >  (outside, exclusive) - MATCH    - 20 is not considered inside the range and we are looking for values outside the range
 *   <= (inside,  inclusive) - MATCH    - 20 is considered inside the range and we are looking for values inside the range
 *   >= (outside, inclusive) - NO match - 20 is considered inside the range but we are looking for values outside the range 
 */
@Test
public class MeasurementRangeNumericCacheElementTest {
    private final Double lo = Double.valueOf(20.0);
    private final Double hi = Double.valueOf(50.0);
    private final Double inside = Double.valueOf(30.0); // inside the range
    private final Double outsideLo = Double.valueOf(1.0); // outside the range on the lo side
    private final Double outsideHi = Double.valueOf(111.0); // outside the range on the hi side

    public void testInsideExclusive() {
        MeasurementRangeNumericCacheElement ele = createCacheElement(AlertConditionOperator.LESS_THAN);
        assert true == ele.matches(inside);
        assert false == ele.matches(outsideLo);
        assert false == ele.matches(outsideHi);
        assert false == ele.matches(lo);
        assert false == ele.matches(hi);
    }

    public void testOutsideExclusive() {
        MeasurementRangeNumericCacheElement ele = createCacheElement(AlertConditionOperator.GREATER_THAN);
        assert false == ele.matches(inside);
        assert true == ele.matches(outsideLo);
        assert true == ele.matches(outsideHi);
        assert true == ele.matches(lo);
        assert true == ele.matches(hi);
    }

    public void testInsideInclusive() {
        MeasurementRangeNumericCacheElement ele = createCacheElement(AlertConditionOperator.LESS_THAN_OR_EQUAL_TO);
        assert true == ele.matches(inside);
        assert false == ele.matches(outsideLo);
        assert false == ele.matches(outsideHi);
        assert true == ele.matches(lo);
        assert true == ele.matches(hi);
    }

    public void testOutsideInclusive() {
        MeasurementRangeNumericCacheElement ele = createCacheElement(AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO);
        assert false == ele.matches(inside);
        assert true == ele.matches(outsideLo);
        assert true == ele.matches(outsideHi);
        assert false == ele.matches(lo);
        assert false == ele.matches(hi);
    }

    public void testUnsupportedComparator() {
        try {
            createCacheElement(AlertConditionOperator.EQUALS);
            assert false : "should not be able to support EQUALS";
        } catch (Exception ok) {
            // this is OK and to be expected
        }
    }

    private MeasurementRangeNumericCacheElement createCacheElement(AlertConditionOperator op) {
        return new MeasurementRangeNumericCacheElement(op, lo, hi, 0);
    }
}
