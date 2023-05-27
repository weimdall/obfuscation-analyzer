package org.polymtl.codeanalysis.writer;

import org.json.JSONArray;
import org.polymtl.codeanalysis.model.ParseTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ParseTreeJsonPrint implements Printer {
    private PrintWriter writer = null;
    private ParseTree pt = null;
    private boolean firstline = true;

    public ParseTreeJsonPrint(ParseTree pt, PrintWriter pw) {
        writer = pw;
        this.pt = pt;
    }
    public ParseTreeJsonPrint(ParseTree pt, File f) throws FileNotFoundException {
        this(pt, new PrintWriter(f));
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
        print("node_root", pt.getRoot());
        if (pt.getFilename() != null)
            print("filename", pt.getFilename());
        for(Integer key : pt.getNodeIds()) {
            print("type", key, pt.getType(key));
            if(pt.getImage(key) != null)
                print("image", key, pt.getImage(key));
            for(Integer childId : pt.getChildren(key))
                print("ast_succ", key, childId);
            if(pt.getPositions(key) != null) {
                if(pt.getPositions(key)[0] != null)
                    print("line_begin",   key, pt.getPositions(key)[0]);
                if(pt.getPositions(key)[1] != null)
                    print("line_end",     key, pt.getPositions(key)[1]);
                if(pt.getPositions(key)[2] != null)
                    print("column_begin", key, pt.getPositions(key)[2]);
                if(pt.getPositions(key)[3] != null)
                    print("column_end",   key, pt.getPositions(key)[3]);
                if(pt.getPositions(key)[4] != null)
                    print("token_begin",  key, pt.getPositions(key)[4]);
                if(pt.getPositions(key)[5] != null)
                    print("token_end",    key, pt.getPositions(key)[5]);
            }
        }
    }

}
