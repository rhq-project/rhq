/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.measurement;

import org.rhq.enterprise.server.util.TimingVoodoo;

/**
 * @author John Sanda
 */
public class DateTimeService {

    private static final long SECOND = 1000L;
    private static final long MINUTE = SECOND * 60;
    private static final long HOUR = MINUTE * 60;
    private static final long DAY = HOUR * 24;
    private static final long SIX_HOUR = HOUR * 6;

    private static DateTimeService instance;

    public static DateTimeService getInstance() {
        return instance;
    }

    /**
     * This is here only as a test hook
     * @param dateTimeService The date time service to use for testing
     */
    public static void setInstance(DateTimeService dateTimeService) {
        instance = dateTimeService;
    }

    public long getCurrentHour() {
        return TimingVoodoo.roundDownTime(System.currentTimeMillis(), HOUR);
    }

}
