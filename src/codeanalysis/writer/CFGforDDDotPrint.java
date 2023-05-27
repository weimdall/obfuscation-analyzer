/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.writer;

import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.visitors.CFGDeadCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class CFGforDDDotPrint implements Printer {

    CFGforDD cfg = null;
    private PrintWriter writer = null;
    private CFGDeadCode deadCode = null;
    private List<Integer> visited = null;

    public CFGforDDDotPrint(CFGforDD tree, PrintWriter pw) {
        cfg = tree;
        writer = pw;
    }
    public CFGforDDDotPrint(CFGforDD tree, File f) throws FileNotFoundException {
        this(tree, new PrintWriter(f));
    }

    public void print() {
        visited = new ArrayList<>();
        writer.write("digraph ast {\n");
        writer.write("node [shape=none];\n");
        visit();
        writer.write("}\n");
        writer.flush();
        writer.close();
    }

    public String getCleanImage(String token) {
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



    public String getDotNode(Integer nodeId, String type, String token) {
        String tableStyle = "border='1' cellspacing='0' cellpadding='10' style='rounded' ";
        String cellStyle = "border='0'";


        String firstline = "<TR><TD "+cellStyle+">"+nodeId+"</TD><TD "+cellStyle+"><B>"+type+"</B></TD></TR>";
        String secondLine = "";
        if (token != null) {
            cellStyle += " cellpadding='5'";
            /*if(astTable.getVarId(nodeId) != null)
                secondLine += "<HR/><TR><TD " + cellStyle + ">(" + astTable.getVarScope(nodeId) + ", " + astTable.getVarId(nodeId) + ")</TD><TD " + cellStyle + ">";
            else*/
                secondLine += "<HR/><TR><TD "+cellStyle+" colspan='2'>";
            secondLine += token;
            secondLine += "</TD></TR>";
        }
        /*else if(astTable.getScopeId(nodeId) != null) {
            cellStyle += " cellpadding='5'";
            secondLine += "<HR/><TR><TD " + cellStyle + ">Scope</TD><TD " + cellStyle + ">"+astTable.getScopeId(nodeId)+ "</TD></TR>";
        }*/

        return "<TABLE " + tableStyle + ">"+firstline+secondLine+"</TABLE>";
    }


    public void visit() {
        //print("node_root", cfg.getRoot());
        boolean main = false;
        List<Integer> processed = new ArrayList<>();
        for(Integer key : new TreeSet<>(cfg.getNodeIdsList())) {
            if(cfg.getType(key) == null)
                writer.write(key + " [label=<" + getDotNode(key, " ", null) + ">];\n");
            else {

                switch (cfg.getType(key)) {
                    case "TK_NODE":
                        writer.write(key + " [label=<" + getDotNode(key, cfg.getType(key).toUpperCase(), getCleanImage(cfg.getAccess(key).toString().replace(Integer.toString(defsInt.UNDEF_VAL), "UNDEF_VAL"))) + ">];\n");
                        break;
                    default:
                        writer.write(key + " [label=<" + getDotNode(key, cfg.getType(key).toUpperCase(), getCleanImage(cfg.getImage(key))) + ">];\n");
                        break;
                }
            }

            // Edges

            List<Integer> children = cfg.getChildren(key);
            if( children != null ) {
                for (Integer child : children) {
                    /*if(cfg.getType(key).equals("Condition") && cfg.getChildren(key).size() == 2) {
                        if(child.equals(cfg.getChildren(key).get(0)))
                            writer.write(key + " -> " + child + " [weight=2;label=True];\n");
                        if(child.equals(cfg.getChildren(key).get(1)))
                            writer.write(key + " -> " + child + " [weight=2;label=False];\n");
                    }
                    else*/
                    writer.write(key + " -> " + child + "[weight=10];\n");
                }
            }

            Integer nodeId = cfg.getCallEnds(key);
            if (nodeId != null)
                writer.write(key + " -> " + nodeId + " [weight=10;style=dotted];\n");
            nodeId = cfg.getIncludeEnds(key);
            if (nodeId != null)
                writer.write(key + " -> " + nodeId + " [weight=10;style=dotted];\n");
            nodeId = cfg.getRhs(key);
            if (nodeId != null)
                writer.write(key + " -> " + nodeId + " [weight=1;color=blue];\n");
            nodeId = cfg.getLhs(key);
            if (nodeId != null)
                writer.write(key + " -> " + nodeId + " [weight=1;color=red];\n");
            List<Integer[]> childrent = cfg.getActArg(key);
            if( childrent != null )
                for (Integer[] child : childrent)
                    writer.write(key + " -> " + child[1] + "[weight=1;color=cyan;label="+child[0]+"];\n");
            /*childrent = cfg.getFormArg(key);
            if( childrent != null )
                for (Integer[] child : childrent)
                    writer.write(key + " -> " + child[1] + "[constraint=false;color=cyan];\n");*/
            children = cfg.getExprArg(key);
            if( children != null )
                for (Integer child : children)
                    writer.write(key + " -> " + child + "[weight=1;color=cyan];\n");


        }
    }
}
