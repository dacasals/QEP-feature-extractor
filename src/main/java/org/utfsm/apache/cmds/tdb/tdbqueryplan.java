package org.utfsm.apache.cmds.tdb;

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
import org.apache.jena.sparql.engine.Plan;
import org.apache.jena.sparql.engine.QueryExecutionBase;
import org.apache.jena.sparql.mgt.Explain;
import org.apache.jena.sparql.resultset.ResultSetException;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.tdb.sys.SystemTDB;
import org.utfsm.apache.cmds.arq.cmdline.ModCsvQueriesIn;
import org.utfsm.jena.tdb.solver.OpExecutorTDBNeo;
import org.utfsm.jena.tdb2.solver.OpExecutorTDB2Neo;
import tdb.cmdline.ModTDBDataset;
import tdb2.cmdline.CmdTDB;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;


public class tdbqueryplan extends CmdARQ
{
    private ArgDecl argRepeat   = new ArgDecl(ArgDecl.HasValue, "repeat") ;
    private ArgDecl argExplain  = new ArgDecl(ArgDecl.NoValue, "explain") ;
    private ArgDecl argOptimize = new ArgDecl(ArgDecl.HasValue, "opt", "optimize") ;
    protected final ArgDecl outFile    = new ArgDecl(ArgDecl.HasValue, "outFile") ;

    protected final ArgDecl queriesFile    = new ArgDecl(ArgDecl.HasValue, "queriesFile") ;
    protected final ArgDecl queryColumn    = new ArgDecl(ArgDecl.HasValue, "queryCol") ;
    public  static HashMap<String, HashMap<String, ArrayList<String>>> registros = new HashMap<>();
    public  static String lastReg = "";
    protected int repeatCount = 1 ;
    protected int warmupCount = 0 ;
    protected boolean queryOptimization = true ;
    protected String outFileVal;
    protected ModTime       modTime =     new ModTime() ;
    protected ModDataset    modDataset;
    protected ModCsvQueriesIn modQueries =  new ModCsvQueriesIn(Syntax.syntaxARQ);
    protected ModResultsOut modResults =  new ModResultsOut() ;
    protected ModEngine     modEngine =   new ModEngine() ;

    public static void main (String... argv) {
        CmdTDB.init();
        new tdbqueryplan(argv).mainRun() ;
    }

    public tdbqueryplan(String[] argv)
    {
        super(argv) ;
        modDataset = setModDataset() ;

        super.addModule(modQueries) ;
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
        super.add(outFile, "--outFile", "outFile") ;

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
        super.processModulesAndArgs();
        if ( contains(argRepeat)) {
            String[] x = getValue(argRepeat).split(",") ;
            if ( x.length == 1 ) {
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
        if ( hasArg(outFile) )
        {
            outFileVal = getValue(outFile) ;
        }
        else {
            cmdError("Not found outFile parameter");
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
    protected Dataset dealWithNoDataset(Query query)  {
        if ( query.hasDatasetDescription() )
            return null;
        return DatasetFactory.createTxnMem();
    }

    protected long totalTime = 0 ;
    protected void queryExec(boolean timed, ResultsFormat fmt)
    {
        try {
            HashMap<String, Query> queries = modQueries.readCsvFile();

            SystemTDB.setOpExecutorFactory(OpExecutorTDBNeo.OpExecFactoryTDB);
            if ( isQuiet() )
                LogCtl.setError(SysRIOT.riotLoggerName) ;
            Dataset dataset = modDataset.getDataset();
            Object[] ids = queries.keySet().toArray();

            for (Object index : ids) {
                String key = String.valueOf(index);
                System.out.println(key);
                Query query = queries.get(key);
                Log.info(tdbqueryplan.class, key);
                HashMap<String,ArrayList<String>> qData = new HashMap<>();
                //Only one for id;
                ArrayList<String> queryArr = new ArrayList<>();
                queryArr.add(query.toString().replaceAll("\n", " "));
                qData.put("query", queryArr);
                lastReg = key;
                registros.put(lastReg, qData);

                // Check there is a dataset. See dealWithNoDataset(query).
                // The default policy is to create an empty one - convenience for VALUES and BIND providing the data.
                if (dataset == null && !query.hasDatasetDescription()) {
                    System.err.println("Dataset not specified in query nor provided on command line.");
                    throw new TerminationException(1);
                }
                    modTime.startTimer();

                    try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
//                        qe.getContext().set(ARQ.optimization, false);
//                        qe.getContext().set(ARQ.optReorderBGP, false);
                        try {
//                            qe.getContext().get(Symbol.create(""))
                            Plan plan = ((QueryExecutionBase) qe).getPlan();
//                            plan.getOp()
//                            QueryExecUtils.executeQuery(query, qe, fmt);
                        } catch (QueryCancelledException ex) {
                            System.out.flush();
                            System.err.println("Query timed out");
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        long time = modTime.endTimer();
                        if (timed) {
                            totalTime += time;
                            System.err.println("Time: " + modTime.timeStr(time) + " sec");
                        }
                    } catch (ResultSetException ex) {
                        System.err.println(ex.getMessage());
                        ex.printStackTrace(System.err);
                    } catch (QueryException qEx) {
                        // System.err.println(qEx.getMessage()) ;
                        throw new CmdException("Query Exeception", qEx);
                    }
            }
            outputQPlanDataset(registros);

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

    private void outputQPlanDataset(HashMap<String, HashMap<String, ArrayList<String>>> registros) {
        Logger.getLogger("COMMAND_NEO").info("Outputing to csv");
        System.out.println("Queries : ".concat(String.valueOf(registros.size())));
        FileOutputStream output;
        try {
            output = new FileOutputStream(new File(outFileVal));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(output));
            String delimiterCol = "ᶶ";
            String delimiterColVals = "ᶷ";
            StringBuilder sb = new StringBuilder();
            for (String key : registros.keySet()) {
                HashMap<String, ArrayList<String>> registro = registros.get(key);
                sb.append(key.concat(delimiterCol));
                sb.append(registro.get("query").get(0).concat(delimiterCol));
                if (registro.containsKey("execution_tree")) {
                    sb.append(String.join(delimiterColVals, ((ArrayList<String>) registro.get("execution_tree"))));
                }
                sb.append("\n");
            }
            bw.write(sb.toString());
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
