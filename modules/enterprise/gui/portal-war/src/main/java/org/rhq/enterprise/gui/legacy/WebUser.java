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
package org.rhq.enterprise.gui.legacy;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;

/**
 * A representation of the person currently interacting with the application.
 * This essentially wraps a {@link Subject} and assigns that subject its
 * {@link Subject#setSessionId(Integer) session ID}.  Instances of this object
 * are placed in HTTP session.
 */
public class WebUser implements Serializable {

    private final Log log = LogFactory.getLog(WebUser.class);

    private Subject subject;

    public WebUser() {
        this(null);
    }

    public WebUser(Subject subject) {
        this.subject = subject;
    }

    /**
     * Returns this web user's {@link Subject}.
     *
     * @return the logged-in user's Subject representation
     */
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * Return the's user's {@link Subject#getId()} or <code>null</code>
     * if this web user is not associated with a particular Subject.
     *
     * @return subject ID or <code>null</code>
     */
    public Integer getId() {
        return (this.subject == null) ? null : this.subject.getId();
    }

    /**
     * Return the session id or <code>null</code> if there is not subject associated with this
     * web user or the session is not known.
     *
     * @return session ID of the currently logged in user, or <code>null</code> if unknown
     */
    public Integer getSessionId() {
        return (this.subject == null) ? null : this.subject.getSessionId();
    }

    /**
     * Set the session id for this web user.  If there is no Subject associated with this web user,
     * an exception is thrown.
     *
     * @param sessionId the new session id
     */
    public void setSessionId(Integer sessionId) {
        if (this.subject == null)
            throw new IllegalStateException("Cannot set a session ID for a web user that has no subject");

        this.subject.setSessionId(sessionId);
    }

    public String getUsername() {
        return (this.subject == null) ? null : this.subject.getName();
    }

    public void setUsername(String username) {
        this.subject.setName(username);
    }

    public String getName() {
        return getUsername();
    }

    public String getSmsaddress() {
        return (this.subject == null) ? null : this.subject.getSmsAddress();
    }

    public void setSmsaddress(String s) {
        this.subject.setSmsAddress(s);
    }

    public String getFirstName() {
        return (this.subject == null) ? null : this.subject.getFirstName();
    }

    public void setFirstName(String name) {
        this.subject.setFirstName(name);
    }

    public String getLastName() {
        return (this.subject == null) ? null : this.subject.getLastName();
    }

    public void setLastName(String name) {
        this.subject.setLastName(name);
    }

    public String getEmailAddress() {
        return (this.subject == null) ? null : this.subject.getEmailAddress();
    }

    public void setEmailAddress(String emailAddress) {
        this.subject.setEmailAddress(emailAddress);
    }

    public String getPhoneNumber() {
        return (this.subject == null) ? null : this.subject.getPhoneNumber();
    }

    public void setPhoneNumber(String phoneNumber) {
        this.subject.setPhoneNumber(phoneNumber);
    }

    public String getDepartment() {
        return (this.subject == null) ? null : this.subject.getDepartment();
    }

    public void setDepartment(String department) {
        this.subject.setDepartment(department);
    }

    public boolean getActive() {
        return (this.subject != null && this.subject.getFactive());
    }

    public void setActive(boolean active) {
        this.subject.setFactive(active);
    }

    /** Return a human readable serialization of this object */
    @Override
    public String toString() {
        StringBuffer str = new StringBuffer("{");
        str.append("id=").append(getId()).append(" ");
        str.append("sessionId=").append(getSessionId()).append(" ");
        str.append("subject=").append(getSubject()).append(" ");
        str.append("}");
        return (str.toString());
    }

    public WebUserPreferences getWebPreferences() {
        return new WebUserPreferences(subject);
    }

    public MeasurementPreferences getMeasurementPreferences() {
        return new MeasurementPreferences(subject);
    }
}
