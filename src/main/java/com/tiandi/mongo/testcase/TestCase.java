package com.tiandi.mongo.testcase;

import com.tiandi.mongo.Attacker;
import com.tiandi.mongo.Monitor;
import jdk.nashorn.internal.objects.annotations.Property;

import java.io.Serializable;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/17.
 */
public class TestCase implements Serializable{
    @Property
    public String schema;
    @Property
    public Scenarios scenario;
    @Property
    public Context context;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Scenarios getScenarios() {
        return scenario;
    }

    public void setScenarios(Scenarios scenarios) {
        this.scenario= scenarios;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public TestCase() {
    }

    public TestCase(String faultLocation, Attacker attacker, Monitor monitor) {
        this.schema = "yardstick:task:0.1";
        this.scenario = new Scenarios(faultLocation,attacker,monitor);
        this.context = new Context();
    }

}
