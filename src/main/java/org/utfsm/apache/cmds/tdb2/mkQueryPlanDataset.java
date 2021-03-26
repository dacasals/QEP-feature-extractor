package org.utfsm.apache.cmds.tdb2;

import arq.cmdline.*;
import jena.cmd.ArgDecl;
import jena.cmd.CmdException;
import org.apache.jena.atlas.json.*;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.query.*;
import tdb2.cmdline.CmdTDB;
import tdb2.cmdline.ModTDBDataset;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class mkQueryPlanDataset extends CmdARQ
{
    protected final ArgDecl logFile    = new ArgDecl(ArgDecl.HasValue, "logFile") ;
    protected final ArgDecl outFile    = new ArgDecl(ArgDecl.HasValue, "outFile") ;

    protected int repeatCount = 1 ;
    protected int warmupCount = 0 ;
    protected boolean queryOptimization = true ;
    protected String logFileVal, outFileVal;

    public static void main (String... argv) {
        CmdTDB.init();


        new mkQueryPlanDataset(argv).mainRun() ;
    }

    public mkQueryPlanDataset(String[] argv)
    {
        super(argv);

        super.getUsage().startCategory("Control") ;
        super.add(logFile,  "--logFile", "logFile") ;
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
        return getCommandName() + " --logFile=<path> --logFile=<path>";
    }

    protected ModDataset setModDataset() {
        return new ModTDBDataset();
    }
    @Override
    protected void processModulesAndArgs()
    {
        super.processModulesAndArgs() ;

        if ( isVerbose() )
            ARQ.getContext().setTrue(ARQ.symLogExec) ;

        if ( hasArg(logFile) )
        {
            logFileVal = getValue(logFile) ;
        }else {
            cmdError("Not found logFile parameter");
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

        queryExec();
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
    protected void queryExec()
    {
        try (FileInputStream reader = new FileInputStream(logFileVal))
        {

            JsonValue json = JSON.parseAny(reader);
            JsonArray array = json.getAsArray();
            ArrayList<HashMap<String,Object>> queriesVal = new ArrayList<>();

            HashMap<String,Object> currentMap = null;
            for (JsonValue current : array) {
                String loggerName = current.getAsObject().get("loggerName").getAsString().value();
                if ("org.utfsm.apache.cmds.tdb2.tdbqueryplan".equals(loggerName)) {
                    if (currentMap != null &&
                            currentMap.containsKey("query") &&
                            currentMap.containsKey("tdb") &&
                            currentMap.containsKey("execute")
                    ) {
                        queriesVal.add(currentMap);
                    }
                    currentMap = new HashMap<>();
                    currentMap.put("id", current.getAsObject().get("message").getAsString().value());
                } else if ("org.apache.jena.arq.exec".equals(loggerName)) {
                    String[] values = ((JsonString) current.getAsObject().get("message")).value().split("\n");
                    switch (values[0].toLowerCase()) {
                        case "query": {
                            values[0] = "";
                            currentMap.put("query", String.join(" ", values));
                            break;
                        }
                        case "algebra": {
                            values[0] = "";
                            currentMap.put("algebra", String.join(" ", values));
                            break;
                        }
                        case "tdb": {
                            values[0] = "";
                            if (!currentMap.containsKey("tdb")) {
                                ArrayList<String> tdbs = new ArrayList<>();
                                currentMap.put("tdb", tdbs);
                            }
                            String text = String.join(" ", values);
                            ArrayList<String> currTdbVal = (ArrayList<String>) currentMap.get("tdb");
                            currTdbVal.add(text);
                            currentMap.put("tdb", currTdbVal);
                            break;
                        }
                        case "execute": {
                            values[0] = "";
                            if (!currentMap.containsKey("execute")) {
                                ArrayList<String> tdbs = new ArrayList<>();
                                currentMap.put("execute", tdbs);
                            }
                            String text = String.join(" ", values);
                            ArrayList<String> currTdbVal = (ArrayList<String>) currentMap.get("execute");
                            currTdbVal.add(text);
                            currentMap.put("execute", currTdbVal);
                            break;
                        }
                        default:
                            Log.warn(mkQueryPlanDataset.class, "Unexpected value: " + values[0].toLowerCase());
                    }
                } else if ("EXECUTION_TREE".equals(loggerName)) {
                    String[] values = ((JsonString) current.getAsObject().get("message")).value().split("\n");
                    if (!currentMap.containsKey("execution_tree")) {
                        ArrayList<String> bgps = new ArrayList<>();
                        currentMap.put("execution_tree", bgps);
                    }
                    String text = String.join(" ", values);
                    ArrayList<String> currTdbVal = (ArrayList<String>) currentMap.get("execution_tree");
                    currTdbVal.add(text);
                    currentMap.put("execution_tree", currTdbVal);
                }
            }
            Logger.getLogger("COMMAND_NEO").info("Outputing to csv");
            FileOutputStream output = new FileOutputStream(new File(outFileVal));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(output));
            String delimiterCol = "ᶶ";
            String delimiterColVals = "ᶷ";
            System.out.println("Queries : ".concat(String.valueOf(queriesVal.size())));
            StringBuilder sb = new StringBuilder();
            for (HashMap<String, Object> stringObjectHashMap : queriesVal) {
                sb.append(((String) stringObjectHashMap.get("id")).concat(delimiterCol));
                sb.append(((String) stringObjectHashMap.get("query")).concat(delimiterCol));
                sb.append(String.join(delimiterColVals, ((ArrayList<String>) stringObjectHashMap.get("tdb"))).concat(delimiterCol));
                sb.append(String.join(delimiterColVals, ((ArrayList<String>) stringObjectHashMap.get("execute"))).concat(delimiterCol));
                if (stringObjectHashMap.containsKey("execution_tree")) {
                    sb.append(String.join(delimiterColVals, ((ArrayList<String>) stringObjectHashMap.get("execution_tree"))).concat(delimiterCol));
                }
                sb.append("\n");
            }
            bw.write(sb.toString());
            bw.close();
        }
        catch (Exception ex) {
            throw new CmdException("Exception", ex) ;
        }
    }

}
