/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.bundle;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentHistory;
import org.rhq.core.domain.criteria.BundleDeploymentHistoryCriteria;

/**
 * Local interface to the manager responsible for creating and managing bundles.
 *  
 * @author Adam YOung
 */
@Local
public interface BundleDeploymentHistoryManagerLocal {

    void addBundleDeploymentHistoryByBundleDeployment(BundleDeploymentHistory history) throws IllegalArgumentException;

    List<BundleDeploymentHistory> findBundleDeploymentHistoryByBundleDeployment(BundleDeployment bundleDeployment);

    List<BundleDeploymentHistory> findBundleDeploymentHistoryByBundleAndPlatform(int bundleId, int platformResourceId);

    List<BundleDeploymentHistory> findBundleDeploymentHistoryByPlatform(int platformResourceId);

    List<BundleDeploymentHistory> findBundleDeploymentHistoryByCriteria(Subject subject,
        BundleDeploymentHistoryCriteria criteria);

}
