package com.tiandi.service.geneticalgorithm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author 谢天帝
 * @version v0.1 2017/12/2.
 */
@Service
public class Individual {

    private int defaultLength = 11;

    private String gene;

    private int fitness = 0;

    private List<String> availableCodeList = new ArrayList<>();

    @Autowired
    private FaultTreeGA faultTreeGA;


    public Individual() {
        Map<String,String> codeMap = faultTreeGA.generateFaultCode();
        for(String name : codeMap.keySet()){
            availableCodeList.add(codeMap.get(name));
        }
    }

    public void generateIndividul(){
        int random = new Random().nextInt(availableCodeList.size());
        this.gene = this.availableCodeList.get(random);
    }

    public int getFitness(){
        return new FitnessCalc(Arrays.asList("Network")).getFitness(this);
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }
}
