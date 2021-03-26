package org.utfsm.jena.arq.sparql.engine.optimizer.reorder;

import org.apache.jena.sparql.engine.optimizer.StatsMatcher;
import org.apache.jena.sparql.engine.optimizer.reorder.PatternTriple;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformationSubstitution;
import org.utfsm.apache.cmds.tdb2.tdbqueryplan;

public class ReorderWeighted extends ReorderTransformationSubstitution {
    private StatsMatcher stats ;

    public ReorderWeighted(StatsMatcher stats)
    {
        this.stats = stats ;
    }

    @Override
    protected double weight(PatternTriple pTriple)
    {
        double weight = stats.match(pTriple);
//        System.out.println(pTriple.toString().concat(" : ").concat(String.valueOf(weight)));
        tdbqueryplan.currentCardinality.put(pTriple.predicate.toString(),String.valueOf(weight));
        return weight;
    }
    public double getTripleWeight(PatternTriple pTriple){
        double weight = stats.match(pTriple);
        System.out.println(pTriple.toString().concat(" : ").concat(String.valueOf(weight)));
//        tdbqueryplan.currentCardinality.put(pTriple.toString(), weight);
        return weight;
    }
}
