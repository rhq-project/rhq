/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.storage;

import java.util.ArrayList;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.GroupStartOpen;
import com.smartgwt.client.types.ImageStyle;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.SummaryFunction;

import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.alert.AlertDataSource;
import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;

/**
 * @author Jirka Kremser
 *
 */
public class StorageNodeAlertHistoryView extends AlertHistoryView {
    private boolean isGouped = true;
    
    public StorageNodeAlertHistoryView(String tableTitle, int[] resourceIds) {
        super(tableTitle, resourceIds);
    }
        
    @Override
    public AlertDataSource getDataSource() {
        return new AlertDataSource() {
            @Override
            public ArrayList<ListGridField> getListGridFields() {
                ArrayList<ListGridField> fields = super.getListGridFields();
                ArrayList<ListGridField> newFields = new ArrayList<ListGridField>(fields.size());
                for (ListGridField field : fields) {
                    if ("priority".equals(field.getName())
                        || AncestryUtil.RESOURCE_NAME.equals(field.getName())
                        || AncestryUtil.RESOURCE_ANCESTRY.equals(field.getName())) {
                        continue;
                    } if (AlertCriteria.SORT_FIELD_CTIME.equals(field.getName())) {
                        field.setWidth(240);
                        field.setShowGridSummary(true);  
                        field.setShowGroupSummary(true);
                        field.setSummaryFunction(new SummaryFunction() {  
                            public Object getSummaryValue(Record[] records, ListGridField field) {
                                if (records != null && records.length > 0 && records[0] != null) {
                                    Integer resourceId = records[0].getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                                    Integer defId = records[0].getAttributeAsInt("definitionId");
                                    String url = LinkManager.getSubsystemAlertDefinitionLink(resourceId, defId);
                                    return LinkManager.getHref(url, "Link to Definition");
                                } else return "";
                            }  
                        });
                        field.setCellFormatter(new CellFormatter() {
                            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                                if (listGridRecord.getAttribute("groupValue") != null) {
                                    return (String) o;
                                }
                                Integer resourceId = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                                Integer defId = listGridRecord.getAttributeAsInt("definitionId");
                                String url = LinkManager.getSubsystemAlertDefinitionLink(resourceId, defId);
                                return LinkManager.getHref(url, o.toString());
                            }
                        });
                        field.setWidth(240);
                    } else if ("conditionValue".equals(field.getName())) {
                        field.setWidth(140);
                    } else if ("acknowledgingSubject".equals(field.getName())) {
                        field.setSummaryFunction(new SummaryFunction() {  
                            public Object getSummaryValue(Record[] records, ListGridField field) {
                                int count = 0;
                                for (Record record : records) {    
                                    if (record.getAttribute("acknowledgingSubject") != null) {
                                        count++;
                                    }
                                }
                                return "(" + count + " / " + records.length + ")";
                            }  
                        });
                        field.setCellFormatter(new CellFormatter() {
                            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                                if (listGridRecord.getAttribute("groupValue") != null) {
                                    return (String) o;
                                }
                                String ackSubject = listGridRecord.getAttribute("acknowledgingSubject");
                                if (ackSubject == null) {
                                    return "&nbsp;";
                                } else {
                                    Img checkedImg = new Img(ImageManager.getAlertStatusCheckedIcon(), 80, 16);
                                    checkedImg.setImageType(ImageStyle.CENTER);
                                    return checkedImg.getInnerHTML();
                                }
                            }
                        });
                        
                        field.setShowGridSummary(false);  
                        field.setShowGroupSummary(true);
                        field.setWidth(90);
                        newFields.add(1, field);
                        continue;
                    } else if ("name".equals(field.getName())) {
                        field.setCellFormatter(new CellFormatter() {
                            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                                return o.toString();
                            }
                        });
                        field.setHidden(true);
                    }
                    newFields.add(field);
                }
                ListGridField descriptionField = new ListGridField("description", MSG.common_title_description());
                descriptionField.setCanSortClientOnly(true);
                newFields.add(descriptionField);
                return newFields;
            }
        };
    }
    
    @Override
    protected void configureListGrid(ListGrid grid) {
        ListGrid listGrid = super.getListGrid();
        listGrid.setGroupStartOpen(GroupStartOpen.ALL);
        listGrid.setShowGroupSummary(true);  
        listGrid.setShowGroupSummaryInHeader(true);
        
        listGrid.setGroupByField("name");
    }
    
    @Override
    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                if (value == null) {
                    return "";
                }
                if (record.getAttribute("groupValue") != null) {
                    return value.toString();
                }
                String detailsUrl = getDetailUrlFromRecord(record);
                String formattedValue = StringUtility.escapeHtml(value.toString());
                return LinkManager.getHref(detailsUrl, formattedValue);
            }
        };
    }
    
    @Override
    public void showDetails(ListGridRecord record) {
        CoreGUI.goToView(getDetailUrlFromRecord(record));
    }
    
    private String getDetailUrlFromRecord(ListGridRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("'record' parameter is null.");
        }
        Integer recordId = getId(record);
        Integer resourceId = record.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
        if (recordId != null && recordId.intValue() > 0 && resourceId != null && resourceId > 0) {
            return "#Resource/" + resourceId + "/Alerts/History/" + convertIDToCurrentViewPath(recordId);
        } else {
            String msg = MSG.view_tableSection_error_badId(this.getClass().toString(), (recordId == null) ? "null"
                : recordId.toString());
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalArgumentException(msg);
        }
    }
    
    @Override
    protected void configureTable() {
        super.configureTable();
        addTableAction("(Un)Group Alerts", new AbstractTableAction(TableActionEnablement.ALWAYS) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                if (isGouped) {
                    getListGrid().ungroup();
                } else {
                    getListGrid().groupBy("name");
                }
                isGouped = !isGouped;
                refreshTableInfo();
            }
        });
    }
}
