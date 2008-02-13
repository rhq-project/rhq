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
package org.rhq.enterprise.gui.image;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class ImageUtil {
    /**
     * Creates a copy of an image and during the process converts the image to an indexed color image.
     *
     * @param  image Image to convert.
     *
     * @return BufferedImage with an indexed color pallette
     */
    public static BufferedImage convertToIndexColorImage(BufferedImage image) {
        byte[][] clrs = ImageUtil.scrapeColors(image);

        IndexColorModel model = new IndexColorModel(8, clrs[0].length, clrs[0], clrs[1], clrs[2], 0);
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_INDEXED,
            model);

        ImageUtil.copyPixels(image, copy);

        return copy;
    }

    /**
     * Copy pixels from an RGB color model to an Indexed color model.
     */
    private static void copyPixels(BufferedImage src, BufferedImage dst, byte[][] rgb) {
        // Get Destination Index Colors
        IndexColorModel model = (IndexColorModel) dst.getColorModel();

        int size = model.getMapSize();
        byte[] r = new byte[size];
        byte[] g = new byte[size];
        byte[] b = new byte[size];

        model.getReds(r);
        model.getGreens(g);
        model.getBlues(b);

        // Get Source Pixels
        int i;
        Raster srcRaster = src.getRaster();
        int[][] srcPixels = new int[srcRaster.getNumBands()][];

        for (int band = 0; band < srcRaster.getNumBands(); band++) {
            srcPixels[band] = srcRaster.getSamples(0, 0, srcRaster.getWidth(), srcRaster.getHeight(), band,
                (int[]) null);
        }

        // Get Destination Pixels
        WritableRaster dstRaster = dst.getRaster();
        int[] dstPixels = new int[dstRaster.getWidth() * dstRaster.getHeight()];

        // Index Pixels
        for (int pixel = 0; pixel < dstPixels.length; pixel++) {
            // Find the Color in the Index
            for (i = 0; i < r.length; i++) {
                byte red = (byte) srcPixels[0][pixel];
                byte green = (byte) srcPixels[1][pixel];
                byte blue = (byte) srcPixels[2][pixel];

                if ((red == r[i]) && (green == g[i]) && (blue == b[i])) {
                    dstPixels[pixel] = i;
                    break;
                }
            }

            if (i == r.length) {
                System.out.println("Missing Color");
            }
        }

        dstRaster.setPixels(0, 0, dstRaster.getWidth(), dstRaster.getHeight(), dstPixels);
    }

    private static void copyPixels(BufferedImage src, BufferedImage dst) {
        // Get Destination Index Colors
        IndexColorModel model = (IndexColorModel) dst.getColorModel();

        int size = model.getMapSize();
        byte[] r = new byte[size];
        byte[] g = new byte[size];
        byte[] b = new byte[size];

        model.getReds(r);
        model.getGreens(g);
        model.getBlues(b);

        // Get Source Pixels
        int i;
        Raster srcRaster = src.getRaster();
        int[][] srcPixels = new int[srcRaster.getNumBands()][];

        for (int band = 0; band < srcRaster.getNumBands(); band++) {
            srcPixels[band] = srcRaster.getSamples(0, 0, srcRaster.getWidth(), srcRaster.getHeight(), band,
                (int[]) null);
        }

        // Get Destination Pixels
        WritableRaster dstRaster = dst.getRaster();
        int[] dstPixels = new int[dstRaster.getWidth() * dstRaster.getHeight()];

        // Index Pixels
        for (int pixel = 0; pixel < dstPixels.length; pixel++) {
            // Find the Color in the Index
            for (i = 0; i < r.length; i++) {
                byte red = (byte) srcPixels[0][pixel];
                byte green = (byte) srcPixels[1][pixel];
                byte blue = (byte) srcPixels[2][pixel];

                if ((red == r[i]) && (green == g[i]) && (blue == b[i])) {
                    dstPixels[pixel] = i;
                    break;
                }
            }

            if (i == r.length) {
                System.out.println("Missing Color");
            }
        }

        dstRaster.setPixels(0, 0, dstRaster.getWidth(), dstRaster.getHeight(), dstPixels);
    }

    private static byte[] findUnusedColor(byte[][] clrs) {
        byte r = 1;
        byte g = 1;
        byte b = 1;

        for (int i = 0; i < clrs.length; i++) {
            if ((r != clrs[0][i]) && (g != clrs[1][i]) && (b != clrs[2][i])) {
                break;
            }

            r++;
            g++;
            b++;
        }

        byte[] result = { r, g, b };
        return result;
    }

    /**
     * Loads an image from a file on disk or in an archive (e.g., .jar).
     *
     * @param  path The name of the image file to load. This should be a relative path starting from anywhere in the
     *              classpath. For example, 'images/foo.gif'.
     *
     * @return A BufferedImage object that contains the loaded image.
     *
     * @throws IOException
     */
    public static BufferedImage loadImage(String path) throws IOException {
        ClassLoader ldr = ImageUtil.class.getClassLoader();

        InputStream i = ldr.getResourceAsStream(path);
        BufferedImage result = ImageIO.read(i);
        i.close();

        return result;
    }

    /**
     * Get's all of the unique colors in an image.
     *
     * @param image The image.
     */
    public static byte[][] scrapeColors(BufferedImage image) {
        int i;
        Raster raster = image.getRaster();
        byte[][] clrs = new byte[raster.getNumBands()][256];
        int[][] pixels = new int[raster.getNumBands()][];
        int nextClr = 0;
        int[] clr = new int[256];

        for (int band = 0; band < raster.getNumBands(); band++) {
            pixels[band] = raster.getSamples(0, 0, raster.getWidth(), raster.getHeight(), band, (int[]) null);
        }

        for (int pixel = 0; pixel < pixels[0].length; pixel++) {
            byte red = 0;
            byte green = 0;
            byte blue = 0;

            // Add the clr if it doesn't already exist in the index
            for (i = 0; i < clrs[0].length; i++) {
                red = (byte) pixels[0][pixel];
                green = (byte) pixels[1][pixel];
                blue = (byte) pixels[2][pixel];

                if ((red == clrs[0][i]) && (green == clrs[1][i]) && (blue == clrs[2][i])) {
                    break;
                }
            }

            if ((i == clrs[0].length) && (nextClr < 256)) {
                clrs[0][nextClr] = red;
                clrs[1][nextClr] = green;
                clrs[2][nextClr++] = blue;

                //                System.out.println("{"+Integer.toHexString(red & 0x000000FF)+','+
                //                                   Integer.toHexString(green & 0x0000FF00 >> 8)+','+
                //                                   Integer.toHexString(blue & 0x00FF0000 >> 16)+'}');
            }
        }

        //        System.out.println("Colors: " + nextClr);

        return clrs;
    }
}