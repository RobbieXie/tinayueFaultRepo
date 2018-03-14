package com.tiandi.service.geneticalgorithm;

import com.tiandi.utils.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * @author 谢天帝
 * @version v0.1 2017/12/3.
 */
public class Population {

    //默认20
    private int size = 50;

    private Boolean elitsm = true;

    private List<Individual> individuals = new ArrayList<>();

    int tournamentSize = 3;

    Double crossoverRate = 0.5;

    Double mutateRate = 0.015;

    List<String> specialList = new ArrayList<>();

    public Population() {
        FaultTreeGA faultTreeGA = SpringUtil.getApplicationContext().getBean(FaultTreeGA.class);
        if(faultTreeGA.getSpecialList().size()==0){
            faultTreeGA.generateFaultCode();
        }
        this.specialList = faultTreeGA.getSpecialList();
    }


    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void generatePopulation(){
//        long start = System.currentTimeMillis();
        for(int i=0;i<size;i++){
            Individual individual = new Individual();
            individual.generateIndividul();
            individuals.add(individual);
        }
//        System.out.println("创建population耗时："+ (System.currentTimeMillis() - start)/1000 +" s");
    }

    public List<Individual> getIndividuals() {
        return individuals;
    }

    public void setIndividuals(List<Individual> individuals) {
        this.individuals = individuals;
    }

    public Population generateNextPopulation(){
//        long start = System.currentTimeMillis();
        Population population = new Population();
        List<Individual> nextPopulationIndividuals = new ArrayList<>();
        List<String> nextPopulationCodes = new ArrayList<>();
        while(nextPopulationIndividuals.size()<this.size){
            Individual i1 = tournamentSelection(this,this.tournamentSize);
            Individual i2 = tournamentSelection(this,this.tournamentSize);
            Individual son = this.crossover(i1,i2);
            son = this.mutate(son);
            if(!nextPopulationCodes.contains(son.getGene())){
                nextPopulationCodes.add(son.getGene());
                nextPopulationIndividuals.add(son);
            }
        }
        population.setSize(this.size);
        population.setIndividuals(nextPopulationIndividuals);

//        System.out.println("创建下一代population耗时："+ (System.currentTimeMillis() - start) +" ms");
        //如果保留精英
        if(elitsm){
            Individual old_fittest = this.getFittest();
            Individual new_fittest = population.getFittest();
            List<Individual> replaceList = new ArrayList<>();

            while (old_fittest.getFitness()>new_fittest.getFitness()){
                replaceList.add(old_fittest);
                this.individuals.remove(old_fittest);
                old_fittest = this.getFittest();
            }
            for(int i=0;i<replaceList.size();i++){
                population.getIndividuals().remove(i);
            }
            for(int i=0;i<replaceList.size();i++){
                population.getIndividuals().add(i,replaceList.get(i));
            }
        }

        return population;
    }

    public Individual tournamentSelection(Population population, int tournamentSize){
        long start = System.currentTimeMillis();
        //新建新种群
        Population newPopu = new Population();
        newPopu.setSize(tournamentSize);
        List<Individual> tournamentIndividuals = new ArrayList<>();
        //随机选size大小个个体
        for(int i=0;i<tournamentSize;i++){
            int random = new Random().nextInt(population.getSize());
            tournamentIndividuals.add(population.getIndividuals().get(random));
        }
        //为新种群赋值
        newPopu.setIndividuals(tournamentIndividuals);

        //返回新种群中最好的
        Individual individual = newPopu.getFittest();
        return individual;
    }

    public Individual getFittest(){
        Individual fittest = this.getIndividuals().get(0);
        for(Individual individual : this.getIndividuals()){
            if(fittest.getFitness()<individual.getFitness()){
                fittest = individual;
            }
        }
        return fittest;
    }

    public Individual crossover(Individual i1, Individual i2){
        List<Integer> layerLengthList = i1.getLayerLengthList();
        String newGene = "";
        Boolean isSpecial = false;

        int start =0;
        int length = 0;
        for(int j=0;j<layerLengthList.size();j++){
            length = layerLengthList.get(j);
            String subString = "";
            //如果是特殊情况，而且不是最后一位，直接补零
            if(isSpecial && j!=(layerLengthList.size()-1)){
                subString = binary2decimal(0,length);
            }else{
                if(new Random().nextDouble()<this.crossoverRate){
                    subString = i1.getGene().substring(start,start+length);
                }else{
                    subString = i2.getGene().substring(start,start+length);
                }
            }

            start += length;
            newGene += subString;

            if(this.specialList.contains(newGene)){
                isSpecial = true;
            }
        }

        if(i1.getAvailableCodeList().contains(newGene)) {
            return new Individual(i1.getDefaultLength(),newGene,i1.getAvailableCodeList(),i1.getLayerLengthList());
        }else{
            return this.crossover(i1,i2);
        }
    }


    public Individual mutate(Individual individual){
        String gene = individual.getGene();
        String newGene = "";
        for(int i=0;i<gene.length();i++){
            if(new Random().nextDouble()<mutateRate){
                if(new Random().nextDouble()<0.5){
                    newGene += "0";
                }else {
                    newGene += "1";
                }
            }else{
                newGene += gene.substring(i,i+1);
            }
        }
        if(individual.getAvailableCodeList().contains(newGene)) {
            individual.setGene(newGene);
            return individual;
        }else{
            return this.mutate(individual);
        }
    }

    public void log(){
        int sum = 0;
        for(int i=0;i<size;i++){
            Individual individual = individuals.get(i);
            sum += individual.getFitness();
            System.out.println("当前编号:"+individual.getGene()+"  适应度： "+ individual.getFitness() );
        }
        System.out.println(String.format("总适应度：%d, 平均适应度：%.3f ",sum,((float)sum/size)));
    }

    public String binary2decimal(int decNum , int digit) {
        String binStr = "";
        for(int i= digit-1;i>=0;i--) {
            binStr +=(decNum>>i)&1;
        }
        return binStr;
    }

    public Boolean getElitsm() {
        return elitsm;
    }

    public void setElitsm(Boolean elitsm) {
        this.elitsm = elitsm;
    }

    public int getTournamentSize() {
        return tournamentSize;
    }

    public void setTournamentSize(int tournamentSize) {
        this.tournamentSize = tournamentSize;
    }

    public Double getCrossoverRate() {
        return crossoverRate;
    }

    public void setCrossoverRate(Double crossoverRate) {
        this.crossoverRate = crossoverRate;
    }

    public Double getMutateRate() {
        return mutateRate;
    }

    public void setMutateRate(Double mutateRate) {
        this.mutateRate = mutateRate;
    }
}
