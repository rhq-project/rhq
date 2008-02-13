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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Filter used to handle multipart/form-data requests.
 *
 * @author Jason Dobies
 */
public class MultipartFilter implements Filter {
    // Filter Implementation  --------------------------------------------

    public void init(FilterConfig config) throws ServletException {
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException {
        // Sanity check
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // If it's not multipart content, simply chain. The URL pattern of the filter should minimize when this occurs.
        boolean isMultipartContent = ServletFileUpload.isMultipartContent(httpRequest);
        if (!isMultipartContent) {
            chain.doFilter(request, response);
            return;
        }

        DiskFileItemFactory factory = new DiskFileItemFactory();

        // TODO: jdobies, Jun 20, 2007: Configure factory

        ServletFileUpload upload = new ServletFileUpload(factory);

        try {
            MultipartRequestWrapper requestWrapper = new MultipartRequestWrapper(httpRequest);
            Map<String, String[]> parameterMap = requestWrapper.getParameterMap();

            List<FileItem> items = (List<FileItem>) upload.parseRequest(httpRequest);

            for (FileItem item : items) {
                String itemString = item.getString();
                String fieldName = item.getFieldName();

                if (item.isFormField()) {
                    parameterMap.put(fieldName, new String[] { itemString });
                } else {
                    httpRequest.setAttribute(fieldName, item);
                }
            }

            chain.doFilter(requestWrapper, response);
        } catch (FileUploadException e) {
            throw new ServletException(e);
        }
    }
}