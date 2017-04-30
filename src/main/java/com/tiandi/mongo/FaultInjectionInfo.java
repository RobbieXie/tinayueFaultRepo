package com.tiandi.mongo;

import org.springframework.data.annotation.Id;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/17.
 */
public class FaultInjectionInfo {
    @Id
    public String id;
    public Attacker attacker;
    public Monitor monitor;
    public FaultInjectionInfo(){}

    public FaultInjectionInfo(String id, Attacker attacker, Monitor monitor) {
        this.id = id;
        this.attacker = attacker;
        this.monitor = monitor;
    }
}
