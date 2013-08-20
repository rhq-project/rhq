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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.util.PageList;

/**
 * API for managing <code>StorageNode</code> instances
 *
 * @author Jirka Kremser
 */
public interface StorageGWTService extends RemoteService {

    /**
     * Finder for <code>StorageNode</code> instances
     * 
     * @param criteria the criteria for finding storage nodes
     * @return a list of <code>StorageNode</code> instances
     * @throws RuntimeException
     */    
    PageList<StorageNode> findStorageNodesByCriteria(StorageNodeCriteria criteria) throws RuntimeException;
    
    void invokeOperationOnStorageService(int storageNodeId, String operationName) throws RuntimeException;
    
    /**
     * <p>Returns the summary of load of the storage node.</p>
     * 
     * <p>the subject needs to have <code>MANAGE_SETTINGS</code> permissions.</p>
     * 
     * @param node       storage node entity (it can be a new object, but the id should be set properly)
     * @param lastN      number of units for the aggregate calculations (min/max/avg)
     * @param unit       units for the previous parameter it could be one of the following:
     *<ul>
     *  <li><code>MeasurementUtils.UNIT_COLLECTION_POINTS</code></li>
     *  <li><code>MeasurementUtils.UNIT_MINUTES</code></li>
     *  <li><code>MeasurementUtils.UNIT_HOURS</code></li>
     *  <li><code>MeasurementUtils.UNIT_DAYS</code></li>
     *  <li><code>MeasurementUtils.UNIT_WEEKS</code></li>
     /  <li><code>MeasurementUtils.UNIT_HOURS</code></li>
     *</ul>
     * @return instance of {@link StorageNodeLoadComposite} with the aggregate measurement data of selected metrics
     */
    StorageNodeLoadComposite getLoad(StorageNode node, int lastN, int unit) throws RuntimeException;
    
    PageList<StorageNodeLoadComposite> getStorageNodeComposites() throws RuntimeException;
    
    Integer[] findResourcesWithAlertDefinitions() throws RuntimeException;
    
    Integer[] findResourcesWithAlertDefinitions(StorageNode storageNode) throws RuntimeException;
    
    int findNotAcknowledgedStorageNodeAlertsCount() throws RuntimeException;
        
    List<Integer> findNotAcknowledgedStorageNodeAlertsCounts(List<Integer> storageNodeIds) throws RuntimeException;
    
    Map<String, List<MeasurementDataNumericHighLowComposite>> findStorageNodeLoadDataForLast(StorageNode node, int lastN, int unit, int numPoints) throws RuntimeException;
    
    StorageNodeConfigurationComposite retrieveConfiguration(StorageNode storageNode) throws RuntimeException;
    
    void updateConfiguration(StorageNodeConfigurationComposite storageNodeConfiguration) throws RuntimeException;
    
    StorageClusterSettings retrieveClusterSettings() throws RuntimeException;
    
    void updateClusterSettings(StorageClusterSettings clusterSettings) throws RuntimeException;
    
    void undeployStorageNode(StorageNode storageNode) throws RuntimeException;
    
    void deployStorageNode(StorageNode storageNode) throws RuntimeException;
}
