/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.domain.util;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.FilterTranslator;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;

/**
 * A query translator factory to be set in the hibernate configuration to report the usage of JOIN FETCH with limit.
 *
 * @author Lukas Krejci
 */
public class JoinFetchReportingQueryTranslatorFactory implements QueryTranslatorFactory {
    @Override
    public QueryTranslator createQueryTranslator(String queryIdentifier, String queryString, Map filters,
        SessionFactoryImplementor factory) {
        return new JoinFetchReportingQueryTranslator(queryIdentifier, queryString, filters, factory);
    }

    @Override
    public FilterTranslator createFilterTranslator(String queryIdentifier, String queryString, Map filters,
        SessionFactoryImplementor factory) {
        return new JoinFetchReportingQueryTranslator(queryIdentifier, queryString, filters, factory);
    }
}
