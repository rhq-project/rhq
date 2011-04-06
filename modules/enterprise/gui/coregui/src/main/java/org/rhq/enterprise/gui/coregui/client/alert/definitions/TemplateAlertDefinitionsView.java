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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.MetadataType;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author John Mazzitelli
 */
public class TemplateAlertDefinitionsView extends AbstractAlertDefinitionsView {

    public static final String CRITERIA_RESOURCE_TYPE_ID = "resourceTypeId";

    private ResourceType resourceType;
    private Set<Permission> globalPermissions;

    public TemplateAlertDefinitionsView(String locatorId, ResourceType resourceType) {
        super(locatorId, "Alert Templates");
        this.resourceType = resourceType;

        // make sure we loaded all the type info we'll need. if one of these is null, either the type
        // doesn't have it or we haven't loaded it yet. since we can't know for sure if it was loaded, we have to ask.
        EnumSet<MetadataType> metadata = EnumSet.noneOf(MetadataType.class);
        if (resourceType.getEventDefinitions() == null)
            metadata.add(MetadataType.events);
        if (resourceType.getMetricDefinitions() == null)
            metadata.add(MetadataType.measurements);
        if (resourceType.getOperationDefinitions() == null)
            metadata.add(MetadataType.operations);
        if (resourceType.getResourceConfigurationDefinition() == null)
            metadata.add(MetadataType.resourceConfigurationDefinition);
        if (!metadata.isEmpty()) {
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(resourceType.getId(), metadata,
                new ResourceTypeRepository.TypeLoadedCallback() {
                    @Override
                    public void onTypesLoaded(ResourceType type) {
                        TemplateAlertDefinitionsView.this.resourceType = type;
                    }
                });
        }

        this.globalPermissions = Collections.emptySet();
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            public void onPermissionsLoaded(Set<Permission> globalPermissions) {
                TemplateAlertDefinitionsView.this.globalPermissions = globalPermissions;
            }
        });
    }

    @Override
    protected ResourceType getResourceType() {
        return resourceType;
    }

    @Override
    protected Criteria getCriteria() {
        Criteria criteria = new Criteria();
        criteria.addCriteria(CRITERIA_RESOURCE_TYPE_ID, resourceType.getId());
        return criteria;
    }

    @Override
    protected AbstractAlertDefinitionsDataSource getAlertDefinitionDataSource() {
        return new TemplateAlertDefinitionsDataSource(resourceType);
    }

    @Override
    public SingleAlertDefinitionView getDetailsView(int id) {
        SingleAlertDefinitionView view = super.getDetailsView(id);
        if (id == 0) {
            // when creating a new alert def, make sure to set this in the new alert def
            view.getAlertDefinition().setResourceType(resourceType);
        }
        return view;
    }

    @Override
    protected boolean isAllowedToModifyAlertDefinitions() {
        return globalPermissions.contains(Permission.MANAGE_SETTINGS);
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

        final Integer[] alertDefIds = new Integer[selection.length];
        int i = 0;
        for (ListGridRecord record : selection) {
            Integer id = record.getAttributeAsInt(AbstractAlertDefinitionsDataSource.FIELD_ID);
            alertDefIds[i++] = id;
        }

        GWTServiceLookup.getAlertTemplateService().enableAlertTemplates(alertDefIds, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_alert_definitions_enable_success(String.valueOf(alertDefIds.length)),
                        Severity.Info));
                TemplateAlertDefinitionsView.this.refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alert_definitions_enable_failure(), caught);
            }
        });
    }

    @Override
    protected void disableButtonPressed(ListGridRecord[] selection) {
        if (selection.length == 0) {
            return;
        }

        final Integer[] alertDefIds = new Integer[selection.length];
        int i = 0;
        for (ListGridRecord record : selection) {
            Integer id = record.getAttributeAsInt(AbstractAlertDefinitionsDataSource.FIELD_ID);
            alertDefIds[i++] = id;
        }

        GWTServiceLookup.getAlertTemplateService().disableAlertTemplates(alertDefIds, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_alert_definitions_disable_success(String.valueOf(alertDefIds.length)),
                        Severity.Info));
                TemplateAlertDefinitionsView.this.refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alert_definitions_disable_failure(), caught);
            }
        });
    }

    @Override
    protected void deleteButtonPressed(ListGridRecord[] selection) {
        if (selection.length == 0) {
            return;
        }

        final Integer[] alertDefIds = new Integer[selection.length];
        int i = 0;
        for (ListGridRecord record : selection) {
            Integer id = record.getAttributeAsInt(AbstractAlertDefinitionsDataSource.FIELD_ID);
            alertDefIds[i++] = id;
        }

        GWTServiceLookup.getAlertTemplateService().removeAlertTemplates(alertDefIds, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_alert_definitions_delete_success(String.valueOf(alertDefIds.length)),
                        Severity.Info));
                TemplateAlertDefinitionsView.this.refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alert_definitions_delete_failure(), caught);
            }
        });
    }

    @Override
    protected void commitAlertDefinition(final AlertDefinition alertDefinition) {
        if (alertDefinition.getId() == 0) {
            GWTServiceLookup.getAlertTemplateService().createAlertTemplate(alertDefinition,
                Integer.valueOf(this.resourceType.getId()), new AsyncCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_alert_definitions_create_success(), Severity.Info));
                        alertDefinition.setId(result.intValue());
                        TemplateAlertDefinitionsView.this.refresh();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_alert_definitions_create_failure(), caught);
                    }
                });
        } else {
            GWTServiceLookup.getAlertTemplateService().updateAlertTemplate(alertDefinition, true,
                new AsyncCallback<AlertDefinition>() {
                    @Override
                    public void onSuccess(AlertDefinition result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_alert_definitions_update_success(), Severity.Info));
                        TemplateAlertDefinitionsView.this.refresh();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_alert_definitions_update_failure(), caught);
                    }
                });
        }
    }
}
