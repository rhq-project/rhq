package org.rhq.enterprise.gui.coregui.client.resource;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

public class ProblemResourcesDataSource extends DataSource {
    public final String resource = "resource";
    public final String location = "location";
    public final String alerts = "alerts";
    public final String available = "available";

    /** Build list of fields for the datasource and then adds them to it.
     */
    public ProblemResourcesDataSource() {
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        DataSourceTextField resourceField = new DataSourceTextField(resource, "Resource");
        resourceField.setPrimaryKey(true);

        DataSourceTextField locationField = new DataSourceTextField(location, "Location");

        DataSourceTextField alertsField = new DataSourceTextField(alerts, "Alerts");

        DataSourceTextField availablilityField = new DataSourceTextField(available, "Current Availability");

        setFields(resourceField, locationField, alertsField, availablilityField);
    }

    /* Intercept DSRequest object to pipe into custom fetch request.
     * (non-Javadoc)
     * @see com.smartgwt.client.data.DataSource#transformRequest(com.smartgwt.client.data.DSRequest)
     */
    protected Object transformRequest(DSRequest request) {
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Assume success
        response.setStatus(0);
        switch (request.getOperationType()) {
        case FETCH:
            executeFetch(request, response);
            break;
        default:
            break;
        }

        return request.getData();
    }

    /** Fetch the ProblemResource data, and populate the response object appropriately.
     * 
     * @param request incoming request
     * @param response outgoing response
     */
    public void executeFetch(final DSRequest request, final DSResponse response) {

        ResourceCriteria c = new ResourceCriteria();
        c.addFilterCurrentAvailability(AvailabilityType.DOWN);

        //        GWTServiceLookup.getResourceService().findRecentlyAddedResources(0, 100,
        GWTServiceLookup.getResourceService().findProblemResources(c,
        //            new AsyncCallback<List<RecentlyAddedResourceComposite>>() {
            new AsyncCallback<List<ProblemResourceComposite>>() {
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError("Failed to load unavailable resources", throwable);
                }

                //                public void onSuccess(List<RecentlyAddedResourceComposite> recentlyAddedList) {
                public void onSuccess(List<ProblemResourceComposite> problemResourcesList) {
                    //                    List<RecentlyAddedResourceComposite> list = new ArrayList<RecentlyAddedResourceComposite>();
                    List<ProblemResourceComposite> list = new ArrayList<ProblemResourceComposite>();

                    //                    for (RecentlyAddedResourceComposite recentlyAdded : problemResourcesList) {
                    for (ProblemResourceComposite problemResource : problemResourcesList) {
                        list.add(problemResource);
                        //                        list.addAll(problemResource.getChildren());
                    }

                    //                    response.setData(buildNodes(list));
                    response.setData(buildList(list));
                    response.setTotalRows(list.size());
                    processResponse(request.getRequestId(), response);
                }
            });
        //
        //        GWTServiceLookup.getResourceService().findResourcesByCriteria(c, new AsyncCallback<PageList<Resource>>() {
        //            public void onFailure(Throwable caught) {
        //                CoreGUI.getErrorHandler().handleError("Failed to load recently added resources data",caught);
        //                response.setStatus(DSResponse.STATUS_FAILURE);
        //                processResponse(request.getRequestId(), response);
        //            }
        //
        //            public void onSuccess(PageList<Resource> result) {
        //                PageList<Resource> all = new PageList<Resource>();
        //
        //                for (Resource root : result) {
        //                    all.add(root);
        //                    if (root.getChildResources() != null)
        //                        all.addAll(root.getChildResources());
        //                }
        //
        //
        //                response.setData(buildNodes(all));
        //                response.setTotalRows(all.getTotalSize());
        //                processResponse(request.getRequestId(), response);
        //            }
        //        });
    }

    protected Record[] buildList(List<ProblemResourceComposite> list) {
        ListGridRecord[] dataValues = null;
        if (list != null) {
            dataValues = new ListGridRecord[list.size()];
            int indx = 0;
            for (ProblemResourceComposite prc : list) {
                ListGridRecord record = new ListGridRecord();
                record.setAttribute(resource, prc.getResourceId());
                record.setAttribute(location, prc.getResourceName());
                record.setAttribute(alerts, prc.getNumAlerts());
                //               record.setAttribute(available, DateTimeFormat.getMediumDateTimeFormat().format(dateAdded));
                record.setAttribute(available, prc.getAvailabilityType().getName());
                dataValues[indx++] = record;
            }
        }
        return dataValues;
    }

}
