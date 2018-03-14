package com.tiandi.service.hillclimbing;

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
public class HillingClimbing {

    @Autowired
    private CloudFailureRepository failureRepository;

    private List<CloudFailure> allLeafNodes ;

    private Map<CloudFailure,Integer> cloudFailureFitnessMap = new HashMap<>();

    public HillingClimbing() {
//        allLeafNodes = failureRepository.findByIsCategory(false);
    }

    private List<String> tags;

    public List<String> getTags() {
        return tags;
    }

    public CloudFailureRepository getFailureRepository() {
        return failureRepository;
    }

    public void setFailureRepository(CloudFailureRepository failureRepository) {
        this.failureRepository = failureRepository;
    }

    public List<CloudFailure> getAllLeafNodes() {
        return allLeafNodes;
    }

    public void setAllLeafNodes(List<CloudFailure> allLeafNodes) {
        this.allLeafNodes = allLeafNodes;
    }

    public void setTags(List<String> tags) {
        this.cloudFailureFitnessMap.clear();
        this.tags = tags;
    }

    public List<CloudFailure> getMaxFitnessList(int size){
        if(allLeafNodes==null || allLeafNodes.size()==0){
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

    public CloudFailure getMaxFitess(List<CloudFailure> allLeafNodes){
        if(allLeafNodes==null || allLeafNodes.size()<=0){
            allLeafNodes = failureRepository.findByIsCategory(false);
            Collections.shuffle(allLeafNodes);
        }
        int index = new Random().nextInt(allLeafNodes.size()-1);

        CloudFailure bestFailure = allLeafNodes.get(index);
        CloudFailure nextFailure = null;

        for(int i =index; i<allLeafNodes.size();i++){
            if(allLeafNodes.size()<=i+1){
                break;
            }
            nextFailure = allLeafNodes.get(i+1);
            if(getFitness(bestFailure,tags) < getFitness(nextFailure,tags)){
                bestFailure = nextFailure;
            }else if(getFitness(bestFailure,tags) == getFitness(nextFailure,tags)){
                bestFailure = nextFailure;
            }else{
                break;
            }
        }
        return bestFailure;
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
