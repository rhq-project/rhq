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

import java.util.Date;
import java.util.EnumSet;

import org.rhq.coregui.client.util.ErrorHandler;
import org.rhq.coregui.client.util.StringUtility;

/**
 * A message to be displayed to the user in one or more places.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class Message {
    private static final String BR = "=~br/~=";
    private static final String PRE_OPEN = "=~pre~=";
    private static final String PRE_CLOSE = "=~/pre~=";
    protected String conciseMessage;
    protected String detailedMessage;
    protected String rootCauseMessage = null;
    protected Date fired = new Date();
    protected Severity severity;
    protected EnumSet<Option> options;

    // TODO: Add Debug severity?
    public enum Severity {
        // keep the order - the ordinals are sorted least severe to highest severe
        Blank("InfoBlank", "info/icn_info_blank.png"), //
        Info("InfoBlock", "info/icn_info_blue.png"), //
        Warning("WarnBlock", "info/icn_info_orange.png"), //
        Error("ErrorBlock", "info/icn_info_red.png"), //
        Fatal("FatalBlock", "info/icn_info_red.png");

        private String style;
        private String icon;

        private Severity(String style, String icon) {
            this.style = style;
            this.icon = icon;
        }

        public String getStyle() {
            return style;
        }

        public String getIcon() {
            return icon;
        }
    };

    public enum Option {
        /**
         * The message will not be persisted in the message center list.
         */
        Transient,

        /**
         * The message will not auto-clear after a delay - it remains on the screen until you navigate away.
         */
        Sticky,

        /**
         * The message will be persisted in the message center list,
         * but will not show up in the main screen message area.
         */
        BackgroundJobResult
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
        this(conciseMessage, getDetailedMessageFromThrowable(details), severity, options);
        this.rootCauseMessage = ErrorHandler.getRootCauseMessage(details);
    }

    public Message(String conciseMessage, String detailedMessage, Severity severity, EnumSet<Option> options) {
        this.conciseMessage = StringUtility.escapeHtml(conciseMessage);
        String escapedDetailedMessage = StringUtility.escapeHtml(detailedMessage);
        this.detailedMessage = makeRestrictedHtmlMessage(escapedDetailedMessage);
        this.severity = (severity != null) ? severity : Severity.Info;
        this.options = (options != null) ? options : EnumSet.noneOf(Option.class);
    }

    private static String getDetailedMessageFromThrowable(Throwable t) {
        return PRE_OPEN + ErrorHandler.getAllMessages(t, true, BR, true) + PRE_CLOSE;
    }

    /**
     * Given a sanitized message with HTML tags escaped, this will put back some HTML tags that we know we
     * still want.
     * 
     * @param escapedDetailedMessage the message with escaped HTML characters
     * @return the escaped message, but with some HTML tags possibly now included
     */
    private String makeRestrictedHtmlMessage(String escapedDetailedMessage) {
        return (escapedDetailedMessage != null) ? escapedDetailedMessage.replaceAll(BR, "<br/>")
            .replaceAll(PRE_OPEN, "<pre>").replaceAll(PRE_CLOSE, "</pre>") : null;
    }

    public String getConciseMessage() {
        return conciseMessage;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public String getRootCauseMessage() {
        return rootCauseMessage;
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
            + ", rootCauseMessage='" + this.rootCauseMessage + '\'' //
            + ", fired=" + this.fired //
            + ", severity=" + this.severity //
            + ", options=" + this.options + '}';
    }
}
