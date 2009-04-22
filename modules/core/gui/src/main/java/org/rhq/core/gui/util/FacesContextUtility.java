/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.gui.util;

import java.lang.reflect.Method;
import java.util.Iterator;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jboss.seam.core.Manager;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.util.Strings;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * A set of utility methods for working with the current JSF {@link FacesContext}.
 *
 * @author Ian Springer
 */
public abstract class FacesContextUtility {
    @Nullable
    public static String getOptionalRequestParameter(@NotNull String paramName) {
        return getOptionalRequestParameter(paramName, String.class, null);
    }

    @Nullable
    public static <T> T getOptionalRequestParameter(@NotNull String paramName, Class<T> type) {
        return getOptionalRequestParameter(paramName, type, null);
    }

    @Nullable
    public static String getOptionalRequestParameter(@NotNull String paramName, String defaultValue) {
        return getOptionalRequestParameter(paramName, String.class, defaultValue);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getOptionalRequestParameter(@NotNull String paramName, Class<T> type, T defaultValue) {
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
    public static String getRequiredRequestParameter(@NotNull String paramName) {
        return getRequiredRequestParameter(paramName, String.class);
    }

    @NotNull
    public static <T> T getRequiredRequestParameter(@NotNull String paramName, Class<T> type) {
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

    public static HttpServletResponse getResponse() {
        HttpServletResponse response = (HttpServletResponse) getFacesContext().getExternalContext().getResponse();
        return response;
    }

    /**
     * Adds a message to the global context so it will be displayed via <code>&lt;h:messages&gt;</code>. The message
     * will have no detail.
     *
     * @param severity the FacesMessage severity (<code>FacesMessage.SEVERITY_WARN</code>, etc.)
     * @param summary  localized summary message text
     */
    public static void addMessage(@NotNull Severity severity, @NotNull String summary) {
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
    public static void addMessage(@NotNull Severity severity, @NotNull String summary, @Nullable Throwable t) {
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
    public static void addMessage(@NotNull Severity severity, @NotNull String summary, @Nullable String detail) {
        //Now that we're using Seam for some of the pages, we use different mechanisms for transferring messages
        //across redirect boundaries.
        //If we are inside a long running conversation, we want Seam to do its thing, otherwise 
        //FacesmessagePropogationPhaseListener will step in eventually.
        if (Manager.instance().isReallyLongRunningConversation()) {
            //nothing's easy... Seam StatusMessage (which we're creating here) considers empty string the same
            //as null. If the message is then eventually shown on the page, the null detail is replaced with the
            //summary which would leave us with the summary displayed twice on the page.
            //Because the detail therefore must not be empty, we must resort to an ugly hack here and put in a unicode
            //for non-breakable space:
            String detailToUse = Strings.isEmpty(detail) ? "\u00a0" : detail;
            FacesMessages.instance().add(toSeverity(severity), null, null, summary, detailToUse);
        } else {
            getFacesContext().addMessage(null, new FacesMessage(severity, summary, (detail != null) ? detail : ""));
        }
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

        // first try to look in the request for the bean
        T result = (T) getRequest().getAttribute(clazz);

        if (result == null) {
            // look in the session, if the bean wasn't found in the request
            result = (T) getRequest().getSession().getAttribute(clazz);
        }

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

    public static <T> void removeSessionScopedBean(Class<T> type) {
        String fqn = type.getName();
        String clazz = fqn.substring(fqn.lastIndexOf('.') + 1);
        getRequest().getSession().removeAttribute(clazz);
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

    private static String createDetailMessage(@Nullable Throwable t) {
        if (t == null)
            return null;

        StringBuilder detailMessage = new StringBuilder("Cause: ");
        if (t.getCause() instanceof WrappedRemotingException) {
            WrappedRemotingException wre = (WrappedRemotingException) t.getCause();
            detailMessage.append(wre.getActualException().getAllMessages());
        } else {
            detailMessage.append(ThrowableUtil.getAllMessages(t));
        }

        return detailMessage.toString();
    }

    /**
     * Convert a FacesMessage.Severity to a StatusMessage.Severity
     * 
     * Taken from {@link org.jboss.seam.faces.FacesMessages} private method. 
     */
    private static org.jboss.seam.international.StatusMessage.Severity toSeverity(
        javax.faces.application.FacesMessage.Severity severity) {
        if (FacesMessage.SEVERITY_ERROR.equals(severity)) {
            return org.jboss.seam.international.StatusMessage.Severity.ERROR;
        } else if (FacesMessage.SEVERITY_FATAL.equals(severity)) {
            return org.jboss.seam.international.StatusMessage.Severity.FATAL;
        } else if (FacesMessage.SEVERITY_INFO.equals(severity)) {
            return org.jboss.seam.international.StatusMessage.Severity.INFO;
        } else if (FacesMessage.SEVERITY_WARN.equals(severity)) {
            return org.jboss.seam.international.StatusMessage.Severity.WARN;
        } else {
            return null;
        }
    }
}