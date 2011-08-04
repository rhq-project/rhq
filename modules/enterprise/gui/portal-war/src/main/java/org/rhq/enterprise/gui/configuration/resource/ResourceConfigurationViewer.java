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

package org.rhq.enterprise.gui.configuration.resource;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.application.FacesMessage;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

@Name("configurationViewer")
@Scope(ScopeType.PAGE)
public class ResourceConfigurationViewer {

    public static final boolean STRUCTURED_MODE = true;
    public static final boolean RAW_MODE = false;

    private boolean mode = STRUCTURED_MODE;

    @In("webUser")
    protected WebUser loggedInUser;

    @Out(required = false)
    protected Configuration resourceConfiguration;

    @Out(required = false)
    protected ConfigurationDefinition resourceConfigurationDefinition;

    @Out(required = false)
    protected Collection<RawConfigDirectory> rawConfigDirectories;

    @Out(required = false)
    protected RawConfigUIBean selectedRawUIBean;

    protected Integer resourceId;

    protected boolean initialized = false;

    /**
     * <p>
     * This method "bootstraps" the viewer/editor and the model objects used in the view. Specifically, the resource
     * configuration and the corresponding configuration definition are loaded from the database and are outjected for
     * use in the view.
     * </p>
     * <p>
     * If the configuration definition or if the configuration fail to load for whatever reason, the initialized flag
     * is set to <code>false</code>. When the configuration definition is null a message is added to the Faces context
     * indicating that the resource does not expose a configuration. When the configuration is null, a message is added
     * to the Faces context indicating that the resource configuration has not been initialized.
     * </p>
     */
    public void initialize() {
        try {
            resourceId = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);

            loadResourceConfigurationDefinition();

            if (resourceConfigurationDefinition == null) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN,
                    "This resource does not expose a configuration.");
                return;
            }

            loadResourceConfiguration();

            if (resourceConfiguration == null) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN,
                    "This resource's configuration has not yet been initialized.");
                return;
            }

            initRawConfigDirectories();
            initMode();

            initialized = true;

            doInitialization();
        } catch (Throwable t) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, t.getMessage());
        }
    }

    protected void doInitialization() {
    }

    private void loadResourceConfigurationDefinition() {
        int resourceTypeId = EnterpriseFacesContextUtility.getResource().getResourceType().getId();

        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        resourceConfigurationDefinition = configurationManager.getResourceConfigurationDefinitionForResourceType(
            loggedInUser.getSubject(), resourceTypeId);
    }

    private void loadResourceConfiguration() {
        Subject subject = loggedInUser.getSubject();

        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        AbstractResourceConfigurationUpdate configurationUpdate = configurationManager
            .getLatestResourceConfigurationUpdate(subject, resourceId);
        resourceConfiguration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;
    }

    protected void initRawConfigDirectories() {
        if (isRawSupported() || isStructuredAndRawSupported()) {
            initConfigDirectories();
        } else {
            rawConfigDirectories = Collections.emptyList();
        }
    }

    private void initConfigDirectories() {
        Map<String, RawConfigDirectory> dirs = new TreeMap<String, RawConfigDirectory>();

        for (RawConfiguration rawConfig : resourceConfiguration.getRawConfigurations()) {
            String parentDirPath = getParentDir(rawConfig);
            RawConfigDirectory dir = dirs.get(parentDirPath);

            if (dir == null) {
                dir = new RawConfigDirectory();
                dir.setPath(parentDirPath);
            }

            dir.addRawConfig(rawConfig);
            dirs.put(parentDirPath, dir);
        }

        rawConfigDirectories = dirs.values();

        RawConfigDirectory selectedDir = null;
        for (RawConfigDirectory dir : rawConfigDirectories) {
            selectedDir = dir;
            break;
        }
        selectedRawUIBean = selectedDir.getRawConfigUIBeans().get(0);
    }

    private String getParentDir(RawConfiguration rawConfig) {
        File file = new File(rawConfig.getPath());
        return file.getParentFile().getAbsolutePath();
    }

    private void initMode() {
        if (isStructuredSupported()) {
            mode = STRUCTURED_MODE;
        } else if (isRawMode()) {
            mode = RAW_MODE;
        } else { // else structured and raw is supported and (at least for now) we will just start
            mode = STRUCTURED_MODE; // the user off in structured mode. We may at some later point want to add logic
            // to remember what mode the user should start in.
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** @return <code>true</code> if the resource configuration supports raw only, <code>false</code> otherwise */
    public boolean isRawSupported() {
        return resourceConfigurationDefinition.getConfigurationFormat() == ConfigurationFormat.RAW;
    }

    /** @return <code>true</code> if the resource configuration supports structured only, <code>false</code> otherwise */
    public boolean isStructuredSupported() {
        return resourceConfigurationDefinition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED;
    }

    public boolean isStructuredMode() {
        return mode == STRUCTURED_MODE;
    }

    public boolean isRawMode() {
        return mode == RAW_MODE;
    }

    public boolean getRenderModalEditor() {
        // Ideally we want to also check that isRawMode() returns true, but doing so causes the wrong results due to
        // how/when the component tree is rendered. The mode field is changed when the user changes tabs in the UI;
        // however this property is read and its value applied to the modalEditor component before the mode is updated.
        //
        // jsanda - 02/10/2010

        return isRawSupported() || isStructuredAndRawSupported();
    }

    /**
     * @return <code>true</code> if the resource configuration supports structured and raw, <code>false</code> otherwise
     */
    public boolean isStructuredAndRawSupported() {
        return resourceConfigurationDefinition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW;
    }

    /** @return The currently selected tab, structured (i.e., Basic Mode) or raw (i.e., Advanced Mode). */
    public String getSelectedTab() {
        if (isStructuredSupported()) {
            return "structuredTab";
        }

        if (isRawSupported()) {
            return "rawTab";
        }

        if (mode == STRUCTURED_MODE) {
            return "structuredTab";
        }
        return "rawTab";
    }

    public void setSelectedTab(String tab) {
        if (tab.equals("structuredTab")) {
            mode = STRUCTURED_MODE;
        } else {
            mode = RAW_MODE;
        }
    }

    public void changeTabs(ValueChangeEvent event) {
        if (event.getNewValue().equals("rawTab")) {
            mode = RAW_MODE;
            changeToRawTab();
        } else if (event.getNewValue().equals("structuredTab")) {
            mode = STRUCTURED_MODE;
            changeToStructuredTab();
        }
    }

    protected void changeToRawTab() {
    }

    protected void changeToStructuredTab() {
    }

    public void select(String path) {
        selectedRawUIBean = findRawConfigUIBeanByPath(path);
    }

    protected RawConfigUIBean findRawConfigUIBeanByPath(String path) {
        for (RawConfigDirectory dir : rawConfigDirectories) {
            for (RawConfigUIBean bean : dir.getRawConfigUIBeans()) {
                if (bean.getPath().equals(path)) {
                    return bean;
                }
            }
        }
        return null;
    }

    public boolean isUpdateInProgress() {
        if (!isInitialized()) {
            return false;
        }

        ConfigurationManagerLocal configurationMgr = LookupUtil.getConfigurationManager();
        return configurationMgr.isResourceConfigurationUpdateInProgress(loggedInUser.getSubject(), resourceId);
    }

    public String download() {
        try {
            File file = new File(selectedRawUIBean.getFileName());

            HttpServletResponse response = FacesContextUtility.getResponse();
            response.setHeader("Content-Disposition", "attachment;filename=" + file.getName());
            OutputStream ostream = response.getOutputStream();
            ostream.write(selectedRawUIBean.getContents().getBytes());
            ostream.flush();
            ostream.close();

            FacesContextUtility.getFacesContext().responseComplete();

            return null;
        } catch (IOException e) {
            //            log.error("Failed to complete download request for " + getCurrentPath(), e);
            throw new RuntimeException(e);
        }
    }

    public String getModalEditorHeader() {
        return selectedRawUIBean.getPath();
    }

    public String getModalEditorContents() {
        return selectedRawUIBean.getContents();
    }

}
