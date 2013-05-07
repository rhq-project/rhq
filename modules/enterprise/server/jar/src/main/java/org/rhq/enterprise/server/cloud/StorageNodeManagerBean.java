/*
 *
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
 *
 */
package org.rhq.enterprise.server.cloud;

import java.util.Collection;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.enterprise.server.RHQConstants;

@Stateless
public class StorageNodeManagerBean {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    @Nullable
    public List<StorageNode> getStorageNodes() {
        Query query = entityManager.createNamedQuery(StorageNode.QUERY_FIND_ALL);
        return (List<StorageNode>) query.getResultList();
    }

    public void updateStorageNodeList(Collection<StorageNode> storageNodes) {
        for (StorageNode storageNode : storageNodes) {
            entityManager.persist(storageNode);
        }
    }
}
