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
package org.rhq.enterprise.server.perspective;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;

@Stateless
// @WebService(endpointInterface = "org.rhq.enterprise.server.perspective.PerspectiveManagerRemote")
public class PerspectiveManagerBean implements PerspectiveManagerLocal, PerspectiveManagerRemote {

    private final Log log = LogFactory.getLog(PerspectiveManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getCoreMenu(org.rhq.core.domain.auth.Subject)
     */
    @Override
    public List<MenuItem> getCoreMenu(Subject subject) throws PerspectiveException {
        List<MenuItem> coreMenu = null;

        try {
            coreMenu = PerspectiveManagerHelper.getPluginMetadataManager().getCoreMenu();
        } catch (Exception e) {
            throw new PerspectiveException("Failed to get Core Menu.", e);
        }

        // TODO: Apply Activators here

        // TODO: Cache session:menu map here

        // TODO: remove this debug code
        // printMenu(coreMenu, "");

        return coreMenu;
    }

    // TODO: remove this debug code
    @SuppressWarnings("unused")
    private void printMenu(List<MenuItem> menu, String indent) {
        if (null == menu)
            return;

        for (MenuItem menuItem : menu) {
            System.out.println(indent + menuItem.getItem().getName());
            printMenu(menuItem.getChildren(), indent + "..");
        }
    }

}