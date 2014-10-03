/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.coregui.client.util;

import java.util.ArrayList;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class ErrorHandler {

    private static final String NL = "<br/>"; // \n if not going to use html
    private static final String INDENT = "   "; // &nbsp;&nbsp;&nbsp;&nbsp; if going to use html

    protected static final Messages MSG = CoreGUI.getMessages();

    public void handleError(String message) {
        handleError(message, null);
    }

    public void handleError(String message, Throwable t) {
        Severity severity;

        if ((t != null) && (t instanceof com.google.gwt.http.client.RequestTimeoutException)) {
            // if its a timeout exception, log it as a warning since the request might still complete on the server
            severity = Message.Severity.Warning;
            message = MSG.common_msg_asyncTimeout(message);
        } else {
            severity = Message.Severity.Error;
        }

        Message errorMessage = new Message(message, t, severity);
        CoreGUI.getMessageCenter().notify(errorMessage);

        if (t != null) {
            Log.warn(message, t);
        }
    }

    public static String getRootCauseMessage(Throwable t) {
        String[] allMessages = getAllMessagesArray(t, false);
        if (allMessages == null || allMessages.length == 0) {
            return null;
        }
        String lastMessage = allMessages[allMessages.length - 1];

        // our server side uses ThrowableUtil.getAllMessages which combines all
        // causes into one long message with each cause separated with the marker " -> ".
        // If we see that marker, take the message after the last marker.
        int lastMarker = lastMessage.lastIndexOf(" -> ");
        if (lastMarker != -1) {
            lastMessage = lastMessage.substring(lastMarker + 4);
        }
        return lastMessage;
    }

    public static String getAllMessages(Throwable t) {
        return getAllMessages(t, false, null, false);
    }

    public static String getAllMessages(Throwable t, boolean includeStackTrace, String newline, boolean includeThrowableClassName) {
        StringBuilder results = new StringBuilder();
        if (newline == null) {
            newline = NL;
        }

        if (t != null) {
            String[] msgs = getAllMessagesArray(t, includeThrowableClassName);
            results.append(msgs[0]);

            String indent = INDENT;
            for (int i = 1; i < msgs.length; i++) {
                results.append(newline).append(indent);
                results.append(msgs[i]);
                indent = indent + INDENT;
            }

            if (includeStackTrace) {
                results.append(newline).append(MSG.view_messageCenter_stackTraceFollows()).append(newline);
                getStackTrace(t, results, newline);
            }

        } else {
            results.append(">> ").append(MSG.util_errorHandler_nullException()).append(" <<");
        }

        return results.toString();
    }

    public static String[] getAllMessagesArray(Throwable t, boolean includeThrowableClassName) {
        ArrayList<String> list = new ArrayList<String>();

        while(t != null && t != t.getCause()) {
            String tMessage = t.getMessage();
            if (includeThrowableClassName) {
                list.add(t.getClass().getName() + ":" + tMessage);
            } else {
                if (tMessage != null) {
                    list.add(tMessage);
                } else {
                    // even though we were told not to show throwable class name,
                    // the problem is we have a null message - so the only thing we have to show is the class name
                    if (t.getCause() == null && t != t.getCause()) {
                        list.add(t.getClass().getName()); // Print only if we're not coming back
                    }
                }
            }
            t = t.getCause();
        }

        return list.toArray(new String[list.size()]);
    }

    public static String getStackTrace(Throwable t, StringBuilder s, String newline) {
        if (s == null) {
            s = new StringBuilder();
        }

        s.append(t.getMessage()).append(newline);

        for (Object line : t.getStackTrace()) {
            s.append(INDENT).append("at ").append(line).append(newline);
        }

        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            getStackTraceAsCause(t, s, cause, newline);
        }

        return s.toString();
    }

    private static void getStackTraceAsCause(Throwable t, StringBuilder s, Throwable cause, String newline) {
        s.append("Caused by: ").append(cause.getClass().getName() + ": " + cause.getMessage()).append(newline);

        for (Object line : cause.getStackTrace()) {
            s.append(INDENT).append("at ").append(line).append(newline);
        }

        Throwable nextCause = cause.getCause();
        if (nextCause != null && nextCause != cause) {
            getStackTraceAsCause(t, s, nextCause, newline);
        }
    }
}
