package org.rhq.enterprise.gui.coregui.client.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.AbstractAlertDefinitionsDataSource;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * A tabular report that shows alert definitions on all resources in inventory.
 * 
 * @author John Mazzitelli
 */
public class AlertDefinitionReportView extends Table<AlertDefinitionReportView.DataSource> {

    public static final ViewName VIEW_ID = new ViewName("AlertDefinitions", MSG.view_reports_alertDefinitions());

    public AlertDefinitionReportView(String locatorId) {
        super(locatorId);
        setDataSource(new DataSource());
    }

    @Override
    protected void configureTable() {
        ListGrid listGrid = getListGrid();
        listGrid.setFields(getDataSource().getListGridFields().toArray(new ListGridField[0]));

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid lg = (ListGrid) event.getSource();
                ListGridRecord selected = lg.getSelectedRecord();
                if (selected != null) {
                    AlertDefinition alertDef = getDataSource().copyValues(selected);
                    int resourceId = alertDef.getResource().getId();
                    int alertDefId = alertDef.getId();
                    String link = LinkManager.getSubsystemAlertDefinitionLink(resourceId, alertDefId);
                    CoreGUI.goToView(link);
                }
            }
        });
    }

    class DataSource extends AbstractAlertDefinitionsDataSource {

        private static final String FIELD_PARENT = "parent";
        private static final String FIELD_RESOURCE = "resource";

        @Override
        protected AlertDefinitionCriteria getFetchCriteria(DSRequest request) {
            AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
            criteria.addFilterResourceOnly(true); // guarantees that all alert defs we get will have a non-null Resource object
            criteria.setPageControl(getPageControl(request));
            criteria.fetchResource(true);
            criteria.fetchGroupAlertDefinition(true);
            return criteria;
        }

        @Override
        protected AlertDefinitionCriteria getSimpleCriteriaForAll() {
            AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
            criteria.addFilterResourceOnly(true);
            criteria.setPageControl(PageControl.getUnlimitedInstance());
            return criteria;
        }

        @Override
        public ArrayList<ListGridField> getListGridFields() {
            ArrayList<ListGridField> fields = super.getListGridFields();

            // hide the created/modified fields, we don't need to show them by default
            // add cell formatter on the name field so we can make it a link
            for (ListGridField field : fields) {
                String fieldName = field.getName();
                if (fieldName.equals(FIELD_CTIME) || fieldName.equals(FIELD_MTIME)) {
                    field.setHidden(true);
                } else if (fieldName.equals(FIELD_NAME)) {
                    field.setCellFormatter(new CellFormatter() {
                        @Override
                        public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                            AlertDefinition alertDef = copyValues(record);
                            int resourceId = alertDef.getResource().getId();
                            int alertDefId = alertDef.getId();
                            String link = LinkManager.getSubsystemAlertDefinitionLink(resourceId, alertDefId);
                            return "<a href=\"" + link + "\">" + StringUtility.escapeHtml(alertDef.getName()) + "</a>";
                        }
                    });
                }
            }

            // add more columns
            ListGridField parentField = new ListGridField(FIELD_PARENT, MSG.view_alerts_field_parent());
            parentField.setWidth(100);
            parentField.setShowHover(true);
            parentField.setHoverCustomizer(new HoverCustomizer() {
                @Override
                public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                    if (record.getAttribute(FIELD_PARENT) != null) {
                        return MSG.view_reports_alertDefinitions_parentHover();
                    }
                    return MSG.common_val_na();
                }
            });
            parentField.addRecordClickHandler(new RecordClickHandler() {
                @Override
                public void onRecordClick(RecordClickEvent event) {
                    // we only do something if we really have a parent.
                    // if we have a template parent, we have to get the resource's type and go to the template page for that type
                    // if we have a group parent, we can directly go to the group's alert def page
                    Record record = event.getRecord();
                    AlertDefinition alertDef = copyValues(record);
                    if (alertDef.getParentId() != null && alertDef.getParentId().intValue() > 0) {
                        final Integer templateId = alertDef.getParentId().intValue();
                        final Integer resourceId = alertDef.getResource().getId();

                        ResourceCriteria resCriteria = new ResourceCriteria();
                        resCriteria.addFilterId(resourceId);
                        resCriteria.fetchResourceType(true);

                        GWTServiceLookup.getResourceService().findResourcesByCriteria(resCriteria,
                            new AsyncCallback<PageList<Resource>>() {
                                @Override
                                public void onSuccess(PageList<Resource> result) {
                                    if (result == null || result.size() != 1) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_reports_alertDefinitions_resTypeLoadError());
                                    } else {
                                        int typeId = result.get(0).getResourceType().getId();
                                        CoreGUI.goToView(LinkManager.getAdminTemplatesLink() + "/Alert/" + typeId + "/"
                                            + templateId);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(
                                        MSG.view_reports_alertDefinitions_resTypeLoadError(), caught);
                                }
                            });
                    } else if (alertDef.getGroupAlertDefinition() != null) {
                        AlertDefinition groupAlertDef = alertDef.getGroupAlertDefinition();
                        CoreGUI.goToView(LinkManager.getResourceGroupLink(groupAlertDef.getResourceGroup().getId())
                            + "/Alerts/Definitions/" + groupAlertDef.getId());
                    }
                }
            });
            fields.add(parentField);

            ListGridField resourceField = new ListGridField(FIELD_RESOURCE, MSG.common_title_resource());
            resourceField.setCellFormatter(new CellFormatter() {
                public String format(Object value, ListGridRecord listGridRecord, int i, int i1) {
                    String url = LinkManager
                        .getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                    return SeleniumUtility.getLocatableHref(url, StringUtility.escapeHtml(value.toString()), null);
                }
            });
            resourceField.setShowHover(true);
            resourceField.setHoverCustomizer(new HoverCustomizer() {
                public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                    return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
                }
            });
            fields.add(resourceField);

            ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();
            fields.add(ancestryField);

            return fields;
        }

        @Override
        public ListGridRecord copyValues(AlertDefinition from) {
            // in order to support sorting our list grid on the parent and resource columns,
            // we have to assign these to something that is sortable
            ListGridRecord record = super.copyValues(from);
            Resource resource = from.getResource();

            record.setAttribute(FIELD_RESOURCE, resource.getName());

            Integer parentId = from.getParentId(); // a valid non-zero number means the alert def came from a template
            AlertDefinition groupAlertDefinition = from.getGroupAlertDefinition();

            if (parentId != null && parentId.intValue() > 0) {
                record.setAttribute(FIELD_PARENT, "<b>" + MSG.view_alert_definition_for_type() + "</b>");
            } else if (groupAlertDefinition != null) {
                record.setAttribute(FIELD_PARENT, "<b>" + MSG.view_alert_definition_for_group() + "</b>");
            }

            // for ancestry handling     
            record.setAttribute(AncestryUtil.RESOURCE_ID, resource.getId());
            record.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
            record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
            record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

            return record;
        }

        @Override
        protected List<DataSourceField> addDataSourceFields() {
            List<DataSourceField> fields = super.addDataSourceFields();

            // hide the created/modified fields, we don't need to show them by default
            for (DataSourceField field : fields) {
                String fieldName = field.getName();
                if (fieldName.equals(FIELD_CTIME) || fieldName.equals(FIELD_MTIME)) {
                    field.setHidden(true);
                }
            }

            // add more columns
            DataSourceField resourceField = new DataSourceTextField(FIELD_RESOURCE);
            fields.add(resourceField);

            DataSourceField parentField = new DataSourceTextField(FIELD_RESOURCE);
            fields.add(parentField);

            return fields;
        }

        /**
         * Additional processing to support a cross-resource view)
         * @param result
         * @param response
         * @param request
         */
        protected void dataRetrieved(final PageList<AlertDefinition> result, final DSResponse response,
            final DSRequest request) {
            HashSet<Integer> typesSet = new HashSet<Integer>();
            HashSet<String> ancestries = new HashSet<String>();
            for (AlertDefinition alertDefinition : result) {
                Resource resource = alertDefinition.getResource();
                if (null != resource) {
                    typesSet.add(resource.getResourceType().getId());
                    ancestries.add(resource.getAncestry());
                }
            }

            // In addition to the types of the result resources, get the types of their ancestry
            typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

            ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
            typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {

                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.                
                    AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                    Record[] records = buildRecords(result);
                    for (Record record : records) {
                        // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                        // Store the types map off the records so we can build a detailed hover string as needed.                      
                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                        // Build the decoded ancestry Strings now for display
                        record
                            .setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil.getAncestryValue(record));
                    }
                    response.setData(records);
                    response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                    processResponse(request.getRequestId(), response);
                }
            });
        }
    }
}
