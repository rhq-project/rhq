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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fedorahosted.cobbler.CobblerConnection;
import org.fedorahosted.cobbler.Finder;
import org.fedorahosted.cobbler.ObjectType;
import org.fedorahosted.cobbler.autogen.Distro;
import org.fedorahosted.cobbler.autogen.Profile;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;

public class CobblerServerPluginComponent implements ServerPluginComponent, ControlFacet {
    private static Log log = LogFactory.getLog(CobblerServerPluginComponent.class);

    private ServerPluginContext context;

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

        if (name.equals("getCobblerDistros")) {
            Configuration results = controlResults.getComplexResults();
            PropertyList list = new PropertyList("distros");
            results.put(list);

            PropertyMap map = new PropertyMap("distro");
            list.add(map);
            map.put(new PropertySimple("distroName", "distroUrl"));
        } else {
            controlResults.setError("Unknown operation name: " + name);
        }

        return controlResults;
    }

    public void synchronizeContent(ScheduledJobInvocationContext invocation) throws Exception {
        log.info("Synchronizing content to the local Cobbler server: " + this);
        this.context.getPluginConfiguration().getSimpleValue("", "");

        CobblerConnection conn = new CobblerConnection("http://172.31.0.227", "testing", "testing");
        Finder finder = Finder.getInstance();
        Profile profile = (Profile) finder.findItemByName(conn, ObjectType.PROFILE, "mazz-profile");
        Distro distro = (Distro) finder.findItemByName(conn, ObjectType.DISTRO, "mazz-distro");

        if (distro != null) {
            log.info("REMOVING PROFILE: " + profile);
            profile.remove();
            log.info("REMOVING DISTRO: " + distro);
            distro.remove();
        } else {
            distro = new Distro(conn);
            distro.setName("mazz-distro");
            distro
                .setKernel("http://download.fedora.redhat.com/pub/fedora/linux/releases/12/Fedora/x86_64/os/isolinux/vmlinuz");
            distro
                .setInitrd("http://download.fedora.redhat.com/pub/fedora/linux/releases/12/Fedora/x86_64/os/isolinux/initrd.img");
            Map<String, String> ksmeta = new HashMap<String, String>();
            ksmeta.put("tree", "http://download.fedora.redhat.com/pub/fedora/linux/releases/12/Fedora/x86_64/os/");
            distro.setKsMeta(ksmeta);
            log.info("CREATING DISTRO: " + distro);
            distro.commit();

            profile = new Profile(conn);
            profile.setDistro(distro.getName());
            profile.setKickstart("http://localhost:7080/content/kickstart/mazz.ks");
            profile.setName("mazz-profile");
            log.info("CREATING PROFILE: " + profile);
            profile.commit();
        }
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
}
