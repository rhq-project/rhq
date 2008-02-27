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
package org.rhq.enterprise.gui.image.chart;

import org.rhq.enterprise.gui.image.data.IStackedDataPoint;

public class PerfDataPointCollection extends DataPointCollection {
    public static final int UNKNOWN = 0;
    public static final int ENDUSER = 1;
    public static final int WEBSERVER = 2;
    public static final int APPSERVER = 3;

    private static final String ENDUSER_NAME = "End User";
    private static final String WEBSERVER_NAME = "Virtual Host";
    private static final String APPSERVER_NAME = "Web Application";

    private String m_url;
    private int m_type;
    private String m_typeName;
    private int m_requests;

    public int getRequest() {
        return m_requests;
    }

    public int getType() {
        return m_type;
    }

    public String getTypeName() {
        return this.m_typeName;
    }

    public String getTypeString() {
        String result;

        switch (m_type) {
        case ENDUSER: {
            result = ENDUSER_NAME;
            break;
        }

        case WEBSERVER: {
            result = WEBSERVER_NAME;
            break;
        }

        case APPSERVER: {
            result = APPSERVER_NAME;
            break;
        }

        default: {
            result = "";
        }
        }

        return result;
    }

    public String getURL() {
        return m_url;
    }

    public void setRequest(int requests) {
        m_requests = requests;
    }

    public void setType(int type) {
        m_type = type;
    }

    public void setType(int type, String name) {
        this.setType(type);
        this.setTypeName(name);
    }

    public void setTypeName(String name) {
        m_typeName = name;
    }

    public void setURL(String url) {
        m_url = url;
    }

    public boolean isStacked() {
        return ((this.size() > 0) && (this.get(0) instanceof IStackedDataPoint));
    }
}