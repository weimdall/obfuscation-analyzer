/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis;

import org.apache.commons.cli.*;
import org.polymtl.codeanalysis.parser.ParseException;
import org.polymtl.codeanalysis.exceptions.*;
import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.reader.*;
import org.polymtl.codeanalysis.util.*;
import org.polymtl.codeanalysis.visitors.*;
import org.polymtl.codeanalysis.writer.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EvalResolver {
    private static final Logger LOGGER = CustomLogger.getLogger(EvalResolver.class.getName(), "eval_resolver");
    private static CommandLine cmdline = null;

    private static final EvalExtractor evalExtractor = new EvalExtractor();
    private static final ASTDynMatcher astDynMatcher = new ASTDynMatcher();
    private static final List<ASTDynamic> parametricMO = new ArrayList<>();
    private static final List<Integer> parametricMO_n = new ArrayList<>();
    private static final List<HashMap<Integer, Integer>> parametricMO_iden_n = new ArrayList<>();
    private static final List<ASTDynamic> identicalMO  = new ArrayList<>();
    private static final List<Integer> identicalMO_n  = new ArrayList<>();
    private static final List<Integer> payloadSizes  = new ArrayList<>();
    private static final HashMap<Integer, Integer> countPatternsFrag  = new HashMap<>();
    private static final HashMap<Integer, HashSet<String>> countPatternsKits  = new HashMap<>();
    private static int evalSum = 0;

    public static void main(String[] args) throws IOException {
        Option option_filelist = Option.builder("f")
                .longOpt("filelist")
                .required(true)
                .hasArg(true)
                .desc("Filelist")
                .build();
        Option option_inDirAst = Option.builder("iast")
                .longOpt("in-dir")
                .required(true)
                .hasArg(true)
                .desc("Input Directory for AST")
                .build();
        Option option_inDirDd = Option.builder("idd")
                .longOpt("in-dir-dd")
                .required(false)
                .hasArg(true)
                .desc("Input Directory for DD")
                .build();
        Option option_outDir = Option.builder("o")
                .longOpt("out-dir")
                .required(true)
                .hasArg(true)
                .desc("Output directory")
                .build();
        Option option_debug = Option.builder("d")
                .longOpt("debug")
                .required(false)
                .hasArg(false)
                .desc("Debug log")
                .build();

        Options options = new Options();
        options.addOption(option_filelist);
        options.addOption(option_inDirAst);
        options.addOption(option_inDirDd);
        options.addOption(option_outDir);
        options.addOption(option_debug);

        CommandLineParser parser = new DefaultParser();
        try {
            cmdline = parser.parse(options, args);
        }
        catch (org.apache.commons.cli.ParseException exp) { // Conflict with ParseException of polymtl
            System.err.println("Wrong arguments.  " + exp.getMessage());
            (new HelpFormatter()).printHelp("EvalResolver", options);
            System.exit(1);
            return;
        }


        final File file_list     = Paths.get(cmdline.getOptionValue("f")).toFile();
        final String inDirAst    = cmdline.getOptionValue("iast");
        final String inDirDd     = cmdline.getOptionValue("idd");
        final String outDir      = cmdline.getOptionValue("o");
        final boolean debug      = cmdline.hasOption("d");

        CustomLogger.setLevel((debug) ? Level.ALL : Level.CONFIG);

        List<String> files           = null;
        List<String>    patternFiles = null;
        try {
            LOGGER.info("---- Reading list : " + file_list + "----");
            files = loadFileList(file_list, inDirAst, ".ast.json.gz");
        } catch (IOException e) {
            LOGGER.severe("Cannot read file list");
            e.printStackTrace();
            System.exit(1);
        }

        ASTJsonReader astReader = new ASTJsonReader();
        List<String> fileListEval = new ArrayList<>();
        List<String> fileListErrorDataflow = new ArrayList<>();
        int evalFound = 0;
        int evalResolved = 0;
        int evalFailDataflow = 0;
        int evalFailParse = 0;
        int evalFailOther = 0;
        int fileFound = 0;
        int fileFailed = 0;


        for (String file : files) {
            fileFound += 1;
            AST ast = null;
            LOGGER.info("---- Reading " + file + "----");
            try {
                ast = astReader.read(inDirAst + file);
            } catch (Exception e) {
                LOGGER.severe("Failed to parse AST");
                fileFailed += 1;
            }
            PatternSubstitution resolver = new PatternSubstitution();
            resolver.visit(ast, file, inDirAst, inDirDd);

            if(resolver.isEvalFound()) {
                fileListEval.add(file);
                file = file.replace(".ast.json.gz", "").replace(".ast.json", "");
                // Print
                ASTDynamic astSub = resolver.getDynamicAst();
                try {
                    File out_file = new File(outDir + "/full/dot/" + file + ".astdyn.dot");
                    File parentDir = out_file.getAbsoluteFile().getParentFile();
                    parentDir.mkdirs();
                    Printer printer = new ASTDynamicDotPrint(astSub, out_file);
                    printer.print();

                    out_file = new File(outDir + "/full/json/" + file + ".astdyn.json");
                    parentDir = out_file.getAbsoluteFile().getParentFile();
                    parentDir.mkdirs();
                    printer = new ASTDynamicJsonPrint(astSub, out_file);
                    printer.print();
                } catch (Exception e) {
                    LOGGER.severe("Failed to print dynamic AST");
                }

                // Count
                evalFound++;
                if (resolver.getEvalErrors().size() != 0) {
                    boolean data = false;
                    boolean parse = false;
                    for (Exception e : resolver.getEvalErrors()) {
                        if (e instanceof ParseException) {
                            parse = true;
                            break;
                        }
                    }
                    for (Exception e : resolver.getEvalErrors()) {
                        if (e instanceof ASTDataflowException) {
                            data = true;
                            break;
                        }
                    }

                    if(parse)
                        evalFailParse++;
                    else if(data) {
                        evalFailDataflow++;
                        fileListErrorDataflow.add(file);
                    }
                    else
                        evalFailOther++;
                    //continue; // GO TO NEXT EVAL IF NOT RESOLVED - NO PARAM / IDENT / SAVE
                }
                else
                    evalResolved++;

                ClassifyAndStoreEvals(astSub, outDir, file);

            }
        }

        FileWriter evalListFile = new FileWriter(outDir + File.separator + "list.txt");
        fileListEval.forEach( (file) -> {
            try {
                evalListFile.write(file+"\n");
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.warning("Failed to write file list");
            }
        });
        evalListFile.close();

        FileWriter evalListDataFile = new FileWriter(outDir + File.separator + "list_data.txt");
        fileListErrorDataflow.forEach( (file) -> {
            try {
                evalListDataFile.write(file+"\n");
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.warning("Failed to write file data flow list");
            }
        });
        evalListDataFile.close();

        FileWriter reportFile   = new FileWriter(outDir + File.separator + "report.txt");
        List<String> report = new ArrayList<>();
        report.add("--------- Report ---------");
        report.add("File found : \t\t" + fileFound);
        report.add("File parse failed : \t" + fileFailed);
        report.add("\n");
        report.add("Eval found : \t\t" + evalFound);
        report.add("Eval resolved : \t" + evalResolved);
        report.add("Eval dataflow error : \t" + evalFailDataflow);
        report.add("Eval parsing error : \t" + evalFailParse);
        report.add("Eval other error : \t" + evalFailOther);
        report.add("\n");
        report.add("Total signatures : \t" + evalSum);
        report.add("Parametric class: \t" + parametricMO_n.size());
        report.add("Identical class: \t" + identicalMO_n.size());
        report.add("--------- Report ---------");

        report.forEach( (line) -> {
            try {
                reportFile.write(line+"\n");
                System.out.println(line);
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.warning("Failed to write report");
            }
        });
        reportFile.close();

        FileWriter dataClassFile   = new FileWriter(outDir + File.separator + "data_class.json");
        dataClassFile.write("{\n");
        dataClassFile.write("  \"payload_sizes\": [");
        for(int i = 0; i < payloadSizes.size() ; i++)
            dataClassFile.write(payloadSizes.get(i) + ((i == payloadSizes.size()-1) ? "" : ", "));
        dataClassFile.write("],\n");
        dataClassFile.write("  \"parametric\": [");
        for(int i = 0; i < parametricMO_n.size() ; i++)
            dataClassFile.write(parametricMO_n.get(i) + ((i == parametricMO_n.size()-1) ? "" : ", "));
        dataClassFile.write("],\n");
        dataClassFile.write("  \"identical\": [");
        for(int i = 0; i < identicalMO_n.size() ; i++)
            dataClassFile.write(identicalMO_n.get(i) + ((i == identicalMO_n.size()-1) ? "" : ", "));
        dataClassFile.write("],\n");
        dataClassFile.write("  \"parametric_identical\": [\n");
        for(int i = 0; i < parametricMO_iden_n.size() ; i++) {
            dataClassFile.write("    [");
            int s = 0;
            for(Integer j : parametricMO_iden_n.get(i).keySet()) {
                dataClassFile.write(parametricMO_iden_n.get(i).get(j) + ((s == parametricMO_iden_n.get(i).size() - 1) ? "" : ", "));
                s++;
            }
            dataClassFile.write("]" + ((i == parametricMO_iden_n.size() - 1) ? "\n" : ",\n"));
        }
        dataClassFile.write("  ]\n");
        dataClassFile.write("}");
        dataClassFile.close();


    }

    private static void ClassifyAndStoreEvals(ASTDynamic astSub, String outDir, String file) {
        evalExtractor.extract(astSub);
        for(int i = 0 ; i < evalExtractor.getEvalList().size() ; i++) {
            evalSum += 1;
            ASTDynamic extractedEval = evalExtractor.getEvalList().get(i);
            payloadSizes.add(evalExtractor.getPayloadSize(i));
            // Look for identical
            boolean match = false;
            int id_iden = -1;
            for(int j = 0; j < identicalMO.size() ; j++) {
                if(astDynMatcher.match(extractedEval, identicalMO.get(j)) == ASTDynMatcher.TYPE.IDENTICAL) {
                    match = true;
                    identicalMO_n.set(j, identicalMO_n.get(j)+1);
                    WriteAstDyn(outDir+"/MO/ident/"+j+"/", identicalMO_n.get(j).toString(), extractedEval);
                    id_iden = j;
                    break;
                }
            }
            if(!match) {
                ASTDynamic asttmp= new ASTDynamic(extractedEval);
                id_iden = identicalMO_n.size();
                identicalMO.add(asttmp);
                identicalMO_n.add(1);
                WriteAstDyn(outDir+"/MO/ident/"+(identicalMO_n.size()-1)+"/", "1", asttmp);
            }

            // Look for parametric
            match = false;
            int id_param = -1;
            for(int j = 0; j < parametricMO.size() ; j++) {
                if(astDynMatcher.match(extractedEval, parametricMO.get(j)) != ASTDynMatcher.TYPE.DIFFERENT) {
                    match = true;
                    parametricMO_n.set(j, parametricMO_n.get(j)+1);
                    WriteAstDyn(outDir+"/MO/param/"+j+"/", parametricMO_n.get(j).toString(), extractedEval);
                    id_param = j;
                    break;
                }
            }
            if(!match) {
                ASTDynamic asttmp= new ASTDynamic(extractedEval);
                id_param = parametricMO_n.size();
                parametricMO.add(asttmp);
                parametricMO_n.add(1);
                parametricMO_iden_n.add(new HashMap<>());
                WriteAstDyn(outDir+"/MO/param/"+(parametricMO_n.size()-1)+"/", "1", asttmp);
                asttmp.getImageTable().clear();
            }

            int old_value = parametricMO_iden_n.get(id_param).getOrDefault(id_iden, 0);
            parametricMO_iden_n.get(id_param).put(id_iden, old_value+1);



            WriteAstDyn(outDir+"/MO/all/", file+"."+i, extractedEval);
        }
    }

    private static void WriteAstDyn(String outDir, String file, ASTDynamic extractedEval) {
        try {
            File out_file = new File(outDir + /*"/json/" +*/ file + ".astdyn.json");
            File parentDir = out_file.getAbsoluteFile().getParentFile();
            parentDir.mkdirs();
            Printer printer = new ASTDynamicJsonPrint(extractedEval, out_file);
            printer.print();

            /*out_file = new File(outDir + "/dot/" + file +".astdyn.dot");
            parentDir = out_file.getAbsoluteFile().getParentFile();
            parentDir.mkdirs();
            printer = new ASTDynamicDotPrint(extractedEval, out_file);
            printer.print();*/
        } catch (Exception e) {
            LOGGER.severe("Failed to print dynamic AST");
        }
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
