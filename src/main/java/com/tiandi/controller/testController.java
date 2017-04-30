package com.tiandi.controller;

/**
 * @author 谢天帝
 * @version v0.1 2017/2/20.
 */

import com.tiandi.mongo.*;
import com.tiandi.mongo.dsl.YamlDSL;
import com.tiandi.mongo.faulttree.FaultTreeNode;
import com.tiandi.mongo.testcase.TestCase;
import com.tiandi.service.FaultTreeService;
import com.tiandi.service.YamlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.ArrayList;
import java.util.List;

@RestController
public class testController {

    @Autowired
    private CloudFailureRepository failureRepository;

    @Autowired
    private FaultInjectionInfoRepository faultInjectionInfoRepository;

    @Autowired
    private FaultTreeService faultTreeService;

    @RequestMapping(path="/")
    public String index() {
        try {
            TestCase tc = (TestCase) YamlService.load(new Constructor(TestCase.class),"F:/testcase/testcase.yaml");
            YamlDSL dsl = (YamlDSL) YamlService.load(new Constructor(YamlDSL.class),"F:/testcase/testcase2.yaml");
            CloudFailure cf = dsl.cloudFailure;
            List<String> index = cf.index;

            CloudFailure parentCf = failureRepository.findById(index.get(index.size()-1));
            if(parentCf.children == null) parentCf.children = new ArrayList<String>();
            parentCf.children.add(cf.id);
            failureRepository.save(parentCf);
            failureRepository.save(cf);

//            String index = "CloudFailure";
//            List<CloudFailure> parentCf = failureRepository.findByIndex(index);
            System.out.print(123);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "Greetings from Spring Boot!";
    }

    @RequestMapping(path="/mongo/savef")
    public String saveFailure() {
        ArrayList<String> children = new ArrayList<>();
        ArrayList<String> index = new ArrayList<>();
        children.add("HW-F");
        children.add("NW-F");
        children.add("COM-F");
        index.add("C-F");
        failureRepository.save(new CloudFailure("C-F","Cloud Failure","The basic failure of a cloud infrastructure.", true, null, null, null, children));
        failureRepository.save(new CloudFailure("HW-F","Hardware Failure","The hardware failure of a cloud infrastructure.",true, null, null, index, null));
        failureRepository.save(new CloudFailure("NW-F","Network Failure", "The network failure of a cloud infrastructure.", true, null, null, index, null));
        children.clear();
        children.add("COM-COM-F");
        failureRepository.save(new CloudFailure("COM-F","Cloud Component Failure", "The component failure of a cloud infrastructure.", true, null, null, index, children));
        index.add("COM-F");
        failureRepository.save(new CloudFailure("COM-COM-F","Cloud Compute Component Failure", "The compute component failure of a cloud infrastructure.", false, "compute-service","service-crash", index, null));
        return "saveFailure";
    }

    @RequestMapping(path="/mongo/savei")
    public String saveInjection() {
        faultInjectionInfoRepository.save(new FaultInjectionInfo("node-network-disconnect",new Attacker("node-network-disconnect",null),new Monitor("service-status",null,10,5)));
        faultInjectionInfoRepository.save(new FaultInjectionInfo("poweroff",new Attacker("node-poweroff",null),new Monitor("service-status",null,10,5)));
        faultInjectionInfoRepository.save(new FaultInjectionInfo("service-crash",new Attacker("kill-main-process",null),new Monitor("service-status",null,10,5)));
        faultInjectionInfoRepository.save(new FaultInjectionInfo("config-file-loss",new Attacker("remove-config-file",null),new Monitor("service-status",null,10,5)));
        return "saveInjection";
    }

    @RequestMapping(path="/mongo/findAll")
    public String findAll() {
        for (CloudFailure cloudFailure : failureRepository.findAll()) {
            System.out.println(cloudFailure);
        }
        System.out.println();
        return "findAll";
    }

    @RequestMapping(path="/mongo/testcase")
    @ResponseBody
    public TestCase generateTestCase() {
        CloudFailure faultNode = failureRepository.findById("COM-COM-F");
        FaultInjectionInfo injectionInfo = faultInjectionInfoRepository.findById(faultNode.cause);
        TestCase tc = new TestCase( faultNode.faultLocation, injectionInfo.attacker, injectionInfo.monitor);
        return tc;
    }

    @RequestMapping(path="/mongo/tree")
    @ResponseBody
    public FaultTreeNode generatefaultTree() {
        FaultTreeNode rootNode = faultTreeService.recursiveTree("C-F");
        faultTreeService.printTree(rootNode);
        return rootNode;
    }

}
