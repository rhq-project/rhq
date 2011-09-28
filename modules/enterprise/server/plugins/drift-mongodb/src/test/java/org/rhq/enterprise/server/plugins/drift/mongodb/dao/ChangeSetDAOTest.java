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

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.DriftCategory;
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
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

public class ChangeSetDAOTest {

    // Tests need to be disabled when committed/pushed to the remote repo until we get
    // mongodb installed on the hudson slave
    static final boolean ENABLED = false;

    Mongo connection;

    Morphia morphia;

    Datastore ds;

    ChangeSetDAO dao;

    @BeforeClass
    public void initDB() throws Exception {
        connection = new Mongo("127.0.0.1");

        morphia = new Morphia()
            .map(MongoDBChangeSet.class)
            .map(MongoDBChangeSetEntry.class)
            .map(MongoDBFile.class);

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
        expected.setDriftConfigurationId(1);
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
        expected.setDriftConfigurationId(1);
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
        expected.setDriftConfigurationId(1);
        expected.add(new MongoDBChangeSetEntry("foo", FILE_ADDED)).add(new MongoDBChangeSetEntry("bar", FILE_ADDED));

        dao.save(expected);

        MongoDBChangeSet actual = dao.get(expected.getObjectId());

        assertChangeSetMatches("Failed to save/load change set with multiple entries", expected, actual);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithResourceIdFilter() throws Exception {
        MongoDBChangeSet c1 = new MongoDBChangeSet();
        c1.setCategory(DRIFT);
        c1.setVersion(1);
        c1.setResourceId(1);
        c1.setDriftConfigurationId(1);
        dao.save(c1);

        MongoDBChangeSet c2 = new MongoDBChangeSet();
        c2.setCategory(DRIFT);
        c2.setVersion(1);
        c2.setResourceId(2);
        c2.setDriftConfigurationId(2);

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

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, 1, 1)
            .add(new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED))
            .add(new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED));
        dao.save(c2);

        MongoDBChangeSet c3 = createChangeSet(DRIFT, 3, 1, 1).add(new MongoDBChangeSetEntry("c1-1.txt", FILE_REMOVED));
        dao.save(c3);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterCategories(FILE_ADDED);

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);
        List<MongoDBChangeSet> expected = asList(c1, c2);

        String ignore = "drifts";
        assertCollectionMatchesNoOrder("Failed to find change sets by drift criteria with category filter.",
            expected, actual, ignore);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithCategoriesFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1).add(new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED));
        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, 1, 1)
            .add(new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED))
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
    public void findByDriftCriteriaWithIdFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        MongoDBChangeSetEntry entry = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        c1.add(entry);

        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(COVERAGE, 2, 1, 1);
        c2.add(new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED));

        dao.save(c2);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterId(entry.getId());

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);

        assertEquals("Expected to get back only one change set when searching by drift criteria with id filter.",
            1, actual.size());
        assertChangeSetMatches("Failed to find change set by drift criteria with id filter.", c1, actual.get(0));
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithStartTimeFilter() throws Exception {
        int resourceId = 1;

        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, resourceId, 1);
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        c1.add(e1);

        dao.save(c1);

        long startTime = System.currentTimeMillis();
        Thread.sleep(10);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, resourceId, 1);
        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED);
        c2.add(e2);

        dao.save(c2);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterResourceIds(resourceId);
        criteria.addFilterStartTime(startTime);

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);

        assertEquals("Expected to get back one change set", 1, actual.size());
        MongoDBChangeSet actualChangeSet = actual.get(0);

        assertChangeSetMatches("Failed to find drift entries by drift criteria with start time filter",
            c2, actualChangeSet);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithEndTimeFilter() throws Exception {
        int resourceId = 1;

        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, resourceId, 1);
        MongoDBChangeSetEntry e1 = new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED);
        c1.add(e1);

        dao.save(c1);

        long endTime = System.currentTimeMillis();
        Thread.sleep(10);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, resourceId, 1);
        MongoDBChangeSetEntry e2 = new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED);
        c2.add(e2);

        dao.save(c2);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterEndTime(endTime);
        criteria.addFilterResourceIds(resourceId);

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);

        assertEquals("Expected to get back one change set", 1, actual.size());
        MongoDBChangeSet actualChangeSet = actual.get(0);

        assertChangeSetMatches("Failed to find drift entries with end time filter", c1, actualChangeSet);
    }

    @Test(enabled = ENABLED)
    public void findByDriftCriteriaWithPathFilter() throws Exception {
        MongoDBChangeSet c1 = createChangeSet(COVERAGE, 1, 1, 1);
        c1.add(new MongoDBChangeSetEntry("c1-1.txt", FILE_ADDED));

        dao.save(c1);

        MongoDBChangeSet c2 = createChangeSet(DRIFT, 2, 1, 1);
        c2.add(new MongoDBChangeSetEntry("c1-1.txt", FILE_CHANGED));

        dao.save(c2);

        MongoDBChangeSet c3 = createChangeSet(DRIFT, 3, 1, 1);
        c3.add(new MongoDBChangeSetEntry("c2-1.txt", FILE_ADDED));

        dao.save(c3);

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        criteria.addFilterPath("c1-1.txt");

        List<MongoDBChangeSet> actual = dao.findByDriftCriteria(criteria);

        assertChangeSetsMatch("Failed to find change sets with drift path filter", asList(c1, c2), actual);
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

        assertChangeSetsMatch("Failed to find change sets by drift criteria with change set id filter",
            asList(c2), actual);
    }

    private void assertChangeSetsMatch(String msg, List<MongoDBChangeSet> expected, List<MongoDBChangeSet> actual) {
        org.testng.Assert.assertEquals(actual.size(), expected.size(), "The number of change sets differ: " + msg);
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
        assertCollectionMatchesNoOrder(msg + ": " + "change set entries do not match expected entries.",
            expected.getDrifts(), actual.getDrifts(), ignore);
    }

    /**
     * A convenience factory method for creating a change set.
     *
     * @param category A {@link DriftChangeSetCategory}
     * @param version The change set version
     * @param resourceId The owning resource id
     * @param driftConfigId The drift configuration id
     * @return A {@link MongoDBChangeSet}
     */
    MongoDBChangeSet createChangeSet(DriftChangeSetCategory category, int version, int resourceId, int driftConfigId) {
        MongoDBChangeSet changeSet = new MongoDBChangeSet();
        changeSet.setCategory(category);
        changeSet.setVersion(version);
        changeSet.setResourceId(resourceId);
        changeSet.setDriftConfigurationId(driftConfigId);

        return changeSet;
    }

}
