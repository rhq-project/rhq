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
package org.rhq.enterprise.gui.common.upload;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Request wrapper to allow parameters to be read via <code>getParameter()</code>.
 *
 * @author Jason Dobies
 */
public class MultipartRequestWrapper extends HttpServletRequestWrapper {
    // Attributes  --------------------------------------------

    private Map<String, String[]> parameterMap = new HashMap<String, String[]>();

    // Constructors  --------------------------------------------

    public MultipartRequestWrapper(HttpServletRequest httpServletRequest) {
        super(httpServletRequest);
    }

    // HttpServletRequestWrapper Overridden Methods --------------------------------------------

    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    public String getParameter(String name) {
        String[] params = getParameterValues(name);
        if (params == null) {
            return null;
        }

        return params[0];
    }

    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }
}