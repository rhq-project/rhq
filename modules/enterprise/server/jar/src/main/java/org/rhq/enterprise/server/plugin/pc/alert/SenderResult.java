/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc.alert;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The result of sending an alert notification via AlertSender#send()
 * @author Heiko W. Rupp
 */
public class SenderResult {

    /** Message returned for logging purposes */
    private String message;
    /** Was calling the AlertSender.send() method successful? */
    private ResultState state;
    /** A list of email addresses that should be notified */
    private List<String> emails = new ArrayList<String>();

    /**
     * Default constructor. State is FAILURE by default
     */
    public SenderResult() {
        this.message = "No message set";
        this.state = ResultState.FAILURE;
    }

    public SenderResult(ResultState state, String message) {
        this.message = message;
        this.state = state;
    }

    public SenderResult(ResultState state, String message, List<String> emails) {
        this.message = message;
        this.state = state;
        this.emails = emails;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ResultState getState() {
        return state;
    }

    public void setState(ResultState state) {
        this.state = state;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public void addEmail(String email) {
        emails.add(email);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SenderResult");
        sb.append("{message='").append(message).append('\'');
        sb.append(", state=").append(state);
        sb.append(", emails=").append(emails);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SenderResult that = (SenderResult) o;

        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (state != that.state) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (state != null ? state.hashCode() : 0);
        return result;
    }
}
