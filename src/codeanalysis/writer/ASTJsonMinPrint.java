package org.polymtl.codeanalysis.writer;

import org.json.JSONArray;
import org.polymtl.codeanalysis.model.AST;
import org.polymtl.codeanalysis.model.defsInt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ASTJsonMinPrint implements Printer {
    private PrintWriter writer = null;
    private AST ast = null;
    private boolean firstline = true;

    public ASTJsonMinPrint(AST ast, File f) throws FileNotFoundException {
        writer = new PrintWriter(f);
        this.ast = ast;
    }

    private void print(Object... args) {
        JSONArray array = new JSONArray();
        for (Object arg : args) {
            array.put(arg);
        }
        if(firstline)
            firstline = false;
        else
            writer.write(",\n");

        writer.write("  "+(new JSONArray(array)).toString());

    }

    @Override
    public void print() {
        writer.write("[\n");
        firstline = true;
        visit();
        writer.write("\n]\n");
        writer.flush();
        writer.close();
    }

    public void visit() {
        print("node_root", ast.getRoot());
        for(Integer key : ast.getNodeIds()) {
            print("type", key, ast.getType(key));
            for(Integer childId : ast.getChildren(key))
                print("ast_succ", key, childId);
        }
    }

}
