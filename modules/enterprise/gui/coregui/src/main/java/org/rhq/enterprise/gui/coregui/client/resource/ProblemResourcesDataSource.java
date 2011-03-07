package org.rhq.enterprise.gui.coregui.client.resource;

/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * Responsible for defining and populating the Smart GWT datasource details and
 * translating the deserialized content into specific record entries for display
 * 
 * @author Simeon Pinder
 * @author Jay Shaughnessy
 */
public class ProblemResourcesDataSource extends RPCDataSource<ProblemResourceComposite> {

    public enum Field {

        ALERTS("numAlerts", CoreGUI.getMessages().dataSource_problemResources_field_alerts()),

        ANCESTRY("resource.ancestry", CoreGUI.getMessages().common_title_ancestry()),

        AVAILABILITY("availabilityType", CoreGUI.getMessages().common_title_availability()),

        RESOURCE("resource", CoreGUI.getMessages().common_title_resource());

        /**
         * Corresponds to a property name of Resource (e.g. resourceType.name).
         */
        private String propertyName;

        /**
         * The table header for the field or property (e.g. Type).
         */
        private String title;

        private Field(String propertyName, String title) {
            this.propertyName = propertyName;
            this.title = title;
        }

        public String propertyName() {
            return propertyName;
        }

        public String title() {
            return title;
        }

        public ListGridField getListGridField() {
            return new ListGridField(propertyName, title);
        }

        public ListGridField getListGridField(int width) {
            return new ListGridField(propertyName, title, width);
        }

    }

    private Portlet portlet = null;
    private long oldestDate = -1;
    //configure elements
    private int maximumProblemResourcesToDisplay = -1;
    private int maximumProblemResourcesWithinHours = -1;

    /**
     * Build list of fields for the datasource and then adds them to it.
     * @param problemResourcesPortlet
     */
    public ProblemResourcesDataSource(Portlet problemResourcesPortlet) {
        this.portlet = problemResourcesPortlet;

        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField ancestryField = new DataSourceTextField(Field.ANCESTRY.propertyName(), Field.ANCESTRY
            .title());
        fields.add(ancestryField);

        DataSourceTextField alertsField = new DataSourceTextField(Field.ALERTS.propertyName, Field.ALERTS.title());
        fields.add(alertsField);

        DataSourceImageField availabilityField = new DataSourceImageField(Field.AVAILABILITY.propertyName,
            Field.AVAILABILITY.title(), 20);
        availabilityField.setCanEdit(false);
        fields.add(availabilityField);

        return fields;
    }

    /** Fetch the ProblemResource data, and populate the response object appropriately.
     *
     * @param request incoming request
     * @param response outgoing response
     */
    public void executeFetch(final DSRequest request, final DSResponse response) {

        long ctime = -1;
        int maxItems = -1;
        //retrieve current portlet display settings
        if ((this.portlet != null) && (this.portlet instanceof ProblemResourcesPortlet)) {
            ProblemResourcesPortlet problemPortlet = (ProblemResourcesPortlet) this.portlet;
            //populate criteria with portlet preferences defined.
            if (problemPortlet != null) {
                if (getMaximumProblemResourcesToDisplay() > 0) {
                    maxItems = getMaximumProblemResourcesToDisplay();
                }
                //define the time window
                if (getMaximumProblemResourcesWithinHours() > 0) {
                    ctime = System.currentTimeMillis() - (getMaximumProblemResourcesWithinHours() * 60 * 60 * 1000);
                    setOldestDate(ctime);
                }
            }
        }

        GWTServiceLookup.getResourceService().findProblemResources(ctime, maxItems,
            new AsyncCallback<PageList<ProblemResourceComposite>>() {

                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_problemResources_error_fetchFailure(),
                        throwable);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<ProblemResourceComposite> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    protected void dataRetrieved(final PageList<ProblemResourceComposite> result, final DSResponse response,
        final DSRequest request) {
        HashSet<Integer> typesSet = new HashSet<Integer>();
        ArrayList<Resource> resources = new ArrayList<Resource>(result.size());
        for (ProblemResourceComposite resourceComposite : result) {
            Resource resource = resourceComposite.getResource();
            resources.add(resource);
            ResourceType type = resource.getResourceType();
            if (type != null) {
                typesSet.add(type.getId());
            }
        }

        // In addition to the types of the result resources, get the types of their ancestry
        // NOTE: this may be too labor intensive in general, but since this is a singleton I coudln't
        //       make it easily optional.
        typesSet.addAll(AncestryUtil.getResourceTypeIds(resources));

        ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
        typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
            @Override
            public void onTypesLoaded(Map<Integer, ResourceType> types) {

                Record[] records = buildRecords(result);
                for (Record record : records) {
                    // decode ancestry
                    String ancestry = record.getAttributeAsString(Field.ANCESTRY.propertyName());
                    if (null == ancestry) {
                        continue;
                    }
                    int resourceId = record.getAttributeAsInt("id");
                    String[] decodedAncestry = AncestryUtil.decodeAncestry(resourceId, ancestry, types);
                    // Preserve the encoded ancestry for special-case formatting at higher levels. Set the
                    // decoded strings as different attributes. 
                    record.setAttribute(ResourceDatasource.ATTR_ANCESTRY_RESOURCES, decodedAncestry[0]);
                    record.setAttribute(ResourceDatasource.ATTR_ANCESTRY_TYPES, decodedAncestry[1]);
                }
                response.setData(records);
                response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    public ListGridRecord copyValues(ProblemResourceComposite from) {
        ListGridRecord record = new ListGridRecord();
        Resource resource = from.getResource();
        record.setAttribute("id", resource.getId());
        record.setAttribute(Field.RESOURCE.propertyName, AncestryUtil.getResourceLongName(resource));
        record.setAttribute(Field.ANCESTRY.propertyName, resource.getAncestry());
        record.setAttribute(Field.ALERTS.propertyName, from.getNumAlerts());
        record.setAttribute(Field.AVAILABILITY.propertyName, ImageManager.getAvailabilityIconFromAvailType(from
            .getAvailabilityType()));

        record.setAttribute("entity", from);
        return record;
    }

    @Override
    public ProblemResourceComposite copyValues(Record from) {
        throw new UnsupportedOperationException("ProblemResource data is read only");
    }

    public long getOldestDate() {
        return oldestDate;
    }

    public void setOldestDate(long oldestDate) {
        this.oldestDate = oldestDate;
    }

    public int getMaximumProblemResourcesToDisplay() {
        return maximumProblemResourcesToDisplay;
    }

    public void setMaximumProblemResourcesToDisplay(int maxPerRow) {
        this.maximumProblemResourcesToDisplay = maxPerRow;
    }

    public void setMaximumProblemResourcesWithinHours(int maximumProblemResourcesWithinHours) {
        this.maximumProblemResourcesWithinHours = maximumProblemResourcesWithinHours;
    }

    public int getMaximumProblemResourcesWithinHours() {
        return maximumProblemResourcesWithinHours;
    }
}
