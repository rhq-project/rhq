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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.GenericValidator;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.rhq.enterprise.gui.legacy.action.resource.ResourceForm;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * Form for editing the control action for an alert definition.
 */
public final class SyslogActionForm extends ResourceForm {
    private Log log = LogFactory.getLog(SyslogActionForm.class.getName());

    //-------------------------------------instance variables

    // control action properties
    private Integer id; // nullable
    private Integer ad; // nullable
    private String metaProject;
    private String project;
    private String version;
    private boolean shouldBeRemoved;

    //-------------------------------------constructors

    public SyslogActionForm() {
        // do nothing
    }

    //-------------------------------------public methods

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAd() {
        return this.ad;
    }

    public void setAd(Integer ad) {
        this.ad = ad;
    }

    public String getMetaProject() {
        return this.metaProject;
    }

    public void setMetaProject(String metaProject) {
        this.metaProject = metaProject;
    }

    public String getProject() {
        return this.project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean getShouldBeRemoved() {
        return this.shouldBeRemoved;
    }

    public void setShouldBeRemoved(boolean shouldBeRemoved) {
        this.shouldBeRemoved = shouldBeRemoved;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        metaProject = null;
        project = null;
        version = null;
        shouldBeRemoved = false;
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        // don't validate if we are preparing the form ...
        if (shouldValidate(mapping, request)) {
            ActionErrors errs = super.validate(mapping, request);
            if (null == errs) {
                errs = new ActionErrors();
            }

            // if we are not removing the action, all the fields are
            // required
            if (!getShouldBeRemoved()) {
                if (GenericValidator.isBlankOrNull(getMetaProject())) {
                    String fieldName = RequestUtils.message(request, "alert.config.props.Syslog.MetaProject");
                    ActionMessage err = new ActionMessage("errors.required", fieldName);
                    errs.add("metaProject", err);
                }

                if (GenericValidator.isBlankOrNull(getProject())) {
                    String fieldName = RequestUtils.message(request, "alert.config.props.Syslog.Project");
                    ActionMessage err = new ActionMessage("errors.required", fieldName);
                    errs.add("project", err);
                }

                if (GenericValidator.isBlankOrNull(getVersion())) {
                    String fieldName = RequestUtils.message(request, "alert.config.props.Syslog.Version");
                    ActionMessage err = new ActionMessage("errors.required", fieldName);
                    errs.add("version", err);
                }
            }

            return errs;
        } else {
            return null;
        }
    }
}

// EOF
