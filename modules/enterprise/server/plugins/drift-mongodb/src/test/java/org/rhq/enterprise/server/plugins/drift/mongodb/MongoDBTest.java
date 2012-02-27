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

package org.rhq.enterprise.server.plugins.drift.mongodb;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBFile;
import org.rhq.test.JMockTest;
import org.testng.annotations.BeforeClass;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
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

    protected void clearCollections(String... collections) {
        for (String collection : collections) {
            ds.getDB().getCollection(collection).drop();
        }
    }

    /**
     * Verifies that the actual list of change sets match the expected list of change sets.
     * The order of the lists is expected to be the same.
     *
     * @param msg
     * @param expected
     * @param actual
     */
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
     * The {@link MongoDBChangeSet#getDrifts() drifts} property is then tested separately
     * using
     * {@link org.rhq.test.AssertUtils#assertCollectionMatchesNoOrder(String, java.util.Collection, java.util.Collection, String...) assertCollectionMatches}.
     *
     * @param msg An error message
     * @param expected The expected change set to test against
     * @param actual The actual change set under test
     * @param ignoredProperties Properties of the MongoDBChangeSet to exclude from comparison
     */
    protected void assertChangeSetMatches(String msg, MongoDBChangeSet expected, MongoDBChangeSet actual,
        String... ignoredProperties) {
        assertNotNull(actual, msg + ": change set is null");

        List<String> ignore = new ArrayList<String>(asList(ignoredProperties));
        ignore.add("drifts");
        assertPropertiesMatch(msg, expected, actual, ignore);

        assertCollectionMatchesNoOrder(msg + ": " + "change set entries do not match expected entries.", expected
                .getDrifts(), actual.getDrifts(), "changeSet", "ctime");
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
