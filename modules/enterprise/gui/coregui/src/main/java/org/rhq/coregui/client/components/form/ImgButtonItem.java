/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.coregui.client.components.form;

import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.form.fields.CanvasItem;

/**
 * Image Button Item is a convenience class to use ImgButton in a DynamicForm.
 *
 * @author Mike Thompson
 */
public class ImgButtonItem extends CanvasItem{

    private Canvas canvas = new Canvas();
    private ImgButton imageButton;

    public ImgButtonItem(){
        construct(null);
    }

    public ImgButtonItem(String imagePath) {
        construct(imagePath);
    }

    private void construct(String imagePath) {
        imageButton = new ImgButton();

        if(null != imagePath){
           imageButton.setIcon(imagePath);
        }
        imageButton.setHeight("30px");
        canvas.addChild(imageButton);
        setShowTitle(false);

        setCanvas(canvas);
        setHeight(30);
        setTitleVAlign(VerticalAlignment.TOP);
    }

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    public ImgButton getImageButton() {
        return imageButton;
    }
}
