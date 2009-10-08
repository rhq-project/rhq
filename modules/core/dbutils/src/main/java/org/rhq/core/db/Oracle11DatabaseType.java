package org.rhq.core.db;

/**
 * Oracle11 database which extends the Oracle10 database.
 *
 * @author John Mazzitelli
 */
public class Oracle11DatabaseType extends Oracle10DatabaseType {
    @Override
    public String getName() {
        return OracleDatabaseType.VENDOR + "11";
    }

    @Override
    public String getVersion() {
        return "11";
    }
}
