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

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceAlreadyExistsException;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

@Stateless
public class RegistrationManagerBean implements RegistrationManagerLocal, RegistrationManagerRemote {

    private final Log log = LogFactory.getLog(RegistrationManagerBean.class.getName());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;
    @EJB
    @IgnoreDependency
    private ResourceManagerLocal resourceManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private AgentManagerLocal agentManager;

    /**
     * Registers the platform as a resource on RHQ server
     * @param user secure id user
     * @param platform to be registered
     * @param parentId if any.
     *
     */
    public void registerPlatform(Subject user, Resource platform, int parentId) {
        try {
            validatePlatform(platform);
        } catch (RegistrationException e) {
            throw new IllegalStateException(e);
        }

        platform.setInventoryStatus(InventoryStatus.COMMITTED);

        Agent agent = createFakeAgent(platform);
        platform.setAgent(agent);

        try {
            resourceManager.createResource(user, platform, parentId);
        } catch (ResourceAlreadyExistsException e) {
            throw new IllegalStateException(e);
        }

        log.error("Resource " + platform + "persisted");

    }

    /**
     * Imports the registered resource into inventory and makes it a managed resource
     * @param user secure id user
     * @param resource to be imported
     *
     */
    public void importPlatform(Subject user, Resource resource) {
        resource.setInventoryStatus(InventoryStatus.COMMITTED);
        entityManager.persist(resource);
    }

    /**
     * subscribe the requested platform to a compatible base repo based on release
     * and arch information. This method does a lookup in ReleaseRepoMapping
     * to get the compatible base channel and tried to tie the platform to
     * the repo.
     *  @param user secure id user
     *  @param platform to be imported
     *  @param release the release string of the client
     *  @param arch arch of the platform
     */
    public void subscribePlatformToBaseRepo(Subject user, Resource platform, String release, String version, String arch)
        throws RegistrationException {
        log.debug("Trying to subscribe " + platform + " to a compatible baserepo");
        ReleaseRepoMapping rm = new ReleaseRepoMapping(release, version, arch);
        String repoName = rm.getCompatibleRepo();
        try {
            RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
            List<Repo> repos = repoManager.getRepoByName(repoName);
            int[] repoIds = getIntRepoIdArray(repos);
            repoManager.subscribeResourceToRepos(user, platform.getId(), repoIds);
        } catch (Exception e) {
            throw new RegistrationException("No Compatible Base repo Found." + e);
        }
    }

    /**
     * Validates the sanity of the resource to be created. Also checks to see if the platform
     * alreday exists.
     * @param platform
     * @throws RegistrationException
     */
    private void validatePlatform(Resource platform) throws RegistrationException {
        if (platform.getResourceType() == null) {
            throw new RegistrationException("Reported resource [" + platform + "] has a null type.");
        }

        if (platform.getResourceKey() == null) {
            throw new RegistrationException("Reported resource [" + platform + "] has a null key.");
        }

        if (platform.getInventoryStatus() == InventoryStatus.DELETED) {
            throw new RegistrationException(
                "Reported resource ["
                    + platform
                    + "] has an illegal inventory status of 'DELETED' - agents are not allowed to delete platforms from inventory.");
        }

        //check if the resource already exists
        Resource existingPlatform = checkResourceExists(platform);

        if (existingPlatform != null) {
            throw new RegistrationException("Reported resource [" + platform + "] Already Exists.");
        }

    }

    /**
     * Checks to see if the resource already exists in rhq
     * @param platform
     * @return
     */
    private Resource checkResourceExists(Resource platform) {
        Resource existingResource = null;

        if (platform.getId() != 0) {
            existingResource = entityManager.find(Resource.class, platform.getId());
        }

        if (existingResource == null) {
            ResourceType resourceType = platform.getResourceType();
            existingResource = resourceManager.getResourceByParentAndKey(subjectManager.getOverlord(), platform
                .getParentResource(), platform.getResourceKey(), resourceType.getPlugin(), resourceType.getName());
            if (existingResource != null) {
                platform.setId(existingResource.getId());
            } else {

                if (platform.getId() != 0) {
                    platform.setId(0);
                }
            }
        }

        return existingResource;
    }

    /**
     * Creates an agent for a given resource and persist in the db. The agents role is noop
     * until its installed on the client and tied to this row in the db.
     * @param resource
     * @return
     */
    private Agent createFakeAgent(Resource resource) {
        String address = resource.getName();
        int port = 16163;
        String endPoint = "socket://" + address + ":" + port + "/?rhq.communications.connector.rhqtype=agent";
        Agent agent = agentManager.getAgentByAddressAndPort(resource.getName(), 16163);
        if (agent == null) {
            agent = new Agent(resource.getName(), address, port, endPoint, resource.getResourceKey());

            entityManager.persist(agent);
            entityManager.flush();
        }
        return agent;

    }

    private int[] getIntRepoIdArray(List<Repo> input) {
        if (input == null) {
            return new int[0];
        }

        int[] output = new int[input.size()];
        for (int i = 0; i < input.size(); i++) {
            output[i] = Integer.valueOf(input.get(i).getId()).intValue();
        }

        return output;
    }

}
