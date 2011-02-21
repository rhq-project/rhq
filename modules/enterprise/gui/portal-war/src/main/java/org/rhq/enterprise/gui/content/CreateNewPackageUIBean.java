/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.model.UploadItem;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.composite.RepoComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentException;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Collects data necessary for creating an artifact and provides actions to perform the create.
 *
 * The associated Facelets page is: /rhq/resource/content/create.xhtml
 *
 * @author Jason Dobies
 * @author Ian Springer
 */
public class CreateNewPackageUIBean {

    /**
     * Option value for deploying the package to a repo the resource is already subscribed to.
     */
    private static final String REPO_OPTION_SUBSCRIBED = "subscribed";

    /**
     * Option value for deploying the package to a repo the resource is not subscribed to, as well as automatically
     * subscribing the resource to that repo.
     */
    private static final String REPO_OPTION_UNSUBSCRIBED = "unsubscribed";

    /**
     * Option value for creating a new repo, subscribing the resource to it, and deploying the package to that
     * repo.
     */
    private static final String REPO_OPTION_NEW = "new";

    /**
     * Option value for no repo.  This is a standalone war that may not be related to any repo.
     */
    private static final String REPO_OPTION_NONE = "none";

    private String packageName;
    private String version = "1.0";
    private int selectedArchitectureId;
    private int selectedPackageTypeId;

    /**
     * If the user selects to add the package to an existing repo that the resource is already subscribed to,
     * this will be populated with that repo ID.
     */
    private String subscribedRepoId;

    /**
     * If the user selects to add the package to an existing repo that the resource is not already subscribed to,
     * this will be populated with that repo ID.
     */
    private String unsubscribedRepoId;

    /**
     * If the user selects to add the package to a new repo, this will be populated with the new repo's name.
     */
    private String newRepoName;

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
        if (!isNeedRequestPackageDetails()) {
            packageName = getBackingPackageName();
            selectedArchitectureId = getBackingPackageArchitectureId();
            selectedPackageTypeId = getBackingPackageTypeId();
        }
        String response = createNewPackage(packageName, version, selectedArchitectureId, selectedPackageTypeId);
        return response;
    }

    public String createNewPackage(String packageName, String version, int architectureId, int packageTypeId) {

        // Collect the necessary information
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResourceIfExists();

        HttpServletRequest request = FacesContextUtility.getRequest();
        UploadNewPackageUIBean uploadUIBean = FacesContextUtility.getManagedBean(UploadNewPackageUIBean.class);

        String repoOption = request.getParameter("repoOption");
        UploadItem fileItem = uploadUIBean.getFileItem();
        boolean usingARepo = true;

        // Validate
        if (packageName == null || packageName.trim().equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Package name must be specified");
            return null;
        }

        if (version == null || version.trim().equals("")) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Package version must be specified");
            return null;
        }

        if (repoOption == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "A repository deployment option must be specified");
            return null;
        }

        if (repoOption.equals(REPO_OPTION_NEW) && (newRepoName == null || newRepoName.trim().equals(""))) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "When creating a new repo, the name of the repository to be created must be specified");
            return null;
        }

        if ((fileItem == null) || fileItem.getFile() == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "A package file must be uploaded");
            return null;
        }

        if (repoOption.equalsIgnoreCase(REPO_OPTION_NONE)) {
            usingARepo = false;
        }

        // Determine which repo the package will go into
        String repoId = null;
        if (usingARepo) {
            if (resource != null) {
                try {
                    repoId = determineRepo(repoOption, subject, resource.getId());
                } catch (ContentException ce) {
                    String errorMessages = ThrowableUtil.getAllMessages(ce);
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to determine repository. Cause: "
                        + errorMessages);
                    return "failure";
                }
            } else {
                //we're creating a package directly inside a repo. The repo id
                //will be in the request params.
                repoId = FacesContextUtility.getRequiredRequestParameter("id");
            }
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
               refactored in the future for a general way of adding packages to the repo as its own operation. For
               now, don't worry about that. The rest of this will be written assuming it's part of the deploy
               workflow and we'll deal with the refactoring later.
               jdobies, Feb 27, 2008
             */
            PackageVersion packageVersion = null;
            try {
                ContentManagerLocal contentManager = LookupUtil.getContentManager();

                //store information about uploaded file for packageDetails as most of it is already available
                Map<String, String> packageUploadDetails = new HashMap<String, String>();
                packageUploadDetails.put(ContentManagerLocal.UPLOAD_FILE_SIZE, String.valueOf(fileItem.getFileSize()));
                packageUploadDetails.put(ContentManagerLocal.UPLOAD_FILE_INSTALL_DATE, String.valueOf(System
                    .currentTimeMillis()));
                packageUploadDetails.put(ContentManagerLocal.UPLOAD_OWNER, subject.getName());
                packageUploadDetails.put(ContentManagerLocal.UPLOAD_FILE_NAME, fileItem.getFileName());

                try {//Easier to implement here than in server side bean. Shouldn't affect performance too much.
                    packageUploadDetails.put(ContentManagerLocal.UPLOAD_MD5, new MessageDigestGenerator(
                        MessageDigestGenerator.MD5).calcDigestString(fileItem.getFile()));
                    packageUploadDetails.put(ContentManagerLocal.UPLOAD_SHA256, new MessageDigestGenerator(
                        MessageDigestGenerator.SHA_256).calcDigestString(fileItem.getFile()));
                } catch (IOException e1) {
                    log.warn("Error calculating file digest(s) : " + e1.getMessage());
                    e1.printStackTrace();
                }

                //TODO: need to get parent id instead right? ref to app server inst itself?
                Integer newResourceTypeId = resource == null ? null : resource.getResourceType().getId();
                packageVersion = contentManager.getUploadedPackageVersion(packageName, packageTypeId, version,
                    architectureId, packageStream, packageUploadDetails, newResourceTypeId);

            } catch (NoResultException nre) {
                //eat the exception.  Some of the queries return no results if no package yet exists which is fine.
            } catch (Exception e) {
                String errorMessages = ThrowableUtil.getAllMessages(e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to create package [" + packageName
                    + "] in repository. Cause: " + errorMessages);
                return "failure";
            }

            int[] packageVersionList = new int[] { packageVersion.getId() };

            // Add the package to the repo
            if (usingARepo) {
                try {
                    int iRepoId = Integer.parseInt(repoId);

                    RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
                    repoManager.addPackageVersionsToRepo(subject, iRepoId, packageVersionList);
                } catch (Exception e) {
                    String errorMessages = ThrowableUtil.getAllMessages(e);
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to associate package ["
                        + packageName + "] with repository ID [" + repoId + "]. Cause: " + errorMessages);
                    return "failure";
                }
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
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "At least one package must be selected.");
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
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ContentManagerLocal contentManager = LookupUtil.getContentManager();
        List<Architecture> architectures = contentManager.findArchitectures(subject);

        SelectItem[] items = new SelectItem[architectures.size()];
        int itemCounter = 0;
        for (Architecture arch : architectures) {
            SelectItem item = new SelectItem(arch.getId(), arch.getName());
            items[itemCounter++] = item;
        }

        return items;
    }

    public SelectItem[] getPackageTypes() {
        Resource resource = EnterpriseFacesContextUtility.getResourceIfExists();

        List<PackageType> packageTypes = null;
        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        if (resource != null) {
            packageTypes = contentUIManager.getPackageTypes(resource.getResourceType().getId());
        } else {
            packageTypes = contentUIManager.getPackageTypes();
        }

        SelectItem[] items = new SelectItem[packageTypes.size()];
        int itemCounter = 0;
        for (PackageType packageType : packageTypes) {
            SelectItem item = new SelectItem(packageType.getId(), packageType.getDisplayName());
            items[itemCounter++] = item;
        }

        return items;
    }

    public SelectItem[] getSubscribedRepos() {
        Resource resource = EnterpriseFacesContextUtility.getResource();

        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        List<RepoComposite> repos = repoManager.findResourceSubscriptions(resource.getId());

        SelectItem[] items = new SelectItem[repos.size()];
        int itemCounter = 0;
        for (RepoComposite repoComposite : repos) {
            Repo repo = repoComposite.getRepo();
            SelectItem item = new SelectItem(repo.getId(), repo.getName());
            items[itemCounter++] = item;
        }

        return items;
    }

    public SelectItem[] getUnsubscribedRepos() {
        Resource resource = EnterpriseFacesContextUtility.getResource();

        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        List<RepoComposite> repos = repoManager.findAvailableResourceSubscriptions(resource.getId());

        SelectItem[] items = new SelectItem[repos.size()];
        int itemCounter = 0;
        for (RepoComposite repoComposite : repos) {
            Repo repo = repoComposite.getRepo();
            SelectItem item = new SelectItem(repo.getId(), repo.getName());
            items[itemCounter++] = item;
        }

        return items;
    }

    public boolean isNeedRequestPackageDetails() {
        if (!isResourcePackage()) {
            return true;
        }
        
        boolean isPackageBacked = isResourcePackageBacked();
        boolean backingPackageExists = lookupBackingPackage() != null;

        return !isPackageBacked || !backingPackageExists;
    }

    public boolean isResourcePackage() {
        return EnterpriseFacesContextUtility.getResourceIfExists() != null;
    }
    
    public boolean isResourcePackageBacked() {
        Resource resource = EnterpriseFacesContextUtility.getResource();
        ResourceType resourceType = resource.getResourceType();

        return resourceType.getCreationDataType() == ResourceCreationDataType.CONTENT;
    }

    public InstalledPackage lookupBackingPackage() {
        if (backingPackage == null) {
            Resource resource = EnterpriseFacesContextUtility.getResource();

            ContentManagerLocal contentManager = LookupUtil.getContentManager();
            backingPackage = contentManager.getBackingPackageForResource(EnterpriseFacesContextUtility.getSubject(),
                resource.getId());
        }

        return backingPackage;
    }

    public String getBackingPackageName() {
        InstalledPackage installedPackage = lookupBackingPackage();
        PackageVersion packageVersion = installedPackage.getPackageVersion();
        Package pkg = packageVersion.getGeneralPackage();

        return pkg.getName();
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
        if (packageName != null) {
            return packageName;
        }
        
        UploadNewPackageUIBean uploadUIBean = FacesContextUtility.getManagedBean(UploadNewPackageUIBean.class);
        UploadItem fileItem = uploadUIBean.getFileItem();
        
        return fileItem == null ? null : fileItem.getFileName();
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

    public String getSubscribedRepoId() {
        return subscribedRepoId;
    }

    public void setSubscribedRepoId(String subscribedRepoId) {
        this.subscribedRepoId = subscribedRepoId;
    }

    public String getUnsubscribedRepoId() {
        return unsubscribedRepoId;
    }

    public void setUnsubscribedRepoId(String unsubscribedRepoId) {
        this.unsubscribedRepoId = unsubscribedRepoId;
    }

    public String getNewRepoName() {
        return newRepoName;
    }

    public void setNewRepoName(String newRepoName) {
        this.newRepoName = newRepoName;
    }

    private String determineRepo(String repoOption, Subject subject, int resourceId) throws ContentException {
        String repoId = null;

        if (repoOption.equals(REPO_OPTION_SUBSCRIBED)) {
            repoId = subscribedRepoId;
        } else if (repoOption.equals(REPO_OPTION_UNSUBSCRIBED)) {
            repoId = unsubscribedRepoId;
            int iRepoId = Integer.parseInt(repoId);

            RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
            repoManager.subscribeResourceToRepos(subject, resourceId, new int[] { iRepoId });

            // Change the subscribedRepoId so if we fall back to the page with a different error,
            // the drop down for selecting an existing subscribed repo will be populated with this
            // new repo
            subscribedRepoId = repoId;
        } else if (repoOption.equals(REPO_OPTION_NEW)) {
            RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();

            Repo newRepo = new Repo(newRepoName);
            newRepo.setCandidate(false);
            newRepo = repoManager.createRepo(subject, newRepo);

            repoId = Integer.toString(newRepo.getId());

            repoManager.subscribeResourceToRepos(subject, resourceId, new int[] { newRepo.getId() });

            // Change the subscribedRepoId so if we fall back to the page with a different error,
            // the drop down for selecting an existing subscribed repo will be populated with this
            // new repo
            subscribedRepoId = repoId;
        }

        return repoId;
    }
}