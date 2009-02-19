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
package org.rhq.enterprise.gui.common.paging;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

/**
 * @author Joseph Marques
 */
public class PageControlELResolver extends ELResolver {

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null; // for gui tools
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null; // for gui tools
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        Class<?> result = null;

        if (context == null) {
            throw new NullPointerException("ELContext was null for getType method in PageControlELResolver");
        }

        if (base == null) {
            // We don't handle setting top-level implicit objects.
        } else if (base.equals(PageControlView.class) || (base instanceof PageControlView)) {
            String propertyName = property.toString().toLowerCase();

            if ("pagesize".equals(propertyName)) {
                result = Integer.class;
            } else if ("pagenumber".equals(propertyName)) {
                result = Integer.class;
            } else if ("unlimited".equals(propertyName)) {
                result = Boolean.class;
            } else {
                throw new PropertyNotWritableException(
                    "Only the pageSize and pageNumber properties of a PageControl object can be resolved");
            }

            context.setPropertyResolved(true);
        }

        return result;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException("ELContext was null for getValue method in PageControlELResolver");
        }

        Object result = null;

        if (base == null) {
            // Resolving first two variables (e.g. ${PageControlView.something}).
            String propertyName = (String) property;
            if ("PageControl".equals(propertyName)) {
                result = PageControlView.class;
                context.setPropertyResolved(true);
            }
        } else if (PageControlView.class.equals(base)) {
            // Getting a specific PageControlView instance

            String viewName = property.toString();

            result = PageControlView.valueOf(viewName);
            context.setPropertyResolved(true);
        } else if (base instanceof PageControlView) {
            // cast to required types
            PageControlView view = (PageControlView) base;
            String methodName = (String) property;
            String lowerCaseMethodName = methodName.toLowerCase();
            log.debug("accessing PageControl." + methodName);
            // allows simple misspellings for developer productivity
            if ("pagesize".equals(lowerCaseMethodName)) {
                // find the user for this session-based operation
                WebUser user = EnterpriseFacesContextUtility.getWebUser();
                WebUserPreferences preferences = user.getWebPreferences();
                // get it
                PageControl pc = preferences.getPageControl(view);
                log.debug("Getting PageControlView[" + view + "] to " + pc);
                result = pc.getPageSize();
            } else if ("pagenumber".equals(lowerCaseMethodName)) {
                // find the user for this session-based operation
                WebUser user = EnterpriseFacesContextUtility.getWebUser();
                WebUserPreferences preferences = user.getWebPreferences();
                // get it
                PageControl pc = preferences.getPageControl(view);
                log.debug("Getting PageControlView[" + view + "] to " + pc);
                result = pc.getPageNumber() + 1; // RF data table is 1-based, our PageControl is 0-based
            } else if ("unlimited".equals(lowerCaseMethodName)) {
                result = view.isUnlimited();
            } else {
                throw new PropertyNotFoundException("The " + methodName
                    + " property of a PageControl object is not accessible");
            }

            // don't let other resolvers touch this
            context.setPropertyResolved(true);
        }

        return result;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        if (context == null) {
            throw new NullPointerException("ELContext was null for setValue method in PageControlELResolver");
        }

        if (base instanceof PageControlView) {
            // cast to required types
            PageControlView view = (PageControlView) base;
            String methodName = (String) property;
            String lowerCaseMethodName = methodName.toLowerCase();

            // allows simple mispellings for developer productivity
            if ("pagesize".equals(lowerCaseMethodName)) {
                if (value != null) {
                    // find the user for this session-based operation
                    WebUser user = EnterpriseFacesContextUtility.getWebUser();
                    WebUserPreferences preferences = user.getWebPreferences();

                    // update it
                    PageControl pc = preferences.getPageControl(view);
                    int pageSize = (Integer) value;
                    pc.setPageSize(pageSize);
                    pc.setPageNumber(0); // reset the page number too

                    log.debug("Setting PageControlView[" + view + "] to " + pc);
                    preferences.setPageControl(view, pc);
                }

                // don't let other resolvers touch this
                context.setPropertyResolved(true);
            } else if ("pagenumber".equals(lowerCaseMethodName)) {
                if (value != null) {
                    // find the user for this session-based operation
                    WebUser user = EnterpriseFacesContextUtility.getWebUser();
                    WebUserPreferences preferences = user.getWebPreferences();

                    // update it
                    PageControl pc = preferences.getPageControl(view);
                    int pageNumber = (Integer) value;
                    pc.setPageNumber(pageNumber - 1); // RF data table is 1-based, our PageControl is 0-based

                    log.debug("Setting PageControlView[" + view + "] to " + pc);
                    preferences.setPageControl(view, pc);
                }

                // don't let other resolvers touch this
                context.setPropertyResolved(true);
            } else {
                throw new PropertyNotFoundException("The " + methodName
                    + " property of a PageControl object can not be set, only pageSize");
            }
        }
    }
}