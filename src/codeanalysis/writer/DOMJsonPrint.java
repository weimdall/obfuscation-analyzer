package org.polymtl.codeanalysis.writer;

import org.json.JSONArray;
import org.polymtl.codeanalysis.model.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class DOMJsonPrint implements Printer {
    private PrintWriter writer = null;
    private Dominator dom = null;
    private boolean firstline = true;

    public DOMJsonPrint(Dominator dom, PrintWriter pw) {
        writer = pw;
        this.dom = dom;
    }
    public DOMJsonPrint(Dominator dom, File f) throws FileNotFoundException {
        this(dom, new PrintWriter(f));
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
        writer.write("\n]\n");;
        writer.flush();
        writer.close();
    }

    public void visit() {
        print("node_root", dom.getRoot());
        if (dom.getFilename() != null)
            print("filename", dom.getFilename());
        for(Integer key : dom.getNodeIds()) {
            print("type", key, dom.getType(key));
            if(dom.getImage(key) != null)
                print("image", key, dom.getImage(key));
            if(dom.getScopeId(key) != null)
                print("scope_id", key, dom.getScopeId(key));
            if(dom.getVarId(key) != null)
                print("var_id", key, dom.getVarId(key));
            if(dom.getVarScope(key) != null)
                print("var_scope", key, dom.getVarScope(key));
            if(dom.getNodeAstPtr(key) != defsInt.UNDEF_VAL)
                print("ast_pt", key, dom.getNodeAstPtr(key));
            if(dom.getCallEnd(key) != null)
                print("call_end", key, dom.getCallEnd(key));
            if(dom.getCallExpr(key) != null)
                print("call_expr", key, dom.getCallExpr(key));
            if(dom.getOpHands(key) != null)
                print("op_hands", key, dom.getOpHands(key).getKey(), dom.getOpHands(key).getValue());
            for(Integer childId : dom.getChildren(key))
                print("cfg_succ", key, childId);
            if(dom.getFuncCallArgs(key) != null)
                for(Integer argId : dom.getFuncCallArgs(key))
                    print("func_call_arg", key, dom.getFuncCallArgs(key).indexOf(argId), argId);
            if(dom.getFuncDefParam(key) != null)
                for(Integer argId : dom.getFuncDefParam(key))
                    print("func_def_param", key, dom.getFuncDefParam(key).indexOf(argId), argId);
            if(dom.getDom(key) != null)
                for(Integer domId : dom.getDom(key))
                    print("dominator", key, domId);
            if(dom.getEntryFuncName(key) != null)
                print("entry_func_name", key, dom.getEntryFuncName(key));
            if(dom.getPositions(key) != null) {
                print("line_begin",   key, dom.getPositions(key)[0]);
                print("line_end",     key, dom.getPositions(key)[1]);
                print("column_begin", key, dom.getPositions(key)[2]);
                print("column_end",   key, dom.getPositions(key)[3]);
                print("token_begin",  key, dom.getPositions(key)[4]);
                print("token_end",    key, dom.getPositions(key)[5]);
            }
        }
    }

}
