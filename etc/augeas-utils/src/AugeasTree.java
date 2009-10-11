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

import java.util.Collections;
import java.util.List;

import net.augeas.Augeas;

/**
 * 
 * @author paji
 * This program prints an augeas tree given a configuration file
 *  to look at. Note it uses the base path provided to determine
 * the lens to load.
 * For example
 * AugeasTree.main("/etc/hosts") will return the following tree
 * 
 * files/etc/hosts
 * #comment[1] = Do not remove the following line, or various programs
 * #comment[2] = that require network functionality will fail.
 * 1
 *   ipaddr = 127.0.0.1
 *   canonical = localhost.localdomain
 *   alias = localhost
 * 2
 *   ipaddr = ::1
 *   canonical = localhost6.localdomain6
 *   alias = localhost6
 * 3
 *   ipaddr = 10.11.231.52
 *   canonical = dhcp231-52.rdu.redhat.com
 *   alias = dhcp231-52
 */
public class AugeasTree {
	private Augeas aug;
	private Node root;
	private  List<String> excludes = Collections.EMPTY_LIST;
	public AugeasTree(String rootLocation, String file,
			List<String> excludableKeys) {
		aug = new Augeas(rootLocation, "", Augeas.SAVE_NEWFILE) ;	
		excludes = excludableKeys;
		root = new Node();
		root.setKey ("/files" + file);
	}
	
	public AugeasTree(String file,
			List<String> excludableKeys) {
		aug = new Augeas(Augeas.SAVE_NEWFILE) ;	
		excludes = excludableKeys;
		root = new Node();
		root.setKey ("/files" + file);
	}	
	public static void main(String args[]) {
        AugeasTree tree;
        if (args.length == 2)  {
            tree = new AugeasTree(args[1], args[0], Collections.EMPTY_LIST);    
        }
        else if (args.length == 1) {
             tree = new AugeasTree(args[0], Collections.EMPTY_LIST);
        }
        else {
            System.out.println ("Usage: java AugeasTree <config file> [<root directory>]");
            System.exit(1);
            return;
        }
		tree.generate();
		tree.print();
	}
	
	private boolean canExclude(String key) {
		for (String exclude : excludes) {
			if (key.contains(exclude)) {
				return true;
			}
		}
		return false;
	}

	private void print() {
		root.print();
	}
	private void generate() {
		generate(root);
	}
	
	private void generate(Node parent) {
		String newRoot = parent.getKey().replace(" ", "\\ ");
		List<String> children = (List<String>)aug.match(newRoot + "/*" );
		
		for (String child :children) {
			if (!canExclude(child)) {
				Node node = new Node();
				node.setKey(child);
				node.setValue((String) aug.get(child.replace(" ", "\\ ")));
				parent.addChild(node);
				generate(node);				
			}
		}
	}	
}
