/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.writer;

import org.polymtl.codeanalysis.model.ASTDynamic;
import org.polymtl.codeanalysis.visitors.CFGDeadCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ASTDynamicDotPrint implements Printer {

    ASTDynamic origAstTable = null;
    private PrintWriter writer = null;
    private CFGDeadCode deadCode = null;
    private List<Integer> visited = null;

    public ASTDynamicDotPrint(ASTDynamic tree, PrintWriter pw) {
        origAstTable = tree;
        writer = pw;
    }
    public ASTDynamicDotPrint(ASTDynamic tree, File f) throws FileNotFoundException {
        this(tree, new PrintWriter(f));
    }

    public void print() {
        visited = new ArrayList<>();
        writer.write("digraph ast {\n");
        writer.write("node [shape=none];\n");
        visit(this.origAstTable);
        writer.write("}\n");
        writer.flush();
        writer.close();
    }

    public String getCleanImage(ASTDynamic astTable, Integer astNodeId) {
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


    public String getDotNode(ASTDynamic astTable, Integer nodeId) {
        String token = getCleanImage(astTable, nodeId);
        String type = astTable.getType(nodeId);
        String tableStyle = "border='1' cellspacing='0' cellpadding='10' style='rounded' ";
        String cellStyle = "border='0'";
        if(type.endsWith("Failed"))
            tableStyle += "bgcolor='#FFCCCC' ";

        String firstline = "<TR><TD "+cellStyle+">"+nodeId+"</TD><TD "+cellStyle+"><B>"+type+"</B></TD></TR>";
        String secondLine = "";
        cellStyle += " cellpadding='5'";
        if(astTable.getPatternsMatched(nodeId) != null) {
            secondLine += "<HR/><TR><TD " + cellStyle + ">Patterns</TD><TD " + cellStyle + ">"+astTable.getPatternsMatched(nodeId).toString()+ "</TD></TR>";
        }
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

    public void visit(ASTDynamic astTable) {
        for(Integer key : astTable.getNodeIds()) {
            if(astTable.getType(key).equals("Dead") && astTable.getParent(key) == null)
                continue;

            writer.write(key + " [label=<" + getDotNode(astTable, key) + ">];\n");
            List<Integer> children = astTable.getChildren(key);
            if( children != null ) {
                for (Integer child : children)
                    writer.write(key + " -> " + child + " [weight=2];\n");
            }
            children = astTable.getDynamicResolution(key);
            if( children != null ) {
                for (Integer child : children)
                    writer.write(key + " -> " + child + " [style=dotted,label=DynamicResolution];\n");
            }
            children = astTable.getDataflowResolution(key);
            if( children != null ) {
                for (Integer child : children)
                    writer.write(key + " -> " + child + " [style=dotted,label=DataflowResolution];\n");
            }
            children = astTable.getParseEdge(key);
            if( children != null ) {
                for (Integer child : children)
                    writer.write(key + " -> " + child + " [style=dotted,label=Parse];\n");
            }
        }
    }

}
