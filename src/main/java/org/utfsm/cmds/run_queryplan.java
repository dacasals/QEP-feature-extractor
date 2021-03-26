package org.utfsm.cmds;

import arq.cmdline.*;
import jena.cmd.ArgDecl;
import jena.cmd.CmdException;
import jena.cmd.TerminationException;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.query.*;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.core.TransactionalNull;
import org.apache.jena.sparql.engine.optimizer.StatsMatcher;
import org.apache.jena.sparql.mgt.Explain;
import org.apache.jena.sparql.resultset.ResultSetException;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.sparql.sse.Item;
import org.apache.jena.sparql.util.QueryExecUtils;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.sys.SystemTDB;
import org.utfsm.apache.cmds.arq.cmdline.ModCsvQueriesIn;
import org.utfsm.apache.cmds.arq.cmdline.ModQueryPlanIn;
import org.utfsm.jena.arq.sparql.engine.optimizer.reorder.ReorderWeighted;
import tdb2.cmdline.CmdTDB;
import tdb2.cmdline.ModTDBDataset;

import java.util.HashMap;


/**
 * Permite correr una query with plan
 */
public class run_queryplan extends CmdARQ
{
    private ArgDecl argRepeat   = new ArgDecl(ArgDecl.HasValue, "repeat") ;
    private ArgDecl argExplain  = new ArgDecl(ArgDecl.NoValue, "explain") ;
    private ArgDecl argOptimize = new ArgDecl(ArgDecl.HasValue, "opt", "optimize") ;
    protected final ArgDecl queriesFile    = new ArgDecl(ArgDecl.HasValue, "queriesFile") ;
    protected final ArgDecl queryColumn    = new ArgDecl(ArgDecl.HasValue, "queryCol") ;

    protected int repeatCount = 1 ;
    protected int warmupCount = 0 ;
    protected boolean queryOptimization = true ;

    protected ModTime       modTime =     new ModTime() ;
    protected ModDataset    modDataset;
    protected ModQueryPlanIn modQueryPlan =  new ModQueryPlanIn(Syntax.syntaxARQ);
    protected ModResultsOut modResults =  new ModResultsOut() ;
    protected ModEngine     modEngine =   new ModEngine() ;

    public static void main (String... argv) {
        CmdTDB.init();
        new run_queryplan(argv).mainRun() ;
    }

    public run_queryplan(String[] argv)
    {
        super(argv) ;
        modDataset = setModDataset() ;

        super.addModule(modQueryPlan) ;
        super.addModule(modResults) ;
        super.addModule(modDataset) ;
        super.addModule(modEngine) ;
        super.addModule(modTime) ;

        super.getUsage().startCategory("Control") ;
        super.add(argExplain,  "--explain", "Explain and log query execution") ;
        super.add(argRepeat,   "--repeat=N or N,M", "Do N times or N warmup and then M times (use for timing to overcome start up costs of Java)");
        super.add(argOptimize, "--optimize=", "Turn the query optimizer on or off (default: on)") ;
        super.add(queriesFile, "--queriesFile=", "Path to csv file containing queries.") ;
        super.add(queryColumn, "--queryColumn=", "queryColumn index on file") ;
    }

    /** Default syntax used when the syntax can not be determined from the command name or file extension
     *  The order of determination is:
     *  <ul>
     *  <li>Explicitly given --syntax</li>
     *  <li>File extension</li>
     *  <li>Command default</li>
     *  <li>System default</li>
     *  </ul>
     *
     */
    protected Syntax getDefaultSyntax()     { return Syntax.defaultQuerySyntax ; }


    @Override
    protected String getSummary() {
        return getCommandName() + " --loc=<path> --queriesFile=</path/to/file.csv> --queryColumn=<int>";
    }

    protected ModDataset setModDataset() {
        return new ModTDBDataset();
    }
    @Override
    protected void processModulesAndArgs()
    {
        super.processModulesAndArgs() ;
        if ( contains(argRepeat) )
        {
            String[] x = getValue(argRepeat).split(",") ;
            if ( x.length == 1 )
            {
                try { repeatCount = Integer.parseInt(x[0]) ; }
                catch (NumberFormatException ex)
                { throw new CmdException("Can't parse "+x[0]+" in arg "+getValue(argRepeat)+" as an integer") ; }
            }
            else if ( x.length == 2 ) {
                try { warmupCount = Integer.parseInt(x[0]) ; }
                catch (NumberFormatException ex)
                { throw new CmdException("Can't parse "+x[0]+" in arg "+getValue(argRepeat)+" as an integer") ; }
                try { repeatCount = Integer.parseInt(x[1]) ; }
                catch (NumberFormatException ex)
                { throw new CmdException("Can't parse "+x[1]+" in arg "+getValue(argRepeat)+" as an integer") ; }
            } else
                throw new CmdException("Wrong format for repeat count: "+getValue(argRepeat)) ;
        }
        if ( isVerbose() )
            ARQ.getContext().setTrue(ARQ.symLogExec) ;

        if ( hasArg(argExplain) )
            ARQ.setExecutionLogging(Explain.InfoLevel.ALL) ;

        if ( hasArg(argOptimize) )
        {
            String x1 = getValue(argOptimize) ;
            if ( hasValueOfTrue(argOptimize) || x1.equalsIgnoreCase("on") || x1.equalsIgnoreCase("yes") )
                queryOptimization = true ;
            else if ( hasValueOfFalse(argOptimize) || x1.equalsIgnoreCase("off") || x1.equalsIgnoreCase("no") )
                queryOptimization = false ;
            else throw new CmdException("Optimization flag must be true/false/on/off/yes/no. Found: "+getValue(argOptimize)) ;
        }
    }

    @Override
    protected void exec()
    {
        if ( ! queryOptimization )
            ARQ.getContext().setFalse(ARQ.optimization) ;
        if ( cmdStrictMode )
            ARQ.getContext().setFalse(ARQ.optimization) ;

        // Warm up.
        for ( int i = 0 ; i < warmupCount ; i++ )
        {
            queryExec(false, ResultsFormat.FMT_NONE) ;
        }

        for ( int i = 0 ; i < repeatCount ; i++ )
            queryExec(modTime.timingEnabled(),  modResults.getResultsFormat()) ;

        if ( modTime.timingEnabled() && repeatCount > 1 )
        {
            long avg = totalTime/repeatCount ;
            String avgStr = modTime.timeStr(avg) ;
            System.err.println("Total time: "+modTime.timeStr(totalTime)+" sec for repeat count of "+repeatCount+ " : average: "+avgStr) ;
        }
    }

    @Override
    protected String getCommandName() { return Lib.className(this) ; }


    // Policy for no command line dataset. null means "whatever" (use FROM)
    protected Dataset dealWithNoDataset(Query query)  {
        if ( query.hasDatasetDescription() )
            return null;
        return DatasetFactory.createTxnMem();
        //throw new CmdException("No dataset provided") ;
    }

    protected long totalTime = 0 ;
    protected void queryExec(boolean timed, ResultsFormat fmt)
    {
        try {
            Item plan = modQueryPlan.getPlan();
            //Todo implement the rest.
//            plan.toString();
//            testMethod();
//            if ( isQuiet() )
//                LogCtl.setError(SysRIOT.riotLoggerName) ;
//            Dataset dataset = modDataset.getDataset();
//            Object[] ids = queries.keySet().toArray();
//
//            testMethod();
//
//            for (Object index : ids) {
//                String key = String.valueOf(index);
//
//                Query query = queries.get(key);
//                Log.info(run_queryplan.class, key);
//                // Check there is a dataset. See dealWithNoDataset(query).
//                // The default policy is to create an empty one - convenience for VALUES and BIND providing the data.
//                if (dataset == null && !query.hasDatasetDescription()) {
//                    System.err.println("Dataset not specified in query nor provided on command line.");
//                    throw new TerminationException(1);
//                }
//                Transactional transactional = (dataset != null && dataset.supportsTransactions()) ? dataset : new TransactionalNull();
//                Txn.executeRead(transactional, () -> {
//                    modTime.startTimer();
//                    try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
////                        qe.getContext()
//                        try {
//                            QueryExecUtils.executeQuery(query, qe, fmt);
//                        } catch (QueryCancelledException ex) {
//                            System.out.flush();
//                            System.err.println("Query timed out");
//                        }
//                        long time = modTime.endTimer();
//                        if (timed) {
//                            totalTime += time;
//                            System.err.println("Time: " + modTime.timeStr(time) + " sec");
//                        }
//                    } catch (ResultSetException ex) {
//                        System.err.println(ex.getMessage());
//                        ex.printStackTrace(System.err);
//                    } catch (QueryException qEx) {
//                        // System.err.println(qEx.getMessage()) ;
//                        throw new CmdException("Query Exeception", qEx);
//                    }
//                });
//            }

        }
        catch (ARQInternalErrorException intEx) {
            System.err.println(intEx.getMessage()) ;
            if ( intEx.getCause() != null )
            {
                System.err.println("Cause:") ;
                intEx.getCause().printStackTrace(System.err) ;
                System.err.println() ;
            }
            intEx.printStackTrace(System.err) ;
        }
        catch (JenaException | CmdException ex) { throw ex ; }
        catch (Exception ex) {
            throw new CmdException("Exception", ex) ;
        }
    }
    public void testMethod(){
        String locationStr = "/mnt/46d157e4-f3b2-4033-90d8-ed2b2b56139e/source/java/jena-3.15.0/jena-fuseki2/databases/dblpreal/Data-0001/stats.opt";
        final String dbPrefix     = "Data";
        final String SEP          = "-";
        final String startCount   = "0001";
        StatsMatcher statsMatcher = new StatsMatcher(locationStr);
        ReorderWeighted reorderWeighted = new ReorderWeighted(statsMatcher);
        SystemTDB.setDefaultReorderTransform(reorderWeighted);
//        String q = "SELECT  *\n" +
//                "WHERE\n" +
//                "  { ?x        <http://xmlns.com/foaf/0.1/page>  ?pag .\n" +
//                "    ?journal  a                     <http://swrc.ontoware.org/ontology#Journal> .\n" +
//                "    ?x        <http://swrc.ontoware.org/ontology#journal>  ?journal\n" +
//                "  }\n";
//
//        Query query = QueryFactory.create(q);
//        Dataset ds = TDB2Factory.connectDataset(location);
    }

}
