/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.enterprise.server.util;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Collections;
import java.util.List;

import javax.persistence.Query;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Lukas Krejci
 * @since 4.9
 */
@Test
public class QueryUtilityTest {

    private int[] attemptCounters;
    private Query dataQuery;
    private Query countQuery;
    private int numberOfInconsistentResults;

    @BeforeClass
    public void setupMocks() {
        attemptCounters = new int[2];
        dataQuery = Mockito.mock(Query.class);
        when(dataQuery.getResultList()).then(new Answer<List<?>>() {
            @Override
            public List<?> answer(InvocationOnMock invocation) throws Throwable {
                int attempt = attemptCounters[0]++;

                if (attempt < numberOfInconsistentResults) {
                    return Collections.emptyList();
                } else if (attempt < QueryUtility.PHANTOM_READ_MAX_ATTEMPTS) {
                    return Collections.singletonList(this);
                } else {
                    throw new AssertionError(
                        "Shouldn't have been called more than " + QueryUtility.PHANTOM_READ_MAX_ATTEMPTS + " times");
                }
            }
        });

        countQuery = Mockito.mock(Query.class);
        when(countQuery.getSingleResult()).then(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                if (attemptCounters[1]++ < QueryUtility.PHANTOM_READ_MAX_ATTEMPTS) {
                    return 1l;
                } else {
                    throw new AssertionError(
                        "Shouldn't have been called more than " + QueryUtility.PHANTOM_READ_MAX_ATTEMPTS + " times");
                }
            }
        });
    }

    @BeforeMethod
    public void resetAttemptCounters() {
        for(int i = 0; i < attemptCounters.length; ++i) {
            attemptCounters[i] = 0;
        }
    }
    public void defaultPagedDataFetchSettings() {
        QueryUtility.PagedDataFetchSettings settings = new QueryUtility.PagedDataFetchSettings();

        assertEquals(settings.getMaxAttempts(), QueryUtility.PHANTOM_READ_MAX_ATTEMPTS, "Wrong default max attempts");
        assertEquals(settings.getMinWaitTime(), QueryUtility.PHANTOM_READ_MIN_WAIT_TIME, "Wrong default min wait time");
        assertEquals(settings.getMaxWaitTime(), QueryUtility.PHANTOM_READ_MAX_WAIT_TIME, "Wrong default max wait time");
        assertFalse(settings.isThrowOnMaxAttempts(), "Wrong default throw on max attempts");
    }

    public void lagCoefficientForPagedDataFetch() {
        QueryUtility.PagedDataFetchSettings settings = new QueryUtility.PagedDataFetchSettings();
        settings.setMaxAttempts(10);
        settings.setMinWaitTime(1);
        settings.setMaxWaitTime(10);

        float lag = settings.getLagIncreaseCoefficient();
        assertEquals(lag, (float) Math.pow(10, 1d / 8), "Unexpected lag computed");
    }

    public void dataFetchPerformsMaxAttemptsOnInconsistentResults() {
        numberOfInconsistentResults = QueryUtility.DEFAULT_PHANTOM_READ_MAX_ATTEMPTS;

        PageControl pc = PageControl.getUnlimitedInstance();

        PageList<Object> result = QueryUtility.fetchPagedDataAndCount(dataQuery, countQuery, pc, null);

        assertEquals(result, Collections.emptyList(), "The result should be empty");
        assertEquals(result.getTotalSize(), 1, "Unexpected total size");
        assertFalse(result.isConsistent(), "The result should be inconsistent");

        assertEquals(attemptCounters[0], QueryUtility.PHANTOM_READ_MAX_ATTEMPTS);
        assertEquals(attemptCounters[1], QueryUtility.PHANTOM_READ_MAX_ATTEMPTS);
    }

    public void dataFetchReturnsConsistentResultsWhenDetected() {
        numberOfInconsistentResults = 2;

        PageControl pc = PageControl.getUnlimitedInstance();

        PageList<Object> result = QueryUtility.fetchPagedDataAndCount(dataQuery, countQuery, pc, null);

        assertEquals(result.size(), 1, "The result should have 1 element");
        assertEquals(result.getTotalSize(), 1, "Unexpected total size");
        assertTrue(result.isConsistent(), "The result should be consistent");

        assertEquals(attemptCounters[0], numberOfInconsistentResults + 1);
        assertEquals(attemptCounters[1], numberOfInconsistentResults + 1);
    }

    public void dataFetchThrowsAfterMaxAttemptsWhenSetUpSo() {
        numberOfInconsistentResults = QueryUtility.DEFAULT_PHANTOM_READ_MAX_ATTEMPTS;

        PageControl pc = PageControl.getUnlimitedInstance();
        QueryUtility.PagedDataFetchSettings settings = new QueryUtility.PagedDataFetchSettings();
        settings.setThrowOnMaxAttempts(true);

        try {
            QueryUtility.fetchPagedDataAndCount(dataQuery, countQuery, pc, settings);
            fail("Fetch should have thrown an exception after max attempts");
        } catch (PhantomReadMaxAttemptsExceededException e) {
            //expected
            PageList<?> result = e.getList();
            assertEquals(result, Collections.emptyList(), "The result should be empty");
            assertEquals(result.getTotalSize(), 1, "Unexpected total size");
            assertFalse(result.isConsistent(), "The result should be inconsistent");
        }

        assertEquals(attemptCounters[0], QueryUtility.PHANTOM_READ_MAX_ATTEMPTS);
        assertEquals(attemptCounters[1], QueryUtility.PHANTOM_READ_MAX_ATTEMPTS);
    }

    public void repeatedFetchesWaitLongEnough() {
        numberOfInconsistentResults = QueryUtility.DEFAULT_PHANTOM_READ_MAX_ATTEMPTS;

        PageControl pc = PageControl.getUnlimitedInstance();

        long time = System.currentTimeMillis();
        PageList<?> result = QueryUtility.fetchPagedDataAndCount(dataQuery, countQuery, pc, null);
        time = System.currentTimeMillis() - time;

        QueryUtility.PagedDataFetchSettings unusedSettings = new QueryUtility.PagedDataFetchSettings();

        assertTrue(time > unusedSettings.getMinimumTotalWaitTime(),
            "The fetch should have spent more time trying. Was " + time + "ms, but should have been at least " +
                unusedSettings.getMinimumTotalWaitTime() + "ms");

        assertEquals(result, Collections.emptyList(), "The result should be empty");
        assertEquals(result.getTotalSize(), 1, "Unexpected total size");
        assertFalse(result.isConsistent(), "The result should be inconsistent");

        assertEquals(attemptCounters[0], QueryUtility.PHANTOM_READ_MAX_ATTEMPTS);
        assertEquals(attemptCounters[1], QueryUtility.PHANTOM_READ_MAX_ATTEMPTS);
    }
}
