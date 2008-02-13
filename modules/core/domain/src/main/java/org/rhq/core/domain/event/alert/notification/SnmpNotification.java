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
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.event.alert.AlertDefinition;

/**
 * An alert notification that sends an SNMP trap to the specified SNMP host+port+OID.
 *
 * @author Ian Springer
 */
@DiscriminatorValue("SNMP")
@Entity
@NamedQueries( { @NamedQuery(name = SnmpNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID, query = "SELECT sn "
    + "  FROM SnmpNotification sn " + " WHERE sn.alertDefinition.id = :alertDefinitionId ") })
public class SnmpNotification extends AlertNotification {
    public static final String QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID = "SnmpNotification.findAllByAlertDefinitionId";

    private static final int DEFAULT_PORT = 161;

    @Column(name = "SNMP_HOST", nullable = false)
    private String host;

    @Column(name = "SNMP_PORT", nullable = false)
    private int port;

    @Column(name = "SNMP_OID", nullable = false)
    private String oid;

    protected SnmpNotification() {
    } // JPA spec

    public SnmpNotification(SnmpNotification snmpNotification) {
        this(snmpNotification.getAlertDefinition(), snmpNotification.host, snmpNotification.port, snmpNotification.oid);
    }

    @SuppressWarnings( { "ConstantConditions" })
    public SnmpNotification(AlertDefinition alertDefinition, @NotNull
    String host, @Nullable
    Integer port, @NotNull
    String oid) {
        super(alertDefinition);
        if (host == null) {
            throw new IllegalArgumentException("host must be non-null.");
        }

        this.host = host;
        this.port = (port != null) ? port : DEFAULT_PORT;
        if (oid == null) {
            throw new IllegalArgumentException("oid must be non-null.");
        }

        this.oid = oid;
    }

    @NotNull
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @NotNull
    public String getOid() {
        return oid;
    }

    @Override
    public AlertNotification copy() {
        return new SnmpNotification(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof SnmpNotification)) {
            return false;
        }

        SnmpNotification that = (SnmpNotification) obj;
        if (this.port != that.port) {
            return false;
        }

        if (!this.host.equalsIgnoreCase(that.host)) {
            return false;
        }

        if (!this.oid.equals(that.oid)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = this.host.toLowerCase().hashCode();
        result = (31 * result) + this.port;
        result = (31 * result) + this.oid.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + "id=" + getId() + ", " + "host=" + this.host + ", " + "port="
            + this.port + ", " + "oid=" + this.oid + ", " + "]";
    }
}