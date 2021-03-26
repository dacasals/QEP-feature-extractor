package org.utfsm.apache.cmds.arq.cmdline;

import jena.cmd.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.PlanOp;
import org.apache.jena.sparql.engine.main.OpExecutorFactory;
import org.apache.jena.sparql.sse.Item;
import org.apache.jena.util.FileUtils;

import java.io.IOException;
import jena.cmd.ArgDecl;
import jena.cmd.CmdArgModule;
import jena.cmd.CmdException;
import jena.cmd.CmdGeneral;
import jena.cmd.ModBase;
import jena.cmd.TerminationException;
import org.utfsm.utils.PlanExecParser;

public class ModQueryPlanIn extends ModBase {
    protected final ArgDecl queryFileDecl   = new ArgDecl(ArgDecl.HasValue, "query", "file") ;
    protected final ArgDecl planFileDecl   = new ArgDecl(ArgDecl.HasValue, "plan", "plan_file") ;
    protected final ArgDecl querySyntaxDecl = new ArgDecl(ArgDecl.HasValue, "syntax", "syn", "in") ;
    protected final ArgDecl queryBaseDecl   = new ArgDecl(ArgDecl.HasValue, "base") ;

    private Syntax defaultQuerySyntax;
    private Syntax          querySyntax;
    private String          queryFilename   = null ;
    private String          planFilename   = null ;
    private String          queryString     = null ;
    private String          planString     = null ;
    private Query query           = null ;
    private PlanOp planOp           = null ;
    private String          baseURI         = null ;

    public ModQueryPlanIn(Syntax defaultSyntax) {
        defaultQuerySyntax = defaultSyntax ;
        querySyntax = defaultSyntax ;
    }

    @Override
    public void registerWith(CmdGeneral cmdLine) {
        cmdLine.getUsage().startCategory("Query") ;
        cmdLine.add(queryFileDecl,   "--query, --file",  "File containing a query") ;
        cmdLine.add(planFileDecl,   "--plan, --plan_file",  "File containing a plan") ;
        cmdLine.add(querySyntaxDecl, "--syntax, --in",   "Syntax of the query") ;
        cmdLine.add(queryBaseDecl,   "--base",           "Base URI for the query") ;
    }

    @Override
    public void processArgs(CmdArgModule cmdline) throws IllegalArgumentException {
        if ( cmdline.contains(queryBaseDecl) )
            baseURI = cmdline.getValue(queryBaseDecl) ;

        if ( cmdline.contains(queryFileDecl) ) {
            queryFilename = cmdline.getValue(queryFileDecl) ;
//            planFilename = cmdline.getValue(planFileDecl) ;
            querySyntax = Syntax.guessQueryFileSyntax(queryFilename, defaultQuerySyntax) ;
        }
        if ( cmdline.contains(planFileDecl) ) {
            planFilename = cmdline.getValue(planFileDecl) ;
            querySyntax = Syntax.guessQueryFileSyntax(queryFilename, defaultQuerySyntax) ;
        }

        if ( cmdline.getNumPositional() == 0 && queryFilename == null )
            cmdline.cmdError("No query string or query file") ;

        if ( cmdline.getNumPositional() == 1 && queryFilename == null && planFilename == null )
            cmdline.cmdError("No plan string or plan file") ;

        if ( cmdline.getNumPositional() > 2 )
            cmdline.cmdError("Only one query and string allowed") ;

        if ( cmdline.getNumPositional() == 1 && queryFilename != null )
            cmdline.cmdError("Either query string or query file - not both") ;

        if ( queryFilename == null ) {
            // One positional argument.
            String qs = cmdline.getPositionalArg(0) ;
            if ( cmdline.matchesIndirect(qs) )
                querySyntax = Syntax.guessQueryFileSyntax(qs, defaultQuerySyntax) ;

            queryString = cmdline.indirect(qs) ;
        }
        if ( planFilename == null ) {
            // Two positional argument.
            String ps = cmdline.getPositionalArg(1) ;
            planString = cmdline.indirect(ps) ;
        }

        // Set syntax
        if ( cmdline.contains(querySyntaxDecl) ) {
            // short name
            String s = cmdline.getValue(querySyntaxDecl) ;
            Syntax syn = Syntax.lookup(s) ;
            if ( syn == null )
                cmdline.cmdError("Unrecognized syntax: " + s) ;
            querySyntax = syn ;
        }
    }

    public Syntax getQuerySyntax() {
        return querySyntax ;
    }

    public Query getQuery() {
        if ( query != null )
            return query ;

        if ( queryFilename != null && queryString != null ) {
            System.err.println("Both query string and query file name given") ;
            throw new TerminationException(1) ;
        }

        if ( queryFilename == null && queryString == null ) {
            System.err.println("No query string and no query file name given") ;
            throw new TerminationException(1) ;
        }

        try {
            if ( queryFilename != null ) {
                if ( queryFilename.equals("-") ) {
                    try {
                        // Stderr?
                        queryString = FileUtils.readWholeFileAsUTF8(System.in) ;
                        // And drop into next if
                    } catch (IOException ex) {
                        throw new CmdException("Error reading stdin", ex) ;
                    }
                } else {
                    query = QueryFactory.read(queryFilename, baseURI, getQuerySyntax()) ;
                    return query ;
                }
            }

            query = QueryFactory.create(queryString, baseURI, getQuerySyntax()) ;
            return query ;
        } catch (ARQInternalErrorException intEx) {
            System.err.println(intEx.getMessage()) ;
            if ( intEx.getCause() != null ) {
                System.err.println("Cause:") ;
                intEx.getCause().printStackTrace(System.err) ;
                System.err.println() ;
            }
            intEx.printStackTrace(System.err) ;
            throw new TerminationException(99) ;
        }
        catch (JenaException ex) {
            System.err.println(ex.getMessage()) ;
            throw new TerminationException(2) ;
        } catch (Exception ex) {
            System.out.flush() ;
            ex.printStackTrace(System.err) ;
            throw new TerminationException(98) ;
        }
    }
    public Item getPlan() {
//        if ( planOp != null )
//            return planOp;

        if ( planFilename != null && planString != null ) {
            System.err.println("Both plan string and plan file name given") ;
            throw new TerminationException(1) ;
        }

        if ( planFilename == null && planString == null ) {
            System.err.println("No plan string and no plan file name given") ;
            throw new TerminationException(1) ;
        }

        try {
            Item item = PlanExecParser.parse(planString);
//            if ( planFilename != null ) {
//                if ( planFilename.equals("-") ) {
//                    try {
//                        // Stderr?
//                        planString = FileUtils.readWholeFileAsUTF8(System.in) ;
//                        // And drop into next if
//                    } catch (IOException ex) {
//                        throw new CmdException("Error reading stdin", ex) ;
//                    }
//                } else {
//                    Item item = PlanExecParser.parse(planString);
////                    plan = planFactory.read(planFilename, baseURI, getQuerySyntax()) ;
//                    return item ;
//                }
//            }
            //TOdo esto es para correr solamente. fix
            return Item.createList();
//            plan = planFactory.create(planString, baseURI, getplanSyntax()) ;
//            return plan ;
        } catch (ARQInternalErrorException intEx) {
            System.err.println(intEx.getMessage()) ;
            if ( intEx.getCause() != null ) {
                System.err.println("Cause:") ;
                intEx.getCause().printStackTrace(System.err) ;
                System.err.println() ;
            }
            intEx.printStackTrace(System.err) ;
            throw new TerminationException(99) ;
        }
        catch (JenaException ex) {
            System.err.println(ex.getMessage()) ;
            throw new TerminationException(2) ;
        } catch (Exception ex) {
            System.out.flush() ;
            ex.printStackTrace(System.err) ;
            throw new TerminationException(98) ;
        }
    }
}

