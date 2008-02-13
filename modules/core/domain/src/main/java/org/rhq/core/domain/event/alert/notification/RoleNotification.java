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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.event.alert.AlertDefinition;

@DiscriminatorValue("ROLE")
@Entity
@NamedQueries( {
    @NamedQuery(name = RoleNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID, query = "SELECT rn "
        + "  FROM RoleNotification rn " + " WHERE rn.alertDefinition.id = :alertDefinitionId "),
    @NamedQuery(name = RoleNotification.QUERY_FIND_BY_IDS, query = "SELECT rn " + "  FROM RoleNotification rn "
        + " WHERE rn.id IN ( :ids )"),
    @NamedQuery(name = RoleNotification.QUERY_FIND_BY_ROLE_IDS, query = "SELECT rn " + "  FROM RoleNotification rn "
        + " WHERE rn.role.id IN ( :ids )") })
public class RoleNotification extends AlertNotification {
    public static final String QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID = "RoleNotification.findAllByAlertDefinitionId";
    public static final String QUERY_FIND_BY_IDS = "RoleNotification.findByIds";
    public static final String QUERY_FIND_BY_ROLE_IDS = "SubjectNotification.findByRoleIds";

    @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Role role;

    protected RoleNotification() {
    } // JPA spec

    public RoleNotification(RoleNotification roleNotification) {
        this(roleNotification.getAlertDefinition(), roleNotification.role);
    }

    @SuppressWarnings( { "ConstantConditions" })
    public RoleNotification(@NotNull
    AlertDefinition alertDefinition, @NotNull
    Role role) {
        super(alertDefinition);
        if (role == null) {
            throw new IllegalArgumentException("role must be non-null.");
        }

        this.role = role;
    }

    @NotNull
    public Role getRole() {
        return role;
    }

    @Override
    public AlertNotification copy() {
        return new RoleNotification(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((role == null) ? 0 : role.hashCode());
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

        if (!(obj instanceof RoleNotification)) {
            return false;
        }

        final RoleNotification other = (RoleNotification) obj;
        if (role == null) {
            if (other.role != null) {
                return false;
            }
        } else if (!role.equals(other.role)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + "id=" + getId() + ", " + "role=" + this.role + ", " + "]";
    }
}