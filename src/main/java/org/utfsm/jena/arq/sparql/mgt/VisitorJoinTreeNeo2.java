package org.utfsm.jena.arq.sparql.mgt;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.QuadPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.optimizer.reorder.PatternTriple;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderProc;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Path0;
import org.apache.jena.sparql.path.P_Path1;
import org.apache.jena.sparql.sse.writers.WriterLib;
import org.apache.jena.tdb2.sys.SystemTDB;
import org.utfsm.jena.arq.sparql.engine.optimizer.reorder.ReorderWeighted;
import org.utfsm.utils.BTNode;
import org.utfsm.utils.BinaryTreePlan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class VisitorJoinTreeNeo2 implements OpVisitorTDB {
    public IndentedWriter out;
    private ExecutionContext sContext;
    private QueryIterator input;
    public BinaryTreePlan TreeTDB;
    private ReorderWeighted transform;

    public VisitorJoinTreeNeo2(ExecutionContext sContext, QueryIterator input) {
        out = new IndentedLineBuffer();
        this.sContext =  sContext;
        this.input =  input;
        TreeTDB = new BinaryTreePlan(",");
        this.transform = (ReorderWeighted) SystemTDB.getDefaultReorderTransform();
    }

    private BTNode visitOpN(OpN op) {
//        BTNode root = new BTNode<>(op.getName());

        BTNode join = new BTNode<>("OpN_".concat(op.getName()));
        start(op, WriterLib.NL);
        for (Iterator<Op> iter = op.iterator(); iter.hasNext(); ) {

            Op sub = iter.next();
            if(join.left == null){
                join.left = visit(sub);
            }
            else if(join.right== null) {
                join.right = visit(sub);
            }
            else{
                BTNode aux = new BTNode<>("JOIN_SEQ");
                aux.left = join.left;
                aux.right = join.right;
                join = new BTNode<>("JOIN_SEQ");
                join.left = aux;
            }
//            out.ensureStartOfLine();
//            printOp(sub);
//            if(iter.hasNext())
//                out.write(", ");
        }
        return join;
    }

    private BTNode visitOp2(Op2 op, ExprList exprs) {
        BTNode node = new BTNode<>(op.getName());
        node.left = visit(op.getLeft());
        node.right = visit(op.getRight());
        return node;
    }

    private BTNode visitOp1(Op1 op) {
        BTNode node = new BTNode<>(op.getName());
        node.left = visit(op.getSubOp());
//        node.right = new BTNode("NONE");
        return node;
    }

    public BTNode visit(OpBGP opBGP) {
        BTNode node = new BTNode(opBGP.getName());

        if (opBGP.getPattern().size() == 1) {
            node.left = write(opBGP.getPattern(), true);
        }
        else{
//            start(opBGP, WriterLib.NL);
//            DatasetGraphTDB ds = (DatasetGraphTDB) sContext.getDataset();
//            ReorderTransformation transform = ds.getDefaultGraphTDB().getDSG().getReorderTransform();
            BasicPattern pattern = opBGP.getPattern();
            if ( this.transform != null )
            {
                ReorderProc proc = this.transform.reorderIndexes(opBGP.getPattern());
                // Then reorder original patten
                pattern = proc.reorder(opBGP.getPattern());
            }
            return write(pattern, false);
        }
//        node.right = new BTNode("NONE");
        return node;
    }

    public BTNode visit(OpQuadPattern opQuadP) {
        QuadPattern quads = opQuadP.getPattern();
        BTNode node = new BTNode(opQuadP.getName());
        if (quads.size() == 1) {
            start(new OpTriple(null), WriterLib.NoNL);
            HashMap<String, ArrayList<String>> res = formatQuad(quads.get(0));
            node.left = new BTNode(res.get("tpf_type"));

        }

//        DatasetGraphTDB ds = (DatasetGraphTDB) sContext.getDataset();
//        thisReorderTransformation transform = ds.getDefaultGraphTDB().getDSG().getReorderTransform();
        BasicPattern pattern = opQuadP.getBasicPattern();
        if ( this.transform != null )
        {
            ReorderProc proc = this.transform.reorderIndexes(pattern);
            // Then reorder original patten
            pattern = proc.reorder(pattern);
        }
        node.left = write(pattern, false);
//        node.right = new BTNode("NONE");
        return  node;
    }

    public BTNode visit(OpQuadBlock opQuads) {
        QuadPattern quads = opQuads.getPattern();
        BTNode node = new BTNode(opQuads.getName());

        if (quads.size() == 1) {
//            start(opQuads, WriterLib.NoNL);
            HashMap<String, ArrayList<String>> temp = formatQuad(quads.get(0));
            node.left = new BTNode(temp.get("tpf_type"));
//            finish(opQuads);
        }else{
            node.left = write(quads, false);
        }
//        node.right = new BTNode("NONE");
        return node;
    }

    private BTNode write(BasicPattern pattern, boolean oneLine) {
//        boolean first = true;
//        for (Triple t : pattern) {
//            formatTriple(t);
//            if (oneLine) {
//                if (!first)
//                    out.print(" ");
//            } else
//                out.println();
//            first = false;
//        }
        BinaryTreePlan tree = new BinaryTreePlan("ᶲ");
        ArrayList<HashMap<String, ArrayList<String>>> triplesArr = new ArrayList<>();

        for (Triple q : pattern) {
            HashMap<String, ArrayList<String>> treeLeaf = formatTriple(q);
            triplesArr.add(treeLeaf);
        }
        if(triplesArr.size() > 0)
        {
            tree.addNodeList(triplesArr);
            out.println(tree.toString().replaceAll("\n", " "));
        }
        return tree.getRoot();
    }

    private BTNode write(QuadPattern quads, boolean oneLine) {
        BinaryTreePlan tree = new BinaryTreePlan("ᶲ");
        ArrayList<HashMap<String, ArrayList<String>>> triplesArr = new ArrayList<>();

        for (Quad q : quads) {
            HashMap<String, ArrayList<String>> treeLeaf = formatQuad(q);
            triplesArr.add(treeLeaf);
        }
        if(triplesArr.size() > 0)
        {
            tree.addNodeList(triplesArr);
            out.println(tree.toString().replaceAll("\n", " "));
        }
        return tree.getRoot();
    }

    public BTNode visit(OpTriple opTriple) {
        BTNode node = new BTNode(opTriple.getName());
        node.left = new BTNode(formatTriple(opTriple.getTriple()));
        return node;
    }

    public BTNode visit(OpQuad opQuad) {
        BTNode node = new BTNode(opQuad.getName());
        node.left = new BTNode(formatQuad(opQuad.getQuad()));
        return node;
    }

    public BTNode visit(OpPath opPath) {

        BTNode node = new BTNode(opPath.getName());
        node.left = new BTNode(formatTriplePath(opPath.getTriplePath()));
        return node;
    }

    public BTNode visit(OpFind opFind) {

        BTNode node = new BTNode(opFind.getName());
        node.left = new BTNode(formatTriple(opFind.getTriple()));
        return node;
    }

    public BTNode visit(OpProcedure opProc) {
        BTNode node = new BTNode(opProc.getName());
        node.left = new BTNode(opProc.getSubOp());
        return node;
    }

    public BTNode visit(OpPropFunc opPropFunc) {
        BTNode node = new BTNode(opPropFunc.getName());
        node.left = visit(opPropFunc.getSubOp());
        return node;
    }

    public BTNode visit(OpJoin opJoin) {
        return visitOp2(opJoin, null);
    }

    public BTNode visit(OpSequence opSequence) {
        return visitOpN(opSequence);
    }

    public BTNode visit(OpDisjunction opDisjunction) {
        return visitOpN(opDisjunction);
    }

    public BTNode visit(OpLeftJoin opLeftJoin) {
        return visitOp2(opLeftJoin, opLeftJoin.getExprs());
    }

    public BTNode visit(OpDiff opDiff) {
        return visitOp2(opDiff, null);
    }

    public BTNode visit(OpMinus opMinus) {
        return visitOp2(opMinus, null);
    }

    public BTNode visit(OpUnion opUnion) {
        return visitOp2(opUnion, null);
    }

    public BTNode visit(OpConditional opCondition) {
        return visitOp2(opCondition, null);
    }

    public BTNode visit(OpFilter opFilter) {


        List<Expr> expressions = opFilter.getExprs().getList();
        HashMap<String, Integer> filters_types = new HashMap<>();
        for(int i=0; i< expressions.size();i++){
            Expr expr = expressions.get(i);
            if (expr.isFunction()){
                String operator = expr.getFunction().getFunctionSymbol().getSymbol();
                if(filters_types.containsKey(operator)){
                    int current = filters_types.get(operator);
                    filters_types.put(operator, current+1);
                }
                else {
                    filters_types.put(operator, 1);
                }
            }
        }
        HashMap<String,Object> data = new HashMap<>();
        data.put("name", opFilter.getName());
        data.put("data",filters_types);
        // Todo verificar si limite por default.
        BTNode node = new BTNode(data);
        node.left = visit(opFilter.getSubOp());
        return node;
    }

    public BTNode visit(OpGraph opGraph) {
        BTNode node = new BTNode(opGraph.getName());
        node.left = visit(opGraph.getSubOp());
        return node;
    }

    public BTNode visit(OpService opService) {
        BTNode node = new BTNode(opService.getName());
        node.left = visit(opService.getSubOp());
        return node;
    }

    public BTNode visit(OpTable opTable) {
        return new BTNode(opTable.getName());
    }

    public BTNode visit(OpDatasetNames dsNames) {
        return new BTNode(dsNames.getName());
    }

    public BTNode visit(OpExt opExt) {
        return new BTNode(opExt.getName());
    }

    public BTNode visit(OpNull opNull) {
        return new BTNode(opNull.getName());
    }

    public BTNode visit(OpLabel opLabel) {
        return new BTNode(opLabel.getName());
    }

    public BTNode visit(OpList opList) {
        return  visitOp1(opList);
    }

    public BTNode visit(OpGroup opGroup) {
        BTNode node = new BTNode(opGroup.getName());
        node.left = visit(opGroup.getSubOp());
        return node;
    }

    public BTNode visit(OpOrder opOrder) {
        BTNode node = new BTNode(opOrder.getName());
        node.left = visit(opOrder.getSubOp());
        return node;
    }

    public BTNode visit(OpTopN opTop) {
        HashMap<String,Object> data = new HashMap<>();
        data.put("name", opTop.getName());
        if (opTop.getLimit() != Query.NOLIMIT){
            data.put("start", Long.toString(0));
        }
        // Todo verificar si limite por default.
        data.put("limit",String.valueOf(data));
        BTNode node = new BTNode(opTop.getName());
        node.left = visit(opTop.getSubOp());
        return node;
    }

    public BTNode visit(OpProject opProject) {
        BTNode node = new BTNode<>(opProject.getName());
        node.left = visit(opProject.getSubOp());
        return node;
    }

    public BTNode visit(OpDistinct opDistinct) {
        return visitOp1(opDistinct);
    }

    public BTNode visit(OpReduced opReduced) {
        return visitOp1(opReduced);
    }

    public BTNode visit(OpAssign opAssign) {
        BTNode node = new BTNode<>(opAssign.getName());
        node.left = visit(opAssign.getSubOp());
        return node;
    }

    public BTNode visit(OpExtend opExtend) {
        BTNode node = new BTNode<>(opExtend.getName());
        node.left = visit(opExtend.getSubOp());
        return node;
    }

    public BTNode visit(OpSlice opSlice) {
        HashMap<String,Object> data = new HashMap<>();
        data.put("name", opSlice.getName());
        if (opSlice.getStart() != Query.NOLIMIT)
            data.put("start",Long.toString(opSlice.getStart()));
        else {
            data.put("start",Long.toString(0));
        }
        data.put("limit",String.valueOf(opSlice.getLength()));
        BTNode node = new BTNode<>(data);

        node.left = visit(opSlice.getSubOp());
        return node;
    }

    private void start(Op op, int newline) {
        WriterLib.start2(out, "\"".concat(op.getName()).concat("\""), newline);
        out.print(",");
    }

    public BTNode visit(Op op) {
        //Im not proud of this method:)
        if (op == null) {
            return new BTNode("NONE");
        }
        else if (op instanceof OpProject) {
            return visit((OpProject) op);
        }
        else if (op instanceof OpSlice) {
            return visit((OpSlice) op);
        }
        else if (op instanceof OpExtend) {
            return visit((OpExtend) op);
        }
        else if (op instanceof OpAssign) {
            return visit((OpAssign) op);
        }
        else if (op instanceof OpReduced) {
            return visit((OpReduced) op);
        }
        else if (op instanceof OpDistinct) {
            return visit((OpDistinct) op);
        }
        else if (op instanceof OpTopN) {
            return visit((OpTopN) op);
        }
        else if (op instanceof OpOrder) {
            return visit((OpOrder) op);
        }
        else if (op instanceof OpGroup) {
            return visit((OpGroup) op);
        }
        else if (op instanceof OpList) {
            return visit((OpList) op);
        }
        else if (op instanceof OpLabel) {
            return visit((OpLabel) op);
        }
        else if (op instanceof OpNull) {
            return visit((OpNull) op);
        }
        else if (op instanceof OpExt) {
            return visit((OpExt) op);
        }
        else if (op instanceof OpDatasetNames) {
            return visit((OpDatasetNames) op);
        }
        else if (op instanceof OpTable) {
            return visit((OpTable) op);
        }
        else if (op instanceof OpService) {
            return visit((OpService) op);
        }
        else if (op instanceof OpGraph) {
            return visit((OpGraph) op);
        }
        else if (op instanceof OpFilter) {
            return visit((OpFilter) op);
        }
        else if (op instanceof OpConditional) {
            return visit((OpConditional) op);
        }
        else if (op instanceof OpUnion) {
            return visit((OpUnion) op);
        }
        else if (op instanceof OpMinus) {
            return visit((OpMinus) op);
        }
        else if (op instanceof OpDiff) {
            return visit((OpDiff) op);
        }
        else if (op instanceof OpLeftJoin) {
            return visit((OpLeftJoin) op);
        }
        else if (op instanceof OpDisjunction) {
            return visit((OpDisjunction) op);
        }
        else if (op instanceof OpSequence) {
            return visit((OpSequence) op);
        }
        else if (op instanceof OpJoin) {
            return visit((OpJoin) op);
        }
        else if (op instanceof OpPropFunc) {
            return visit((OpPropFunc) op);
        }
        else if (op instanceof OpProcedure) {
            return visit((OpProcedure) op);
        }
        else if (op instanceof OpFind) {
            return visit((OpFind) op);
        }
        else if (op instanceof OpPath) {
            return visit((OpPath) op);
        }
        else if (op instanceof OpQuad) {
            return visit((OpQuad) op);
        }
        else if (op instanceof OpTriple) {
            return visit((OpTriple) op);
        }
        else if (op instanceof OpQuadPattern) {
            return visit((OpQuadPattern) op);
        }
        else if (op instanceof OpQuadBlock) {
            return visit((OpQuadBlock) op);
        }
        else if (op instanceof OpBGP) {
            return visit((OpBGP) op);
        }
        else
            return new BTNode("UNKNOW");
    }


    private HashMap<String, ArrayList<String>> formatQuad(Quad qp) {
        return formatTriple(qp.asTriple());
    }

    private HashMap<String, ArrayList<String>> formatTriple(Triple triple) {
        ArrayList<String> subType, predType, objType;
        subType = getType(triple.getSubject());
        if(triple.getPredicate() == null){
            predType = new ArrayList<>();
            predType.add("PATH");
        }
        else
            predType = getType(triple.getPredicate());

        objType = getType(triple.getObject());
        HashMap<String, ArrayList<String>> treeLeaf = new HashMap<>();
        // Get tpf_type
        String patron = subType.get(0).concat("_").concat(predType.get(0)).concat("_").concat(objType.get(0));
        ArrayList<String> tpfList = new ArrayList<>();
        tpfList.add(patron);
        treeLeaf.put("tpf_type", tpfList);
        ArrayList<String> cardinality = new ArrayList<>();
        double peso = this.transform.getTripleWeight(new PatternTriple(triple));
        cardinality.add( String.valueOf(peso));
        treeLeaf.put("cardinality", cardinality);
        //Define predicates, in case of leaf only one( the triple predicate).
        ArrayList<String> preds = new ArrayList<>();
        //Todo, need to validate.
        //Define the predicate of tpf in the order: (1)predicate, (2)subject,(3)object
        if(!patron.equals("VAR_VAR_VAR") && !patron.equals("VAR_VAR_LITERAL") && !patron.equals("LITERAL_VAR_VAR")){
            if(predType.get(0).equals("URI")){
                preds.add(triple.getPredicate().getURI());
            }else if (predType.get(0).equals("PATH")){
                preds.add("PATH");
            }
            else if (subType.get(0).equals("URI")){
                preds.add(triple.getSubject().getURI());
            }

            else{
                preds.add(triple.getObject().getURI());
            }
        }
        treeLeaf.put("predicates", preds);
        return treeLeaf;
    }

    private HashMap<String, ArrayList<String>> formatTriplePath(TriplePath tp) {
        Node predicate;
        if(tp.getPath() != null){
            if(tp.getPath() instanceof P_Path1) {
                predicate = ((P_Link)((P_Path1) tp.getPath()).getSubPath()).getNode();
            }
            else if(tp.getPath() instanceof P_Path0) {
                predicate = ((P_Path0) tp.getPath()).getNode();
            }
            else {
                //Todo is is just to avoid error
                predicate = tp.getPredicate();
            }
        }else
            predicate = tp.getPredicate();

        Triple triple = new Triple(tp.getSubject(), predicate, tp.getObject());
        return formatTriple(triple);
    }

    private ArrayList<String> getType(Node node){
        ArrayList<String> lista = new ArrayList<>();
        if (node.isVariable()){
            lista.add("VAR");
            lista.add(node.getName());
        }
        else if (node.isURI()){
            lista.add("URI");
            lista.add(node.getURI());
        }
        else if(node.isLiteral()){
            lista.add("LITERAL");
            lista.add(node.getLiteral().toString());
        }
        return lista;
    }
}
