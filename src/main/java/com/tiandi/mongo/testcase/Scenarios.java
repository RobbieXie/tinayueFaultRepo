package com.tiandi.mongo.testcase;

import com.tiandi.mongo.Attacker;
import com.tiandi.mongo.Monitor;
import jdk.nashorn.internal.objects.annotations.Property;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/17.
 */
public class Scenarios {
    @Property
    public String type;
    @Property
    public Attacker attacker;
    @Property
    public Monitor monitor;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Attacker getAttacker() {
        return attacker;
    }

    public void setAttacker(Attacker attacker) {
        this.attacker = attacker;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }
    public Scenarios(){}
    public Scenarios( String faultLocation, Attacker attacker, Monitor monitor) {
        this.type = "ServiceHA";
        this.attacker = attacker;
        this.monitor = monitor;
        this.attacker.attackerPoint = faultLocation;
        this.monitor.monitorPoint = faultLocation;
    }
}
