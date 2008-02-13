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
package org.rhq.core.domain.event.alert.notification;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.event.alert.AlertDefinition;

@DiscriminatorValue("EMAIL")
@Entity
@NamedQueries( {
    @NamedQuery(name = EmailNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID, query = "SELECT en "
        + "  FROM EmailNotification en " + " WHERE en.alertDefinition.id = :alertDefinitionId "),
    @NamedQuery(name = EmailNotification.QUERY_FIND_BY_IDS, query = "SELECT en " + "  FROM EmailNotification en "
        + " WHERE en.id IN ( :ids )") })
public class EmailNotification extends AlertNotification {
    public static final String QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID = "EmailNotification.findAllByAlertDefinitionId";
    public static final String QUERY_FIND_BY_IDS = "EmailNotification.findByIds";

    @Column(name = "EMAIL_ADDRESS", nullable = false)
    private String emailAddress;

    protected EmailNotification() {
    } // JPA spec

    public EmailNotification(EmailNotification emailNotification) {
        this(emailNotification.getAlertDefinition(), emailNotification.emailAddress);
    }

    @SuppressWarnings( { "ConstantConditions" })
    public EmailNotification(@NotNull
    AlertDefinition alertDefinition, @NotNull
    String emailAddress) {
        super(alertDefinition);
        if (emailAddress == null) {
            throw new IllegalArgumentException("emailAddress must be non-null.");
        }

        this.emailAddress = emailAddress;
    }

    @NotNull
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public AlertNotification copy() {
        return new EmailNotification(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((emailAddress == null) ? 0 : emailAddress.toLowerCase().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof EmailNotification)) {
            return false;
        }

        final EmailNotification other = (EmailNotification) obj;
        if (emailAddress == null) {
            return other.emailAddress == null;
        } else {
            return emailAddress.equalsIgnoreCase(other.emailAddress);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + "id=" + getId() + ", " + "emailAddress=" + this.emailAddress
            + ", " + "]";
    }
}