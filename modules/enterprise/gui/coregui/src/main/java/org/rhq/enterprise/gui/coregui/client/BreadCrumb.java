/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client;

import org.rhq.enterprise.gui.coregui.client.places.Place;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Hyperlink;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.HStack;

import java.util.ArrayList;

/**
 * @author Greg Hinkle
 */
public class BreadCrumb extends HTMLPane {

    private ArrayList<Place> trail = new ArrayList<Place>();

    public BreadCrumb() {

        setHeight(28);
        setBackgroundColor("#E6E3E3");
        setPadding(5);
        setOverflow(Overflow.CLIP_V);
    }


    public void initialize(String historyPath) {
        String[] ids =historyPath.split("\\/");
        
        for (String id : ids) {
            trail.add(new Place(id,"?"));
        }

    }

    public void verify(String historyPath) {
        String[] ids = historyPath.split("\\/");

        for (int i = 0; i < ids.length; i++) {
            if (trail.size() > i && !trail.get(i).getId().equals(ids[i])) {
                trail = new ArrayList(trail.subList(0,i));
                trail.add(new Place(ids[i], "?"));
            } else {
                if (trail.size() > i) {
                    trail.set(i,new Place(ids[i],"?"));
                } else {
                    trail.add(new Place(ids[i],"?"));
                }
            }
        }
    }



    public void setPlace(Place place) {
        trail.clear();
        addPlace(place);
    }

    public void addPlace(Place place) {
        trail.add(place);

        refresh();
    }

    public void refresh() {
        String content = "";
        boolean first = true;

        String path = "";
        for (Place place : trail) {
            if (!first) {
                content += " > ";
            }
            first = false;
            if (path.length() > 0) {
                path += "/";
            }
            path += place.getId();


            content += "<a class=\"BreadCrumb\" href=\"#" + path + "\">" + place.getName() + "</a>";
        }

        setContents(content);

        History.newItem(path);
        if (!trail.isEmpty())
            Window.setTitle("RHQ: " + trail.get(trail.size()-1));

        redraw();
    }


    public ArrayList<Place> getTrail() {
        return trail;
    }

}
