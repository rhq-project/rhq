/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugins.cobbler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fedorahosted.cobbler.CobblerConnection;
import org.fedorahosted.cobbler.CobblerObject;
import org.fedorahosted.cobbler.Finder;
import org.fedorahosted.cobbler.ObjectType;
import org.fedorahosted.cobbler.autogen.Distro;
import org.fedorahosted.cobbler.autogen.Profile;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.util.LookupUtil;

public class CobblerServerPluginComponent implements ServerPluginComponent, ControlFacet {
    private static Log log = LogFactory.getLog(CobblerServerPluginComponent.class);

    /**
     * This string is placed at the start of all distro comments created in Cobbler.
     * When we see this marker at the start of a distro comment, we know its a distro we created.
     */
    private static final String COMMENT_MARKER = "[rhq]";

    private ServerPluginContext context;

    /**
     * Used to avoid having an operation running at the same time as a sync.
     */
    private boolean syncInProgress;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        log.info("initialized: " + this);
    }

    public void start() {
        log.info("started: " + this);
    }

    public void stop() {
        log.info("stopped: " + this);
    }

    public void shutdown() {
        log.info("shutdown: " + this);
    }

    public ControlResults invoke(String name, Configuration parameters) {
        ControlResults controlResults = new ControlResults();

        try {
            if (name.equals("getCobblerDistros")) {
                String searchRegex = parameters.getSimpleValue("searchRegex", null);
                Pattern pattern = null;
                if (searchRegex != null) {
                    pattern = Pattern.compile(searchRegex);
                }

                Configuration results = controlResults.getComplexResults();
                PropertyList list = new PropertyList("distros");
                results.put(list);

                Collection<Distro> distros = getAllCobblerDistros().values();
                for (Distro d : distros) {
                    if (pattern == null || pattern.matcher(d.getName()).matches()) {
                        PropertyMap map = new PropertyMap("distro");
                        map.put(new PropertySimple("name", d.getName()));
                        map.put(new PropertySimple("breed", d.getBreed()));
                        map.put(new PropertySimple("osversion", d.getOsVersion()));
                        map.put(new PropertySimple("arch", d.getArch()));
                        map.put(new PropertySimple("initrd", d.getInitrd()));
                        map.put(new PropertySimple("kernel", d.getKernel()));
                        list.add(map);
                    }
                }
            } else if (name.equals("getCobblerProfiles")) {
                String searchRegex = parameters.getSimpleValue("searchRegex", null);
                Pattern pattern = null;
                if (searchRegex != null) {
                    pattern = Pattern.compile(searchRegex);
                }

                Configuration results = controlResults.getComplexResults();
                PropertyList list = new PropertyList("profiles");
                results.put(list);

                List<Profile> profiles = getAllCobblerProfiles();
                for (Profile p : profiles) {
                    if (pattern == null || pattern.matcher(p.getName()).matches()) {
                        PropertyMap map = new PropertyMap("profile");
                        map.put(new PropertySimple("name", p.getName()));
                        map.put(new PropertySimple("distro", p.getDistro()));
                        map.put(new PropertySimple("kickstart", p.getKickstart()));
                        list.add(map);
                    }
                }
            } else if (name.equals("removeCobblerDistros")) {
                String searchRegex = parameters.getSimpleValue("searchRegex", null);
                Pattern pattern = null;
                if (searchRegex != null) {
                    pattern = Pattern.compile(searchRegex);
                }

                if (!this.syncInProgress) {
                    Collection<Distro> distros = getAllCobblerDistros().values();
                    for (Distro d : distros) {
                        if (pattern == null || pattern.matcher(d.getName()).matches()) {
                            if (d.getComment().startsWith(COMMENT_MARKER)) {
                                d.remove();
                            }
                        }
                    }
                } else {
                    controlResults.setError("A synchronize is currently in progress - please wait for it to finish");
                }
            } else {
                controlResults.setError("Unknown operation name: " + name);
            }
        } catch (Exception e) {
            controlResults.setError(e);
        }

        return controlResults;
    }

    public void synchronizeContent(ScheduledJobInvocationContext invocation) throws Exception {
        log.info("Synchronizing content to the local Cobbler server: " + this);

        try {
            this.syncInProgress = true;

            Server server = LookupUtil.getServerManager().getServer();
            String rootUrl = "http://" + server.getAddress() + ":" + server.getPort() + "/content/";

            Map<String, Distro> cobblerDistros = getAllCobblerDistros(); // Cobbler distros
            Map<Repo, Map<String, Distribution>> reposDistributions = getAllDistributions(); // RHQ distros

            CobblerConnection conn = getConnection();

            for (Map.Entry<Repo, Map<String, Distribution>> repoEntry : reposDistributions.entrySet()) {
                Repo repo = repoEntry.getKey();
                String repoName = repo.getName();

                for (Distribution distribution : repoEntry.getValue().values()) {

                    Distro existingCobblerDistro = cobblerDistros.get(distribution.getLabel());
                    Distro desiredCobblerDistro = instantiateCobblerDistro(conn, distribution, repoName, rootUrl);

                    if (existingCobblerDistro != null) {
                        // cobbler already has a distro with the name we are looking for.
                        // let's make sure its data is the same, otherwise, we need to upgrade it.
                        // but first, we need to take it out of our map, because whatever is left in this map will be removed from Cobbler later.
                        cobblerDistros.remove(existingCobblerDistro.getName());

                        if (!compareCobblerDistros(existingCobblerDistro, desiredCobblerDistro)) {
                            // the one Cobbler has is old and needs to be updated with the latest data.
                            updateCobblerDistro(existingCobblerDistro, desiredCobblerDistro);
                            existingCobblerDistro.commit();
                            log.info("Updated existing Cobbler distro [" + distribution.getLabel() + "]");
                        } else {
                            log.debug("Cobbler already has distro [" + distribution.getLabel() + "]; keeping it");
                        }
                    } else {
                        desiredCobblerDistro.commit();
                        log.info("Added new distro to Cobbler: [" + distribution.getLabel() + "]");
                    }
                }
            }

            // now remove those RHQ distros that we no longer have, only remove RHQ distros though
            for (Distro doomed : cobblerDistros.values()) {
                if (doomed.getComment().startsWith(COMMENT_MARKER)) {
                    doomed.remove();
                    log.info("Removed obsolete distro from Cobbler: [" + doomed.getName() + "]");
                }
            }
        } catch (Throwable t) {
            log.error("Failed to synchronize distributions to Cobbler server", t);
        } finally {
            this.syncInProgress = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void updateCobblerDistro(Distro existingCobblerDistro, Distro desiredCobblerDistro) {
        existingCobblerDistro.setName(desiredCobblerDistro.getName());
        existingCobblerDistro.setComment(desiredCobblerDistro.getComment());
        existingCobblerDistro.setKernel(desiredCobblerDistro.getKernel());
        existingCobblerDistro.setInitrd(desiredCobblerDistro.getInitrd());

        Map<String, String> ksMetaDesired = desiredCobblerDistro.getKsMeta();
        if (ksMetaDesired != null) {
            Map<String, String> ksMetaExisting = existingCobblerDistro.getKsMeta();
            if (ksMetaExisting == null) {
                ksMetaExisting = new HashMap<String, String>(ksMetaDesired.size());
                existingCobblerDistro.setKsMeta(ksMetaExisting);
            }
            ksMetaExisting.clear();
            ksMetaExisting.putAll(ksMetaDesired);
        } else {
            existingCobblerDistro.setKsMeta(null);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean compareCobblerDistros(Distro cobblerDistro1, Distro cobblerDistro2) {
        if (!compareObjects(cobblerDistro1.getName(), cobblerDistro2.getName())) {
            return false;
        }
        if (!compareObjects(cobblerDistro1.getComment(), cobblerDistro2.getComment())) {
            return false;
        }
        if (!compareObjects(cobblerDistro1.getKernel(), cobblerDistro2.getKernel())) {
            return false;
        }
        if (!compareObjects(cobblerDistro1.getInitrd(), cobblerDistro2.getInitrd())) {
            return false;
        }

        Map<String, String> ksMeta1 = cobblerDistro1.getKsMeta();
        Map<String, String> ksMeta2 = cobblerDistro2.getKsMeta();
        String tree1 = null;
        String tree2 = null;
        if (ksMeta1 != null) {
            tree1 = ksMeta1.get("tree");
        }
        if (ksMeta2 != null) {
            tree2 = ksMeta2.get("tree");
        }
        if (!compareObjects(tree1, tree2)) {
            return false;
        }

        return true;
    }

    /**
     * Given a RHQ distribution domaon object, this instantiates a Cobbler Distro object
     * that can allow Cobbler to access the distribution content.
     * NOTE: this does NOT create the Distro on the Cobbler server, this only instantiates
     * a Distro class object and returns it.
     * 
     * @param conn a connection to the Cobbler server
     * @param distribution the RHQ distribution data
     * @param repoName the name of the repository that the distribution is associated with
     * @param rootUrl the root URL where the distribution content can be found
     * @return an instance of a Cobbler Distro object
     */
    private Distro instantiateCobblerDistro(CobblerConnection conn, Distribution distribution, String repoName,
        String rootUrl) {

        Distro cobblerDistro;
        String distroRootUrl = rootUrl + repoName + "/distributions/" + distribution.getLabel();
        String kernel = distroRootUrl + "/images/pxeboot/vmlinuz"; // TODO what about other arches
        String initrd = distroRootUrl + "/images/pxeboot/initrd.img"; // TODO what about other arches
        String ksTree = distroRootUrl;

        cobblerDistro = new Distro(conn);
        cobblerDistro.setName(distribution.getLabel());
        cobblerDistro.setComment(COMMENT_MARKER + " " + distribution.getLabel());
        cobblerDistro.setKernel(kernel);
        cobblerDistro.setInitrd(initrd);
        Map<String, String> ksmeta = new HashMap<String, String>();
        ksmeta.put("tree", ksTree);
        cobblerDistro.setKsMeta(ksmeta);

        return cobblerDistro;
    }

    @Override
    public String toString() {
        if (this.context == null) {
            return "<no context>";
        }

        StringBuilder str = new StringBuilder();
        str.append("plugin-key=").append(this.context.getPluginEnvironment().getPluginKey()).append(",");
        str.append("plugin-url=").append(this.context.getPluginEnvironment().getPluginUrl()).append(",");
        str.append("plugin-config=[").append(getPluginConfigurationString()).append(']'); // do not append ,
        return str.toString();
    }

    private Map<String, Distro> getAllCobblerDistros() {
        Map<String, Distro> distros = new HashMap<String, Distro>();

        CobblerConnection conn = getConnection();
        Finder finder = Finder.getInstance();
        List<? extends CobblerObject> objs = finder.listItems(conn, ObjectType.DISTRO);
        for (CobblerObject obj : objs) {
            if (obj instanceof Distro) {
                distros.put(((Distro) obj).getName(), (Distro) obj);
            } else {
                log.error("Instead of a distro, Cobbler returned an object of type [" + obj.getClass() + "]: " + obj);
            }
        }
        return distros;
    }

    private List<Profile> getAllCobblerProfiles() {
        List<Profile> profiles = new ArrayList<Profile>();

        CobblerConnection conn = getConnection();
        Finder finder = Finder.getInstance();
        List<? extends CobblerObject> objs = finder.listItems(conn, ObjectType.PROFILE);
        for (CobblerObject obj : objs) {
            if (obj instanceof Profile) {
                profiles.add((Profile) obj);
            } else {
                log.error("Instead of a profile, Cobbler returned an object of type [" + obj.getClass() + "]: " + obj);
            }
        }
        return profiles;
    }

    private Map<Repo, Map<String, Distribution>> getAllDistributions() {
        final int repoPageSize = 10;
        final int distroPageSize = 10;

        Map<Repo, Map<String, Distribution>> reposDistros = new HashMap<Repo, Map<String, Distribution>>();

        RepoManagerLocal repoMgr = LookupUtil.getRepoManagerLocal();
        PageControl repoPC = new PageControl(0, repoPageSize);
        int totalReposProcessed = 0;
        while (true) {
            PageList<Repo> repoPage = repoMgr.findRepos(LookupUtil.getSubjectManager().getOverlord(), repoPC);

            if (repoPage.size() <= 0) {
                break;
            }

            for (Repo repoPageItem : repoPage) {
                if (!repoPageItem.isCandidate()) {
                    Map<String, Distribution> distrosMap = reposDistros.get(repoPageItem);
                    if (distrosMap == null) {
                        distrosMap = new HashMap<String, Distribution>();
                        reposDistros.put(repoPageItem, distrosMap);
                    }

                    PageControl distroPC = new PageControl(0, distroPageSize);
                    int totalDistrosProcessed = 0;
                    while (true) {
                        PageList<Distribution> distroPage = repoMgr.findAssociatedDistributions(LookupUtil
                            .getSubjectManager().getOverlord(), repoPageItem.getId(), distroPC);
                        if (distroPage.size() <= 0) {
                            break;
                        }
                        for (Distribution distroPageItem : distroPage) {
                            distrosMap.put(distroPageItem.getLabel(), distroPageItem);
                        }
                        totalDistrosProcessed += distroPage.size();
                        if (totalDistrosProcessed >= distroPage.getTotalSize()) {
                            break; // the previous page that was processed was the last one
                        }

                        distroPC.setPageNumber(distroPC.getPageNumber() + 1); // advance to the next distro page
                    }
                }
            }

            totalReposProcessed += repoPage.size();
            if (totalReposProcessed >= repoPage.getTotalSize()) {
                break; // the previous page that was processed was the last one
            }

            repoPC.setPageNumber(repoPC.getPageNumber() + 1); // advance to the repo page
        }

        return reposDistros;
    }

    private CobblerConnection getConnection() {
        Configuration pc = this.context.getPluginConfiguration();
        String url = pc.getSimpleValue("url", "http://127.0.0.1");
        String username = pc.getSimpleValue("username", "");
        String password = pc.getSimpleValue("password", "");

        if (log.isDebugEnabled()) {
            log.debug("Connecting to Cobbler at [" + url + "] as user [" + username + "]");
        }

        CobblerConnection conn = new CobblerConnection(url, username, password);
        return conn;
    }

    private String getPluginConfigurationString() {
        String results = "";
        Configuration config = this.context.getPluginConfiguration();
        for (PropertySimple prop : config.getSimpleProperties().values()) {
            if (results.length() > 0) {
                results += ", ";
            }
            results = results + prop.getName() + "=" + prop.getStringValue();
        }
        return results;
    }

    private boolean compareObjects(Object o1, Object o2) {
        // this ensures we don't throw an NPE if either is null
        if (o1 != null) {
            return o1.equals(o2);
        } else {
            return o2 == null;
        }
    }
}
