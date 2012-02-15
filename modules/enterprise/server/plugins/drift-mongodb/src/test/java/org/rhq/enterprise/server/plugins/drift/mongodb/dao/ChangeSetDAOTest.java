/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugins.drift.mongodb.dao;

import java.util.List;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;

import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.util.PageOrdering;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBFile;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_CHANGED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_REMOVED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ChangeSetDAOTest {

    // Tests need to be disabled when committed/pushed to the remote repo until we get
    // mongodb installed on the hudson slave
    static final boolean ENABLED = true;

    Mongo connection;

    Morphia morphia;

    Datastore ds;

    ChangeSetDAO dao;

    @BeforeClass
    public void initDB() throws Exception {
        connection = new Mongo("127.0.0.1");

        morphia = new Morphia().map(MongoDBChangeSet.class).map(MongoDBChangeSetEntry.class).map(MongoDBFile.class);

        ds = morphia.createDatastore(connection, "rhqtest");
    }

    @BeforeMethod
    public void clearCollections() throws Exception {
        Query deleteAll = ds.createQuery(MongoDBChangeSet.class);
        ds.delete(deleteAll);

        dao = new ChangeSetDAO(morphia, connection, "rhqtest");
    }

    @Test(enabled = ENABLED)
    public void saveAndLoadEmptyChangeSet() throws Exception {
        MongoDBChangeSet expected = new MongoDBChangeSet();
        expected.setCategory(COVERAGE);
        expected.setVersion(1);
        expected.setDriftDefinitionId(1);
        expected.setResourceId(1);
        expected.setDriftHandlingMode(normal);

        dao.save(expected);
        MongoDBChangeSet actual = dao.get(expected.getObjectId());

        assertChangeSetMatches("Failed to save/load change set with no entries", expected, actual);
    }

    @Test(enabled = ENABLED)
    public void saveAndLoadChangeSetWithAddedFileEntry() throws Exception {
        MongoDBChangeSet expected = new MongoDBChangeSet();
        expected.setCategory(DRIFT);
        expected.setResourceId(1);
        expected.setDriftDefinitionId(1);
        expected.setVersion(1);

        MongoDBChangeSetEntry entry = new MongoDBChangeSetEntry();
        entry.setCategory(FILE_ADDED);
        entry.setPath("foo");

        // Adding the file to the change set entry causes the test to fail because the
        // custom assert uses the equals method when comparing the MongoDBFile objects and
        // MongoDBFile does not implement equals/hashCode...yet.

        //        MongoDBFile file = new MongoDBFile();
        //        file.setDataSize(1024L);
        //        file.setHashId("a1b2c3d4");
        //        file.setStatus(EMPTY);
        //
        //        entry.setNewDriftFile(file);
        expected.add(entry);

        dao.save(expected);

        MongoDBChangeSet actual = dao.get(expected.getObjectId());

        assertChangeSetMatches("Failed to save/load change set with one entry", expected, actual);
    }

    @Test(enabled = ENABLED)
    public void saveAndLoadChangeSetWithMultipleEntries() throws Exception {
        MongoDBChangeSet expected = new MongoDBChangeSet();
        expected.setCategory(DRIFT);
        expected.setVersion(1);
        expected.setResourceId(1);
        expected.setDriftDefinitionId(1);
        expected.add(new MongoDBChangeSetEntry("foo", FILE_ADDED)).add(new MongoDBChangeSetEntry("bar", FILE_ADDED));

        dao.save(expected);

        MongoDBChangeSet actual = dao.get(expected.getObjectId());

        assertChangeSetMatches("Failed to save/load change set with multiple entries", expected, actual);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithIdFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(1);
        c2.setResourceId(2);
        c2.setDriftDefinitionId(2);
        dao.save(c2);

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterId(c1.getId());

        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c1);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change set criteria with id filter",
                expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithResourceIdFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(2);
        c2.setDriftDefinitionId(2);
        dao.save(c2);

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterResourceId(1);
        
        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c1);
        
        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change set criteria with resource id " +
                "filter", expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithVersionFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);
        dao.save(c2);
        
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addFilterVersion("2");
        
        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c2);
        
        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change criteria with version filter",
                expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithStartVersionFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);
        dao.save(c2);

        MongoDBChangeSet c3 = new MongoDBChangeSet();
        c3.setCategory(DRIFT);
        c3.setVersion(3);
        c3.setResourceId(1);
        c3.setDriftDefinitionId(1);
        dao.save(c3);

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addFilterStartVersion("2");

        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c2, c3);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change criteria with start version filter",
                expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithEndVersionFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);
        dao.save(c2);

        MongoDBChangeSet c3 = new MongoDBChangeSet();
        c3.setCategory(DRIFT);
        c3.setVersion(3);
        c3.setResourceId(1);
        c3.setDriftDefinitionId(1);
        dao.save(c3);

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addFilterEndVersion("2");

        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c1, c2);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change criteria with end version filter",
                expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithDriftDefIdFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);
        
        MongoDBChangeSet c11 = new MongoDBChangeSet();
        c11.setCategory(DRIFT);
        c11.setVersion(2);
        c11.setResourceId(1);
        c11.setDriftDefinitionId(1);
        dao.save(c11);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(1);
        c2.setResourceId(2);
        c2.setDriftDefinitionId(2);
        dao.save(c2);
        
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        
        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c1, c11);
        
        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change set criteria with drift " +
                "definition id filter", expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithCreatedAfterFilter() throws Exception {
        long creationTime = System.currentTimeMillis() - (1000 * 60 * 10);  // 10 minutes ago
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        c1.setCtime(creationTime);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);
        c2.setCtime(c1.getCtime() + (1000 * 60 * 5));  // 5 minutes ago
        dao.save(c2);

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addFilterCreatedAfter(creationTime + 1000);

        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c2);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change set criteria with created after " +
                "filter", expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithCreatedBeforeFilter() throws Exception {
        long creationTime = System.currentTimeMillis() - (1000 * 60 * 10);  // 10 minutes ago
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        c1.setCtime(creationTime);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);
        c2.setCtime(c1.getCtime() + (1000 * 60 * 5));  // 5 minutes ago
        dao.save(c2);

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addFilterCreatedBefore(c2.getCtime());

        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c1);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change set criteria with created after " +
                "filter", expected, actual, ignore);
    }
    
    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithPathFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("/test/foo.txt", FILE_ADDED);
        c1.add(e1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);

        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("/test/bar.txt", FILE_ADDED);
        c2.add(e2);
        dao.save(c2);
        
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftPath("foo");
        
        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c1);
        
        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change set critiera with drift path filter",
                expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithDirectoryFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);

        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("./foo/foo.txt", FILE_ADDED);
        c1.add(e1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);

        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("./bar/bar.txt", FILE_ADDED);
        c2.add(e2);
        dao.save(c2);

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addFilterDriftDirectory("./bar");

        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c2);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change set critiera with drift directory filter",
                expected, actual, ignore);
    }
    
    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithCategoryFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(COVERAGE);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);
        dao.save(c2);

        MongoDBChangeSet c3 = new MongoDBChangeSet();
        c3.setCategory(DRIFT);
        c3.setVersion(3);
        c3.setResourceId(1);
        c3.setDriftDefinitionId(1);
        dao.save(c3);
        
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addFilterCategory(DRIFT);
        
        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c2, c3);
        
        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change set criteria with category filter",
                expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaWithDriftCategoryFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(COVERAGE);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        c1.add(new MongoDBChangeSetEntry("foo.txt", FILE_ADDED));
        c1.add(new MongoDBChangeSetEntry("bar.txt", FILE_ADDED));
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);
        c2.add(new MongoDBChangeSetEntry("foo.txt", FILE_CHANGED));
        dao.save(c2);

        MongoDBChangeSet c3 = new MongoDBChangeSet();
        c3.setCategory(DRIFT);
        c3.setVersion(3);
        c3.setResourceId(1);
        c3.setDriftDefinitionId(1);
        c3.add(new MongoDBChangeSetEntry("bar.txt", FILE_REMOVED));
        dao.save(c3);

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addFilterDriftCategories(FILE_CHANGED, FILE_REMOVED);

        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c2, c3);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift change set criteria with drift categories filter",
                expected, actual, ignore);
    }
    
    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaAndDoNotFetchDrifts() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(COVERAGE);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        c1.add(new MongoDBChangeSetEntry("foo.txt", FILE_ADDED));
        c1.add(new MongoDBChangeSetEntry("bar.txt", FILE_ADDED));
        dao.save(c1);
        
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.fetchDrifts(false);
        
        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c1);
        
        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change set when not fetching drifts", expected, actual, ignore);
        assertTrue(actual.get(0).getDrifts().isEmpty(),
                "The drift entries should not have been fetched with the change set");
    }
    
    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaAndSortByVersionAscending() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(COVERAGE);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);
        dao.save(c2);

        MongoDBChangeSet c3 = new MongoDBChangeSet();
        c3.setCategory(DRIFT);
        c3.setVersion(3);
        c3.setResourceId(1);
        c3.setDriftDefinitionId(1);
        dao.save(c3);
        
        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addSortVersion(PageOrdering.ASC);
        
        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c1, c2, c3);
        
        assertChangeSetsMatch("Failed to sort change sets by version in ascending order", expected, actual);
    }

    @Test(enabled = ENABLED)
    public void findByChangeSetCriteriaAndSortByVersionDescending() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(COVERAGE);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(2);
        c2.setResourceId(1);
        c2.setDriftDefinitionId(1);
        dao.save(c2);

        MongoDBChangeSet c3 = new MongoDBChangeSet();
        c3.setCategory(DRIFT);
        c3.setVersion(3);
        c3.setResourceId(1);
        c3.setDriftDefinitionId(1);
        dao.save(c3);

        GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
        criteria.addFilterDriftDefinitionId(1);
        criteria.addSortVersion(PageOrdering.DESC);

        List<MongoDBChangeSet> actual = dao.findByChangeSetCritiera(criteria);
        List<MongoDBChangeSet> expected = asList(c3, c2, c1);

        assertChangeSetsMatch("Failed to sort change sets by version in descending order", expected, actual);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithResourceIdFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftDefinitionId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(1);
        c2.setResourceId(2);
        c2.setDriftDefinitionId(2);
        dao.save(c2);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterResourceIds(1);

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);
        List<MongoDBChangeSet> expected = asList(c1);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift criteria with resource id filter.",
            expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithResourceIdsFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(COVERAGE, 1, 2, 2);
        dao.save(c2);

        MongoDBChangeSet c3 = createChangeSet(COVERAGE, 1, 3, 3);
        dao.save(c3);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterResourceIds(1, 2);

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);
        List<MongoDBChangeSet> expected = asList(c1, c2);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift criteria with resource ids filter.",
            expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithCategoryFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1).add(new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED));
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, 1, 1).add(new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED))
            .add(new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED));
        dao.save(c2);

        MongoDBChangeSet c3 = createChangeSet(DRIFT, 3, 1, 1).add(new MongoDBChangeSetEntry("c1-1.txt", FILE_REMOVED));
        dao.save(c3);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterCategories(FILE_ADDED);

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);
        List<MongoDBChangeSet> expected = asList(c1, c2);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift criteria with category filter.", expected,
            actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithCategoriesFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1).add(new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED));
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, 1, 1).add(new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED))
            .add(new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED));
        dao.save(c2);

        MongoDBChangeSet c3 = createChangeSet(DRIFT, 3, 1, 1).add(new MongoDBChangeSetEntry("c1-1.txt", FILE_REMOVED));
        dao.save(c3);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterCategories(FILE_CHANGED, FILE_REMOVED);

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);
        List<MongoDBChangeSet> expected = asList(c2, c3);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift criteria with categories filter.",
            expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findEntriesWithIdFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED);
        c1.add(e1);
        c1.add(e2);
        dao.save(c1);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterId(e2.getId());

        List<MongoDBChangeSetEntry> entries = dao.findEntries(criteria);
        assertEntriesMatch("Failed to find change set entries with id filter", asList(e2), entries);
    }

    @Test(enabled = ENABLED)
    public void findEntriesWithCategoriesFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("c1-2.txt", FILE_ADDED);
        c1.add(e1).add(e2);
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, 1, 1);
        MongoDBChangeSetEntry e3 = new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED);
        MongoDBChangeSetEntry e4 = new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED);
        MongoDBChangeSetEntry e5 = new MongoDBChangeSetEntry("c1-2.txt", FILE_REMOVED);
        c2.add(e3).add(e4).add(e5);
        dao.save(c2);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterCategories(FILE_CHANGED, FILE_REMOVED);

        List<MongoDBChangeSetEntry> entries = dao.findEntries(criteria);
        assertEntriesMatch("Failed to find change set entries with category filter", asList(e4, e5), entries);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithStartTimeFilter() throws Exception {
        long startTime = System.currentTimeMillis() - (1000 * 60 * 60);  // one hour ago

        int resourceId = 1;

        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, resourceId, 1);
        c1.setCtime(startTime - (1000 * 5));  // c1 is created 5 seconds before startTime
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        e1.setCtime(startTime - (1000 * 5));  // e1 is created 5 seconds before startTime
        c1.add(e1);

        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, resourceId, 1);
        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED);
        c2.add(e2);

        dao.save(c2);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterResourceIds(resourceId);
        criteria.addFilterStartTime(startTime);

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);

        assertEquals(actual.size(), 1, "Expected to get back one change set");
        MongoDBChangeSet actualChangeSet = actual.get(0);

        assertChangeSetMatches("Failed to find drift entries by drift criteria with start time filter", c2,
            actualChangeSet);
    }

    @Test(enabled = ENABLED)
    public void findEntriesWithResourceIdFilters() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        c1.add(e1);
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(COVERAGE, 1, 2, 1);
        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED);
        c2.add(e2);
        dao.save(c2);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterResourceIds(1);

        List<MongoDBChangeSetEntry> entries = dao.findEntries(criteria);
        assertEntriesMatch("Failed to find change set entries with resource id filter", asList(e1), entries);
    }

    @Test(enabled = ENABLED)
    public void findEntriesWithResourceIdAndCategoryFilters() {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("c1-2.txt", FILE_ADDED);
        c1.add(e1).add(e2);
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(COVERAGE, 1, 2, 1);
        MongoDBChangeSetEntry e3 = new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED);
        c2.add(e3);
        dao.save(c2);

        MongoDBChangeSet c3 = createChangeSet(DRIFT, 2, 1, 1);
        MongoDBChangeSetEntry e4 = new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED);
        MongoDBChangeSetEntry e5 = new MongoDBChangeSetEntry("c1-2.txt", FILE_REMOVED);
        c3.add(e4).add(e5);
        dao.save(c3);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterResourceIds(1);
        criteria.addFilterCategories(FILE_ADDED, FILE_CHANGED);

        List<MongoDBChangeSetEntry> entries = dao.findEntries(criteria);
        assertEntriesMatch("Failed to find change set entries with resource id and category filters",
            asList(e1, e2, e4), entries);
    }

    @Test(enabled = ENABLED)
    public void findEntriesWithResourceIdAndStartTimeFilter() throws Exception {
        long startTime = System.currentTimeMillis() - (1000 * 60 * 60);  // one hour ago

        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        c1.setCtime(startTime - (1000 * 5));  // c1 is created 5 seconds before startTime
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        c1.add(e1);
        e1.setCtime(startTime - (1000 * 5));  // e1 is created 5 seconds before startTime
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(COVERAGE, 1, 2, 1);
        c2.setCtime(startTime - (1000 * 5));  // c2 is created 5 seconds before startTime
        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED);
        e2.setCtime(startTime - (1000 * 5));  // e2 is created 5 seconds before startTime
        c2.add(new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED));
        dao.save(c2);

        MongoDBChangeSet c3 = createChangeSet(DRIFT, 2, 1, 1);
        MongoDBChangeSetEntry e3 = new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED);
        c3.add(e3);
        dao.save(c3);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterStartTime(startTime);
        criteria.addFilterResourceIds(1);

        List<MongoDBChangeSetEntry> entries = dao.findEntries(criteria);
        assertEntriesMatch("Failed to find change set entries with resource id and start time filters", asList(e3),
            entries);
    }

    @Test(enabled = ENABLED)
    public void findEntriesWithResourceIdAndEndTimeFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        c1.add(e1);
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(COVERAGE, 1, 2, 1);
        c2.add(new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED));
        dao.save(c2);

        long endTime = System.currentTimeMillis();
        Thread.sleep(10);

        MongoDBChangeSet c3 = createChangeSet(DRIFT, 2, 1, 1);
        MongoDBChangeSetEntry e3 = new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED);
        c3.add(e3);
        dao.save(c3);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterEndTime(endTime);
        criteria.addFilterResourceIds(1);

        List<MongoDBChangeSetEntry> entries = dao.findEntries(criteria);
        assertEntriesMatch("Failed to find change set entries with resource id and end time filters", asList(e1),
            entries);
    }

    @Test(enabled = ENABLED)
    public void findEntriesWithPathFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        c1.add(e1);
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, 1, 1);
        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED);
        MongoDBChangeSetEntry e3 = new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED);
        c2.add(e2).add(e3);
        dao.save(c2);

        MongoDBChangeSet c3 = createChangeSet(COVERAGE, 1, 2, 1);
        MongoDBChangeSetEntry e4 = new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED);
        MongoDBChangeSetEntry e5 = new MongoDBChangeSetEntry("c3-1.txt", FILE_ADDED);
        c3.add(e4).add(e5);
        dao.save(c3);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterPath("c2-1.txt");

        List<MongoDBChangeSetEntry> entries = dao.findEntries(criteria);
        assertEntriesMatch("Failed to find change set entries with path filter", asList(e3, e4), entries);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithChangeSetIdFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        c1.add(new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED));

        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, 1, 1);
        c2.add(new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED));

        dao.save(c2);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterChangeSetId(c2.getId());

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);

        assertChangeSetsMatchNoOrder("Failed to find change sets by drift criteria with change set id filter", asList(c2),
                actual);
    }
    
    private void assertChangeSetsMatch(String msg, List<MongoDBChangeSet> expected, List<MongoDBChangeSet> actual) {
        assertEquals(actual.size(), expected.size(), "The number of change sets differ: " + msg);
        int i = 0;
        for (MongoDBChangeSet expectedChangeSet : expected) {
            assertChangeSetMatches("Change sets do not match: " + msg, expectedChangeSet, actual.get(i++));
        }
    }

    private void assertChangeSetsMatchNoOrder(String msg, List<MongoDBChangeSet> expected, 
            List<MongoDBChangeSet> actual) {
        assertEquals(actual.size(), expected.size(), "The number of change sets differ: " + msg);
        for (MongoDBChangeSet expectedChangeSet : expected) {
            MongoDBChangeSet actualChangeSet = null;
            for (MongoDBChangeSet changeSet : actual) {
                if (expectedChangeSet.getObjectId().equals(changeSet.getObjectId())) {
                    actualChangeSet = changeSet;
                    break;
                }
            }
            assertNotNull(actualChangeSet, msg + ": expected to find change set " + expectedChangeSet);
            assertChangeSetMatches(msg, expectedChangeSet, actualChangeSet);
        }
    }

    private void assertEntriesMatch(String msg, List<MongoDBChangeSetEntry> expected,
        List<MongoDBChangeSetEntry> actual) {
        assertEquals(actual.size(), expected.size(), "The number of entries differ: " + msg);
        assertCollectionMatchesNoOrder(msg, expected, actual, "changeSet");
    }

    /**
     * This method first checks that the actual change set is not null. It then performs a
     * property-wise comparison against the expected change set using
     * {@link org.rhq.test.AssertUtils#assertPropertiesMatch(String, Object, Object, String...) assertPropertiesMatch}.
     * The {@link org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet#getDrifts() drifts}
     * property is then tested separately using
     * {@link org.rhq.test.AssertUtils#assertCollectionMatchesNoOrder(String, java.util.Collection, java.util.Collection, String...) assertCollectionMatches}.
     *
     * @param msg An error message
     * @param expected The expected change set to test against
     * @param actual The actual change set under test
     */
    void assertChangeSetMatches(String msg, MongoDBChangeSet expected, MongoDBChangeSet actual) {
        assertNotNull(actual, msg + ": change set is null");

        String ignore = "drifts";
        assertPropertiesMatch(msg, expected, actual, ignore);

        ignore = "changeSet";
        assertCollectionMatchesNoOrder(msg + ": " + "change set entries do not match expected entries.", expected
            .getDrifts(), actual.getDrifts(), ignore);
    }

    /**
     * A convenience factory method for creating a change set.
     *
     * @param category A {@link DriftChangeSetCategory}
     * @param version The change set version
     * @param resourceId The owning resource id
     * @param driftDefId The drift definition id
     * @return A {@link MongoDBChangeSet}
     */
    MongoDBChangeSet createChangeSet(DriftChangeSetCategory category, int version, int resourceId, int driftDefId) {
        MongoDBChangeSet changeSet = new MongoDBChangeSet();
        changeSet.setCategory(category);
        changeSet.setVersion(version);
        changeSet.setResourceId(resourceId);
        changeSet.setDriftDefinitionId(driftDefId);

        return changeSet;
    }

}
