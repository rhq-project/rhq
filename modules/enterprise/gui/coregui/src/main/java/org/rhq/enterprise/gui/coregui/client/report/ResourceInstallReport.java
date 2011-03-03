package org.rhq.enterprise.gui.coregui.client.report;

import java.util.HashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A tabular report that shows the types of resources are installed and how many
 * of them are installed.
 * 
 * @author John Mazzitelli
 */
public class ResourceInstallReport extends LocatableVLayout implements BookmarkableView {

    public static final ViewName VIEW_ID = new ViewName("InventorySummary", MSG.common_title_inventorySummary());

    private ResourceSearchView resourceList;

    public ResourceInstallReport(String locatorId) {
        super(locatorId);
        setHeight100();
        setWidth100();
    }

    @Override
    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            int resourceTypeId = Integer.parseInt(viewPath.getCurrent().getPath());
            viewPath.next();
            Criteria criteria;
            if (!viewPath.isEnd()) {
                String resourceVersion = viewPath.getCurrent().getPath();
                criteria = createResourceSearchViewCriteria(resourceTypeId, resourceVersion);
            } else {
                criteria = createResourceSearchViewCriteria(resourceTypeId);
            }
            showResourceList(criteria);
        } else {
            hideResourceList();
        }
    }

    @Override
    protected void onInit() {
        super.onInit();

        addMember(new ResourceInstallReportTable(extendLocatorId("table")));
    }

    protected Criteria createResourceSearchViewCriteria(int resourceTypeId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(ResourceDataSourceField.TYPE.propertyName(), resourceTypeId);
        return criteria;
    }

    protected Criteria createResourceSearchViewCriteria(int resourceTypeId, String resourceVersion) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(ResourceDataSourceField.TYPE.propertyName(), resourceTypeId);
        criteria.addCriteria("version", resourceVersion);
        return criteria;
    }

    private void showResourceList(Criteria criteria) {
        hideResourceList();
        resourceList = new ResourceSearchView(extendLocatorId("resourceList"), criteria);
        addMember(resourceList);
        markForRedraw();
    }

    private void hideResourceList() {
        if (resourceList != null) {
            removeMember(resourceList);
            resourceList.destroy();
            resourceList = null;
        }
        markForRedraw();
    }

    class ResourceInstallReportTable extends Table<ResourceInstallReportTable.DataSource> {

        public ResourceInstallReportTable(String locatorId) {
            super(locatorId, VIEW_ID.getTitle());
            setDataSource(new DataSource());
        }

        @Override
        protected void configureTable() {
            ListGridField fieldTypeName = new ListGridField(DataSource.Field.TYPENAME, MSG.common_title_resource_type());
            ListGridField fieldPlugin = new ListGridField(DataSource.Field.TYPEPLUGIN, MSG.common_title_plugin());
            ListGridField fieldCategory = new ListGridField(DataSource.Field.CATEGORY, MSG.common_title_category());
            ListGridField fieldVersion = new ListGridField(DataSource.Field.VERSION, MSG.common_title_version());
            ListGridField fieldCount = new ListGridField(DataSource.Field.COUNT, MSG.common_title_count());

            fieldTypeName.setWidth("35%");
            fieldPlugin.setWidth("10%");
            fieldCategory.setWidth("25");
            fieldVersion.setWidth("*");
            fieldCount.setWidth("10%");

            fieldCategory.setType(ListGridFieldType.ICON);
            fieldCategory.setShowValueIconOnly(true);
            HashMap<String, String> categoryIcons = new HashMap<String, String>(3);
            categoryIcons
                .put(ResourceCategory.PLATFORM.name(), ImageManager.getResourceIcon(ResourceCategory.PLATFORM));
            categoryIcons.put(ResourceCategory.SERVER.name(), ImageManager.getResourceIcon(ResourceCategory.SERVER));
            categoryIcons.put(ResourceCategory.SERVICE.name(), ImageManager.getResourceIcon(ResourceCategory.SERVICE));
            fieldCategory.setValueIcons(categoryIcons);
            fieldCategory.setShowHover(true);
            fieldCategory.setHoverCustomizer(new HoverCustomizer() {
                @Override
                public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String cat = record.getAttribute(DataSource.Field.CATEGORY);
                    if (ResourceCategory.PLATFORM.name().equals(cat)) {
                        return MSG.common_title_platform();
                    } else if (ResourceCategory.SERVER.name().equals(cat)) {
                        return MSG.common_title_server();
                    } else if (ResourceCategory.SERVICE.name().equals(cat)) {
                        return MSG.common_title_service();
                    }
                    return "";
                }
            });

            setListGridDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    ListGrid lg = (ListGrid) event.getSource();
                    ListGridRecord selected = lg.getSelectedRecord();
                    if (selected != null) {
                        int resourceTypeId = selected.getAttributeAsInt(DataSource.Field.TYPEID);
                        String version = selected.getAttribute(DataSource.Field.VERSION);
                        if (version == null) {
                            CoreGUI.goToView("#Reports/Inventory/InventorySummary/" + resourceTypeId);
                        } else {
                            CoreGUI.goToView("#Reports/Inventory/InventorySummary/" + resourceTypeId + "/" + version);
                        }
                    }
                }
            });

            setListGridFields(fieldTypeName, fieldPlugin, fieldCategory, fieldVersion, fieldCount);
        }

        class DataSource extends RPCDataSource<ResourceInstallCount> {

            public class Field {
                public static final String COUNT = "count"; // long that we convert to int
                public static final String TYPENAME = "typeName"; // String
                public static final String TYPEPLUGIN = "typePlugin"; // String
                public static final String CATEGORY = "category"; // ResourceCategory
                public static final String TYPEID = "typeId"; // int
                public static final String VERSION = "version"; // String
                public static final String OBJECT = "object";
            }

            @Override
            public ResourceInstallCount copyValues(Record from) {
                return (ResourceInstallCount) from.getAttributeAsObject(DataSource.Field.OBJECT);
            }

            @Override
            public ListGridRecord copyValues(ResourceInstallCount from) {
                ListGridRecord record = new ListGridRecord();

                record.setAttribute(Field.COUNT, Long.valueOf(from.getCount()).intValue()); // we'll never have over Integer.MAX_VALUE, overflow not a worry
                record.setAttribute(Field.TYPENAME, from.getTypeName());
                record.setAttribute(Field.TYPEPLUGIN, from.getTypePlugin());
                record.setAttribute(Field.CATEGORY, from.getCategory().name());
                record.setAttribute(Field.TYPEID, from.getTypeId());
                record.setAttribute(Field.VERSION, from.getVersion());

                record.setAttribute(Field.OBJECT, from);

                return record;
            }

            @Override
            protected void executeFetch(final DSRequest request, final DSResponse response) {
                ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

                resourceService.findResourceInstallCounts(true, new AsyncCallback<List<ResourceInstallCount>>() {

                    @Override
                    public void onSuccess(List<ResourceInstallCount> result) {
                        response.setData(buildRecords(result));
                        response.setTotalRows(result.size());
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_group_pluginConfig_table_failFetch(), caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }
                });
            }
        }
    }
}
