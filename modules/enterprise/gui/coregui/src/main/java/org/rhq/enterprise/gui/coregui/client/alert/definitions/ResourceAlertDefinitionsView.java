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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.MetadataType;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author John Mazzitelli
 */
public class ResourceAlertDefinitionsView extends AbstractAlertDefinitionsView {

    public static final String CRITERIA_RESOURCE_ID = "resourceId";

    private Resource resource;
    private ResourcePermission permissions;

    public ResourceAlertDefinitionsView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId, "Alert Definitions");
        this.resource = resourceComposite.getResource();
        this.permissions = resourceComposite.getResourcePermission();

        // make sure we loaded all the type info we'll need. if one of these is null, either the type
        // doesn't have it or we haven't loaded it yet. since we can't know for sure if it was loaded, we have to ask.
        ResourceType rt = this.resource.getResourceType();
        EnumSet<MetadataType> metadata = EnumSet.noneOf(MetadataType.class);
        if (rt.getEventDefinitions() == null)
            metadata.add(MetadataType.events);
        if (rt.getMetricDefinitions() == null)
            metadata.add(MetadataType.measurements);
        if (rt.getOperationDefinitions() == null)
            metadata.add(MetadataType.operations);
        if (rt.getResourceConfigurationDefinition() == null)
            metadata.add(MetadataType.resourceConfigurationDefinition);
        if (!metadata.isEmpty()) {
            ArrayList<Resource> list = new ArrayList<Resource>(1);
            list.add(this.resource);
            ResourceTypeRepository.Cache.getInstance().loadResourceTypes(list, metadata, null);
        }
    }

    @Override
    protected ResourceType getResourceType() {
        return resource.getResourceType();
    }

    @Override
    protected Criteria getCriteria() {
        Criteria criteria = new Criteria();
        criteria.addCriteria(CRITERIA_RESOURCE_ID, resource.getId());
        return criteria;
    }

    @Override
    protected AbstractAlertDefinitionsDataSource getAlertDefinitionDataSource() {
        return new ResourceAlertDefinitionsDataSource(this.resource);
    }

    @Override
    public SingleAlertDefinitionView getDetailsView(int id) {
        SingleAlertDefinitionView view = super.getDetailsView(id);
        if (id == 0) {
            // when creating a new alert def, make sure to set this in the new alert def
            view.getAlertDefinition().setResource(resource);
        }
        return view;
    }

    @Override
    protected boolean isAllowedToModifyAlertDefinitions() {
        return this.permissions.isAlert();
    }

    @Override
    protected void newButtonPressed(ListGridRecord[] selection) {
        newDetails();
    }

    @Override
    protected void enableButtonPressed(ListGridRecord[] selection) {
        if (selection.length == 0) {
            return;
        }

        int[] alertDefIds = new int[selection.length];
        int i = 0;
        for (ListGridRecord record : selection) {
            Integer id = record.getAttributeAsInt(AbstractAlertDefinitionsDataSource.FIELD_ID);
            alertDefIds[i++] = id;
        }
        GWTServiceLookup.getAlertDefinitionService().enableAlertDefinitions(alertDefIds, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("[" + result + "] alert definitions enabled.", Severity.Info));
                ResourceAlertDefinitionsView.this.refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to enable alert definitions", caught);
            }
        });
    }

    @Override
    protected void disableButtonPressed(ListGridRecord[] selection) {
        if (selection.length == 0) {
            return;
        }

        int[] alertDefIds = new int[selection.length];
        int i = 0;
        for (ListGridRecord record : selection) {
            Integer id = record.getAttributeAsInt(AbstractAlertDefinitionsDataSource.FIELD_ID);
            alertDefIds[i++] = id;
        }
        GWTServiceLookup.getAlertDefinitionService().disableAlertDefinitions(alertDefIds, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("[" + result + "] alert definitions disabled.", Severity.Info));
                ResourceAlertDefinitionsView.this.refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to disable alert definitions.", caught);
            }
        });
    }

    @Override
    protected void deleteButtonPressed(ListGridRecord[] selection) {
        if (selection.length == 0) {
            return;
        }

        int[] alertDefIds = new int[selection.length];
        int i = 0;
        for (ListGridRecord record : selection) {
            Integer id = record.getAttributeAsInt(AbstractAlertDefinitionsDataSource.FIELD_ID);
            alertDefIds[i++] = id;
        }
        GWTServiceLookup.getAlertDefinitionService().removeAlertDefinitions(alertDefIds, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("[" + result + "] alert definitions deleted.", Severity.Info));
                ResourceAlertDefinitionsView.this.refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to delete alert definitions.", caught);
            }
        });
    }

    @Override
    protected void commitAlertDefinition(final AlertDefinition alertDefinition) {
        if (alertDefinition.getId() == 0) {
            GWTServiceLookup.getAlertDefinitionService().createAlertDefinition(alertDefinition,
                Integer.valueOf(resource.getId()), new AsyncCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer result) {
                        CoreGUI.getMessageCenter().notify(new Message("Alert definition is created", Severity.Info));
                        alertDefinition.setId(result.intValue());
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to create alert definition.", caught);
                    }
                });
        } else {
            GWTServiceLookup.getAlertDefinitionService().updateAlertDefinition(alertDefinition.getId(),
                alertDefinition, true, new AsyncCallback<AlertDefinition>() {
                    @Override
                    public void onSuccess(AlertDefinition result) {
                        CoreGUI.getMessageCenter().notify(new Message("Alert definition is updated.", Severity.Info));
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to update alert definition.", caught);
                    }
                });
        }
    }
}
