package org.utfsm.utils;

import org.apache.jena.atlas.json.JsonString;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BTNode2<T>
{
    public T data;
    public BTNode2<T> left;
    public BTNode2<T> right;
    public BTNode2(T item)
    {
        data = item;
        left = right = null;
    }

//    @Override
//    public String toString() {
//        JSONArray json = new JSONArray();
//
//
//        if(data instanceof HashMap){
//            if(((HashMap<?, ?>) data).containsKey("name")){
//                String name  = (String) ((HashMap<?, ?>) data).get("name");
//                if(name.equals("slice") || name.equals("top") ){
//                    String start  = (String) ((HashMap<?, ?>) data).get("start");
//                    String limit  = (String) ((HashMap<?, ?>) data).get("limit");
//                    json.add(new JsonString(name.concat("ᶲ").concat(start).concat("ᶲ").concat(limit)));
//                }
//                else if(name.equals("filter")){
//                    HashMap<String,Integer> datafilters = (HashMap<String, Integer>) ((HashMap<String, Object>) data).get("data");
//                    Iterator it = datafilters.entrySet().iterator();
//
//                    String jsonString =name;
//                    while (it.hasNext()) {
//                        Map.Entry pair = (Map.Entry)it.next();
//                        System.out.println(pair.getKey() + " = " + pair.getValue());
//                        jsonString = jsonString.concat("ᶲ").concat(pair.getKey().toString().concat(":").concat(pair.getValue().toString()));
//                    }
//                    json.add(new JsonString(jsonString));
//                }
//                else {
//                    json.add(new JsonString(name));
//                }
//
//            }else{
//                json.add(new JsonString(((HashMap<String, ArrayList>) data).get("tpf_type").get(0).toString()));
//            }
//        }
//        else {
//            json.add(new JsonString(data.toString()));
//        }
//        //Add Children
//        JSONArray children = new JSONArray();
//        if(this.left != null){
//            children.add(this.left);
//        }else {
//            children.add(new JsonString("NONE"));
//        }
//        if(this.right != null){
//            children.add(this.right);
//        }else {
//            children.add(new JsonString("NONE"));
//        }
//        json.add(children);
//        return  json.toString();
//    }
    @Override
    public String toString() {
        JSONArray json = new JSONArray();

        if(data instanceof HashMap){
            if(((HashMap<?, ?>) data).containsKey("name")){
                String name  = (String) ((HashMap<?, ?>) data).get("name");
                if(name.equals("slice") || name.equals("top") ){
                    String start  = (String) ((HashMap<?, ?>) data).get("start");
                    String limit  = (String) ((HashMap<?, ?>) data).get("limit");
                    json.add(new JsonString(name.concat("ᶲ").concat(start).concat("ᶲ").concat(limit)));
                }
                else if(name.equals("filter")){
                    HashMap<String,Integer> datafilters = (HashMap<String, Integer>) ((HashMap<String, Object>) data).get("data");
                    Iterator it = datafilters.entrySet().iterator();

                    String jsonString =name;
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        System.out.println(pair.getKey() + " = " + pair.getValue());
                        jsonString = jsonString.concat("ᶲ").concat(pair.getKey().toString().concat(":").concat(pair.getValue().toString()));
                    }
                    json.add(new JsonString(jsonString));
                }
                else {
                    json.add(new JsonString(name));
                }
            }
            else{
                String a ="";
                String tpf_type = ((HashMap<String, ArrayList>) data).get("tpf_type").get(0).toString();
                a = a.concat(tpf_type);
                a = a.concat("ᶲ").concat(((HashMap<String, ArrayList>) data).get("cardinality").get(0).toString());

//                json.add(new JsonString(tpf_type));
                ArrayList<String> predicates = ((HashMap<String,ArrayList>) data).get("predicates");
                for (int i = 0; i < predicates.size(); i++) {
                    a = a.concat("ᶲ").concat(predicates.get(i));
                }
                json.add(new JsonString(a));
            }
        }
        else {
            if(data.equals("LEFT_JOIN")){
                String resp = data.toString();
                double card = 0.;
                try {
                    String[] left = this.getLeftJoin(this.left).split("ᶲ");
                    String[] right = this.getLeftJoin(this.right).split("ᶲ");

                    if(!left[0].equals("NONE_TPF"))
                        card = Double.parseDouble(left[1]);

                    resp = resp.concat("ᶲ").concat(String.valueOf(card));
                    for (int i = 2; i < left.length; i++) {
                        resp = resp.concat("ᶲ").concat(left[i]);
                    }
                    for (int i = 2; i < right.length; i++) {
                        resp = resp.concat("ᶲ").concat(right[i]);
                    }
                }
                catch (Exception ex){
                    System.out.println("Error on LEFT_JOIN");
                    ex.printStackTrace();
                    resp.concat("ᶲ").concat(String.valueOf(card));
                }

                json.add(new JsonString(resp));
            }
            else {
                json.add(new JsonString(data.toString()));
            }

        }
        //Add Children
        JSONArray children = new JSONArray();
        if(this.left == null && this.right == null){
            return json.toString();
        }
        if(this.left != null){
            children.add(this.left);
        }else {
            children.add(new JsonString("NONE"));
        }
        if(this.right != null){
            children.add(this.right);
        }
        else {
            children.add(new JsonString("NONE"));
        }
        json.add(children);
        return  json.toString();
    }

    public String getLeftJoin(BTNode2 node) {

        if(node.data instanceof String) {
            return (String) node.data;
        }
        else if(node.data instanceof HashMap && ((HashMap<?, ?>) node.data).containsKey("tpf_type")){
            String a ="";
            String tpf_type = ((HashMap<String, ArrayList>) node.data).get("tpf_type").get(0).toString();
            a = a.concat(tpf_type);
            a = a.concat("ᶲ").concat(((HashMap<String, ArrayList>) node.data).get("cardinality").get(0).toString());

//                json.add(new JsonString(tpf_type));
            ArrayList<String> predicates = ((HashMap<String,ArrayList>) node.data).get("predicates");
            for (int i = 0; i < predicates.size(); i++) {
                a = a.concat("ᶲ").concat(predicates.get(i));
            }
            return a;
        }
        else {
            return "NONE_TPF";
        }
    }
}