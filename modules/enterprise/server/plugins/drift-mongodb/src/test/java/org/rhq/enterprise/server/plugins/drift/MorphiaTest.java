package org.rhq.enterprise.server.plugins.drift;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.DB;
import com.mongodb.Mongo;

import org.testng.annotations.Test;

import org.rhq.core.domain.drift.DriftChangeSet;

public class MorphiaTest {

    @Test
    public void connectToMongoDB() throws Exception {
        Mongo connection = new Mongo("localhost");
        DB db = connection.getDB("test");

        Morphia morphia = new Morphia();
        morphia.map(DriftChangeSet.class);

        Datastore ds = morphia.createDatastore(connection, "test");

//        DriftChangeSet changeSet = new DriftChangeSet(null, 1, COVERAGE);
//        ds.save(changeSet);
    }

}
