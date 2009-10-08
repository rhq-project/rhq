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
// -*- Mode: Java; indent-tabs-mode: nil; -*-
/*
 * ProblemMetricsDisplayForm.java
 *
 */

package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;

/**
 * Represents the seleted resources for problem metrics display
 */
public class ProblemMetricsDisplayForm extends MetricDisplayRangeForm {
    public static int TYPE_PROBLEMS = 1;
    public static int TYPE_ALL = 2;

    private String mode;
    private String[] child;
    private String[] host;
    private int showType;
    private boolean fresh;

    public ProblemMetricsDisplayForm() {
        super();
        setDefaults();
    }

    //-------------------------------------public methods

    public String[] getChild() {
        return child;
    }

    public void setChild(String[] s) {
        child = s;
    }

    /**
     * @param host The host to set.
     */
    public void setHost(String[] host) {
        this.host = host;
    }

    /**
     * @return Returns the host.
     */
    public String[] getHost() {
        return host;
    }

    /**
     * @return Returns the mode.
     */
    public String getMode() {
        return mode;
    }

    /**
     * @param mode The mode to set.
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getShowType() {
        return showType;
    }

    public void setShowType(int showType) {
        this.showType = showType;
    }

    public boolean getFresh() {
        return fresh;
    }

    public void setFresh(boolean fresh) {
        this.fresh = fresh;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        setDefaults();
        super.reset(mapping, request);

        // Make sure mode is valid
        mode = request.getParameter(Constants.MODE_PARAM);
        if ((mode == null) || (mode.length() == 0)) {
            mode = Constants.MODE_MON_CUR;
        }
    }

    protected void setDefaults() {
        super.setDefaults();
        child = new String[0];
        host = new String[0];
        mode = null;
        showType = TYPE_PROBLEMS;
        fresh = true;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        if (child != null) {
            s.append(" children=").append(Arrays.asList(child));
        }

        if (host != null) {
            s.append(" hosts=").append(Arrays.asList(host));
        }

        s.append(" mode=").append(mode);

        return s.toString();
    }
}