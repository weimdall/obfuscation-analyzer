package org.polymtl.codeanalysis.writer;

import org.polymtl.codeanalysis.model.Graph;
import org.polymtl.codeanalysis.exceptions.ASTJsonException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
/**
 * Copyright (C) 2021 - Julien Cassagne - Ettore Merlo - All rights reserved
 */

public class ASTPrettyPrint implements Printer  {

    Graph astTable = null;
    private PrintWriter writer = null;
    private int curIndent = 0;

    public ASTPrettyPrint(Graph ast, File f) throws FileNotFoundException {
        astTable = ast;
        writer = new PrintWriter(f);
    }

    public void print() {
        print(astTable.getRoot());
    }
    public void print(int root) {
        visit(root, 0);
        writer.flush();
        writer.close();
    }

    private String getIndentStr() {
        StringBuilder indentStr = new StringBuilder();
        for (int i = 0; i < curIndent; ++i) {
            indentStr.append("   ");
        }
        return indentStr.toString();
    }

    private void visitChildren(Integer astNodeId, int depth) {
        List<Integer> children = astTable.getChildren(astNodeId);
        if (children != null) {
            for (Integer childId : children) {
                visit(childId, depth);
            }
        }
    }


    /////////////////////////////////////////////////////

    public void visit(Integer astNodeId, int depth) {
        String curType = astTable.getType(astNodeId);
        String token = astTable.getImage(astNodeId);
        Integer[] positions = astTable.getPositions(astNodeId);
        if (curType == null)
            throw new ASTJsonException("Missing node " + astNodeId);

        writer.write(getIndentStr() + curType +
                " (" + astNodeId + " " + depth + ")" +
                ((positions == null) ? "" : " (" + Arrays.toString(positions) + ")") +
                ((token == null) ? "" : " '" + token + "'") +
                "\n");

        curIndent++;
        visitChildren(astNodeId, (depth + 1));
        curIndent--;
    }


}
