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
package org.rhq.helpers.ui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/** Is a simple ui that generates a very simple managed
 *  resource with files and directories that can be managed/monitored
 *  by an RHQ plugin.
 *  
 *  @author Simeon Pinder
 */
public class FileHive extends JFrame {
    /**
     * @param args
     */
    public static void main(String[] args) {
        new FileHive();
    }

    public FileHive() {
        //create ui layout
        initializeUi();
    }

    /** Responsible for putting together the layout components.
     * 
     */
    private void initializeUi() {

        setTitle("FileHive:");
        getContentPane().setLayout(new BorderLayout());
        // top panel definition
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(LineBorder.createGrayLineBorder());

        // center
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        //        // build center panel
        //        buildCenterPanel(center);

        // final component layout
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        this.setSize(500, 500);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        setVisible(true);
    }
}
