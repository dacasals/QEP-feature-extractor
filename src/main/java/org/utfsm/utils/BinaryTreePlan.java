package org.utfsm.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class BinaryTreePlan extends BinaryTree<HashMap<String, ArrayList<String>>> {

    public BinaryTreePlan(String delimiter) {
        super(delimiter);
    }
    public BTNode<HashMap<String, ArrayList<String>>> getRoot() {
        return this.root;
    }

    @Override
    public void defineDataJoinNode(BTNode<HashMap<String, ArrayList<String>>> node) {
        HashMap<String, ArrayList<String>> join = new HashMap<>();
        String tpf_type = "JOIN";
        //Add node type
        ArrayList<String> tpfList = new ArrayList<>();
        tpfList.add(tpf_type);
        join.put("tpf_type", tpfList);
        //Add preds from left and right
        ArrayList<String> preds = new ArrayList<>();
        preds.addAll(node.left.data.get("predicates"));
        preds.addAll(node.right.data.get("predicates"));
        join.put("predicates", preds);
        //Todo define other info.
//        ArrayList<String> min = node.left.data.get("cardinality");
//        ArrayList<String> minr = node.right.data.get("cardinality");
//        if(Double.parseDouble(min.get(0)) > Double.parseDouble(minr.get(0)))
//            min = minr;
//        join.put("cardinality",min);
        node.data = join;
    }
    public void defineLeftJoinNode(BTNode<HashMap<String, ArrayList<String>>> node, String tpf_type) {

        HashMap<String, ArrayList<String>> join = new HashMap<>();
        //Add node type
        ArrayList<String> tpfList = new ArrayList<>();
        tpfList.add(tpf_type);
        join.put("tpf_type", tpfList);
        //Add preds from left and right
        ArrayList<String> preds = new ArrayList<>();
        preds.addAll(node.left.data.get("predicates"));
        preds.addAll(node.right.data.get("predicates"));
        join.put("predicates", preds);
        //Todo define other info.
        node.data = join;
    }

    @Override
    public String printDataJoin(BTNode<HashMap<String, ArrayList<String>>> node) {
        return node.data.get("tpf_type").get(0).
                concat(this.delimiterValues).
                concat(String.join(this.delimiterValues, node.data.get("predicates")));
    }

    @Override
    public String printLeafDataNode(BTNode<HashMap<String, ArrayList<String>>> node) {
        //If predicate list ins empty then we add "NONE" symbol to indicate note preds in the tpf
        //Todo interpret this in the parse
        return node.data.get("tpf_type").get(0).
                concat(this.delimiterValues).
                concat(node.data.get("predicates").size() == 0 ? "NONE" : node.data.get("predicates").get(0));
    }
    public String printLeafDataNode(HashMap<String, ArrayList<String>> node) {
        //If predicate list ins empty then we add "NONE" symbol to indicate note preds in the tpf
        //Todo interpret this in the parse
        return node.get("tpf_type").get(0).
                concat(this.delimiterValues).
                concat(node.get("predicates").size() == 0 ? "NONE" : node.get("predicates").get(0));
    }
}
