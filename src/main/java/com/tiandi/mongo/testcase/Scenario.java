package com.tiandi.mongo.testcase;

import com.tiandi.mongo.Attacker;
import com.tiandi.mongo.Monitor;
import jdk.nashorn.internal.objects.annotations.Property;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/17.
 */
public class Scenario {
    @Property
    public String type;

    public Options options;

    public Runner runner = new Runner();

    public Nodes nodes = new Nodes();

    public SLA sla = new SLA();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Scenario(){}
    public Scenario(String faultLocation, Attacker attacker, Monitor monitor) {
        this.type = "ServiceHA";
        this.options = new Options(faultLocation,attacker,monitor);
    }
}
