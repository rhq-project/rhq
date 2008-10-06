 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pluginapi.operation;

import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * Test {@link OperationResult}
 *
 * @author John Mazzitelli
 */
@Test
public class OperationResultTest {
    public void testComplex() {
        OperationResult result;

        result = new OperationResult();
        result.getComplexResults().setId(111);
        result.getComplexResults().setVersion(222);
        result.getComplexResults().setNotes("my notes");
        result.getComplexResults().put(new PropertySimple("foo", "bar"));
        result.getComplexResults().put(new PropertyMap("foo", new PropertySimple("a", "b")));
        result.getComplexResults().put(new PropertyList("foo", new PropertySimple("c", "d")));
        Configuration config = result.getComplexResults().deepCopy();

        assert config != null;
        assert config != result.getComplexResults();
        assert config.equals(result.getComplexResults());
        assert config.getId() == 111;
        assert config.getVersion() == 222;
        assert config.getNotes().equals("my notes");
    }

    public void testSimple() {
        OperationResult result;

        result = new OperationResult("a simple result");
        assert result.getSimpleResult().equals("a simple result");
        assert result.getComplexResults().getSimpleValue(OperationResult.SIMPLE_OPERATION_RESULT_NAME, "").equals(
            "a simple result");
        result = new OperationResult();
        result.setSimpleResult("a simple result2");
        assert result.getSimpleResult().equals("a simple result2");
        assert result.getComplexResults().getSimpleValue(OperationResult.SIMPLE_OPERATION_RESULT_NAME, "").equals(
            "a simple result2");
    }
}