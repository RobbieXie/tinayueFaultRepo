package com.tiandi.mongo.testcase;

import com.tiandi.mongo.Attacker;
import com.tiandi.mongo.Monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 谢天帝
 * @version v0.1 2017/5/5.
 */
public class Options {
    public List<Attacker> attackers = new ArrayList<>();
    public List<Monitor> monitors = new ArrayList<>();

    public Options(){}

    public Options(String faultLocation, Attacker a, Monitor m){
        a.attackerPoint = faultLocation;
        m.monitorPoint = faultLocation;
        this.attackers.add(a);
        this.monitors.add(m);
    }
}
