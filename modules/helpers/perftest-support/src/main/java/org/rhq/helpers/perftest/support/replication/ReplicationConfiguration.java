/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.replication;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A configuration object to describe the replication.
 *
 * @author Lukas Krejci
 */
public class ReplicationConfiguration {

    private List<Class<?>> entities;
    
    private Map<Class<?>, String> limitingSql;
    
    private NextIdProvider nextIdProvider;
    
    private ReplicaModifier modifier;

    /**
     * @return the entities
     */
    public List<Class<?>> getEntities() {
        if (entities == null) {
            entities = new ArrayList<Class<?>>();
        }
        return entities;
    }

    /**
     * @param entities the entities to set
     */
    public void setEntities(List<Class<?>> entities) {
        this.entities = entities;
    }

    /**
     * @return the limitingSql
     */
    public Map<Class<?>, String> getLimitingSql() {
        if (limitingSql == null) {
            limitingSql = new HashMap<Class<?>, String>();
        }
        return limitingSql;
    }

    /**
     * @param limitingSql the limitingSql to set
     */
    public void setLimitingSql(Map<Class<?>, String> limitingSql) {
        this.limitingSql = limitingSql;
    }

    /**
     * @return the nextIdProvider
     */
    public NextIdProvider getNextIdProvider() {
        return nextIdProvider;
    }

    /**
     * @param nextIdProvider the nextIdProvider to set
     */
    public void setNextIdProvider(NextIdProvider nextIdProvider) {
        this.nextIdProvider = nextIdProvider;
    }

    /**
     * @return the modifier
     */
    public ReplicaModifier getModifier() {
        return modifier;
    }

    /**
     * @param modifier the modifier to set
     */
    public void setModifier(ReplicaModifier modifier) {
        this.modifier = modifier;
    }
}
