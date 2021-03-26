package org.utfsm.jena.arq.sparql.mgt;

import com.github.jsonldjava.core.RDFDataset;
import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.table.TableEmpty;
import org.apache.jena.sparql.algebra.table.TableUnit;
import org.apache.jena.sparql.core.*;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.iterator.QueryIterPeek;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderProc;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.path.*;
import org.apache.jena.sparql.sse.Tags;
import org.apache.jena.sparql.sse.writers.*;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.tdb2.store.DatasetGraphTDB;
import org.utfsm.utils.BinaryTreePlan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

public class VisitorNeo implements OpVisitor {
    public IndentedWriter out;
    private ExecutionContext sContext;
    private QueryIterator input;
    public String salida = "";
    public VisitorNeo(ExecutionContext sContext, QueryIterator input) {
        out = new IndentedLineBuffer();
        this.sContext =  sContext;
        this.input =  input;
    }

    private void visitOpN(OpN op) {
        ArrayList<Op> ops = new ArrayList<>();
        for (Iterator<Op> iter = op.iterator(); iter.hasNext(); ) {
            Op sub = iter.next();
            if(sub instanceof OpTable){
                continue;
            }
            ops.add(sub);
        }
        if(ops.size() == 0){
            return;
        }
        else {
            printJoin(ops,"\"JOIN\"");
        }
    }
    private void printJoin(ArrayList<Op> ops, String joinType){
        if(ops.size() > 2){
            out.print(Tags.LBRACKET);
            out.print(joinType);
            out.print(", ");
                printOp(ops.get(0));
                out.print(", ");
                ops.remove(0);
                printJoin(ops, joinType);
            out.print(Tags.RBRACKET);
        }
        else if(ops.size() == 2){
            out.print(Tags.LBRACKET);
            out.print("\"JOIN\"");
            out.print(", ");
                printOp(ops.get(0));
                out.print(", ");
                printOp(ops.get(1));
            out.print(Tags.RBRACKET);
        }
        else {
            printOp(ops.get(0));
        }
    }

    private void visitOp2(OpConditional op, ExprList exprs) {
        if(op.getLeft() instanceof OpTable || op.getLeft() instanceof OpLabel) {
            printOp(op.getRight());
            return;
        }
        if(op.getRight() instanceof OpTable || op.getRight() instanceof OpLabel) {
            printOp(op.getLeft());
            return;
        }
        out.print(Tags.LBRACKET);
        out.println("\"LEFT_JOIN\"");
        out.print(", ");
        printOp(op.getLeft());
        out.ensureStartOfLine();
        out.print(", ");
        printOp(op.getRight());
        finish(op);
    }
    private void visitOp2(OpUnion op, ExprList exprs) {
        if(op.getLeft() instanceof OpTable || op.getLeft() instanceof OpLabel) {
            printOp(op.getRight());
            return;
        }
        if(op.getRight() instanceof OpTable || op.getRight() instanceof OpLabel) {
            printOp(op.getLeft());
            return;
        }
        out.print(Tags.LBRACKET);
        out.println("\"LEFT_JOIN\"");
        out.print(", ");
        printOp(op.getLeft());

        out.ensureStartOfLine();
        out.print(", ");
        printOp(op.getRight());
        finish(op);
    }

    private void visitOp2(OpJoin op, ExprList exprs) {
        out.print(Tags.LBRACKET);
        out.println("\"JOIN\"");
        out.print(", ");
        printOp(op.getLeft());
        out.ensureStartOfLine();
        out.print(", ");
        printOp(op.getRight());
        finish(op);
    }
    private void visitOp2(OpMinus op, ExprList exprs) {
        out.print(Tags.LBRACKET);
        out.println("\"MINUS\"");
        out.print(", ");
        printOp(op.getLeft());
        out.print(", ");
        printOp(op.getRight());
        finish(op);
    }
    private void visitOp2(Op2 op, ExprList exprs) {
        out.print(Tags.LBRACKET);
        out.println("\"OP2\"");
        out.print(", ");
        printOp(op.getLeft());

        out.ensureStartOfLine();
        out.print(", ");
        printOp(op.getRight());
        finish(op);
    }
    private void visitOp1(OpDistinct op) {

//        start(op, WriterLib.NL);
        printOp(op.getSubOp());
//        finish(op);
    }
    private void visitOp1(Op1 op) {

        start(op, WriterLib.NL);
        printOp(op.getSubOp());
        finish(op);
    }

    @Override
    public void visit(OpBGP opBGP) {
        if (opBGP.getPattern().size() == 1) {
            start(opBGP, WriterLib.NoNL);
            HashMap<String, ArrayList<String>> res = formatTriple(opBGP.getPattern().get(0));
            BinaryTreePlan tree = new BinaryTreePlan("ᶲ");
            out.print("\"".concat(tree.printLeafDataNode(res)).concat("\""));
            finish(opBGP);
            return;
        }

        start(opBGP, WriterLib.NL);
        DatasetGraphTDB ds = (DatasetGraphTDB) sContext.getDataset();
        ReorderTransformation transform = ds.getDefaultGraphTDB().getDSG().getReorderTransform();
        BasicPattern pattern = opBGP.getPattern();
        if ( transform != null )
        {
            ReorderProc proc = transform.reorderIndexes(opBGP.getPattern());
            // Then reorder original patten
            pattern = proc.reorder(opBGP.getPattern());
        }
        write(pattern, false);
        finish(opBGP);
    }

    @Override
    public void visit(OpQuadPattern opQuadP) {
        QuadPattern quads = opQuadP.getPattern();
        if (quads.size() == 1) {
//            start(opQuadP, WriterLib.NoNL);
            out.print(Tags.LBRACKET);

            HashMap<String, ArrayList<String>> res = formatQuad(quads.get(0));
            BinaryTreePlan tree = new BinaryTreePlan("ᶲ");
            out.print("\"".concat(tree.printLeafDataNode(res)).concat("\""));
            out.print(Tags.RBRACKET);
            return;
        }

        DatasetGraphTDB ds = (DatasetGraphTDB) sContext.getDataset();
        ReorderTransformation transform = ds.getDefaultGraphTDB().getDSG().getReorderTransform();
        OpBGP opBGP = new OpBGP(opQuadP.getBasicPattern()) ;
        BasicPattern pattern = opBGP.getPattern();
        if ( transform != null )
        {
            ReorderProc proc = transform.reorderIndexes(opBGP.getPattern());
            // Then reorder original patten
            pattern = proc.reorder(opBGP.getPattern());
        }
        write(pattern, true);
    }

    @Override
    public void visit(OpQuadBlock opQuads) {
        QuadPattern quads = opQuads.getPattern();
        if (quads.size() == 1) {
            start(opQuads, WriterLib.NoNL);
            HashMap<String, ArrayList<String>> res = formatQuad(quads.get(0));
            BinaryTreePlan tree = new BinaryTreePlan("ᶲ");
            out.print("\"".concat(tree.printLeafDataNode(res)).concat("\""));
            finish(opQuads);
            return;
        }
        start(opQuads, WriterLib.NL);
        write(quads, false);
        finish(opQuads);
    }

    private void write(BasicPattern pattern, boolean oneLine) {
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
    private void write(QuadPattern quads, boolean oneLine) {
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
//
//        int count= 0;
//        out.println(Tags.LBRACKET);
//        for (Quad t : quads) {
//            count ++;
//            formatQuad(t);
//            if(count < quads.size()) {
//                out.print(", ");
//            }
//        }
//        out.println(Tags.RBRACKET);
    }

    @Override
    public void visit(OpTriple opTriple) {
        HashMap<String, ArrayList<String>> res = formatTriple(opTriple.getTriple());
        BinaryTreePlan tree = new BinaryTreePlan("ᶲ");
        out.print("\"".concat(tree.printLeafDataNode(res)).concat("\""));
    }

    @Override
    public void visit(OpQuad opQuad) {
        HashMap<String, ArrayList<String>> res = formatQuad(opQuad.getQuad());
        BinaryTreePlan tree = new BinaryTreePlan("ᶲ");
        out.print("\"".concat(tree.printLeafDataNode(res)).concat("\""));
    }

    @Override
    public void visit(OpPath opPath) {
        out.print(Tags.LBRACKET);
//        out.print(formatTriplePath(opPath.getTriplePath());
        HashMap<String, ArrayList<String>> res = formatTriplePath(opPath.getTriplePath());
        BinaryTreePlan tree = new BinaryTreePlan("ᶲ");
        out.print("\"".concat(tree.printLeafDataNode(res)).concat("\""));
        out.print(Tags.RBRACKET);
    }

    @Override
    public void visit(OpFind opFind) {
        start(opFind, WriterLib.NoNL);
//        out.print(opFind.getVar());
        out.print(", ");
        HashMap<String, ArrayList<String>> res = formatTriple(opFind.getTriple());
        BinaryTreePlan tree = new BinaryTreePlan("ᶲ");
        out.print("\"".concat(tree.printLeafDataNode(res)).concat("\""));
        finish(opFind);
    }

    @Override
    public void visit(OpProcedure opProc) {
//        start(opProc, WriterLib.NoNL);
//        WriterNode.output(out, opProc.getProcId(), sContext);
//        out.println();
//        WriterExpr.output(out, opProc.getArgs(), true, false, sContext);
//        out.println();
        printOp(opProc.getSubOp());
//        finish(opProc);
    }

    @Override
    public void visit(OpPropFunc opPropFunc) {
//        start(opPropFunc, WriterLib.NoNL);
//        out.print(FmtUtils.stringForNode(opPropFunc.getProperty(), sContext));
//        out.println();
//
//        outputPF(opPropFunc.getSubjectArgs());
//        out.print(" ");
//        outputPF(opPropFunc.getObjectArgs());
//        out.println();
        printOp(opPropFunc.getSubOp());
//        finish(opPropFunc);
    }

//    private void outputPF(PropFuncArg pfArg) {
//        if (pfArg.isNode()) {
//            WriterNode.output(out, pfArg.getArg(), sContext);
//            return;
//        }
//        WriterNode.output(out, pfArg.getArgList(), sContext);
//    }

    @Override
    public void visit(OpJoin opJoin) {
        visitOp2(opJoin, null);
    }

    @Override
    public void visit(OpSequence opSequence) {
        visitOpN(opSequence);
    }

    @Override
    public void visit(OpDisjunction opDisjunction) {
        visitOpN(opDisjunction);
    }

    @Override
    public void visit(OpLeftJoin opLeftJoin) {
        visitOp2(opLeftJoin, opLeftJoin.getExprs());
    }

    @Override
    public void visit(OpDiff opDiff) {
        visitOp2(opDiff, null);
    }

    @Override
    public void visit(OpMinus opMinus) {
        visitOp2(opMinus, null);
    }

    @Override
    public void visit(OpUnion opUnion) {
        visitOp2(opUnion, null);
    }

    @Override
    public void visit(OpConditional opCondition) {
        visitOp2(opCondition, null);
    }

    @Override
    public void visit(OpFilter opFilter) {
        printOp(opFilter.getSubOp());
    }

    @Override
    public void visit(OpGraph opGraph) {
//        start(opGraph, WriterLib.NoNL);
//        out.println(FmtUtils.stringForNode(opGraph.getNode(), sContext));
        opGraph.getSubOp().visit(this);
//        finish(opGraph);
    }

    @Override
    public void visit(OpService opService) {
//        start(opService, WriterLib.NoNL);
//        if (opService.getSilent())
//            out.println("silent ");
//        out.println(FmtUtils.stringForNode(opService.getService(), sContext));
        opService.getSubOp().visit(this);
//        finish(opService);
    }

    @Override
    public void visit(OpTable opTable) {
//        if (TableUnit.isTableUnit(opTable.getTable())) {
//            start(opTable, WriterLib.NoNL);
//            out.print("unit");
//            finish(opTable);
//            return;
//        }
//
//        if (TableEmpty.isTableEmpty(opTable.getTable())) {
//            start(opTable, WriterLib.NoNL);
//            out.print("empty");
//            finish(opTable);
//            return;
//        }
//
//        start(opTable, WriterLib.NoNL);
//        WriterNode.outputVars(out, opTable.getTable().getVars(), null);
//        if (!opTable.getTable().isEmpty()) {
//            out.println();
//            WriterTable.outputPlain(out, opTable.getTable(), null);
//        }
        out.print(Tags.LBRACKET);
        out.print("\"TABLE\"");
        finish(opTable);
    }

    @Override
    public void visit(OpDatasetNames dsNames) {
        start(dsNames, WriterLib.NoNL);
        WriterNode.output(out, dsNames.getGraphNode(), null);
        finish(dsNames);
    }

    @Override
    public void visit(OpExt opExt) {
        // start(opExt, WriterLib.NL) ;
        opExt.output(out, null);
        // finish(opExt) ;
    }

    @Override
    public void visit(OpNull opNull) {
        start(opNull, WriterLib.NoSP);
        finish(opNull);
    }

    @Override
    public void visit(OpLabel opLabel) {
        String x = FmtUtils.stringForString(opLabel.getObject().toString());
//        if (opLabel.hasSubOp()) {
//            start(opLabel, WriterLib.NL);
//            out.println(x);
//            printOp(opLabel.getSubOp());
//            finish(opLabel);
//        } else {
//            start(opLabel, WriterLib.NoNL);
//            out.print(x);
//            finish(opLabel);
//        }
        out.print(Tags.LBRACKET);
        out.print("\"".concat(x).concat("\""));
        out.println(Tags.RBRACKET);
    }

    @Override
    public void visit(OpList opList) {
        visitOp1(opList);
    }

    @Override
    public void visit(OpGroup opGroup) {
//        start(opGroup, WriterLib.NoNL);
//        writeNamedExprList(opGroup.getGroupVars());
//        if (!opGroup.getAggregators().isEmpty()) {
//            // --- Aggregators
//            out.print(" ");
//            start();
//            out.incIndent();
//            boolean first = true;
//            for (ExprAggregator agg : opGroup.getAggregators()) {
//                if (!first) {
//                    out.print(" ");
//                }
//                first = false;
//                Var v = agg.getVar();
//                String str = agg.getAggregator().toPrefixString();
//                start();
//                out.print(v.toString());
//                out.print(" ");
//                out.print(str);
//                finish();
//            }
//            finish();
//            out.decIndent();
//        }
//        out.println();
        printOp(opGroup.getSubOp());
//        finish(opGroup);
    }

    @Override
    public void visit(OpOrder opOrder) {
//        start(opOrder, WriterLib.NoNL);

        // Write conditions
//        start();
//
//        boolean first = true;
//        for (SortCondition sc : opOrder.getConditions()) {
//            if (!first)
//                out.print(" ");
//            first = false;
//            formatSortCondition(sc);
//        }
//        finish();
//        out.newline();
        printOp(opOrder.getSubOp());
//        finish(opOrder);
    }

    @Override
    public void visit(OpTopN opTop) {
//        start(opTop, WriterLib.NoNL);
//
//        // Write conditions
//        start();
//        writeIntOrDefault(opTop.getLimit());
//        out.print(" ");
//
//        boolean first = true;
//        for (SortCondition sc : opTop.getConditions()) {
//            if (!first)
//                out.print(" ");
//            first = false;
//            formatSortCondition(sc);
//        }
//        finish();
//        out.newline();
        printOp(opTop.getSubOp());
//        finish(opTop);
    }

    // Neater would be a pair of explicit SortCondition formatters
    private void formatSortCondition(SortCondition sc) {
        boolean close = true;
        String tag = null;

        if (sc.getDirection() != Query.ORDER_DEFAULT) {
            if (sc.getDirection() == Query.ORDER_ASCENDING) {
                tag = Tags.tagAsc;
                WriterLib.start(out, tag, WriterLib.NoNL);
            }

            if (sc.getDirection() == Query.ORDER_DESCENDING) {
                tag = Tags.tagDesc;
                WriterLib.start(out, tag, WriterLib.NoNL);
            }

        }

        WriterExpr.output(out, sc.getExpression(), null);

        if (tag != null)
            WriterLib.finish(out, tag);
    }

    @Override
    public void visit(OpProject opProject) {
//        start(opProject, WriterLib.NoNL);
//        out.println(Tags.LBRACKET);
        printOp(opProject.getSubOp());
//        finish(opProject);
    }

    @Override
    public void visit(OpDistinct opDistinct) {
        visitOp1(opDistinct);
    }

    @Override
    public void visit(OpReduced opReduced) {
        visitOp1(opReduced);
    }

    @Override
    public void visit(OpAssign opAssign) {
//        start(opAssign, WriterLib.NoNL);
//        writeNamedExprList(opAssign.getVarExprList());
//        out.println();
        printOp(opAssign.getSubOp());
//        finish(opAssign);
    }

    @Override
    public void visit(OpExtend opExtend) {
//        start(opExtend, WriterLib.NoNL);
//        writeNamedExprList(opExtend.getVarExprList());
//        out.println();
        printOp(opExtend.getSubOp());
//        finish(opExtend);
    }

    @Override
    public void visit(OpSlice opSlice) {
//        start(opSlice, WriterLib.NoNL);
//        writeIntOrDefault(opSlice.getStart());
//        out.print(" ");
//        writeIntOrDefault(opSlice.getLength());
//        out.println();
        printOp(opSlice.getSubOp());
//        finish(opSlice);
    }

    private void writeIntOrDefault(long value) {
        String x = "_";
        if (value != Query.NOLIMIT)
            x = Long.toString(value);
        out.print(x);
    }

    private void start(Op op, int newline) {
        WriterLib.start2(out, op.getName(), newline);
    }

    private void finish(Op op) {
        WriterLib.finish2(out, op.getName());
    }

    private void start() {
        WriterLib.start(out);
    }

    private void finish() {
        WriterLib.finish(out);
    }

    private void printOp(Op op) {
        if (op == null) {
            WriterLib.start(out, Tags.tagNull, WriterLib.NoSP);
            WriterLib.finish(out, Tags.tagNull);
        } else
            op.visit(this);
    }

    private void writeNamedExprList(VarExprList project) {
        start();
        boolean first = true;
        for (Var v : project.getVars()) {
            if (!first)
                out.print(" ");
            first = false;
            Expr expr = project.getExpr(v);
            if (expr != null) {
                start();
                out.print(v.toString());
                out.print(" ");
                WriterExpr.output(out, expr, null);
                finish();
            } else
                out.print(v.toString());
        }
        finish();
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

    private HashMap<String, ArrayList<String>> formatQuad(Quad qp) {
        return formatTriple(qp.asTriple());
    }

    private HashMap<String, ArrayList<String>> formatTriplePath(TriplePath tp) {
        Node predicate = NodeFactory.createURI("<http://fakeuri.com/fake>");
        String predi =tp.getPath().toString();
        String[] resp = predi.split("<|>");
        for (int i = 0; i < resp.length; i++) {
            if(resp[i].startsWith("http")){
                predicate = NodeFactory.createURI("<".concat(resp[i]).concat(">"));
                break;
            }
        }
        Triple triple = new Triple(tp.getSubject(), predicate, tp.getObject());
        return formatTriple(triple);
    }

}
