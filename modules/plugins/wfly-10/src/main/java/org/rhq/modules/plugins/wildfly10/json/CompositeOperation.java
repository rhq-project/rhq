/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10.json;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A composite operation contains multiple steps of a
 * task that requires many individual operations to complete
 * @author Heiko W. Rupp
 */
public class CompositeOperation extends Operation {

    @JsonProperty
    List<Operation> steps = new ArrayList<Operation>();

    public CompositeOperation(List<Operation> steps) {
        super("composite", new Address());
        this.steps = steps;
    }

    public CompositeOperation() {
        super("composite", new Address());
    }

    public void addStep(Operation step) {
        steps.add(step);
    }

    /**
     * @deprecated as of 4.13. Not used
     */
    @Deprecated
    public void setOperationHeaders(OperationHeaders headers) {
    }

    private class OperationHeaders {
    }

    public int numberOfSteps() {
        return steps.size();
    }

    public Operation step(int i) {
        return steps.get(i);
    }

    @Override
    public String toString() {
        return "CompositeOperation{steps=" + steps.size() + "}";
    }
}
