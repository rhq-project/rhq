package org.rhq.cassandra.schema;

/**
 * This class is test hook to facilitate writing tests for schema changes, updates, etc. It can be subclassed so that
 * tests can fully control what update files are applied. {@link org.rhq.cassandra.schema.SchemaManager SchemaManager}
 * uses this class by default. Call
 * {@link org.rhq.cassandra.schema.SchemaManager#setUpdateFolderFactory(UpdateFolderFactory) setUpdateFolderFactory} to
 * override.
 *
 * @author John Sanda
 */
public class UpdateFolderFactory {

    public UpdateFolder newUpdateFolder(String folder) throws Exception {
        return new UpdateFolder(folder);
    }

}
