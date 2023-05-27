/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.util;

import org.polymtl.codeanalysis.exceptions.ASTJsonException;
import org.polymtl.codeanalysis.model.AST;
import org.polymtl.codeanalysis.model.ParseTree;
import org.polymtl.codeanalysis.parser.*;
import org.polymtl.codeanalysis.reader.ParseTreeJsonReader;
import org.polymtl.codeanalysis.visitors.ParseTreeToAST;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringParser {
    public static AST parse(String pCode) throws ASTJsonException, ParseException, IOException, TokenMgrError {
        File parentDir = null;
        String code = "<?php\n" + ReplaceEscapedHex(pCode);

        Php parser = new Php(new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)));
        SimpleNode parserTree = parser.Stop();

        parserTree.jjtAccept(new AnnotateVisitor("#DirectString#"), null);

        File tokenJsonFile = File.createTempFile("tokens", null);
        PrintWriter tokenJsonWriter = new PrintWriter(tokenJsonFile);
        DumpFlatJsonTokens tokenJsonDumper = new DumpFlatJsonTokens(tokenJsonWriter);
        parserTree.jjtAccept(tokenJsonDumper, null);
        tokenJsonDumper.close();

        File astJsonFile = File.createTempFile("parsetree", null);
        PrintWriter astJsonWriter = new PrintWriter(astJsonFile);
        DumpFlatJsonAST astJsonDumper = new DumpFlatJsonAST(tokenJsonDumper.getTokenIndices(), astJsonWriter);
        parserTree.jjtAccept(astJsonDumper, null);
        astJsonWriter.close();

        ParseTreeJsonReader reader = new ParseTreeJsonReader();
        ParseTree newPt = reader.read(new BufferedReader(new FileReader(astJsonFile)));
        for (Integer i : newPt.getNodeIds()) { // Remove <?php line in positions
            Integer[] pos = newPt.getPositions(i);
            if (pos != null) {
                if(pos[0] != null)
                    pos[0] -= 1;
                if(pos[1] != null)
                    pos[1] -= 1;
                newPt.setNodePosition(i, pos);
            }
        }
        ParseTreeToAST newPtTranslator = new ParseTreeToAST(newPt);
        tokenJsonFile.delete();
        astJsonFile.delete();
        return newPtTranslator.visit();
    }

    public static String ReplaceEscapedHex(String in) {
        // hexa
        Pattern pattern = Pattern.compile("(\\\\x[0-9A-Fa-f]{1,2})");
        Matcher matcher = pattern.matcher(in);
        String out = new String(in);
        while(matcher.find()) {
            String hex = matcher.group(1);
            int c = Integer.valueOf(hex.substring(2), 16);
            if(c == '"' || c == '\'')
                continue;
            out = out.replace(hex, String.valueOf((char) c));
        }
        // octal
        pattern = Pattern.compile("(\\\\[0-9A-Fa-f]{2,3})");
        matcher = pattern.matcher(in);
        while(matcher.find()) {
            String hex = matcher.group(1);
            int c = Integer.valueOf(hex.substring(1), 8);
            if(c == '"' || c == '\'')
                continue;
            out = out.replace(hex, String.valueOf((char) c));
        }
        return out;
    }
}
