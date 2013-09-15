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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.GroupStartOpen;
import com.smartgwt.client.types.ImageStyle;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.SummaryFunction;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.alert.AlertDataSource;
import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.components.form.DateFilterItem;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * The view for accessing alerts on storage node resource and its children.
 * 
 * @author Jirka Kremser
 */
public class StorageNodeAlertHistoryView extends AlertHistoryView {
    private boolean isGouped = true;
    private final HTMLFlow header;
    private final int storageNodeId;
    private final boolean allStorageNodes;
    private Map<Integer, Integer> resourceIdToStorageNodeIdMap;
    private Map<Integer, String> storageNodeIdToAddressMap;
    
    public StorageNodeAlertHistoryView(String tableTitle, Map<Integer, Integer> resourceIdToStorageNodeIdMap) {
        this(tableTitle, ArrayUtils.unwrapArray(resourceIdToStorageNodeIdMap.keySet().toArray(new Integer[]{})), null, -1);
        this.resourceIdToStorageNodeIdMap = resourceIdToStorageNodeIdMap;
    }
    
    public StorageNodeAlertHistoryView(String tableTitle, int[] resourceIds, HTMLFlow header, int storageNodeId) {
        super(tableTitle, resourceIds);
        this.header = header;
        this.storageNodeId = storageNodeId;
        this.allStorageNodes = storageNodeId == -1;
        storageNodeIdToAddressMap = new HashMap<Integer, String>();
    }
    
    @Override
    protected void onInit() {
        super.onInit();
        fetchAddresses();
    }
    
    @Override
    protected void configureTableFilters() {
        startDateFilter = new DateFilterItem(DateFilterItem.START_DATE_FILTER, MSG.filter_from_date());
        endDateFilter = new DateFilterItem(DateFilterItem.END_DATE_FILTER, MSG.filter_to_date());

        SpacerItem spacerItem = new SpacerItem();
        spacerItem.setColSpan(2);

        if (isShowFilterForm()) {
            setFilterFormItems(startDateFilter, spacerItem, endDateFilter);
        }
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
//                        field.setShowGridSummary(true);
//                        field.setShowGroupSummary(true);
//                        field.setSummaryFunction(new SummaryFunction() {
//                            public Object getSummaryValue(Record[] records, ListGridField field) {
//                                if (records != null && records.length > 0 && records[0] != null) {
//                                    http://localhost:7080/coregui/#Administration/Configuration/AlertDefTemplates/10190/10002
//                                        
//                                    Integer resourceId = records[0].getAttributeAsInt(AncestryUtil.RESOURCE_ID);
//                                    Integer defId = records[0].getAttributeAsInt("definitionId");
//                                    String url = LinkManager.getSubsystemAlertDefinitionLink(resourceId, defId);
//                                    return LinkManager.getHref(url, "Link to Definition");
//                                } else return "";
//                            }  
//                        });
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
                
                if (allStorageNodes) { // all storage nodes
                    ListGridField storageNodeLinkField = new ListGridField("storageNodeLink", "Storage Node");
                    storageNodeLinkField.setCellFormatter(new CellFormatter() {
                        public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                            if (listGridRecord.getAttribute("groupValue") != null) {
                                return (String) o;
                            }
                            Integer resourceId = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                            int storageNodeId = resourceIdToStorageNodeIdMap.get(resourceId);
                            String url = LinkManager.getStorageNodeLink(storageNodeId);
                            return LinkManager.getHref(url, storageNodeIdToAddressMap.get(storageNodeId));

                        }
                    });
                    storageNodeLinkField.setWidth(90);
                    newFields.add(2, storageNodeLinkField);
                }
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
    
    private void fetchAddresses() {
        if (header != null && !allStorageNodes) {
            StorageNodeCriteria criteria = new StorageNodeCriteria();
            criteria.addFilterId(storageNodeId);
            GWTServiceLookup.getStorageService().findStorageNodesByCriteria(criteria,
                new AsyncCallback<PageList<StorageNode>>() {
                    public void onSuccess(final PageList<StorageNode> storageNodes) {
                        if (storageNodes == null || storageNodes.isEmpty() || storageNodes.size() != 1) {
                            Message msg = new Message(MSG.view_adminTopology_message_fetchServerFail(String
                                .valueOf(storageNodeId)), Message.Severity.Error);
                            CoreGUI.goToView(StorageNodeTableView.VIEW_PATH, msg);
                            return;
                        }
                        final StorageNode node = storageNodes.get(0);
                        header
                            .setContents("<div style='text-align: center; font-weight: bold; font-size: medium;'> Storage Node ("
                                + node.getAddress() + ")</div>");
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)) + " "
                                + caught.getMessage(), caught);
                    }
                });
        } else { // fetch the addresses of all storage nodes
            GWTServiceLookup.getStorageService().getStorageNodes(new AsyncCallback<List<StorageNode>>() {
                public void onSuccess(final List<StorageNode> storageNodes) {
                    if (storageNodes != null && !storageNodes.isEmpty()) {
                        for (StorageNode node : storageNodes) {
                            storageNodeIdToAddressMap.put(node.getId(), node.getAddress());
                        }
                    }
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)) + " "
                            + caught.getMessage(), caught);
                }
            });
            
        }
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
