package org.utfsm.utils;

import org.apache.jena.graph.Triple;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/** Misc tests for TestBinaryTree. */
public class TestBinaryTreePlan {


    @BeforeClass
    public static void beforeClass() {
    }

    @AfterClass
    public static void afterClass() {
    }

    // JENA-1746 : tests abort1 and abort2.
    // Inlines are not relevant.
    // Errors that can occur:
    //   One common term -> no conversion.
    //   Two common terms -> bad read.

    @Test
    public void dephTree() {
        BinaryTreePlan tree = new BinaryTreePlan("#");
        ArrayList<HashMap<String,ArrayList<String>>> triplesArr = new ArrayList<>();
        for (int i = 0; i < 4;i++) {
//            ArrayList<String> subType, predType, objType;
//            subType = getType(value.getSubject());
//            predType = getType(value.getPredicate());
//            objType = getType(value.getObject());
            HashMap<String, ArrayList<String>> treeLeaf = new HashMap<>();
//            // Get tpf_type
            ArrayList<String> tpfList = new ArrayList<>();
            tpfList.add("patron".concat(String.valueOf(i)));
            treeLeaf.put("tpf_type", tpfList);
            //Define predicates, in case of leaf only one( the triple predicate).
            ArrayList<String> preds = new ArrayList<>();
            preds.add("http://utfsm.cl/uri".concat(String.valueOf(i)));
            treeLeaf.put("predicates", preds);
            triplesArr.add(treeLeaf);
        }
        tree.addNodeList(triplesArr);
        assertEquals(
                "[ \"NONE#http://utfsm.cl/uri0#http://utfsm.cl/uri1#http://utfsm.cl/uri2#http://utfsm.cl/uri3\" ,\n" +
                        "  [ \"NONE#http://utfsm.cl/uri0#http://utfsm.cl/uri1#http://utfsm.cl/uri2\" ,\n" +
                        "    [ \"NONE#http://utfsm.cl/uri0#http://utfsm.cl/uri1\" ,\n" +
                        "      [ \"patron0#http://utfsm.cl/uri0\" ] ,\n" +
                        "      [ \"patron1#http://utfsm.cl/uri1\" ]\n" +
                        "    ] ,\n" +
                        "    [ \"patron2#http://utfsm.cl/uri2\" ]\n" +
                        "  ] ,\n" +
                        "  [ \"patron3#http://utfsm.cl/uri3\" ]\n" +
                        "]",
                tree.toString());
    }
    @Test
    public void singleNode() {
        BinaryTreePlan tree = new BinaryTreePlan("#");
        ArrayList<HashMap<String, ArrayList<String>>> lista = new ArrayList<>();
        HashMap<String, ArrayList<String>> treeLeaf = new HashMap<>();
//            // Get tpf_type
        ArrayList<String> tpfList = new ArrayList<>();
        tpfList.add("patron".concat("0"));
        treeLeaf.put("tpf_type", tpfList);
        //Define predicates, in case of leaf only one( the triple predicate).
        ArrayList<String> preds = new ArrayList<>();
        preds.add("http://utfsm.cl/uri".concat("single"));
        treeLeaf.put("predicates", preds);
        lista.add(treeLeaf);
        tree.addNodeList(lista);
        assertEquals("[ \"patron0#http://utfsm.cl/urisingle\" ]", tree.toString());
    }
    @Test
    public void singleNodeNull() {
        BinaryTreePlan tree = new BinaryTreePlan("#");
        ArrayList<HashMap<String, ArrayList<String>>> lista = new ArrayList<>();
        HashMap<String, ArrayList<String>> treeLeaf = new HashMap<>();
//            // Get tpf_type

        lista.add(null);
        tree.addNodeList(lista);
        assertEquals("[ \"patron0#http://utfsm.cl/urisingle\" ]", tree.toString());
    }
}
