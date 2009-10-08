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
package org.rhq.helpers.rtfilter.filter;

import java.io.IOException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Extend the ServletResponse to be able to retrieve the status code. I would love to correctly use @Override, but this
 * needs to run with jre1.4 as well.
 *
 * @author Heiko W. Rupp
 */
public class RtFilterResponseWrapper extends HttpServletResponseWrapper {
    /**
     * Http status code (200=ok, 404=not found, 500=error etc. Default is 200, as the code only gets set when there is
     * something not ok.
     */
    private int statusCode = 200;

    public RtFilterResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    public RtFilterResponseWrapper(ServletResponse response) {
        super((HttpServletResponse) response);
    }

    public void setStatus(int code) {
        statusCode = code;
        super.setStatus(code);
    }

    public void setStatus(int code, String msg) {
        statusCode = code;
        super.setStatus(code, msg);
    }

    public int getStatus() {
        return statusCode;
    }

    public void sendError(int sc) throws IOException {
        statusCode = sc;
        super.sendError(sc);
    }

    public void sendError(int sc, String msg) throws IOException {
        statusCode = sc;
        super.sendError(sc, msg);
    }
}