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
package org.rhq.enterprise.server.registration;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class representing the repo to release relationship.
 * Ideally This should be persisted in the database at the sync time 
 * from hosted and should do a lookup in the db to extract compatible repos.
 * 
 * Since we're still unsure if we want go the agentless route,
 * Just using static mappings for now. This will be eventually replaced
 * by persisting these mapping in the db.
 *  
 * @author pkilambi
 *
 */
public class ReleaseRepoMapping {
    private final Log log = LogFactory.getLog(ReleaseRepoMapping.class.getName());
    private String release;
    private String arch;
    private String version;

    public ReleaseRepoMapping(String release, String version, String arch) {
        this.release = release;
        this.arch = arch;
        this.version = version; //ignored for now, used for z-stream repos in future
    }

    public String getCompatibleRepo() {
        HashMap<String, String> repomap = generateRepoMap();
        log.debug("mapping generated " + repomap);
        return repomap.get(this.release + "." + this.arch);
    }

    /**
     * Static mappings for base repos
     * @return
     */
    private HashMap<String, String> generateRepoMap() {
        HashMap<String, String> hm = new HashMap<String, String>();

        hm.put("5Server.arch", "rhel-arch-server-5");
        hm.put("5Client.arch", "rhel-arch-client-5");

        String[] arches = { "i386", "x86_64", "ia64", "s390x", "ppc" };
        HashMap<String, String> newhm = new HashMap();
        for (String k : hm.keySet()) {
            for (String arch : arches) {
                newhm.put(k.replace("arch", arch), hm.get(k).replace("arch", arch));
            }
        }
        return newhm;
    }

}
