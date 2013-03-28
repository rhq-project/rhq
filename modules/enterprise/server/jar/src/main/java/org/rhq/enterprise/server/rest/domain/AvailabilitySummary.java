/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.enterprise.server.rest.domain;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.composite.ResourceAvailabilitySummary;

/**
 * Reports the summary of the availability for a resource
 * @author Heiko W. Rupp
 */
@ApiClass("Describes the availability summary for a resource")
@XmlRootElement
public class AvailabilitySummary {

    private long currentTime; // set to the current time when this object was created
    private long upTime;
    private long downTime;
    private long disabledTime;
    private long unknownTime;
    private int failures;
    private int disabled;
    private long lastChange;
    private AvailabilityType current;
    private int resourceId;
    private double disabledPercentage;
    private double upPercentage;
    private long knownTime;
    private long mtbf;
    private long mttr;
    private double downPercentage;


    List<Link> links = new ArrayList<Link>();

    public AvailabilitySummary() {

    }

    public AvailabilitySummary(int resourceId, ResourceAvailabilitySummary ras) {

        this.resourceId = resourceId;
        currentTime = ras.getCurrentTime();
        upTime = ras.getUpTime();
        downTime = ras.getDownTime();
        disabledTime = ras.getDisabledTime();
        unknownTime = ras.getUnknownTime();
        failures = ras.getFailures();
        disabled = ras.getDisabled();
        lastChange = ras.getLastChange().getTime();
        current = ras.getCurrent();

        disabledPercentage = ras.getDisabledPercentage();
        downPercentage = ras.getDownPercentage();
        upPercentage = ras.getUpPercentage();
        knownTime = ras.getKnownTime();
        unknownTime = ras.getUnknownTime();
        mtbf = ras.getMTBF();
        mttr = ras.getMTTR();
    }

    public AvailabilityType getCurrent() {
        return current;
    }

    public void setCurrent(AvailabilityType current) {
        this.current = current;
    }

    public int getDisabled() {
        return disabled;
    }

    public void setDisabled(int disabled) {
        this.disabled = disabled;
    }

    public long getDisabledTime() {
        return disabledTime;
    }

    public void setDisabledTime(long disabledTime) {
        this.disabledTime = disabledTime;
    }

    public long getDownTime() {
        return downTime;
    }

    public void setDownTime(long downTime) {
        this.downTime = downTime;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public long getLastChange() {
        return lastChange;
    }

    public void setLastChange(long lastChange) {
        this.lastChange = lastChange;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public long getUnknownTime() {
        return unknownTime;
    }

    public void setUnknownTime(long unknownTime) {
        this.unknownTime = unknownTime;
    }

    public long getUpTime() {
        return upTime;
    }

    public void setUpTime(long upTime) {
        this.upTime = upTime;
    }

    public double getDisabledPercentage() {
        return disabledPercentage;
    }

    public void setDisabledPercentage(double disabledPercentage) {
        this.disabledPercentage = disabledPercentage;
    }

    public double getDownPercentage() {
        return downPercentage;
    }

    public void setDownPercentage(double downPercentage) {
        this.downPercentage = downPercentage;
    }

    public long getKnownTime() {
        return knownTime;
    }

    public void setKnownTime(long knownTime) {
        this.knownTime = knownTime;
    }

    public long getMtbf() {
        return mtbf;
    }

    public void setMtbf(long mtbf) {
        this.mtbf = mtbf;
    }

    public long getMttr() {
        return mttr;
    }

    public void setMttr(long mttr) {
        this.mttr = mttr;
    }

    public double getUpPercentage() {
        return upPercentage;
    }

    public void setUpPercentage(double upPercentage) {
        this.upPercentage = upPercentage;
    }
}
