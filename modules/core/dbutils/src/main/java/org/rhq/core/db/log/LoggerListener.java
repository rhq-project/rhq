/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.db.log;

/**
 * Listens for notifications that need to be logged.
 */
public interface LoggerListener {
    /**
     * Returns <code>true</code> if only SQL statements are logged; <code>false</code> means other types of
     * notifications are logged.
     *
     * @return log SQL only flag
     */
    boolean isLogSqlOnly();

    /**
     * Initializes the listener
     *
     * @param sql_only if <code>true</code>, this listener will only log SQL, not other notifications.
     */
    void initialize(boolean sql_only);

    /**
     * Logs an SQL statement that is being executed.
     *
     * @param sql the SQL that is being executed
     */
    void logSQL(String sql);

    /**
     * Logs the given notification message, but only if this listener was not told to log only SQL. If
     * {@link #initialize(boolean)} was given <code>true</code>, then this method does nothing and returns quietly.
     *
     * @param msg notification message
     */
    void log(String msg);
}