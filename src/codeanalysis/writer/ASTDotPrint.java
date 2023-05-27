/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.writer;

import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.util.*;
import org.polymtl.codeanalysis.visitors.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ASTDotPrint implements Printer {

    GraphIndexedVariable astTable = null;
    private PrintWriter writer = null;
    private CFGDeadCode deadCode = null;
    private List<Integer> visited = null;

    public ASTDotPrint(GraphIndexedVariable tree, PrintWriter pw) {
        astTable = tree;
        writer = pw;
        /*if(tree instanceof AST) {
            varIndexer = new ASTVariableIndexer(tree);
            varIndexer.visit();
        }*/
        if(tree instanceof CFG && !(tree instanceof Dominator)) {
            deadCode = new CFGDeadCode((CFG) tree);
            deadCode.visit();

            //varIndexer = new ASTVariableIndexer(tree);
            //varIndexer.visit();
        }
    }
    public ASTDotPrint(GraphIndexedVariable tree, File f) throws FileNotFoundException {
        this(tree, new PrintWriter(f));
    }

    public void print() {
        visited = new ArrayList<>();
        if(astTable instanceof Dominator)
            writer.write("digraph dom {\n");
        else if(astTable instanceof CFG)
            writer.write("digraph cfg {\n");
        else
            writer.write("digraph ast {\n");
        writer.write("node [shape=none];\n");
        visit();
        writer.write("}\n");
        writer.flush();
        writer.close();
    }

    public String getCleanImage(Integer astNodeId) {
        String token = astTable.getImage(astNodeId);
        if(token == null)
            return null;
        if(token.length() >= 40)
            token = token.substring(0, 40-3) + "...";
        token = token.replace("\"", "'");
        token = token.replace("\\", "");
        token = token.replace("/", "");
        token = token.replace("&", "&amp;");
        token = token.replace("<", "&lt;");
        token = token.replace(">", "&gt;");
        token = token.replace("\r", "");
        token = token.replace("\n", " ");
        return token;
    }


    public String getDotNode(Integer nodeId) {
        String token = getCleanImage(nodeId);
        String type = astTable.getType(nodeId);
        String tableStyle = "border='1' cellspacing='0' cellpadding='10' style='rounded' ";
        String cellStyle = "border='0'";

        if(deadCode != null && deadCode.getDead().contains(nodeId) )
            tableStyle += " bgcolor='#CCCCCC'";
        if(astTable instanceof CFG && ((CFG) astTable).getNodeAstPtr(nodeId) == defsInt.UNDEF_VAL)
            tableStyle += " color='#880000'";

        String firstline = "<TR><TD "+cellStyle+">"+nodeId+"</TD><TD "+cellStyle+"><B>"+type+"</B></TD></TR>";
        String secondLine = "";
        cellStyle += " cellpadding='5'";
        if(astTable.getScopeId(nodeId) != null) {
            secondLine += "<HR/><TR><TD " + cellStyle + ">Scope</TD><TD " + cellStyle + ">"+astTable.getScopeId(nodeId)+ "</TD></TR>";
        }
        if (token != null) {
            if(astTable.getVarId(nodeId) != null)
                secondLine += "<HR/><TR><TD " + cellStyle + ">(" + astTable.getVarScope(nodeId) + ", " + astTable.getVarId(nodeId) + ")</TD><TD " + cellStyle + ">";
            else
                secondLine += "<HR/><TR><TD "+cellStyle+" colspan='2'>";
            secondLine += token;
            secondLine += "</TD></TR>";
        }


        return "<TABLE " + tableStyle + ">"+firstline+secondLine+"</TABLE>";
    }

    public void visit() {
        for(Integer key : astTable.getNodeIds()) {
            if(astTable.getType(key).equals("Dead") && astTable.getParent(key).size() == 0)
                continue;

            writer.write(key + " [label=<" + getDotNode(key) + ">];\n");
            List<Integer> children = astTable.getChildren(key);
            if( children != null ) {
                for (Integer child : children) {
                    if(astTable instanceof CFG && !(astTable instanceof Dominator) &&
                            astTable.getType(key).endsWith("Condition") &&
                            astTable.getChildren(key).size() == 2) {
                        if(child.equals(astTable.getChildren(key).get(0)))
                            writer.write(key + " -> " + child + " [weight=10;label=True];\n");
                        if(child.equals(astTable.getChildren(key).get(1)))
                            writer.write(key + " -> " + child + " [weight=10;label=False];\n");
                    }
                    else
                        writer.write(key + " -> " + child + " [weight=10];\n");
                }
            }

            if(astTable instanceof CFG) {
                Integer nodeId = ((CFG) astTable).getCallEnd(key);
                if (nodeId != null)
                    writer.write(key + " -> " + nodeId + " [weight=10;style=dotted];\n");
                nodeId = ((CFG) astTable).getCallExpr(key);
                if (nodeId != null)
                    writer.write(key + " -> " + nodeId + " [weight=1;color=purple];\n");
                List<Integer> nodesId = ((CFG) astTable).getFuncCallArgs(key);
                if (nodesId != null)
                    for(Integer node : nodesId)
                        writer.write(key + " -> " + node + " [weight=1;color=cyan];\n");
                /*nodesId = ((CFG) astTable).getFuncDefParam(key);
                if (nodesId != null)
                    for(Integer node : nodesId)
                        writer.write(key + " -> " + node + " [constraint=false;color=cyan];\n");*/

                Pair<Integer, Integer> opHands = ((CFG) astTable).getOpHands(key);
                if (opHands != null) {
                    if (astTable.getType(key).equals("ArrayIndex") || (astTable.getImage(key) != null && astTable.getImage(key).equals("="))) {
                        writer.write(key + " -> " + opHands.getKey() + " [weight=1;color=red];\n");
                        if(opHands.getValue() != null) // $a[]
                            writer.write(key + " -> " + opHands.getValue() + " [weight=1;color=blue];\n");
                    }
                    else {
                        writer.write(key + " -> " + opHands.getKey() + " [weight=1;color=cyan];\n");
                        if(opHands.getValue() != null) // $a[]
                            writer.write(key + " -> " + opHands.getValue() + " [weight=1;color=cyan];\n");
                    }
                }
            }
        }
    }
}
