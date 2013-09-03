/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.util;

import java.util.List;

import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

public class QueryUtility {
    private static final Log LOG = LogFactory.getLog(QueryUtility.class);

    private static String ESCAPE_CHARACTER = null;
    private static String ESCAPE_CLAUSE_CHARACTER = null;
    private static String ESCAPED_ESCAPE = null;
    private static String ESCAPED_PERCENT = null;
    private static String ESCAPED_UNDERSCORE = null;

    /**
     * Default value for {@link #PHANTOM_READ_MAX_ATTEMPTS}.
     */
    public static final int DEFAULT_PHANTOM_READ_MAX_ATTEMPTS = 10;

    /**
     * Default value for {@link #PHANTOM_READ_MIN_WAIT_TIME}.
     */
    public static final int DEFAULT_PHANTOM_READ_MIN_WAIT_TIME = 100;

    /**
     * Default value for {@link #PHANTOM_READ_MAX_WAIT_TIME}.
     */
    public static final int DEFAULT_PHANTOM_READ_MAX_WAIT_TIME = 1000;

    /**
     * The maximum number of retries the {@link #fetchPagedDataAndCount(javax.persistence.Query,
     * javax.persistence.Query, org.rhq.core.domain.util.PageControl,
     * org.rhq.enterprise.server.util.QueryUtility.PagedDataFetchSettings)} method tries to get consistent results.
     * <p/>
     * Inconsistent results may be caused by a phantom read in the database between performing the data and total count
     * queries.
     */
    public static final int PHANTOM_READ_MAX_ATTEMPTS = readIntFromSysProp(
        "rhq.server.database.phantom-read.max-retries", DEFAULT_PHANTOM_READ_MAX_ATTEMPTS, true);

    /**
     * The minimum wait time between retries in the {@link #fetchPagedDataAndCount(javax.persistence.Query,
     * javax.persistence.Query, org.rhq.core.domain.util.PageControl,
     * org.rhq.enterprise.server.util.QueryUtility.PagedDataFetchSettings)} method when trying to get consistent
     * results. The wait time gradually increases in a geometric progression from this value to the
     * {@link #PHANTOM_READ_MAX_WAIT_TIME} over {@link #PHANTOM_READ_MAX_ATTEMPTS}.
     * <p/>
     * Inconsistent results may be caused by a phantom read in the database between performing the data and total count
     * queries.
     */
    public static final int PHANTOM_READ_MIN_WAIT_TIME = readIntFromSysProp(
        "rhq.server.database.phantom-read.min-wait-time", DEFAULT_PHANTOM_READ_MIN_WAIT_TIME, false);

    /**
     * The maximum wait time between retries in the {@link #fetchPagedDataAndCount(javax.persistence.Query,
     * javax.persistence.Query, org.rhq.core.domain.util.PageControl,
     * org.rhq.enterprise.server.util.QueryUtility.PagedDataFetchSettings)} method when trying to get consistent
     * results. The wait time gradually increases in a geometric progression from {@link #PHANTOM_READ_MIN_WAIT_TIME} to
     * this value over {@link #PHANTOM_READ_MAX_ATTEMPTS}.
     * <p/>
     * Inconsistent results may be caused by a phantom read in the database between performing the data and total count
     * queries.
     */
    public static final int PHANTOM_READ_MAX_WAIT_TIME = readIntFromSysProp(
        "rhq.server.database.phantom-read.max-wait-time", DEFAULT_PHANTOM_READ_MAX_WAIT_TIME, false);

    //this is a default value to use when people pass "null" as settings to the fetchDataAndCount method.
    //we store it in a static field to avoid the overhead of computation of pow() on real numbers every time we query
    //the database, which we do a lot.
    private static final float INCREASE_COEFF;
    static {
        INCREASE_COEFF = new PagedDataFetchSettings().getLagIncreaseCoefficient();
    }

    /**
     * A settings object for the {@link #fetchPagedDataAndCount(javax.persistence.Query, javax.persistence.Query,
     * org.rhq.core.domain.util.PageControl, org.rhq.enterprise.server.util.QueryUtility.PagedDataFetchSettings)}
     * method.
     * <p/>
     * If a no-arg constructor is used, the instance is initialized with the following values:
     * <ul>
     *     <li><b>throwOnMaxAttempts</b> - {@code false}</li>
     *     <li><b>maxAttempts</b> - {@link #PHANTOM_READ_MAX_ATTEMPTS}</li>
     *     <li><b>minWaitTime</b> - {@link #PHANTOM_READ_MIN_WAIT_TIME}</li>
     *     <li><b>maxWaitTime</b> - {@link #PHANTOM_READ_MAX_WAIT_TIME}</li>
     * </ul>
     */
    public static class PagedDataFetchSettings {
        private boolean throwOnMaxAttempts = false;
        private int maxAttempts = PHANTOM_READ_MAX_ATTEMPTS;
        private int minWaitTime = PHANTOM_READ_MIN_WAIT_TIME;
        private int maxWaitTime = PHANTOM_READ_MAX_WAIT_TIME;
        private float increaseCoeff;

        public PagedDataFetchSettings() {
            recalculateIncreaseCoeff();
        }

        /**
         * The coefficient of a geometric progression starting from {@link #getMinWaitTime()} and going to
         * {@link #getMaxWaitTime()} over {@link #getMaxAttempts()}.
         * 
         * This is how this coefficient is computed and used in {@link QueryUtility#fetchPagedDataAndCount(
         * javax.persistence.Query, javax.persistence.Query, org.rhq.core.domain.util.PageControl,
         * org.rhq.enterprise.server.util.QueryUtility.PagedDataFetchSettings)}:<br/>
         *
         * <code>
         * fetch(0);                            <br/>
         * wait(0) = min;                       <br/>
         * fetch(1);                            <br/>
         * wait(1) = wait(0) * q;               <br/>
         * fetch(2);                            <br/>
         * wait(2) = wait(1) * q;               <br/>
         * ...                                  <br/>
         * wait(A-2) = wait(A-3) * q = max;     <br/>
         * fetch(A-1);                          <br/>
         *                                      <br/>
         * q = pow(max / min, 1 / (A - 2));
         * </code><br/>
         * where {@code min} is the min wait time, {@code max} is max wait time, {@code q} is the coefficient and
         * {@code A} is the number of attempts.
         * <p/>
         * Computing the pause between the attempts for data fetches this way, we assume that the longer we have
         * inconsistent results, the longer it is probable to last. I.e. all operations are either relatively fast
         * and should clean up in a couple of milliseconds or last quite long.
         */
        public float getLagIncreaseCoefficient() {
            return increaseCoeff;
        }

        /**
         * @return The minimum time spent when performing {@link #getMaxAttempts() maximum number} of attempts to
         * prevent a phantom read.
         */
        public long getMinimumTotalWaitTime() {
            if (increaseCoeff != 1) {
                return (long) (minWaitTime * (Math.pow(increaseCoeff, maxAttempts - 1) - 1) / (increaseCoeff - 1));
            } else {
                return minWaitTime * (maxAttempts - 1);
            }
        }

        /**
         * Never negative, but might be zero (which has the same semantics as 1).
         */
        public int getMaxAttempts() {
            return maxAttempts;
        }

        /**
         * @throws  IllegalArgumentException when a negative value is passed
         */
        public void setMaxAttempts(int maxAttempts) {
            if (maxAttempts < 0) {
                throw new IllegalArgumentException("Max attempts must be >= 0");
            }
            this.maxAttempts = maxAttempts;
            recalculateIncreaseCoeff();
        }

        /**
         * Positive integer, never 0.
         */
        public int getMaxWaitTime() {
            return maxWaitTime;
        }

        /**
         * @throws IllegalArgumentException when a negative number or 0 is passed
         */
        public void setMaxWaitTime(int maxWaitTime) {
            if (maxWaitTime <= 0) {
                throw new IllegalArgumentException("Max wait time must be > 0");
            }
            this.maxWaitTime = maxWaitTime;
            recalculateIncreaseCoeff();
        }

        /**
         * Positive integer, never 0.
         */
        public int getMinWaitTime() {
            return minWaitTime;
        }

        /**
         * @throws IllegalArgumentException when a negative number or 0 is passed
         */
        public void setMinWaitTime(int minWaitTime) {
            if (maxWaitTime <= 0) {
                throw new IllegalArgumentException("Min wait time must be > 0");
            }
            this.minWaitTime = minWaitTime;
            recalculateIncreaseCoeff();
        }

        public boolean isThrowOnMaxAttempts() {
            return throwOnMaxAttempts;
        }

        public void setThrowOnMaxAttempts(boolean throwOnMaxAttempts) {
            this.throwOnMaxAttempts = throwOnMaxAttempts;
        }

        private void recalculateIncreaseCoeff() {
            if (maxAttempts < 3) {
                increaseCoeff = 1; //doesn't really matter, because it is never in effect if maxAttempts < 3
            } else {
                increaseCoeff = (float) Math
                    .pow((double) maxWaitTime / minWaitTime, 1d / (maxAttempts - 2));
            }
        }
    }

    private static int readIntFromSysProp(String propName, int defaultValue, boolean zeroAllowed) {
        String valueAsString = System.getProperty(propName, Integer.toString(
            defaultValue));
        int value = defaultValue;

        String errorMessage = "The '" + propName + "' property has an invalid value '" + valueAsString + "'. " +
            "It is expected to be a positive integer " + (zeroAllowed ? "or 0" : "") + ". It has been set instead to " +
            "the default value of '" + defaultValue + "'.";

        try {
            value = Integer.parseInt(valueAsString);

            if ((zeroAllowed && value < 0) || value <= 0) {
                LOG.error(errorMessage);
                value = defaultValue;
            }
        } catch (NumberFormatException e) {
            LOG.error(errorMessage, e);
        }

        return value;
    }

    /**
     * Fetches the data and the total count using the provided queries, according to the provided {@code pageControl}
     * object.
     * <p/>
     * The fetch can be set up to try and avoid phantom reads - i.e. the possible inconsistencies between the number
     * of results, the obtained total count and the page control objects.
     *
     * @see PageControl#isConsistentWith(java.util.Collection, int)
     *
     * @param dataQuery the query to use to fetch the data. The provided {@code pageControl} is used to set the paging
     *                  on this query.
     * @param countQuery the query to use to obtain the total number of results
     * @param pageControl the object to control the paging of the results
     * @param settings the optional settings of the fetching. If null is passed, the behavior is the same as if a "default"
     *                 instance of the {@link PagedDataFetchSettings} was passed.
     * @param <T> the type of the result entities
     * @return the page list containing the page of the data, total count and page control
     * @throws PhantomReadMaxAttemptsExceededException if the settings specified to throw an exception on exceeding the
     * number of attempts to avoid phantom read
     */
    @SuppressWarnings("unchecked")
    public static <T> PageList<T> fetchPagedDataAndCount(Query dataQuery, Query countQuery, PageControl pageControl,
        PagedDataFetchSettings settings) throws PhantomReadMaxAttemptsExceededException {

        PersistenceUtility.setDataPage(dataQuery, pageControl);

        List<T> data = dataQuery.getResultList();
        int count = (int) (long) (Long) countQuery.getSingleResult();

        int cnt = 0;
        //this is float, so that we don't suffer from rounding errors when increasing it along the geometric progression
        float waitTime = settings == null ? PHANTOM_READ_MIN_WAIT_TIME : settings.getMinWaitTime();

        int maxAttempts = settings == null ? PHANTOM_READ_MAX_ATTEMPTS : settings.getMaxAttempts();

        long time = System.currentTimeMillis();
        float coeff = settings == null ? INCREASE_COEFF : settings.getLagIncreaseCoefficient();

        while (!pageControl.isConsistentWith(data, count) &&
            ++cnt < maxAttempts) { //++cnt - we already made 1 attempt out of the loop

            if (LOG.isDebugEnabled()) {
                LOG.debug("Possible phantom read detected while running a query. The collection size = " +
                    data.size() + ", count = " + count + ", pageControl = " + pageControl +
                    ". Attempt number " + cnt + ". Will wait for " + ((int) waitTime) + "ms.", new Exception());
            }

            try {
                Thread.sleep((int) waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            data = dataQuery.getResultList();
            count = (int) (long) (Long) countQuery.getSingleResult();

            waitTime *= coeff;
        }

        PageList<T> ret = new PageList<T>(data, count, pageControl);

        if (cnt == maxAttempts && (settings != null && settings.isThrowOnMaxAttempts())) {
            time = System.currentTimeMillis() - time;
            throw new PhantomReadMaxAttemptsExceededException(maxAttempts, ret, time);
        } else {
            return ret;
        }
    }

    /**
     * Given the settings for the current DatabaseType, properly handle escaping special SQL characters.
     * 
     * @param value
     * @return the properly escaped value.
     */
    public static String escapeSearchParameter(String value) {
        if (value == null || value.trim().equals("")) {
            return ""; // if we return null, query will get created as...where pathExpression LIKE '%null%'
        }

        return doEscapeSearchParameter(value);
    }

    private static String doEscapeSearchParameter(String value) {
        init();

        // Escape LIKE's wildcard characters with escaped characters so that the user's input
        // will be matched literally
        value = value.replace(ESCAPE_CHARACTER, ESCAPED_ESCAPE);
        value = value.replace("_", ESCAPED_UNDERSCORE);
        value = value.replace("%", ESCAPED_PERCENT);
        value = value.replace("'", "''");

        return value;
    }

    /**
     * Given the settings for the current DatabaseType, properly handle escaping special SQL characters as
     * well as UPCASING the value (standard for rhq filter searches) and wrapping with SQL wildcard for
     * implicit "contains" (i.e. '%' characters)  
     * 
     * @param value
     * @return the properly escaped and formatted value.
     */
    public static String formatSearchParameter(String value) {
        if (value == null || value.trim().equals("")) {
            return null;
        }

        return "%" + doEscapeSearchParameter(value).toUpperCase() + "%";
    }

    /**
     * Get the proper LIKE operator escape clause for the current DatabaseType.
     * 
     * @return The escape clause buffered with single spaces. For example: " ESCAPE '\' "
     */
    public static String getEscapeClause() {
        init();

        return " ESCAPE '" + ESCAPE_CLAUSE_CHARACTER + "' ";
    }

    /**
     * Get the proper ESCAPE clause character for the current DatabaseType. This is for use when
     * constructing query strings to be parsed (it may itself escape the escape character for
     * proper parsing (like in Postgres when standard_conforming_strings is off).
     * Call getEscapeCharacterParam() when needed for setting a NamedQuery parameter.
     * 
     * @return The escape character as a String.  The string may actually be multiple character but
     * when parsed by the vendor it will parse out the single character. 
     */
    public static String getEscapeClauseCharacter() {
        init();

        return ESCAPE_CLAUSE_CHARACTER;
    }

    /**
     * Get the proper ESCAPE clause character for the current DatabaseType. This is for use when
     * setting a NamedQuery paramater (unparsed, guaranteed to be a single char). If constructing
     * query strings to be parsed  Call getEscapeCharacter()
     * 
     * @return The single escape character as a String.
     */
    public static String getEscapeCharacter() {
        init();

        return ESCAPE_CHARACTER;
    }

    private static void init() {
        if (null == ESCAPE_CLAUSE_CHARACTER) {
            ESCAPE_CLAUSE_CHARACTER = DatabaseTypeFactory.getDefaultDatabaseType().getEscapeCharacter();

            // The escape character should be a single character. In postgres and possibly other
            // db types the character itself may need to be escaped for proper parsing of the ESCAPE value.
            // (for example, ESCAPE '\\' in postgres because backslash in a string literal is
            // escaped by default. In such a case assume the last character is the true escape character.
            int len = ESCAPE_CLAUSE_CHARACTER.length();
            ESCAPE_CHARACTER = (len > 1) ? ESCAPE_CLAUSE_CHARACTER.substring(len - 1) : ESCAPE_CLAUSE_CHARACTER;

            ESCAPED_ESCAPE = ESCAPE_CHARACTER + ESCAPE_CHARACTER;
            ESCAPED_UNDERSCORE = ESCAPE_CHARACTER + "_";
            ESCAPED_PERCENT = ESCAPE_CHARACTER + "%";
        }
    }
}
