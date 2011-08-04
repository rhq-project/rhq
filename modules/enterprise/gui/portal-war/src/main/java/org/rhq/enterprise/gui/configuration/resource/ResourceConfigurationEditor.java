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

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Name("configurationEditor")
@Scope(ScopeType.CONVERSATION)
public class ResourceConfigurationEditor extends ResourceConfigurationViewer implements Serializable {

    private Configuration originalResourceConfiguration;

    private Set<String> modifiedFiles = new HashSet<String>();

    private String modalEditorContents;

    @RequestParameter
    private String tab;

    @Override
    protected void doInitialization() {
        if (tab != null) {
            setSelectedTab(tab);
        }

        originalResourceConfiguration = resourceConfiguration.deepCopy(true);
    }

    @Override
    protected void changeToRawTab() {
        resourceConfiguration = translateToRaw();
        for (RawConfiguration raw : resourceConfiguration.getRawConfigurations()) {
            RawConfigUIBean uiBean = findRawConfigUIBeanByPath(raw.getPath());
            uiBean.setRawConfiguration(raw);
        }
    }

    private Configuration translateToRaw() {
        ConfigurationManagerLocal configurationMgr = LookupUtil.getConfigurationManager();
        return configurationMgr.translateResourceConfiguration(loggedInUser.getSubject(), resourceId,
            resourceConfiguration, STRUCTURED_MODE);
    }

    @Override
    protected void changeToStructuredTab() {
        resourceConfiguration = translateToStructured();
        for (RawConfiguration raw : resourceConfiguration.getRawConfigurations()) {
            RawConfigUIBean uiBean = findRawConfigUIBeanByPath(raw.getPath());
            uiBean.setRawConfiguration(raw);
        }
    }

    private Configuration translateToStructured() {
        ConfigurationManagerLocal configurationMgr = LookupUtil.getConfigurationManager();
        return configurationMgr.translateResourceConfiguration(loggedInUser.getSubject(), resourceId,
            resourceConfiguration, RAW_MODE);
    }

    @Observer("rawConfigUpdate")
    public void rawConfigUpdated(RawConfigUIBean rawConfigUIBean) {
        if (rawConfigUIBean.isModified()) {
            // If the file is modified and not already in the cache, then the file was previously in an unmodified state
            // so we want to increment the number of files modified and put the file in the cache to track its current
            // state.
            if (!modifiedFiles.contains(rawConfigUIBean.getPath())) {
                modifiedFiles.add(rawConfigUIBean.getPath());
            }
            // There is kind of an implicit else block to do nothing. If the file is modified and already in the cache,
            // then that means we have already incremented the number of files modified, so we do not need to
            // increment again.
        } else if (modifiedFiles.contains(rawConfigUIBean.getPath())) {
            // We fall into this block if the file is not modified and if the cache contains the file, which means it
            // was previously in a modified state; therefore, we remove it from the cache, and decrement the number of
            // files modified.
            modifiedFiles.remove(rawConfigUIBean.getPath());
        }
    }

    public boolean isDisplayChangedFilesLabel() {
        if (isStructuredMode()) {
            return false;
        }

        if (isRawSupported()) {
            return true;
        }

        return isRawMode();
    }

    public String getModifiedFilesMsg() {
        if (!isDisplayChangedFilesLabel() || modifiedFiles.size() == 0) {
            return "";
        }

        if (modifiedFiles.size() == 1) {
            return "1 file changed in this configuration";
        }

        return modifiedFiles.size() + " files changed in this configuration";
    }

    public boolean getRenderFileUpload() {
        return isRawSupported() || isStructuredAndRawSupported();
    }

    public String updateConfiguration() {
        ConfigurationManagerLocal configurationMgr = LookupUtil.getConfigurationManager();

        try {
            AbstractResourceConfigurationUpdate updateRequest;

            if (isStructuredAndRawSupported()) {
                updateRequest = configurationMgr.updateStructuredOrRawConfiguration(loggedInUser.getSubject(),
                    resourceId, resourceConfiguration, isStructuredMode());                
            }
            else {
                updateRequest = configurationMgr.updateResourceConfiguration(loggedInUser.getSubject(),
                    resourceId, resourceConfiguration);
            }

            clearErrors();

            if (updateRequest != null) {
                switch (updateRequest.getStatus()) {
                case SUCCESS:
                case INPROGRESS:
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Configuration update request with id "
                        + updateRequest.getId() + " has been sent to the Agent.");
                    return "success";
                case FAILURE:
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Configuration update request with id "
                        + updateRequest.getId() + " failed.", updateRequest.getErrorMessage());
//                    for (RawConfiguration raw : resourceConfiguration.getRawConfigurations()) {
//                        String message = raw.errorMessage;
//                        if (message != null) {
//                            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, raw.getPath(), message);
//                        }
//                    }
                    copyErrorMessages(updateRequest);
                    return "failure";

                case NOCHANGE:
                    addNoChangeMsgToFacesContext();
                    return "nochange";
                }
            }
        } catch (EJBException e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Unable to contact the remote agent.", e
                .getCause());
            return "failure";
        }

        // updateRequest will be null if there is no change to the configuration. ConfigurationManagerBean checks to
        // see if the configuration has been modified before sending the request to the agent. If no change is detected,
        // it simply returns null.

        addNoChangeMsgToFacesContext();

        return "nochange";
    }

    private void clearErrors() {
        for (RawConfigDirectory dir : rawConfigDirectories) {
            for (RawConfigUIBean bean : dir.getRawConfigUIBeans()) {
                bean.setErrorMessage(null);
            }
        }
    }

    private void copyErrorMessages(AbstractResourceConfigurationUpdate update) {
        Configuration updatedConfiguration = update.getConfiguration();
        for (RawConfiguration updatedRaw : updatedConfiguration.getRawConfigurations()) {
            RawConfigUIBean rawUIBean = findRawConfigUIBeanByPath(updatedRaw.getPath());
            if (rawUIBean != null) {
                rawUIBean.setErrorMessage(updatedRaw.errorMessage);
            }

//            RawConfiguration raw = findRawConfigurationByPath(updatedRaw.getPath());
//            if (raw != null) {
//                raw.errorMessage = updatedRaw.errorMessage;
//            }
        }
    }

    private RawConfiguration findRawConfigurationByPath(String path) {
        for (RawConfiguration raw : resourceConfiguration.getRawConfigurations()) {
            if (raw.getPath().equals(path)) {
                return raw;
            }
        }

        return null;
    }

    private void addNoChangeMsgToFacesContext() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN, "No changes were made to the configuration, so "
            + "no update request has been sent to the Agent.");
    }

    public void undoEdit(String path) {
        RawConfigUIBean rawConfigUIBean = findRawConfigUIBeanByPath(path);
        rawConfigUIBean.undoEdit();
    }

    /**
     * 
     * @return
     */
    public String finishAddMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map added.");
        return "success";
    }

    public String finishEditMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map updated.");
        return "success";
    }

    public void setModalEditorContents(String contents) {
        modalEditorContents = contents;
    }

    public void applyModalEditContents() {
        selectedRawUIBean.setContents(modalEditorContents);
    }
}
