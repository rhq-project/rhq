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
package org.rhq.coregui.client.util.message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.rhq.coregui.client.util.Log;

/**
 * @author Greg Hinkle
 */
public class MessageCenter {

    private LinkedList<Message> messages = new LinkedList<Message>();
    private List<MessageListener> listeners = new ArrayList<MessageListener>();

    private int maxMessages = 50;

    public void notify(Message message) {
        log(message);
        if (!message.isTransient()) {
            // put the newest messages up front; old messages are at the end
            this.messages.addFirst(message);
            if (messages.size() > maxMessages) {
                messages.removeLast(); // we should only have 1 extra, so removeLast should remove all extras
            }
        }
        for (MessageListener listener : listeners) {
            listener.onMessage(message);
        }
    }

    public void addMessageListener(MessageListener listener) {
        this.listeners.add(listener);
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int max) {
        // if we are shrinking the list, clip the extra, older, messages
        if (max < this.maxMessages && messages.size() > max) {
            messages.subList(max, messages.size()).clear();
        }
        this.maxMessages = max;
    }

    /**
     * Returns a list of recently published non-transient messages.
     *
     * @return a list of recently published non-transient messages
     */
    public List<Message> getMessages() {
        return messages;
    }

    public void reset() {
        this.messages.clear();
    }

    interface MessageListener {
        void onMessage(Message message);
    }

    private void log(Message message) {
        String formattedMessage = "At [" + message.getFired() + "] MessageCenter received: "
            + message.getConciseMessage();
        if (message.severity == Message.Severity.Info) {
            Log.info(formattedMessage);
        } else if (message.severity == Message.Severity.Warning) {
            Log.warn(formattedMessage);
        } else if (message.severity == Message.Severity.Error) {
            Log.error(formattedMessage);
        } else if (message.severity == Message.Severity.Fatal) {
            Log.fatal(formattedMessage);
        } else {
            Log.debug(formattedMessage);
        }
    }

}
