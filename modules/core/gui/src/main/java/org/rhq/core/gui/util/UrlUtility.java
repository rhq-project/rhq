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

import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A set of utility methods for working with URLs.
 *
 * @author Ian Springer
 */
public abstract class UrlUtility {
    /**
     * Add the specified parameters to the specified URL's query string.
     *
     * @param  url        a URL
     * @param  parameters a map of request parameters
     *
     * @return the updated URL
     */
    @NotNull
    public static String addParametersToQueryString(@NotNull
    String url, @Nullable
    Map<String, String> parameters) {
        if ((parameters == null) || parameters.isEmpty()) {
            return url;
        }

        StringBuilder updatedURL = new StringBuilder(url);
        if (updatedURL.lastIndexOf("?") == -1) {
            // URL does not have an existing query string.
            updatedURL.append('?');
        } else {
            // URL has an existing query string.
            char lastChar = updatedURL.charAt(updatedURL.length() - 1);

            // Avoid "?&" and "&&" which causes a JSF WARN log message to be emitted.
            if ((lastChar != '&') && (lastChar != '?')) {
                updatedURL.append('&');
            }
        }

        Set<String> paramNames = parameters.keySet();
        for (String paramName : paramNames) {
            updatedURL.append(paramName);
            updatedURL.append('=');
            updatedURL.append(parameters.get(paramName));
            updatedURL.append('&');
        }

        updatedURL.deleteCharAt(updatedURL.length() - 1);
        return updatedURL.toString();
    }
}