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

import org.rhq.core.domain.util.PageList;

/**
 * @author Lukas Krejci
 * @since 4.9
 */
public class PhantomReadMaxAttemptsExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int numberOfAttempts;
    private final PageList<?> list;
    private final long millisecondsSpentTrying;

    public PhantomReadMaxAttemptsExceededException(int numberOfAttempts, PageList<?> list,
        long millisecondsSpentTrying) {
        super(initMessage(numberOfAttempts, list.size(), list.getTotalSize(), millisecondsSpentTrying));
        this.numberOfAttempts = numberOfAttempts;
        this.list = list;
        this.millisecondsSpentTrying = millisecondsSpentTrying;
    }

    public PhantomReadMaxAttemptsExceededException(int numberOfAttempts, PageList<?> list,
        long millisecondsSpentTrying, Throwable cause) {
        super(initMessage(numberOfAttempts, list.size(), list.getTotalSize(), millisecondsSpentTrying), cause);
        this.numberOfAttempts = numberOfAttempts;
        this.list = list;
        this.millisecondsSpentTrying = millisecondsSpentTrying;
    }

    public PageList<?> getList() {
        return list;
    }

    public long getMillisecondsSpentTrying() {
        return millisecondsSpentTrying;
    }

    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }

    private static String initMessage(int numberOfAttempts, int collectionSize, int totalCountSize,
        long millisecondsSpentTrying) {
        return "Could not get consistent results of the paged data and a total count after " + numberOfAttempts +
            " attempts, the collection size is " + collectionSize + ", while the count query reports " +
            totalCountSize + ". The discrepancy has not cleared up in " + millisecondsSpentTrying + "ms so we're " +
            "giving up, returning inconsistent results. Note that is most possibly NOT an error. It is likely " +
            "caused by concurrent database activity that changes the contents of the database that the criteria " +
            "query is querying.";
    }
}
