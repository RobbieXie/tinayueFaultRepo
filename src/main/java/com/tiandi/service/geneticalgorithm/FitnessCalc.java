package com.tiandi.service.geneticalgorithm;

import com.tiandi.mongo.CloudFailure;
import com.tiandi.mongo.CloudFailureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 谢天帝
 * @version v0.1 2017/12/2.
 */
@Service
public class FitnessCalc {

    private List<String> tags = new ArrayList<>();

    private Map<Integer,List<String>> compareLayerAndCodeMap = new LinkedHashMap<>();

    private List<Integer> layerLengthList = new ArrayList<>();

    @Autowired
    private FaultTreeGA faultTreeGA;

    @Autowired
    private CloudFailureRepository failureRepository;

    public FitnessCalc(){

    }

    public void setTags(List<String> tags) {
        this.tags = tags;
        layerLengthList = faultTreeGA.getLayerLengthList();

        for(int i=0;i<tags.size();i++){
            String tag = tags.get(i);
            List<CloudFailure> cfs = failureRepository.findByIndexSize(1);
            // 逐层遍历数据库
            for(int j = 1; cfs!=null&&cfs.size()!=0;j++){
                List<String> compareCodes = new ArrayList<>();
                for(CloudFailure cf : cfs){
                    if(cf.isCategory && cf.getTags().indexOf(tag)!=-1){
                        if(!compareCodes.contains(faultTreeGA.getCategoryLayerCodeMap().get(j).get(cf.getName())))
                            compareCodes.add(faultTreeGA.getCategoryLayerCodeMap().get(j).get(cf.getName()));
                    }
                }
                if(compareCodes.size()>0)
                    compareLayerAndCodeMap.put(j,compareCodes);
                cfs = failureRepository.findByIndexSize(j+1);
            }
        }
    }

    public int getFitness(Individual individual){
        int start = 0;
        int length = 0;
        int fitness = 0;
        for(int i=0;i<layerLengthList.size();i++){
            length = layerLengthList.get(i);
            if(compareLayerAndCodeMap.keySet().contains(i+1)){
                String sub_gene = individual.getGene().substring(start,start+length);
                if(compareLayerAndCodeMap.get(i+1).contains(sub_gene))
                    fitness++;
            }
            start+=length;
        }
        return fitness;
    }


}
