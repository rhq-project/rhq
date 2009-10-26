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
package org.rhq.enterprise.visualizer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;


public class CustomPopupFactory extends PopupFactory {

    private static final int MARGIN = 3;
    private static final Color START_COLOR = Color.WHITE;
    private static final Color END_COLOR = new Color(214, 217, 236);
    private static final Color BORDER_COLOR = Color.BLACK;


    public static interface CustomPopupProvider {
        JComponent getCustomToolTipComponent(int x, int y);
    }


    @Override
    public Popup getPopup(Component owner, Component contents, int x, int y) throws IllegalArgumentException {
        if (owner instanceof CustomPopupProvider) {

            Point p = new Point(x,y);
            SwingUtilities.convertPointFromScreen(p, owner);

            Point compLoc = owner.getLocationOnScreen();

            JComponent toolTip = ((CustomPopupProvider) owner).getCustomToolTipComponent(p.x,p.y); //x - compLoc.x, y - compLoc.y);
            return super.getPopup(owner, toolTip, x, y);
        }

        if (contents instanceof JToolTip) {
            final JToolTip tooltip = (JToolTip) contents;
            JPanel panel = new JPanel(new BorderLayout(), true) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setPaint(new GradientPaint(0, 0, START_COLOR, 0, tooltip.getPreferredSize().height + 2 * MARGIN, END_COLOR));
                    g2d.fillRect(0, 0, tooltip.getPreferredSize().width + 2 * MARGIN, tooltip.getPreferredSize().height + 2 * MARGIN);
                }
            };
            Border border = new CompoundBorder(new LineBorder(BORDER_COLOR), new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
            panel.setBorder(border);
            JLabel label = new JLabel(tooltip.getTipText());
            label.setFont(tooltip.getFont());
            label.setOpaque(false);
            panel.add(label, BorderLayout.CENTER);
            panel.setPreferredSize(new Dimension(tooltip.getPreferredSize().width + 2 * MARGIN, tooltip.getPreferredSize().height + 2 * MARGIN));
            return super.getPopup(owner, panel, x, y);
        }

        return super.getPopup(owner, contents, x, y);
    }


    public static void install() {
        PopupFactory.setSharedInstance(new CustomPopupFactory()); // just do this one time, when you start your application !!
    }
}

