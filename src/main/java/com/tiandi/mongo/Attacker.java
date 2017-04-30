package com.tiandi.mongo;

import jdk.nashorn.internal.objects.annotations.Property;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/17.
 */
public class Attacker {
    @Property
    public String faultType;
    @Property
    public String attackerPoint;

//    public Attacker(String faultType) {
//        this.faultType = faultType;
//        this.parameter = null;
//    }
    public Attacker(){}

    public Attacker(String faultType,String attackerPoint) {
        this.faultType = faultType;
        this.attackerPoint = attackerPoint;
    }
}
