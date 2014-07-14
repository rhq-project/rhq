/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.JSOHelper;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertFilter;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.cloud.PartitionEvent.ExecutionStatus;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.criteria.BaseCriteria;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.util.effects.ColoringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedUtility;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.rpc.DataSourceResponseStatistics;

/**
 * Base GWT-RPC oriented DataSource class.
 * 
 * The <T> type is the entity POJO type that represents a record retrieved by the data source
 * The <C> type is the criteria type that is used to fetch data from the data source
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public abstract class RPCDataSource<T, C extends BaseCriteria> extends DataSource {

    protected static final Messages MSG = CoreGUI.getMessages();

    private List<String> hightlightingFieldNames = new ArrayList<String>();
    private Criteria previousCriteria;
    private Integer dataPageSize;

    public RPCDataSource() {
        this(null);
    }

    public RPCDataSource(String name) {
        if (name != null) {
            Log.info("Trying to build DataSource: " + name);
            setID(EnhancedUtility.getSafeId(name));
        }
        // TODO until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed always go to the server for data
        setClientOnly(false);
        setAutoCacheAllData(false);
        setCacheAllData(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);
        setDataPageSize(50);
    }

    /**
     * A pattern that can be used for Datasource subclassing.  Each subclass can add its own fields prior to
     * all of the fields being added to the datasource. 
     */
    protected List<DataSourceField> addDataSourceFields() {
        return new ArrayList<DataSourceField>();
    }

    @Override
    protected Object transformRequest(DSRequest request) {
        try {
            DSResponse response = new DSResponse();
            response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
            // Assume success as the default.
            response.setStatus(0);

            switch (request.getOperationType()) {
            case FETCH:
                C criteria = getFetchCriteria(request);
                if (criteria != null) {
                    // we are always going to supply a PageControl due to https://bugzilla.redhat.com/show_bug.cgi?id=682304.
                    // This can cause some subtle issues with Criteria handling, it's important that code overriding
                    // getFetchCriteria() take a close look at the jdoc.
                    if (criteria.getPageControlOverrides() == null) {
                        criteria.setPageControl(getPageControl(request));
                    }
                    if (Log.isDebugEnabled()) {
                        Log.debug(getClass().getName() + " using [" + criteria.getPageControlOverrides()
                            + "] for fetch request.");
                    }
                } else {
                    Log.warn(getClass().getName()
                        + ".getFetchCriteria() returned null - no paging of results will be done.");
                }
                executeFetch(request, response, criteria);
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
                ListGridRecord deletedRecord = getDataObject(request);
                executeRemove(deletedRecord, request, response);
                break;
            default:
                super.transformRequest(request);
                break;
            }
        } catch (Throwable t) {
            CoreGUI.getErrorHandler().handleError(
                MSG.dataSource_rpc_error_transformRequestFailure(request.getOperationType().name()), t);
            return null;
        }
        return request.getData();
    }

    /**
     * Returns the data page size that should be used for fetch requests, or null if results should not be paged.
     * Default value is 50.
     *
     * @return the data page size that should be used for fetch requests, or null if results should not be paged
     */
    public Integer getDataPageSize() {
        return dataPageSize;
    }

    /**
     * Sets the data page size that should be used for fetch requests, or null if results should not be paged.
     * Default value is 50. Subclasses that wish to use a different value should call this method in their constructors.
     *
     * @param dataPageSize the data page size that should be used for fetch requests, or null if results should not be paged
     */
    public void setDataPageSize(Integer dataPageSize) {
        if (dataPageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0.");
        }
        this.dataPageSize = dataPageSize;
    }

    private Record getUpdatedRecord(DSRequest request, Record oldRecord) {
        // Get changed values.
        JavaScriptObject data = request.getData();
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
        if (previousCriteria != null && !CriteriaUtility.equals(request.getCriteria(), this.previousCriteria)) {
            // The criteria has changed since the last fetch request - reset paging.
            Log.debug("Resetting paging on " + getClass().getName() + "...");
            request.setStartRow(0);
            request.setEndRow(getDataPageSize());
        }
        this.previousCriteria = (request.getCriteria() != null) ? request.getCriteria() : new Criteria();

        // Create PageControl and initialize paging.
        PageControl pageControl;
        if (request.getEndRow() == null) {
            // A null endRow means no paging. However, there is a bug in the RHQ criteria API, where when an unlimited
            // PageControl is used in combination with one or more join fetches, the results contain duplicates.
            // So until that bug is fixed, always use paging.
            //Log.debug("WARNING: " + getClass().getName() + " is not using paging for fetch request.");
            //pageControl = PageControl.getUnlimitedInstance();
            pageControl = PageControl.getExplicitPageControl(0, getDataPageSize());
        } else {
            int startRow = (request.getStartRow() != null) ? request.getStartRow() : 0;
            int endRow = request.getEndRow();
            pageControl = PageControl.getExplicitPageControl(startRow, (endRow - startRow));
        }

        // Initialize sorting.
        initializeSorting(pageControl, request);

        return pageControl;
    }

    private void initializeSorting(PageControl pageControl, DSRequest request) {
        SortSpecifier[] sortSpecifiers = request.getSortBy();
        if (sortSpecifiers != null) {
            for (SortSpecifier sortSpecifier : sortSpecifiers) {
                PageOrdering ordering = (sortSpecifier.getSortDirection() == SortDirection.ASCENDING) ? PageOrdering.ASC
                    : PageOrdering.DESC;
                String columnName = sortSpecifier.getField();
                String sortField = getSortFieldForColumn(columnName);
                if (sortField != null) {
                    pageControl.addDefaultOrderingField(sortField, ordering);
                } else {
                    Log.warn("Field [" + columnName + "] in [" + getClass().getName()
                        + "] could not be mapped to a PageControl ordering field.");
                }
            }
        }
    }

    /**
     * By default, for a sortable column, the field name (usually a ListGridField name) is used as the
     * OrderingField in the PageControl passed to the server.  If for some reason the field name does not
     * properly map to a sortable entity field, this method can be overridden to provide the proper mapping.
     * Note that certain columns may also leverage sort fields in the underlying (rhq) criteria class itself, but
     * not every sortable column may be appropriate as a sort field in the criteria.      
     * 
     * @param columnName The column field name 
     * @return The entity field for the desired sort. By default just returns back the columnName. Can be null if
     * there is no valid mapping. 
     */
    protected String getSortFieldForColumn(String columnName) {
        return columnName;
    }

    @Override
    public void processResponse(String requestId, DSResponse responseProperties) {
        super.processResponse(requestId, responseProperties);
        DataSourceResponseStatistics.record(requestId, responseProperties);
    }

    protected void sendSuccessResponse(DSRequest request, DSResponse response, T dataObject) {
        sendSuccessResponse(request, response, dataObject, null);
    }

    protected void sendSuccessResponse(DSRequest request, DSResponse response, Record record) {
        sendSuccessResponse(request, response, record, null);
    }

    protected void sendSuccessResponse(DSRequest request, DSResponse response, T dataObject, Message message) {
        sendSuccessResponse(request, response, dataObject, message, null);
    }

    protected void sendSuccessResponse(DSRequest request, DSResponse response, Record record, Message message) {
        sendSuccessResponse(request, response, record, message, null);
    }

    protected void sendSuccessResponse(DSRequest request, DSResponse response, T dataObject, Message message,
        String viewPath) {
        Record record = copyValues(dataObject);
        sendSuccessResponse(request, response, record, message, viewPath);
    }

    protected void sendSuccessResponse(DSRequest request, DSResponse response, Record record, Message message,
        String viewPath) {
        response.setStatus(RPCResponse.STATUS_SUCCESS);
        response.setData(new Record[] { record });
        processResponse(request.getRequestId(), response);
        if (viewPath != null) {
            CoreGUI.goToView(viewPath, message);
        } else if (message != null) {
            CoreGUI.getMessageCenter().notify(message);
        }
    }

    protected void sendSuccessResponse(DSRequest request, DSResponse response, PageList<T> dataObjects) {
        Record[] records = buildRecords(dataObjects);
        PageList<Record> recordsPageList = new PageList<Record>(dataObjects.getPageControl());
        recordsPageList.setTotalSize(dataObjects.getTotalSize());
        recordsPageList.setUnbounded(dataObjects.isUnbounded());
        recordsPageList.addAll(Arrays.asList(records));
        sendSuccessResponseRecords(request, response, recordsPageList);
    }

    protected void sendSuccessResponseRecords(DSRequest request, DSResponse response, PageList<Record> records) {
        response.setStatus(RPCResponse.STATUS_SUCCESS);
        Record[] recordsArray = records.toArray(new Record[records.size()]);
        response.setData(recordsArray);
        setPagingInfo(response, records);
        processResponse(request.getRequestId(), response);
    }

    protected void setPagingInfo(DSResponse response, PageList<?> pageList) {
        // For paging to work, we have to specify size of full result set.
        int totalRows = (pageList.isUnbounded()) ? pageList.size() : pageList.getTotalSize();
        response.setTotalRows(totalRows);
        // Also set start row and end row in case for some reason we're not returning the same page the ListGrid
        // requested.
        //PageControl pageControl = pageList.getPageControl();
        //response.setStartRow(pageControl.getStartRow());
        //response.setEndRow(pageControl.getStartRow() + pageControl.getPageSize());
    }

    protected void sendFailureResponse(DSRequest request, DSResponse response, String message, Throwable caught) {
        CoreGUI.getErrorHandler().handleError(message, caught);
        response.setStatus(RPCResponse.STATUS_FAILURE);
        processResponse(request.getRequestId(), response);
    }

    protected void sendValidationErrorResponse(DSRequest request, DSResponse response, Map<String, String> errorMessages) {
        response.setStatus(RPCResponse.STATUS_VALIDATION_ERROR);
        response.setErrors(errorMessages);
        processResponse(request.getRequestId(), response);
    }

    /**
     * @deprecated use {@link #sendSuccessResponseRecords(DSRequest, DSResponse, PageList)} instead
     */
    @Deprecated
    protected void populateSuccessResponse(PageList<T> dataObjects, DSResponse response) {
        response.setStatus(RPCResponse.STATUS_SUCCESS);
        Record[] records = buildRecords(dataObjects);
        response.setData(records);
        setPagingInfo(response, dataObjects);
    }

    public ListGridRecord[] buildRecords(Collection<T> dataObjects) {
        return buildRecords(dataObjects, true);
    }

    public ListGridRecord[] buildRecords(Collection<T> dataObjects, boolean cascade) {
        if (dataObjects == null) {
            return null;
        }

        ListGridRecord[] records = new ListGridRecord[dataObjects.size()];
        int i = 0;
        for (T item : dataObjects) {
            records[i++] = copyValues(item, cascade);
        }
        return records;
    }

    public Set<T> buildDataObjects(Record[] records) {
        if (records == null) {
            return null;
        }

        Set<T> results = new LinkedHashSet<T>(records.length);
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
     * Given a request, this returns a criteria object that should be used to fetch data that the request
     * is asking for. If a particular data source subclass does not use criteria, this can return <code>null</code>.
     * <br/><br/>
     * IMPORTANT!  If the criteria returned does not include a PageControl it will be assigned the PageControl
     * of the DSRequest. (See https://bugzilla.redhat.com/show_bug.cgi?id=682304 for more on why we do this.) This
     * is important because it means that this criteria object will completely ignore calls made to
     * setPaging(pageNumber, pageSize) as well as addSortField(fieldName), see
     * {@link org.rhq.core.domain.criteria.Criteria#setPageControl(PageControl)}.  So, when overriding this
     * method, the preferred way to set specific paging or sorting is to manipulate the provided
     * PageControl in the DSRequest. The DSRequest can be implicitly manipulated by using InitialSortSpecifiers in
     * {@link Table} construction, or it can be manipulated manually.  It is not necessary to call
     * {@link org.rhq.core.domain.criteria.Criteria#setPageControl(PageControl)} since it will be applied
     * automatically at fetch-time (by this class).  To completely override the request's PageControl
     * you can create a new one and set it on the Criteria, but remember that you may lose paging/sorting state.
     * 
     * @param request the request being made for data
     * @return a criteria object that is to be used when fetching for the requested data, or <code>null</code> if not used
     */
    protected abstract C getFetchCriteria(final DSRequest request);

    /**
     * Extensions should implement this method to retrieve data. Paging solutions should use
     * {@link #getPageControl(com.smartgwt.client.data.DSRequest)}. All implementations should call processResponse()
     * whether they fail or succeed. Data should be set on the request via setData. Implementations can use
     * buildRecords() to get the list of records.
     *
     * @param request
     * @param response
     * @param criteria can be used by the method to perform queries in order to fetch the required data
     */
    protected abstract void executeFetch(final DSRequest request, final DSResponse response, final C criteria);

    public abstract T copyValues(Record from);

    /**
     *
     * @param from
     * @return
     */
    // TODO (ips): This really should return Records, rather than ListGridRecords, so the DataSource is not specific to
    //             ListGrids, but that will require a lot of refactoring at this point...
    public abstract ListGridRecord copyValues(T from);

    public ListGridRecord copyValues(T from, boolean cascade) {
        return copyValues(from);
    }

    /**
     * Executed on <code>REMOVE</code> operation. <code>processResponse (requestId, response)</code>
     * should be called when operation completes (either successful or failure).
     *
     * @param recordToRemove
     * @param request  <code>DSRequest</code> being processed. <code>request.getData ()</code>
     *                 contains record should be removed.
     * @param response <code>DSResponse</code>. <code>setData (list)</code> should be called on
    *                 successful execution of this method. Array should contain single element representing
     */
    protected void executeRemove(Record recordToRemove, final DSRequest request, final DSResponse response) {
        throw new UnsupportedOperationException("This dataSource does not support removals.");
    }

    /**
     * TODO
     *
     * @param recordToAdd
     * @param request
     * @param response
     */
    protected void executeAdd(Record recordToAdd, final DSRequest request, final DSResponse response) {
        throw new UnsupportedOperationException("This dataSource does not support additions.");
    }

    /**
     * TODO
     *
     * @param editedRecord
     * @param oldRecord
     * @param request
     * @param response
     */
    protected void executeUpdate(Record editedRecord, Record oldRecord, final DSRequest request,
        final DSResponse response) {
        throw new UnsupportedOperationException("This dataSource does not support updates.");
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

    public static <S> S[] getArrayFilter(DSRequest request, String paramName, Class<S> type) {
        return getArrayFilter(request.getCriteria(), paramName, type);
    }

    @SuppressWarnings("unchecked")
    public static <S> S[] getArrayFilter(Criteria criteria, String paramName, Class<S> type) {
        Map<String, Object> criteriaMap = criteria.getValues();

        S[] resultArray;
        Object value = criteriaMap.get(paramName);
        if (value == null) {
            resultArray = null;
        } else if (type == Integer.class) {
            int[] intermediates;
            if (isArray(value)) {
                intermediates = criteria.getAttributeAsIntArray(paramName);
            } else { // want array return, but only single instance of the type in the request
                intermediates = new int[] { criteria.getAttributeAsInt(paramName) };
            }
            resultArray = (S[]) new Integer[intermediates.length];
            int index = 0;
            for (int next : intermediates) {
                resultArray[index++] = (S) Integer.valueOf(next);
            }
        } else if (type == String.class) {
            String[] intermediates;
            if (isArray(value)) {
                intermediates = criteria.getAttributeAsStringArray(paramName);
            } else { // want array return, but only single instance of the type in the request
                intermediates = new String[] { criteria.getAttributeAsString(paramName) };
            }
            resultArray = (S[]) new String[intermediates.length];
            int index = 0;
            for (String next : intermediates) {
                resultArray[index++] = (S) next;
            }
        } else if (type.isEnum()) {
            String[] intermediates;
            if (isArray(value)) {
                intermediates = criteria.getAttributeAsStringArray(paramName);
            } else { // want array return, but only single instance of the type in the request
                intermediates = new String[] { criteria.getAttributeAsString(paramName) };
            }
            List<S> buffer = new ArrayList<S>();
            for (String next : intermediates) {
                buffer.add((S) Enum.valueOf((Class<? extends Enum>)type, next));
            }
            resultArray = buffer.toArray(getEnumArray(type, buffer.size()));
        } else {
            throw new IllegalArgumentException(MSG.dataSource_rpc_error_unsupportedArrayFilterType(type.getName()));
        }

        if (Log.isDebugEnabled() && resultArray != null) {
            Log.debug("Array filter: " + paramName + "=[" + Arrays.toString(resultArray) + "]");
        }

        return resultArray;
    }

    private static boolean isArray(Object value) {
        return value.getClass().isArray() || value.getClass().equals(ArrayList.class);
    }

    @SuppressWarnings("unchecked")
    private static <S> S[] getEnumArray(Class<S> genericEnumType, int size) {
        // workaround until GWT implements reflection APIs, so we can do: 
        //   array=(S[])Array.newInstance(Class<S>,capacity);
        if (genericEnumType == AlertPriority.class) {
            return (S[]) new AlertPriority[size];
        } else if (genericEnumType == EventSeverity.class) {
            return (S[]) new EventSeverity[size];
        } else if (genericEnumType == OperationRequestStatus.class) {
            return (S[]) new OperationRequestStatus[size];
        } else if (genericEnumType == ResourceCategory.class) {
            return (S[]) new ResourceCategory[size];
        } else if (genericEnumType == DriftCategory.class) {
            return (S[]) new DriftCategory[size];
        } else if (genericEnumType == ExecutionStatus.class) {
            return (S[]) new ExecutionStatus[size];
        } else if (genericEnumType == PartitionEventType.class) {
            return (S[]) new PartitionEventType[size];
        } else if (genericEnumType == Server.OperationMode.class) {
            return (S[]) new Server.OperationMode[size];
        } else if (genericEnumType == StorageNode.OperationMode.class) {
            return (S[]) new StorageNode.OperationMode[size];
        } else if (genericEnumType == AlertFilter.class) {
            return (S[]) new AlertFilter[size];
        } else {
            throw new IllegalArgumentException(MSG.dataSource_rpc_error_unsupportedEnumType(genericEnumType.getName()));
        }
    }

    @SuppressWarnings("unchecked")
    public static void printRequestCriteria(DSRequest request) {
        Criteria criteria = request.getCriteria();
        Map<String, Object> criteriaMap = criteria.getValues();

        for (Map.Entry<String, Object> nextEntry : criteriaMap.entrySet()) {
            Log.debug("Request Criteria: " + nextEntry.getKey() + ":" + nextEntry.getValue());
        }
    }

    public static <S> S getFilter(DSRequest request, String paramName, Class<S> type) {
        return getFilter(request.getCriteria(), paramName, type);
    }

    @SuppressWarnings("unchecked")
    public static <S> S getFilter(Criteria criteria, String paramName, Class<S> type) {

        Map<String, Object> criteriaMap = (criteria != null) ? criteria.getValues() : Collections
            .<String, Object> emptyMap();

        S result;
        Object value = criteriaMap.get(paramName);
        if (value == null || value.toString().equals("")) {
            result = null;
        } else {
            String strValue = value.toString();
            if (type == String.class) {
                result = (S) strValue;
            } else if (type == Integer.class) {
                result = (S) Integer.valueOf(strValue);
            } else if (type == Long.class) {
                result = (S) Long.valueOf(strValue);
            } else if (type.isEnum()) {
                result = (S) Enum.valueOf((Class<? extends Enum>) type, strValue);
            } else {
                result = (S) value; // otherwise presume the object is already that type, and just cast it
            }
        }

        //if (Log.isDebugEnabled() && result != null) {
        Log.debug("Filter: " + paramName + "=[" + result + "]");
        //}

        return result;
    }

    protected static DataSourceTextField createTextField(String name, String title, Integer minLength,
        Integer maxLength, Boolean required) {
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

    protected static DataSourceTextField createBooleanField(String name, String title, Boolean required) {
        DataSourceTextField textField = new DataSourceTextField(name, title);
        textField.setLength(Boolean.FALSE.toString().length());
        textField.setRequired(required);
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        valueMap.put(Boolean.TRUE.toString(), MSG.common_val_yes_lower());
        valueMap.put(Boolean.FALSE.toString(), MSG.common_val_no_lower());
        textField.setValueMap(valueMap);
        return textField;
    }

    protected static DataSourceIntegerField createIntegerField(String name, String title, Integer minValue,
        Integer maxValue, Boolean required) {
        DataSourceIntegerField textField = new DataSourceIntegerField(name, title);
        textField.setRequired(required);
        if (minValue != null || maxValue != null) {
            IntegerRangeValidator integerRangeValidator = new IntegerRangeValidator();
            if (minValue != null) {
                integerRangeValidator.setMin(minValue);
            } else {
                integerRangeValidator.setMin(Integer.MIN_VALUE);
            }
            if (maxValue != null) {
                integerRangeValidator.setMax(maxValue);
            } else {
                integerRangeValidator.setMax(Integer.MAX_VALUE);
            }

            textField.setValidators(integerRangeValidator);
        }
        return textField;
    }

    protected static Date convertTimestampToDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        // Assume 0 means null, not Jan 1, 1970.
        return (timestamp != 0) ? new Date(timestamp) : null;
    }

}
