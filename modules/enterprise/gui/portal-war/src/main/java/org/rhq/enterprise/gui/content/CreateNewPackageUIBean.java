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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.composite.ChannelComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
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
    private int subscribedChannelId;

    /**
     * If the user selects to add the package to an existing channel taht the resource is not already subscribed to,
     * this will be populated with that channel ID.
     */
    private int unsubscribedChannelId;

    /**
     * If the user selects to add the package to a new channel, this will be populated with the new channel's name.
     */
    private String newChannelName;

    private final Log log = LogFactory.getLog(this.getClass());

    public String createPackage() {

        // Collect the necessary information
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();

        HttpServletRequest request = FacesContextUtility.getRequest();

        String channelOption = request.getParameter("channelOption");
        FileItem fileItem = (FileItem)request.getAttribute("uploadForm:uploadFile");

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
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "A channel deployment option must be specified");
            return null;
        }

        if (channelOption.equals(CHANNEL_OPTION_NEW) &&
            (newChannelName == null || newChannelName.trim().equals(""))) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "When creating a new channel, the name of the channel to be created must be specified");
            return null;
        }

        if ((fileItem.getName() == null) || fileItem.getName().equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "An artifact file must be specified");
            return null;
        }

        // Determine which channel the package will go into
        int channelId = determineChannel(channelOption, subject, resource.getId());

        // Grab a stream for the file being uploaded
        InputStream packageStream;

        try {
            packageStream = fileItem.getInputStream();
        } catch (IOException e) {
            String errorMessages = ThrowableUtil.getAllMessages(e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to retrieve the input stream. Cause: "
                + errorMessages);
            return "failure";
        }

        // Ask the bean to create the package

        /* Currently, this is just used in the workflow for deploying a new package. This will probably get
           refactored in the future for a general way of adding packages to the channel as its own operation. For
           now, don't worry about that. The rest of this will be written assuming it's part of the deploy
           workflow and we'll deal with the refactoring later.
           jdobies, Feb 27, 2008
         */
        try {
            ContentManagerLocal contentManager = LookupUtil.getContentManager();
            contentManager.createPackageVersion(packageName, selectedPackageTypeId,
                                                version, selectedArchitectureId, packageStream);
        } catch (Exception e) {
            String errorMessages = ThrowableUtil.getAllMessages(e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to create package [" + packageName + "] in channel. Cause: " + errorMessages);
            return "failure";
        }

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
        List<ChannelComposite> channels = channelManager.getResourceSubscriptions(resource.getId());

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
        List<ChannelComposite> channels = channelManager.getAvailableResourceSubscriptions(resource.getId());

        SelectItem[] items = new SelectItem[channels.size()];
        int itemCounter = 0;
        for (ChannelComposite channelComposite : channels) {
            Channel channel = channelComposite.getChannel();
            SelectItem item = new SelectItem(channel.getId(), channel.getName());
            items[itemCounter++] = item;
        }

        return items;
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

    public int getSubscribedChannelId() {
        return subscribedChannelId;
    }

    public void setSubscribedChannelId(int subscribedChannelId) {
        this.subscribedChannelId = subscribedChannelId;
    }

    public int getUnsubscribedChannelId() {
        return unsubscribedChannelId;
    }

    public void setUnsubscribedChannelId(int unsubscribedChannelId) {
        this.unsubscribedChannelId = unsubscribedChannelId;
    }

    public String getNewChannelName() {
        return newChannelName;
    }

    public void setNewChannelName(String newChannelName) {
        this.newChannelName = newChannelName;
    }

    private int determineChannel(String channelOption, Subject subject, int resourceId) {
        int channelId = -1;
        
        if (channelOption.equals(CHANNEL_OPTION_SUBSCRIBED)) {
            channelId = subscribedChannelId;
        }
        else if (channelOption.equals(CHANNEL_OPTION_UNSUBSCRIBED)) {
            channelId = unsubscribedChannelId;

            ChannelManagerLocal channelManager = LookupUtil.getChannelManagerLocal();
            channelManager.subscribeResourceToChannels(subject, resourceId, new int[]{channelId});
        }
        else if (channelOption.equals(CHANNEL_OPTION_NEW)) {
            ChannelManagerLocal channelManager = LookupUtil.getChannelManagerLocal();

            Channel newChannel = new Channel(newChannelName);
            newChannel = channelManager.createChannel(subject, newChannel);

            channelId = newChannel.getId();

            channelManager.subscribeResourceToChannels(subject, resourceId, new int[]{channelId});
        }

        return channelId;
    }
}