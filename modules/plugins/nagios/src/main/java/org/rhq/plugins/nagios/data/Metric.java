package org.rhq.plugins.nagios.data;
/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

/**
 * This class implements a Nagios metric
 *
 * @author Alexander Kiefer
 */
public class Metric
{
	/**
	 * Each metric has an id and a value
	 */
	private String id;
	private String value;

	/**
	 * default Constructor is private because it should not be used
	 */
	private Metric()
	{

	}

	public Metric(String id, String value)
	{
		this.id = id;
		this.value = value;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Metric");
        sb.append("{id='").append(id).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
