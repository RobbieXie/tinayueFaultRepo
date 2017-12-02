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
import java.util.Arrays;
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
        ArrayList<String> tags = new ArrayList<>();
        children.addAll(Arrays.asList("Node1-Failure","Node2-Failure","Node3-Failure"));
        tags.add("Cloud");
        failureRepository.save(new CloudFailure("Cloud-Failure","Cloud Failure","Category for basic failures of a cloud infrastructure.", true,tags , null, null, null, children));

        //Node1
        children.clear();tags.clear();
        index.add("Cloud-Failure");
        tags.addAll(Arrays.asList("Node1"));
        children.addAll(Arrays.asList("Node1-Hardware-Failure","Node1-Network-Failure","Node1-Software-Failure"));
        failureRepository.save(new CloudFailure("Node1-Failure","Node1 Failure","Category for basic failures of Node1.", true,tags , null, null, index, children));

        //Node2
        children.clear();tags.clear();
        tags.addAll(Arrays.asList("Node2"));
//        children.addAll(Arrays.asList("Node2-Hardware-Failure","Node2-Network-Failure","Node2-Software-Failure"));
        failureRepository.save(new CloudFailure("Node2-Failure","Node2 Failure","Category for basic failures of Node2.", true,tags , null, null, index, children));

        //Node3
        children.clear();tags.clear();
        tags.addAll(Arrays.asList("Node3"));
//        children.addAll(Arrays.asList("Node3-Hardware-Failure","Node3-Network-Failure","Node3-Software-Failure"));
        failureRepository.save(new CloudFailure("Node3-Failure","Node3 Failure","Category for basic failures of Node3.", true,tags , null, null, index, children));

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

        //Node1 -> Software
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure"));
        tags.addAll(Arrays.asList("Software"));
        children.addAll(Arrays.asList("Node1-Process-Crash","Node1-Config-Loss","Node1-Config-Wrong"));
        failureRepository.save(new CloudFailure("Node1-Software-Failure","Software Failure","Category for Software failures of Node1.", true,tags , null, null, index, children));


        //Node1 -> Software -> Process & Config  Loss&Wrong
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure"));
        tags.addAll(Arrays.asList("ProcessCrash"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Nova","Node1-Process-Crash-Neutron","Node1-Process-Crash-Keystone",
                "Node1-Process-Crash-Cinder","Node1-Process-Crash-Glance","Node1-Process-Crash-Swift","Node1-Process-Crash-Ironic"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash","Process Crash","Category for Process-Crash of Node1.", true,tags , null, null, index, children));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure"));
        tags.addAll(Arrays.asList("ConfigLoss"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Nova","Node1-Process-Crash-Neutron","Node1-Process-Crash-Keystone",
                "Node1-Process-Crash-Cinder","Node1-Process-Crash-Glance","Node1-Process-Crash-Swift","Node1-Process-Crash-Ironic"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss","Config Loss","Category for ConfigLoss of Node1.", true,tags , null, null, index, children));

        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure"));
        tags.addAll(Arrays.asList("ConfigWrong"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Nova","Node1-Process-Crash-Neutron","Node1-Process-Crash-Keystone",
                "Node1-Process-Crash-Cinder","Node1-Process-Crash-Glance","Node1-Process-Crash-Swift","Node1-Process-Crash-Ironic"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong","Config Wrong","Category for ConfigWrong of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ProcessCrash -> Nova&Neutron&Keystone
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Nova-NovaApi","Node1-Process-Crash-Nova-NovaCompute","Node1-Process-Crash-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Nova","Nova Failure","Category for Nova Process-Crash of Node1.", true,tags , null, null, index, children));

        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Neutron"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Neutron-NeutronServer","Node1-Process-Crash-Neutron-L3Agent","Node1-Process-Crash-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Neutron","Neutron Failure","Category for Neutron Process-Crash of Node1.", true,tags , null, null, index, children));

        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Keystone","Keystone Failure","Category for Keystone Process-Crash of Node1.", true,tags , null, null, index, children));

        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Cinder-CinderError1","Node1-Process-Crash-Cinder-CinderError2","Node1-Process-Crash-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Cinder","Cinder Failure","Category for Cinder Process-Crash of Node1.", true,tags , null, null, index, children));

        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Glance-GlanceError1","Node1-Process-Crash-Glance-GlanceError2","Node1-Process-Crash-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Glance","Glance Failure","Category for Glance Process-Crash of Node1.", true,tags , null, null, index, children));

        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Swift-SwiftError1","Node1-Process-Crash-Swift-SwiftError2","Node1-Process-Crash-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Swift","Swift Failure","Category for Swift Process-Crash of Node1.", true,tags , null, null, index, children));

        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node1-Process-Crash-Ironic-IronicError1","Node1-Process-Crash-Ironic-IronicError2","Node1-Process-Crash-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node1-Process-Crash-Ironic","Ironic Failure","Category for Ironic Process-Crash of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigLoss -> Nova&Neutron&Keystone
        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Nova-NovaApi","Node1-Config-Loss-Nova-NovaCompute","Node1-Config-Loss-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Nova","Nova Failure","Category for Nova Config-Loss of Node1.", true,tags , null, null, index, children));

        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Neutron"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Neutron-NeutronServer","Node1-Config-Loss-Neutron-L3Agent","Node1-Config-Loss-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Neutron","Neutron Failure","Category for Neutron Config-Loss of Node1.", true,tags , null, null, index, children));

        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Keystone","Keystone Failure","Category for Keystone Config-Loss of Node1.", true,tags , null, null, index, children));

        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Cinder-CinderError1","Node1-Config-Loss-Cinder-CinderError2","Node1-Config-Loss-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Cinder","Cinder Failure","Category for Cinder Config-Loss of Node1.", true,tags , null, null, index, children));

        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Glance-GlanceError1","Node1-Config-Loss-Glance-GlanceError2","Node1-Config-Loss-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Glance","Glance Failure","Category for Glance Config-Loss of Node1.", true,tags , null, null, index, children));

        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Swift-SwiftError1","Node1-Config-Loss-Swift-SwiftError2","Node1-Config-Loss-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Swift","Swift Failure","Category for Swift Config-Loss of Node1.", true,tags , null, null, index, children));

        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node1-Config-Loss-Ironic-IronicError1","Node1-Config-Loss-Ironic-IronicError2","Node1-Config-Loss-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Loss-Ironic","Ironic Failure","Category for Ironic Config-Loss of Node1.", true,tags , null, null, index, children));

        //Node1 -> Software -> ConfigWrong -> Nova&Neutron&Keystone

        //--1-- nova
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Nova"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Nova-NovaApi","Node1-Config-Wrong-Nova-NovaCompute","Node1-Config-Wrong-Nova-NovaScheduler"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Nova","Nova Failure","Category for Nova Config-Wrong of Node1.", true,tags , null, null, index, children));

        //--2-- neutron
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Neutron"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Neutron-NeutronServer","Node1-Config-Wrong-Neutron-L3Agent","Node1-Config-Wrong-Neutron-L2Agent"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Neutron","Neutron Failure","Category for Neutron Config-Wrong of Node1.", true,tags , null, null, index, children));

        //--3-- keystone
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Keystone"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Keystone-KeystoneService"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Keystone","Keystone Failure","Category for Keystone Config-Wrong of Node1.", true,tags , null, null, index, children));

        //--4--Cinder
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Cinder"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Cinder-CinderError1","Node1-Config-Wrong-Cinder-CinderError2","Node1-Config-Wrong-Cinder-CinderError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Cinder","Cinder Failure","Category for Cinder Config-Wrong of Node1.", true,tags , null, null, index, children));

        //--5--Glance
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Glance"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Glance-GlanceError1","Node1-Config-Wrong-Glance-GlanceError2","Node1-Config-Wrong-Glance-GlanceError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Glance","Glance Failure","Category for Glance Config-Wrong of Node1.", true,tags , null, null, index, children));

        //--6--Swift
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Swift"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Swift-SwiftError1","Node1-Config-Wrong-Swift-SwiftError2","Node1-Config-Wrong-Swift-SwiftError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Swift","Swift Failure","Category for Swift Config-Wrong of Node1.", true,tags , null, null, index, children));

        //--7--Ironic
        index.clear();children.clear();tags.clear();
        index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong"));
        tags.addAll(Arrays.asList("Ironic"));
        children.addAll(Arrays.asList("Node1-Config-Wrong-Ironic-IronicError1","Node1-Config-Wrong-Ironic-IronicError2","Node1-Config-Wrong-Ironic-IronicError3"));
        failureRepository.save(new CloudFailure("Node1-Config-Wrong-Ironic","Ironic Failure","Category for Ironic Config-Wrong of Node1.", true,tags , null, null, index, children));

        //------------------------------------

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
        //Node1 -> Software -> ProcessCrash -> Keystone -> keystone
            //--keystone
            index.clear();children.clear();tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Process-Crash","Node1-Process-Crash-Keystone"));
            tags.addAll(Arrays.asList("Keystone"));
            failureRepository.save(new CloudFailure("Node1-Process-Crash-Keystone-KeystoneService","Node1-Process-Crash-Keystone-KeystoneService","Node1-Process-Crash-Keystone-KeystoneService", false,tags , null, null, index, null));

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
        //Node1 -> Software -> ConfigLoss -> Keystone -> keystone
            //--keystone
            index.clear();children.clear();tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Loss","Node1-Config-Loss-Keystone"));
            tags.addAll(Arrays.asList("Keystone"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Keystone-KeystoneService","Node1-Config-Loss-Keystone-KeystoneService","Node1-Config-Loss-Keystone-KeystoneService", false,tags , null, null, index, null));

        //Node1 -> Software -> ConfigLoss -> Cinder -> CinderError1&CinderError2&CinderError3
            //--1-- CinderError1
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Cinder"));
            tags.addAll(Arrays.asList("CinderError1"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Cinder-CinderError1", "Node1-Config-Loss-Cinder-CinderError1", "Node1-Config-Loss-Cinder-CinderError1", false, tags, null, null, index, null));

            //--2-- CinderError2
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Cinder"));
            tags.addAll(Arrays.asList("CinderError2"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Cinder-CinderError2", "Node1-Config-Loss-Cinder-CinderError2", "Node1-Config-Loss-Cinder-CinderError2", false, tags, null, null, index, null));

            //--3-- CinderError3
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Cinder"));
            tags.addAll(Arrays.asList("CinderError3"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Cinder-CinderError3", "Node1-Config-Loss-Cinder-CinderError3", "Node1-Config-Loss-Cinder-CinderError3", false, tags, null, null, index, null));

            //Node1 -> Software -> ConfigLoss -> Glance -> GlanceError1&GlanceError2&GlanceError3
            //--1-- GlanceError1
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Glance"));
            tags.addAll(Arrays.asList("GlanceError1"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Glance-GlanceError1", "Node1-Config-Loss-Glance-GlanceError1", "Node1-Config-Loss-Glance-GlanceError1", false, tags, null, null, index, null));

            //--2-- GlanceError2
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Glance"));
            tags.addAll(Arrays.asList("GlanceError2"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Glance-GlanceError2", "Node1-Config-Loss-Glance-GlanceError2", "Node1-Config-Loss-Glance-GlanceError2", false, tags, null, null, index, null));

            //--3-- GlanceError3
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Glance"));
            tags.addAll(Arrays.asList("GlanceError3"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Glance-GlanceError3", "Node1-Config-Loss-Glance-GlanceError3", "Node1-Config-Loss-Glance-GlanceError3", false, tags, null, null, index, null));

            //Node1 -> Software -> ConfigLoss -> Swift -> SwiftError1&SwiftError2&SwiftError3
            //--1-- SwiftError1
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Swift"));
            tags.addAll(Arrays.asList("SwiftError1"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Swift-SwiftError1", "Node1-Config-Loss-Swift-SwiftError1", "Node1-Config-Loss-Swift-SwiftError1", false, tags, null, null, index, null));

            //--2-- SwiftError2
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Swift"));
            tags.addAll(Arrays.asList("SwiftError2"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Swift-SwiftError2", "Node1-Config-Loss-Swift-SwiftError2", "Node1-Config-Loss-Swift-SwiftError2", false, tags, null, null, index, null));

            //--3-- SwiftError3
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Swift"));
            tags.addAll(Arrays.asList("SwiftError3"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Swift-SwiftError3", "Node1-Config-Loss-Swift-SwiftError3", "Node1-Config-Loss-Swift-SwiftError3", false, tags, null, null, index, null));

            //Node1 -> Software -> ConfigLoss -> Ironic -> IronicError1&IronicError2&IronicError3
            //--1-- IronicError1
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Ironic"));
            tags.addAll(Arrays.asList("IronicError1"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Ironic-IronicError1", "Node1-Config-Loss-Ironic-IronicError1", "Node1-Config-Loss-Ironic-IronicError1", false, tags, null, null, index, null));

            //--2-- IronicError2
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Ironic"));
            tags.addAll(Arrays.asList("IronicError2"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Ironic-IronicError2", "Node1-Config-Loss-Ironic-IronicError2", "Node1-Config-Loss-Ironic-IronicError2", false, tags, null, null, index, null));

            //--3-- IronicError3
            index.clear();
            children.clear();
            tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure", "Node1-Failure", "Node1-Software-Failure", "Node1-Config-Loss", "Node1-Config-Loss-Ironic"));
            tags.addAll(Arrays.asList("IronicError3"));
            failureRepository.save(new CloudFailure("Node1-Config-Loss-Ironic-IronicError3", "Node1-Config-Loss-Ironic-IronicError3", "Node1-Config-Loss-Ironic-IronicError3", false, tags, null, null, index, null));

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
        //Node1 -> Software -> ConfigWrong -> Keystone -> keystone
            //--keystone
            index.clear();children.clear();tags.clear();
            index.addAll(Arrays.asList("Cloud-Failure","Node1-Failure","Node1-Software-Failure","Node1-Config-Wrong","Node1-Config-Wrong-Keystone"));
            tags.addAll(Arrays.asList("Keystone"));
            failureRepository.save(new CloudFailure("Node1-Config-Wrong-Keystone-KeystoneService","Node1-Config-Wrong-Keystone-KeystoneService","Node1-Config-Wrong-Keystone-KeystoneService", false,tags , null, null, index, null));

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

        return faultTreeService.ShowFaultTreeStructure();
    }

    public void createFaultTree(){
        createFaultTreeNode1();
        createFaultTreeNode2();
        createFaultTreeNode3();
    }

    public void createFaultTreeNode1(){

    }

    public void createFaultTreeNode2(){

    }

    public void createFaultTreeNode3(){

    }
    public void CreateFaultTreeNode1Hardware(){

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
