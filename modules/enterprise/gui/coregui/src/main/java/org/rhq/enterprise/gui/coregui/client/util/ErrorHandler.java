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
package org.rhq.enterprise.gui.coregui.client.util;

import java.util.ArrayList;

import com.allen_sauer.gwt.log.client.Log;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class ErrorHandler {

    private static final String NL = "<br/>"; // \n if not going to use html
    private static final String INDENT = "&nbsp;&nbsp;&nbsp;&nbsp;"; // \t if not going to use html

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

    public static String getAllMessages(Throwable t) {
        return getAllMessages(t, false);
    }

    public static String getAllMessages(Throwable t, boolean includeStackTrace) {
        StringBuilder results = new StringBuilder();

        if (t != null) {
            String[] msgs = getAllMessagesArray(t);
            results.append(msgs[0]);

            String indent = INDENT;
            for (int i = 1; i < msgs.length; i++) {
                results.append(NL).append(indent);
                results.append(msgs[i]);
                indent = indent + INDENT;
            }

            if (includeStackTrace) {
                results.append(NL).append(MSG.view_messageCenter_stackTraceFollows()).append(NL);
                getStackTrace(t, results);
            }

        } else {
            results.append(">> " + MSG.util_errorHandler_nullException() + " <<");
        }

        return results.toString();
    }

    public static String[] getAllMessagesArray(Throwable t) {
        ArrayList<String> list = new ArrayList<String>();

        if (t != null) {
            list.add(t.getClass().getName() + ":" + t.getMessage());

            while ((t.getCause() != null) && (t != t.getCause())) {
                t = t.getCause();

                list.add(t.getClass().getName() + ":" + t.getMessage());
            }
        }

        return list.toArray(new String[list.size()]);
    }

    public static String getStackTrace(Throwable t, StringBuilder s) {
        if (s == null) {
            s = new StringBuilder();
        }

        s.append(t.getMessage()).append(NL);

        for (Object line : t.getStackTrace()) {
            s.append(INDENT).append("at ").append(line).append(NL);
        }

        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            getStackTraceAsCause(t, s, cause);
        }

        return s.toString();
    }

    private static void getStackTraceAsCause(Throwable t, StringBuilder s, Throwable cause) {
        s.append("Caused by: " + cause.getMessage()).append(NL);

        for (Object line : cause.getStackTrace()) {
            s.append(INDENT).append("at ").append(line).append(NL);
        }

        Throwable nextCause = cause.getCause();
        if (nextCause != null && nextCause != cause) {
            getStackTraceAsCause(t, s, nextCause);
        }
    }

}
