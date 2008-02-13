/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.gui.util;

import java.lang.reflect.Method;
import java.util.Iterator;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * A set of utility methods for working with the current JSF {@link FacesContext}.
 *
 * @author Ian Springer
 */
public abstract class FacesContextUtility {
    @Nullable
    public static String getOptionalRequestParameter(@NotNull
    String paramName) {
        return getOptionalRequestParameter(paramName, String.class, null);
    }

    @Nullable
    public static <T> T getOptionalRequestParameter(@NotNull
    String paramName, Class<T> type) {
        return getOptionalRequestParameter(paramName, type, null);
    }

    @Nullable
    public static String getOptionalRequestParameter(@NotNull
    String paramName, String defaultValue) {
        return getOptionalRequestParameter(paramName, String.class, defaultValue);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getOptionalRequestParameter(@NotNull
    String paramName, Class<T> type, T defaultValue) {
        FacesContext facesContext = getFacesContext();
        String paramValue = facesContext.getExternalContext().getRequestParameterMap().get(paramName);

        T result;
        if (paramValue != null) {
            if (type == String.class) {
                result = (T) paramValue; // cast string to self-type
            } else {
                try {
                    if (type == Boolean.class) {
                        if (paramValue.equalsIgnoreCase("on") || paramValue.equalsIgnoreCase("yes")) {
                            paramValue = "true"; // flexible support for boolean translations from forms
                        }
                    }

                    Method m = type.getMethod("valueOf", String.class);
                    result = (T) m.invoke(null, paramValue); // static method
                } catch (Exception e) {
                    result = null;
                }
            }
        } else {
            result = defaultValue;
        }

        return result;
    }

    @NotNull
    public static String getRequiredRequestParameter(@NotNull
    String paramName) {
        return getRequiredRequestParameter(paramName, String.class);
    }

    @NotNull
    public static <T> T getRequiredRequestParameter(@NotNull
    String paramName, Class<T> type) {
        T paramValue = getOptionalRequestParameter(paramName, type, null);
        if (paramValue == null) {
            throw new IllegalStateException("Required request parameter '" + paramName + "' is missing.");
        }

        return paramValue;
    }

    /**
     * Return the current request.
     *
     * @return the current request
     */
    public static HttpServletRequest getRequest() {
        HttpServletRequest request = (HttpServletRequest) getFacesContext().getExternalContext().getRequest();
        return request;
    }

    /**
     * Adds a message to the global context so it will be displayed via <code>&lt;h:messages&gt;</code>. The message
     * will have no detail.
     *
     * @param severity the FacesMessage severity (<code>FacesMessage.SEVERITY_WARN</code>, etc.)
     * @param summary  localized summary message text
     */
    public static void addMessage(@NotNull
    Severity severity, @NotNull
    String summary) {
        String detail = "";
        addMessage(severity, summary, detail);
    }

    /**
     * Adds a message to the global context so it will be displayed via <code>&lt;h:messages&gt;</code>.
     *
     * @param severity the FacesMessage severity (<code>FacesMessage.SEVERITY_WARN</code>, etc.)
     * @param summary  localized summary message text
     * @param t        an exception that will be used to construct the detail portion of the message; if null, no detail
     *                 will be included in the message
     */
    public static void addMessage(@NotNull
    Severity severity, @NotNull
    String summary, @Nullable
    Throwable t) {
        String detail = createDetailMessage(t);
        addMessage(severity, summary, detail);
    }

    /**
     * Adds a message to the global context so it will be displayed via <code>&lt;h:messages&gt;</code>.
     *
     * @param severity the FacesMessage severity (<code>FacesMessage.SEVERITY_WARN</code>, etc.)
     * @param summary  localized summary message text
     * @param detail   localized detail message text; if null, no detail will be included in the message
     */
    public static void addMessage(@NotNull
    Severity severity, @NotNull
    String summary, @Nullable
    String detail) {
        getFacesContext().addMessage(null, new FacesMessage(severity, summary, (detail != null) ? detail : ""));
    }

    @NotNull
    public static FacesContext getFacesContext() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext;
    }

    @SuppressWarnings("unchecked")
    // TODO: Do this instead by evaluating an EL expression.
    public static <T> T getBean(Class<T> type) {
        String fqn = type.getName();
        String clazz = fqn.substring(fqn.lastIndexOf('.') + 1);

        T result = (T) getRequest().getAttribute(clazz);

        if (result == null) {
            try {
                result = type.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            setBean(result);
        }

        return result;
    }

    public static <T> T getManagedBean(Class<T> type) {
        ValueExpression managedBeanValueExpression = getValueExpressionForManagedBean(type);
        T managedBean = FacesExpressionUtility.getValue(managedBeanValueExpression, type);
        return managedBean;
    }

    public static void setManagedBean(Object managedBean) {
        ValueExpression managedBeanValueExpression = getValueExpressionForManagedBean(managedBean.getClass());
        FacesExpressionUtility.setValue(managedBeanValueExpression, managedBean);
    }

    private static <T> ValueExpression getValueExpressionForManagedBean(Class<T> type) {
        String managedBeanName = type.getSimpleName();
        String managedBeanExpressionString = "#{" + managedBeanName + "}";
        ValueExpression managedBeanValueExpression = FacesExpressionUtility.createValueExpression(
            managedBeanExpressionString, type);
        return managedBeanValueExpression;
    }

    @SuppressWarnings("unchecked")
    // TODO: Do this instead by evaluating an EL expression.
    public static void setBean(Object object) {
        HttpServletRequest request = FacesContextUtility.getRequest();
        request.setAttribute(object.getClass().getSimpleName(), object);
    }

    public static void clearMessages() {
        Iterator<FacesMessage> messages = FacesContext.getCurrentInstance().getMessages();
        while (messages.hasNext()) {
            messages.next();
            messages.remove();
        }
    }

    public static void clearMessages(String clientId) {
        Iterator<FacesMessage> messages = FacesContext.getCurrentInstance().getMessages(clientId);
        while (messages.hasNext()) {
            messages.next();
            messages.remove();
        }
    }

    private static String createDetailMessage(@Nullable
    Throwable t) {
        if (t == null) {
            return null;
        }

        String detailMessage = t.getLocalizedMessage() + " - Cause: ";
        if (t.getCause() instanceof WrappedRemotingException) {
            WrappedRemotingException wre = (WrappedRemotingException) t.getCause();
            detailMessage += wre.getActualException().getAllMessages();
        } else {
            detailMessage += ThrowableUtil.getAllMessages(t);
        }

        return detailMessage;
    }
}