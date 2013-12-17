package org.rhq.plugins.database;

/**
 * @author Thomas Segismont
 */
public class NonPoolingCustomTableComponent extends CustomTableComponent {

    @Override
    public boolean supportsConnectionPooling() {
        return false;
    }

    @Override
    public PooledConnectionProvider getPooledConnectionProvider() {
        return null;
    }
}
