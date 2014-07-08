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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.coregui.client.alert.AlertFormatUtility;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.MetadataType;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author John Mazzitelli
 */
public class ConditionsEditor extends EnhancedVLayout {

    private final ResourceType resourceType;
    private HashSet<AlertCondition> conditions;
    private Map<Integer, AlertCondition> modifiedConditions;
    private Table<ConditionDataSource> table;
    private final SelectItem conditionExpression;
    private boolean updated;

    public ConditionsEditor(SelectItem conditionExpression, ResourceType resourceType,
        HashSet<AlertCondition> conditions) {
        super();
        this.conditionExpression = conditionExpression;
        this.resourceType = resourceType;
        this.updated = false;
        setConditions(conditions);
        modifiedConditions = new HashMap<Integer, AlertCondition>();
    }

    /**
     * Returns the conditions that this editor currently has in memory.
     * This will never be <code>null</code>. This collection serves for new
     * or deleted conditions.
     * 
     * @return conditions set that was possibly edited by the user
     */
    public HashSet<AlertCondition> getConditions() {
        return conditions;
    }

    /**
     * Returns the conditions that this editor currently has in memory.
     * This will never be <code>null</code>. This collection holds modified
     * existing conditions.
     * 
     * @return modifiedConditions map of modified conditions that exist in the db, key is id
     */
    public Map<Integer, AlertCondition> getModifiedConditions() {
        return modifiedConditions;
    }

    public void setConditions(Set<AlertCondition> set) {
        conditions = new HashSet<AlertCondition>(); // make our own copy
        modifiedConditions = new HashMap<Integer, AlertCondition>(conditions.size()); //reset changes
        if (set != null) {

            // we need to make sure we have the full measurement definition, including the units.
            // this is so we can display the condition values with the proper units. If an alert
            // condition is a measurement condition, but the measurement definition isn't the full
            // definition, look up the full definition from the resource type and use it.
            for (AlertCondition alertCondition : set) {
                MeasurementDefinition measDef = alertCondition.getMeasurementDefinition();
                if (measDef != null) {
                    if (measDef.getUnits() == null) {
                        MeasurementDefinition fullMeasDef = findMeasurementDefinition(measDef.getId());
                        if (fullMeasDef != null) {
                            alertCondition.setMeasurementDefinition(fullMeasDef);
                        }
                    }
                }
            }

            conditions.addAll(set);
        }
        if (table != null) {
            table.refresh();
        }
    }

    private MeasurementDefinition findMeasurementDefinition(int measurementDefinitionId) {
        Set<MeasurementDefinition> measDefs = this.resourceType.getMetricDefinitions();
        if (measDefs != null) {
            for (MeasurementDefinition measDef : measDefs) {
                if (measDef.getId() == measurementDefinitionId) {
                    return measDef;
                }
            }
        }
        return null;
    }

    @Override
    protected void onInit() {
        super.onInit();

        table = new ConditionsTable();

        addMember(table);
    }

    public void setEditable(boolean editable) {
        table.setTableActionDisableOverride(!editable);
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public boolean isConditionInternallyUpdated() {
        return !modifiedConditions.isEmpty();
    }

    private class ConditionsTable extends Table<ConditionDataSource> {
        private ConditionsTable() {
            super();

            setShowHeader(false);

            final ConditionDataSource dataSource = new ConditionDataSource();
            setDataSource(dataSource);
        }

        @Override
        protected void configureTable() {
            addTableAction(MSG.common_button_add(), null, ButtonColor.BLUE, new AbstractTableAction() {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    showConditionEditor(null);
                }
            });

            table.addTableAction(MSG.common_button_delete(), MSG
                .view_alert_definition_condition_editor_delete_confirm(), ButtonColor.RED, new AbstractTableAction(
                TableActionEnablement.ANY) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    for (ListGridRecord record : selection) {
                        AlertCondition condition = getDataSource().copyValues(record);
                        conditions.remove(condition);
                        modifiedConditions.remove(condition);
                        updated = true;
                    }
                    refresh();
                }
            });

            table.addTableAction(MSG.view_alert_definition_editCondition(), null, ButtonColor.GRAY, new AbstractTableAction(TableActionEnablement.SINGLE) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    AlertCondition condition = getDataSource().copyValues(selection[0]);
                    showConditionEditor(condition);
                }
            });

        }

        private void showConditionEditor(final AlertCondition existingCondition) {

            // we need the drift definition templates (if there are any) so we know if we should offer drift conditions as an option
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(resourceType.getId(),
                EnumSet.of(MetadataType.driftDefinitionTemplates), new ResourceTypeRepository.TypeLoadedCallback() {
                    @Override
                    public void onTypesLoaded(ResourceType type) {
                        // the resource type repo caches types - so if this resource type was already cached prior
                        // to the conditions editor component created (which it probably was) then we are getting the same
                        // exact instance that we had before (resourceType). When this happens, great! Our resourceType
                        // instance will have its drift definition templates populated. But, I'm being paranoid. If somehow
                        // we have a resourceType that is different than the type being passed to us, we need to copy
                        // the drift definition.
                        if (type != resourceType) {
                            // paranoia, unsure if this is needed but clear out any old drift definition still hanging around
                            if (resourceType.getDriftDefinitionTemplates() != null) {
                                resourceType.getDriftDefinitionTemplates().clear();
                            }
                            // if the newly loaded resource type supports drift, put it in our type object
                            if (type.getDriftDefinitionTemplates() != null) {
                                for (DriftDefinitionTemplate template : type.getDriftDefinitionTemplates()) {
                                    resourceType.addDriftDefinitionTemplate(template);
                                }
                            }
                        }
                        final Window winModal = new Window();
                        winModal.setTitle(existingCondition == null ? MSG
                            .view_alert_common_tab_conditions_modal_title() : MSG
                            .view_alert_common_tab_conditions_modalEdit_title());
                        winModal.setOverflow(Overflow.VISIBLE);
                        winModal.setShowMinimizeButton(false);
                        winModal.setIsModal(true);
                        winModal.setShowModalMask(true);
                        winModal.setAutoSize(true);
                        winModal.setAutoCenter(true);
                        winModal.centerInPage();
                        winModal.addCloseClickHandler(new CloseClickHandler() {
                            @Override
                            public void onCloseClick(CloseClickEvent event) {
                                winModal.markForDestroy();
                                refreshTableInfo();
                            }
                        });

                        final int numConditions = conditions.size();
                        final ConditionEditor newConditionEditor = new ConditionEditor(conditions, modifiedConditions,
                            ConditionsEditor.this.conditionExpression, ConditionsEditor.this.resourceType,
                            existingCondition, new Runnable() {
                                @Override
                                public void run() {
                                    updated = updated || numConditions != conditions.size()
                                        || isConditionInternallyUpdated();
                                    winModal.markForDestroy();
                                    refresh();
                                }
                            });
                        winModal.addItem(newConditionEditor);
                        winModal.show();
                    }
                });
        }
    }

    private class ConditionDataSource extends RPCDataSource<AlertCondition, Criteria> {
        private static final String FIELD_OBJECT = "obj";
        private static final String FIELD_CONDITION = "condition";

        public ConditionDataSource() {
            super();
            List<DataSourceField> fields = addDataSourceFields();
            addFields(fields);
        }

        @Override
        protected List<DataSourceField> addDataSourceFields() {
            List<DataSourceField> fields = super.addDataSourceFields();

            DataSourceTextField conditionField = new DataSourceTextField(FIELD_CONDITION,
                MSG.view_alert_common_tab_conditions_text());
            fields.add(conditionField);

            return fields;
        }

        @Override
        public AlertCondition copyValues(Record from) {
            return (AlertCondition) from.getAttributeAsObject(FIELD_OBJECT);
        }

        @Override
        public ListGridRecord copyValues(AlertCondition from) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute(FIELD_CONDITION, AlertFormatUtility.formatAlertConditionForDisplay(from));
            record.setAttribute(FIELD_OBJECT, from);
            return record;
        }

        @Override
        protected void executeFetch(DSRequest request, DSResponse response, Criteria unused) {
            response.setData(buildRecords(conditions));
            processResponse(request.getRequestId(), response);
        }

        @Override
        protected Criteria getFetchCriteria(DSRequest request) {
            // we don't use criterias for this datasource, just return null
            return null;
        }
    }
}
