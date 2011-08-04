/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.util.exception;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Utilities for working with throwables and their messages.
 *
 * @author John Mazzitelli
 */
public class ThrowableUtil {

    private static final String EXCEPTION_WAS_NULL = ">> exception was null <<";
    private static final String DOTS = " ... ";
    private static final String ARROW = " -> ";

    /**
     * Prevent instantiation.
     */
    private ThrowableUtil() {
    }

    /**
     * Returns all the messages for the throwable and all of its causes in one long string. This is useful for logging
     * an exception when you don't want to dump the entire stack trace but you still want to see the throwable and all
     * of its causes.
     *
     * @param  t                    the top throwable (may be <code>null</code>)
     * @param  includeExceptionName if <code>true</code>, the exception name will prefix all messages
     *
     * @return string containing the throwable's message and its causes' messages in the order of the causes (the lowest
     *         nested throwable's message appears last in the string)
     */
    public static String getAllMessages(Throwable t, boolean includeExceptionName) {
        StringBuffer ret_message = new StringBuffer();

        if (t != null) {
            String[] msgs = getAllMessagesArray(t, includeExceptionName);
            ret_message.append(msgs[0]);

            for (int i = 1; i < msgs.length; i++) {
                ret_message.append(ARROW);
                ret_message.append(msgs[i]);
            }
        } else {
            ret_message.append(EXCEPTION_WAS_NULL);
        }

        return ret_message.toString();
    }

    /**
     * Generates a string with all exception messages similarly to {@link #getAllMessages(Throwable, boolean)}
     * but limits the length of the string to the provided limit.
     * The messages are left out of the resulting string in the following manner:
     * <ul>
     * <li>The last message (i.e. the ultimate cause) is *always* in the output, regardless the maxSize
     * <li>The first throwable is output, then the second, etc. up until the point where appending the
     * next message *AND* the last message would make the output longer than maxSize.
     * <li>An ellipsis is appended if the ultimate cause isn't the direct cause of the 
     * throwable which was last output according to the above algo.
     * </ul>
     * @param t
     * @param includeExceptionNames
     * @param maxSize the maximum size of the message. If &lt; 0, the output is not limited.
     * @return
     */
    public static final String getAllMessages(Throwable t, boolean includeExceptionNames, int maxSize) {
        if (maxSize < 0) {
            return getAllMessages(t, includeExceptionNames);
        }
        
        if (t == null) {
            return EXCEPTION_WAS_NULL;
        }

        int arrowLength = ARROW.length();
        int dotsLength = DOTS.length();
        
        StringBuilder bld = new StringBuilder();
        
        String[] msgs = getAllMessagesArray(t, includeExceptionNames);
        
        //reduce the max size by the length of the last message
        int maxDottedSize = maxSize - msgs[msgs.length - 1].length() - dotsLength;
        // the dots and arrow have different lengths so we have to specialize for
        //the case where the output actually fits in the maxSize
        int maxFullSize = maxDottedSize + dotsLength - arrowLength; 
        
        if (msgs.length == 1 || maxDottedSize < 0) {
            return msgs[msgs.length - 1];
        }
        
        int maxIdx = msgs.length - 1;
        int lastIdx = maxIdx - 1;
        int curLen = msgs[0].length();
        
        if (curLen <= maxDottedSize) {
            bld.append(msgs[0]);
        }
        
        int curIdx = 1;
        for(; curIdx < maxIdx; ++curIdx) {
            int lenIncr = arrowLength + msgs[curIdx].length();
            
            if (curIdx == lastIdx) {
                if (curLen + lenIncr > maxFullSize) {
                    break;
                }
            } else {
                if (curLen + lenIncr > maxDottedSize) {
                    break;
                }
            }
            
            bld.append(ARROW);
            bld.append(msgs[curIdx]);
            
            curLen += lenIncr;
        }
        
        if (curIdx < msgs.length - 1) {
            bld.append(DOTS);
        } else {
            bld.append(ARROW);
        }
        
        bld.append(msgs[msgs.length - 1]);
        
        return bld.toString();
    }
    
    /**
     * Same as {@link #getAllMessages(Throwable, boolean)} with the "include exception name" parameter set to <code>
     * true</code>.
     *
     * @param  t the top throwable (may be <code>null</code>)
     *
     * @return string containing the throwable's message and its causes' messages in the order of the causes (the lowest
     *         nested throwable's message appears last in the string)
     */
    public static String getAllMessages(Throwable t) {
        return getAllMessages(t, true);
    }

    /**
     * Returns all the messages for the throwable and all of its causes.
     *
     * @param  t                    the top throwable (may be <code>null</code>)
     * @param  includeExceptionName if <code>true</code>, the exception name will prefix all messages
     *
     * @return array of strings containing the throwable's message and its causes' messages in the order of the causes
     *         (the lowest nested throwable's message appears last)
     */
    public static String[] getAllMessagesArray(Throwable t, boolean includeExceptionName) {
        ArrayList<String> list = new ArrayList<String>();

        if (t != null) {
            
            String msg;

            if (includeExceptionName) {
                msg = t.getClass().getName() + ":" + t.getMessage();
            } else {
                msg = t.getMessage();
            }

            if (t instanceof SQLException) {
                msg += "[SQLException=" + getAllSqlExceptionMessages((SQLException) t, false) + "]";
            }

            list.add(msg);

            while ((t.getCause() != null) && (t != t.getCause())) {
                t = t.getCause();

                if (includeExceptionName) {
                    msg = t.getClass().getName() + ":" + t.getMessage();
                } else {
                    msg = t.getMessage();
                }

                if (t instanceof SQLException) {
                    msg += "[SQLException=" + getAllSqlExceptionMessages((SQLException) t, false) + "]";
                }

                list.add(msg);
            }
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Same as {@link #getAllMessagesArray(Throwable, boolean)} with the "include exception name" parameter set to
     * <code>true</code>.
     *
     * @param  t the top throwable (may be <code>null</code>)
     *
     * @return string containing the throwable's message and its causes' messages in the order of the causes (the lowest
     *         nested throwable's message appears last in the string)
     */
    public static String[] getAllMessagesArray(Throwable t) {
        return getAllMessagesArray(t, true);
    }

    /**
     * Returns all the messages for the SQL Exception and all of its next exceptions in one long string.
     *
     * @param  t                    the top SQL Exception (may be <code>null</code>)
     * @param  includeExceptionName if <code>true</code>, the exception name will prefix all messages
     *
     * @return string containing the SQL Exception's message and its next exception's messages in order
     */
    public static String getAllSqlExceptionMessages(SQLException t, boolean includeExceptionName) {
        StringBuffer ret_message = new StringBuffer();

        if (t != null) {
            String[] msgs = getAllSqlExceptionMessagesArray(t, includeExceptionName);
            ret_message.append(msgs[0]);

            for (int i = 1; i < msgs.length; i++) {
                ret_message.append(ARROW);
                ret_message.append(msgs[i]);
            }
        } else {
            ret_message.append(">> sql exception was null <<");
        }

        return ret_message.toString();
    }

    /**
     * Same as {@link #getAllSqlExceptionMessages(Throwable, boolean)} with the "include exception name" parameter set
     * to <code>true</code>.
     *
     * @param  t the top sql exception (may be <code>null</code>)
     *
     * @return string containing the SQL Exception's message and its next exception's messages in order
     */
    public static String getAllSqlExceptionMessages(SQLException t) {
        return getAllSqlExceptionMessages(t, true);
    }

    /**
     * Returns all the messages for the SQL Exception and all of its causes.
     *
     * @param  t                    the top SQL Exception (may be <code>null</code>)
     * @param  includeExceptionName if <code>true</code>, the exception name will prefix all messages
     *
     * @return strings containing the SQL Exception's message and its next exception's messages in order
     */
    public static String[] getAllSqlExceptionMessagesArray(SQLException t, boolean includeExceptionName) {
        ArrayList<String> list = new ArrayList<String>();

        if (t != null) {
            if (includeExceptionName) {
                list.add(t.getClass().getName() + ":" + t.getMessage());
            } else {
                list.add(t.getMessage());
            }

            while ((t.getNextException() != null) && (t != t.getNextException())) {
                String msg;

                t = t.getNextException();
                if (includeExceptionName) {
                    msg = t.getClass().getName() + ":" + t.getMessage();
                } else {
                    msg = t.getMessage();
                }

                list.add(msg + "(error-code=" + t.getErrorCode() + ",sql-state=" + t.getSQLState() + ")");
            }
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Same as {@link #getAllSqlExceptionMessagesArray(SQLException, boolean)} with the "include exception name"
     * parameter set to <code>true</code>.
     *
     * @param  t the top sql exception (may be <code>null</code>)
     *
     * @return strings containing the SQL Exception's message and its next exception's messages in order
     */
    public static String[] getAllSqlExceptionMessagesArray(SQLException t) {
        return getAllSqlExceptionMessagesArray(t, true);
    }

    public static String getStackAsString(Throwable t) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(baos));
        return baos.toString();
    }

    public static String getRootMessage(Throwable t) {
        while ((t.getCause() != null) && (t != t.getCause())) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}