package com.tiandi.mongo.dsl;

import com.tiandi.mongo.CloudFailure;
import com.tiandi.mongo.FaultInjectionInfo;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/24.
 */
public class YamlDSL {
    public String operation;
    public CloudFailure cloudFailure;
    public FaultInjectionInfo faultInjectionInfo;
    YamlDSL(){}

}
