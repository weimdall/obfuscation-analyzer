/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.util;

import org.apache.commons.cli.*;
import org.json.JSONException;
import org.polymtl.codeanalysis.exceptions.ASTJsonException;
import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.reader.*;
import org.polymtl.codeanalysis.writer.*;

import java.io.*;

public class JSONtoDOT {
    private static CommandLine cmdline = null;
    public static void main(String[] args) {
        Option option_file = Option.builder("f")
                .longOpt("filename")
                .required(false)
                .hasArg(true)
                .desc("Filename (- for stdin)")
                .build();
        Option option_type = Option.builder("t")
                .longOpt("type")
                .required(false)
                .hasArg(true)
                .desc("Graph type (parsetree, ast, astdyn, cfg, dom or cfgfordd)")
                .build();
        Option option_debug = Option.builder("d")
                .longOpt("debug")
                .required(false)
                .hasArg(false)
                .desc("Show debug log")
                .build();


        Options options = new Options();
        options.addOption(option_file);
        options.addOption(option_type);
        options.addOption(option_debug);

        CommandLineParser parser = new DefaultParser();
        try {
            cmdline = parser.parse(options, args);
            if(!cmdline.hasOption("type") && (!cmdline.hasOption("filename") || cmdline.getOptionValue("filename").equals("-")))
                throw new ParseException("Type required when reading from stdin");
        }
        catch (ParseException exp) {
            System.err.println("Wrong arguments: " + exp.getMessage());
            (new HelpFormatter()).printHelp("java -jar JSONtoDOT.jar [--filename <json_file>] [--type <type>]", options);
            System.exit(1);
            return;
        }

        try {
            BufferedReader reader;
            if(!cmdline.hasOption("filename") || cmdline.getOptionValue("filename").equals("-"))
                reader = new BufferedReader(new InputStreamReader(System.in));
            else
                reader = new BufferedReader(new FileReader(cmdline.getOptionValue("filename")));

            String graph_type = null;
            if(cmdline.hasOption("type"))
                graph_type = cmdline.getOptionValue("type");
            //Guess type if not yet resolved
            if(graph_type == null && cmdline.hasOption("filename")) {
                if(cmdline.getOptionValue("filename").endsWith(".parsetree.json"))
                    graph_type = "parsetree";
                else if(cmdline.getOptionValue("filename").endsWith(".compact.json") || cmdline.getOptionValue("filename").endsWith(".ast.json"))
                    graph_type = "ast";
                else if(cmdline.getOptionValue("filename").endsWith(".astdyn.json"))
                    graph_type = "astdyn";
                else if(cmdline.getOptionValue("filename").endsWith(".cfg.json"))
                    graph_type = "cfg";
                else if(cmdline.getOptionValue("filename").endsWith(".dom.json"))
                    graph_type = "dom";
                else if(cmdline.getOptionValue("filename").endsWith(".cfgfordd.json"))
                    graph_type = "cfgfordd";
            }

            if(graph_type == null)
                graph_type = "UNKNOWN";
            Graph graph = null;
            Printer printer = null;

            switch (graph_type) {
                case "parsetree":
                    ParseTreeJsonReader reader_pt = new ParseTreeJsonReader();
                    graph = reader_pt.read(reader);
                    printer = new ASTDotPrint((ParseTree) graph, new PrintWriter(System.out));
                    break;
                case "ast":
                    ASTJsonReader reader_ast = new ASTJsonReader();
                    graph = reader_ast.read(reader);
                    printer = new ASTDotPrint((AST) graph, new PrintWriter(System.out));
                    break;
                case "astdyn":
                    ASTDynamicJsonReader reader_astdyn = new ASTDynamicJsonReader();
                    graph = reader_astdyn.read(reader);
                    printer = new ASTDynamicDotPrint((ASTDynamic) graph, new PrintWriter(System.out));
                    break;
                case "cfg":
                    CFGJsonReader reader_cfg = new CFGJsonReader();
                    graph = reader_cfg.read(reader);
                    printer = new ASTDotPrint((CFG) graph, new PrintWriter(System.out));
                    break;
                case "dom":
                    DOMJsonReader reader_dom = new DOMJsonReader();
                    graph = reader_dom.read(reader);
                    printer = new ASTDotPrint((Dominator) graph, new PrintWriter(System.out));
                    break;
                case "cfgfordd":
                    CFGforDDJsonReader reader_cfgfordd = new CFGforDDJsonReader();
                    graph = reader_cfgfordd.read(reader);
                    printer = new CFGforDDDotPrint((CFGforDD) graph, new PrintWriter(System.out));
                    break;
                default:
                    System.err.println("Unknown graph type.\nPlease specify a valid type (parsetree, ast, astdyn, cfg or cfgfordd)");
                    System.exit(1);
            }
            printer.print();
        }
        catch (FileNotFoundException e) {
            System.err.println("ERROR: Input file not found.");
        }  catch (ASTJsonException e) {
            System.err.println("ERROR: Failed to build graph");
            debug(e);
        } catch (JSONException e) {
            System.err.println("ERROR: Failed to read JSON");
            debug(e);
        } catch (Exception e) {
            System.err.println("ERROR: Unhandled Exception.");
            debug(e);
        }

    }

    private static void debug(Exception e) {
        System.err.println("\t" + e.getMessage());
        if(cmdline.hasOption("debug")) {
            System.err.println("----- DEBUG LOG -----");
            e.printStackTrace();
            System.err.println("----- DEBUG LOG -----");
        }
    }
}
