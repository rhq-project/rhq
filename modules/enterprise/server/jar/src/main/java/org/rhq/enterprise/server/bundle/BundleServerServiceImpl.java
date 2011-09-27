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
package org.rhq.enterprise.server.bundle;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.util.exception.WrappedRemotingException;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility.SerializationType;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Server-side implementation of the <code>BundleServerService</code>. This implmentation simply forwards
 * the requests to the appropriate session bean.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
public class BundleServerServiceImpl implements BundleServerService {
    private final Log log = LogFactory.getLog(this.getClass());

    public void addDeploymentHistory(int bundleDeploymentId, BundleResourceDeploymentHistory history) {
        try {
            BundleManagerLocal bm = LookupUtil.getBundleManager();
            bm.addBundleResourceDeploymentHistory(LookupUtil.getSubjectManager().getOverlord(), bundleDeploymentId,
                history);
        } catch (Exception e) {
            log.error("Failed to add history to deployment id: " + bundleDeploymentId, e);
            throw new WrappedRemotingException(e);
        }
    }

    public List<PackageVersion> getAllBundleVersionPackageVersions(int bundleVersionId) {
        try {
            BundleManagerLocal bm = LookupUtil.getBundleManager();
            ContentManagerLocal cm = LookupUtil.getContentManager();
            Subject subject = LookupUtil.getSubjectManager().getOverlord();
            BundleFileCriteria bfc = new BundleFileCriteria();
            PackageVersionCriteria pvc = new PackageVersionCriteria();

            bfc.addFilterBundleVersionId(bundleVersionId);
            bfc.fetchPackageVersion(true);
            List<BundleFile> bundleFiles = bm.findBundleFilesByCriteria(subject, bfc);
            List<PackageVersion> packageVersions = new ArrayList<PackageVersion>(bundleFiles.size());
            PackageVersion packageVersion = null;
            for (BundleFile bundleFile : bundleFiles) {
                pvc.addFilterId(bundleFile.getPackageVersion().getId());
                packageVersion = cm.findPackageVersionsByCriteria(subject, pvc).get(0);
                HibernateDetachUtility.nullOutUninitializedFields(packageVersion, SerializationType.SERIALIZATION);
                packageVersions.add(packageVersion);
            }
            return packageVersions;
        } catch (Exception e) {
            log.error("Failed to obtain bundle files for bundle version id: " + bundleVersionId, e);
            throw new WrappedRemotingException(e);
        }
    }

    public long downloadPackageBits(PackageVersion packageVersion, OutputStream outputStream) {
        try {
            ContentSourceManagerLocal csm = LookupUtil.getContentSourceManager();
            long size = csm.outputPackageVersionBits(packageVersion, outputStream);
            return size;
        } catch (Exception e) {
            log.error("Failed to obtain package version bits for package version: " + packageVersion, e);
            throw new WrappedRemotingException(e);
        }
    }

    public void setBundleDeploymentStatus(int bundleDeploymentId, BundleDeploymentStatus status) {
        try {
            BundleManagerLocal bm = LookupUtil.getBundleManager();
            bm.setBundleResourceDeploymentStatus(LookupUtil.getSubjectManager().getOverlord(), bundleDeploymentId,
                status);
        } catch (Exception e) {
            log.error("Failed to set status for deployment id: " + bundleDeploymentId, e);
            throw new WrappedRemotingException(e);
        }
    }

}