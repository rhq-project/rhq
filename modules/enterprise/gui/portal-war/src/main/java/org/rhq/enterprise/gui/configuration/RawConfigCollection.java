package org.rhq.enterprise.gui.configuration;

import java.util.HashMap;

import org.ajax4jsf.component.UIRepeat;

import org.rhq.core.domain.configuration.RawConfiguration;

public class RawConfigCollection {
    private UIRepeat repeater;

    public UIRepeat getRepeater() {
        return repeater;
    }

    public void setRepeater(UIRepeat repeater) {
        this.repeater = repeater;
    }

    HashMap<String, RawConfiguration> raws;

    RawConfiguration current;

    public RawConfigCollection() {
        current = new RawConfiguration();
        current.setPath("/etc/httpd/httpd.conf");
        current.setContents("This is the contents of the file".getBytes());
        raws = new HashMap<String, RawConfiguration>();
        raws.put(current.getPath(), current);

        String[] fileNames = { "/etc/httpd/conf.d/cobbler.conf", "/etc/httpd/conf.d/proxy_ajp.conf",
            "/etc/httpd/conf.d/cobbler_svc.conf", "/etc/httpd/conf.d/python.conf", "/etc/httpd/conf.d/mod_dnssd.conf",
            "/etc/httpd/conf.d/welcome.conf", "/etc/httpd/conf.d/php.conf" };

        for (String string : fileNames) {
            RawConfiguration c = new RawConfiguration();
            c.setPath(string);
            c.setContents(("contents of file" + string).getBytes());
            raws.put(c.getPath(), c);
        }

    }

    public RawConfiguration getCurrent() {
        return current;
    }

    public String getCurrentContents() {
        return new String(current.getContents());
    }

    public void setCurrentPath(String path) {
        current = raws.get(path);
    }

    public String getCurrentPath() {
        return current.getPath();
    }

    public Object[] getPaths() {
        return raws.keySet().toArray();
    }

}
