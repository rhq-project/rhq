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
import java.util.EnumSet;

import org.rhq.enterprise.gui.coregui.client.util.ErrorHandler;

/**
 * A message to be displayed to the user in one or more places.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class Message {
    protected String conciseMessage;
    protected String detailedMessage;
    protected Date fired = new Date();
    protected Severity severity;
    protected EnumSet<Option> options;

    // TODO: Add Debug severity?
    public enum Severity {
        Info, Warning, Error, Fatal
    };

    public enum Option {
        Transient, Sticky, BackgroundJobResult
    };

    public Message(String conciseMessage) {
        this(conciseMessage, (Severity) null);
    }

    public Message(String conciseMessage, Severity severity) {
        this(conciseMessage, (String) null, severity);
    }

    public Message(String conciseMessage, String detailedMessage) {
        this(conciseMessage, detailedMessage, null);
    }

    public Message(String conciseMessage, Throwable details) {
        this(conciseMessage, details, null);
    }

    public Message(String conciseMessage, EnumSet<Option> options) {
        this(conciseMessage, null, options);
    }

    public Message(String conciseMessage, String detailedMessage, Severity severity) {
        this(conciseMessage, detailedMessage, severity, null);
    }

    public Message(String conciseMessage, Throwable details, Severity severity) {
        this(conciseMessage, details, severity, null);
    }

    public Message(String conciseMessage, Severity severity, EnumSet<Option> options) {
        this(conciseMessage, (String) null, severity, options);
    }

    public Message(String conciseMessage, Throwable details, Severity severity, EnumSet<Option> options) {
        this(conciseMessage, ErrorHandler.getAllMessages(details), severity, options);
    }

    public Message(String conciseMessage, String detailedMessage, Severity severity, EnumSet<Option> options) {
        this.conciseMessage = conciseMessage;
        this.detailedMessage = detailedMessage;
        this.severity = (severity != null) ? severity : Severity.Info;
        this.options = (options != null) ? options : EnumSet.noneOf(Option.class);
    }

    public String getConciseMessage() {
        return conciseMessage;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public Date getFired() {
        return fired;
    }

    public Severity getSeverity() {
        return severity;
    }

    public boolean isTransient() {
        return options.contains(Option.Transient);
    }

    public boolean isSticky() {
        return options.contains(Option.Sticky);
    }

    public boolean isBackgroundJobResult() {
        return options.contains(Option.BackgroundJobResult);
    }

    @Override
    public String toString() {
        return "Message{" //
            + "conciseMessage='" + this.conciseMessage + '\'' //
            + ", detailedMessage='" + this.detailedMessage + '\'' //
            + ", fired=" + this.fired //
            + ", severity=" + this.severity //
            + ", options=" + this.options + '}';
    }
}
