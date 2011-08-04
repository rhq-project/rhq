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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.application.FacesMessage;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.richfaces.model.UploadItem;

import org.jboss.seam.annotations.Create;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.enterprise.gui.common.upload.FileUploadUIBean;
import org.rhq.enterprise.gui.configuration.AbstractConfigurationUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

// This class is temporarily decleared in components.xml since it is declared under two names - ViewUIBean and
// UIBean. This class is undergoing some refactoring and part of that will include declaring the @Name and @Scope
// annotations again.
//
//@Name(value = "ExistingResourceConfigurationViewUIBean")
//@Scope(ScopeType.PAGE)
public class ExistingResourceConfigurationUIBean extends AbstractConfigurationUIBean {
    public static final String MANAGED_BEAN_NAME = "ExistingResourceConfigurationUIBean";

    private static final boolean STRUCTURED_MODE = true;
    private static final boolean RAW_MODE = false;

    private boolean mode = STRUCTURED_MODE;

    //    @Out
    private Collection<RawConfigDirectory> rawConfigDirectories;

    //    @Out
    private FileUploadUIBean fileUploader = new FileUploadUIBean();

    private String modalEditorContents;

    private int numberOfModifiedFiles;

    public ExistingResourceConfigurationUIBean() {
        removeSessionScopedBeanIfInView("/rhq/resource/configuration/view.xhtml",
            ExistingResourceConfigurationUIBean.class);
    }

    @Create
    public void begin() {
        if (isRawSupported() || isStructuredAndRawSupported()) {
            initConfigDirectories();
        } else {
            rawConfigDirectories = Collections.emptyList();
        }
    }

    private void initConfigDirectories() {
        Map<String, RawConfigDirectory> dirs = new HashMap<String, RawConfigDirectory>();

        for (RawConfiguration rawConfig : getConfiguration().getRawConfigurations()) {
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
    }

    private String getParentDir(RawConfiguration rawConfig) {
        File file = new File(rawConfig.getPath());
        return file.getParentFile().getAbsolutePath();
    }

    public String editConfiguration() {
        mode = STRUCTURED_MODE;
        return SUCCESS_OUTCOME;
    }

    public String editRawConfiguration() {
        mode = RAW_MODE;
        return SUCCESS_OUTCOME;
    }

    public void changeTabs(ValueChangeEvent event) {
        if (event.getNewValue().equals("rawTab")) {
            switchToRaw();
            mode = RAW_MODE;
        } else if (event.getNewValue().equals("structuredTab")) {
            switchToStructured();
            mode = STRUCTURED_MODE;
        }
    }

    public String switchToRaw() {
        Configuration configuration = LookupUtil.getConfigurationManager().translateResourceConfiguration(
            EnterpriseFacesContextUtility.getSubject(), getResourceId(), getConfiguration(), true);

        setConfiguration(configuration);

        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            getRaws().put(raw.getPath(), raw);
        }
        current = null;
        setConfiguration(configuration);
        updateModifiedCache();
        mode = RAW_MODE;
        return null;
    }

    private void updateModifiedCache() {
        Configuration configuration = getConfiguration();

        for (RawConfiguration updatedRaw : configuration.getRawConfigurations()) {
            RawConfiguration cachedRaw = modified.get(updatedRaw.getPath());
            if (cachedRaw != null && !updatedRaw.getSha256().equals(cachedRaw.getSha256())) {
                modified.remove(cachedRaw.getPath());
            }
        }
    }

    public String switchToStructured() {
        Configuration configuration = LookupUtil.getConfigurationManager().translateResourceConfiguration(
            EnterpriseFacesContextUtility.getSubject(), getResourceId(), getMergedConfiguration(), false);

        for (Property property : configuration.getAllProperties().values()) {
            property.setConfiguration(configuration);
        }

        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            getRaws().put(raw.getPath(), raw);
            setConfiguration(configuration);
        }
        current = null;
        setConfiguration(configuration);

        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            System.out.println(raw.getPath() + " -\n" + raw.getContents() + "\n");
        }

        updateModifiedCache();

        mode = STRUCTURED_MODE;

        return null;
    }

    public String updateConfiguration() {
        return updateConfiguration(true);

    }

    public String updateRawConfiguration() {
        return updateConfiguration(false);
    }

    public void saveModalEditorContents() {
        setCurrentContents(modalEditorContents);
    }

    public String updateConfiguration(boolean fromStructured) {
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();
        Configuration mergedConfiguration = getMergedConfiguration();

        AbstractResourceConfigurationUpdate updateRequest = this.configurationManager
            .updateStructuredOrRawConfiguration(EnterpriseFacesContextUtility.getSubject(), resourceId,
                mergedConfiguration, fromStructured);
        if (updateRequest != null) {
            switch (updateRequest.getStatus()) {
            case SUCCESS:
            case INPROGRESS:
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Configuration update request with id "
                    + updateRequest.getId() + " has been sent to the Agent.");
                clearConfiguration();
                return SUCCESS_OUTCOME;
            case FAILURE:
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Configuration update request with id "
                    + updateRequest.getId() + " failed.", updateRequest.getErrorMessage());

                if (null != mergedConfiguration) {
                    for (RawConfiguration raw : mergedConfiguration.getRawConfigurations()) {
                        String message = raw.errorMessage;
                        if (message != null) {
                            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, raw.getPath(), message);
                        }
                    }
                }

                return FAILURE_OUTCOME;
            case NOCHANGE:
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN,
                    "No changes were made to the configuration, so " + "no update request has been sent to the Agent.");
                clearConfiguration();
                return SUCCESS_OUTCOME;

            }
        }
        return FAILURE_OUTCOME;
    }

    public String finishAddMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map added.");
        return SUCCESS_OUTCOME;
    }

    public String finishEditMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map updated.");
        return SUCCESS_OUTCOME;
    }

    protected int getConfigurationDefinitionKey() {
        return EnterpriseFacesContextUtility.getResource().getResourceType().getId();
    }

    @Nullable
    protected ConfigurationDefinition lookupConfigurationDefinition() {
        int resourceTypeId = EnterpriseFacesContextUtility.getResource().getResourceType().getId();
        ConfigurationDefinition configurationDefinition = this.configurationManager
            .getResourceConfigurationDefinitionForResourceType(EnterpriseFacesContextUtility.getSubject(),
                resourceTypeId);
        return configurationDefinition;
    }

    protected int getConfigurationKey() {
        return EnterpriseFacesContextUtility.getResource().getId();
    }

    protected ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    @Nullable
    protected Configuration lookupConfiguration() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();
        AbstractResourceConfigurationUpdate configurationUpdate = this.configurationManager
            .getLatestResourceConfigurationUpdate(subject, resourceId);
        Configuration configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;

        return configuration;
    }

    private final Log log = LogFactory.getLog(AbstractConfigurationUIBean.class);
    private Integer resourceId = null;

    private static final long serialVersionUID = 4837157548556168146L;

    public String commit() {

        Subject subject = EnterpriseFacesContextUtility.getSubject();
        LookupUtil.getConfigurationManager().updateResourceConfiguration(subject, getResourceId(),
            getMergedConfiguration());

        return "/rhq/resource/configuration/view.xhtml?id=" + getResourceId();
    }

    private Configuration getMergedConfiguration() {
        for (RawConfiguration raw : getModified().values()) {
            getRaws().put(raw.getPath(), raw);
            log.error("Just merged in raw path =[" + raw.getPath() + "]");
            log.error("                   file =[" + new String(raw.getContents()) + "]");

        }
        getConfiguration().getRawConfigurations().clear();
        getConfiguration().getRawConfigurations().addAll(getRaws().values());
        return getConfiguration();
    }

    public String editCurrent() {
        RawConfiguration editedRaw = getCurrent();
        RawConfiguration originalRaw = raws.get(editedRaw.getPath());

        if (editedRaw.getSha256().equals(originalRaw.getSha256())) {
            --numberOfModifiedFiles;
            RawConfigUIBean rawUIBean = findRawConfigUIBeanByPath(editedRaw.getPath());
            //            rawUIBean.setModified(false);
            getModified().remove(editedRaw.getPath());
        } else {
            ++numberOfModifiedFiles;
        }
        return null;
    }

    public void undoEdit(String path) {
        modified.remove(path);
        RawConfigUIBean bean = findRawConfigUIBeanByPath(path);

        if (bean != null) {
            //            bean.setModified(false);
            //            bean.setIcon("/images/blank.png");
        }

    }

    public int getConfigId() {
        return getConfiguration().getId();
    }

    private ConfigurationFormat getConfigurationFormat() {
        return getConfigurationDefinition().getConfigurationFormat();
    }

    public RawConfiguration getCurrent() {
        if (null == current) {
            Iterator<RawConfiguration> iterator = getRaws().values().iterator();
            if (iterator.hasNext()) {
                current = iterator.next();
            } else {
                current = new RawConfiguration();
                current.setPath("/dev/null");
                String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString("");
                current.setContents("", sha256);
            }
        }
        return current;
    }

    public String getCurrentContents() {
        return new String(getCurrent().getContents());
    }

    public String getCurrentPath() {
        return getCurrent().getPath();
    }

    public String getModalEditorHeader() {
        return getCurrentPath();
    }

    public String getModalEditorContents() {
        if (modalEditorContents == null) {
            modalEditorContents = getCurrentContents();
        }
        return modalEditorContents;
    }

    public void setModalEditorContents(String contents) {
        modalEditorContents = contents;
    }

    public TreeMap<String, RawConfiguration> getModified() {
        if (modified == null) {
            modified = new TreeMap<String, RawConfiguration>();
        }
        return modified;
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
        } else {
            mode = RAW_MODE;
        }
    }

    public Object[] getPaths() {

        return getRaws().keySet().toArray();
    }

    /**
    *      
    * @return the id associated with the resource.  
    * The value Cached in order to be available on the completeUpload page,
    * where seeing the resource id conflicts with the rich completeUpload tag.
    */
    public int getResourceId() {
        if (resourceId == null) {
            resourceId = EnterpriseFacesContextUtility.getResource().getId();
        }
        return resourceId;
    }

    public boolean isStructuredMode() {
        return mode == STRUCTURED_MODE;
    }

    public boolean isRawMode() {
        return mode == RAW_MODE;
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
        if (!isDisplayChangedFilesLabel() || numberOfModifiedFiles == 0) {
            return "";
        }

        if (numberOfModifiedFiles == 1) {
            return "1 file changed in this configuration";
        }

        return numberOfModifiedFiles + " files changed in this configuration";
    }

    public boolean isModified(String path) {
        return getModified().keySet().contains(path);
    }

    public boolean isRawSupported() {
        return getConfigurationDefinition().getConfigurationFormat() == ConfigurationFormat.RAW;
    }

    public boolean isStructuredSupported() {
        return getConfigurationDefinition().getConfigurationFormat() == ConfigurationFormat.STRUCTURED;
    }

    public boolean isStructuredAndRawSupported() {
        return getConfigurationDefinition().getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW;
    }

    public boolean isFileUploadAvailable() {
        return isRawSupported() || (isStructuredAndRawSupported() && isRawMode());
    }

    /**
     * Indicates which of the raw configuration files is currently selected.
     * @param s
     */
    public void select(String s) {
        selectedPath = s;
        setCurrentPath(selectedPath);
        setCurrentContents(getCurrentContents());
        modalEditorContents = getCurrentContents();
    }

    public void setCurrentContents(String updated) {
        String original = new String(getCurrent().getContents());
        if (!updated.equals(original)) {
            current = current.deepCopy(false);
            String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(updated);
            current.setContents(updated, sha256);

            markCurrentRawConfigUIBeanModified();
            getModified().put(current.getPath(), current);
        }
    }

    private void markCurrentRawConfigUIBeanModified() {
        RawConfigUIBean bean = findRawConfigUIBeanByPath(current.getPath());
        if (bean != null) {
            //            bean.setModified(true);
        }
    }

    private RawConfigUIBean findRawConfigUIBeanByPath(String path) {
        for (RawConfigDirectory dir : rawConfigDirectories) {
            for (RawConfigUIBean bean : dir.getRawConfigUIBeans()) {
                if (bean.getPath().equals(current.getPath())) {
                    return bean;
                }
            }
        }
        return null;
    }

    public void setCurrentPath(String path) {
        RawConfiguration raw = getModified().get(path);
        if (null == raw) {
            raw = getRaws().get(path);
        }
        if (null != raw) {
            current = raw;
        }
    }

    public void setModified(RawConfiguration raw) {
        getModified().put(raw.getPath(), raw);
    }

    public String download() {
        try {
            File file = new File(getCurrentPath());

            HttpServletResponse response = FacesContextUtility.getResponse();
            response.setHeader("Content-Disposition", "attachment;filename=" + file.getName());
            OutputStream ostream = response.getOutputStream();
            ostream.write(getCurrentContents().getBytes());
            ostream.flush();
            ostream.close();

            FacesContextUtility.getFacesContext().responseComplete();

            return null;
        } catch (IOException e) {
            log.error("Failed to complete download request for " + getCurrentPath(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This is a no-op, since the completeUpload work was done by completeUpload file
     * But is kept as a target for the "save" icon from the full screen page
     */
    public String update() {
        return "/rhq/resource/configuration/edit-raw.xhtml?currentResourceId=" + getResourceId();
    }

    public String completeUpload() {
        try {
            if (fileUploader.getFileItem() != null) {
                UploadItem fileItem = fileUploader.getFileItem();
                if (fileItem.isTempFile()) {
                    setCurrentContents(FileUtils.readFileToString(fileItem.getFile()));
                } else {
                    setCurrentContents(new String(fileItem.getData()));
                }
                fileUploader.clear();
            }

            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void dumpProperties(Configuration conf, Log log) {
        for (String key : conf.getAllProperties().keySet()) {
            log.error("property=" + conf.getAllProperties().get(key));
        }
    }

    void populateRaws() {
        Collection<RawConfiguration> rawConfigurations = getConfiguration().getRawConfigurations();

        for (RawConfiguration raw : rawConfigurations) {
            raws.put(raw.getPath(), raw);
        }
    }

    public void setRaws(TreeMap<String, RawConfiguration> raws) {
        this.raws = raws;
    }

    public TreeMap<String, RawConfiguration> getRaws() {

        if (null == raws) {
            raws = new TreeMap<String, RawConfiguration>();
            populateRaws();
        }

        return raws;
    }

    String selectedPath;

    private TreeMap<String, RawConfiguration> raws;

    TreeMap<String, RawConfiguration> modified = new TreeMap<String, RawConfiguration>();
    RawConfiguration current = null;

}
