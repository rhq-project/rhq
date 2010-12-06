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

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class ErrorHandler {

    protected static final Messages MSG = CoreGUI.getMessages();

    public void handleError(String message) {
        handleError(message, null);
    }

    public void handleError(String message, Throwable t) {
        Message errorMessage = new Message(message, t, Message.Severity.Fatal);
        CoreGUI.getMessageCenter().notify(errorMessage);

        if (t != null) {
            System.err.println(message);
            t.printStackTrace();
        }
    }

    public static String getAllMessages(Throwable t) {
        StringBuffer results = new StringBuffer();

        if (t != null) {
            String[] msgs = getAllMessagesArray(t);
            results.append(msgs[0]);

            String indent = "   ";
            for (int i = 1; i < msgs.length; i++) {
                results.append("\n").append(indent);
                results.append(msgs[i]);
                indent = indent + "   ";
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
    
}
