/**
 * @(#)Circle.java  1.0  Jan 17, 2008
 *
 * Copyright (c) 2008 Werner Randelshofer
 * Staldenmattweg 2, CH-6405 Immensee, Switzerland
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */
package ch.randelshofer.tree.circlemap;

import java.awt.geom.*;

/**
 * Describes a Circle by its radius and the location of its center.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 17, 2008 Created.
 */
public class Circle {

    public double cx;
    public double cy;
    public double radius;

    /**
     * Creates a new circle at location 0,0 and a radius of 0.
     */
    public Circle() {

    }

    /**
     * Creates a new circle with the specified coordinates and radius.
     *
     */
    public Circle(double cx, double cy, double r) {
        this.cx = cx;
        this.cy = cy;
        this.radius = r;
    }
    /**
     * Returns the radius of the circle.
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Returns the x-coordinate of the center of the circle.
     *
     * @return the x-coordinate of the center.
     */
    public double getCX() {
        return cx;
    }

    /**
     * Returns the y-coordinate of the center of the circle.
     *
     * @return the y-coordinate of the center.
     */
    public double getCY() {
        return cy;
    }

    /**
     * Returns true, if this circle intersects that circle.
     */
    public boolean intersects(Circle that) {
        double dist = /*Math.sqrt(*/
                (this.cx - that.cx) * (this.cx - that.cx) +
                (this.cy - that.cy) * (this.cy - that.cy)/*)*/;

        return dist < (this.radius + that.radius) * (this.radius + that.radius) - 0.001;
        //return dist < (this.radius + that.radius) * (this.radius + that.radius);
    }
    /**
     * Returns true, if this circle intersects that circle.
     */
    public double getIntersectionRadius(Circle that) {
        double dist = /*Math.sqrt(*/
                (this.cx - that.cx) * (this.cx - that.cx) +
                (this.cy - that.cy) * (this.cy - that.cy)/*)*/;

        return Math.sqrt(dist) - that.radius;
    }
    /**
     * Returns true, if this circle contains that circle.
     */
    public boolean contains(Circle that) {
        double dist = Math.sqrt(
                (this.cx - that.cx) * (this.cx - that.cx) +
                (this.cy - that.cy) * (this.cy - that.cy));
        return this.radius >= dist + that.radius;
    }
    /**
     * Returns true, if this circle contains the specified point.
     */
    public boolean contains(double px, double py) {
        double dist = Math.sqrt(
                (this.cx - px) * (this.cx - px) +
                (this.cy - py) * (this.cy - py));
        return this.radius >= dist;
    }
}
