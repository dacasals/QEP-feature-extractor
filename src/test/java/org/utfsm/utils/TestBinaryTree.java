package org.utfsm.utils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/** Misc tests for TestBinaryTree. */
public class TestBinaryTree {
    // Safe on MS Windows - different directories for abort1 and abort2.
//    static String DIR1 = "target/queries_test.csv";
//    static String DIR2 = "target/tdb-testing/DB_2";

    @BeforeClass
    public static void beforeClass() {
//        FileOps.ensureDir(DIR1);
//        FileOps.ensureDir(DIR2);
    }

    @AfterClass
    public static void afterClass() {
//        try {
//            FileOps.clearAll(DIR1);
////            FileOps.clearAll(DIR2);
//            FileOps.deleteSilent(DIR1);
////            FileOps.deleteSilent(DIR2);
//        } catch (Exception ex) {}
    }

    // JENA-1746 : tests abort1 and abort2.
    // Inlines are not relevant.
    // Errors that can occur:
    //   One common term -> no conversion.
    //   Two common terms -> bad read.

    @Test
    public void dephTree() {
        BinaryTree<Integer> tree = new BinaryTree<Integer>() {
            @Override
            public void defineDataJoinNode(BTNode<Integer> node) {
                node.data = -1;
            }
            @Override
            public String printDataJoin(BTNode<Integer> node) {
                return node.data.toString();
            }
            @Override
            public String printLeafDataNode(BTNode<Integer> node) {
                return node.data.toString();
            }
        };
        ArrayList<Integer> lista = new ArrayList<>();
        lista.add(6);
        lista.add(7);
        lista.add(5);
        tree.addNodeList(lista);
        assertEquals(
                "[ \"-1\" ,\n  [ \"-1\" ,\n    [ \"6\" ] ,\n    [ \"7\" ]\n  ] ,\n  [ \"5\" ]\n]",
                tree.toString());

    }
    @Test
    public void singleNode() {
        BinaryTree<Integer> tree = new BinaryTree<Integer>() {
            @Override
            public void defineDataJoinNode(BTNode<Integer> node) {
                node.data = -1;
            }
            @Override
            public String printDataJoin(BTNode<Integer> node) {
                return node.data.toString();
            }
            @Override
            public String printLeafDataNode(BTNode<Integer> node) {
                return node.data.toString();
            }
        };
        ArrayList<Integer> lista = new ArrayList<>();
        lista.add(6);
        tree.addNodeList(lista);
        assertEquals("[ \"6\" ]", tree.toString());
//        System.out.println(tree);

    }
}
