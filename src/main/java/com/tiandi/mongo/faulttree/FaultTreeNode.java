package com.tiandi.mongo.faulttree;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 谢天帝
 * @version v0.1 2017/4/29.
 */
public class FaultTreeNode {

    public String id;
    public boolean isCategory;
    public List<FaultTreeNode> children = new ArrayList<>();

    public FaultTreeNode(){}
}
