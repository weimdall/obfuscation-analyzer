package org.polymtl.codeanalysis.writer;

import org.json.JSONArray;
import org.polymtl.codeanalysis.model.AST;
import org.polymtl.codeanalysis.model.defsInt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ASTJsonPrint implements Printer {
    protected PrintWriter writer = null;
    protected AST ast = null;
    private boolean firstline = true;

    public ASTJsonPrint(AST ast, File f) throws FileNotFoundException {
        writer = new PrintWriter(f);
        this.ast = ast;
    }

    protected ASTJsonPrint() {
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
        if (ast.getFilename() != null)
            print("filename", ast.getFilename());
        for(Integer key : ast.getNodeIds()) {
            print("type", key, ast.getType(key));
            if(ast.getImage(key) != null)
                print("image", key, ast.getImage(key));
            if(ast.getScopeId(key) != null)
                print("scope_id", key, ast.getScopeId(key));
            if(ast.getVarId(key) != null)
                print("var_id", key, ast.getVarId(key));
            if(ast.getVarScope(key) != null)
                print("var_scope", key, ast.getVarScope(key));
            if(ast.getNodePTPtr(key) != defsInt.UNDEF_VAL)
                print("parsetree_pt", key, ast.getNodePTPtr(key));
            for(Integer childId : ast.getChildren(key))
                print("ast_succ", key, childId);
            if(ast.getPositions(key) != null) {
                if(ast.getPositions(key)[0] != null)
                    print("line_begin",   key, ast.getPositions(key)[0]);
                if(ast.getPositions(key)[1] != null)
                    print("line_end",     key, ast.getPositions(key)[1]);
                if(ast.getPositions(key)[2] != null)
                    print("column_begin", key, ast.getPositions(key)[2]);
                if(ast.getPositions(key)[3] != null)
                    print("column_end",   key, ast.getPositions(key)[3]);
                if(ast.getPositions(key)[4] != null)
                    print("token_begin",  key, ast.getPositions(key)[4]);
                if(ast.getPositions(key)[5] != null)
                    print("token_end",    key, ast.getPositions(key)[5]);
            }
        }
    }

}
