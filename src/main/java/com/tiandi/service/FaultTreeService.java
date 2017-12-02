package com.tiandi.service;

import com.tiandi.mongo.CloudFailure;
import com.tiandi.mongo.CloudFailureRepository;
import com.tiandi.mongo.faulttree.FaultTreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/29.
 */
@Service
public class FaultTreeService {

    @Autowired
    private CloudFailureRepository failureRepository;

    public String ShowFaultTreeStructure(){
        List<CloudFailure> cfs = failureRepository.findByIndex(null);
        CloudFailure cf = cfs.get(0);
        if(cf==null) return "Empty!";
        FaultTreeNode rootNode = recursiveTree(cf.id,true);
        String result = printTree(rootNode);
        return result;
    }

    //isAll判定是否返回叶子节点
    public FaultTreeNode recursiveTree(String id, boolean isAllNeeded){
        CloudFailure cf = failureRepository.findById(id);
        if(!cf.isCategory && !isAllNeeded) return null;
        FaultTreeNode node = new FaultTreeNode();
        node.id = cf.id;
        node.isCategory = cf.isCategory;
        System.out.println(node.id);
        if(cf.children == null) return node;
        else{
            for(int i=0;i<cf.children.size();i++){
                FaultTreeNode childNode = recursiveTree(cf.children.get(i),isAllNeeded);
                if(childNode != null) node.children.add(childNode);
            }
        }
        return node;
    }

    public String printTree(FaultTreeNode tree) {
        String resultTree = "";
        List<FaultTreeNode> firstStack = new ArrayList<>();
        firstStack.add(tree);

        List<List<FaultTreeNode>> childListStack = new ArrayList<List<FaultTreeNode>>();
        childListStack.add(firstStack);

        while (childListStack.size() > 0) {
            List<FaultTreeNode> childStack = childListStack.get(childListStack.size() - 1);

            if (childStack.size() == 0) {
                childListStack.remove(childListStack.size() - 1);
            }
            else {
                tree = childStack.get(0);
                childStack.remove(0);

                String indent = "";
                for (int i = 0; i < childListStack.size() - 1; i++) {
                    indent += (childListStack.get(i).size() > 0) ? "|  " : "   ";
                }
                if(tree.isCategory) {
                    System.out.println(indent + "+- " + tree.id);
                    resultTree = resultTree + indent + "+- " + tree.id +"\n";
                }
                else{
                    System.out.println(indent + "+- " + tree.id + " *");
                    resultTree = resultTree + indent + "+- " + tree.id+ " *" +"\n";
                }

                if (tree.children.size() > 0) {
                    childListStack.add(new ArrayList<FaultTreeNode>(tree.children));
                }
            }
        }
        return resultTree;
    }
}
