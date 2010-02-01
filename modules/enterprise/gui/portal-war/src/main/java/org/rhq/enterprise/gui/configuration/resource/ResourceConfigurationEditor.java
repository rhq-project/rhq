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

package org.rhq.enterprise.gui.configuration.resource;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@Name("configurationEditor")
@Scope(ScopeType.CONVERSATION)
@Role(name = "configurationViewer", scope = ScopeType.PAGE)
public class ResourceConfigurationEditor {

    private static final boolean STRUCTURED_MODE = true;
    private static final boolean RAW_MODE = false;

    private boolean mode = STRUCTURED_MODE;

    private Configuration originalResourceConfiguration;

    @In("webUser")
    private WebUser loggedInUser;

    @Out
    private Configuration resourceConfiguration;

    @Out
    private ConfigurationDefinition resourceConfigurationDefinition;

    @Out
    private Collection<RawConfigDirectory> rawConfigDirectories;

    @Out(required = false)
    private RawConfigUIBean selectedRawUIBean;

    @RequestParameter("id")
    private int resourceId;

    @RequestParameter
    private String tab;

    private boolean readOnly = true;

    @Create
    public void initialize() {
        loadResourceConfigurationDefinition();
        loadResourceConfiguration();
        initRawConfigDirectories();

        if (tab != null) {
            setSelectedTab(tab);
        }
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

        AbstractResourceConfigurationUpdate configurationUpdate =
                configurationManager.getLatestResourceConfigurationUpdate(subject, resourceId);
        resourceConfiguration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;

        if (resourceConfiguration != null) {
            ConfigurationMaskingUtility.maskConfiguration(resourceConfiguration, resourceConfigurationDefinition);
        }

    }

    private void initRawConfigDirectories() {
        if (isRawSupported() || isStructuredAndRawSupported()) {
            initConfigDirectories();
        }
        else {
            rawConfigDirectories = Collections.EMPTY_LIST;
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

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isRawSupported() {
        return resourceConfigurationDefinition.getConfigurationFormat() == ConfigurationFormat.RAW;
    }

    public boolean isStructuredSupported() {
        return resourceConfigurationDefinition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED;
    }

    public boolean isStructuredAndRawSupported() {
        return resourceConfigurationDefinition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW;
    }

    public String getSelectedTab() {
        if (mode == STRUCTURED_MODE) {
            return "structuredTab";
        }
        return "rawTab";
    }

    public void setSelectedTab(String tab) {
        if (tab.equals("structuredTab")) {
            mode = STRUCTURED_MODE;
        }
        else {
            mode = RAW_MODE;
        }
    }

    public void changeTabs(ValueChangeEvent event) {
        if (event.getNewValue().equals("rawTab")) {
//            switchToRaw();
            mode = RAW_MODE;
        }
        else if (event.getNewValue().equals("structuredTab")) {
//            switchToStructured();
            mode = STRUCTURED_MODE;
        }
    }

    public void edit() {
        originalResourceConfiguration = resourceConfiguration.deepCopy(true);
    }

    public void select(String path) {
        selectedRawUIBean = findRawConfigUIBeanByPath(path);
    }

    private RawConfigUIBean findRawConfigUIBeanByPath(String path) {
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
        }
        catch (IOException e) {
//            log.error("Failed to complete download request for " + getCurrentPath(), e);
            throw new RuntimeException(e);
        }
    }

}
