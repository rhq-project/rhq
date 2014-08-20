/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

package org.rhq.enterprise.server.tagging;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.bundle.TestBundlePluginComponent;
import org.rhq.enterprise.server.bundle.TestBundleServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * @author Thomas Segismont
 */
@Test
public class TagManagerBeanTest extends AbstractEJB3Test {

    private TagManagerLocal tagManager;

    private BundleManagerLocal bundleManager;

    private Subject overlord;

    private Random random = new Random();

    private String testNamespace = getRandomString();

    private String testSemantic = getRandomString();

    @Override
    protected void beforeMethod() throws Exception {
        tagManager = LookupUtil.getTagManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        bundleManager = LookupUtil.getBundleManager();
        overlord = subjectManager.getOverlord();
        TestBundleServerPluginService bundleServerPluginService = new TestBundleServerPluginService(getTempDir(), new TestBundlePluginComponent());
        prepareCustomServerPluginService(bundleServerPluginService);
        bundleServerPluginService.startMasterPluginContainer();
    }

    @Override
    protected void afterMethod() throws Exception {
        unprepareServerPluginService();
    }

    public void testAddTags() {
        executeInTransaction(new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                Set<Tag> tags = generateTagSet(1000);
                tags = tagManager.addTags(overlord, tags);
                assertEquals(1000, tags.size());

                PageList<Tag> foundTags = tagManager.findTagsByCriteria(overlord, getTestCriteria());
                assertTrue("Default paging should find less tags than created", foundTags.size() < tags.size());

                TagCriteria tagCriteria = getTestCriteria();
                tagCriteria.clearPaging();
                foundTags = tagManager.findTagsByCriteria(overlord, tagCriteria);
                assertTrue("Cleared paging should find as much tags as created", foundTags.size() == tags.size());
            }
        });
    }

    public void addTagsShouldSilentlyHandleTagDuplicates() {
        executeInTransaction(new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                Set<Tag> tags = generateTagSet(5);
                tags = tagManager.addTags(overlord, tags);
                while (tags.size() < 7) {
                    tags.add(generateTag());
                }
                Set<Tag> allTags = tagManager.addTags(overlord, tags);
                assertTrue("#addTags should silently handle tag duplicates", tags.equals(allTags));
            }
        });
    }

    public void testRemoveTags() {
        executeInTransaction(new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                Set<Tag> tags = generateTagSet(11);
                tags = tagManager.addTags(overlord, tags);
                Set<Tag> tagsToRemove = emptyTagSet();
                // Remove about half of the inserted tags
                for (Tag tag : tags) {
                    tagsToRemove.add(tag);
                    if (tagsToRemove.size() > tags.size() / 2) {
                        break;
                    }
                }
                tagManager.removeTags(overlord, tagsToRemove);
                TagCriteria tagCriteria = getTestCriteria();
                tagCriteria.clearPaging();
                PageList<Tag> remainingTags = tagManager.findTagsByCriteria(overlord, tagCriteria);
                assertEquals(tags.size() - tagsToRemove.size(), remainingTags.size());
            }
        });
    }

    public void testUpdateResourceTags() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Resource resource = SessionTestHelper.createNewResource(em, getRandomString());
                resource.setTags(emptyTagSet());
                Set<Tag> tags = generateTagSet(7);
                tagManager.updateResourceTags(overlord, resource.getId(), tags);
                assertEquals(tags, resource.getTags());
            }
        });
    }

    public void testUpdateResourceGroupTags() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                ResourceGroup resourceGroup = createResourceGroup();
                resourceGroup.setTags(emptyTagSet());
                Set<Tag> tags = generateTagSet(7);
                tagManager.updateResourceGroupTags(overlord, resourceGroup.getId(), tags);
                assertEquals(tags, resourceGroup.getTags());
            }
        });
    }

    public void testUpdateBundleTags() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Set<Tag> tags = generateTagSet(7);
                Bundle bundle = createBundle();
                bundle.setTags(emptyTagSet());
                tagManager.updateBundleTags(overlord, bundle.getId(), tags);
                assertEquals(tags, bundle.getTags());
            }
        });
    }

    public void testUpdateBundleVersionTags() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Set<Tag> tags = generateTagSet(7);
                BundleVersion bundleVersion = createBundleVersion();
                bundleVersion.setTags(emptyTagSet());
                tagManager.updateBundleVersionTags(overlord, bundleVersion.getId(), tags);
                assertEquals(tags, bundleVersion.getTags());
            }
        });
    }

    public void testUpdateBundleDeploymentTags() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Set<Tag> tags = generateTagSet(7);
                BundleDeployment bundleDeployment = createBundleDeployment();
                bundleDeployment.setTags(emptyTagSet());
                tagManager.updateBundleDeploymentTags(overlord, bundleDeployment.getId(), tags);
                assertEquals(tags, bundleDeployment.getTags());
            }
        });
    }

    public void testUpdateBundleDestinationTags() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Set<Tag> tags = generateTagSet(7);
                BundleDestination bundleDestination = createBundleDestination();
                bundleDestination.setTags(emptyTagSet());
                tagManager.updateBundleDestinationTags(overlord, bundleDestination.getId(), tags);
                assertEquals(tags, bundleDestination.getTags());
            }
        });
    }

    private Set<Tag> emptyTagSet() {
        return new HashSet<Tag>();
    }

    private Set<Tag> generateTagSet(int size) {
        Set<Tag> generatedTags = emptyTagSet();
        while (generatedTags.size() < size) {
            generatedTags.add(generateTag());
        }
        return generatedTags;
    }

    private Tag generateTag() {
        return new Tag(testNamespace, testSemantic, getRandomString());
    }

    private String getRandomString() {
        return new BigInteger(50, random).toString(32);
    }

    private TagCriteria getTestCriteria() {
        TagCriteria criteria = new TagCriteria();
        criteria.addFilterNamespace(testNamespace);
        criteria.addFilterSemantic(testSemantic);
        return criteria;
    }

    private ResourceGroup createResourceGroup() {
        Subject subject = SessionTestHelper.createNewSubject(em, getRandomString());
        Role role = SessionTestHelper.createNewRoleForSubject(em, subject, getRandomString());
        return SessionTestHelper.createNewCompatibleGroupForRole(em, role, getRandomString());
    }

    private Bundle createBundle() throws Exception {
        ResourceType resourceType = SessionTestHelper.createNewResourceType(em);
        BundleType bundleType = bundleManager.createBundleType(overlord, getRandomString(), resourceType.getId());
        return bundleManager.createBundle(overlord, getRandomString(), getRandomString(), bundleType.getId(), null);
    }

    private BundleVersion createBundleVersion() throws Exception {
        return createBundleVersion(createBundle());
    }

    private BundleVersion createBundleVersion(Bundle bundle) throws Exception {
        return bundleManager.createBundleVersion(overlord, bundle.getId(), getRandomString(), getRandomString(),
            getRandomString(), getRandomString());
    }

    private BundleDestination createBundleDestination() throws Exception {
        Bundle bundle = createBundle();
        ResourceGroup resourceGroup = createResourceGroupForBundleDeployments();
        String destName = resourceGroup.getResourceType().getResourceTypeBundleConfiguration()
            .getBundleDestinationSpecifications().iterator().next().getName();

        return bundleManager.createBundleDestination(overlord, bundle.getId(), getRandomString(), getRandomString(),
            destName, getRandomString(), resourceGroup.getId());
    }

    private BundleDeployment createBundleDeployment() throws Exception {
        BundleDestination bundleDestination = createBundleDestination();
        Configuration configuration = new Configuration();
        configuration.put(new PropertySimple("bundletest.property", "bundletest.property value"));
        BundleVersion bundleVersion = createBundleVersion(bundleDestination.getBundle());
        return bundleManager.createBundleDeployment(overlord, bundleVersion.getId(), bundleDestination.getId(),
            getRandomString(), configuration);
    }

    private ResourceGroup createResourceGroupForBundleDeployments() {

        ResourceType resourceType = SessionTestHelper.createNewResourceType(em);

        ResourceTypeBundleConfiguration resourceTypeBundleConfiguration = new ResourceTypeBundleConfiguration(
            new Configuration());
        resourceTypeBundleConfiguration.addBundleDestinationBaseDirectory(getRandomString(),
            ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory.Context.pluginConfiguration.name(),
            getRandomString(), getRandomString());
        resourceType.setResourceTypeBundleConfiguration(resourceTypeBundleConfiguration);

        Resource resource = SessionTestHelper.createNewResource(em, getRandomString(), resourceType);

        ResourceGroup resourceGroup = createResourceGroup();
        resourceGroup.addExplicitResource(resource);
        resourceGroup.setResourceType(resourceType);

        return resourceGroup;
    }

}
