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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.util.effects.ColoringUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

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
            Log.info("Trying to build DataSource: " + name);
            setID(SeleniumUtility.getSafeId(name));
        }
        // TODO until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed always go to the server for data
        setClientOnly(false);
        setAutoCacheAllData(false);
        setCacheAllData(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);
    }

    /**
     * A pattern that can be used for Datasource subclassing.  Each subclass can add it's own fields prior to
     * all of the fields being added to the datasource. 
     */
    protected List<DataSourceField> addDataSourceFields() {
        return new ArrayList<DataSourceField>();
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
                ListGridRecord newRecord = getDataObject(request);
                executeAdd(newRecord, request, response);
                break;
            case UPDATE:
                Record oldRecord = request.getOldValues(); // original values before the update
                Record updatedRecord = getUpdatedRecord(request, oldRecord);
                executeUpdate(updatedRecord, oldRecord, request, response);
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

    private Record getUpdatedRecord(DSRequest request, Record oldRecord) {
        // Get changed values.
        JavaScriptObject data = request.getData ();
        // Apply changes.
        JSOHelper.apply(data, oldRecord.getJsObj());
        return new ListGridRecord(data);
    }

    private static ListGridRecord getDataObject(DSRequest request) {
        JavaScriptObject data = request.getData();
        ListGridRecord newRecord = new ListGridRecord(data);
        return newRecord;
    }

    /**
     * Returns a prepopulated PageControl based on the provided DSRequest. This will set sort fields,
     * pagination, but *not* filter fields.
     *
     * @param request the request to turn into a page control
     * @return the page control for passing to criteria and other queries
     */
    protected PageControl getPageControl(DSRequest request) {
        // Create PageControl and initialize paging.
        PageControl pageControl;
        if (request.getStartRow() == null || request.getEndRow() == null) {
            pageControl = new PageControl();
        } else {
            pageControl = PageControl.getExplicitPageControl(request.getStartRow(),
                request.getEndRow() - request.getStartRow());
        }
                
        initializeSorting(pageControl, request);
        
        return pageControl;
    }

    private void initializeSorting(PageControl pageControl, DSRequest request) {
        SortSpecifier[] sortSpecifiers = request.getSortBy();
        if (sortSpecifiers != null) {
            for (SortSpecifier sortSpecifier : sortSpecifiers) {
                PageOrdering ordering = (sortSpecifier.getSortDirection() == SortDirection.ASCENDING) ?
                    PageOrdering.ASC : PageOrdering.DESC;
                String columnName = sortSpecifier.getField();
                pageControl.addDefaultOrderingField(columnName, ordering);
            }
        }
    }

    protected void populateSuccessResponse(PageList<T> result, DSResponse response) {
        response.setStatus(RPCResponse.STATUS_SUCCESS);
        Record[] records = buildRecords(result);
        response.setData(records);
        // For paging to work, we have to specify size of full result set.
        int totalRows = (result.isUnbounded()) ? records.length : result.getTotalSize();
        response.setTotalRows(totalRows);
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

    public Set<T> buildDataObjects(Record[] records) {
        if (records == null) {
            return null;
        }

        Set<T> results = new HashSet<T>(records.length);
        int i = 0;
        for (Record record : records) {
            results.add(copyValues(record));
        }
        return results;
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
    protected void highlightFilterMatches(final DSRequest request, final Record[] records) {
        Map<String, Object> criteriaMap = request.getCriteria().getValues();

        for (String filterName : hightlightingFieldNames) {
            String filterValue = (String) criteriaMap.get(filterName);
            for (Record nextRecord : records) {
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

    public abstract T copyValues(Record from);

    /**
     *
     * @param from
     * @return
     */
    // TODO (ips): This really should return Records, rather than ListGridRecords, so the DataSource is not specific to
    //             ListGrids, but that will require a lot of refactoring at this point...
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

    /**
     * TODO
     *
     * @param newRecord
     * @param request
     * @param response
     */
    protected void executeAdd(Record newRecord, final DSRequest request, final DSResponse response) {
        throw new UnsupportedOperationException("This dataSource does not support addition.");
    }

    /**
     * TODO
     *
     * @param updatedRecord
     * @param oldRecord
     * @param request
     * @param response
     */
    protected void executeUpdate(Record updatedRecord, Record oldRecord, final DSRequest request,
                                 final DSResponse response) {
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

    public void addFields(DataSourceField... fields) {
        addFields(Arrays.asList(fields));
    }


    @SuppressWarnings("unchecked")
    public static <S> S[] getArrayFilter(DSRequest request, String paramName, Class<S> type) {
        Log.debug("Fetching array " + paramName + " (" + type + ")");
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
        } else if (type.isEnum()) {
            String[] intermediates = criteria.getAttributeAsStringArray(paramName);
            List<S> buffer = new ArrayList<S>();
            for (String next : intermediates) {
                buffer.add((S) Enum.valueOf((Class<? extends Enum>) type, next));
            }
            resultArray = buffer.toArray((S[]) getEnumArray(type, buffer.size()));
        } else {
            throw new IllegalArgumentException("No support for passing array filters of type " + type);
        }

        com.allen_sauer.gwt.log.client.Log.debug("Result array = " + resultArray);

        return resultArray;
    }

    @SuppressWarnings("unchecked")
    private static <S> S[] getEnumArray(Class<S> genericEnumType, int size) {
        // workaround until GWT implements reflection APIs, so we can do: 
        //   array=(S[])Array.newInstance(Class<S>,capacity);
        if (genericEnumType == AlertPriority.class) {
            return (S[]) new AlertPriority[size];
        } else if (genericEnumType == EventSeverity.class) {
            return (S[]) new EventSeverity[size];
        } else {
            throw new IllegalArgumentException("Please add an appropriate code block for enum " + genericEnumType
                + " to RPCDataSource.getEnumArray(Class)");
        }
    }

    @SuppressWarnings("unchecked")
    public static <S> S getFilter(DSRequest request, String paramName, Class<S> type) {
        Log.debug("Fetching " + paramName + " (" + type + ")");
        Criteria criteria = request.getCriteria();
        Map<String, Object> criteriaMap = criteria.getValues();

        S result = null;

        Object value = criteriaMap.get(paramName);
        if (value == null || value.toString().equals("")) {
            // nothing to do, result is already null
        } else {
            String strValue = value.toString();
            if (type == String.class) {
                result = (S) strValue;
            } else if (type == Integer.class) {
                result = (S) Integer.valueOf(strValue);
            } else if (type.isEnum()) {
                result = (S) Enum.valueOf((Class<? extends Enum>) type, strValue);
            } else {
                result = (S) value; // otherwise presume the object is already that type, and just cast it
            }
        }

        Log.debug("Result = " + result);

        return result;
    }

    protected DataSourceTextField createTextField(String name, String title, Integer minLength, Integer maxLength,
                                                Boolean required) {
        DataSourceTextField textField = new DataSourceTextField(name, title);
        textField.setLength(maxLength);
        textField.setRequired(required);
        if (minLength != null || maxLength != null) {
            LengthRangeValidator lengthRangeValidator = new LengthRangeValidator();
            lengthRangeValidator.setMin(minLength);
            lengthRangeValidator.setMax(maxLength);
            textField.setValidators(lengthRangeValidator);
        }
        return textField;
    }

    protected DataSourceTextField createBooleanField(String name, String title, Boolean required) {
        DataSourceTextField textField = new DataSourceTextField(name, title);
        textField.setLength(Boolean.FALSE.toString().length());
        textField.setRequired(required);
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        valueMap.put(Boolean.TRUE.toString(), "yes");
        valueMap.put(Boolean.FALSE.toString(), "no");
        textField.setValueMap(valueMap);
        return textField;
    }


    /** Quick method to determine if current user is still logged in.
     *  
     * @return boolean indication of logged in status.
     */
    protected boolean userStillLoggedIn() {
        return UserSessionManager.isLoggedIn();
    }
}
