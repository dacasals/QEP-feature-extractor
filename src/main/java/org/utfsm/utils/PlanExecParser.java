package org.utfsm.utils;

import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.engine.PlanOp;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.sse.Item;
import org.apache.jena.sparql.sse.SSE;

public class PlanExecParser {

    public static Item parse(String planStr) {
        Item planItem = SSE.parse(planStr);
        return  planItem;
    }
}
