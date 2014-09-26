/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.util;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.RetryPolicy;

import org.rhq.core.util.obfuscation.PicketBoxObfuscator;

/**
 * This class should generally be used for creating a {@link Cluster} instead of
 * {@link Cluster.Builder}. It ensures that the cluster and session objects are configured
 * in a consistent fashion everywhere they are used.
 *
 * @author John Sanda
 */
public class ClusterBuilder {

    private Cluster.Builder builder = Cluster.builder();

    private ProtocolOptions.Compression compression;

    /**
     * @see Cluster.Builder#addContactPoints(String...)
     */
    public ClusterBuilder addContactPoints(String... addresses) {
        builder.addContactPoints(addresses);
        return this;
    }

    /**
     * @see com.datastax.driver.core.Cluster.Builder#poolingOptions()
     */
    public PoolingOptions poolingOptions() {
        return builder.poolingOptions();
    }

    /**
     * @see Cluster.Builder#withCredentials(String, String)
     */
    public ClusterBuilder withCredentials(String username, String password) {
        builder.withCredentials(username, password);
        return this;
    }

    /**
     * @see Cluster.Builder#withCredentials(String, String)
     */
    public ClusterBuilder withCredentialsObfuscated(String username, String obfuscatedPassword) {
        builder.withCredentials(username, PicketBoxObfuscator.decode(obfuscatedPassword));
        return this;
    }

    /**
     * This method will throw an IllegalArgumentException if you try to use snappy
     * compression while running on an IBM JRE. See <a href="https://bugzilla.redhat.com/show_bug.cgi?id=907485">BZ 907485</a>
     * for details.
     *
     * @see Cluster.Builder#withCompression(com.datastax.driver.core.ProtocolOptions.Compression)
     */
    public ClusterBuilder withCompression(ProtocolOptions.Compression compression) {
        if (isIBMJRE() && compression == ProtocolOptions.Compression.SNAPPY) {
            throw new IllegalArgumentException(compression.name() + " compression cannot be used with an IBM JRE. " +
                "See https://bugzilla.redhat.com/show_bug.cgi?id=907485 for details.");
        }
        this.compression = compression;
        builder.withCompression(compression);
        return this;
    }

    /**
     * @see Cluster.Builder#withPort(int)
     */
    public ClusterBuilder withPort(int port) {
        builder.withPort(port);
        return this;
    }

    /**
     * @see Cluster.Builder#withLoadBalancingPolicy(com.datastax.driver.core.policies.LoadBalancingPolicy)
     */
    public ClusterBuilder withLoadBalancingPolicy(LoadBalancingPolicy policy) {
        builder.withLoadBalancingPolicy(policy);
        return this;
    }

    /**
     * @see Cluster.Builder#withRetryPolicy(com.datastax.driver.core.policies.RetryPolicy)
     */
    public ClusterBuilder withRetryPolicy(RetryPolicy policy) {
        builder.withRetryPolicy(policy);
        return this;
    }

    /**
     * @see com.datastax.driver.core.Cluster.Builder#build()
     */
    public Cluster build() {
//        if (compression == null && !isIBMJRE()) {
//            builder.withCompression(ProtocolOptions.Compression.SNAPPY);
//        }
//        builder.withoutMetrics();
        builder.withCompression(ProtocolOptions.Compression.NONE);
        return builder.build();
    }

    private boolean isIBMJRE() {
        return System.getProperty("java.vm.vendor").startsWith("IBM");
    }
}
