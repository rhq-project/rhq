package org.rhq.enterprise.server.plugins.drift.mongodb;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;
import org.rhq.enterprise.server.plugins.drift.mongodb.dao.ChangeSetDAO;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBFile;
import org.rhq.test.JMockTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.util.List;

import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class MongoDBTest extends JMockTest {

    // Tests need to be disabled when committed/pushed to the remote repo until we get
    // mongodb installed on the hudson slave
    public static final boolean ENABLED = true;

    protected Mongo connection;

    protected Morphia morphia;

    protected Datastore ds;

    @BeforeClass
    public void initDB() throws Exception {
        connection = new Mongo("127.0.0.1");

        morphia = new Morphia().map(MongoDBChangeSet.class).map(MongoDBChangeSetEntry.class).map(MongoDBFile.class);

        ds = morphia.createDatastore(connection, "rhqtest");
    }

    protected void assertChangeSetsMatch(String msg, List<MongoDBChangeSet> expected, List<MongoDBChangeSet> actual) {
        assertEquals(actual.size(), expected.size(), "The number of change sets differ: " + msg);
        int i = 0;
        for (MongoDBChangeSet expectedChangeSet : expected) {
            assertChangeSetMatches("Change sets do not match: " + msg, expectedChangeSet, actual.get(i++));
        }
    }

    protected void assertChangeSetsMatchNoOrder(String msg, List<MongoDBChangeSet> expected,
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

    protected void assertEntriesMatch(String msg, List<MongoDBChangeSetEntry> expected,
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
    protected void assertChangeSetMatches(String msg, MongoDBChangeSet expected, MongoDBChangeSet actual) {
        assertNotNull(actual, msg + ": change set is null");

        String ignore = "drifts";
        assertPropertiesMatch(msg, expected, actual, ignore);

        ignore = "changeSet";
        assertCollectionMatchesNoOrder(msg + ": " + "change set entries do not match expected entries.", expected
            .getDrifts(), actual.getDrifts(), ignore);
    }
}
