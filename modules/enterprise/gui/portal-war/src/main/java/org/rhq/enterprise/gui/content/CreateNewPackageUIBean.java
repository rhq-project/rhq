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
package org.rhq.enterprise.gui.content;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.model.UploadItem;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.composite.ChannelComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.content.ContentException;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Collects data necessary for creating an artifact and provides actions to perform the create.
 *
 * @author Jason Dobies
 */
public class CreateNewPackageUIBean {

    /**
     * Option value for deploying the package to a channel the resource is already subscribed to.
     */
    private static final String CHANNEL_OPTION_SUBSCRIBED = "subscribed";

    /**
     * Option value for deploying the package to a channel the resource is not subscribed to, as well as automatically
     * subscribing the resource to that channel.
     */
    private static final String CHANNEL_OPTION_UNSUBSCRIBED = "unsubscribed";

    /**
     * Option value for creating a new channel, subscribing the resource to it, and deploying the package to that
     * channel.
     */
    private static final String CHANNEL_OPTION_NEW = "new";

    private String packageName;
    private String version;
    private int selectedArchitectureId;
    private int selectedPackageTypeId;

    /**
     * If the user selects to add the package to an existing channel that the resource is already subscribed to,
     * this will be populated with that channel ID.
     */
    private String subscribedChannelId;

    /**
     * If the user selects to add the package to an existing channel taht the resource is not already subscribed to,
     * this will be populated with that channel ID.
     */
    private String unsubscribedChannelId;

    /**
     * If the user selects to add the package to a new channel, this will be populated with the new channel's name.
     */
    private String newChannelName;

    /**
     * Type of resource against which the package is being created. This is loaded from information in the request
     * and is used to determine if we need to perform different handling for package-backed resources.
     */
    private ResourceType resourceType;

    /**
     * If this create is against a package-backed resource, this will hold the current package backing the resource.
     * We'll use this to auto-populate the name, architecture, and type in the case of pushing an update.
     */
    private InstalledPackage backingPackage;

    private final Log log = LogFactory.getLog(this.getClass());

    public String cancel() {
        UploadNewPackageUIBean uploadUIBean = FacesContextUtility.getManagedBean(UploadNewPackageUIBean.class);
        if (uploadUIBean != null) {
            uploadUIBean.clear();
        }
        return "cancel";
    }

    public String createPackage() {
        HttpServletRequest request = FacesContextUtility.getRequest();

        String response;
        if (request.getParameter("newPackage") != null) {
            response = createNewPackage(packageName, version, selectedArchitectureId, selectedPackageTypeId);
        } else {
            String packageName = getBackingPackageName();
            String version = Long.toString(System.currentTimeMillis());
            int architectureId = getBackingPackageArchitectureId();
            int packageTypeId = getBackingPackageTypeId();

            response = createNewPackage(packageName, version, architectureId, packageTypeId);
        }

        return response;
    }

    public String createNewPackage(String packageName, String version, int architectureId, int packageTypeId) {

        // Collect the necessary information
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();

        HttpServletRequest request = FacesContextUtility.getRequest();
        UploadNewPackageUIBean uploadUIBean = FacesContextUtility.getManagedBean(UploadNewPackageUIBean.class);

        String channelOption = request.getParameter("channelOption");
        UploadItem fileItem = uploadUIBean.getFileItem();

        // Validate
        if (packageName == null || packageName.trim().equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Package name must be specified");
            return null;
        }

        if (version == null || version.trim().equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Package version must be specified");
            return null;
        }

        if (channelOption == null) {
            FacesContextUtility
                .addMessage(FacesMessage.SEVERITY_ERROR, "A channel deployment option must be specified");
            return null;
        }

        if (channelOption.equals(CHANNEL_OPTION_NEW) && (newChannelName == null || newChannelName.trim().equals(""))) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "When creating a new channel, the name of the channel to be created must be specified");
            return null;
        }

        if ((fileItem == null) || fileItem.getFile() == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "A package file must be uploaded");
            return null;
        }

        // Determine which channel the package will go into
        String channelId = null;
        try {
            channelId = determineChannel(channelOption, subject, resource.getId());
        } catch (ContentException ce) {
            String errorMessages = ThrowableUtil.getAllMessages(ce);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to determine channel. Cause: "
                + errorMessages);
            return "failure";
        }

        try {
            // Grab a stream for the file being uploaded
            InputStream packageStream;

            try {
                log.debug("Streaming new package bits from uploaded file: " + fileItem.getFile());
                packageStream = new FileInputStream(fileItem.getFile());
            } catch (IOException e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to retrieve the input stream. Cause: " + errorMessages);
                return "failure";
            }

            // Ask the bean to create the package

            /* Currently, this is just used in the workflow for deploying a new package. This will probably get
               refactored in the future for a general way of adding packages to the channel as its own operation. For
               now, don't worry about that. The rest of this will be written assuming it's part of the deploy
               workflow and we'll deal with the refactoring later.
               jdobies, Feb 27, 2008
             */
            PackageVersion packageVersion;
            try {
                ContentManagerLocal contentManager = LookupUtil.getContentManager();
                packageVersion = contentManager.createPackageVersion(packageName, packageTypeId, version,
                    architectureId, packageStream);
            } catch (Exception e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to create package [" + packageName
                    + "] in channel. Cause: " + errorMessages);
                return "failure";
            }

            int[] packageVersionList = new int[] { packageVersion.getId() };

            // Add the package to the channel
            try {
                int iChannelId = Integer.parseInt(channelId);

                ChannelManagerLocal channelManager = LookupUtil.getChannelManagerLocal();
                channelManager.addPackageVersionsToChannel(subject, iChannelId, packageVersionList);
            } catch (Exception e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to associate package ["
                    + packageName + "] with channel ID [" + channelId + "]. Cause: " + errorMessages);
                return "failure";
            }

            // Put the package ID in the session so it can fit into the deploy existing package workflow
            HttpSession session = request.getSession();
            session.setAttribute("selectedPackages", packageVersionList);
        } finally {
            // clean up the temp file
            uploadUIBean.clear();
        }

        return "success";
    }

    public String deployExisting() {
        // Stuff the selected packages into an attribute for the next step in the flow
        String[] selectedPackages = FacesContextUtility.getRequest().getParameterValues("selectedPackages");

        if (selectedPackages == null || selectedPackages.length == 0) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "At least one package must be selected");
            return "failure";
        }

        // Convert into int[] to hold on to the package version IDs we're going to deploy
        // Ultimately, this will need to go in a holder object so we can populate and associate the configuration
        // values if they exist
        int[] selectedPackageIds = new int[selectedPackages.length];
        int counter = 0;
        for (String sPackageId : selectedPackages) {
            int iPackageId = Integer.parseInt(sPackageId);
            selectedPackageIds[counter++] = iPackageId;
        }

        HttpServletRequest request = FacesContextUtility.getRequest();
        HttpSession session = request.getSession();
        session.setAttribute("selectedPackages", selectedPackageIds);

        return "success";
    }

    public SelectItem[] getArchitectures() {
        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        List<Architecture> architectures = contentUIManager.getArchitectures();

        SelectItem[] items = new SelectItem[architectures.size()];
        int itemCounter = 0;
        for (Architecture arch : architectures) {
            SelectItem item = new SelectItem(arch.getId(), arch.getName());
            items[itemCounter++] = item;
        }

        return items;
    }

    public SelectItem[] getPackageTypes() {
        Resource resource = EnterpriseFacesContextUtility.getResource();

        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        List<PackageType> packageTypes = contentUIManager.getPackageTypes(resource.getResourceType().getId());

        SelectItem[] items = new SelectItem[packageTypes.size()];
        int itemCounter = 0;
        for (PackageType packageType : packageTypes) {
            SelectItem item = new SelectItem(packageType.getId(), packageType.getDisplayName());
            items[itemCounter++] = item;
        }

        return items;
    }

    public SelectItem[] getSubscribedChannels() {
        Resource resource = EnterpriseFacesContextUtility.getResource();

        ChannelManagerLocal channelManager = LookupUtil.getChannelManagerLocal();
        List<ChannelComposite> channels = channelManager.findResourceSubscriptions(resource.getId());

        SelectItem[] items = new SelectItem[channels.size()];
        int itemCounter = 0;
        for (ChannelComposite channelComposite : channels) {
            Channel channel = channelComposite.getChannel();
            SelectItem item = new SelectItem(channel.getId(), channel.getName());
            items[itemCounter++] = item;
        }

        return items;
    }

    public SelectItem[] getUnsubscribedChannels() {
        Resource resource = EnterpriseFacesContextUtility.getResource();

        ChannelManagerLocal channelManager = LookupUtil.getChannelManagerLocal();
        List<ChannelComposite> channels = channelManager.findAvailableResourceSubscriptions(resource.getId());

        SelectItem[] items = new SelectItem[channels.size()];
        int itemCounter = 0;
        for (ChannelComposite channelComposite : channels) {
            Channel channel = channelComposite.getChannel();
            SelectItem item = new SelectItem(channel.getId(), channel.getName());
            items[itemCounter++] = item;
        }

        return items;
    }

    public boolean getNeedRequestPackageDetails() {
        boolean isPackageBacked = isResourcePackageBacked();
        boolean backingPackageExists = lookupBackingPackage() != null;

        return !isPackageBacked || !backingPackageExists;
    }

    public boolean isResourcePackageBacked() {
        Resource resource = EnterpriseFacesContextUtility.getResource();
        ResourceType resourceType = resource.getResourceType();

        return resourceType.getCreationDataType() == ResourceCreationDataType.CONTENT;
    }

    public InstalledPackage lookupBackingPackage() {
        if (backingPackage == null) {
            Resource resource = EnterpriseFacesContextUtility.getResource();

            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
            backingPackage = contentUIManager.getBackingPackageForResource(resource.getId());
        }

        return backingPackage;
    }

    public String getBackingPackageName() {
        InstalledPackage ip = lookupBackingPackage();
        PackageVersion pv = ip.getPackageVersion();
        Package p = pv.getGeneralPackage();

        return p.getName();
    }

    public int getBackingPackageArchitectureId() {
        InstalledPackage ip = lookupBackingPackage();
        PackageVersion pv = ip.getPackageVersion();

        return pv.getArchitecture().getId();
    }

    public int getBackingPackageTypeId() {
        InstalledPackage ip = lookupBackingPackage();
        PackageVersion pv = ip.getPackageVersion();
        Package p = pv.getGeneralPackage();

        return p.getPackageType().getId();
    }

    public String getNextBackingPackageVersion() {
        return Long.toString(System.currentTimeMillis());
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getSelectedArchitectureId() {
        return selectedArchitectureId;
    }

    public void setSelectedArchitectureId(int selectedArchitectureId) {
        this.selectedArchitectureId = selectedArchitectureId;
    }

    public int getSelectedPackageTypeId() {
        return selectedPackageTypeId;
    }

    public void setSelectedPackageTypeId(int selectedPackageTypeId) {
        this.selectedPackageTypeId = selectedPackageTypeId;
    }

    public String getSubscribedChannelId() {
        return subscribedChannelId;
    }

    public void setSubscribedChannelId(String subscribedChannelId) {
        this.subscribedChannelId = subscribedChannelId;
    }

    public String getUnsubscribedChannelId() {
        return unsubscribedChannelId;
    }

    public void setUnsubscribedChannelId(String unsubscribedChannelId) {
        this.unsubscribedChannelId = unsubscribedChannelId;
    }

    public String getNewChannelName() {
        return newChannelName;
    }

    public void setNewChannelName(String newChannelName) {
        this.newChannelName = newChannelName;
    }

    private String determineChannel(String channelOption, Subject subject, int resourceId) throws ContentException {
        String channelId = null;

        if (channelOption.equals(CHANNEL_OPTION_SUBSCRIBED)) {
            channelId = subscribedChannelId;
        } else if (channelOption.equals(CHANNEL_OPTION_UNSUBSCRIBED)) {
            channelId = unsubscribedChannelId;
            int iChannelId = Integer.parseInt(channelId);

            ChannelManagerLocal channelManager = LookupUtil.getChannelManagerLocal();
            channelManager.subscribeResourceToChannels(subject, resourceId, new int[] { iChannelId });

            // Change the subscribedChannelId so if we fall back to the page with a different error,
            // the drop down for selecting an existing subscribed channel will be populated with this
            // new channel
            subscribedChannelId = channelId;
        } else if (channelOption.equals(CHANNEL_OPTION_NEW)) {
            ChannelManagerLocal channelManager = LookupUtil.getChannelManagerLocal();

            Channel newChannel = new Channel(newChannelName);
            newChannel = channelManager.createChannel(subject, newChannel);

            channelId = Integer.toString(newChannel.getId());

            channelManager.subscribeResourceToChannels(subject, resourceId, new int[] { newChannel.getId() });

            // Change the subscribedChannelId so if we fall back to the page with a different error,
            // the drop down for selecting an existing subscribed channel will be populated with this
            // new channel
            subscribedChannelId = channelId;
        }

        return channelId;
    }
}