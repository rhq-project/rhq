/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.core.db.upgrade;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ContentSourceConfigurationObfuscationUpgradeTask extends AbstractConfigurationObfuscationUpgradeTask {

    @Override
    protected Map<Integer, Integer> getConfigurationIdConfigurationDefinitionIdPairs() throws SQLException {
        Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
        String sql =
            "SELECT cs.configuration_id, ct.source_config_def_id FROM"
                + " rhq_content_source cs, rhq_content_source_type ct WHERE" + " cs.content_source_type_id = ct.id";

        List<Object[]> results = databaseType.executeSelectSql(connection, sql);

        for (Object[] row : results) {
            Integer configId = (Integer) row[0];
            Integer configDefId = (Integer) row[1];

            ret.put(configId, configDefId);
        }

        return ret;
    }
    
    @Override
    protected String getEntityTypeDescription() {
        return "Content Source";
    }
}
