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
package org.rhq.enterprise.gui.coregui.client.components.tagging;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class TaggingDataSource extends RPCDataSource<Tag> {

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        TagCriteria criteria = new TagCriteria();

        String search = (String) request.getCriteria().getValues().get("tag");
        if (search != null) {
            // todo
//            criteria.addFilter
        }

        GWTServiceLookup.getTagService().findTagsByCriteria(new TagCriteria(),
                new AsyncCallback<PageList<Tag>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load tags",caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }

                    public void onSuccess(PageList<Tag> result) {
                        response.setData(buildRecords(result));
                        processResponse(request.getRequestId(), response);
                    }
                });

    }

    @Override
    public Tag copyValues(ListGridRecord from) {
        return null;  // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(Tag from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id",from.getId());
        record.setAttribute("namespace",from.getNamespace());
        record.setAttribute("semantic",from.getSemantic());
        record.setAttribute("name",from.getName());
        record.setAttribute("tag",from.toString());
        return record;
    }
}
