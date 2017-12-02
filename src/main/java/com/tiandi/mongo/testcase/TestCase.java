package com.tiandi.mongo.testcase;

import com.tiandi.mongo.Attacker;
import com.tiandi.mongo.Monitor;
import jdk.nashorn.internal.objects.annotations.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/17.
 */
public class TestCase implements Serializable{
    @Property
    public String schema;
    @Property
    public List<Scenario> scenarios = new ArrayList<>();
    @Property
    public Context context;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public void setScenarios(Scenario scenario) {
        this.scenarios.add(scenario);
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
        Scenario scenario = new Scenario(faultLocation,attacker,monitor);
        this.scenarios.add(scenario);
        this.context = new Context();
    }

}
