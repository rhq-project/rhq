/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.client.utility;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;

/**
 * @author Lukas Krejci
 *
 */
public class ChangeRegisteringPrintWriter extends PrintWriter {

    private boolean changed;

    public ChangeRegisteringPrintWriter(Writer out) {
        super(out);
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    @Override
    public PrintWriter append(char c) {
        setChanged(true);
        return super.append(c);
    }

    @Override
    public void write(int c) {
        setChanged(true);
        super.write(c);
    }

    @Override
    public void write(char[] buf, int off, int len) {
        setChanged(true);
        super.write(buf, off, len);
    }

    @Override
    public void write(char[] buf) {
        setChanged(true);
        super.write(buf);
    }

    @Override
    public void write(String s, int off, int len) {
        setChanged(true);
        super.write(s, off, len);
    }

    @Override
    public void write(String s) {
        setChanged(true);
        super.write(s);
    }

    @Override
    public void print(boolean b) {
        setChanged(true);
        super.print(b);
    }

    @Override
    public void print(char c) {
        setChanged(true);
        super.print(c);
    }

    @Override
    public void print(int i) {
        setChanged(true);
        super.print(i);
    }

    @Override
    public void print(long l) {
        setChanged(true);
        super.print(l);
    }

    @Override
    public void print(float f) {
        setChanged(true);
        super.print(f);
    }

    @Override
    public void print(double d) {
        setChanged(true);
        super.print(d);
    }

    @Override
    public void print(char[] s) {
        setChanged(true);
        super.print(s);
    }

    @Override
    public void print(String s) {
        setChanged(true);
        super.print(s);
    }

    @Override
    public void print(Object obj) {
        setChanged(true);
        super.print(obj);
    }

    @Override
    public void println() {
        setChanged(true);
        super.println();
    }

    @Override
    public void println(boolean x) {
        setChanged(true);
        super.println(x);
    }

    @Override
    public void println(char x) {
        setChanged(true);
        super.println(x);
    }

    @Override
    public void println(int x) {
        setChanged(true);
        super.println(x);
    }

    @Override
    public void println(long x) {
        setChanged(true);
        super.println(x);
    }

    @Override
    public void println(float x) {
        setChanged(true);
        super.println(x);
    }

    @Override
    public void println(double x) {
        setChanged(true);
        super.println(x);
    }

    @Override
    public void println(char[] x) {
        setChanged(true);
        super.println(x);
    }

    @Override
    public void println(String x) {
        setChanged(true);
        super.println(x);
    }

    @Override
    public void println(Object x) {
        setChanged(true);
        super.println(x);
    }

    @Override
    public PrintWriter printf(String format, Object... args) {
        setChanged(true);
        return super.printf(format, args);
    }

    @Override
    public PrintWriter printf(Locale l, String format, Object... args) {
        setChanged(true);
        return super.printf(l, format, args);
    }

    @Override
    public PrintWriter format(String format, Object... args) {
        setChanged(true);
        return super.format(format, args);
    }

    @Override
    public PrintWriter format(Locale l, String format, Object... args) {
        setChanged(true);
        return super.format(l, format, args);
    }

    @Override
    public PrintWriter append(CharSequence csq) {
        setChanged(true);
        return super.append(csq);
    }

    @Override
    public PrintWriter append(CharSequence csq, int start, int end) {
        setChanged(true);
        return super.append(csq, start, end);
    }

}
