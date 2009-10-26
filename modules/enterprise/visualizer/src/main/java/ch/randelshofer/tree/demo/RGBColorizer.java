/**
 * @(#)RGBColorizer.java  1.0  Jan 27, 2008
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
package ch.randelshofer.tree.demo;

import ch.randelshofer.tree.Colorizer;
import java.awt.Color;

/**
 * RGBColorizer converts real numbers in the range from 0 to 1 into
 * colors.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 27, 2008 Created.
 */
public class RGBColorizer implements Colorizer {

    private float[] fractions;
    private int[] reds;
    private int[] greens;
    private int[] blues;

    public RGBColorizer() {
        this(new Color[]{
            new Color(0x64c8ff),
            new Color(0xf5f5f5),
            new Color(0xff9946)
        });
        /*
        this(new Color[]{
            new Color(0x000000),
            new Color(0x0000f5),
            new Color(0xf50000),
            new Color(0xf5f500),
            new Color(0xf5f5f5)
        });*/
    }

    public RGBColorizer(Color[] colors) {
        fractions = new float[colors.length];
        reds = new int[colors.length];
        greens = new int[colors.length];
        blues = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            fractions[i] = i / (float) (colors.length - 1);
            reds[i] = colors[i].getRed();
            greens[i] = colors[i].getGreen();
            blues[i] = colors[i].getBlue();
        }
    }

    public RGBColorizer(float[] fractions, Color[] colors) {
        this.fractions = fractions;
        reds = new int[colors.length];
        greens = new int[colors.length];
        blues = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            reds[i] = colors[i].getRed();
            greens[i] = colors[i].getGreen();
            blues[i] = colors[i].getBlue();
        }
    }

    public Color get(float value) {
        int i;
        for (i = 0; i < fractions.length; i++) {
            if (fractions[i] > value) {
                break;
            }
        }
        if (i == 0) {
            return new Color(reds[i], greens[i], blues[i]);
        } else if (i == fractions.length) {
            return new Color(reds[i - 1], greens[i - 1], blues[i - 1]);
        } else {
            int startR = reds[i - 1];
            int startG = greens[i - 1];
            int startB = blues[i - 1];
            int endR = reds[i];
            int endG = greens[i];
            int endB = blues[i];
            float range = fractions[i] - fractions[i - 1];
            float scaledValue = (value - fractions[i - 1]) / range;
            return new Color(
                    (int) (startR * (1f - scaledValue) + scaledValue * endR),
                    (int) (startG * (1f - scaledValue) + scaledValue * endG),
                    (int) (startB * (1f - scaledValue) + scaledValue * endB));
        }
    }
}
