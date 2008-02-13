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
 * Listener that received notifications of something that needs to be logged. Subclasses need only implement
 * {@link #writeLogMessage(String)}.
 *
 * @author John Mazzitelli
 *
 */
public abstract class AbstractLoggerListener implements LoggerListener {
    /**
     * System property whose value is the maximum length of messages that are to be logged. If this property is not set,
     * the default is no limit.
     */
    private static final String PROP_MAX_LENGTH = "jdbcLogMaxLength";

    private boolean m_logSqlOnly = false;
    private int m_maxLength = Integer.MAX_VALUE;

    /**
     * @see LoggerListener#isLogSqlOnly()
     */
    public boolean isLogSqlOnly() {
        return m_logSqlOnly;
    }

    /**
     * @see LoggerListener#initialize(boolean)
     */
    public void initialize(boolean sql_only) {
        m_logSqlOnly = sql_only;

        try {
            String max_length_str = System.getProperty(PROP_MAX_LENGTH);
            m_maxLength = Integer.parseInt(max_length_str);
            if (m_maxLength < 10) {
                m_maxLength = 10; // don't let it get too small
            }
        } catch (Exception e) {
            m_maxLength = Integer.MAX_VALUE;
        }
    }

    /**
     * @see LoggerListener#logSQL(String)
     */
    public void logSQL(String sql) {
        writeLogMessage(truncateMessage(sql));
    }

    /**
     * @see LoggerListener#log(String)
     */
    public void log(String msg) {
        if (!isLogSqlOnly()) {
            writeLogMessage(truncateMessage(msg));
        }
    }

    /**
     * Truncates the message to a maximum line length.
     *
     * @param  msg
     *
     * @return truncated message
     */
    protected String truncateMessage(String msg) {
        if (msg.length() > m_maxLength) {
            StringBuffer buf = new StringBuffer(msg.substring(0, m_maxLength - 3));
            buf.append("...");
            msg = buf.toString();
        }

        return msg;
    }

    /**
     * Performs the actual logging of the given message string.
     *
     * @param msg
     */
    protected abstract void writeLogMessage(String msg);
}