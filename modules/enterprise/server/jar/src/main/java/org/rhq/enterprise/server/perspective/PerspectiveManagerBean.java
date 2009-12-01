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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Map of sessionId to cached menu entry.  The cached menu is re-used for the same sessionId.
    // This should more appropriately use Subject as the key but since Subject equality is
    // based on username it's not quite appropriate.
    // The cache is cleaned anytime there is a new entry.
    static private Map<Integer, CacheEntry> cache = new HashMap<Integer, CacheEntry>();
    private List<MenuItem> coreMenu;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getCoreMenu(org.rhq.core.domain.auth.Subject)
     */
    @Override
    public synchronized List<MenuItem> getCoreMenu(Subject subject) throws PerspectiveException {
        Integer sessionId = subject.getSessionId();
        CacheEntry cacheEntry = cache.get(sessionId);

        if (null == cacheEntry) {
            // take this opportunity to clean the cache
            cleanCache();

            cacheEntry = new CacheEntry();
            cache.put(sessionId, cacheEntry);
        }

        List<MenuItem> coreMenu = cacheEntry.getCoreMenu();

        if (null == coreMenu) {
            try {
                coreMenu = PerspectiveManagerHelper.getPluginMetadataManager().getCoreMenu();

                // TODO : make safe copy
                cacheEntry.setCoreMenu(coreMenu);
            } catch (Exception e) {
                throw new PerspectiveException("Failed to get Core Menu.", e);
            }
        }

        // TODO: Apply Activators here

        return coreMenu;
    }

    private void cleanCache() {
        Subject subject;

        for (Integer sessionId : cache.keySet()) {
            try {
                subject = subjectManager.getSessionSubject(sessionId);
                if (null == subject) {
                    log.debug("Removing perspective cache entry for session " + sessionId);
                    cache.remove(sessionId);
                }
            } catch (Exception e) {
                log.debug("Removing perspective cache entry for session " + sessionId);
                cache.remove(sessionId);
            }
        }
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

    private static class CacheEntry {
        private List<MenuItem> coreMenu = null;

        public CacheEntry() {
        }

        /**
         * @return the coreMenu
         */
        public List<MenuItem> getCoreMenu() {
            return coreMenu;
        }

        /**
         * @param coreMenu the coreMenu to set
         */
        public void setCoreMenu(List<MenuItem> coreMenu) {
            this.coreMenu = coreMenu;
        }
    }

}