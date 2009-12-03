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
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.richfaces.event.UploadEvent;

import org.jboss.seam.annotations.Create;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.configuration.AbstractConfigurationUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
//@ Name(value = "ExistingResourceConfigurationUIBean")
//@ Scope(ScopeType.PAGE)
public class ExistingResourceConfigurationUIBean extends AbstractConfigurationUIBean {
    public static final String MANAGED_BEAN_NAME = "ExistingResourceConfigurationUIBean";

    // =========== actions ===========

    public ExistingResourceConfigurationUIBean() {
        removeSessionScopedBeanIfInView("/rhq/resource/configuration/view.xhtml",
            ExistingResourceConfigurationUIBean.class);
    }

    public String editConfiguration() {
        return SUCCESS_OUTCOME;
    }

    public String editRawConfiguration() {
        return SUCCESS_OUTCOME;
    }

    public String switchToRaw() {
        ConfigurationMaskingUtility.unmaskConfiguration(getConfiguration(), getConfigurationDefinition());
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();
        this.configurationManager.translateResourceConfiguration(EnterpriseFacesContextUtility.getSubject(),
            resourceId, getConfiguration(), true);

        return SUCCESS_OUTCOME;
    }

    public String updateConfiguration() {
        return updateConfiguration(true);

    }

    public String updateRawConfiguration() {
        return updateConfiguration(false);
    }

    public String updateConfiguration(boolean fromStructured) {

        ConfigurationMaskingUtility.unmaskConfiguration(getConfiguration(), getConfigurationDefinition());
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();

        //TODO this is two round trips to the agent.  need to blend these into one.
        Configuration configuration = LookupUtil.getConfigurationManager().translateResourceConfiguration(
            EnterpriseFacesContextUtility.getSubject(), getResourceId(), getMergedConfiguration(), false);

        AbstractResourceConfigurationUpdate updateRequest = this.configurationManager.updateResourceConfiguration(
            EnterpriseFacesContextUtility.getSubject(), resourceId, configuration, fromStructured);
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
                return FAILURE_OUTCOME;
            }
        } else {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN, "No changes were made to the configuration, so "
                + "no update request has been sent to the Agent.");
            clearConfiguration();
            return SUCCESS_OUTCOME;
        }
        return null;
    }

    public String finishAddMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map added.");
        return SUCCESS_OUTCOME;
    }

    public String finishEditMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map updated.");
        return SUCCESS_OUTCOME;
    }

    // =========== impls of superclass abstract methods ===========

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
        if (configuration != null) {
            ConfigurationMaskingUtility.maskConfiguration(configuration, getConfigurationDefinition());
        }

        return configuration;
    }

    private final Log log = LogFactory.getLog(AbstractConfigurationUIBean.class);
    private Integer resourceId = null;

    private static final long serialVersionUID = 4837157548556168146L;

    public String commit() {

        Subject subject = EnterpriseFacesContextUtility.getSubject();
        LookupUtil.getConfigurationManager().updateResourceConfiguration(subject, getResourceId(),
            getMergedConfiguration());

        nullify();

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

    public String discard() {
        nullify();
        return "/rhq/resource/configuration/view.xhtml?id=" + getResourceId();
    }

    public void fileUploadListener(UploadEvent event) throws Exception {
        File uploadFile;
        log.error("fileUploadListener called");
        uploadFile = event.getUploadItem().getFile();
        if (uploadFile != null) {
            log.debug("fileUploadListener got file named " + event.getUploadItem().getFileName());
            log.debug("content type is " + event.getUploadItem().getContentType());
            if (uploadFile != null) {
                try {
                    FileReader fileReader = new FileReader(uploadFile);
                    char[] buff = new char[1024];
                    StringBuffer stringBuffer = new StringBuffer();
                    for (int count = fileReader.read(buff); count != -1; count = fileReader.read(buff)) {
                        stringBuffer.append(buff, 0, count);
                    }
                    setCurrentContents(stringBuffer.toString());
                } catch (IOException e) {
                    log.error("problem reading uploaded file", e);
                }
            }
        }
    }

    public int getConfigId() {
        return getConfiguration().getId();
    }

    /*
        public Configuration getConfiguration() {
            if (null == configuration) {

                Subject subject = EnterpriseFacesContextUtility.getSubject();
                int resourceId = EnterpriseFacesContextUtility.getResource().getId();
                AbstractResourceConfigurationUpdate configurationUpdate = LookupUtil.getConfigurationManager()
                    .getLatestResourceConfigurationUpdate(subject, resourceId);
                Configuration configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;
                if (configuration != null) {
                    //ConfigurationMaskingUtility.maskConfiguration(configuration, getConfigurationDefinition());
                }

                return configuration;

            }
            return configuration;
        
        }
    */

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
                current.setContents("".getBytes());
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

    public TreeMap<String, RawConfiguration> getModified() {
        if (modified == null) {
            modified = new TreeMap<String, RawConfiguration>();
        }
        return modified;
    }

    public Object[] getPaths() {
        return getRaws().keySet().toArray();
    }

    /**
    *      
    * @return the id associated with the resource.  
    * The value Cached in order to be available on the upload page, 
    * where seeing the resource id conflicts with the rich upload tag.
    */
    public int getResourceId() {
        if (resourceId == null) {
            resourceId = EnterpriseFacesContextUtility.getResource().getId();
        }
        return resourceId;
    }

    /**
     * Hack Alert.  This bean needs to be initialized on one of the pages that has id or resourceId set
     * In order to capture the resource.  It will then Keep track of that particular resources until
     * commit is called.  Ideally, this should be a conversation scoped bean, but that has other issues
     * 
     */
    @Create
    public void init() {

    }

    public boolean isModified(String path) {
        return getModified().keySet().contains(path);
    }

    public boolean isRawSupported() {
        return getConfigurationFormat().isRawSupported();
    }

    public boolean isStructuredSupported() {
        return getConfigurationFormat().isStructuredSupported();
    }

    void nullify() {

    }

    /**
     * Indicates which of the raw configuration files is currently selected.
     * @param s
     */
    public void select(String s) {
        selectedPath = s;
        setCurrentPath(selectedPath);
    }

    public void setCurrentContents(String updated) {

        String original = new String(getCurrent().getContents());
        if (!updated.equals(original)) {
            current = current.deepCopy(false);
            current.setContents(updated.getBytes());
            //TODO update other values like MD5
            getModified().put(current.getPath(), current);
        }
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

    /**
     * This is a no-op, since the upload work was done by upload file
     * But is kept as a target for the "save" icon from the full screen page
     */
    public String update() {
        return "/rhq/resource/configuration/edit-raw.xhtml?currentResourceId=" + getResourceId();
    }

    /**
     * This is a no-op, since the upload work was done by upload file
     * But is kept as a target for the "action" value 
     */
    public String upload() {
        return "/rhq/resource/configuration/edit-raw.xhtml?currentResourceId=" + getResourceId();
    }

    public String switchToraw() {
        log.error("switch2raw called");
        dumpProperties(getConfiguration(), log);
        Configuration configuration = LookupUtil.getConfigurationManager().translateResourceConfiguration(
            EnterpriseFacesContextUtility.getSubject(), getResourceId(), getMergedConfiguration(), true);
        log.error("switch2raw post merge");
        dumpProperties(getConfiguration(), log);

        setConfiguration(configuration);
        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            getRaws().put(raw.getPath(), raw);
        }
        current = null;
        setConfiguration(configuration);

        return "/rhq/resource/configuration/edit-raw.xhtml?currentResourceId=" + getResourceId();
    }

    void dumpProperties(Configuration conf, Log log) {
        for (String key : conf.getAllProperties().keySet()) {
            log.error("property=" + conf.getAllProperties().get(key));
        }
    }

    public String switchTostructured() {
        log.error("switch2structured called");

        dumpProperties(getConfiguration(), log);
        Configuration configuration = LookupUtil.getConfigurationManager().translateResourceConfiguration(
            EnterpriseFacesContextUtility.getSubject(), getResourceId(), getMergedConfiguration(), false);
        log.error("switch2structured post merge");

        dumpProperties(configuration, log);

        for (Property property : configuration.getAllProperties().values()) {
            property.setConfiguration(configuration);
        }

        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            getRaws().put(raw.getPath(), raw);
            setConfiguration(configuration);
        }
        current = null;
        setConfiguration(configuration);

        return "/rhq/resource/configuration/edit.xhtml?currentResourceId=" + getResourceId();
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

    /**
     * 
     */
    String selectedPath;
    private TreeMap<String, RawConfiguration> raws;
    TreeMap<String, RawConfiguration> modified = new TreeMap<String, RawConfiguration>();
    RawConfiguration current = null;

}