/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.alert.notification;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;

@DiscriminatorValue("SUBJECT")
@Entity
@NamedQueries( {
    @NamedQuery(name = SubjectNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID, query = "SELECT sn "
        + "  FROM SubjectNotification sn " + " WHERE sn.alertDefinition.id = :alertDefinitionId "),
    @NamedQuery(name = SubjectNotification.QUERY_FIND_BY_IDS, query = "SELECT sn " + "  FROM SubjectNotification sn "
        + " WHERE sn.id IN ( :ids )"),
    @NamedQuery(name = SubjectNotification.QUERY_FIND_BY_SUBJECT_IDS, query = "SELECT sn "
        + "  FROM SubjectNotification sn " + " WHERE sn.subject.id IN ( :ids )") })
public class SubjectNotification extends AlertNotification {

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID = "SubjectNotification.findAllByAlertDefinitionId";
    public static final String QUERY_FIND_BY_IDS = "SubjectNotification.findByIds";
    public static final String QUERY_FIND_BY_SUBJECT_IDS = "SubjectNotification.findBySubjectIds";

    @JoinColumn(name = "SUBJECT_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Subject subject;

    protected SubjectNotification() {
    } // JPA spec

    public SubjectNotification(SubjectNotification subjectNotification) {
        this(subjectNotification.getAlertDefinition(), subjectNotification.subject);
    }

    public SubjectNotification(@NotNull AlertDefinition alertDefinition, @NotNull Subject subject) {
        super(alertDefinition);
        if (subject == null) {
            throw new IllegalArgumentException("subject must be non-null.");
        }

        this.subject = subject;
    }

    @NotNull
    public Subject getSubject() {
        return subject;
    }

    @Override
    public AlertNotification copy() {
        return new SubjectNotification(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((subject == null) ? 0 : subject.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof SubjectNotification)) {
            return false;
        }

        final SubjectNotification other = (SubjectNotification) obj;
        if (subject == null) {
            if (other.subject != null) {
                return false;
            }
        } else if (!subject.equals(other.subject)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + "id=" + getId() + ", " + "subject=" + this.subject + ", " + "]";
    }
}