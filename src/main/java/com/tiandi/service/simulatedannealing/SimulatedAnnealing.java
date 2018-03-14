package com.tiandi.service.simulatedannealing;

import com.tiandi.mongo.CloudFailure;
import com.tiandi.mongo.CloudFailureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author 谢天帝
 * @version v0.1 2018/1/22.
 */
@Service
public class SimulatedAnnealing {

    public static final int T = 100;// 初始化温度
    public static final double Tmin = 1e-4;// 温度的下界
    public static final int k = 10;// 每次迭代随机选取用例个数
    public static final double delta = 0.97  ;// 温度的下降率

    @Autowired
    private CloudFailureRepository failureRepository;

    private List<CloudFailure> allLeafNodes ;

    private List<String> tags;

    private Map<CloudFailure,Integer> cloudFailureFitnessMap = new HashMap<>();

    public SimulatedAnnealing() {
    }

    public List<CloudFailure> getAllLeafNodes() {
        return allLeafNodes;
    }

    public void setAllLeafNodes(List<CloudFailure> allLeafNodes) {
        this.allLeafNodes = allLeafNodes;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.cloudFailureFitnessMap.clear();
        this.tags = tags;
    }

    public CloudFailure getMaxFitess(){
        return this.getMaxFitess(this.allLeafNodes);
    }

    public CloudFailure getMaxFitess(List<CloudFailure> allLeafNodes){
        if(allLeafNodes==null || allLeafNodes.size()<=0){
            allLeafNodes = failureRepository.findByIsCategory(false);
            Collections.shuffle(allLeafNodes);
        }
        double t = T;
        CloudFailure result;
        int[] indexArr = new int[k];
        int[] maxFitArr = new int[k];
        CloudFailure[] maxFailureArr = new CloudFailure[k];
        for(int i=0;i<k;i++){
            indexArr[i] = new Random().nextInt(allLeafNodes.size()-1);
            maxFitArr[i] = 0;
            maxFailureArr[i] = null;
        }

        // 迭代的过程
        while (t > Tmin) {
            for(int i =0; i<k; i++){
                CloudFailure oldFailure = allLeafNodes.get(indexArr[i]);
                int fitness_old = getFitness(oldFailure,tags);
                if(maxFitArr[i]<=fitness_old){
                    maxFitArr[i] = fitness_old;
                    maxFailureArr[i] = oldFailure;
                }
                int interval = new Random().nextInt(4)-2;
                if(indexArr[i]+interval >=0 && indexArr[i]+interval<allLeafNodes.size()){
                    CloudFailure newFailure = allLeafNodes.get(indexArr[i]+interval);
                    int fitness_new = getFitness(newFailure,tags);
                    if(fitness_new>fitness_old){
                        indexArr[i] = indexArr[i]+interval;
                        if(maxFitArr[i]<fitness_new){
                            maxFitArr[i] = fitness_new;
                            maxFailureArr[i] = newFailure;
                        }
                    }else {
                        // 以概率替换
                        double p = 1 / (1 + Math.exp(-(fitness_new - fitness_old)*10 / t));
                        if (Math.random() < p) {
                            indexArr[i] =  indexArr[i]+interval;
                        }
                    }
                }
            }
            t = t*delta;
        }
//        result = maxFailureArr[0];
//        for(int i=1;i<k;i++){
//            if(getFitness(result,tags)<getFitness(maxFailureArr[i],tags)) {
//                result = maxFailureArr[i];
//            }
//        }
        result = allLeafNodes.get(indexArr[0]);
        for(int i=1;i<k;i++){
            if(getFitness(result,tags)<getFitness(allLeafNodes.get(i),tags)) {
                result = allLeafNodes.get(i);
            }
        }
        return result;
    }

    public List<CloudFailure> getMaxFitessList(int size){
        if(allLeafNodes==null || allLeafNodes.size()<=0){
            allLeafNodes = failureRepository.findByIsCategory(false);
            Collections.shuffle(allLeafNodes);
        }
        List<CloudFailure> tmpAllLeafNodes = new ArrayList<>(allLeafNodes);
        List<CloudFailure> resultList = new ArrayList<>();

        while (resultList.size()<size){
            CloudFailure bestFailure = this.getMaxFitess(tmpAllLeafNodes);
            if(!resultList.contains(bestFailure)) {
                resultList.add(bestFailure);
                tmpAllLeafNodes.remove(bestFailure);
            }
        }
        return resultList;
    }

    public int getFitness(CloudFailure cf, List<String> tags){
        if(cloudFailureFitnessMap.get(cf)!=null){
            return cloudFailureFitnessMap.get(cf);
        }
        CloudFailure originCf = cf;
        List<String> allTags = new ArrayList<>();
        List<String> cfTags = cf.getTags();
        if(cfTags!=null)
            allTags.addAll(cfTags);
        while(cf.getIndex()!= null && cf.getIndex().size()>0){
            CloudFailure parentCf = failureRepository.findById(cf.getIndex().get(cf.getIndex().size()-1));
            if(parentCf.getIndex() != null)
                allTags.addAll(parentCf.getTags());
            cf = parentCf;
        }
        int fitness = 0;
        for(String tag : allTags){
            if(tags.contains(tag)){
                fitness++;
            }
        }
        cloudFailureFitnessMap.put(originCf,fitness);
        return fitness;
    }

    public Map<CloudFailure, Integer> getCloudFailureFitnessMap() {
        return cloudFailureFitnessMap;
    }

    public void setCloudFailureFitnessMap(Map<CloudFailure, Integer> cloudFailureFitnessMap) {
        this.cloudFailureFitnessMap = cloudFailureFitnessMap;
    }
}
