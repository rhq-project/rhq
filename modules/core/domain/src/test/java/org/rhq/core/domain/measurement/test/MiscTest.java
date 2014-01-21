/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.core.domain.measurement.test;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.NumericType;

/**
 * Just test some misc stuff (that may not even require a data source)
 * @author Heiko W. Rupp
 */
public class MiscTest {


    @Test
    public void testMeasurementScheduleRequest() throws Exception {

        MeasurementScheduleRequest test = new MeasurementScheduleRequest(1,"test",30000,true, DataType.AVAILABILITY,
            NumericType.DYNAMIC);
        assert test.getDataType()==DataType.AVAILABILITY;
        assert test.getRawNumericType()==NumericType.DYNAMIC;
        assert test.getInterval()==30000;

        test = new MeasurementScheduleRequest(1,"test",30000,true, DataType.AVAILABILITY, null);
        assert test.getDataType()==DataType.AVAILABILITY;
        assert test.getRawNumericType()==null;
        assert test.getInterval()==30000;

        test = new MeasurementScheduleRequest(1,"test",30005,true, DataType.MEASUREMENT, NumericType.TRENDSDOWN);
        assert test.getDataType()==DataType.MEASUREMENT;
        assert test.getRawNumericType()==NumericType.TRENDSDOWN;
        assert test.getInterval()==30000; // We loose a bit of precision in the sub-second area

        MeasurementScheduleRequest test2 = new MeasurementScheduleRequest(test);
        assert test2.getDataType()==DataType.MEASUREMENT;
        assert test2.getRawNumericType()==NumericType.TRENDSDOWN;
        assert test2.getInterval()==30000; // We loose a bit of precision in the sub-second area

        test = new MeasurementScheduleRequest(1,"test",30105,true, DataType.MEASUREMENT);
        assert test.getDataType()==DataType.MEASUREMENT;
        assert test.getRawNumericType()==null;
        assert test.getInterval()==30000; // We loose a bit of precision in the sub-second area

        test2 = new MeasurementScheduleRequest(test);
        assert test2.getDataType()==DataType.MEASUREMENT;
        assert test2.getRawNumericType()==null;
        assert test2.getInterval()==30000;

    }
}
