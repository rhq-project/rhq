package org.rhq.core.gui.configuration;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.faces.component.UIComponent;
import javax.faces.context.ResponseWriter;

public class MockResponseWriter extends ResponseWriter {

    public StringWriter stringWriter = new StringWriter();

    @Override
    public ResponseWriter cloneWithWriter(Writer writer) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void endDocument() throws IOException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void endElement(String name) throws IOException {
        stringWriter.write("end element:" + name + "\n");

    }

    @Override
    public void flush() throws IOException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String getCharacterEncoding() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String getContentType() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void startDocument() throws IOException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void startElement(String name, UIComponent component) throws IOException {
        stringWriter.write("startElement: " + name + ": componenet:" + component.getId() + "\n");
    }

    @Override
    public void writeAttribute(String name, Object value, String property) throws IOException {
        stringWriter.write("attribute:" + name + " value = " + value.toString() + " property = " + property + "\n");

    }

    @Override
    public void writeComment(Object comment) throws IOException {
        stringWriter.write("comment:" + comment + "\n");

    }

    @Override
    public void writeText(Object text, String property) throws IOException {
        stringWriter.write("text:" + text + ":" + property + "\n");
    }

    @Override
    public void writeText(char[] text, int off, int len) throws IOException {
        write(text, off, len);
    }

    @Override
    public void writeURIAttribute(String name, Object value, String property) throws IOException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void close() throws IOException {
        //stringWriter.close();
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        stringWriter.write(cbuf, off, len);
    }

}
