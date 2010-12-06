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
package org.rhq.enterprise.gui.coregui.client.util;

import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;

/**
 * @author Greg Hinkle
 */
public class CanvasUtility {

    public static void blink(Canvas canvas) {
        canvas.animateFade(10, new FadeAnimationCallback(3,canvas,false));
    }

    private static class FadeAnimationCallback implements AnimationCallback {
        int count;
        Canvas canvas;
        boolean fadeOut;

        private FadeAnimationCallback(int count, Canvas canvas, boolean fadeOut) {
            this.count = count;
            this.canvas = canvas;
            this.fadeOut = fadeOut;
        }

        public void execute(boolean b) {
            if (count > 0) {
                canvas.animateFade(fadeOut ? 10 : 100, new FadeAnimationCallback(count-1,canvas, !fadeOut));
            }
        }
    }

}
