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

    public FaultTreeNode recursiveTree(String id){
        CloudFailure cf = failureRepository.findById(id);
        if(!cf.isCategory) return null;
        FaultTreeNode node = new FaultTreeNode();
        node.id = cf.id;
        if(cf.children == null) return node;
        else{
            for(int i=0;i<cf.children.size();i++){
                FaultTreeNode childNode = recursiveTree(cf.children.get(i));
                if(childNode != null) node.children.add(childNode);
            }
        }
        return node;
    }

    public void printTree(FaultTreeNode tree) {
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

                System.out.println(indent + "+- " + tree.id);

                if (tree.children.size() > 0) {
                    childListStack.add(new ArrayList<FaultTreeNode>(tree.children));
                }
            }
        }
    }
}
