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
import com.tiandi.service.geneticalgorithm.FaultTreeGA;
import com.tiandi.service.geneticalgorithm.Individual;
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
import java.util.Arrays;
import java.util.List;

@RestController
public class testController {

    ArrayList<String> children = new ArrayList<>();
    ArrayList<String> index = new ArrayList<>();
    ArrayList<String> tags = new ArrayList<>();

    @Autowired
    private CloudFailureRepository failureRepository;

    @Autowired
    private FaultInjectionInfoRepository faultInjectionInfoRepository;

    @Autowired
    private FaultTreeService faultTreeService;

    @Autowired
    private FaultInjectionInfoService faultInjectionInfoService;

    @Autowired
    private FaultTreeGA faultTreeGA;

    @Autowired
    private Individual individual;


    @RequestMapping(path="/mongo/ga")
    public String ga(){
//        Individual i = new Individual();
//        int  fitness = i.getFitness();
        individual.generateIndividul();
        individual.getFitness();
        faultTreeGA.generateFaultCode();
        return "ga";
    }

    @RequestMapping(path="/mongo/savef")
    public String saveFailure() {
        createFaultTree();
        return faultTreeService.ShowFaultTreeStructure();
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


    public void createFaultTree(){
        index.clear();tags.clear();children.clear();
        children.addAll(Arrays.asList("Node1-Failure","Node2-Failure","Node3-Failure"));
        tags.add("Cloud");
        failureRepository.save(new CloudFailure("Cloud-Failure","Cloud Failure","Category for basic failures of a cloud infrastructure.", true,tags , null, null, null, children));

        createFaultTreeNode1();
        createFaultTreeNode2();
        createFaultTreeNode3();
    }

    public void createFaultTreeNode1(){
        //Node1
        index.clear();children.clear();tags.clear();
        index.add("Cloud-Failure");
        tags.addAll(Arrays.asList("Node1"));
        children.addAll(Arrays.asList("Node1-Hardware-Failure","Node1-Network-Failure","Node1-Software-Failure"));
        failureRepository.save(new CloudFailure("Node1-Failure","Node1 Failure","Category for basic failures of Node1.", true,tags , null, null, index, children));

        createFaultTreeNode1Hardware();
        createFaultTreeNode1Network();
        createFaultTreeNode1Software();
    }

    public void createFaultTreeNode2(){
        //Node2
        index.clear();children.clear();tags.clear();
        tags.addAll(Arrays.asList("Node2"));
        index.add("Cloud-Failure");
        children.addAll(Arrays.asList("Node2-Hardware-Failure","Node2-Network-Failure","Node2-Software-Failure"));
        failureRepository.save(new CloudFailure("Node2-Failure","Node2 Failure","Category for basic failures of Node2.", true,tags , null, null, index, children));

        createFaultTreeNode2Hardware();
        createFaultTreeNode2Network();
        createFaultTreeNode2Software();
    }

    public void createFaultTreeNode3(){
        //Node3
        index.clear();children.clear();tags.clear();
        tags.addAll(Arrays.asList("Node3"));
        index.add("Cloud-Failure");
        children.addAll(Arrays.asList("Node3-Hardware-Failure","Node3-Network-Failure","Node3-Software-Failure"));
        failureRepository.save(new CloudFailure("Node3-Failure","Node3 Failure","Category for basic failures of Node3.", true,tags , null, null, index, children));

        createFaultTreeNode3Hardware();
        createFaultTreeNode3Network();
        createFaultTreeNode3Software();
    }

    public void createFaultTreeNode1Hardware(){
        ArrayList<String> children = new ArrayList<>();
        ArrayList<String> index = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();

        //Node1 -> HardWare
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure"));
        tags.addAll(Arrays.asList("Hardware"));
        children.addAll(Arrays.asList("Node1-CPU-Full","Node1-Disk-Full","Node1-Power-Off"));
        failureRepository.save(new CloudFailure("Node1-Hardware-Failure","Hardware Failure","Category for hardware failures of Node1.", true,tags , null, null, index, children));

        //Node1 -> Hardware -> CPU&Disk&PowerOff

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Hardware-Failure"));
        tags.addAll(Arrays.asList("CPU"));
        failureRepository.save(new CloudFailure("Node1-CPU-Full","Node1-CPU-Full Failure","Node1-CPU-Full Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Hardware-Failure"));
        tags.addAll(Arrays.asList("Disk"));
        failureRepository.save(new CloudFailure("Node1-Disk-Full","Node1-Disk-Full Failure","Node1-Disk-Full Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Hardware-Failure"));
        tags.addAll(Arrays.asList("PowerOff"));
        failureRepository.save(new CloudFailure("Node1-Power-Off","Node1-Power-Off Failure","Node1-PowerOff Failure.", false,tags , null, null, index, null));

    }

    public void createFaultTreeNode1Network(){
        ArrayList<String> children = new ArrayList<>();
        ArrayList<String> index = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();

        //Node1 -> Network
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure"));
        tags.addAll(Arrays.asList("Network"));
        children.addAll(Arrays.asList("Node1-Package-Lag","Node1-Package-Loss","Node1-NIC-Down"));
        failureRepository.save(new CloudFailure("Node1-Network-Failure","Network Failure","Category for network failures of Node1.", true,tags , null, null, index, children));

        //Node1 -> Network -> Package Lag&loss&NIC down

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Network-Failure"));
        tags.addAll(Arrays.asList("PackageLag"));
        failureRepository.save(new CloudFailure("Node1-Package-Lag","Node1-Package-Lag Failure","Node1-Package-Lag Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Network-Failure"));
        tags.addAll(Arrays.asList("PackageLoss"));
        failureRepository.save(new CloudFailure("Node1-Package-Loss","Node1-Package-Loss Failure","Node1-Package-Loss Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Network-Failure"));
        tags.addAll(Arrays.asList("NIC-Down"));
        failureRepository.save(new CloudFailure("Node1-NIC-Down","Node1-NIC-Down Failure","Node1-NIC-Down Failure.", false,tags , null, null, index, null));
    }

    public void createFaultTreeNode1Software(){
        //Node1 -> Software
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure"));
        tags.addAll(Arrays.asList("Software"));
        children.addAll(Arrays.asList("Node1-Process-Crash","Node1-Config-Loss","Node1-Config-Wrong"));
        failureRepository.save(new CloudFailure("Node1-Software-Failure","Software Failure","Category for Software failures of Node1.", true,tags , null, null, index, children));

        createFaultTreeNode1SoftwareProcess();
        createFaultTreeNode1SoftwareConfigLoss();
        createFaultTreeNode1SoftwareConfigWrong();
    }

    public void createFaultTreeNode1SoftwareProcess(){
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure"));
        tags.addAll(Arrays.asList("ProcessCrash"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Nova","Node1-Process-Crash-Neutron","Node1-Process-Crash-Keystone",
                "Node1-Process-Crash-Cinder","Node1-Process-Crash-Glance","Node1-Process-Crash-Swift","Node1-Process-Crash-Ironic"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash","Process Crash","Category for Process-Crash of Node1.", true,tags , null, null, index, children));

        createNode1ProcessCrashNova();
        createNode1ProcessCrashNeutron();
        createNode1ProcessCrashKeystone();
        createNode1ProcessCrashCinder();
        createNode1ProcessCrashGlance();
        createNode1ProcessCrashSwift();
        createNode1ProcessCrashIronic();
    }

    private void createNode1ProcessCrashIronic() {
        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Ironic-IronicError1","Node1-Process-Crash-Ironic-IronicError2","Node1-Process-Crash-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Ironic","Ironic Failure","Category for Ironic Process-Crash of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ProcessCrash -> Ironic -> IronicError1&IronicError2&IronicError3
        //--1-- IronicError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Ironic"));
        tags.addAll(Arrays.asList("IronicError1"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Ironic-IronicError1","Node1-Process-Crash-Ironic-IronicError1","Node1-Process-Crash-Ironic-IronicError1", false,tags , null, null, index, null));

        //--2-- IronicError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Ironic"));
        tags.addAll(Arrays.asList("IronicError2"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Ironic-IronicError2","Node1-Process-Crash-Ironic-IronicError2","Node1-Process-Crash-Ironic-IronicError2", false,tags , null, null, index, null));

        //--3-- IronicError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Ironic"));
        tags.addAll(Arrays.asList("IronicError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Ironic-IronicError3","Node1-Process-Crash-Ironic-IronicError3","Node1-Process-Crash-Ironic-IronicError3", false,tags , null, null, index, null));
    }

    private void createNode1ProcessCrashSwift() {
        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Swift-SwiftError1","Node1-Process-Crash-Swift-SwiftError2","Node1-Process-Crash-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Swift","Swift Failure","Category for Swift Process-Crash of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ProcessCrash -> Swift -> SwiftError1&SwiftError2&SwiftError3
        //--1-- SwiftError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Swift"));
        tags.addAll(Arrays.asList("SwiftError1"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Swift-SwiftError1","Node1-Process-Crash-Swift-SwiftError1","Node1-Process-Crash-Swift-SwiftError1", false,tags , null, null, index, null));

        //--2-- SwiftError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Swift"));
        tags.addAll(Arrays.asList("SwiftError2"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Swift-SwiftError2","Node1-Process-Crash-Swift-SwiftError2","Node1-Process-Crash-Swift-SwiftError2", false,tags , null, null, index, null));

        //--3-- SwiftError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Swift"));
        tags.addAll(Arrays.asList("SwiftError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Swift-SwiftError3","Node1-Process-Crash-Swift-SwiftError3","Node1-Process-Crash-Swift-SwiftError3", false,tags , null, null, index, null));
    }

    private void createNode1ProcessCrashGlance() {
        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Glance-GlanceError1","Node1-Process-Crash-Glance-GlanceError2","Node1-Process-Crash-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Glance","Glance Failure","Category for Glance Process-Crash of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ProcessCrash -> Glance -> GlanceError1&GlanceError2&GlanceError3
        //--1-- GlanceError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Glance"));
        tags.addAll(Arrays.asList("GlanceError1"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Glance-GlanceError1","Node1-Process-Crash-Glance-GlanceError1","Node1-Process-Crash-Glance-GlanceError1", false,tags , null, null, index, null));

        //--2-- GlanceError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Glance"));
        tags.addAll(Arrays.asList("GlanceError2"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Glance-GlanceError2","Node1-Process-Crash-Glance-GlanceError2","Node1-Process-Crash-Glance-GlanceError2", false,tags , null, null, index, null));

        //--3-- GlanceError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Glance"));
        tags.addAll(Arrays.asList("GlanceError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Glance-GlanceError3","Node1-Process-Crash-Glance-GlanceError3","Node1-Process-Crash-Glance-GlanceError3", false,tags , null, null, index, null));

    }

    private void createNode1ProcessCrashCinder() {
        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Cinder-CinderError1","Node1-Process-Crash-Cinder-CinderError2","Node1-Process-Crash-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Cinder","Cinder Failure","Category for Cinder Process-Crash of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ProcessCrash -> Cinder -> CinderError1&CinderError2&CinderError3
        //--1-- CinderError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Cinder"));
        tags.addAll(Arrays.asList("CinderError1"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Cinder-CinderError1","Node1-Process-Crash-Cinder-CinderError1","Node1-Process-Crash-Cinder-CinderError1", false,tags , null, null, index, null));

        //--2-- CinderError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Cinder"));
        tags.addAll(Arrays.asList("CinderError2"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Cinder-CinderError2","Node1-Process-Crash-Cinder-CinderError2","Node1-Process-Crash-Cinder-CinderError2", false,tags , null, null, index, null));

        //--3-- CinderError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Cinder"));
        tags.addAll(Arrays.asList("CinderError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Cinder-CinderError3","Node1-Process-Crash-Cinder-CinderError3","Node1-Process-Crash-Cinder-CinderError3", false,tags , null, null, index, null));
    }

    private void createNode1ProcessCrashKeystone() {
        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Keystone","Keystone Failure","Category for Keystone Process-Crash of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ProcessCrash -> Keystone -> keystone
        //--keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Keystone"));
        tags.addAll(Arrays.asList("Keystone"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Keystone-KeystoneService","Node1-Process-Crash-Keystone-KeystoneService","Node1-Process-Crash-Keystone-KeystoneService", false,tags , null, null, index, null));
    }

    private void createNode1ProcessCrashNeutron() {
        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Neutron","Network"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Neutron-NeutronServer","Node1-Process-Crash-Neutron-L3Agent","Node1-Process-Crash-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Neutron","Neutron Failure","Category for Neutron Process-Crash of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ProcessCrash -> Neutron -> neutronServer& L3Agent&L2Agent
        //--1-- neutronServer
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Neutron"));
        tags.addAll(Arrays.asList("NeutronServer"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Neutron-NeutronServer","Node1-Process-Crash-Neutron-NeutronServer","Node1-Process-Crash-Neutron-NeutronServer", false,tags , null, null, index, null));

        //--2-- L3Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Neutron"));
        tags.addAll(Arrays.asList("L3Agent"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Neutron-L3Agent","Node1-Process-Crash-Neutron-L3Agent","Node1-Process-Crash-Neutron-L3Agent", false,tags , null, null, index, null));

        //--3-- L2Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Neutron"));
        tags.addAll(Arrays.asList("L2Agent"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Neutron-L2Agent","Node1-Process-Crash-Neutron-L2Agent","Node1-Process-Crash-Neutron-L2Agent", false,tags , null, null, index, null));
    }

    private void createNode1ProcessCrashNova() {
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Nova-NovaApi","Node1-Process-Crash-Nova-NovaCompute","Node1-Process-Crash-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Nova","Nova Failure","Category for Nova Process-Crash of Node1.", true,tags , null, null, index, children));


        //Node1 -> Software -> ProcessCrash -> Nova -> novaApi&novaCompute&novaScheduler
        //--1-- novaApi
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Nova"));
        tags.addAll(Arrays.asList("NovaApi"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Nova-NovaApi","Node1-Process-Crash-Nova-NovaApi","Node1-Process-Crash-Nova-NovaApi", false,tags , null, null, index, null));

        //--2-- novaCompute
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Nova"));
        tags.addAll(Arrays.asList("NovaCompute"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Nova-NovaCompute","Node1-Process-Crash-Nova-NovaCompute","Node1-Process-Crash-Nova-NovaCompute", false,tags , null, null, index, null));

        //--3-- novaScheduler
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Nova"));
        tags.addAll(Arrays.asList("NovaScheduler"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Nova-NovaScheduler","Node1-Process-Crash-Nova-NovaScheduler","Node1-Process-Crash-Nova-NovaScheduler", false,tags , null, null, index, null));
    }


    public void createFaultTreeNode1SoftwareConfigWrong(){

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure"));
        tags.addAll(Arrays.asList("ConfigWrong"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Nova","Node1-Config-Wrong-Neutron","Node1-Config-Wrong-Keystone",
                "Node1-Config-Wrong-Cinder","Node1-Config-Wrong-Glance","Node1-Config-Wrong-Swift","Node1-Config-Wrong-Ironic"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong","Config Wrong","Category for Config-Wrong of Node1.", true,tags , null, null, index, children));

        createNode1ConfigWrongNova();
        createNode1ConfigWrongNeutron();
        createNode1ConfigWrongKeystone();
        createNode1ConfigWrongCinder();
        createNode1ConfigWrongGlance();
        createNode1ConfigWrongSwift();
        createNode1ConfigWrongIronic();
    }

    private void createNode1ConfigWrongIronic() {
        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Ironic-IronicError1","Node1-Config-Wrong-Ironic-IronicError2","Node1-Config-Wrong-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Ironic","Ironic Failure","Category for Ironic Config-Wrong of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigWrong -> Ironic -> IronicError1&IronicError2&IronicError3
        //--1-- IronicError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Ironic"));
        tags.addAll(Arrays.asList("IronicError1"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Ironic-IronicError1","Node1-Config-Wrong-Ironic-IronicError1","Node1-Config-Wrong-Ironic-IronicError1", false,tags , null, null, index, null));

        //--2-- IronicError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Ironic"));
        tags.addAll(Arrays.asList("IronicError2"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Ironic-IronicError2","Node1-Config-Wrong-Ironic-IronicError2","Node1-Config-Wrong-Ironic-IronicError2", false,tags , null, null, index, null));

        //--3-- IronicError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Ironic"));
        tags.addAll(Arrays.asList("IronicError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Ironic-IronicError3","Node1-Config-Wrong-Ironic-IronicError3","Node1-Config-Wrong-Ironic-IronicError3", false,tags , null, null, index, null));
    }

    private void createNode1ConfigWrongSwift() {
        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Swift-SwiftError1","Node1-Config-Wrong-Swift-SwiftError2","Node1-Config-Wrong-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Swift","Swift Failure","Category for Swift Config-Wrong of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigWrong -> Swift -> SwiftError1&SwiftError2&SwiftError3
        //--1-- SwiftError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Swift"));
        tags.addAll(Arrays.asList("SwiftError1"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Swift-SwiftError1","Node1-Config-Wrong-Swift-SwiftError1","Node1-Config-Wrong-Swift-SwiftError1", false,tags , null, null, index, null));

        //--2-- SwiftError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Swift"));
        tags.addAll(Arrays.asList("SwiftError2"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Swift-SwiftError2","Node1-Config-Wrong-Swift-SwiftError2","Node1-Config-Wrong-Swift-SwiftError2", false,tags , null, null, index, null));

        //--3-- SwiftError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Swift"));
        tags.addAll(Arrays.asList("SwiftError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Swift-SwiftError3","Node1-Config-Wrong-Swift-SwiftError3","Node1-Config-Wrong-Swift-SwiftError3", false,tags , null, null, index, null));
    }

    private void createNode1ConfigWrongGlance() {
        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Glance-GlanceError1","Node1-Config-Wrong-Glance-GlanceError2","Node1-Config-Wrong-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Glance","Glance Failure","Category for Glance Config-Wrong of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigWrong -> Glance -> GlanceError1&GlanceError2&GlanceError3
        //--1-- GlanceError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Glance"));
        tags.addAll(Arrays.asList("GlanceError1"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Glance-GlanceError1","Node1-Config-Wrong-Glance-GlanceError1","Node1-Config-Wrong-Glance-GlanceError1", false,tags , null, null, index, null));

        //--2-- GlanceError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Glance"));
        tags.addAll(Arrays.asList("GlanceError2"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Glance-GlanceError2","Node1-Config-Wrong-Glance-GlanceError2","Node1-Config-Wrong-Glance-GlanceError2", false,tags , null, null, index, null));

        //--3-- GlanceError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Glance"));
        tags.addAll(Arrays.asList("GlanceError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Glance-GlanceError3","Node1-Config-Wrong-Glance-GlanceError3","Node1-Config-Wrong-Glance-GlanceError3", false,tags , null, null, index, null));

    }

    private void createNode1ConfigWrongCinder() {
        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Cinder-CinderError1","Node1-Config-Wrong-Cinder-CinderError2","Node1-Config-Wrong-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Cinder","Cinder Failure","Category for Cinder Config-Wrong of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigWrong -> Cinder -> CinderError1&CinderError2&CinderError3
        //--1-- CinderError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Cinder"));
        tags.addAll(Arrays.asList("CinderError1"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Cinder-CinderError1","Node1-Config-Wrong-Cinder-CinderError1","Node1-Config-Wrong-Cinder-CinderError1", false,tags , null, null, index, null));

        //--2-- CinderError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Cinder"));
        tags.addAll(Arrays.asList("CinderError2"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Cinder-CinderError2","Node1-Config-Wrong-Cinder-CinderError2","Node1-Config-Wrong-Cinder-CinderError2", false,tags , null, null, index, null));

        //--3-- CinderError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Cinder"));
        tags.addAll(Arrays.asList("CinderError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Cinder-CinderError3","Node1-Config-Wrong-Cinder-CinderError3","Node1-Config-Wrong-Cinder-CinderError3", false,tags , null, null, index, null));
    }

    private void createNode1ConfigWrongKeystone() {
        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Keystone","Keystone Failure","Category for Keystone Config-Wrong of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigWrong -> Keystone -> keystone
        //--keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Keystone"));
        tags.addAll(Arrays.asList("Keystone"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Keystone-KeystoneService","Node1-Config-Wrong-Keystone-KeystoneService","Node1-Config-Wrong-Keystone-KeystoneService", false,tags , null, null, index, null));
    }

    private void createNode1ConfigWrongNeutron() {
        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Neutron","Network"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Neutron-NeutronServer","Node1-Config-Wrong-Neutron-L3Agent","Node1-Config-Wrong-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Neutron","Neutron Failure","Category for Neutron Config-Wrong of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigWrong -> Neutron -> neutronServer& L3Agent&L2Agent
        //--1-- neutronServer
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Neutron"));
        tags.addAll(Arrays.asList("NeutronServer"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Neutron-NeutronServer","Node1-Config-Wrong-Neutron-NeutronServer","Node1-Config-Wrong-Neutron-NeutronServer", false,tags , null, null, index, null));

        //--2-- L3Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Neutron"));
        tags.addAll(Arrays.asList("L3Agent"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Neutron-L3Agent","Node1-Config-Wrong-Neutron-L3Agent","Node1-Config-Wrong-Neutron-L3Agent", false,tags , null, null, index, null));

        //--3-- L2Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Neutron"));
        tags.addAll(Arrays.asList("L2Agent"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Neutron-L2Agent","Node1-Config-Wrong-Neutron-L2Agent","Node1-Config-Wrong-Neutron-L2Agent", false,tags , null, null, index, null));
    }

    private void createNode1ConfigWrongNova() {
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Nova-NovaApi","Node1-Config-Wrong-Nova-NovaCompute","Node1-Config-Wrong-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Nova","Nova Failure","Category for Nova Config-Wrong of Node1.", true,tags , null, null, index, children));


        //Node1 -> Software -> ConfigWrong -> Nova -> novaApi&novaCompute&novaScheduler
        //--1-- novaApi
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Nova"));
        tags.addAll(Arrays.asList("NovaApi"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Nova-NovaApi","Node1-Config-Wrong-Nova-NovaApi","Node1-Config-Wrong-Nova-NovaApi", false,tags , null, null, index, null));

        //--2-- novaCompute
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Nova"));
        tags.addAll(Arrays.asList("NovaCompute"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Nova-NovaCompute","Node1-Config-Wrong-Nova-NovaCompute","Node1-Config-Wrong-Nova-NovaCompute", false,tags , null, null, index, null));

        //--3-- novaScheduler
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Nova"));
        tags.addAll(Arrays.asList("NovaScheduler"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Nova-NovaScheduler","Node1-Config-Wrong-Nova-NovaScheduler","Node1-Config-Wrong-Nova-NovaScheduler", false,tags , null, null, index, null));
    }


    public void createFaultTreeNode1SoftwareConfigLoss(){

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure"));
        tags.addAll(Arrays.asList("ConfigLoss"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Nova","Node1-Config-Loss-Neutron","Node1-Config-Loss-Keystone",
                "Node1-Config-Loss-Cinder","Node1-Config-Loss-Glance","Node1-Config-Loss-Swift","Node1-Config-Loss-Ironic"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss","Config Loss","Category for Config-Loss of Node1.", true,tags , null, null, index, children));

        createNode1ConfigLossNova();
        createNode1ConfigLossNeutron();
        createNode1ConfigLossKeystone();
        createNode1ConfigLossCinder();
        createNode1ConfigLossGlance();
        createNode1ConfigLossSwift();
        createNode1ConfigLossIronic();
    }

    private void createNode1ConfigLossIronic() {
        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Ironic-IronicError1","Node1-Config-Loss-Ironic-IronicError2","Node1-Config-Loss-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Ironic","Ironic Failure","Category for Ironic Config-Loss of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigLoss -> Ironic -> IronicError1&IronicError2&IronicError3
        //--1-- IronicError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Ironic"));
        tags.addAll(Arrays.asList("IronicError1"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Ironic-IronicError1","Node1-Config-Loss-Ironic-IronicError1","Node1-Config-Loss-Ironic-IronicError1", false,tags , null, null, index, null));

        //--2-- IronicError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Ironic"));
        tags.addAll(Arrays.asList("IronicError2"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Ironic-IronicError2","Node1-Config-Loss-Ironic-IronicError2","Node1-Config-Loss-Ironic-IronicError2", false,tags , null, null, index, null));

        //--3-- IronicError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Ironic"));
        tags.addAll(Arrays.asList("IronicError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Ironic-IronicError3","Node1-Config-Loss-Ironic-IronicError3","Node1-Config-Loss-Ironic-IronicError3", false,tags , null, null, index, null));
    }

    private void createNode1ConfigLossSwift() {
        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Swift-SwiftError1","Node1-Config-Loss-Swift-SwiftError2","Node1-Config-Loss-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Swift","Swift Failure","Category for Swift Config-Loss of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigLoss -> Swift -> SwiftError1&SwiftError2&SwiftError3
        //--1-- SwiftError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Swift"));
        tags.addAll(Arrays.asList("SwiftError1"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Swift-SwiftError1","Node1-Config-Loss-Swift-SwiftError1","Node1-Config-Loss-Swift-SwiftError1", false,tags , null, null, index, null));

        //--2-- SwiftError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Swift"));
        tags.addAll(Arrays.asList("SwiftError2"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Swift-SwiftError2","Node1-Config-Loss-Swift-SwiftError2","Node1-Config-Loss-Swift-SwiftError2", false,tags , null, null, index, null));

        //--3-- SwiftError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Swift"));
        tags.addAll(Arrays.asList("SwiftError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Swift-SwiftError3","Node1-Config-Loss-Swift-SwiftError3","Node1-Config-Loss-Swift-SwiftError3", false,tags , null, null, index, null));
    }

    private void createNode1ConfigLossGlance() {
        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Glance-GlanceError1","Node1-Config-Loss-Glance-GlanceError2","Node1-Config-Loss-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Glance","Glance Failure","Category for Glance Config-Loss of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigLoss -> Glance -> GlanceError1&GlanceError2&GlanceError3
        //--1-- GlanceError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Glance"));
        tags.addAll(Arrays.asList("GlanceError1"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Glance-GlanceError1","Node1-Config-Loss-Glance-GlanceError1","Node1-Config-Loss-Glance-GlanceError1", false,tags , null, null, index, null));

        //--2-- GlanceError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Glance"));
        tags.addAll(Arrays.asList("GlanceError2"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Glance-GlanceError2","Node1-Config-Loss-Glance-GlanceError2","Node1-Config-Loss-Glance-GlanceError2", false,tags , null, null, index, null));

        //--3-- GlanceError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Glance"));
        tags.addAll(Arrays.asList("GlanceError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Glance-GlanceError3","Node1-Config-Loss-Glance-GlanceError3","Node1-Config-Loss-Glance-GlanceError3", false,tags , null, null, index, null));

    }

    private void createNode1ConfigLossCinder() {
        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Cinder-CinderError1","Node1-Config-Loss-Cinder-CinderError2","Node1-Config-Loss-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Cinder","Cinder Failure","Category for Cinder Config-Loss of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigLoss -> Cinder -> CinderError1&CinderError2&CinderError3
        //--1-- CinderError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Cinder"));
        tags.addAll(Arrays.asList("CinderError1"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Cinder-CinderError1","Node1-Config-Loss-Cinder-CinderError1","Node1-Config-Loss-Cinder-CinderError1", false,tags , null, null, index, null));

        //--2-- CinderError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Cinder"));
        tags.addAll(Arrays.asList("CinderError2"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Cinder-CinderError2","Node1-Config-Loss-Cinder-CinderError2","Node1-Config-Loss-Cinder-CinderError2", false,tags , null, null, index, null));

        //--3-- CinderError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Cinder"));
        tags.addAll(Arrays.asList("CinderError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Cinder-CinderError3","Node1-Config-Loss-Cinder-CinderError3","Node1-Config-Loss-Cinder-CinderError3", false,tags , null, null, index, null));
    }

    private void createNode1ConfigLossKeystone() {
        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Keystone","Keystone Failure","Category for Keystone Config-Loss of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigLoss -> Keystone -> keystone
        //--keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Keystone"));
        tags.addAll(Arrays.asList("Keystone"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Keystone-KeystoneService","Node1-Config-Loss-Keystone-KeystoneService","Node1-Config-Loss-Keystone-KeystoneService", false,tags , null, null, index, null));
    }

    private void createNode1ConfigLossNeutron() {
        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Neutron","Network"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Neutron-NeutronServer","Node1-Config-Loss-Neutron-L3Agent","Node1-Config-Loss-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Neutron","Neutron Failure","Category for Neutron Config-Loss of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigLoss -> Neutron -> neutronServer& L3Agent&L2Agent
        //--1-- neutronServer
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Neutron"));
        tags.addAll(Arrays.asList("NeutronServer"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Neutron-NeutronServer","Node1-Config-Loss-Neutron-NeutronServer","Node1-Config-Loss-Neutron-NeutronServer", false,tags , null, null, index, null));

        //--2-- L3Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Neutron"));
        tags.addAll(Arrays.asList("L3Agent"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Neutron-L3Agent","Node1-Config-Loss-Neutron-L3Agent","Node1-Config-Loss-Neutron-L3Agent", false,tags , null, null, index, null));

        //--3-- L2Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Neutron"));
        tags.addAll(Arrays.asList("L2Agent"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Neutron-L2Agent","Node1-Config-Loss-Neutron-L2Agent","Node1-Config-Loss-Neutron-L2Agent", false,tags , null, null, index, null));
    }

    private void createNode1ConfigLossNova() {
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Nova-NovaApi","Node1-Config-Loss-Nova-NovaCompute","Node1-Config-Loss-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Nova","Nova Failure","Category for Nova Config-Loss of Node1.", true,tags , null, null, index, children));


        //Node1 -> Software -> ConfigLoss -> Nova -> novaApi&novaCompute&novaScheduler
        //--1-- novaApi
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Nova"));
        tags.addAll(Arrays.asList("NovaApi"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Nova-NovaApi","Node1-Config-Loss-Nova-NovaApi","Node1-Config-Loss-Nova-NovaApi", false,tags , null, null, index, null));

        //--2-- novaCompute
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Nova"));
        tags.addAll(Arrays.asList("NovaCompute"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Nova-NovaCompute","Node1-Config-Loss-Nova-NovaCompute","Node1-Config-Loss-Nova-NovaCompute", false,tags , null, null, index, null));

        //--3-- novaScheduler
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Nova"));
        tags.addAll(Arrays.asList("NovaScheduler"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Nova-NovaScheduler","Node1-Config-Loss-Nova-NovaScheduler","Node1-Config-Loss-Nova-NovaScheduler", false,tags , null, null, index, null));
    }


    public void createFaultTreeNode2Hardware(){
        ArrayList<String> children = new ArrayList<>();
        ArrayList<String> index = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();

        //Node2 -> HardWare
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure"));
        tags.addAll(Arrays.asList("Hardware"));
        children.addAll(Arrays.asList("Node2-CPU-Full","Node2-Disk-Full","Node2-Power-Off"));
        failureRepository.save(new CloudFailure("Node2-Hardware-Failure","Hardware Failure","Category for hardware failures of Node2.", true,tags , null, null, index, children));

        //Node2 -> Hardware -> CPU&Disk&PowerOff

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Hardware-Failure"));
        tags.addAll(Arrays.asList("CPU"));
        failureRepository.save(new CloudFailure("Node2-CPU-Full","Node2-CPU-Full Failure","Node2-CPU-Full Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Hardware-Failure"));
        tags.addAll(Arrays.asList("Disk"));
        failureRepository.save(new CloudFailure("Node2-Disk-Full","Node2-Disk-Full Failure","Node2-Disk-Full Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Hardware-Failure"));
        tags.addAll(Arrays.asList("PowerOff"));
        failureRepository.save(new CloudFailure("Node2-Power-Off","Node2-Power-Off Failure","Node2-PowerOff Failure.", false,tags , null, null, index, null));

    }

    public void createFaultTreeNode2Network(){
        ArrayList<String> children = new ArrayList<>();
        ArrayList<String> index = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();

        //Node2 -> Network
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure"));
        tags.addAll(Arrays.asList("Network"));
        children.addAll(Arrays.asList("Node2-Package-Lag","Node2-Package-Loss","Node2-NIC-Down"));
        failureRepository.save(new CloudFailure("Node2-Network-Failure","Network Failure","Category for network failures of Node2.", true,tags , null, null, index, children));

        //Node2 -> Network -> Package Lag&loss&NIC down

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Network-Failure"));
        tags.addAll(Arrays.asList("PackageLag"));
        failureRepository.save(new CloudFailure("Node2-Package-Lag","Node2-Package-Lag Failure","Node2-Package-Lag Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Network-Failure"));
        tags.addAll(Arrays.asList("PackageLoss"));
        failureRepository.save(new CloudFailure("Node2-Package-Loss","Node2-Package-Loss Failure","Node2-Package-Loss Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Network-Failure"));
        tags.addAll(Arrays.asList("NIC-Down"));
        failureRepository.save(new CloudFailure("Node2-NIC-Down","Node2-NIC-Down Failure","Node2-NIC-Down Failure.", false,tags , null, null, index, null));
    }

    public void createFaultTreeNode2Software(){
        //Node2 -> Software
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure"));
        tags.addAll(Arrays.asList("Software"));
        children.addAll(Arrays.asList("Node2-Process-Crash","Node2-Config-Loss","Node2-Config-Wrong"));
        failureRepository.save(new CloudFailure("Node2-Software-Failure","Software Failure","Category for Software failures of Node2.", true,tags , null, null, index, children));

        createFaultTreeNode2SoftwareProcess();
        createFaultTreeNode2SoftwareConfigLoss();
        createFaultTreeNode2SoftwareConfigWrong();
    }

    public void createFaultTreeNode2SoftwareProcess(){
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure"));
        tags.addAll(Arrays.asList("ProcessCrash"));
        children.addAll(Arrays.asList("Node2-Process-Crash-Nova","Node2-Process-Crash-Neutron","Node2-Process-Crash-Keystone",
                "Node2-Process-Crash-Cinder","Node2-Process-Crash-Glance","Node2-Process-Crash-Swift","Node2-Process-Crash-Ironic"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash","Process Crash","Category for Process-Crash of Node2.", true,tags , null, null, index, children));

        createNode2ProcessCrashNova();
        createNode2ProcessCrashNeutron();
        createNode2ProcessCrashKeystone();
        createNode2ProcessCrashCinder();
        createNode2ProcessCrashGlance();
        createNode2ProcessCrashSwift();
        createNode2ProcessCrashIronic();
    }

    private void createNode2ProcessCrashIronic() {
        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node2-Process-Crash-Ironic-IronicError1","Node2-Process-Crash-Ironic-IronicError2","Node2-Process-Crash-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Ironic","Ironic Failure","Category for Ironic Process-Crash of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ProcessCrash -> Ironic -> IronicError1&IronicError2&IronicError3
        //--1-- IronicError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Ironic"));
        tags.addAll(Arrays.asList("IronicError1"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Ironic-IronicError1","Node2-Process-Crash-Ironic-IronicError1","Node2-Process-Crash-Ironic-IronicError1", false,tags , null, null, index, null));

        //--2-- IronicError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Ironic"));
        tags.addAll(Arrays.asList("IronicError2"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Ironic-IronicError2","Node2-Process-Crash-Ironic-IronicError2","Node2-Process-Crash-Ironic-IronicError2", false,tags , null, null, index, null));

        //--3-- IronicError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Ironic"));
        tags.addAll(Arrays.asList("IronicError3"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Ironic-IronicError3","Node2-Process-Crash-Ironic-IronicError3","Node2-Process-Crash-Ironic-IronicError3", false,tags , null, null, index, null));
    }

    private void createNode2ProcessCrashSwift() {
        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node2-Process-Crash-Swift-SwiftError1","Node2-Process-Crash-Swift-SwiftError2","Node2-Process-Crash-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Swift","Swift Failure","Category for Swift Process-Crash of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ProcessCrash -> Swift -> SwiftError1&SwiftError2&SwiftError3
        //--1-- SwiftError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Swift"));
        tags.addAll(Arrays.asList("SwiftError1"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Swift-SwiftError1","Node2-Process-Crash-Swift-SwiftError1","Node2-Process-Crash-Swift-SwiftError1", false,tags , null, null, index, null));

        //--2-- SwiftError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Swift"));
        tags.addAll(Arrays.asList("SwiftError2"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Swift-SwiftError2","Node2-Process-Crash-Swift-SwiftError2","Node2-Process-Crash-Swift-SwiftError2", false,tags , null, null, index, null));

        //--3-- SwiftError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Swift"));
        tags.addAll(Arrays.asList("SwiftError3"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Swift-SwiftError3","Node2-Process-Crash-Swift-SwiftError3","Node2-Process-Crash-Swift-SwiftError3", false,tags , null, null, index, null));
    }

    private void createNode2ProcessCrashGlance() {
        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node2-Process-Crash-Glance-GlanceError1","Node2-Process-Crash-Glance-GlanceError2","Node2-Process-Crash-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Glance","Glance Failure","Category for Glance Process-Crash of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ProcessCrash -> Glance -> GlanceError1&GlanceError2&GlanceError3
        //--1-- GlanceError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Glance"));
        tags.addAll(Arrays.asList("GlanceError1"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Glance-GlanceError1","Node2-Process-Crash-Glance-GlanceError1","Node2-Process-Crash-Glance-GlanceError1", false,tags , null, null, index, null));

        //--2-- GlanceError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Glance"));
        tags.addAll(Arrays.asList("GlanceError2"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Glance-GlanceError2","Node2-Process-Crash-Glance-GlanceError2","Node2-Process-Crash-Glance-GlanceError2", false,tags , null, null, index, null));

        //--3-- GlanceError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Glance"));
        tags.addAll(Arrays.asList("GlanceError3"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Glance-GlanceError3","Node2-Process-Crash-Glance-GlanceError3","Node2-Process-Crash-Glance-GlanceError3", false,tags , null, null, index, null));

    }

    private void createNode2ProcessCrashCinder() {
        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node2-Process-Crash-Cinder-CinderError1","Node2-Process-Crash-Cinder-CinderError2","Node2-Process-Crash-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Cinder","Cinder Failure","Category for Cinder Process-Crash of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ProcessCrash -> Cinder -> CinderError1&CinderError2&CinderError3
        //--1-- CinderError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Cinder"));
        tags.addAll(Arrays.asList("CinderError1"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Cinder-CinderError1","Node2-Process-Crash-Cinder-CinderError1","Node2-Process-Crash-Cinder-CinderError1", false,tags , null, null, index, null));

        //--2-- CinderError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Cinder"));
        tags.addAll(Arrays.asList("CinderError2"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Cinder-CinderError2","Node2-Process-Crash-Cinder-CinderError2","Node2-Process-Crash-Cinder-CinderError2", false,tags , null, null, index, null));

        //--3-- CinderError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Cinder"));
        tags.addAll(Arrays.asList("CinderError3"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Cinder-CinderError3","Node2-Process-Crash-Cinder-CinderError3","Node2-Process-Crash-Cinder-CinderError3", false,tags , null, null, index, null));
    }

    private void createNode2ProcessCrashKeystone() {
        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node2-Process-Crash-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Keystone","Keystone Failure","Category for Keystone Process-Crash of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ProcessCrash -> Keystone -> keystone
        //--keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Keystone"));
        tags.addAll(Arrays.asList("Keystone"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Keystone-KeystoneService","Node2-Process-Crash-Keystone-KeystoneService","Node2-Process-Crash-Keystone-KeystoneService", false,tags , null, null, index, null));
    }

    private void createNode2ProcessCrashNeutron() {
        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash"));
        tags.addAll(Arrays.asList("Neutron","Network"));
        children.addAll(Arrays.asList("Node2-Process-Crash-Neutron-NeutronServer","Node2-Process-Crash-Neutron-L3Agent","Node2-Process-Crash-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Neutron","Neutron Failure","Category for Neutron Process-Crash of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ProcessCrash -> Neutron -> neutronServer& L3Agent&L2Agent
        //--1-- neutronServer
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Neutron"));
        tags.addAll(Arrays.asList("NeutronServer"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Neutron-NeutronServer","Node2-Process-Crash-Neutron-NeutronServer","Node2-Process-Crash-Neutron-NeutronServer", false,tags , null, null, index, null));

        //--2-- L3Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Neutron"));
        tags.addAll(Arrays.asList("L3Agent"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Neutron-L3Agent","Node2-Process-Crash-Neutron-L3Agent","Node2-Process-Crash-Neutron-L3Agent", false,tags , null, null, index, null));

        //--3-- L2Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Neutron"));
        tags.addAll(Arrays.asList("L2Agent"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Neutron-L2Agent","Node2-Process-Crash-Neutron-L2Agent","Node2-Process-Crash-Neutron-L2Agent", false,tags , null, null, index, null));
    }

    private void createNode2ProcessCrashNova() {
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node2-Process-Crash-Nova-NovaApi","Node2-Process-Crash-Nova-NovaCompute","Node2-Process-Crash-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Nova","Nova Failure","Category for Nova Process-Crash of Node2.", true,tags , null, null, index, children));


        //Node2 -> Software -> ProcessCrash -> Nova -> novaApi&novaCompute&novaScheduler
        //--1-- novaApi
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Nova"));
        tags.addAll(Arrays.asList("NovaApi"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Nova-NovaApi","Node2-Process-Crash-Nova-NovaApi","Node2-Process-Crash-Nova-NovaApi", false,tags , null, null, index, null));

        //--2-- novaCompute
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Nova"));
        tags.addAll(Arrays.asList("NovaCompute"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Nova-NovaCompute","Node2-Process-Crash-Nova-NovaCompute","Node2-Process-Crash-Nova-NovaCompute", false,tags , null, null, index, null));

        //--3-- novaScheduler
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Process-Crash","Node2-Process-Crash-Nova"));
        tags.addAll(Arrays.asList("NovaScheduler"));
        failureRepository.save(new CloudFailure("Node2-Process-Crash-Nova-NovaScheduler","Node2-Process-Crash-Nova-NovaScheduler","Node2-Process-Crash-Nova-NovaScheduler", false,tags , null, null, index, null));
    }


    public void createFaultTreeNode2SoftwareConfigWrong(){

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure"));
        tags.addAll(Arrays.asList("ConfigWrong"));
        children.addAll(Arrays.asList("Node2-Config-Wrong-Nova","Node2-Config-Wrong-Neutron","Node2-Config-Wrong-Keystone",
                "Node2-Config-Wrong-Cinder","Node2-Config-Wrong-Glance","Node2-Config-Wrong-Swift","Node2-Config-Wrong-Ironic"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong","Config Wrong","Category for Config-Wrong of Node2.", true,tags , null, null, index, children));

        createNode2ConfigWrongNova();
        createNode2ConfigWrongNeutron();
        createNode2ConfigWrongKeystone();
        createNode2ConfigWrongCinder();
        createNode2ConfigWrongGlance();
        createNode2ConfigWrongSwift();
        createNode2ConfigWrongIronic();
    }

    private void createNode2ConfigWrongIronic() {
        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node2-Config-Wrong-Ironic-IronicError1","Node2-Config-Wrong-Ironic-IronicError2","Node2-Config-Wrong-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Ironic","Ironic Failure","Category for Ironic Config-Wrong of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigWrong -> Ironic -> IronicError1&IronicError2&IronicError3
        //--1-- IronicError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Ironic"));
        tags.addAll(Arrays.asList("IronicError1"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Ironic-IronicError1","Node2-Config-Wrong-Ironic-IronicError1","Node2-Config-Wrong-Ironic-IronicError1", false,tags , null, null, index, null));

        //--2-- IronicError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Ironic"));
        tags.addAll(Arrays.asList("IronicError2"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Ironic-IronicError2","Node2-Config-Wrong-Ironic-IronicError2","Node2-Config-Wrong-Ironic-IronicError2", false,tags , null, null, index, null));

        //--3-- IronicError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Ironic"));
        tags.addAll(Arrays.asList("IronicError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Ironic-IronicError3","Node2-Config-Wrong-Ironic-IronicError3","Node2-Config-Wrong-Ironic-IronicError3", false,tags , null, null, index, null));
    }

    private void createNode2ConfigWrongSwift() {
        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node2-Config-Wrong-Swift-SwiftError1","Node2-Config-Wrong-Swift-SwiftError2","Node2-Config-Wrong-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Swift","Swift Failure","Category for Swift Config-Wrong of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigWrong -> Swift -> SwiftError1&SwiftError2&SwiftError3
        //--1-- SwiftError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Swift"));
        tags.addAll(Arrays.asList("SwiftError1"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Swift-SwiftError1","Node2-Config-Wrong-Swift-SwiftError1","Node2-Config-Wrong-Swift-SwiftError1", false,tags , null, null, index, null));

        //--2-- SwiftError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Swift"));
        tags.addAll(Arrays.asList("SwiftError2"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Swift-SwiftError2","Node2-Config-Wrong-Swift-SwiftError2","Node2-Config-Wrong-Swift-SwiftError2", false,tags , null, null, index, null));

        //--3-- SwiftError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Swift"));
        tags.addAll(Arrays.asList("SwiftError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Swift-SwiftError3","Node2-Config-Wrong-Swift-SwiftError3","Node2-Config-Wrong-Swift-SwiftError3", false,tags , null, null, index, null));
    }

    private void createNode2ConfigWrongGlance() {
        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node2-Config-Wrong-Glance-GlanceError1","Node2-Config-Wrong-Glance-GlanceError2","Node2-Config-Wrong-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Glance","Glance Failure","Category for Glance Config-Wrong of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigWrong -> Glance -> GlanceError1&GlanceError2&GlanceError3
        //--1-- GlanceError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Glance"));
        tags.addAll(Arrays.asList("GlanceError1"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Glance-GlanceError1","Node2-Config-Wrong-Glance-GlanceError1","Node2-Config-Wrong-Glance-GlanceError1", false,tags , null, null, index, null));

        //--2-- GlanceError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Glance"));
        tags.addAll(Arrays.asList("GlanceError2"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Glance-GlanceError2","Node2-Config-Wrong-Glance-GlanceError2","Node2-Config-Wrong-Glance-GlanceError2", false,tags , null, null, index, null));

        //--3-- GlanceError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Glance"));
        tags.addAll(Arrays.asList("GlanceError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Glance-GlanceError3","Node2-Config-Wrong-Glance-GlanceError3","Node2-Config-Wrong-Glance-GlanceError3", false,tags , null, null, index, null));

    }

    private void createNode2ConfigWrongCinder() {
        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node2-Config-Wrong-Cinder-CinderError1","Node2-Config-Wrong-Cinder-CinderError2","Node2-Config-Wrong-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Cinder","Cinder Failure","Category for Cinder Config-Wrong of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigWrong -> Cinder -> CinderError1&CinderError2&CinderError3
        //--1-- CinderError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Cinder"));
        tags.addAll(Arrays.asList("CinderError1"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Cinder-CinderError1","Node2-Config-Wrong-Cinder-CinderError1","Node2-Config-Wrong-Cinder-CinderError1", false,tags , null, null, index, null));

        //--2-- CinderError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Cinder"));
        tags.addAll(Arrays.asList("CinderError2"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Cinder-CinderError2","Node2-Config-Wrong-Cinder-CinderError2","Node2-Config-Wrong-Cinder-CinderError2", false,tags , null, null, index, null));

        //--3-- CinderError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Cinder"));
        tags.addAll(Arrays.asList("CinderError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Cinder-CinderError3","Node2-Config-Wrong-Cinder-CinderError3","Node2-Config-Wrong-Cinder-CinderError3", false,tags , null, null, index, null));
    }

    private void createNode2ConfigWrongKeystone() {
        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node2-Config-Wrong-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Keystone","Keystone Failure","Category for Keystone Config-Wrong of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigWrong -> Keystone -> keystone
        //--keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Keystone"));
        tags.addAll(Arrays.asList("Keystone"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Keystone-KeystoneService","Node2-Config-Wrong-Keystone-KeystoneService","Node2-Config-Wrong-Keystone-KeystoneService", false,tags , null, null, index, null));
    }

    private void createNode2ConfigWrongNeutron() {
        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong"));
        tags.addAll(Arrays.asList("Neutron","Network"));
        children.addAll(Arrays.asList("Node2-Config-Wrong-Neutron-NeutronServer","Node2-Config-Wrong-Neutron-L3Agent","Node2-Config-Wrong-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Neutron","Neutron Failure","Category for Neutron Config-Wrong of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigWrong -> Neutron -> neutronServer& L3Agent&L2Agent
        //--1-- neutronServer
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Neutron"));
        tags.addAll(Arrays.asList("NeutronServer"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Neutron-NeutronServer","Node2-Config-Wrong-Neutron-NeutronServer","Node2-Config-Wrong-Neutron-NeutronServer", false,tags , null, null, index, null));

        //--2-- L3Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Neutron"));
        tags.addAll(Arrays.asList("L3Agent"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Neutron-L3Agent","Node2-Config-Wrong-Neutron-L3Agent","Node2-Config-Wrong-Neutron-L3Agent", false,tags , null, null, index, null));

        //--3-- L2Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Neutron"));
        tags.addAll(Arrays.asList("L2Agent"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Neutron-L2Agent","Node2-Config-Wrong-Neutron-L2Agent","Node2-Config-Wrong-Neutron-L2Agent", false,tags , null, null, index, null));
    }

    private void createNode2ConfigWrongNova() {
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node2-Config-Wrong-Nova-NovaApi","Node2-Config-Wrong-Nova-NovaCompute","Node2-Config-Wrong-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Nova","Nova Failure","Category for Nova Config-Wrong of Node2.", true,tags , null, null, index, children));


        //Node2 -> Software -> ConfigWrong -> Nova -> novaApi&novaCompute&novaScheduler
        //--1-- novaApi
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Nova"));
        tags.addAll(Arrays.asList("NovaApi"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Nova-NovaApi","Node2-Config-Wrong-Nova-NovaApi","Node2-Config-Wrong-Nova-NovaApi", false,tags , null, null, index, null));

        //--2-- novaCompute
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Nova"));
        tags.addAll(Arrays.asList("NovaCompute"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Nova-NovaCompute","Node2-Config-Wrong-Nova-NovaCompute","Node2-Config-Wrong-Nova-NovaCompute", false,tags , null, null, index, null));

        //--3-- novaScheduler
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Wrong","Node2-Config-Wrong-Nova"));
        tags.addAll(Arrays.asList("NovaScheduler"));
        failureRepository.save(new CloudFailure("Node2-Config-Wrong-Nova-NovaScheduler","Node2-Config-Wrong-Nova-NovaScheduler","Node2-Config-Wrong-Nova-NovaScheduler", false,tags , null, null, index, null));
    }


    public void createFaultTreeNode2SoftwareConfigLoss(){

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure"));
        tags.addAll(Arrays.asList("ConfigLoss"));
        children.addAll(Arrays.asList("Node2-Config-Loss-Nova","Node2-Config-Loss-Neutron","Node2-Config-Loss-Keystone",
                "Node2-Config-Loss-Cinder","Node2-Config-Loss-Glance","Node2-Config-Loss-Swift","Node2-Config-Loss-Ironic"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss","Config Loss","Category for Config-Loss of Node2.", true,tags , null, null, index, children));

        createNode2ConfigLossNova();
        createNode2ConfigLossNeutron();
        createNode2ConfigLossKeystone();
        createNode2ConfigLossCinder();
        createNode2ConfigLossGlance();
        createNode2ConfigLossSwift();
        createNode2ConfigLossIronic();
    }

    private void createNode2ConfigLossIronic() {
        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node2-Config-Loss-Ironic-IronicError1","Node2-Config-Loss-Ironic-IronicError2","Node2-Config-Loss-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Ironic","Ironic Failure","Category for Ironic Config-Loss of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigLoss -> Ironic -> IronicError1&IronicError2&IronicError3
        //--1-- IronicError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Ironic"));
        tags.addAll(Arrays.asList("IronicError1"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Ironic-IronicError1","Node2-Config-Loss-Ironic-IronicError1","Node2-Config-Loss-Ironic-IronicError1", false,tags , null, null, index, null));

        //--2-- IronicError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Ironic"));
        tags.addAll(Arrays.asList("IronicError2"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Ironic-IronicError2","Node2-Config-Loss-Ironic-IronicError2","Node2-Config-Loss-Ironic-IronicError2", false,tags , null, null, index, null));

        //--3-- IronicError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Ironic"));
        tags.addAll(Arrays.asList("IronicError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Ironic-IronicError3","Node2-Config-Loss-Ironic-IronicError3","Node2-Config-Loss-Ironic-IronicError3", false,tags , null, null, index, null));
    }

    private void createNode2ConfigLossSwift() {
        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node2-Config-Loss-Swift-SwiftError1","Node2-Config-Loss-Swift-SwiftError2","Node2-Config-Loss-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Swift","Swift Failure","Category for Swift Config-Loss of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigLoss -> Swift -> SwiftError1&SwiftError2&SwiftError3
        //--1-- SwiftError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Swift"));
        tags.addAll(Arrays.asList("SwiftError1"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Swift-SwiftError1","Node2-Config-Loss-Swift-SwiftError1","Node2-Config-Loss-Swift-SwiftError1", false,tags , null, null, index, null));

        //--2-- SwiftError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Swift"));
        tags.addAll(Arrays.asList("SwiftError2"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Swift-SwiftError2","Node2-Config-Loss-Swift-SwiftError2","Node2-Config-Loss-Swift-SwiftError2", false,tags , null, null, index, null));

        //--3-- SwiftError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Swift"));
        tags.addAll(Arrays.asList("SwiftError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Swift-SwiftError3","Node2-Config-Loss-Swift-SwiftError3","Node2-Config-Loss-Swift-SwiftError3", false,tags , null, null, index, null));
    }

    private void createNode2ConfigLossGlance() {
        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node2-Config-Loss-Glance-GlanceError1","Node2-Config-Loss-Glance-GlanceError2","Node2-Config-Loss-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Glance","Glance Failure","Category for Glance Config-Loss of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigLoss -> Glance -> GlanceError1&GlanceError2&GlanceError3
        //--1-- GlanceError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Glance"));
        tags.addAll(Arrays.asList("GlanceError1"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Glance-GlanceError1","Node2-Config-Loss-Glance-GlanceError1","Node2-Config-Loss-Glance-GlanceError1", false,tags , null, null, index, null));

        //--2-- GlanceError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Glance"));
        tags.addAll(Arrays.asList("GlanceError2"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Glance-GlanceError2","Node2-Config-Loss-Glance-GlanceError2","Node2-Config-Loss-Glance-GlanceError2", false,tags , null, null, index, null));

        //--3-- GlanceError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Glance"));
        tags.addAll(Arrays.asList("GlanceError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Glance-GlanceError3","Node2-Config-Loss-Glance-GlanceError3","Node2-Config-Loss-Glance-GlanceError3", false,tags , null, null, index, null));

    }

    private void createNode2ConfigLossCinder() {
        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node2-Config-Loss-Cinder-CinderError1","Node2-Config-Loss-Cinder-CinderError2","Node2-Config-Loss-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Cinder","Cinder Failure","Category for Cinder Config-Loss of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigLoss -> Cinder -> CinderError1&CinderError2&CinderError3
        //--1-- CinderError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Cinder"));
        tags.addAll(Arrays.asList("CinderError1"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Cinder-CinderError1","Node2-Config-Loss-Cinder-CinderError1","Node2-Config-Loss-Cinder-CinderError1", false,tags , null, null, index, null));

        //--2-- CinderError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Cinder"));
        tags.addAll(Arrays.asList("CinderError2"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Cinder-CinderError2","Node2-Config-Loss-Cinder-CinderError2","Node2-Config-Loss-Cinder-CinderError2", false,tags , null, null, index, null));

        //--3-- CinderError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Cinder"));
        tags.addAll(Arrays.asList("CinderError3"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Cinder-CinderError3","Node2-Config-Loss-Cinder-CinderError3","Node2-Config-Loss-Cinder-CinderError3", false,tags , null, null, index, null));
    }

    private void createNode2ConfigLossKeystone() {
        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node2-Config-Loss-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Keystone","Keystone Failure","Category for Keystone Config-Loss of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigLoss -> Keystone -> keystone
        //--keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Keystone"));
        tags.addAll(Arrays.asList("Keystone"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Keystone-KeystoneService","Node2-Config-Loss-Keystone-KeystoneService","Node2-Config-Loss-Keystone-KeystoneService", false,tags , null, null, index, null));
    }

    private void createNode2ConfigLossNeutron() {
        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss"));
        tags.addAll(Arrays.asList("Neutron","Network"));
        children.addAll(Arrays.asList("Node2-Config-Loss-Neutron-NeutronServer","Node2-Config-Loss-Neutron-L3Agent","Node2-Config-Loss-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Neutron","Neutron Failure","Category for Neutron Config-Loss of Node2.", true,tags , null, null, index, children));

        //Node2 -> Software -> ConfigLoss -> Neutron -> neutronServer& L3Agent&L2Agent
        //--1-- neutronServer
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Neutron"));
        tags.addAll(Arrays.asList("NeutronServer"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Neutron-NeutronServer","Node2-Config-Loss-Neutron-NeutronServer","Node2-Config-Loss-Neutron-NeutronServer", false,tags , null, null, index, null));

        //--2-- L3Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Neutron"));
        tags.addAll(Arrays.asList("L3Agent"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Neutron-L3Agent","Node2-Config-Loss-Neutron-L3Agent","Node2-Config-Loss-Neutron-L3Agent", false,tags , null, null, index, null));

        //--3-- L2Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Neutron"));
        tags.addAll(Arrays.asList("L2Agent"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Neutron-L2Agent","Node2-Config-Loss-Neutron-L2Agent","Node2-Config-Loss-Neutron-L2Agent", false,tags , null, null, index, null));
    }

    private void createNode2ConfigLossNova() {
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node2-Config-Loss-Nova-NovaApi","Node2-Config-Loss-Nova-NovaCompute","Node2-Config-Loss-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Nova","Nova Failure","Category for Nova Config-Loss of Node2.", true,tags , null, null, index, children));


        //Node2 -> Software -> ConfigLoss -> Nova -> novaApi&novaCompute&novaScheduler
        //--1-- novaApi
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Nova"));
        tags.addAll(Arrays.asList("NovaApi"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Nova-NovaApi","Node2-Config-Loss-Nova-NovaApi","Node2-Config-Loss-Nova-NovaApi", false,tags , null, null, index, null));

        //--2-- novaCompute
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Nova"));
        tags.addAll(Arrays.asList("NovaCompute"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Nova-NovaCompute","Node2-Config-Loss-Nova-NovaCompute","Node2-Config-Loss-Nova-NovaCompute", false,tags , null, null, index, null));

        //--3-- novaScheduler
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node2-Failure","Node2-Software-Failure","Node2-Config-Loss","Node2-Config-Loss-Nova"));
        tags.addAll(Arrays.asList("NovaScheduler"));
        failureRepository.save(new CloudFailure("Node2-Config-Loss-Nova-NovaScheduler","Node2-Config-Loss-Nova-NovaScheduler","Node2-Config-Loss-Nova-NovaScheduler", false,tags , null, null, index, null));
    }


    public void createFaultTreeNode3Hardware(){
        ArrayList<String> children = new ArrayList<>();
        ArrayList<String> index = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();

        //Node3 -> HardWare
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure"));
        tags.addAll(Arrays.asList("Hardware"));
        children.addAll(Arrays.asList("Node3-CPU-Full","Node3-Disk-Full","Node3-Power-Off"));
        failureRepository.save(new CloudFailure("Node3-Hardware-Failure","Hardware Failure","Category for hardware failures of Node3.", true,tags , null, null, index, children));

        //Node3 -> Hardware -> CPU&Disk&PowerOff

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Hardware-Failure"));
        tags.addAll(Arrays.asList("CPU"));
        failureRepository.save(new CloudFailure("Node3-CPU-Full","Node3-CPU-Full Failure","Node3-CPU-Full Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Hardware-Failure"));
        tags.addAll(Arrays.asList("Disk"));
        failureRepository.save(new CloudFailure("Node3-Disk-Full","Node3-Disk-Full Failure","Node3-Disk-Full Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Hardware-Failure"));
        tags.addAll(Arrays.asList("PowerOff"));
        failureRepository.save(new CloudFailure("Node3-Power-Off","Node3-Power-Off Failure","Node3-PowerOff Failure.", false,tags , null, null, index, null));

    }

    public void createFaultTreeNode3Network(){
        ArrayList<String> children = new ArrayList<>();
        ArrayList<String> index = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();

        //Node3 -> Network
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure"));
        tags.addAll(Arrays.asList("Network"));
        children.addAll(Arrays.asList("Node3-Package-Lag","Node3-Package-Loss","Node3-NIC-Down"));
        failureRepository.save(new CloudFailure("Node3-Network-Failure","Network Failure","Category for network failures of Node3.", true,tags , null, null, index, children));

        //Node3 -> Network -> Package Lag&loss&NIC down

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Network-Failure"));
        tags.addAll(Arrays.asList("PackageLag"));
        failureRepository.save(new CloudFailure("Node3-Package-Lag","Node3-Package-Lag Failure","Node3-Package-Lag Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Network-Failure"));
        tags.addAll(Arrays.asList("PackageLoss"));
        failureRepository.save(new CloudFailure("Node3-Package-Loss","Node3-Package-Loss Failure","Node3-Package-Loss Failure.", false,tags , null, null, index, null));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Network-Failure"));
        tags.addAll(Arrays.asList("NIC-Down"));
        failureRepository.save(new CloudFailure("Node3-NIC-Down","Node3-NIC-Down Failure","Node3-NIC-Down Failure.", false,tags , null, null, index, null));
    }

    public void createFaultTreeNode3Software(){
        //Node3 -> Software
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure"));
        tags.addAll(Arrays.asList("Software"));
        children.addAll(Arrays.asList("Node3-Process-Crash","Node3-Config-Loss","Node3-Config-Wrong"));
        failureRepository.save(new CloudFailure("Node3-Software-Failure","Software Failure","Category for Software failures of Node3.", true,tags , null, null, index, children));

        createFaultTreeNode3SoftwareProcess();
        createFaultTreeNode3SoftwareConfigLoss();
        createFaultTreeNode3SoftwareConfigWrong();
    }

    public void createFaultTreeNode3SoftwareProcess(){
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure"));
        tags.addAll(Arrays.asList("ProcessCrash"));
        children.addAll(Arrays.asList("Node3-Process-Crash-Nova","Node3-Process-Crash-Neutron","Node3-Process-Crash-Keystone",
                "Node3-Process-Crash-Cinder","Node3-Process-Crash-Glance","Node3-Process-Crash-Swift","Node3-Process-Crash-Ironic"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash","Process Crash","Category for Process-Crash of Node3.", true,tags , null, null, index, children));

        createNode3ProcessCrashNova();
        createNode3ProcessCrashNeutron();
        createNode3ProcessCrashKeystone();
        createNode3ProcessCrashCinder();
        createNode3ProcessCrashGlance();
        createNode3ProcessCrashSwift();
        createNode3ProcessCrashIronic();
    }

    private void createNode3ProcessCrashIronic() {
        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node3-Process-Crash-Ironic-IronicError1","Node3-Process-Crash-Ironic-IronicError2","Node3-Process-Crash-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Ironic","Ironic Failure","Category for Ironic Process-Crash of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ProcessCrash -> Ironic -> IronicError1&IronicError2&IronicError3
        //--1-- IronicError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Ironic"));
        tags.addAll(Arrays.asList("IronicError1"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Ironic-IronicError1","Node3-Process-Crash-Ironic-IronicError1","Node3-Process-Crash-Ironic-IronicError1", false,tags , null, null, index, null));

        //--2-- IronicError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Ironic"));
        tags.addAll(Arrays.asList("IronicError2"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Ironic-IronicError2","Node3-Process-Crash-Ironic-IronicError2","Node3-Process-Crash-Ironic-IronicError2", false,tags , null, null, index, null));

        //--3-- IronicError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Ironic"));
        tags.addAll(Arrays.asList("IronicError3"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Ironic-IronicError3","Node3-Process-Crash-Ironic-IronicError3","Node3-Process-Crash-Ironic-IronicError3", false,tags , null, null, index, null));
    }

    private void createNode3ProcessCrashSwift() {
        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node3-Process-Crash-Swift-SwiftError1","Node3-Process-Crash-Swift-SwiftError2","Node3-Process-Crash-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Swift","Swift Failure","Category for Swift Process-Crash of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ProcessCrash -> Swift -> SwiftError1&SwiftError2&SwiftError3
        //--1-- SwiftError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Swift"));
        tags.addAll(Arrays.asList("SwiftError1"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Swift-SwiftError1","Node3-Process-Crash-Swift-SwiftError1","Node3-Process-Crash-Swift-SwiftError1", false,tags , null, null, index, null));

        //--2-- SwiftError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Swift"));
        tags.addAll(Arrays.asList("SwiftError2"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Swift-SwiftError2","Node3-Process-Crash-Swift-SwiftError2","Node3-Process-Crash-Swift-SwiftError2", false,tags , null, null, index, null));

        //--3-- SwiftError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Swift"));
        tags.addAll(Arrays.asList("SwiftError3"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Swift-SwiftError3","Node3-Process-Crash-Swift-SwiftError3","Node3-Process-Crash-Swift-SwiftError3", false,tags , null, null, index, null));
    }

    private void createNode3ProcessCrashGlance() {
        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node3-Process-Crash-Glance-GlanceError1","Node3-Process-Crash-Glance-GlanceError2","Node3-Process-Crash-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Glance","Glance Failure","Category for Glance Process-Crash of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ProcessCrash -> Glance -> GlanceError1&GlanceError2&GlanceError3
        //--1-- GlanceError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Glance"));
        tags.addAll(Arrays.asList("GlanceError1"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Glance-GlanceError1","Node3-Process-Crash-Glance-GlanceError1","Node3-Process-Crash-Glance-GlanceError1", false,tags , null, null, index, null));

        //--2-- GlanceError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Glance"));
        tags.addAll(Arrays.asList("GlanceError2"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Glance-GlanceError2","Node3-Process-Crash-Glance-GlanceError2","Node3-Process-Crash-Glance-GlanceError2", false,tags , null, null, index, null));

        //--3-- GlanceError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Glance"));
        tags.addAll(Arrays.asList("GlanceError3"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Glance-GlanceError3","Node3-Process-Crash-Glance-GlanceError3","Node3-Process-Crash-Glance-GlanceError3", false,tags , null, null, index, null));

    }

    private void createNode3ProcessCrashCinder() {
        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node3-Process-Crash-Cinder-CinderError1","Node3-Process-Crash-Cinder-CinderError2","Node3-Process-Crash-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Cinder","Cinder Failure","Category for Cinder Process-Crash of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ProcessCrash -> Cinder -> CinderError1&CinderError2&CinderError3
        //--1-- CinderError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Cinder"));
        tags.addAll(Arrays.asList("CinderError1"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Cinder-CinderError1","Node3-Process-Crash-Cinder-CinderError1","Node3-Process-Crash-Cinder-CinderError1", false,tags , null, null, index, null));

        //--2-- CinderError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Cinder"));
        tags.addAll(Arrays.asList("CinderError2"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Cinder-CinderError2","Node3-Process-Crash-Cinder-CinderError2","Node3-Process-Crash-Cinder-CinderError2", false,tags , null, null, index, null));

        //--3-- CinderError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Cinder"));
        tags.addAll(Arrays.asList("CinderError3"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Cinder-CinderError3","Node3-Process-Crash-Cinder-CinderError3","Node3-Process-Crash-Cinder-CinderError3", false,tags , null, null, index, null));
    }

    private void createNode3ProcessCrashKeystone() {
        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node3-Process-Crash-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Keystone","Keystone Failure","Category for Keystone Process-Crash of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ProcessCrash -> Keystone -> keystone
        //--keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Keystone"));
        tags.addAll(Arrays.asList("Keystone"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Keystone-KeystoneService","Node3-Process-Crash-Keystone-KeystoneService","Node3-Process-Crash-Keystone-KeystoneService", false,tags , null, null, index, null));
    }

    private void createNode3ProcessCrashNeutron() {
        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash"));
        tags.addAll(Arrays.asList("Neutron","Network"));
        children.addAll(Arrays.asList("Node3-Process-Crash-Neutron-NeutronServer","Node3-Process-Crash-Neutron-L3Agent","Node3-Process-Crash-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Neutron","Neutron Failure","Category for Neutron Process-Crash of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ProcessCrash -> Neutron -> neutronServer& L3Agent&L2Agent
        //--1-- neutronServer
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Neutron"));
        tags.addAll(Arrays.asList("NeutronServer"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Neutron-NeutronServer","Node3-Process-Crash-Neutron-NeutronServer","Node3-Process-Crash-Neutron-NeutronServer", false,tags , null, null, index, null));

        //--2-- L3Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Neutron"));
        tags.addAll(Arrays.asList("L3Agent"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Neutron-L3Agent","Node3-Process-Crash-Neutron-L3Agent","Node3-Process-Crash-Neutron-L3Agent", false,tags , null, null, index, null));

        //--3-- L2Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Neutron"));
        tags.addAll(Arrays.asList("L2Agent"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Neutron-L2Agent","Node3-Process-Crash-Neutron-L2Agent","Node3-Process-Crash-Neutron-L2Agent", false,tags , null, null, index, null));
    }

    private void createNode3ProcessCrashNova() {
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node3-Process-Crash-Nova-NovaApi","Node3-Process-Crash-Nova-NovaCompute","Node3-Process-Crash-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Nova","Nova Failure","Category for Nova Process-Crash of Node3.", true,tags , null, null, index, children));


        //Node3 -> Software -> ProcessCrash -> Nova -> novaApi&novaCompute&novaScheduler
        //--1-- novaApi
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Nova"));
        tags.addAll(Arrays.asList("NovaApi"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Nova-NovaApi","Node3-Process-Crash-Nova-NovaApi","Node3-Process-Crash-Nova-NovaApi", false,tags , null, null, index, null));

        //--2-- novaCompute
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Nova"));
        tags.addAll(Arrays.asList("NovaCompute"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Nova-NovaCompute","Node3-Process-Crash-Nova-NovaCompute","Node3-Process-Crash-Nova-NovaCompute", false,tags , null, null, index, null));

        //--3-- novaScheduler
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Process-Crash","Node3-Process-Crash-Nova"));
        tags.addAll(Arrays.asList("NovaScheduler"));
        failureRepository.save(new CloudFailure("Node3-Process-Crash-Nova-NovaScheduler","Node3-Process-Crash-Nova-NovaScheduler","Node3-Process-Crash-Nova-NovaScheduler", false,tags , null, null, index, null));
    }


    public void createFaultTreeNode3SoftwareConfigWrong(){

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure"));
        tags.addAll(Arrays.asList("ConfigWrong"));
        children.addAll(Arrays.asList("Node3-Config-Wrong-Nova","Node3-Config-Wrong-Neutron","Node3-Config-Wrong-Keystone",
                "Node3-Config-Wrong-Cinder","Node3-Config-Wrong-Glance","Node3-Config-Wrong-Swift","Node3-Config-Wrong-Ironic"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong","Config Wrong","Category for Config-Wrong of Node3.", true,tags , null, null, index, children));

        createNode3ConfigWrongNova();
        createNode3ConfigWrongNeutron();
        createNode3ConfigWrongKeystone();
        createNode3ConfigWrongCinder();
        createNode3ConfigWrongGlance();
        createNode3ConfigWrongSwift();
        createNode3ConfigWrongIronic();
    }

    private void createNode3ConfigWrongIronic() {
        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node3-Config-Wrong-Ironic-IronicError1","Node3-Config-Wrong-Ironic-IronicError2","Node3-Config-Wrong-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Ironic","Ironic Failure","Category for Ironic Config-Wrong of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigWrong -> Ironic -> IronicError1&IronicError2&IronicError3
        //--1-- IronicError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Ironic"));
        tags.addAll(Arrays.asList("IronicError1"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Ironic-IronicError1","Node3-Config-Wrong-Ironic-IronicError1","Node3-Config-Wrong-Ironic-IronicError1", false,tags , null, null, index, null));

        //--2-- IronicError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Ironic"));
        tags.addAll(Arrays.asList("IronicError2"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Ironic-IronicError2","Node3-Config-Wrong-Ironic-IronicError2","Node3-Config-Wrong-Ironic-IronicError2", false,tags , null, null, index, null));

        //--3-- IronicError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Ironic"));
        tags.addAll(Arrays.asList("IronicError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Ironic-IronicError3","Node3-Config-Wrong-Ironic-IronicError3","Node3-Config-Wrong-Ironic-IronicError3", false,tags , null, null, index, null));
    }

    private void createNode3ConfigWrongSwift() {
        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node3-Config-Wrong-Swift-SwiftError1","Node3-Config-Wrong-Swift-SwiftError2","Node3-Config-Wrong-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Swift","Swift Failure","Category for Swift Config-Wrong of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigWrong -> Swift -> SwiftError1&SwiftError2&SwiftError3
        //--1-- SwiftError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Swift"));
        tags.addAll(Arrays.asList("SwiftError1"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Swift-SwiftError1","Node3-Config-Wrong-Swift-SwiftError1","Node3-Config-Wrong-Swift-SwiftError1", false,tags , null, null, index, null));

        //--2-- SwiftError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Swift"));
        tags.addAll(Arrays.asList("SwiftError2"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Swift-SwiftError2","Node3-Config-Wrong-Swift-SwiftError2","Node3-Config-Wrong-Swift-SwiftError2", false,tags , null, null, index, null));

        //--3-- SwiftError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Swift"));
        tags.addAll(Arrays.asList("SwiftError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Swift-SwiftError3","Node3-Config-Wrong-Swift-SwiftError3","Node3-Config-Wrong-Swift-SwiftError3", false,tags , null, null, index, null));
    }

    private void createNode3ConfigWrongGlance() {
        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node3-Config-Wrong-Glance-GlanceError1","Node3-Config-Wrong-Glance-GlanceError2","Node3-Config-Wrong-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Glance","Glance Failure","Category for Glance Config-Wrong of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigWrong -> Glance -> GlanceError1&GlanceError2&GlanceError3
        //--1-- GlanceError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Glance"));
        tags.addAll(Arrays.asList("GlanceError1"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Glance-GlanceError1","Node3-Config-Wrong-Glance-GlanceError1","Node3-Config-Wrong-Glance-GlanceError1", false,tags , null, null, index, null));

        //--2-- GlanceError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Glance"));
        tags.addAll(Arrays.asList("GlanceError2"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Glance-GlanceError2","Node3-Config-Wrong-Glance-GlanceError2","Node3-Config-Wrong-Glance-GlanceError2", false,tags , null, null, index, null));

        //--3-- GlanceError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Glance"));
        tags.addAll(Arrays.asList("GlanceError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Glance-GlanceError3","Node3-Config-Wrong-Glance-GlanceError3","Node3-Config-Wrong-Glance-GlanceError3", false,tags , null, null, index, null));

    }

    private void createNode3ConfigWrongCinder() {
        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node3-Config-Wrong-Cinder-CinderError1","Node3-Config-Wrong-Cinder-CinderError2","Node3-Config-Wrong-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Cinder","Cinder Failure","Category for Cinder Config-Wrong of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigWrong -> Cinder -> CinderError1&CinderError2&CinderError3
        //--1-- CinderError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Cinder"));
        tags.addAll(Arrays.asList("CinderError1"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Cinder-CinderError1","Node3-Config-Wrong-Cinder-CinderError1","Node3-Config-Wrong-Cinder-CinderError1", false,tags , null, null, index, null));

        //--2-- CinderError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Cinder"));
        tags.addAll(Arrays.asList("CinderError2"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Cinder-CinderError2","Node3-Config-Wrong-Cinder-CinderError2","Node3-Config-Wrong-Cinder-CinderError2", false,tags , null, null, index, null));

        //--3-- CinderError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Cinder"));
        tags.addAll(Arrays.asList("CinderError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Cinder-CinderError3","Node3-Config-Wrong-Cinder-CinderError3","Node3-Config-Wrong-Cinder-CinderError3", false,tags , null, null, index, null));
    }

    private void createNode3ConfigWrongKeystone() {
        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node3-Config-Wrong-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Keystone","Keystone Failure","Category for Keystone Config-Wrong of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigWrong -> Keystone -> keystone
        //--keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Keystone"));
        tags.addAll(Arrays.asList("Keystone"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Keystone-KeystoneService","Node3-Config-Wrong-Keystone-KeystoneService","Node3-Config-Wrong-Keystone-KeystoneService", false,tags , null, null, index, null));
    }

    private void createNode3ConfigWrongNeutron() {
        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong"));
        tags.addAll(Arrays.asList("Neutron","Network"));
        children.addAll(Arrays.asList("Node3-Config-Wrong-Neutron-NeutronServer","Node3-Config-Wrong-Neutron-L3Agent","Node3-Config-Wrong-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Neutron","Neutron Failure","Category for Neutron Config-Wrong of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigWrong -> Neutron -> neutronServer& L3Agent&L2Agent
        //--1-- neutronServer
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Neutron"));
        tags.addAll(Arrays.asList("NeutronServer"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Neutron-NeutronServer","Node3-Config-Wrong-Neutron-NeutronServer","Node3-Config-Wrong-Neutron-NeutronServer", false,tags , null, null, index, null));

        //--2-- L3Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Neutron"));
        tags.addAll(Arrays.asList("L3Agent"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Neutron-L3Agent","Node3-Config-Wrong-Neutron-L3Agent","Node3-Config-Wrong-Neutron-L3Agent", false,tags , null, null, index, null));

        //--3-- L2Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Neutron"));
        tags.addAll(Arrays.asList("L2Agent"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Neutron-L2Agent","Node3-Config-Wrong-Neutron-L2Agent","Node3-Config-Wrong-Neutron-L2Agent", false,tags , null, null, index, null));
    }

    private void createNode3ConfigWrongNova() {
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node3-Config-Wrong-Nova-NovaApi","Node3-Config-Wrong-Nova-NovaCompute","Node3-Config-Wrong-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Nova","Nova Failure","Category for Nova Config-Wrong of Node3.", true,tags , null, null, index, children));


        //Node3 -> Software -> ConfigWrong -> Nova -> novaApi&novaCompute&novaScheduler
        //--1-- novaApi
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Nova"));
        tags.addAll(Arrays.asList("NovaApi"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Nova-NovaApi","Node3-Config-Wrong-Nova-NovaApi","Node3-Config-Wrong-Nova-NovaApi", false,tags , null, null, index, null));

        //--2-- novaCompute
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Nova"));
        tags.addAll(Arrays.asList("NovaCompute"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Nova-NovaCompute","Node3-Config-Wrong-Nova-NovaCompute","Node3-Config-Wrong-Nova-NovaCompute", false,tags , null, null, index, null));

        //--3-- novaScheduler
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Wrong","Node3-Config-Wrong-Nova"));
        tags.addAll(Arrays.asList("NovaScheduler"));
        failureRepository.save(new CloudFailure("Node3-Config-Wrong-Nova-NovaScheduler","Node3-Config-Wrong-Nova-NovaScheduler","Node3-Config-Wrong-Nova-NovaScheduler", false,tags , null, null, index, null));
    }


    public void createFaultTreeNode3SoftwareConfigLoss(){

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure"));
        tags.addAll(Arrays.asList("ConfigLoss"));
        children.addAll(Arrays.asList("Node3-Config-Loss-Nova","Node3-Config-Loss-Neutron","Node3-Config-Loss-Keystone",
                "Node3-Config-Loss-Cinder","Node3-Config-Loss-Glance","Node3-Config-Loss-Swift","Node3-Config-Loss-Ironic"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss","Config Loss","Category for Config-Loss of Node3.", true,tags , null, null, index, children));

        createNode3ConfigLossNova();
        createNode3ConfigLossNeutron();
        createNode3ConfigLossKeystone();
        createNode3ConfigLossCinder();
        createNode3ConfigLossGlance();
        createNode3ConfigLossSwift();
        createNode3ConfigLossIronic();
    }

    private void createNode3ConfigLossIronic() {
        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node3-Config-Loss-Ironic-IronicError1","Node3-Config-Loss-Ironic-IronicError2","Node3-Config-Loss-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Ironic","Ironic Failure","Category for Ironic Config-Loss of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigLoss -> Ironic -> IronicError1&IronicError2&IronicError3
        //--1-- IronicError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Ironic"));
        tags.addAll(Arrays.asList("IronicError1"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Ironic-IronicError1","Node3-Config-Loss-Ironic-IronicError1","Node3-Config-Loss-Ironic-IronicError1", false,tags , null, null, index, null));

        //--2-- IronicError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Ironic"));
        tags.addAll(Arrays.asList("IronicError2"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Ironic-IronicError2","Node3-Config-Loss-Ironic-IronicError2","Node3-Config-Loss-Ironic-IronicError2", false,tags , null, null, index, null));

        //--3-- IronicError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Ironic"));
        tags.addAll(Arrays.asList("IronicError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Ironic-IronicError3","Node3-Config-Loss-Ironic-IronicError3","Node3-Config-Loss-Ironic-IronicError3", false,tags , null, null, index, null));
    }

    private void createNode3ConfigLossSwift() {
        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node3-Config-Loss-Swift-SwiftError1","Node3-Config-Loss-Swift-SwiftError2","Node3-Config-Loss-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Swift","Swift Failure","Category for Swift Config-Loss of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigLoss -> Swift -> SwiftError1&SwiftError2&SwiftError3
        //--1-- SwiftError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Swift"));
        tags.addAll(Arrays.asList("SwiftError1"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Swift-SwiftError1","Node3-Config-Loss-Swift-SwiftError1","Node3-Config-Loss-Swift-SwiftError1", false,tags , null, null, index, null));

        //--2-- SwiftError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Swift"));
        tags.addAll(Arrays.asList("SwiftError2"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Swift-SwiftError2","Node3-Config-Loss-Swift-SwiftError2","Node3-Config-Loss-Swift-SwiftError2", false,tags , null, null, index, null));

        //--3-- SwiftError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Swift"));
        tags.addAll(Arrays.asList("SwiftError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Swift-SwiftError3","Node3-Config-Loss-Swift-SwiftError3","Node3-Config-Loss-Swift-SwiftError3", false,tags , null, null, index, null));
    }

    private void createNode3ConfigLossGlance() {
        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node3-Config-Loss-Glance-GlanceError1","Node3-Config-Loss-Glance-GlanceError2","Node3-Config-Loss-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Glance","Glance Failure","Category for Glance Config-Loss of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigLoss -> Glance -> GlanceError1&GlanceError2&GlanceError3
        //--1-- GlanceError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Glance"));
        tags.addAll(Arrays.asList("GlanceError1"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Glance-GlanceError1","Node3-Config-Loss-Glance-GlanceError1","Node3-Config-Loss-Glance-GlanceError1", false,tags , null, null, index, null));

        //--2-- GlanceError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Glance"));
        tags.addAll(Arrays.asList("GlanceError2"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Glance-GlanceError2","Node3-Config-Loss-Glance-GlanceError2","Node3-Config-Loss-Glance-GlanceError2", false,tags , null, null, index, null));

        //--3-- GlanceError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Glance"));
        tags.addAll(Arrays.asList("GlanceError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Glance-GlanceError3","Node3-Config-Loss-Glance-GlanceError3","Node3-Config-Loss-Glance-GlanceError3", false,tags , null, null, index, null));

    }

    private void createNode3ConfigLossCinder() {
        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node3-Config-Loss-Cinder-CinderError1","Node3-Config-Loss-Cinder-CinderError2","Node3-Config-Loss-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Cinder","Cinder Failure","Category for Cinder Config-Loss of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigLoss -> Cinder -> CinderError1&CinderError2&CinderError3
        //--1-- CinderError1
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Cinder"));
        tags.addAll(Arrays.asList("CinderError1"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Cinder-CinderError1","Node3-Config-Loss-Cinder-CinderError1","Node3-Config-Loss-Cinder-CinderError1", false,tags , null, null, index, null));

        //--2-- CinderError2
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Cinder"));
        tags.addAll(Arrays.asList("CinderError2"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Cinder-CinderError2","Node3-Config-Loss-Cinder-CinderError2","Node3-Config-Loss-Cinder-CinderError2", false,tags , null, null, index, null));

        //--3-- CinderError3
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Cinder"));
        tags.addAll(Arrays.asList("CinderError3"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Cinder-CinderError3","Node3-Config-Loss-Cinder-CinderError3","Node3-Config-Loss-Cinder-CinderError3", false,tags , null, null, index, null));
    }

    private void createNode3ConfigLossKeystone() {
        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node3-Config-Loss-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Keystone","Keystone Failure","Category for Keystone Config-Loss of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigLoss -> Keystone -> keystone
        //--keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Keystone"));
        tags.addAll(Arrays.asList("Keystone"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Keystone-KeystoneService","Node3-Config-Loss-Keystone-KeystoneService","Node3-Config-Loss-Keystone-KeystoneService", false,tags , null, null, index, null));
    }

    private void createNode3ConfigLossNeutron() {
        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss"));
        tags.addAll(Arrays.asList("Neutron","Network"));
        children.addAll(Arrays.asList("Node3-Config-Loss-Neutron-NeutronServer","Node3-Config-Loss-Neutron-L3Agent","Node3-Config-Loss-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Neutron","Neutron Failure","Category for Neutron Config-Loss of Node3.", true,tags , null, null, index, children));

        //Node3 -> Software -> ConfigLoss -> Neutron -> neutronServer& L3Agent&L2Agent
        //--1-- neutronServer
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Neutron"));
        tags.addAll(Arrays.asList("NeutronServer"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Neutron-NeutronServer","Node3-Config-Loss-Neutron-NeutronServer","Node3-Config-Loss-Neutron-NeutronServer", false,tags , null, null, index, null));

        //--2-- L3Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Neutron"));
        tags.addAll(Arrays.asList("L3Agent"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Neutron-L3Agent","Node3-Config-Loss-Neutron-L3Agent","Node3-Config-Loss-Neutron-L3Agent", false,tags , null, null, index, null));

        //--3-- L2Agent
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Neutron"));
        tags.addAll(Arrays.asList("L2Agent"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Neutron-L2Agent","Node3-Config-Loss-Neutron-L2Agent","Node3-Config-Loss-Neutron-L2Agent", false,tags , null, null, index, null));
    }

    private void createNode3ConfigLossNova() {
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node3-Config-Loss-Nova-NovaApi","Node3-Config-Loss-Nova-NovaCompute","Node3-Config-Loss-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Nova","Nova Failure","Category for Nova Config-Loss of Node3.", true,tags , null, null, index, children));


        //Node3 -> Software -> ConfigLoss -> Nova -> novaApi&novaCompute&novaScheduler
        //--1-- novaApi
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Nova"));
        tags.addAll(Arrays.asList("NovaApi"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Nova-NovaApi","Node3-Config-Loss-Nova-NovaApi","Node3-Config-Loss-Nova-NovaApi", false,tags , null, null, index, null));

        //--2-- novaCompute
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Nova"));
        tags.addAll(Arrays.asList("NovaCompute"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Nova-NovaCompute","Node3-Config-Loss-Nova-NovaCompute","Node3-Config-Loss-Nova-NovaCompute", false,tags , null, null, index, null));

        //--3-- novaScheduler
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node3-Failure","Node3-Software-Failure","Node3-Config-Loss","Node3-Config-Loss-Nova"));
        tags.addAll(Arrays.asList("NovaScheduler"));
        failureRepository.save(new CloudFailure("Node3-Config-Loss-Nova-NovaScheduler","Node3-Config-Loss-Nova-NovaScheduler","Node3-Config-Loss-Nova-NovaScheduler", false,tags , null, null, index, null));
    }



}
