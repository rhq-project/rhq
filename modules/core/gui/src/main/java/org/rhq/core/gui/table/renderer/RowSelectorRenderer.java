/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
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
package org.rhq.core.gui.table.renderer;

import org.ajax4jsf.component.UIDataAdaptor;
import org.ajax4jsf.model.ExtendedDataModel;
import org.ajax4jsf.resource.InternetResource;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.gui.table.component.RowSelectorComponent;
import org.rhq.core.gui.util.FacesComponentUtility;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.model.DataModel;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An HTML renderer for {@link RowSelectorComponent}s (i.e. rhq:rowSelector).
 *
 * @author Ian Springer
 */
public class RowSelectorRenderer extends AbstractRenderer {    
    private static final String TABLE_SCRIPT = "/org/rhq/core/gui/table/renderer/js/table.js";

    private InternetResource[] scripts; // Could this be made static?

    @Override
    @SuppressWarnings("unchecked")
    public void decode(FacesContext context, UIComponent component) {
        validateParameters(context, component);

        RowSelectorComponent rowSelector = (RowSelectorComponent) component;

        UIData data = getEnclosingData(rowSelector);

        // We store the List of selected data objects in a request context attribute and add to it as the data table's
        // rows are iterated during the updateModelValues phase. If the row is selected, we add the corresponding
        // data object to the List.
        List selectedDataObjects = getSelectedRowDataObjects(context, data);

        // Always set the submitted value, so that, even if no rows were selected, the setter on the managed bean will
        // still be passed an empty List, rather than null.
        rowSelector.setSubmittedValue(selectedDataObjects);

        Set<String> selectedRowKeys = getSelectedRowKeys(context, data, rowSelector);

        String rowKeyString = getRowKeyAsString(context, component, data);
        if (selectedRowKeys.contains(rowKeyString)) {
            // The current row is selected - add the row's data object to our List.
            Object rowData = data.getRowData();
            selectedDataObjects.add(rowData);
        }
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        validateParameters(context, component);

        RowSelectorComponent rowSelector = (RowSelectorComponent) component;

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("input", component);

        writeIdAttributeIfNecessary(context, writer, component);

        RowSelectorComponent.Mode selectionMode = rowSelector.getMode();
        String type = (selectionMode == RowSelectorComponent.Mode.single) ? "radio" : "checkbox";
        writer.writeAttribute("type", type, "type");

        String clientId = component.getClientId(context);
        writer.writeAttribute("name", clientId, "clientId");

        UIData data = getEnclosingData(rowSelector);
        Object rowKey = getRowKey(data);
        writer.writeAttribute("value", rowKey, null);

        // TODO: Write 'checked' attribute to allow checkbox to be selected by default? Probably overkill.

        String onclick = "updateButtons('" + clientId + "')";
        String userSpecifiedOnclick = (String) rowSelector.getAttributes().get("onclick");
        if (userSpecifiedOnclick != null) {
            onclick += "; " + userSpecifiedOnclick;
        }        
        writer.writeAttribute("onclick", onclick, "onclick");
        // TODO: Add support for all the other common HTML attributes.
        //RenderKitUtils.renderPassThruAttributes(writer, component, ATTRIBUTES);

        Boolean disabled = (Boolean) rowSelector.getAttributes().get("disabled");
        if (disabled != null && disabled) {
            writer.writeAttribute("disabled", "disabled", "disabled");
        }
        //RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);

        writer.endElement("input");
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        validateParameters(context, component);
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("input");
    }

    @NotNull
    private List getSelectedRowDataObjects(FacesContext context, UIData data) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        initializeComponentId(context, data);
        String selectedRowDataObjectsRequestAttributeName = data.getId() + ":" + "selectedRowDataObjects";
        List selectedDataObjects = (List) requestMap.get(selectedRowDataObjectsRequestAttributeName);
        if (selectedDataObjects == null) {
            selectedDataObjects = new ArrayList();
            requestMap.put(selectedRowDataObjectsRequestAttributeName, selectedDataObjects);
        }
        return selectedDataObjects;
    }

    /*
     * Returns an immutable Set containing the String representations of the selected row keys.
     */
    @NotNull
    private Set<String> getSelectedRowKeys(FacesContext context, UIData data, RowSelectorComponent rowSelector) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        initializeComponentId(context, data);
        String selectedRowKeysRequestAttributeName = data.getId() + ":" + "selectedRowKeys";
        Set<String> selectedRowKeys = (Set<String>) requestMap.get(selectedRowKeysRequestAttributeName);
        if (selectedRowKeys == null) {
            Map<String, String[]> requestParamMap = context.getExternalContext().getRequestParameterValuesMap();
            String requestParamName = rowSelector.getClientId(context);
            String[] selectedRowKeyArray = requestParamMap.get(requestParamName);
            if (selectedRowKeyArray != null) {
                selectedRowKeys = new HashSet<String>(selectedRowKeyArray.length);
                Collections.addAll(selectedRowKeys, selectedRowKeyArray);
                selectedRowKeys = Collections.unmodifiableSet(selectedRowKeys);
            } else {
                selectedRowKeys = Collections.emptySet();
            }
        }
        return selectedRowKeys;
    }

    private Object getRowKey(UIData data) {
        Object rowKey;
        DataModel dataModel = (DataModel) data.getAttributes().get("dataModel");
        if (data instanceof UIDataAdaptor && dataModel instanceof ExtendedDataModel) {
            UIDataAdaptor dataAdaptor = (UIDataAdaptor) data;
            rowKey = dataAdaptor.getRowKey();
        } else {
            Object rowData = data.getRowData();
            rowKey = getPrimaryKey(rowData);
        }
        return rowKey;
    }

    private String getRowKeyAsString(FacesContext context, UIComponent component, UIData data) {
        String rowKeyString;
        Object rowKey = getRowKey(data);
        if (data instanceof UIDataAdaptor) {
            UIDataAdaptor dataAdaptor = (UIDataAdaptor) data;
            Converter rowKeyConverter = dataAdaptor.getRowKeyConverter();
            rowKeyString = rowKeyConverter.getAsString(context, component, rowKey);
        } else {
            rowKeyString = String.valueOf(rowKey);
        }
        return rowKeyString;
    }

    /**
     * Return the specified row data object's primary key. Pretty much all RHQ domain objects use an Integer field named
     * 'id' as the primary key.
     *
     * TODO: Nevertheless, it would be nice to provide a way for the user to override this via an attribute on the
     *       rowSelector component.
     *
     * @param object a row data object
     * @return the row data object's primary key - typically an Integer for RHQ domain objects
     */
    private Object getPrimaryKey(Object object) {
        Method method;
        try {
            method = object.getClass().getMethod("getId");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(object.getClass() + " does not define a public getId() method.");
        }
        try {
            return method.invoke(object);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke getId() on " + object + ".", e);
        }
    }

    @NotNull
    private UIData getEnclosingData(UIComponent component) {
        UIData data = FacesComponentUtility.getAncestorOfType(component, UIData.class);
        if (data == null) {
            throw new IllegalStateException("Enclosing UIData component (i.e. h:dataTable or rich:*dataTable) not found for component "
                    + component + ".");
        }
        return data;
    }

    /* (non-Javadoc)
     * @see org.ajax4jsf.renderkit.HeaderResourcesRendererBase#getScripts()
     */
    protected InternetResource[] getScripts() {
    	synchronized (this) {
        	if (scripts == null) {
    			scripts = new InternetResource[] { getResource(TABLE_SCRIPT) };				    			 			
    		}
		}
    	return scripts;
	}
}
