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

import java.util.LinkedList;
import java.util.List;

public class Node {
	private  List <Node> children = new LinkedList<Node>();
	private String key;
	private String value;
	
	public void addChild(Node child) {
		children.add(child);
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	private String padded(int indent) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			str.append(" ");
		}
		return str.toString();
	}
	public void print() {
		print (0,0);
	}
	
	private void print(int indent, int start) {
		String out = padded(indent) + key.substring(start + 1);
		if (value != null && !"".equals(value.trim())) {
			out +=  " = " + value;
		}
		System.out.println(out);
		for (Node child : children) {
			child.print(indent + 2, key.length());
		}
	}
}
