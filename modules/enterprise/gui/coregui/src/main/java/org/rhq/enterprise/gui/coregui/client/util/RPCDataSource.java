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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.effects.ColoringUtility;

/**
 * Base GWT-RPC oriented DataSource class.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public abstract class RPCDataSource<T> extends DataSource {

    private List<String> hightlightingFieldNames = new ArrayList<String>();

    public RPCDataSource() {
        this(null);
    }

    public RPCDataSource(String name) {
        if (name != null) {
            System.out.println("Trying to build DS: " + name);
            setID(name);
        }
        // TODO until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed always go to the server for data
        setClientOnly(false);
        setAutoCacheAllData(false);
        setCacheAllData(false);
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

    @Override
    public void addField(DataSourceField field) throws IllegalStateException {
        super.addField(field);
        if ((field instanceof HighlightingDatasourceTextField) == false) {
            return;
        }
        field.setHidden(true);

        hightlightingFieldNames.add(field.getName());

        String name = field.getName() + "-highlight";
        String title = field.getTitle();
        DataSourceTextField fieldToDisplayHighlighting = new DataSourceTextField(name, title);
        super.addField(fieldToDisplayHighlighting);
    }

    @SuppressWarnings("unchecked")
    protected void highlightFilterMatches(final DSRequest request, final ListGridRecord[] records) {
        Map<String, Object> criteriaMap = request.getCriteria().getValues();

        for (String filterName : hightlightingFieldNames) {
            String filterValue = (String) criteriaMap.get(filterName);
            for (ListGridRecord nextRecord : records) {
                String originalData = nextRecord.getAttribute(filterName);
                String decoratedData = (filterValue != null) ? ColoringUtility.highlight(originalData, filterValue)
                    : originalData;
                nextRecord.setAttribute(filterName + "-highlight", decoratedData);
            }
        }
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

    /**
     * Add the specified fields to this data source. When the data source is associated with a
     * {@link com.smartgwt.client.widgets.grid.ListGrid}, the fields will be displayed in the order they are specified
     * here.
     *
     * @param fields the fields to be added
     */
    public void addFields(List<DataSourceField> fields) {
        for (DataSourceField field : fields) {
            addField(field);
        }
    }

    public ListGridRecord getEditedRecord(DSRequest request) {
        // Retrieving values before edit
        JavaScriptObject oldValues = request.getAttributeAsJavaScriptObject("oldValues");
        // Creating new record for combining old values with changes
        ListGridRecord newRecord = new ListGridRecord();
        // Copying properties from old record
        JSOHelper.apply(oldValues, newRecord.getJsObj());
        // Retrieving changed values
        JavaScriptObject data = request.getData();
        // Apply changes
        JSOHelper.apply(data, newRecord.getJsObj());
        return newRecord;
    }

    @SuppressWarnings("unchecked")
    public static <S> S[] getArrayFilter(DSRequest request, String paramName, Class<S> type) {
        //System.out.println("Fetching array " + paramName + " (" + type + ")");
        Criteria criteria = request.getCriteria();
        Map<String, Object> criteriaMap = criteria.getValues();

        S[] resultArray = null;

        Object value = criteriaMap.get(paramName);
        if (value == null) {
            // nothing to do, result is already null
        } else if (type == Integer.class) {
            int[] intermediates = criteria.getAttributeAsIntArray(paramName);
            resultArray = (S[]) new Integer[intermediates.length];
            int index = 0;
            for (int next : intermediates) {
                resultArray[index++] = (S) (Integer) next;
            }
        } else if (type == String.class) {
            String[] intermediates = criteria.getAttributeAsStringArray(paramName);
            resultArray = (S[]) new String[intermediates.length];
            int index = 0;
            for (String next : intermediates) {
                resultArray[index++] = (S) next;
            }
        } else {
            throw new IllegalArgumentException("No support for passing array filters of type " + type);
        }

        //System.out.println("Result array = " + resultArray);

        return resultArray;
    }

    @SuppressWarnings("unchecked")
    public static <S> S getFilter(DSRequest request, String paramName, Class<S> type) {
        //System.out.println("Fetching " + paramName + " (" + type + ")");
        Criteria criteria = request.getCriteria();
        Map<String, Object> criteriaMap = criteria.getValues();

        S result = null;

        Object value = criteriaMap.get(paramName);
        if (value == null) {
            // nothing to do, result is already null
        } else {
            String strValue = value.toString();
            if (type == String.class) {
                result = (S) strValue;
            } else if (type == Integer.class) {
                result = (S) Integer.valueOf(strValue);
            } else if (type.isEnum()) {
                result = (S) Enum.valueOf((Class<? extends Enum>) type, strValue.toUpperCase());
            } else {
                result = (S) value; // otherwise presume the object is already that type, and just cast it
            }
        }

        //System.out.println("Result = " + result);

        return result;
    }
}
