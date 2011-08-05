package org.rhq.enterprise.server.plugins.drift.mongodb.dao;

import java.util.List;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;

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
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertNotNull;

public class ChangeSetDAOTest {

    static final boolean ENABLED = true;

    Mongo connection;

    Morphia morphia;

    Datastore ds;

    ChangeSetDAO dao;

    @BeforeClass
    public void initDB() throws Exception {
        connection = new Mongo("localhost");

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

        dao.save(expected);
        MongoDBChangeSet actual = dao.get(expected.getObjectId());

        assertChangeSetMatches("Failed to save/load change set with no entries", expected, actual);
    }

    @Test(enabled = ENABLED)
    public void saveAndLoadChangeSetWithOneEntry() throws Exception {
        MongoDBChangeSet expected = new MongoDBChangeSet();
        expected.setCategory(DRIFT);
        expected.setResourceId(1);
        expected.setDriftConfigurationId(1);
        expected.setVersion(1);
        expected.add(new MongoDBChangeSetEntry("foo", FILE_ADDED));

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

    @Test
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

//    MongoDBChangeSetEntry createEntry(String path, DriftCategory category) {
//        MongoDBChangeSetEntry entry = new MongoDBChangeSetEntry(path, category);
//        // TODO init oldFile
//    }

}
