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
package org.rhq.enterprise.gui.coregui.client.util.message;

import java.util.Date;

/**
 * @author Greg Hinkle
 */
@SuppressWarnings({"UnnecessarySemicolon"})
public class Message {
    protected String title;
    protected String detail;
    protected Date fired = new Date();
    protected Severity severity;

    // TODO: Add Debug severity?
    public enum Severity { Info, Warning, Error };

    public Message(String title, Severity severity) {
        this(title, null, severity);
    }

    public Message(String title, String detail, Severity severity) {
        this.title = title;
        this.detail = detail;
        this.severity = (severity != null) ? severity : Severity.Info;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public Date getFired() {
        return fired;
    }

    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return "Message{" +
            "title='" + title + '\'' +
            ", detail='" + detail + '\'' +
            ", fired=" + fired +
            ", severity=" + severity +
            '}';
    }
}
