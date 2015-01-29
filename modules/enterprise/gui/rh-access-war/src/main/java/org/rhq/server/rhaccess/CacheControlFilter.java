/*
 * RHQ Management Platform
 * Copyright (C) 2015 Red Hat, Inc.
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
package org.rhq.server.rhaccess;

import java.io.IOException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Specify the cache policy browsers should use for GWT-compiled files, as well as static images. See
 * http://code.google.com/webtoolkit/doc/latest/DevGuideCompilingAndDebugging.html#perfect_caching for more info on
 * caching of GWT-compiled files.
 *
 * @author Libor Zoubek
 */
public class CacheControlFilter implements Filter {

    public void init(FilterConfig config) throws ServletException {
        return;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String requestURI = httpServletRequest.getRequestURI();

        if (requestURI.endsWith(".js") || requestURI.endsWith(".css") || requestURI.endsWith(".html")) {
            // Tell browser to disable caching of the content.
            Date now = new Date();
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setDateHeader("Date", now.getTime());
            httpResponse.setDateHeader("Expires", now.getTime() - 1);
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Cache-Control", "public, max-age=0, must-revalidate");
        }
        filterChain.doFilter(request, response);
    }

    public void destroy() {
        return;
    }

}
