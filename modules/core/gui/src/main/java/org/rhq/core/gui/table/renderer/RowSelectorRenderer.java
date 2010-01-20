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

import com.sun.faces.util.MessageUtils;
import org.ajax4jsf.component.UIDataAdaptor;
import org.ajax4jsf.model.ExtendedDataModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.gui.table.component.RowSelectorComponent;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.model.DataModel;
import javax.faces.render.Renderer;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An HTML renderer for {@link RowSelectorComponent}s (i.e. rhq:rowSelector).
 *
 * @author Ian Springer
 */
public class RowSelectorRenderer extends Renderer {
    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    @SuppressWarnings("unchecked")
    public void decode(FacesContext context, UIComponent component) {
        validateParameters(context, component);

        RowSelectorComponent rowSelector = (RowSelectorComponent) component;

        UIDataAdaptor dataAdaptor = getEnclosingDataAdaptor(rowSelector);

        List selectedDataObjects = getSelectedDataObjects(context, dataAdaptor);
        
        // Always set the submitted value, so that, even if no rows were selected, the setter on the managed bean will
        // still be passed an empty List, rather than null.
        rowSelector.setSubmittedValue(selectedDataObjects);

        Map<String, String> requestParamMap = context.getExternalContext().getRequestParameterMap();
        String requestParamName = rowSelector.getClientId(context);
        String selectedRowKey = requestParamMap.get(requestParamName);

        if (selectedRowKey != null) {
            Object rowKey = dataAdaptor.getRowKey();
            Converter rowKeyConverter = dataAdaptor.getRowKeyConverter();
            String rowKeyString = (rowKeyConverter != null) ? rowKeyConverter.getAsString(context, component, rowKey) :
                    String.valueOf(rowKey);            
            if (selectedRowKey.equals(rowKeyString)) {
                Object rowData = dataAdaptor.getRowData();
                selectedDataObjects.add(rowData);
            }
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

        UIDataAdaptor dataAdaptor = getEnclosingDataAdaptor(rowSelector);
        Object rowKey = getRowKey(dataAdaptor);
        writer.writeAttribute("value", rowKey, null);

        // TODO: Write 'checked' attribute to allow checkbox to be selected by default? Probably overkill.

        String onclick = "updateButtons('" + clientId + "')";
        String userSpecifiedOnclick = (String) rowSelector.getAttributes().get("onclick");
        if (userSpecifiedOnclick != null) {
            onclick += "; " + userSpecifiedOnclick;
        }
        rowSelector.getAttributes().put("onclick", onclick);
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
        writer.write("</input>");
    }

    private void validateParameters(FacesContext context, UIComponent component) {
        if (context == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "context"));
        }

        if (component == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "component"));
        }
    }

    private List getSelectedDataObjects(FacesContext context, UIDataAdaptor dataAdaptor) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        String selectedDataObjectsRequestAttributeName = dataAdaptor.getId() + ":" + "selectedDataObjects";
        List selectedDataObjects = (List) requestMap.get(selectedDataObjectsRequestAttributeName);
        if (selectedDataObjects == null) {
            selectedDataObjects = new ArrayList();
            requestMap.put(selectedDataObjectsRequestAttributeName, selectedDataObjects);
        }
        return selectedDataObjects;
    }

    private Object getRowKey(UIDataAdaptor dataAdaptor) {
        Object rowKey;
        DataModel dataModel = (DataModel) dataAdaptor.getAttributes().get("dataModel");
        if (dataModel instanceof ExtendedDataModel) {
            rowKey = dataAdaptor.getRowKey();
        } else {
            Object rowData = dataAdaptor.getRowData();
            rowKey = getPrimaryKey(rowData);
        }
        return rowKey;
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

    private UIDataAdaptor getEnclosingDataAdaptor(UIComponent component) {
        UIComponent current = component;
        while ((current = current.getParent()) != null) {
            if (current instanceof UIDataAdaptor) {
                return (UIDataAdaptor) current;
            }
        }
        throw new IllegalStateException("Enclosing UIDataAdaptor component (i.e. rich:*dataTable) not found for component "
                + component + ".");
    }

    /**
     * @param component the component of interest
     *
     * @return true if this renderer should render an id attribute.
     */
    protected boolean shouldWriteIdAttribute(UIComponent component) {
        String id;
        return (null != (id = component.getId()) &&
                    !id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX));

    }

    protected String writeIdAttributeIfNecessary(FacesContext context,
                                                 ResponseWriter writer,
                                                 UIComponent component) {
        String id = null;
        if (shouldWriteIdAttribute(component)) {
            try {
                writer.writeAttribute("id", id = component.getClientId(context),
                                      "id");
            } catch (IOException e) {
                String message = MessageUtils.getExceptionMessageString
                      (MessageUtils.CANT_WRITE_ID_ATTRIBUTE_ERROR_MESSAGE_ID,
                       e.getMessage());
                log.warn(message);
            }
        }
        return id;
    }
}
