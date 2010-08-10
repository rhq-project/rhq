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
package org.rhq.enterprise.gui.coregui.client.util;

import java.util.Collection;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;

/**
 * Base GWT-RPC oriented DataSource class.
 *
 * @author Greg Hinkle
 */
public abstract class RPCDataSource<T> extends DataSource {

    public RPCDataSource() {
        this(null);
    }

    public RPCDataSource(String name) {
        if (name != null) {
            System.out.println("Trying to build DS: " + name);
            setID(name);
        }
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);
    }

    @Override
    protected Object transformRequest(DSRequest request) {
        try {
            DSResponse response = createResponse(request);

            switch (request.getOperationType()) {
            case FETCH:
                executeFetch(request, response);
                break;
            case ADD:
                executeAdd(request, response);
                break;
            case UPDATE:
                executeUpdate(request, response);
                break;
            case REMOVE:
                executeRemove(request, response);
                break;
            default:
                super.transformRequest(request);
                break;
            }
        } catch (Throwable t) {
            CoreGUI.getErrorHandler().handleError("Failure in datasource [" + request.getOperationType() + "]", t);
            return null;
        }
        return request.getData();
    }

    /**
     * Returns a prepopulated PageControl based on the provided DSRequest. This will set sort fields,
     * pagination, but *not* filter fields.
     *
     * @param request the request to turn into a page control
     * @return the page control for passing to criteria and other queries
     */
    protected PageControl getPageControl(DSRequest request) {
        // Initialize paging.
        PageControl pageControl;
        if (request.getStartRow() == null || request.getEndRow() == null) {
            pageControl = new PageControl();
        } else {
            pageControl = PageControl.getExplicitPageControl(request.getStartRow(), request.getEndRow()
                - request.getStartRow());
        }

        // Initialize sorting.
        String sortBy = request.getAttribute("sortBy");
        if (sortBy != null) {
            String[] sorts = sortBy.split(",");
            for (String sort : sorts) {
                PageOrdering ordering = (sort.startsWith("-")) ? PageOrdering.DESC : PageOrdering.ASC;
                String columnName = (ordering == PageOrdering.DESC) ? sort.substring(1) : sort;
                pageControl.addDefaultOrderingField(columnName, ordering);
            }
        }

        return pageControl;
    }

    public ListGridRecord[] buildRecords(Collection<T> list) {
        if (list == null) {
            return null;
        }

        ListGridRecord[] records = new ListGridRecord[list.size()];
        int i = 0;
        for (T item : list) {
            records[i++] = copyValues(item);
        }
        return records;
    }

    /**
     * Extensions should implement this method to retrieve data. Paging solutions should use
     * {@link #getPageControl(com.smartgwt.client.data.DSRequest)}. All implementations should call processResponse()
     * whether they fail or succeed. Data should be set on the request via setData. Implementations can use
     * buildRecords() to get the list of records.
     *
     * @param request
     * @param response
     */
    protected abstract void executeFetch(final DSRequest request, final DSResponse response);

    public abstract T copyValues(ListGridRecord from);

    public abstract ListGridRecord copyValues(T from);

    /**
     * Executed on <code>REMOVE</code> operation. <code>processResponse (requestId, response)</code>
     * should be called when operation completes (either successful or failure).
     *
     * @param request  <code>DSRequest</code> being processed. <code>request.getData ()</code>
     *                 contains record should be removed.
     * @param response <code>DSResponse</code>. <code>setData (list)</code> should be called on
     *                 successful execution of this method. Array should contain single element representing
     *                 removed row. <code>setStatus (&lt;0)</code> should be called on failure.
     */
    protected void executeRemove(final DSRequest request, final DSResponse response) {
        throw new UnsupportedOperationException("This dataSource does not support removal.");
    }

    protected void executeAdd(final DSRequest request, final DSResponse response) {
        throw new UnsupportedOperationException("This dataSource does not support addition.");
    }

    protected void executeUpdate(final DSRequest request, final DSResponse response) {
        throw new UnsupportedOperationException("This dataSource does not support updates.");
    }

    private DSResponse createResponse(DSRequest request) {
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Assume success as the default.
        response.setStatus(0);
        return response;
    }
}
