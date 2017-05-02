package com.tiandi.controller;

/**
 * @author 谢天帝
 * @version v0.1 2017/2/20.
 */

import com.tiandi.mongo.*;
import com.tiandi.mongo.dsl.TestCaseParams;
import com.tiandi.mongo.dsl.YamlDSL;
import com.tiandi.mongo.faulttree.FaultTreeNode;
import com.tiandi.mongo.testcase.TestCase;
import com.tiandi.service.FaultInjectionInfoService;
import com.tiandi.service.FaultTreeService;
import com.tiandi.service.YamlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
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

    @Autowired
    private FaultInjectionInfoService faultInjectionInfoService;

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
    public String generatefaultTree() {
        FaultTreeNode rootNode = faultTreeService.recursiveTree("C-F",true);
        faultTreeService.printTree(rootNode);

        rootNode = faultTreeService.recursiveTree("C-F",false);
        return faultTreeService.printTree(rootNode);
    }

    @RequestMapping(path="/dsl")
    public String operateDSL( MultipartFile file) {
        //@RequestParam(value = "file", required = true)
        try {
            FileInputStream fileInputStream;
            YamlDSL dsl;
            if(file != null){
                fileInputStream = (FileInputStream) file.getInputStream();
                dsl = (YamlDSL) YamlService.load(new Constructor(YamlDSL.class),fileInputStream);
            }else {
//                dsl = (YamlDSL) YamlService.load(new Constructor(YamlDSL.class),"F:/testcase/testcase2.yaml");
                return "上传文件为空！";
            }
            String faultTreeStructure = faultTreeService.ShowFaultTreeStructure();
            String operation = dsl.operation;
            if(operation.equals("AddFaultIntoLib")){
                CloudFailure cf = dsl.cloudFailure;
                List<String> index = cf.index;
                String id = cf.id;

                CloudFailure checkCfInDatabase = failureRepository.findById(id);
                if(checkCfInDatabase!=null) return "ID already Existed!" + "\n" + faultTreeStructure;

                if(index == null) {
                    List<CloudFailure> cfs = failureRepository.findByIndex(null);
                    if(cfs.size()>0) return "Index cannot be null! Root node has already existed, id:"+cfs.get(0).id+ "\n" + faultTreeStructure;
                }

                CloudFailure parentCf = failureRepository.findById(index.get(index.size()-1));
                if(parentCf == null) return "Index Wrong! Parent ID cannot be found!"+ "\n" + faultTreeStructure;
                if(parentCf.children == null) parentCf.children = new ArrayList<String>();
                parentCf.children.add(cf.id);
                failureRepository.save(parentCf);
                failureRepository.save(cf);
                faultTreeStructure = faultTreeService.ShowFaultTreeStructure();
                return "Add Fault Into Lib Successfully"+ "\n" + faultTreeStructure;

            }else if(operation.equals("GenerateTestCase")){
                TestCaseParams params = dsl.testCaseParams;
                CloudFailure cf = failureRepository.findById(params.faultId);
                if(cf == null || cf.isCategory || cf.cause==null ||cf.faultLocation==null) return "FaultID Wrong or FaultInfo Wrong!";
                TestCase tc = faultInjectionInfoService.generateTestCase(params.faultId,params.attackerPoint,params.monitorPoint);
//                else tc = faultInjectionInfoService.generateTestCase(params.faultId);
                faultInjectionInfoService.createTestCaseFile(tc,params.outputPath+"//" +params.faultId+".yaml");
                return "Generate TestCase Successfully!   " + params.outputPath+"//" +params.faultId+".yaml";

            }else if(operation.equals("ShowFaultTreeStructure")){
                return faultTreeStructure;

            }else{
                return "Error! Operation not Found!";
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return "文件格式或内容错误！";
    }

}
