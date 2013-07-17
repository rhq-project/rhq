/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.List;

import org.rhq.core.clientapi.util.ArrayUtil;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.coregui.client.gwt.StorageGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jirka Kremser
 */
public class StorageGWTServiceImpl extends AbstractGWTServiceImpl implements StorageGWTService {

    private static final long serialVersionUID = 1L;
    
    private StorageNodeManagerLocal storageNodeManager = LookupUtil.getStorageNodeManager();
    
    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    @Override
    public PageList<StorageNode> findStorageNodesByCriteria(StorageNodeCriteria criteria) throws RuntimeException {
        try {
            return SerialUtility.prepare(storageNodeManager.findStorageNodesByCriteria(getSessionSubject(), criteria),
                "StorageGWTServiceImpl.findStorageNodesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void invokeOperationOnStorageService(int storageNodeId, String operationName) throws RuntimeException {
        try {
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterParentResourceId(storageNodeId);
            criteria.addFilterResourceTypeName("StorageService");
            List<Resource> resources = resourceManager.findResourcesByCriteria(getSessionSubject(), criteria);
            if (resources == null || resources.size() != 1) {
                throw new IllegalStateException(
                    "There is not just one resources of type StorageService among child resources of resource with id "
                        + storageNodeId);
            }
            operationManager.scheduleResourceOperation(getSessionSubject(), resources.get(0).getId(), operationName, 0,
                0, 0, 0, null, "Run by Storage Node Administrations UI");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public StorageNodeLoadComposite getLoad(StorageNode node, int lastN, int unit)
        throws RuntimeException {
        try {
            List<Long> beginEnd = MeasurementUtils.calculateTimeFrame(lastN, unit);
            return SerialUtility.prepare(storageNodeManager.getLoad(getSessionSubject(), node, beginEnd.get(0), beginEnd.get(1)),
                "StorageGWTServiceImpl.getLoad");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
    
    @Override
    public PageList<StorageNodeLoadComposite> getStorageNodeComposites() throws RuntimeException {
        try {
            return SerialUtility.prepare(storageNodeManager.getStorageNodeComposites(),
                "StorageGWTServiceImpl.getStorageNodeComposites");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
    
    @Override
    public Integer[] findResourcesWithAlertDefinitions() throws RuntimeException {
        try {
            return storageNodeManager.findResourcesWithAlertDefinitions();
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}
