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
package org.rhq.core.domain.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.transfer.EventReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * This tests to make sure things are serializable and externalizable.
 *
 * @author John Mazzitelli
 */
@Test
public class SerializableTest {
    public void testSerialization() {
        List<Serializable> objects = new ArrayList<Serializable>();
        Serializable[] simpleObjects = new Serializable[] { new AvailabilityReport("fakeAgent"), new PackageType(),
            new Architecture(), new Repo(), new ContentServiceRequest(),
            new ContentSource(), new ContentSourceType(), new InstalledPackage(), new Package(),
            new PackageInstallationStep(), new PackageType(), new PackageVersion() };
        objects.addAll(Arrays.asList(simpleObjects));

        ResourceType resourceType = new ResourceType("name", "plugin", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource("key", "name", resourceType);

        objects.add(resourceType);
        objects.add(resource);

        for (Serializable obj : objects) {
            ensureSerializable(obj);
        }
    }

    public void testMeasurementReport() {
        MeasurementReport report = new MeasurementReport();
        report.setCollectionTime(1111);

        CallTimeData callTimeData = new CallTimeData(new MeasurementScheduleRequest(1, "1", 1, true, DataType.CALLTIME));
        callTimeData.addCallData("dest1", new Date(1111), 1);
        callTimeData.addCallData("dest1", new Date(1112), 2);

        report.addData(callTimeData);

        report.addData(new MeasurementDataNumeric(2, new MeasurementScheduleRequest(2, "2", 2, true,
            DataType.MEASUREMENT), new Double(2.2)));

        report.addData(new MeasurementDataTrait(3, new MeasurementScheduleRequest(3, "3", 3, true, DataType.TRAIT),
            "trait3"));

        MeasurementReport copy = ensureSerializable(report);
        assert copy.getDataCount() == 3 : "-->" + copy.getDataCount();
        assert copy.getCallTimeData().size() == 1 : "-->" + copy.getCallTimeData();
        assert copy.getNumericData().size() == 1 : "-->" + copy.getNumericData();
        assert copy.getTraitData().size() == 1 : "-->" + copy.getTraitData();
    }

    public void testAvailabilityReport() {
        assert ensureSerializable(new AvailabilityReport(true, "the-agent-name")).isChangesOnlyReport();
        assert !ensureSerializable(new AvailabilityReport(false, "the-agent-name")).isChangesOnlyReport();
        assert !ensureSerializable(new AvailabilityReport("the-agent-name")).isChangesOnlyReport();
        assert ensureSerializable(new AvailabilityReport("the-agent-name")).getAgentName().equals("the-agent-name");

        AvailabilityReport report;

        report = new AvailabilityReport("the-agent-name");
        report.addAvailability(new Availability(new Resource(), new Date(), AvailabilityType.UP));
        assert ensureSerializable(report).getResourceAvailability().size() == 1;

        report = new AvailabilityReport("the-agent-name");
        report.addAvailability(new Availability(new Resource(), new Date(), null));
        assert ensureSerializable(report).getResourceAvailability().size() == 1;

        report = new AvailabilityReport("the-agent-name");
        report.addAvailability(new Availability(new Resource(), new Date(), AvailabilityType.UP));
        report.addAvailability(new Availability(new Resource(), new Date(), AvailabilityType.DOWN));
        report.addAvailability(new Availability(new Resource(), new Date(), null));
        assert ensureSerializable(report).getResourceAvailability().size() == 3;
    }

    public void testEventReport() {
        ResourceType resourceType = new ResourceType("foo", "foo", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource(1);
        EventDefinition eventDefinition = new EventDefinition(resourceType, "foo");
        EventSource eventSource = new EventSource("foo", eventDefinition, resource);
        EventReport report = new EventReport(2, 2);
        report.addEvent(new Event("foo", "foo", 0, EventSeverity.DEBUG, ""), eventSource);
        assert ensureSerializable(report).getEvents().size() == 1;
        report.addEvent(new Event("foo", "foo", 1, EventSeverity.DEBUG, ""), eventSource);
        assert ensureSerializable(report).getEvents().size() == 1; // only one event source still!
        assert ensureSerializable(report).getEvents().values().iterator().next().size() == 2;
    }

    /**
     * Tests should call this to make sure the given object is serializable. This method will serialize the given
     * object, immediately deserialize it and return that deserialized object - essentially returning a copy of the
     * given object. Caller can test equality of the returned object with the given object if appropriate (if equals()
     * is implemented, the returned object should be equal to the given object).
     *
     * @param  object
     *
     * @return a copy of object
     */
    private <T> T ensureSerializable(T object) {
        byte[] bytes = null;

        try {
            bytes = serialize(object);
        } catch (Exception e) {
            assert false : "Cannot serialize object [" + object + "]: " + e;
        }

        Object copy = null;

        try {
            copy = deserialize(bytes);
        } catch (Exception e) {
            assert false : "Cannot deserialize object [" + object + "]: " + e;
        }

        return (T) copy;
    }

    private byte[] serialize(Object object) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(byteStream);

        oos.writeObject(object);
        oos.close();

        return byteStream.toByteArray();
    }

    private Object deserialize(byte[] serializedData) throws Exception {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(serializedData);
        ObjectInputStream ois = new ObjectInputStream(byteStream);
        Object retObject = ois.readObject();

        ois.close();

        return retObject;
    }
}