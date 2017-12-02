package com.tiandi.service;

import com.tiandi.mongo.CloudFailure;
import com.tiandi.mongo.CloudFailureRepository;
import com.tiandi.mongo.FaultInjectionInfo;
import com.tiandi.mongo.FaultInjectionInfoRepository;
import com.tiandi.mongo.testcase.TestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.representer.Representer;

import java.beans.IntrospectionException;
import java.io.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/17.
 */
@Service
public class FaultInjectionInfoService {
    @Autowired
    private CloudFailureRepository failureRepository;

    @Autowired
    private FaultInjectionInfoRepository faultInjectionInfoRepository;

    public TestCase generateTestCase(String id) {
        CloudFailure faultNode = failureRepository.findById(id);
        FaultInjectionInfo injectionInfo = faultInjectionInfoRepository.findById(faultNode.cause);
        TestCase tc = new TestCase(faultNode.faultLocation, injectionInfo.attacker,injectionInfo.monitor);
        return tc;
    }

    public TestCase generateTestCase(String id,String attackerPoint, String monitorPoint) {
        CloudFailure faultNode = failureRepository.findById(id);
        FaultInjectionInfo injectionInfo = faultInjectionInfoRepository.findById(faultNode.cause);
        TestCase tc = new TestCase(faultNode.faultLocation, injectionInfo.attacker,injectionInfo.monitor);
        if(attackerPoint!=null) tc.scenarios.get(0).options.attackers.get(0).attackerPoint = attackerPoint;
        if(monitorPoint!=null) tc.scenarios.get(0).options.monitors.get(0).monitorPoint = monitorPoint;
        return tc;
    }

    public void createTestCaseFile(TestCase tc, String filename) throws IOException{
        Representer repr = new Representer();
        repr.setPropertyUtils(new FaultInjectionInfoService.UnsortedPropertyUtils());
        YamlService.dump(repr, tc, filename);
    }

    private class UnsortedPropertyUtils extends PropertyUtils {
        @Override
        protected Set<Property> createPropertySet(Class<? extends Object> type, BeanAccess bAccess)
                throws IntrospectionException {
            Set<Property> result = new LinkedHashSet<Property>(getPropertiesMap(type,
                    BeanAccess.FIELD).values());
            return result;
        }
    }
}
