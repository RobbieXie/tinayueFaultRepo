package com.tiandi.service.geneticalgorithm;

import com.tiandi.utils.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.swing.*;
import java.util.*;

/**
 * @author 谢天帝
 * @version v0.1 2017/12/2.
 */
public class Individual {

    private int defaultLength = 11;

    private String gene;

    private int fitness = 0;

    private List<String> availableCodeList = new ArrayList<>();

    private FaultTreeGA faultTreeGA;

    private FitnessCalc fitnessCalc;

    //存储每层编码长度为几位
    List<Integer> layerLengthList = new ArrayList<>();

    public Individual() {
        faultTreeGA = SpringUtil.getApplicationContext().getBean(FaultTreeGA.class);
        fitnessCalc = SpringUtil.getApplicationContext().getBean(FitnessCalc.class);
    }

    public Individual(int defaultLength, String gene, List<String> availableCodeList, List<Integer> layerLengthList) {
        faultTreeGA = SpringUtil.getApplicationContext().getBean(FaultTreeGA.class);
        fitnessCalc = SpringUtil.getApplicationContext().getBean(FitnessCalc.class);
        this.defaultLength = defaultLength;
        this.gene = gene;
        this.availableCodeList = availableCodeList;
        this.layerLengthList = layerLengthList;
    }

    public void generateIndividul(){
        Map<String,String> codeMap = faultTreeGA.generateFaultCode();
        for(String name : codeMap.keySet()){
            availableCodeList.add(codeMap.get(name));
        }

        int random = new Random().nextInt(availableCodeList.size());
        this.gene = this.availableCodeList.get(random);
        layerLengthList = faultTreeGA.getLayerLengthList();
    }

    public int getFitness(){
        if(fitnessCalc.getTags()==null || fitnessCalc.getTags().size()==0)
            fitnessCalc.setTags(Arrays.asList("Nova","ConfigWrong","Node1"));
        return fitnessCalc.getFitness(this);
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }

    public List<Integer> getLayerLengthList() {
        return layerLengthList;
    }

    public void setLayerLengthList(List<Integer> layerLengthList) {
        this.layerLengthList = layerLengthList;
    }

    public int getDefaultLength() {
        return defaultLength;
    }

    public void setDefaultLength(int defaultLength) {
        this.defaultLength = defaultLength;
    }

    public void setFitness(int fitness) {
        this.fitness = fitness;
    }

    public List<String> getAvailableCodeList() {
        return availableCodeList;
    }

    public void setAvailableCodeList(List<String> availableCodeList) {
        this.availableCodeList = availableCodeList;
    }
}
