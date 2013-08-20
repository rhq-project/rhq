/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.domain.cloud;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;

/**
 * An RHQ Storage Node (Cassandra)
 *
 * @author Jay Shaughnessy
 */
@Entity(name = "StorageNode")
@NamedQueries( //
{
    @NamedQuery(name = StorageNode.QUERY_FIND_ALL, query = "SELECT s FROM StorageNode s"),
    @NamedQuery(name = StorageNode.QUERY_FIND_BY_ADDRESS, query = "" //
        + "         SELECT s " //
        + "           FROM StorageNode s " //
        + "LEFT JOIN FETCH s.resource r " //
        + "          WHERE s.address = :address"),
    @NamedQuery(name = StorageNode.QUERY_FIND_ALL_BY_MODE, query =
        "SELECT s FROM StorageNode s WHERE s.operationMode = :operationMode"),
    @NamedQuery(name = StorageNode.QUERY_FIND_ALL_BY_MODE_EXCLUDING, query =
        "SELECT s FROM StorageNode s WHERE s.operationMode = :operationMode AND s <> :storageNode"),
    @NamedQuery(name = StorageNode.QUERY_FIND_ALL_NOT_INSTALLED, query = "SELECT s FROM StorageNode s WHERE NOT s.operationMode = 'INSTALLED'"),
    @NamedQuery(name = StorageNode.QUERY_FIND_ALL_NORMAL, query = "SELECT s FROM StorageNode s WHERE s.operationMode = 'NORMAL'"),
    @NamedQuery(name = StorageNode.QUERY_DELETE_BY_ID, query = "" //
        + "DELETE FROM StorageNode s WHERE s.id = :storageNodeId "),
    @NamedQuery(name = StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_PARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES, query = "" //
        + "   SELECT def.name, def.id, ms.id, res.id  FROM MeasurementSchedule ms   " //
        + "     JOIN ms.definition def " //
        + "     JOIN ms.resource res  " //
        + "    WHERE ms.definition = def    " //
        + "      AND res.parentResource.id = :parrentId    " //
        + "      AND ms.enabled = true" //
        + "      AND def.name IN (:metricNames)"), //

    @NamedQuery(name = StorageNode.QUERY_FIND_SCHEDULE_IDS_BY_GRANDPARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES, query = "" //
        + "   SELECT def.name, def.id, ms.id, res.id  FROM MeasurementSchedule ms   " //
        + "     JOIN ms.definition def " //
        + "     JOIN ms.resource res  " //
        + "    WHERE ms.definition = def    " //
        + "      AND res.parentResource.parentResource.id = :grandparrentId    " //
        + "      AND ms.enabled = true" //
        + "      AND def.name IN (:metricNames)"), //
    @NamedQuery(name = StorageNode.QUERY_UPDATE_REMOVE_LINKED_RESOURCES, query = "" //
        + "   UPDATE StorageNode s " //
        + "      SET s.resource = NULL  " //
        + "    WHERE s.resource.id in (:resourceIds)"),
    @NamedQuery(name = StorageNode.QUERY_UPDATE_OPERATION_MODE, query =
        "UPDATE StorageNode s SET s.operationMode = :newOperationMode WHERE s.operationMode = :oldOperationMode")
})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_STORAGE_NODE_ID_SEQ", sequenceName = "RHQ_STORAGE_NODE_ID_SEQ")
@Table(name = "RHQ_STORAGE_NODE")
public class StorageNode implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "StorageNode.findAll";
    public static final String QUERY_FIND_BY_ADDRESS = "StorageNode.findByAddress";
    public static final String QUERY_FIND_ALL_BY_MODE = "StorageNode.findAllByMode";
    public static final String QUERY_FIND_ALL_BY_MODE_EXCLUDING = "StorageNode.findAllByModeExcluding";
    public static final String QUERY_FIND_ALL_NOT_INSTALLED = "StorageNode.findAllCloudMembers";
    public static final String QUERY_DELETE_BY_ID = "StorageNode.deleteById";
    public static final String QUERY_FIND_ALL_NORMAL = "StorageNode.findAllNormalCloudMembers";
    public static final String QUERY_FIND_SCHEDULE_IDS_BY_PARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES = "StorageNode.findScheduleIdsByParentResourceIdAndMeasurementDefinitionNames";
    public static final String QUERY_FIND_SCHEDULE_IDS_BY_GRANDPARENT_RESOURCE_ID_AND_MEASUREMENT_DEFINITION_NAMES = "StorageNode.findScheduleIdsByGrandparentResourceIdAndMeasurementDefinitionNames";
    public static final String QUERY_UPDATE_REMOVE_LINKED_RESOURCES = "StorageNode.updateRemoveLinkedResources";
    public static final String QUERY_UPDATE_OPERATION_MODE = "StorageNode.updateOperationMode";

    private static final String JMX_CONNECTION_STRING = "service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_STORAGE_NODE_ID_SEQ")
    @Id
    private int id;

    @Column(name = "ADDRESS", nullable = false)
    private String address;

    @Column(name = "JMX_PORT", nullable = false)
    private int jmxPort;

    @Column(name = "CQL_PORT", nullable = false)
    private int cqlPort;

    @Column(name = "OPERATION_MODE", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationMode operationMode;

    // the time this storage node was installed into the infrastructure
    @Column(name = "CTIME", nullable = false)
    private long ctime;

    // the time this storage node was last updated
    @Column(name = "MTIME", nullable = false)
    private long mtime;

    @Column(name = "MAINTENANCE_PENDING", nullable = false)
    private boolean maintenancePending;

    @Column(name = "ERROR_MSG", nullable = true)
    private String errorMessage;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(fetch = FetchType.EAGER, optional = true)
    private Resource resource;

    @JoinColumn(name = "RESOURCE_OP_HIST_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(optional = true, cascade = {CascadeType.REMOVE})
    private ResourceOperationHistory failedOperation;

    // required for JPA
    public StorageNode() {
    }

    public StorageNode(int storageNodeId) {
        this.id = storageNodeId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    public void setJmxPort(int jmxPort) {
        this.jmxPort = jmxPort;
    }

    public int getCqlPort() {
        return cqlPort;
    }

    public void setCqlPort(int cqlPort) {
        this.cqlPort = cqlPort;
    }

    public long getCtime() {
        return ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public boolean isMaintenancePending() {
        return maintenancePending;
    }

    public void setMaintenancePending(boolean maintenancePending) {
        this.maintenancePending = maintenancePending;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ResourceOperationHistory getFailedOperation() {
        return failedOperation;
    }

    public void setFailedOperation(ResourceOperationHistory failedOperation) {
        this.failedOperation = failedOperation;
    }

    public OperationMode getOperationMode() {
        return operationMode;
    }

    public void setOperationMode(OperationMode operationMode) {
        this.operationMode = operationMode;
    }

    public Status getStatus() {
        if (operationMode == OperationMode.INSTALLED) {
            return Status.INSTALLED;
        }
        if (operationMode == OperationMode.ANNOUNCE || operationMode == OperationMode.BOOTSTRAP ||
            operationMode == OperationMode.ADD_MAINTENANCE) {
            if (errorMessage == null && failedOperation == null) {
                return Status.JOINING;
            } else {
                return Status.DOWN;
            }
        }
        if (operationMode == OperationMode.DECOMMISSION || operationMode == OperationMode.UNANNOUNCE ||
            operationMode == OperationMode.REMOVE_MAINTENANCE || operationMode == OperationMode.UNINSTALL) {
            if (errorMessage == null && failedOperation == null) {
                return Status.LEAVING;
            } else {
                return Status.DOWN;
            }
        }
        if (operationMode == OperationMode.NORMAL) {
            return Status.NORMAL;
        }
        return Status.DOWN;
    }

    public enum OperationMode {
        DECOMMISSION("Remove the storage node from service"),
        DOWN("This storage node is down"), //
        INSTALLED("This storage node is newly installed but not yet operational"), //
        MAINTENANCE("This storage node is in maintenance mode"), //
        NORMAL("This storage node is running normally"),
        ANNOUNCE("The storage node is installed but not yet part of the cluster. It is being announced so that it " +
            "can join the cluster."),
        UNANNOUNCE("The storage node has been decommissioned and the cluster is being notified to stop accepting " +
            "gossip from its IP address."),
        BOOTSTRAP("The storage is installed but not yet part of the cluster. It is getting bootstrapped into the " +
            "cluster"),
        ADD_MAINTENANCE("The storage node is running and is preparing to undergo routine maintenance that is " +
            "necessary when a new node joins the cluster."),
        REMOVE_MAINTENANCE("The storage node is no longer part of the cluster. Remaining storage node are " +
            "undergoing cluster maintenance due to the topology change."),
        UNINSTALL("The storage node is being removed from inventory and its bits on disk are getting purged.");

        public final String message;

        private OperationMode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public enum Status {
        INSTALLED,
        DOWN,
        NORMAL,
        JOINING,
        LEAVING
    }

    public String getJMXConnectionURL() {
        // GWT doesn't support String.format()
        String[] split = JMX_CONNECTION_STRING.split("%s");
        return split[0] + this.address + split[1] + this.jmxPort + split[2];
    }

    @Override
    public String toString() {
        return "StorageNode[id=" + id + ", address=" + address + ", jmxPort=" + jmxPort + ", cqlPort=" + cqlPort
            + ", operationMode=" + operationMode + ", mtime=" + mtime + "]";
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
        this.mtime = this.ctime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof StorageNode)) {
            return false;
        }

        final StorageNode other = (StorageNode) obj;

        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }

        return true;
    }

    public void parseNodeInformation(String s) {
        String[] params = s.split("\\|");
        if (params.length != 3) {
            throw new IllegalArgumentException("Expected string of the form, hostname|jmxPort|nativeTransportPort: ["
                + s + "]");
        }

        this.setAddress(params[0]);
        this.setJmxPort(Integer.parseInt(params[1]));
        this.setCqlPort(Integer.parseInt(params[2]));
    }

}
