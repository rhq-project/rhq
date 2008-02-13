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
package org.rhq.enterprise.server.plugin.content;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.server.plugin.content.ContentSourceAdapter;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.clientapi.server.plugin.content.metadata.ContentSourcePluginMetadataManager;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceSyncStatus;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Responsible for managing {@link ContentSourceAdapter adapters}.
 *
 * @author John Mazzitelli
 */
public class ContentSourceAdapterManager {
    private static final Log log = LogFactory.getLog(ContentSourceAdapterManager.class);

    private ContentSourcePluginContainerConfiguration configuration;
    private ContentSourcePluginManager pluginManager;
    private Map<ContentSource, ContentSourceAdapter> adapters;

    // This is used as a monitor lock to the synchronizeContentSource method;
    // it helps us avoid two content sources getting synchronized at the same time.
    private Object synchronizeContentSourceLock = new Object();

    /**
     * Asks that the adapter responsible for the given content source return a stream to the package bits for the
     * package at the given location.
     *
     * @param  contentSourceId the adapter for this content source will be used to stream the bits
     * @param  location        where the adapter can find the package bits on the content source
     *
     * @return the stream to the package bits
     *
     * @throws Exception if the adapter failed to load the bits
     */
    public InputStream loadPackageBits(int contentSourceId, String location) throws Exception {
        ContentSourceAdapter adapter = getIsolatedContentSourceAdapter(contentSourceId);
        InputStream inputStream = adapter.getInputStream(location);

        if (inputStream == null) {
            throw new Exception("Adapter for content source [" + contentSourceId
                + "] failed to give us a stream to the package at location [" + location + "]");
        }

        return inputStream;
    }

    /**
     * Asks the adapter responsible for the given content source to synchronize with the remote repository. This will
     * not attempt to load any package bits - it only synchronizes the package version information. Note that if a
     * synchronization is already currently underway, this method will not do anything and will return <code>
     * false</code> (in effect, will let the currently running synchronization continue; we try not to step on it). If
     * this method do actually do a sync, <code>true</code> will be returned.
     *
     * @param  contentSourceId
     *
     * @return <code>true</code> if the synchronization completed; <code>false</code> if there was already a
     *         synchronization happening and this method aborted
     *
     * @throws Exception
     */
    public boolean sychronizeContentSource(int contentSourceId) throws Exception {
        ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
        ContentSourceAdapter adapter = getIsolatedContentSourceAdapter(contentSourceId);
        ContentSourceSyncResults results = null;
        SubjectManagerLocal subjMgr = LookupUtil.getSubjectManager();
        StringBuilder progress = new StringBuilder(); // append to this as we go along, building a status report
        long start;

        try {
            ContentSource contentSource = manager.getContentSource(subjMgr.getOverlord(), contentSourceId);
            if (contentSource == null) {
                throw new Exception("Cannot sync a non-existing content source [" + contentSourceId + "]");
            }

            // This should not take very long so it should be OK to block other callers.
            // We are avoiding the problem that would occur if we try to synchronize the same content source
            // at the same time. We could do it more cleverly by synchronizing on
            // a per content source basis, but I don't see a need right now to make this more complicated.
            // We can come back and revisit if we need more fine-grained locking.
            synchronized (synchronizeContentSourceLock) {
                progress.append(new Date()).append(": ");
                progress.append("Start synchronization of content source [" + contentSource.getName() + "]");
                progress.append('\n');
                progress.append(new Date()).append(": ");
                progress.append("Getting currently known list of content source packages...");
                results = new ContentSourceSyncResults(contentSource);
                results.setResults(progress.toString());
                results = manager.persistContentSourceSyncResults(results);
            }

            if (results == null) {
                // note that it technically is still possible to have concurrent syncs - if two threads running
                // in two different servers (i.e. different VMs) both try to sync the same content source and
                // both enter the persistContentSourceSyncResults method at the same time, you'll get two
                // inprogress rows - this is so rare as to not care.  Even if it does happen, it may still work, or
                // one sync will get an error and rollback its tx and no harm will be done.
                log.info("Content source [" + contentSource.getName()
                    + "] is already currently being synchronized, this sync request will be ignored");
                return false;
            }

            PackageSyncReport report; // the plugin will fill this in for us
            List<PackageVersionContentSource> existingPVCS; // what we already know about this content source

            Set<ContentSourcePackageDetails> allDetails; // what we tell the plugin about what we know
            Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> keyPVCSMap;

            start = System.currentTimeMillis();
            report = new PackageSyncReport();
            PageControl pc = PageControl.getUnlimitedInstance(); // gulp - I assume we can fit all package versions in mem
            existingPVCS = manager
                .getPackageVersionsFromContentSource(subjMgr.getOverlord(), contentSource.getId(), pc);
            int existingCount = existingPVCS.size();
            keyPVCSMap = new HashMap<ContentSourcePackageDetailsKey, PackageVersionContentSource>(existingCount);
            allDetails = new HashSet<ContentSourcePackageDetails>(existingCount);

            for (PackageVersionContentSource pvcs : existingPVCS) {
                PackageVersion pv = pvcs.getPackageVersionContentSourcePK().getPackageVersion();
                Package p = pv.getGeneralPackage();
                ResourceType rt = p.getPackageType().getResourceType();

                ContentSourcePackageDetailsKey key;
                key = new ContentSourcePackageDetailsKey(p.getName(), pv.getVersion(), p.getPackageType().getName(), pv
                    .getArchitecture().getName(), rt.getName(), rt.getPlugin());

                ContentSourcePackageDetails details = new ContentSourcePackageDetails(key);
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
                details.setSHA265(pv.getSHA256());
                details.setShortDescription(pv.getShortDescription());

                allDetails.add(details);
                keyPVCSMap.put(key, pvcs);
            }

            log.info("sychronizeContentSource: [" + contentSource.getName() + "]: loaded existing list of size=["
                + existingCount + "] (" + (System.currentTimeMillis() - start) + ")ms");

            progress.append(existingCount).append('\n');
            progress.append(new Date()).append(": ");
            progress.append("Asking content source to update the list of packages...");
            results.setResults(progress.toString());
            results = manager.mergeContentSourceSyncResults(results);
            start = System.currentTimeMillis();

            adapter.synchronizePackages(report, allDetails);

            log.info("sychronizeContentSource: [" + contentSource.getName() + "]: got sync report from adapter=["
                + report + "] (" + (System.currentTimeMillis() - start) + ")ms");

            progress.append("new=").append(report.getNewPackages().size());
            progress.append(", updated=").append(report.getUpdatedPackages().size());
            progress.append(", deleted=").append(report.getDeletedPackages().size());
            progress.append('\n');
            progress.append(new Date()).append(": ");
            progress.append("FULL SUMMARY FOLLOWS:");
            progress.append('\n');
            progress.append(report.getSummary());
            progress.append('\n');
            progress.append(new Date()).append(": ");
            progress.append("Merging the updated list of packages to database");
            progress.append('\n');
            results.setResults(progress.toString());
            results = manager.mergeContentSourceSyncResults(results);
            start = System.currentTimeMillis();

            results = manager.mergeContentSourceSyncReport(contentSource, report, keyPVCSMap, results);

            // let's reset our progress string to the full results including the text added by the merge
            progress.setLength(0);
            progress.append(results.getResults());

            log.info("sychronizeContentSource: [" + contentSource.getName() + "]: report has been merged ("
                + (System.currentTimeMillis() - start) + ")ms");

            progress.append(new Date()).append(": ").append("DONE.");
            results.setResults(progress.toString());
            results.setStatus(ContentSourceSyncStatus.SUCCESS);
            results = manager.mergeContentSourceSyncResults(results);
        } catch (Throwable t) {
            if (results != null) {
                // try to reload the results in case it was updated by the SLSB before the exception happened
                ContentSourceSyncResults reloadedResults = manager.getContentSourceSyncResults(results.getId());
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
                results.setEndTime(new Long(System.currentTimeMillis()));
                manager.mergeContentSourceSyncResults(results);
            }
        }

        return true;
    }

    /**
     * Tests the connection to the content source that has the given ID.
     *
     * @param  contentSourceId
     *
     * @return <code>true</code> if there is an adapter that can successfully connect to the given content source <code>
     *         false</code> if there is an adapter but it cannot successfully connect
     *
     * @throws Exception if failed to get an adapter to attempt the connection
     */
    public boolean testConnection(int contentSourceId) throws Exception {
        ContentSourceAdapter adapter = getIsolatedContentSourceAdapter(contentSourceId);

        try {
            adapter.testConnection();
        } catch (Throwable t) {
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
     * Call this method when a new content source is added to the system during runtime. This can also be used to
     * restart an adapter that was previously {@link #shutdownAdapter(ContentSource) shutdown}.
     *
     * <p>If there is already an adapter currently started for the given content source, this returns silently.</p>
     *
     * @param contentSource the new content source that was added
     */
    public void startAdapter(ContentSource contentSource) {
        synchronized (this.adapters) {
            if (this.adapters.containsKey(contentSource)) {
                return; // already exists, which means it was already initialized
            }
        }

        instantiateAdapter(contentSource);

        try {
            log.info("Initializing content source adapter for [" + contentSource + "] of type ["
                + contentSource.getContentSourceType() + "]");

            ContentSourceAdapter adapter = getIsolatedContentSourceAdapter(contentSource);
            adapter.initialize(contentSource.getConfiguration());
        } catch (Throwable t) {
            log.warn("Failed to initialize adapter for content source [" + contentSource.getName() + "]", t);
        }
    }

    /**
     * Call this method when a content source is removed from the system during runtime or if you just want to shutdown
     * the adapter for whatever reason. You can restart the adapter by calling {@link #startAdapter(ContentSource)}.
     *
     * <p>If there are no adapters currently started for the given content source, this returns silently.</p>
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

            ContentSourceAdapter adapter = getIsolatedContentSourceAdapter(contentSource);
            adapter.shutdown();
        } catch (Throwable t) {
            log.warn("Failed to shutdown adapter for content source [" + contentSource.getName() + "]", t);
        } finally {
            this.adapters.remove(contentSource);
        }
    }

    /**
     * Convienence method that simply {@link #shutdownAdapter(ContentSource) shuts down the adapter} and then
     * {@link #startAdapter(ContentSource) restarts it}. Call this when, for example, a content source's
     * {@link ContentSource#getConfiguration() configuration} has changed.
     *
     * @param contentSource the content source whose adapter is to be restarted
     */
    public void restartAdapter(ContentSource contentSource) {
        shutdownAdapter(contentSource);
        startAdapter(contentSource);
    }

    /**
     * Sets the internal configuration that can be used by this manager.
     *
     * <p>This is protected so only the plugin container and subclasses can use it.</p>
     *
     * @param config
     */
    protected void setConfiguration(ContentSourcePluginContainerConfiguration config) {
        this.configuration = config;
    }

    /**
     * Tells this manager to initialize itself which will initialize all the adapters.
     *
     * <p>This is protected so only the plugin container and subclasses can use it.</p>
     *
     * @param pluginManager the plugin manager this object can use to obtain information from (like classloaders)
     */
    protected void initialize(ContentSourcePluginManager pluginManager) {
        this.pluginManager = pluginManager;

        ContentSourceMetadataManagerLocal metadataManager = LookupUtil.getContentSourceMetadataManager();
        ContentSourceManagerLocal contentSourceManager = LookupUtil.getContentSourceManager();

        // Our plugin manager should have parsed all descriptors and have our types for us.
        // Let's register the types to make sure they are merged with the old existing types.
        ContentSourcePluginMetadataManager pluginMetadataManager = this.pluginManager.getMetadataManager();
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
                startAdapter(contentSource);
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
        HashMap<ContentSource, ContentSourceAdapter> adaptersCopy;

        // shutdown all adapters to give them a chance to clean up after themselves
        synchronized (this.adapters) {
            adaptersCopy = new HashMap<ContentSource, ContentSourceAdapter>(this.adapters);
        }

        for (ContentSource contentSource : adaptersCopy.keySet()) {
            shutdownAdapter(contentSource);
        }

        synchronized (this.adapters) {
            this.adapters.clear();
        }

        return;
    }

    /**
     * <p>This is protected so only the plugin container and subclasses can use it.</p>
     */
    protected void createInitialAdaptersMap() {
        this.adapters = new HashMap<ContentSource, ContentSourceAdapter>();
    }

    /**
     * Creates an adapter that will service the give {@link ContentSource}.
     *
     * @param  contentSource the source that the adapter will connect to
     *
     * @return an adapter instance; will be <code>null</code> if failed to create adapter
     */
    private ContentSourceAdapter instantiateAdapter(ContentSource contentSource) {
        ContentSourceAdapter adapter = null;
        String apiClassName = "?";
        String pluginName = "?";

        try {
            ContentSourceType type = contentSource.getContentSourceType();
            apiClassName = type.getContentSourceApiClass();
            pluginName = this.pluginManager.getMetadataManager().getPluginNameFromContentSourceType(type);

            ContentSourcePluginEnvironment pluginEnv = this.pluginManager.getPlugin(pluginName);
            ClassLoader pluginClassloader = pluginEnv.getClassLoader();

            ClassLoader startingClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(pluginClassloader);

                Class<?> apiClass = Class.forName(apiClassName, true, pluginClassloader);

                if (ContentSourceAdapter.class.isAssignableFrom(apiClass)) {
                    adapter = (ContentSourceAdapter) apiClass.newInstance();
                } else {
                    log.warn("The API class [" + apiClassName + "] does not implement ["
                        + ContentSourceAdapter.class.getName() + "] in plugin [" + pluginName + "]");
                }
            } finally {
                Thread.currentThread().setContextClassLoader(startingClassLoader);
            }
        } catch (Throwable t) {
            log.warn("Failed to create the API class [" + apiClassName + "] for plugin [" + pluginName + "]", t);
        }

        if (adapter != null) {
            synchronized (this.adapters) {
                this.adapters.put(contentSource, adapter);
            }
        }

        return adapter;
    }

    /**
     * Given a ID to a content source, this returns the adapter that is responsible for communicating with that content
     * source where that adaptor object will ensure invocations on it are isolated to its plugin classloader.
     *
     * @param  contentSourceId an ID to a {@link ContentSource}
     *
     * @return the adapter that is communicating with the content source, isolated to its classloader
     *
     * @throws RuntimeException if there is no content source with the given ID
     */
    private ContentSourceAdapter getIsolatedContentSourceAdapter(int contentSourceId) throws RuntimeException {
        synchronized (this.adapters) {
            for (ContentSource contentSource : this.adapters.keySet()) {
                if (contentSource.getId() == contentSourceId) {
                    return getIsolatedContentSourceAdapter(contentSource);
                }
            }
        }

        throw new RuntimeException("Content source ID [" + contentSourceId + "] doesn't exist; can't get adapter");
    }

    /**
     * This returns the adapter that is responsible for communicating with the given content source where that adaptor
     * object will ensure invocations on it are isolated to its plugin classloader.
     *
     * @param  contentSource the returned adapter communicates with this content source
     *
     * @return the adapter that is communicating with the content source, isolated to its classloader
     *
     * @throws RuntimeException if there is no content source adapter available
     */
    private ContentSourceAdapter getIsolatedContentSourceAdapter(ContentSource contentSource) throws RuntimeException {
        ContentSourceAdapter adapter;

        synchronized (this.adapters) {
            adapter = this.adapters.get(contentSource);
        }

        if (adapter == null) {
            throw new RuntimeException("There is no adapter for content source [" + adapter + "]");
        }

        ContentSourcePluginEnvironment env = this.pluginManager.getPlugin(contentSource.getContentSourceType());
        if (env == null) {
            throw new RuntimeException("There is no plugin env. for content source [" + contentSource + "]");
        }

        ClassLoader classLoader = env.getClassLoader();
        IsolatedInvocationHandler handler = new IsolatedInvocationHandler(adapter, classLoader);
        Class<?>[] ifaces = new Class<?>[] { ContentSourceAdapter.class };

        return (ContentSourceAdapter) Proxy.newProxyInstance(classLoader, ifaces, handler);
    }

    /**
     * This will handle invocations to an object ensuring that the call is isolated to within the appropriate
     * classloader.
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