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
package org.rhq.enterprise.server.plugin.pc.content;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceSyncStatus;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.DistributionManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.content.metadata.ContentSourcePluginMetadataManager;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Responsible for managing {@link ContentProvider} implementations. These implementations
 * come from the content plugins themselves.
 *
 * @author John Mazzitelli
 */
public class ContentProviderManager {
    private static final Log log = LogFactory.getLog(ContentProviderManager.class);

    private ContentServerPluginManager pluginManager;
    private Map<ContentSource, ContentProvider> adapters;

    // This is used as a monitor lock to the synchronizeContentSource method;
    // it helps us avoid two content sources getting synchronized at the same time.
    private final Object synchronizeContentSourceLock = new Object();

    /**
     * Asks that the adapter responsible for the given content source return a stream to the package
     * bits for the package at the given location.
     *
     * @param contentSourceId the adapter for this content source will be used to stream the bits
     * @param location        where the adapter can find the package bits on the content source
     * @return the stream to the package bits
     * @throws Exception if the adapter failed to load the bits
     */
    public InputStream loadPackageBits(int contentSourceId, String location) throws Exception {
        ContentProvider adapter = getIsolatedContentProvider(contentSourceId);

        PackageSource packageSource = (PackageSource) adapter;
        InputStream inputStream = packageSource.getInputStream(location);

        if (inputStream == null) {
            throw new Exception("Adapter for content source [" + contentSourceId
                + "] failed to give us a stream to the package at location [" + location + "]");
        }

        return inputStream;
    }


    /**
     * Asks that the adapter responsible for the given content source return a stream to the
     * DistributionFile bits for the DistributionFile at the given location.
     *
     * @param contentSourceId the adapter for this content source will be used to stream the bits
     * @param location        where the adapter can find the DistributionFile bits on the source
     * @return the stream to the DistributionFile bits
     * @throws Exception if the adapter failed to load the bits
     */
    public InputStream loadDistributionFileBits(int contentSourceId, String location)
        throws Exception {
        ContentProvider adapter = getIsolatedContentProvider(contentSourceId);

        DistributionSource distSource = (DistributionSource) adapter;
        InputStream inputStream = distSource.getInputStream(location);

        if (inputStream == null) {
            throw new Exception("Adapter for content source [" + contentSourceId
                + "] failed to give us a stream to the distribution file at location [" + location +
                "]");
        }

        return inputStream;
    }

    /**
     * Asks the provider responsible for the given content source to synchronize with its remote
     * repository. This will not attempt to load any package bits - it only synchronizes the repos
     * and package version information. Note that if a synchronization is already currently underway,
     * this method will not do anything and will return <code>false</code> (in effect, will let the
     * currently running synchronization continue; we try not to step on it). If this method does
     * actually do a sync, <code>true</code> will be returned.
     *
     * @param contentSourceId identifies the database entry of the provider
     * @return <code>true</code> if the synchronization completed; <code>false</code> if there was
     *         already a synchronization happening and this method aborted
     * @throws Exception
     */
    public boolean synchronizeContentSource(int contentSourceId) throws Exception {

        ContentSourceManagerLocal contentSourceManager = LookupUtil.getContentSourceManager();
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();

        ContentProvider provider = getIsolatedContentProvider(contentSourceId);
        ContentSourceSyncResults results = null;
        SubjectManagerLocal subjMgr = LookupUtil.getSubjectManager();
        Subject overlord = subjMgr.getOverlord();

        // append to this as we go along, building a status report
        StringBuilder progress = new StringBuilder();
        long start = System.currentTimeMillis();

        try {
            ContentSource contentSource = contentSourceManager
                .getContentSource(overlord, contentSourceId);
            if (contentSource == null) {
                throw new Exception(
                    "Cannot sync a non-existing content source [" + contentSourceId + "]");
            }

            // This should not take very long so it should be OK to block other callers.
            // We are avoiding the problem that would occur if we try to synchronize the same source
            // at the same time. We could do it more cleverly by synchronizing on a per content source
            // basis, but I don't see a need right now to make this more complicated.
            // We can come back and revisit if we need more fine-grained locking.
            synchronized (synchronizeContentSourceLock) {
                progress.append(new Date()).append(": ");
                progress.append("Start synchronization of content provider [")
                    .append(contentSource.getName()).append(
                    "]");
                progress.append('\n');
                progress.append(new Date()).append(": ");
                progress.append("Getting currently known list of content source packages...");
                results = new ContentSourceSyncResults(contentSource);
                results.setResults(progress.toString());
                results = contentSourceManager.persistContentSourceSyncResults(results);
            }

            if (results == null) {
                // note that it technically is still possible to have concurrent syncs - if two
                // threads running in two different servers (i.e. different VMs) both try to sync the
                // same content source and both enter the persistContentSourceSyncResults method at
                // the same time, you'll get two inprogress rows - this is so rare as to not care.
                // Even if it does happen, it may still work, or
                // one sync will get an error and rollback its tx and no harm will be done.
                log.info("Content provider [" + contentSource.getName()
                    + "] is already currently being synchronized, this sync request will be ignored");
                return false;
            }

            // If the provider is capable of syncing repos, do that call first
            if (provider instanceof RepoSource) {
                progress.append(new Date()).append(": ");
                progress.append("Asking content provider for repositories to import...");

                RepoSource repoSource = (RepoSource) provider;

                start = System.currentTimeMillis();

                // Call to the plugin
                RepoImportReport report = repoSource.importRepos();

                repoManager.processRepoImportReport(overlord, report, contentSourceId, progress);

                log.info("importRepos: [" + contentSource.getName() + "]: report has been merged ("
                    + (System.currentTimeMillis() - start) + ")ms");
            }

            // ------------------
            // NOTE: Ultimately, this will be moved out as repos get their own schedules. For now,
            //       simply synchronize *all* repos for this provider.
            // ------------------

            // If the provider is capable of handling packages, perform the package synchronization
            // portion of the sync
            if (provider instanceof PackageSource) {
                PackageSource packageSource = (PackageSource) provider;

                RepoCriteria reposForContentSource = new RepoCriteria();
                reposForContentSource.addFilterContentSourceIds(contentSourceId);
                reposForContentSource.addFilterCandidate(false); // Don't sync packages for candidates

                List<Repo> repos = repoManager.findReposByCriteria(overlord, reposForContentSource);

                // Temporary functionality: loop for all repos that use this content source
                for (Repo repo : repos) {
                    // Convert the existing package list for this repo into DTOs for the API
                    // --------------------------------------------
                    List<PackageVersionContentSource> existingPVCS; // already know about this source
                    Set<ContentProviderPackageDetails> allDetails; // send to plugin
                    Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> keyPVCSMap;

                    start = System.currentTimeMillis();

                    existingPVCS = contentSourceManager
                        .getPackageVersionsFromContentSourceForRepo(subjMgr.getOverlord(),
                            contentSource.getId(), repo.getId());

                    int existingCount = existingPVCS.size();
                    keyPVCSMap = new HashMap<ContentProviderPackageDetailsKey,
                        PackageVersionContentSource>(existingCount);
                    allDetails = new HashSet<ContentProviderPackageDetails>(existingCount);

                    translateDomainToDto(existingPVCS, allDetails, keyPVCSMap);

                    log.info("synchronizePackages: [" + contentSource.getName() +
                        "]: loaded existing list of size=["
                        + existingCount + "] (" + (System.currentTimeMillis() - start) + ")ms");

                    progress.append(existingCount).append('\n');
                    progress.append(new Date()).append(": ");
                    progress.append("Asking content source to update the list of packages...");
                    results.setResults(progress.toString());
                    results = contentSourceManager.mergeContentSourceSyncResults(results);
                    start = System.currentTimeMillis();

                    PackageSyncReport report; // the plugin will fill this in for us
                    report = new PackageSyncReport();

                    packageSource.synchronizePackages(repo.getName(), report, allDetails);

                    log.info("synchronizePackages: [" + contentSource.getName() +
                        "]: got sync report from adapter=["
                        + report + "] (" + (System.currentTimeMillis() - start) + ")ms");

                    progress.append("new=").append(report.getNewPackages().size());
                    progress.append(", updated=").append(report.getUpdatedPackages().size());
                    progress.append(", deleted=").append(report.getDeletedPackages().size())
                        .append('\n');
                    progress.append(new Date()).append(": ");
                    progress.append("FULL SUMMARY FOLLOWS:").append('\n');
                    progress.append(report.getSummary()).append('\n');
                    progress.append(new Date()).append(": ");
                    progress.append("Merging the updated list of packages to database").append('\n');
                    results.setResults(progress.toString());
                    results = contentSourceManager.mergeContentSourceSyncResults(results);
                    start = System.currentTimeMillis();

                    results = contentSourceManager
                        .mergeContentSourceSyncReport(contentSource, report, keyPVCSMap, results);

                }

                // Reset our progress string to the full results including the text added by the merge
                progress.setLength(0);
                progress.append(results.getResults());

                log.info(
                    "synchronizePackages: [" + contentSource.getName() + "]: report has been merged ("
                        + (System.currentTimeMillis() - start) + ")ms");

                progress.append(new Date()).append(": ").append("DONE.");
                results.setResults(progress.toString());
                results.setStatus(ContentSourceSyncStatus.SUCCESS);
                results = contentSourceManager.mergeContentSourceSyncResults(results);
            }

            log.info("Checking if provider: " + provider + " is a DistributionSource");
            if (provider instanceof DistributionSource) {
                log.info("synchronizeContentSource(" + contentSourceId +
                    "):  this source is a DistributionSource");
                log.info("We will skip the sync for now until PackageSource is working.");

                DistributionSource distSource = (DistributionSource) provider;
                RepoCriteria reposForContentSource = new RepoCriteria();
                reposForContentSource.addFilterContentSourceIds(contentSourceId);
                reposForContentSource.addFilterCandidate(false); // Don't sync packages for candidates
                List<Repo> repos = repoManager.findReposByCriteria(overlord, reposForContentSource);

                // Temporary functionality: loop for all repos that use this content source
                for (Repo repo : repos) {
                    log.info(
                        "ContentProviderManager::synchronizeContentSource DistributionSource would " +
                            "be syncing repo: " + repo.getName());

                    start = System.currentTimeMillis();

                    PageControl pc = PageControl.getUnlimitedInstance();
                    log.debug("Looking up existing distributions for repoId: " + repo.getId());
                    List<Distribution> dists = repoManager
                        .findAssociatedDistributions(overlord, repo.getId(), pc);
                    log.debug("Found " + dists.size() + " Distributions for repoId " + repo.getId());

                    progress.append(dists.size()).append('\n');
                    progress.append(new Date()).append(": ");
                    progress.append("Asking content source to update the list of packages...");

                    DistributionSyncReport distReport = new DistributionSyncReport(repo.getId());
                    List<DistributionDetails> distDetails = new ArrayList<DistributionDetails>(
                        dists.size());
                    translateDomainToDto(dists, distDetails);

                    distSource.synchronizeDistribution(repo.getName(), distReport, distDetails);

                    log.info("synchronizeDistributions: [" + contentSource.getName() +
                        "]: loaded existing list of size=["
                        + dists.size() + "] (" + (System.currentTimeMillis() - start) + ")ms");

                    results.setResults(progress.toString());
                    results = contentSourceManager.mergeContentSourceSyncResults(results);
                    start = System.currentTimeMillis();


                    distSource.synchronizeDistribution(repo.getName(), distReport, distDetails);
                    log.info("synchronizeDistributions: [" + contentSource.getName() +
                        "]: got sync report from adapter=["
                        + distReport + "] (" + (System.currentTimeMillis() - start) + ")ms");

                    progress.append("new=").append(distReport.getDistributions().size());

                    progress.append(", deleted=").append(distReport.getDeletedDistributions().size())
                        .append('\n');
                    results.setResults(progress.toString());
                    results = contentSourceManager.mergeContentSourceSyncResults(results);
                    start = System.currentTimeMillis();
                    results = contentSourceManager
                        .mergeContentSourceSyncReport(contentSource, distReport, results);
                }
                // Reset our progress string to the full results including the text added by the merge
                progress.setLength(0);
                progress.append(results.getResults());

                log.info("synchronizeDistributions: [" + contentSource.getName() +
                    "]: report has been merged ("
                    + (System.currentTimeMillis() - start) + ")ms");

                progress.append(new Date()).append(": ").append("DONE.");
                results.setResults(progress.toString());
                results.setStatus(ContentSourceSyncStatus.SUCCESS);
                results = contentSourceManager.mergeContentSourceSyncResults(results);

            }
        }
        catch (Throwable t) {
            if (results != null) {
                // try to reload the results in case it was updated by the SLSB before the
                // exception happened
                ContentSourceSyncResults reloadedResults = contentSourceManager
                    .getContentSourceSyncResults(results.getId());
                if (reloadedResults != null) {
                    results = reloadedResults;
                    if (results.getResults() != null) {
                        progress = new StringBuilder(results.getResults());
                    }
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                t.printStackTrace(new PrintStream(baos));
                progress.append(new Date()).append(": ");
                progress.append("SYNCHRONIZATION ERROR - STACK TRACE FOLLOWS:\n");
                progress.append(baos.toString());
                results.setResults(progress.toString());
                results.setStatus(ContentSourceSyncStatus.FAILURE);
                // finally clause will merge this
            }

            throw new Exception("Failed to sync content source [" + contentSourceId + "]", t);
        } finally {
            if (results != null) {
                results.setEndTime(System.currentTimeMillis());
                contentSourceManager.mergeContentSourceSyncResults(results);
            }
        }

        return true;
    }

    /**
     * Tests the connection to the content source that has the given ID.
     *
     * @param contentSourceId
     * @return <code>true</code> if there is an adapter that can successfully connect to the given
     *         content source <code>false</code> if there is an adapter but it cannot connect
     * @throws Exception if failed to get an adapter to attempt the connection
     */
    public boolean testConnection(int contentSourceId) throws Exception {
        ContentProvider adapter = getIsolatedContentProvider(contentSourceId);

        try {
            adapter.testConnection();
        }
        catch (Throwable t) {
            return false;
        }

        return true;
    }

    /**
     * Returns a set of all content sources whose adapters are managed by this object.
     *
     * @return all content sources
     */
    public Set<ContentSource> getAllContentSources() {
        synchronized (this.adapters) {
            return new HashSet<ContentSource>(this.adapters.keySet());
        }
    }

    /**
     * Call this method when a new content source is added to the system during runtime. This can
     * also be used to restart an adapter that was previously {@link #shutdownAdapter(ContentSource)
     * shutdown}.
     *
     * <p>If there is already an adapter currently started for the given content source, this
     * returns silently.</p>
     *
     * @param contentSource the new content source that was added
     * @throws InitializationException if the provider throws an error on its startup
     */
    public void startAdapter(ContentSource contentSource) throws InitializationException {
        synchronized (this.adapters) {
            if (this.adapters.containsKey(contentSource)) {
                return; // already exists, which means it was already initialized
            }
        }

        instantiateAdapter(contentSource);

        try {
            log.info("Initializing content source adapter for [" + contentSource + "] of type ["
                + contentSource.getContentSourceType() + "]");

            ContentProvider adapter = getIsolatedContentSourceAdapter(contentSource);
            adapter.initialize(contentSource.getConfiguration());
        }
        catch (Exception e) {
            log.warn(
                "Failed to initialize adapter for content source [" + contentSource.getName() + "]",
                e);
            throw new InitializationException(e);
        }

        return;
    }

    /**
     * Call this method when a content source is removed from the system during runtime or if you
     * just want to shutdown the adapter for whatever reason. You can restart the adapter by calling
     * {@link #startAdapter(ContentSource)}.
     *
     * <p>If there are no adapters currently started for the given content source, this returns
     * silently.</p>
     *
     * @param contentSource the content source being deleted
     */
    public void shutdownAdapter(ContentSource contentSource) {
        synchronized (this.adapters) {
            if (!this.adapters.containsKey(contentSource)) {
                return; // doesn't exist, it was already shutdown
            }
        }

        try {
            log.info("Shutting down content source adapter for [" + contentSource + "] of type ["
                + contentSource.getContentSourceType() + "]");

            ContentProvider adapter = getIsolatedContentSourceAdapter(contentSource);
            adapter.shutdown();
        }
        catch (Throwable t) {
            log.warn(
                "Failed to shutdown adapter for content source [" + contentSource.getName() + "]", t);
        } finally {
            this.adapters.remove(contentSource);
        }
    }

    /**
     * Convienence method that simply {@link #shutdownAdapter(ContentSource) shuts down the adapter}
     * and then {@link #startAdapter(ContentSource) restarts it}. Call this when, for example, a
     * content source's {@link ContentSource#getConfiguration() configuration} has changed.
     *
     * @param contentSource the content source whose adapter is to be restarted
     * @throws Exception if there is an error asking the provider to shutdown or start
     */
    public void restartAdapter(ContentSource contentSource) throws Exception {
        shutdownAdapter(contentSource);
        startAdapter(contentSource);
    }

    /**
     * Tells this manager to initialize itself which will initialize all the adapters.
     *
     * <p>This is protected so only the plugin container and subclasses can use it.</p>
     *
     * @param pluginManager the plugin manager this object can use to obtain information from (like
     *                      classloaders)
     * @throws InitializationException if any of the providers throw an error on startup
     */
    protected void initialize(ContentServerPluginManager pluginManager) {
        this.pluginManager = pluginManager;

        ContentSourceMetadataManagerLocal metadataManager = LookupUtil
            .getContentSourceMetadataManager();
        ContentSourceManagerLocal contentSourceManager = LookupUtil.getContentSourceManager();

        // Our plugin manager should have parsed all descriptors and have our types for us.
        // Let's register the types to make sure they are merged with the old existing types.
        ContentSourcePluginMetadataManager pluginMetadataManager = this.pluginManager
            .getMetadataManager();
        Set<ContentSourceType> allTypes = pluginMetadataManager.getAllContentSourceTypes();
        metadataManager.registerTypes(allTypes);

        // now let's instantiate all adapters for all known content sources
        createInitialAdaptersMap();

        PageControl pc = PageControl.getUnlimitedInstance();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        List<ContentSource> contentSources = contentSourceManager.getAllContentSources(overlord, pc);

        // let's initalize all adapters for all content sources
        if (contentSources != null) {
            for (ContentSource contentSource : contentSources) {
                try {
                    startAdapter(contentSource);
                }
                catch (Exception e) {
                    log.warn("Failed to start adapator for content source [" + contentSource + "]");
                }
            }
        }

        return;
    }

    /**
     * Tells this manager to shutdown. This will effectively shutdown all of its adapters.
     *
     * <p>This is protected so only the plugin container and subclasses can use it.</p>
     */
    protected void shutdown() {
        HashMap<ContentSource, ContentProvider> adaptersCopy;

        // shutdown all adapters to give them a chance to clean up after themselves
        synchronized (this.adapters) {
            adaptersCopy = new HashMap<ContentSource, ContentProvider>(this.adapters);
        }

        for (ContentSource contentSource : adaptersCopy.keySet()) {
            shutdownAdapter(contentSource);
        }

        synchronized (this.adapters) {
            this.adapters.clear();
        }
    }

    /**
     * This is protected so only the plugin container and subclasses can use it.
     */
    protected void createInitialAdaptersMap() {
        this.adapters = new HashMap<ContentSource, ContentProvider>();
    }

    /**
     * Given a ID to a content source, this returns the adapter that is responsible for communicating
     * with that content source where that adapter object will ensure invocations on it are isolated
     * to its plugin classloader.
     *
     * @param contentProviderId an ID to a {@link ContentSource}
     * @return the adapter that is communicating with the content source, isolated to its classloader
     * @throws RuntimeException if there is no content source with the given ID
     */
    public ContentProvider getIsolatedContentProvider(int contentProviderId) throws RuntimeException {
        synchronized (this.adapters) {
            for (ContentSource contentSource : this.adapters.keySet()) {
                if (contentSource.getId() == contentProviderId) {
                    return getIsolatedContentSourceAdapter(contentSource);
                }
            }
        }

        throw new RuntimeException(
            "Content source ID [" + contentProviderId + "] doesn't exist; can't get adapter");
    }

    /**
     * This returns the adapter that is responsible for communicating with the given content source
     * where that adaptor object will ensure invocations on it are isolated to its plugin classloader.
     *
     * @param contentSource the returned adapter communicates with this content source
     * @return the adapter that is communicating with the content source, isolated to its classloader
     * @throws RuntimeException if there is no content source adapter available
     */
    protected ContentProvider getIsolatedContentSourceAdapter(ContentSource contentSource)
        throws RuntimeException {
        ContentProvider adapter;

        synchronized (this.adapters) {
            adapter = this.adapters.get(contentSource);
        }

        if (adapter == null) {
            throw new RuntimeException("There is no adapter for content source [" + adapter + "]");
        }

        ServerPluginEnvironment env = this.pluginManager
            .getPluginEnvironment(contentSource.getContentSourceType());
        if (env == null) {
            throw new RuntimeException(
                "There is no plugin env. for content source [" + contentSource + "]");
        }

        ClassLoader classLoader = env.getPluginClassLoader();
        IsolatedInvocationHandler handler = new IsolatedInvocationHandler(adapter, classLoader);

        List<Class<?>> ifacesList = new ArrayList<Class<?>>(1);
        ifacesList.add(ContentProvider.class);

        if (adapter instanceof RepoSource) {
            ifacesList.add(RepoSource.class);
        }
        if (adapter instanceof PackageSource) {
            ifacesList.add(PackageSource.class);
        }
        if (adapter instanceof DistributionSource) {
            ifacesList.add(DistributionSource.class);
        }

        Class<?>[] ifaces = ifacesList.toArray(new Class<?>[ifacesList.size()]);

        return (ContentProvider) Proxy.newProxyInstance(classLoader, ifaces, handler);
    }

    /**
     * Creates a provider instance that will service the give {@link ContentSource}.
     *
     * @param contentSource the source that the adapter will connect to
     * @return an adapter instance; will be <code>null</code> if failed to create adapter
     */
    private ContentProvider instantiateAdapter(ContentSource contentSource) {
        ContentProvider adapter = null;
        String apiClassName = "?";
        String pluginName = "?";

        try {
            ContentSourceType type = contentSource.getContentSourceType();
            apiClassName = type.getContentSourceApiClass();
            pluginName = this.pluginManager.getMetadataManager()
                .getPluginNameFromContentSourceType(type);

            ServerPluginEnvironment pluginEnv = this.pluginManager.getPluginEnvironment(pluginName);
            ClassLoader pluginClassloader = pluginEnv.getPluginClassLoader();

            ClassLoader startingClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(pluginClassloader);

                Class<?> apiClass = Class.forName(apiClassName, true, pluginClassloader);

                if (ContentProvider.class.isAssignableFrom(apiClass)) {
                    adapter = (ContentProvider) apiClass.newInstance();
                } else {
                    log.warn("The API class [" + apiClassName + "] does not implement ["
                        + ContentProvider.class.getName() + "] in plugin [" + pluginName + "]");
                }
            } finally {
                Thread.currentThread().setContextClassLoader(startingClassLoader);
            }
        }
        catch (Throwable t) {
            log.warn(
                "Failed to create the API class [" + apiClassName + "] for plugin [" + pluginName +
                    "]", t);
        }

        if (adapter != null) {
            synchronized (this.adapters) {
                this.adapters.put(contentSource, adapter);
            }
        }

        return adapter;
    }

    /**
     * Translates the domain representation of a list of packages into DTOs used in the plugin APIs.
     * During the translation the two collections (allDetails and keyPVCSMap) will be populated with
     * different views into the data.
     *
     * @param existingPVCS list of packages in the form of the wrapper object linking them to
     *                     the content source
     * @param allDetails   set of all translated package DTOs
     * @param keyPVCSMap   mapping of package version key to package domain object
     */
    private void translateDomainToDto(List<PackageVersionContentSource> existingPVCS,
                                      Set<ContentProviderPackageDetails> allDetails,
                                      Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> keyPVCSMap) {

        for (PackageVersionContentSource pvcs : existingPVCS) {
            PackageVersion pv = pvcs.getPackageVersionContentSourcePK().getPackageVersion();
            Package p = pv.getGeneralPackage();
            ResourceType rt = p.getPackageType().getResourceType();

            ContentProviderPackageDetailsKey key;
            key = new ContentProviderPackageDetailsKey(p.getName(), pv.getVersion(),
                p.getPackageType()
                    .getName(), pv.getArchitecture().getName(), rt.getName(), rt.getPlugin());

            ContentProviderPackageDetails details = new ContentProviderPackageDetails(key);
            details.setClassification(pv.getGeneralPackage().getClassification());
            details.setDisplayName(pv.getDisplayName());
            details.setDisplayVersion(pv.getDisplayVersion());
            details.setExtraProperties(pv.getExtraProperties());
            details.setFileCreatedDate(pv.getFileCreatedDate());
            details.setFileName(pv.getFileName());
            details.setFileSize(pv.getFileSize());
            details.setLicenseName(pv.getLicenseName());
            details.setLicenseVersion(pv.getLicenseVersion());
            details.setLocation(pvcs.getLocation());
            details.setLongDescription(pv.getLongDescription());
            details.setMD5(pv.getMD5());
            details.setMetadata(pv.getMetadata());
            details.setSHA256(pv.getSHA256());
            details.setShortDescription(pv.getShortDescription());

            allDetails.add(details);
            keyPVCSMap.put(key, pvcs);
        }
    }

    private void translateDomainToDto(List<Distribution> dists,
                                      List<DistributionDetails> distDetails) {
        DistributionManagerLocal distManager = LookupUtil.getDistributionManagerLocal();

        for (Distribution d : dists) {
            DistributionDetails detail = new DistributionDetails(d.getLabel(),
                d.getDistributionType().getName());
            detail.setLabel(d.getLabel());
            detail.setDistributionPath(d.getBasePath());
            detail.setDescription(d.getDistributionType().getDescription());
            List<DistributionFile> files = distManager.getDistributionFilesByDistId(d.getId());
            for (DistributionFile f : files) {
                DistributionFileDetails dfd =
                    new DistributionFileDetails(f.getRelativeFilename(), f.getLastModified(),
                        f.getMd5sum());
                detail.addFile(dfd);
            }
            distDetails.add(detail);
        }
    }

    /**
     * This will handle invocations to an object ensuring that the call is isolated to within the
     * appropriate classloader.
     */
    private class IsolatedInvocationHandler implements InvocationHandler {
        private final Object instance;
        private final ClassLoader classLoader;

        public IsolatedInvocationHandler(Object obj, ClassLoader cl) {
            instance = obj;
            classLoader = cl;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ClassLoader startingClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                return method.invoke(instance, args);
            } finally {
                Thread.currentThread().setContextClassLoader(startingClassLoader);
            }
        }
    }
}
