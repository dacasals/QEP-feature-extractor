/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.utfsm.apache.cmds.arq.cmdline;

import jena.cmd.ArgDecl;
import jena.cmd.CmdArgModule;
import jena.cmd.CmdGeneral;
import jena.cmd.ModBase;
import liquibase.util.csv.CSVReader;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ModCsvQueriesIn extends ModBase {
    protected final ArgDecl queriesFileDecl   = new ArgDecl(ArgDecl.HasValue, "queriesFile", "file") ;
    protected final ArgDecl queryColumnDecl   = new ArgDecl(ArgDecl.HasValue, "queryColumn", "col") ;
    protected final ArgDecl idColumnDecl   = new ArgDecl(ArgDecl.HasValue, "idColumn", "icol") ;
    protected final ArgDecl inputDelimiterDecl   = new ArgDecl(ArgDecl.HasValue, "delimiter", "del") ;
    protected final ArgDecl querySyntaxDecl = new ArgDecl(ArgDecl.HasValue, "syntax", "syn", "in") ;

    private Syntax          defaultQuerySyntax;
    private Syntax          querySyntax;
    private String          queriesFilename   = null ;
    private int             queryColumnVal   = 0 ;
    private int             idColumnVal   = -1 ;
    private char            inputDelimiterChar;

    public ModCsvQueriesIn(Syntax defaultSyntax) {
        defaultQuerySyntax = defaultSyntax ;
        querySyntax = defaultSyntax ;
    }

    @Override
    public void registerWith(CmdGeneral cmdLine) {
        cmdLine.getUsage().startCategory("Query") ;
        cmdLine.add(queriesFileDecl,   "--queriesFile, --file",  "File containing queries") ;
        cmdLine.add(queryColumnDecl,   "--queryColumn, --col",  "Col containing a query") ;
        cmdLine.add(idColumnDecl,   "--idyColumn, --icol",  "Col containing a query id") ;

        cmdLine.add(inputDelimiterDecl,   "--delimiter, --dcol",  "delimiter char") ;
        cmdLine.add(querySyntaxDecl, "--syntax, --in",   "Syntax of the query") ;
    }

    @Override
    public void processArgs(CmdArgModule cmdline) throws IllegalArgumentException {
        if ( cmdline.contains(queriesFileDecl) ) {
            queriesFilename = cmdline.getValue(queriesFileDecl) ;
            querySyntax = Syntax.guessQueryFileSyntax(queriesFilename, defaultQuerySyntax) ;
        }

        if ( cmdline.getNumPositional() == 0 && queriesFilename == null )
            cmdline.cmdError("No query string or query file") ;

        if ( cmdline.getNumPositional() > 1 )
            cmdline.cmdError("Only one query string allowed") ;

        if ( cmdline.getNumPositional() == 1 && queriesFilename != null )
            cmdline.cmdError("Either query string or query file - not both") ;

//        if ( queriesFilename == null ) {
//            // One positional argument.
//            String qs = cmdline.getPositionalArg(0) ;
//            if ( cmdline.matchesIndirect(qs) )
//                querySyntax = Syntax.guessQueryFileSyntax(qs, defaultQuerySyntax) ;
//
//            queryString = cmdline.indirect(qs) ;
//        }

        // Set syntax
        if ( cmdline.contains(querySyntaxDecl) ) {
            // short name
            String s = cmdline.getValue(querySyntaxDecl) ;
            Syntax syn = Syntax.lookup(s) ;
            if ( syn == null )
                cmdline.cmdError("Unrecognized syntax: " + s) ;
            querySyntax = syn ;
        }
        if ( cmdline.contains(inputDelimiterDecl) ) {
            // short name
            String s = cmdline.getValue(inputDelimiterDecl) ;
            if (s.isEmpty())
                cmdline.cmdError("Unrecognized delimiter: " + s) ;
            inputDelimiterChar = s.charAt(0) ;
        }
        if ( cmdline.contains(queryColumnDecl) ) {
            // short name
            String s = cmdline.getValue(queryColumnDecl) ;
            if (s.isEmpty())
                cmdline.cmdError("Unrecognized column: " + s) ;
            queryColumnVal = Integer.parseInt(s);
        }
        if ( cmdline.contains(idColumnDecl) ) {
            // short name
            String s = cmdline.getValue(idColumnDecl) ;
            if (!s.isEmpty())
                idColumnVal = Integer.parseInt(s);
        }
    }

    public Syntax getQuerySyntax() {
        return querySyntax ;
    }
    public HashMap<String, Query> readCsvFile(){
        System.out.println("Reading Queries..");
        boolean header = true;
        HashMap<String, Query> queries = new HashMap<>();
        try {
            InputStreamReader csv = new InputStreamReader(new FileInputStream(queriesFilename));
            CSVReader csvReader = new CSVReader (csv, inputDelimiterChar);
            String[] record;
            while ((record = csvReader.readNext()) != null) {
                if (header){
                    header = false;
                    continue;
                }
                String query = record[queryColumnVal];
                String id = "";
                if(idColumnVal >= 0)
                    id = record[idColumnVal];
                else
                    id = DigestUtils.md5Hex(query);

//                query = URLDecoder.decode(query, StandardCharsets.UTF_8.toString());
                
                try {
                    Query queryObj = QueryFactory.create(query, getQuerySyntax());

                    queries.put(id, queryObj);
                }
                catch (Exception exception){
                    exception.printStackTrace();
                    System.out.println("Error leyendo query: ".concat(record[queryColumnVal]));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Queries : ".concat(String.valueOf(queries.size())));
        return  queries;
    }
}
