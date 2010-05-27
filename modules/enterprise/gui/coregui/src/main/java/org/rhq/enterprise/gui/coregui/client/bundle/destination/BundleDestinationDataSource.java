/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.bundle.destination;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class BundleDestinationDataSource extends RPCDataSource<BundleDestination> {

    public BundleDestinationDataSource() {

        DataSourceIntegerField idField = new DataSourceIntegerField("id", "Id");
        idField.setPrimaryKey(true);
        addField(idField);

        DataSourceTextField name = new DataSourceTextField("name", "Name");
        addField(name);

        DataSourceTextField description = new DataSourceTextField("description", "Description");
        addField(description);

        DataSourceTextField bundle = new DataSourceTextField("bundleName", "Bundle");
        addField(bundle);

        DataSourceTextField group = new DataSourceTextField("groupName", "Group");
        addField(group);

        DataSourceTextField deployDir = new DataSourceTextField("deployDir", "Deploy Directory");
        addField(deployDir);

        DataSourceTextField latestDeploymentVersion = new DataSourceTextField("latestDeploymentVersion", "Last Deployed Version");
        addField(latestDeploymentVersion);

        DataSourceTextField latestDeploymentDate = new DataSourceTextField("latestDeploymentDate", "Last Deployment Date");
        addField(latestDeploymentDate);

        DataSourceTextField latestDeploymentStatus = new DataSourceTextField("latestDeploymentStatus", "Last Deployment Status");
        addField(latestDeploymentStatus);

    }


    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        BundleDestinationCriteria criteria = new BundleDestinationCriteria();

        if (request.getCriteria().getValues().containsKey("bundleId")) {
            criteria.addFilterBundleId(Integer.parseInt(request.getCriteria().getAttributeAsString("bundleId")));
        }

         if (request.getCriteria().getValues().get("tagNamespace") != null) {
            criteria.addFilterTagNamespace((String) request.getCriteria().getValues().get("tagNamespace"));
        }

        if (request.getCriteria().getValues().get("tagSemantic") != null) {
            criteria.addFilterTagSemantic((String) request.getCriteria().getValues().get("tagSemantic"));
        }

        if (request.getCriteria().getValues().get("tagName") != null) {
            criteria.addFilterTagName((String) request.getCriteria().getValues().get("tagName"));
        }


        criteria.fetchBundle(true);
        criteria.fetchDeployments(true);
        criteria.fetchGroup(true);
        criteria.fetchTags(true);


        GWTServiceLookup.getBundleService().findBundleDestinationsByCriteria(criteria,
                new AsyncCallback<PageList<BundleDestination>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load bundle destinations",caught);
                    }

                    public void onSuccess(final PageList<BundleDestination> result) {

                        BundleVersionCriteria versionCriteria = new BundleVersionCriteria();
                        if (request.getCriteria().getValues().containsKey("bundleId")) {
                            versionCriteria.addFilterBundleId(Integer.parseInt(request.getCriteria().getAttributeAsString("bundleId")));
                        }
                        GWTServiceLookup.getBundleService().findBundleVersionsByCriteria(versionCriteria, new AsyncCallback<PageList<BundleVersion>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to load bundle destination deployed version info",caught);
                            }

                            public void onSuccess(PageList<BundleVersion> versions) {

                                for (BundleDestination dest : result) {

                                    for (BundleDeployment dep : dest.getDeployments()) {

                                        for (BundleVersion version : versions) {
                                            if (dep.getBundleVersion().getId() == version.getId()) {
                                                dep.setBundleVersion(version);
                                            }
                                        }
                                    }
                                }

                                response.setData(buildRecords(result));
                                processResponse(request.getRequestId(), response);

                            }
                        });
                    }
                });
    }

    @Override
    public BundleDestination copyValues(ListGridRecord from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(BundleDestination from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("bundleId", from.getBundle().getId());
        record.setAttribute("bundleName", from.getBundle().getName());

        record.setAttribute("groupId", from.getGroup().getId());
        record.setAttribute("groupName", from.getGroup().getName());

        record.setAttribute("deployDir", from.getDeployDir());
        record.setAttribute("entity", from);


        long last = 0;
        for (BundleDeployment dep : from.getDeployments()) {
            if (last < dep.getCtime()) {
                last = dep.getCtime();

                record.setAttribute("latestDeployment", dep);
                record.setAttribute("latestDeploymentVersion", dep.getBundleVersion().getVersion());
                record.setAttribute("latestDeploymentDate", new Date(dep.getCtime()));
                record.setAttribute("latestDeploymentStatus", dep.getStatus().name());

            }
        }


        return record;
    }
}
