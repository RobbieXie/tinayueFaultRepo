package com.tiandi.mongo.testcase;

import jdk.nashorn.internal.objects.annotations.Property;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/17.
 */
public class Context {
    @Property
    public String type;
    @Property
    public String name;
    @Property
    public String file;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Context() {
        this.type = "Node";
        this.name = "LF";
        this.file = "/root/yardstick/etc/yardstick/nodes/fuel_virtual/pod.yaml";
    }
}
