package org.rhq.enterprise.server.plugins.drift.mongodb.entities;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertNotNull;

public class MongoDBChangeSetTest {

    Mongo connection;

    Morphia morphia;

    Datastore ds;

    @BeforeClass
    public void initDB() throws Exception {
        connection = new Mongo("localhost");

        morphia = new Morphia()
            .map(MongoDBChangeSet.class)
            .map(MongoDBChangeSetEntry.class)
            .map(MongoDBFile.class);

        ds = morphia.createDatastore(connection, "rhq");
    }

    @BeforeMethod
    public void clearCollections() throws Exception {
        Query deleteAll = ds.createQuery(MongoDBChangeSet.class);
        ds.delete(deleteAll);
    }

    @Test
    public void saveAndLoadEmptyChangeSet() throws Exception {
        MongoDBChangeSet expected = new MongoDBChangeSet();
        expected.setCategory(COVERAGE);
        expected.setVersion(1);
        expected.setDriftConfigurationId(1);

        ds.save(expected);
        MongoDBChangeSet actual = ds.get(MongoDBChangeSet.class, expected.getObjectId());

        assertNotNull(actual, "Failed to load change set");
        assertPropertiesMatch("Failed to save change set", expected, actual);
    }

    @Test
    public void saveAndLoadChangeSetWithOneEntry() throws Exception {
        MongoDBChangeSet expected = new MongoDBChangeSet();
        expected.setResourceId(10001);
        expected.getDrifts().add(new MongoDBChangeSetEntry("foo", FILE_ADDED));

        ds.save(expected);

        MongoDBChangeSet actual = ds.get(MongoDBChangeSet.class, expected.getObjectId());

        assertNotNull(expected, "Failed to load change set");
        assertPropertiesMatch("Failed to save change set", expected, actual, "drifts");
        assertCollectionMatchesNoOrder(expected.getDrifts(), actual.getDrifts(), "Failed to save change set entries");
    }

}
