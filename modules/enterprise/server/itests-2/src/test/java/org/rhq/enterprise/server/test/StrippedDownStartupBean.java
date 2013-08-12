/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.server.test;

import static org.rhq.enterprise.server.cloud.StorageNodeManagerLocal.STORAGE_NODE_GROUP_NAME;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.StartupBean;
import org.rhq.enterprise.server.naming.NamingHack;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * This is a replacement for the fullblown {@link StartupBean} of the actual RHQ server.
 * @author Lukas Krejci
 */
@Singleton
public class StrippedDownStartupBean {

    public static final String RHQ_SERVER_NAME_PROPERTY = "rhq.server.high-availability.name";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    private void secureNaming() {
        NamingHack.bruteForceInitialContextFactoryBuilder();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void init() {
        secureNaming();
    }

    /**
     * <p>
     * Purges the test server and any storage nodes created during server initialization from a prior test run.
     * </p>
     */
    public void purgeTestServerAndStorageNodes() {
        entityManager.createQuery("DELETE FROM " + StorageNode.class.getName()).executeUpdate();
        entityManager.createQuery("DELETE FROM " + Server.class.getName() + " WHERE name = :serverName")
            .setParameter("serverName", TestConstants.RHQ_TEST_SERVER_NAME)
            .executeUpdate();
    }
}
