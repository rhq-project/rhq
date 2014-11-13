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

package org.rhq.coregui.client.alert.definitions;

import java.util.EnumSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.MetadataType;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author John Mazzitelli
 */
public class TemplateAlertDefinitionsView extends AbstractAlertDefinitionsView {

    public static final String CRITERIA_RESOURCE_TYPE_ID = "resourceTypeId";

    private ResourceType resourceType;
    private Set<Permission> globalPermissions;

    public TemplateAlertDefinitionsView(ResourceType resourceType, Set<Permission> globalPermissions) {
        super(getTitle(resourceType), getCriteria(resourceType));
        this.resourceType = resourceType;
        this.globalPermissions = globalPermissions;

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
    }

    public static String getTitle(ResourceType type) {
        return MSG.view_adminConfig_alertDefTemplates() + " [" + ResourceTypeUtility.displayName(type) + "]";
    }

    @Override
    protected boolean isAuthorizedToModifyAlertDefinitions() {
        return globalPermissions.contains(Permission.MANAGE_SETTINGS);
    }

    @Override
    protected ResourceType getResourceType() {
        return resourceType;
    }

    private static Criteria getCriteria(ResourceType type) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(CRITERIA_RESOURCE_TYPE_ID, type.getId());
        return criteria;
    }

    @Override
    protected AbstractAlertDefinitionsDataSource getAlertDefinitionDataSource() {
        return new TemplateAlertDefinitionsDataSource(resourceType);
    }

    @Override
    public SingleAlertDefinitionView getDetailsView(Integer id) {
        SingleAlertDefinitionView view = super.getDetailsView(id);
        if (id == 0) {
            // when creating a new alert def, make sure to set this in the new alert def
            view.getAlertDefinition().setResourceType(resourceType);
        }
        return view;
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

        final int[] alertDefIds = new int[selection.length];
        int i = 0;
        for (ListGridRecord record : selection) {
            int id = record.getAttributeAsInt(AbstractAlertDefinitionsDataSource.FIELD_ID);
            alertDefIds[i++] = id;
        }

        GWTServiceLookup.getAlertDefinitionService().enableAlertDefinitions(alertDefIds, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer v) {
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

        final int[] alertDefIds = new int[selection.length];
        int i = 0;
        for (ListGridRecord record : selection) {
            int id = record.getAttributeAsInt(AbstractAlertDefinitionsDataSource.FIELD_ID);
            alertDefIds[i++] = id;
        }

        GWTServiceLookup.getAlertDefinitionService().disableAlertDefinitions(alertDefIds, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer v) {
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

        final int[] alertDefIds = new int[selection.length];
        int i = 0;
        for (ListGridRecord record : selection) {
            int id = record.getAttributeAsInt(AbstractAlertDefinitionsDataSource.FIELD_ID);
            alertDefIds[i++] = id;
        }

        GWTServiceLookup.getAlertDefinitionService().removeAlertDefinitions(alertDefIds, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer v) {
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
    protected void commitAlertDefinition(final AlertDefinition alertDefinition, boolean resetMatching,
        final AsyncCallback<AlertDefinition> resultReceiver) {
        AlertDefinition newAlertDefinition = new AlertDefinition(alertDefinition);
        newAlertDefinition.setId(alertDefinition.getId());
        ResourceType fakeResourceType = new ResourceType();
        fakeResourceType.setId(resourceType.getId());
        newAlertDefinition.setResourceType(fakeResourceType); // this was causing the serialization issues in GWT 2.5.0 (bz1058318)
        // the 3 lines above can go away after update to >= GWT 2.6.0 rc3
        
        if (alertDefinition.getId() == 0) {
            GWTServiceLookup.getAlertTemplateService().createAlertTemplate(alertDefinition,
                Integer.valueOf(this.resourceType.getId()), new AsyncCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_alert_definitions_create_success(), Severity.Info));
                        alertDefinition.setId(result.intValue());
                        TemplateAlertDefinitionsView.this.refresh();
                        resultReceiver.onSuccess(alertDefinition);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_alert_definitions_create_failure(), caught);
                        resultReceiver.onFailure(caught);
                    }
                });
        } else {
            GWTServiceLookup.getAlertTemplateService().updateAlertTemplate(alertDefinition, resetMatching,
                new AsyncCallback<AlertDefinition>() {
                    @Override
                    public void onSuccess(AlertDefinition result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_alert_definitions_update_success(), Severity.Info));
                        TemplateAlertDefinitionsView.this.refresh();
                        resultReceiver.onSuccess(result);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_alert_definitions_update_failure(), caught);
                        resultReceiver.onFailure(caught);
                    }
                });
        }
    }

    protected AlertDefinitionCriteria getDetailCriteria() {
        AlertDefinitionCriteria criteria = super.getDetailCriteria();
        criteria.addFilterAlertTemplateOnly(true);
        return criteria;
    }

}
