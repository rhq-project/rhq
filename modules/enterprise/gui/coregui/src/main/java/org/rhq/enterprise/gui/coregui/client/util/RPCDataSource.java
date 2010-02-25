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

import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;

/**
 * @author Greg Hinkle
 */
public abstract class RPCDataSource extends DataSource {


    public RPCDataSource() {
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

    }


    public RPCDataSource(String name) {
        System.out.println("Trying to build DS: " + name);
        setID(name);
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);


    }


    @Override
    protected Object transformRequest(DSRequest request) {
        String requestId = request.getRequestId();
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Asume success
        response.setStatus(0);
        switch (request.getOperationType()) {
            case ADD:
                //executeAdd(lstRec, true);
                break;
            case FETCH:
                executeFetch(requestId, request, response);
                break;
            case REMOVE:
                //executeRemove(lstRec);
                break;
            case UPDATE:
                //executeAdd(lstRec, false);
                break;

            default:
                break;
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

        PageControl pageControl = PageControl.getExplicitPageControl(request.getStartRow(), request.getEndRow() - request.getStartRow());

        String sortBy = request.getAttribute("sortBy");
        if (sortBy != null) {
            String[] sorts = sortBy.split(",");

            for (String sort : sorts) {
                if (sort.startsWith("-")) {
                    pageControl.addDefaultOrderingField(sort.substring(1), PageOrdering.DESC);
                } else {
                    pageControl.addDefaultOrderingField(sort, PageOrdering.ASC);
                }
            }
        }
        return pageControl;
    }

    public abstract void executeFetch(final String requestId, final DSRequest request, final DSResponse response);

}
