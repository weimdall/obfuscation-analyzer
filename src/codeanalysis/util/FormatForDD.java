/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.util;

import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.reader.*;
import org.polymtl.codeanalysis.visitors.*;
import org.polymtl.codeanalysis.writer.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class FormatForDD {
    private static CommandLine cmdline = null;
    private static final Logger LOGGER = CustomLogger.getLogger(FormatForDD.class.getName());
    public static void main(String[] args) {
        Option option_file = Option.builder("f")
                .longOpt("filelist")
                .required(true)
                .hasArg(true)
                .desc("Filelist")
                .build();
        Option option_input = Option.builder("i")
                .longOpt("input")
                .required(true)
                .hasArg(true)
                .desc("Input folder")
                .build();
        Option option_output_dd = Option.builder("odd")
                .longOpt("output-dd")
                .required(true)
                .hasArg(true)
                .desc("Output folder cfgfordd")
                .build();
        Option option_output_cfg = Option.builder("ocfg")
                .longOpt("output-cfg")
                .required(true)
                .hasArg(true)
                .desc("Output folder cfg")
                .build();
        Option option_debug = Option.builder("d")
                .longOpt("debug")
                .required(false)
                .hasArg(false)
                .desc("Show debug log")
                .build();


        Options options = new Options();
        options.addOption(option_file);
        options.addOption(option_input);
        options.addOption(option_output_dd);
        options.addOption(option_output_cfg);
        options.addOption(option_debug);

        CommandLineParser parser = new DefaultParser();
        try {
            cmdline = parser.parse(options, args);
        } catch (ParseException exp) {
            System.err.println("Wrong arguments: " + exp.getMessage());
            (new HelpFormatter()).printHelp("FormatForDD -f <filelist> -i <source_folder> -odd <output_folder> -ocfg <output_folder>", options);
            System.exit(1);
            return;
        }

        List<String> files           = null;
        try {
            LOGGER.info("---- Reading list : " + cmdline.getOptionValue("filelist") + "----");
            files = loadFileList(new File(cmdline.getOptionValue("filelist")), cmdline.getOptionValue("input") , ".ast.json.gz");
        } catch (IOException e) {
            LOGGER.severe("Cannot read file list");
            e.printStackTrace();
            System.exit(1);
        }

        LOGGER.info("Found " + files.size() + " CFGs");

        for(String file : files) {
            LOGGER.info("---- Reading " + file + "----");
            try {
                String kitName = file.substring(2, file.indexOf("/", 3));

                File inFile = Paths.get(cmdline.getOptionValue("input"), file).toFile();
                String fileCfg = file.replace(".ast.json.gz", ".cfg.json").replace(".ast.json", ".cfg.json");
                File outFileDd = Paths.get(cmdline.getOptionValue("output-dd"), fileCfg).toFile();
                File outFileCfg = Paths.get(cmdline.getOptionValue("output-cfg"),  fileCfg).toFile();
                outFileDd.getAbsoluteFile().getParentFile().mkdirs();
                outFileCfg.getAbsoluteFile().getParentFile().mkdirs();

                List<String> include_files = new ArrayList<>();
                listIncludedFiles(inFile.toString(), include_files);

                // Copy primal file
                writeCfgFromAst(inFile, outFileCfg, outFileDd);
                LOGGER.info("Writing CFG of " + fileCfg);
                // Copy referred files
                for(String include_file : include_files) {
                    File parent = Paths.get(fileCfg).toFile().getParentFile();
                    inFile = Paths.get(cmdline.getOptionValue("input"), parent.toString(), include_file+".ast.json.gz").toFile();
                    if(!inFile.exists() || inFile.isDirectory()) {
                        LOGGER.warning("No AST found for " + parent.toString()+"/"+include_file);
                        continue;
                    }
                    outFileDd = Paths.get(cmdline.getOptionValue("output-dd"), parent.toString(), include_file+".cfg.json").toFile();
                    outFileDd.getAbsoluteFile().getParentFile().mkdirs();
                    outFileCfg = Paths.get(cmdline.getOptionValue("output-cfg"), parent.toString(), include_file+".cfg.json").toFile();
                    outFileCfg.getAbsoluteFile().getParentFile().mkdirs();
                    writeCfgFromAst(inFile, outFileCfg, outFileDd);
                    LOGGER.info("Writing CFG of " + parent.toString()+"/"+include_file);
                }

            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.severe("Failed - " + e.getMessage());
            }
        }
        try {
            File dir = new File(cmdline.getOptionValue("output-dd"));
            for(File kit : dir.listFiles()) {
                List<String> cfgs = new ArrayList<>();
                walk(kit.getPath(), cfgs);
                FileWriter table = new FileWriter(Paths.get(kit.getAbsolutePath(), "chunkTable.json").toFile());
                boolean firstline=true;

                table.write("[\n");
                for(int i = 0 ; i < cfgs.size() ; i++) {
                    if(firstline) firstline = false;
                    else table.write(",\n");

                    String processed_name = cfgs.get(i);
                    processed_name = processed_name.replace(kit.getPath(), ".");

                    JSONArray array = new JSONArray();
                    array.put("chunk_table").put(i).put(processed_name);

                    table.write("  " + array.toString());
                }
                table.write("\n]\n");
                table.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.severe("Failed - " + e.getMessage());
            System.exit(1);
        }

    }

    private static void walk( String path, List<String> filelist ) {
        File root = new File( path );
        File[] list = root.listFiles();
        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() )
                walk( f.getPath(), filelist );
            else
                filelist.add(f.getPath());
        }
    }

    private static void listIncludedFiles(String inFile, List<String> include_list) throws IOException {
        ASTJsonReader reader = new ASTJsonReader();
        AST ast = reader.read(inFile);
        for(Integer nodeId : ast.getTypeTable().keySet()) {
            if(Arrays.asList("IncludeStatement", "IncludeOnceStatement", "RequireStatement", "RequireOnceStatement").contains(ast.getType(nodeId))) {
                if (Arrays.asList("StringLiteral", "StringExpression").contains(ast.getType(ast.getChildren(nodeId).get(0)))) {
                    String file = ast.getImage(ast.getChildren(nodeId).get(0));
                    try {
                        listIncludedFiles(file, include_list); //TODO: File doesn't exist, has to be relative to parent
                    } catch (IOException e) {
                        LOGGER.severe("Cannot read included file " + file);
                    }
                    include_list.add(file);
                }
                else
                    LOGGER.warning("Include expr is : " + ast.getType(ast.getChildren(nodeId).get(0)));
            }
        }
    }

    private static void writeCfgFromAst(File astFile, File cfgFile, File cfgDDFile) throws Exception {
        ASTJsonReader reader = new ASTJsonReader();
        AST ast = reader.read(astFile.toString());
        ASTtoCFG cfgTrans = new ASTtoCFG(ast);
        CFG cfg = cfgTrans.visit();
        Printer printer = new CFGJsonPrint(cfg, cfgFile);
        printer.print();
        CFGtoCFGforDD ddTrans = new CFGtoCFGforDD(cfg);
        CFGforDD cfgdd = ddTrans.visit();
        printer = new CFGforDDJsonPrint(cfgdd, cfgDDFile);
        printer.print();
    }

    private static List<String> loadFileList(File path, String srcFolder, String extension) throws IOException {
        List<String> files = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#") && line.length() > 0) {
                    File file = Paths.get(srcFolder, line).toFile();
                    if (file.exists() && file.isFile())
                        files.add(line);
                    else {
                        if(!line.endsWith(extension))
                            file = Paths.get(srcFolder, line+extension).toFile();
                        if (file.exists() && file.isFile())
                            files.add(line+extension);
                        else
                            LOGGER.warning("File not found: " + line);
                    }
                }
            }
        }
        return files;
    }
}
