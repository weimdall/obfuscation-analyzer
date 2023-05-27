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
import org.polymtl.codeanalysis.visitors.CFGtoCFGforDD;
import org.polymtl.codeanalysis.writer.*;

import java.io.*;

public class CFGTranslator {
    private static CommandLine cmdline = null;
    public static void main(String[] args) {
        Option option_file = Option.builder("f")
                .longOpt("filename")
                .required(false)
                .hasArg(true)
                .desc("Filename (- for stdin)")
                .build();
        Option option_debug = Option.builder("d")
                .longOpt("debug")
                .required(false)
                .hasArg(false)
                .desc("Show debug log")
                .build();


        Options options = new Options();
        options.addOption(option_file);
        options.addOption(option_debug);

        CommandLineParser parser = new DefaultParser();
        try {
            cmdline = parser.parse(options, args);
            if(!cmdline.hasOption("type") && (!cmdline.hasOption("filename") || cmdline.getOptionValue("filename").equals("-")))
                throw new ParseException("Type required when reading from stdin");
        }
        catch (ParseException exp) {
            System.err.println("Wrong arguments: " + exp.getMessage());
            (new HelpFormatter()).printHelp("java -jar CFGTranslator.jar [--filename <json_file>] [--type <type>]", options);
            System.exit(1);
            return;
        }

        try {
            BufferedReader reader;
            if(!cmdline.hasOption("filename") || cmdline.getOptionValue("filename").equals("-"))
                reader = new BufferedReader(new InputStreamReader(System.in));
            else
                reader = new BufferedReader(new FileReader(cmdline.getOptionValue("filename")));

            CFG graph = null;
            Printer printer = null;
            CFGJsonReader reader_cfg = new CFGJsonReader();
            graph = reader_cfg.read(reader);
            CFGtoCFGforDD translator = new CFGtoCFGforDD(graph);
            CFGforDD newGraph = translator.visit();
            printer = new CFGforDDJsonPrint(newGraph, new PrintWriter(System.out));
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
