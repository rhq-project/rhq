package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.AVAILABILITY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.DESCRIPTION;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A DataSource basically the same as ResourceDatasource in the fields it defines, but that works with
 * ResourceComposite as opposed to Resource records.  In this way the Records can provide additional info,
 * like the user's resource permissions, for the resources. 
 *  
 * @author Jay Shaughnessy
 */
public class ResourceCompositeDataSource extends RPCDataSource<ResourceComposite> {
    private static ResourceCompositeDataSource INSTANCE;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public static ResourceCompositeDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResourceCompositeDataSource();
        }
        return INSTANCE;
    }

    public ResourceCompositeDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    // Defined the same way as ResourceDatasource 
    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idDataField = new DataSourceIntegerField("id", "ID", 50);
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);
        fields.add(idDataField);

        DataSourceImageField iconField = new DataSourceImageField("icon", "");
        iconField.setImageURLPrefix("types/");
        fields.add(iconField);

        DataSourceTextField nameDataField = new DataSourceTextField(NAME.propertyName(), NAME.title(), 200);
        nameDataField.setCanEdit(false);
        fields.add(nameDataField);

        DataSourceTextField descriptionDataField = new DataSourceTextField(DESCRIPTION.propertyName(), DESCRIPTION
            .title());
        descriptionDataField.setCanEdit(false);
        fields.add(descriptionDataField);

        DataSourceTextField typeNameDataField = new DataSourceTextField(TYPE.propertyName(), TYPE.title());
        fields.add(typeNameDataField);

        DataSourceTextField pluginNameDataField = new DataSourceTextField(PLUGIN.propertyName(), PLUGIN.title());
        fields.add(pluginNameDataField);

        DataSourceTextField categoryDataField = new DataSourceTextField(CATEGORY.propertyName(), CATEGORY.title());
        fields.add(categoryDataField);

        DataSourceImageField availabilityDataField = new DataSourceImageField(AVAILABILITY.propertyName(), AVAILABILITY
            .title(), 20);
        availabilityDataField.setCanEdit(false);
        fields.add(availabilityDataField);

        return fields;
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        ResourceCriteria criteria = getFetchCriteria(request);

        getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to fetch resource data", caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<ResourceComposite> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    protected void dataRetrieved(PageList<ResourceComposite> result, DSResponse response, DSRequest request) {
        Record[] records = this.buildRecords(result);
        response.setData(records);
        response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
        processResponse(request.getRequestId(), response);
    }

    protected ResourceCriteria getFetchCriteria(final DSRequest request) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.setPageControl(getPageControl(request));

        criteria.addFilterId(getFilter(request, "id", Integer.class));
        criteria.addFilterParentResourceId(getFilter(request, "parentId", Integer.class));
        criteria.addFilterCurrentAvailability(getFilter(request, AVAILABILITY.propertyName(), AvailabilityType.class));
        criteria.addFilterResourceCategory(getFilter(request, CATEGORY.propertyName(), ResourceCategory.class));
        criteria.addFilterIds(getArrayFilter(request, "resourceIds", Integer.class));
        criteria.addFilterImplicitGroupIds(getFilter(request, "groupId", Integer.class));
        criteria.addFilterName(getFilter(request, NAME.propertyName(), String.class));
        criteria.addFilterResourceTypeId(getFilter(request, TYPE.propertyName(), Integer.class));
        criteria.addFilterPluginName(getFilter(request, PLUGIN.propertyName(), String.class));
        criteria.addFilterTagNamespace(getFilter(request, "tagNamespace", String.class));
        criteria.addFilterTagSemantic(getFilter(request, "tagSemantic", String.class));
        criteria.addFilterTagName(getFilter(request, "tagName", String.class));

        return criteria;
    }

    @Override
    public ResourceComposite copyValues(Record from) {
        // not very strong...
        return new ResourceComposite(new Resource(from.getAttributeAsInt("id")), AvailabilityType.DOWN);
    }

    @Override
    public ListGridRecord copyValues(ResourceComposite from) {
        Resource res = from.getResource();
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("resourceComposite", from);
        record.setAttribute("resource", res);
        record.setAttribute("id", res.getId());
        record.setAttribute(NAME.propertyName(), res.getName());
        record.setAttribute(DESCRIPTION.propertyName(), res.getDescription());
        record.setAttribute(TYPE.propertyName(), res.getResourceType().getId());
        record.setAttribute(PLUGIN.propertyName(), res.getResourceType().getPlugin());
        record.setAttribute(CATEGORY.propertyName(), res.getResourceType().getCategory().name());
        record.setAttribute("icon", res.getResourceType().getCategory().getDisplayName() + "_"
            + (res.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? "up" : "down") + "_16.png");

        record
            .setAttribute(
                AVAILABILITY.propertyName(),
                res.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? "/images/icons/availability_green_16.png"
                    : "/images/icons/availability_red_16.png");

        record.setAttribute("resourcePersmission", from.getResourcePermission());
        return record;
    }

    public ResourceGWTServiceAsync getResourceService() {
        return resourceService;
    }
}
