/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.core.db.builders;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.FeatureNotSupportedException;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;

import java.util.HashMap;

/**
 * @author Robert Buck
 */
public abstract class CreateSequenceExprBuilder {

    public static final String KEY_SEQ_NAME = "SEQ_NAME";
    public static final String KEY_SEQ_START = "SEQ_START";
    public static final String KEY_SEQ_INCREMENT = "SEQ_INCREMENT";
    public static final String KEY_SEQ_CACHE_SIZE = "SEQ_CACHE_SIZE";

    public static CreateSequenceExprBuilder getBuilder(DatabaseType type) {
        return getBuilder(type.getVendor());
    }

    public static CreateSequenceExprBuilder getBuilder(String type) {
        if (OracleDatabaseType.VENDOR.equals(type)) {
            return new OracleInnerBuilder();
        }
        if (SQLServerDatabaseType.VENDOR_NAME.equals(type)) {
            return new SqlServerInnerBuilder();
        }
        if (PostgresqlDatabaseType.VENDOR_NAME.equals(type)) {
            return new PostgresInnerBuilder();
        }
        if (H2DatabaseType.VENDOR_NAME.equals(type)) {
            return new H2InnerBuilder();
        }
        throw new UnsupportedOperationException("Cannot create a CREATE SEQUENCE builder for the requested database type: "
                + type);
    }

    /**
     * Indicates that the NOCACHE option should be used.
     */
    public static final int USE_SEQID_NOCACHE_SIZE = 0;

    /**
     * Get the default factory sequence id cache size.
     *
     * @return the factory sequence id cache size
     */
    public int getFactorySeqIdCacheSizeLiteral() {
        return 0; // this is the global default
    }

    protected String getSeqIdNoCacheLiteral() {
        return "";
    }

    protected String getSeqIdCacheLiteral() {
        return "CACHE";
    }

    protected abstract StringBuilder appendCreateSeqStem(HashMap<String, Object> terms, StringBuilder builder);

    protected StringBuilder appendSeqIdNoCacheTerms(HashMap<String, Object> terms, StringBuilder builder) {
        return builder.append(" ").append(getSeqIdNoCacheLiteral());
    }

    protected StringBuilder appendSeqIdCacheTerms(HashMap<String, Object> terms, StringBuilder builder) {
        return builder.append(" ").append(getSeqIdCacheLiteral()).append(" ").append(terms.get(KEY_SEQ_CACHE_SIZE));
    }

    protected StringBuilder appendCreateSeqCacheSize(HashMap<String, Object> terms, StringBuilder builder) {
        final int specifiedCacheSize = (Integer) terms.get(KEY_SEQ_CACHE_SIZE);
        // If the user specifies a cache size that is negative we use the factory
        // default cache size. If the factory default cache size is identical to
        // the specified value we do not need to append a cache term.
        if ((specifiedCacheSize >= USE_SEQID_NOCACHE_SIZE) && (specifiedCacheSize != getFactorySeqIdCacheSizeLiteral())) {
            if (USE_SEQID_NOCACHE_SIZE == specifiedCacheSize) {
                // values of zero map to 'no cache'...
                appendSeqIdNoCacheTerms(terms, builder);
            } else {
                // otherwise use specified value, even if they are less than factory defaults...
                appendSeqIdCacheTerms(terms, builder);
            }
        } else {
            // use to factory defaults...
        }
        return builder;
    }

    public String build(HashMap<String, Object> terms) {
        StringBuilder builder = new StringBuilder();
        appendCreateSeqStem(terms, builder);
        appendCreateSeqCacheSize(terms, builder);
        return builder.toString();
    }

    /**
     * @author Robert Buck
     */
    private static class PostgresInnerBuilder extends CreateSequenceExprBuilder {

        @Override
        public int getFactorySeqIdCacheSizeLiteral() {
            return 1; // believe it or not!
        }

        @Override
        protected StringBuilder appendCreateSeqStem(HashMap<String, Object> terms, StringBuilder builder) {
            builder.append("CREATE SEQUENCE ").append(terms.get(KEY_SEQ_NAME))
                    .append(" START ").append(terms.get(KEY_SEQ_START))
                    .append(" INCREMENT ").append(terms.get(KEY_SEQ_INCREMENT));
            return builder;
        }

        /**
         * Postgres does not support NO CACHE terms, so we fake it out by setting the
         * CACHE size to one. Yes, no matter what postgres will lose at least one id
         * when the database restarts. One is the minimum value as indicated by its
         * documentation, and its the default.
         *
         * @param builder the builder to append to
         * @return the string builder with ' CACHE 1' appended to it.
         */
        @Override
        protected StringBuilder appendSeqIdNoCacheTerms(HashMap<String, Object> terms, StringBuilder builder) {
            // We do not pass in getFactorySeqIdCacheSize() here because if they ever
            // changed the default we don't want to break the behavior intended here.
            terms.put(KEY_SEQ_CACHE_SIZE, 1);
            return appendSeqIdCacheTerms(terms, builder);
        }
    }

    /**
     * @author Robert Buck
     */
    private static class OracleInnerBuilder extends CreateSequenceExprBuilder {

        @Override
        public int getFactorySeqIdCacheSizeLiteral() {
            return 32;
        }

        @Override
        protected StringBuilder appendCreateSeqStem(HashMap<String, Object> terms, StringBuilder builder) {
            builder.append("CREATE SEQUENCE ").append(terms.get(KEY_SEQ_NAME))
                    .append(" START WITH ").append(terms.get(KEY_SEQ_START))
                    .append(" INCREMENT BY ").append(terms.get(KEY_SEQ_INCREMENT))
                    .append(" NOMAXVALUE NOCYCLE");
            return builder;
        }

        @Override
        protected StringBuilder appendSeqIdNoCacheTerms(HashMap<String, Object> terms, StringBuilder builder) {
            return builder.append(" NOCACHE");
        }
    }

    /**
     * @author Robert Buck
     */
    private static class SqlServerInnerBuilder extends CreateSequenceExprBuilder {

        public static final String SEQ_SUFFIX = "_ID_SEQ";

        @Override
        public int getFactorySeqIdCacheSizeLiteral() {
            return 10; // for identity columns only; not applicable for Denali! In Denali the default is 50
        }

        @Override
        protected StringBuilder appendCreateSeqStem(HashMap<String, Object> terms, StringBuilder builder) {
            final String name = ((String) terms.get(CreateSequenceExprBuilder.KEY_SEQ_NAME)).toUpperCase();
            if (!name.endsWith(SEQ_SUFFIX)) {
                throw new FeatureNotSupportedException(SQLServerDatabaseType.SEQ_ERROR_MSG);
            }
            String tableName = name.substring(0, name.length() - SEQ_SUFFIX.length());

            builder.append("ALTER TABLE ").append(tableName)
                    .append(" ALTER COLUMN ID IDENTITY( ").append(terms.get(CreateSequenceExprBuilder.KEY_SEQ_START))
                    .append(", ").append(terms.get(CreateSequenceExprBuilder.KEY_SEQ_INCREMENT))
                    .append(")");
            return builder;
        }

        @Override
        protected StringBuilder appendCreateSeqCacheSize(HashMap<String, Object> terms, StringBuilder builder) {
            // Only sql server 2011 (Denali) supports SEQUENCE..CACHE constructs,
            // and this change does not introduce support for Denali. So we only
            // apply the stem. This poses an interesting problem, how should the
            // upgrades work for future implementations where the underlying
            // SQL implementation undergoes such a fundamental change to the
            // language?
            return builder;
        }

        @Override
        protected StringBuilder appendSeqIdNoCacheTerms(HashMap<String, Object> terms, StringBuilder builder) {
            // Ditto, see comments above...
            return builder;
        }
    }

    /**
     * @author Robert Buck
     */
    private static class H2InnerBuilder extends CreateSequenceExprBuilder {

        @Override
        public int getFactorySeqIdCacheSizeLiteral() {
            return 32;
        }

        @Override
        protected StringBuilder appendCreateSeqStem(HashMap<String, Object> terms, StringBuilder builder) {
            builder.append("CREATE SEQUENCE ").append(terms.get(KEY_SEQ_NAME))
                    .append(" START WITH ").append(terms.get(KEY_SEQ_START))
                    .append(" INCREMENT BY ").append(terms.get(KEY_SEQ_INCREMENT));
            return builder;
        }

        @Override
        protected StringBuilder appendSeqIdNoCacheTerms(HashMap<String, Object> terms, StringBuilder builder) {
            // We do not pass in getFactorySeqIdCacheSize() here because if they ever
            // changed the default we don't want to break the behavior intended here.
            terms.put(KEY_SEQ_CACHE_SIZE, 1);
            return appendSeqIdCacheTerms(terms, builder);
        }
    }
}

