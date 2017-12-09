package com.tiandi.service.geneticalgorithm;

import com.tiandi.mongo.CloudFailure;
import com.tiandi.mongo.CloudFailureRepository;
import org.bson.ByteBuf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author 谢天帝
 * @version v0.1 2017/12/2.
 */
@Service
public class FaultTreeGA {

    //一共有几层
    Integer layerSize = 0;

    //编码总长度
    Integer  totalCodeLength = 0;

    //存储每层编码长度为几位
    List<Integer> layerLengthList = new ArrayList<>();

    //存储每层，每个故障的对应编码
    Map<Integer,Map<String,String>> categoryLayerCodeMap = new LinkedHashMap<>();

    List<String> specialList = new ArrayList<>();
    int maxLeafNodes = 0;

    Map<String,String> leafCodeMap = new LinkedHashMap<>();

    List<String> tagsList = new ArrayList<>();

    @Autowired
    private CloudFailureRepository failureRepository;

    public Map<String,String> generateFaultCode(){
        if(this.leafCodeMap.size()>0){
            return leafCodeMap;
        }
        layerSize =0 ;
        totalCodeLength=0;
        layerLengthList = new ArrayList<>();
        categoryLayerCodeMap = new LinkedHashMap<>();
        specialList = new ArrayList<>();
        maxLeafNodes = 0 ;
        leafCodeMap = new LinkedHashMap<>();
        tagsList = new ArrayList<>();

        List<CloudFailure> cfs = failureRepository.findByIndexSize(1);
        // 逐层遍历数据库
        for(int i = 1; cfs!=null&&cfs.size()!=0;i++){
            List<CloudFailure> leafCfList = new ArrayList<>();
            List<CloudFailure> categoryCfList = new ArrayList<>();
            Map<String,Integer> nameToCodeMap = new LinkedHashMap<>();
            // 分开叶子结点和目录节点到两个数组
            for(CloudFailure cf : cfs){
                if(cf.isCategory) {
                    categoryCfList.add(cf);

                    //记录tags
                    List<String> tags = cf.getTags();
                    for(String tag : tags){
                        if(!tagsList.contains(tag))
                            tagsList.add(tag);
                    }
                }
                else {
                    leafCfList.add(cf);
                }
            }

            //通过map去除相同name的个数
            for(CloudFailure cf : categoryCfList){
                nameToCodeMap.put(cf.getName(),1);
            }

            //计算 存储name所需要的位数
            Set<String> restCfs = nameToCodeMap.keySet();
            int length = restCfs.size();
            int bitLength = Integer.toBinaryString(length).length();
            layerLengthList.add(bitLength);

            //为每一个name进行编码
            Map<String,String> resultMap = new LinkedHashMap<>();
            int cnt =1;
            for(String failureName : restCfs){
                resultMap.put(failureName,binary2decimal(cnt++,bitLength));
            }

            //将编码后的map存入当前层，存入大map中
            if(resultMap.size()>0)
                categoryLayerCodeMap.put(i,resultMap);
            layerSize = i+1;
            cfs = failureRepository.findByIndexSize(i+1);
        }
        //-----------------------------------------------

        List<CloudFailure> allLeafFailures = failureRepository.findByIsCategory(false);
        //算叶子节点
        maxLeafNodes = getMaxLeafNodesNum();
        layerLengthList.remove(layerLengthList.size()-1);
        layerLengthList.add(Integer.toBinaryString(maxLeafNodes).length());
        // 算总长度几位
        for(int i=0; i<layerLengthList.size();i++){
            totalCodeLength+=layerLengthList.get(i);
        }
        //
        for(CloudFailure cf : allLeafFailures){
            String code = "";
            List<String> index = cf.getIndex();
            for(int i=1;i<index.size();i++){
                CloudFailure parent = failureRepository.findById(index.get(i));
                String parentCode = categoryLayerCodeMap.get(i).get(parent.getName());
                code += parentCode;
            }
            if(index.size()<layerSize-1) {
                if(!specialList.contains(code))
                    specialList.add(code);
            }
            while(code.length()<totalCodeLength-layerLengthList.get(layerLengthList.size()-1)){
                code += "0";
            }
            int sequence = failureRepository.findById(index.get(index.size()-1)).getChildren().indexOf(cf.getId())+1;
            code += binary2decimal(sequence, Integer.toBinaryString(maxLeafNodes).length());

            leafCodeMap.put(cf.getId(),code);

        }

        System.out.println("finish");
        return leafCodeMap;
    }

    public int getMaxLeafNodesNum(){
        int totalMax =0;
        List<CloudFailure> allLeafFailures = failureRepository.findByIsCategory(false);
        for(CloudFailure cf : allLeafFailures){
            int max = 0;
            List<String> index = cf.getIndex();
            List<String> brothers = failureRepository.findById(index.get(index.size()-1)).getChildren();
            for(String brother : brothers){
                if(!failureRepository.findById(brother).isCategory) max++;
            }
            if(totalMax<max) totalMax = max;
        }
        return totalMax;
    }

    public String binary2decimal(int decNum , int digit) {
        String binStr = "";
        for(int i= digit-1;i>=0;i--) {
            binStr +=(decNum>>i)&1;
        }
        return binStr;
    }

    public FaultTreeGA() {
    }

    public Integer getLayerSize() {
        return layerSize;
    }

    public void setLayerSize(Integer layerSize) {
        this.layerSize = layerSize;
    }

    public Integer getTotalCodeLength() {
        return totalCodeLength;
    }

    public void setTotalCodeLength(Integer totalCodeLength) {
        this.totalCodeLength = totalCodeLength;
    }

    public List<Integer> getLayerLengthList() {
        return layerLengthList;
    }

    public void setLayerLengthList(List<Integer> layerLengthList) {
        this.layerLengthList = layerLengthList;
    }

    public Map<Integer, Map<String, String>> getCategoryLayerCodeMap() {
        return categoryLayerCodeMap;
    }

    public void setCategoryLayerCodeMap(Map<Integer, Map<String, String>> categoryLayerCodeMap) {
        this.categoryLayerCodeMap = categoryLayerCodeMap;
    }

    public List<String> getSpecialList() {
        return specialList;
    }

    public void setSpecialList(List<String> specialList) {
        this.specialList = specialList;
    }

    public int getMaxLeafNodes() {
        return maxLeafNodes;
    }

    public void setMaxLeafNodes(int maxLeafNodes) {
        this.maxLeafNodes = maxLeafNodes;
    }

    public Map<String, String> getLeafCodeMap() {
        return leafCodeMap;
    }

    public void setLeafCodeMap(Map<String, String> leafCodeMap) {
        this.leafCodeMap = leafCodeMap;
    }

    public CloudFailureRepository getFailureRepository() {
        return failureRepository;
    }

    public void setFailureRepository(CloudFailureRepository failureRepository) {
        this.failureRepository = failureRepository;
    }

    public void logLeafMap(){
        for(String name : leafCodeMap.keySet()){
            System.out.println(name + " : "+leafCodeMap.get(name));
        }
    }

    public List<String> getTagsList() {
        return tagsList;
    }
}
