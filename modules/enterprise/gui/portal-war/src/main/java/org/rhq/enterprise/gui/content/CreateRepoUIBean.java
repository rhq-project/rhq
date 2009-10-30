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

import javax.faces.application.FacesMessage;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.RepoException;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class CreateRepoUIBean {
    private Repo newRepo = new Repo();

    public Repo getRepo() {
        return newRepo;
    }

    public void setRepo(Repo newRepo) {
        this.newRepo = newRepo;
    }

    public String save() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

        try {
            Repo created = manager.createRepo(subject, newRepo);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Saved [" + created.getName()
                + "] with the ID of [" + created.getId() + "]");
        } catch (RepoException ce) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error: " + ce.getMessage());
            return "failed";
        }

        newRepo = new Repo();
        return "save";
    }

    public String cancel() {
        newRepo = new Repo();
        return "cancel";
    }
}