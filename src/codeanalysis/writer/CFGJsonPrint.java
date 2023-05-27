package org.polymtl.codeanalysis.writer;

import org.json.JSONArray;
import org.polymtl.codeanalysis.model.CFG;
import org.polymtl.codeanalysis.model.defsInt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CFGJsonPrint implements Printer {
    private PrintWriter writer = null;
    private CFG cfg = null;
    private boolean firstline = true;

    public CFGJsonPrint(CFG cfg, PrintWriter pw) {
        writer = pw;
        this.cfg = cfg;
    }
    public CFGJsonPrint(CFG cfg, File f) throws FileNotFoundException {
        this(cfg, new PrintWriter(f));
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
        print("node_root", cfg.getRoot());
        if (cfg.getFilename() != null)
            print("filename", cfg.getFilename());
        for(Integer key : cfg.getNodeIds()) {
            print("type", key, cfg.getType(key));
            if(cfg.getImage(key) != null)
                print("image", key, cfg.getImage(key));
            if(cfg.getScopeId(key) != null)
                print("scope_id", key, cfg.getScopeId(key));
            if(cfg.getVarId(key) != null)
                print("var_id", key, cfg.getVarId(key));
            if(cfg.getVarScope(key) != null)
                print("var_scope", key, cfg.getVarScope(key));
            if(cfg.getNodeAstPtr(key) != defsInt.UNDEF_VAL)
                print("ast_pt", key, cfg.getNodeAstPtr(key));
            if(cfg.getCallEnd(key) != null)
                print("call_end", key, cfg.getCallEnd(key));
            if(cfg.getCallExpr(key) != null)
                print("call_expr", key, cfg.getCallExpr(key));
            if(cfg.getOpHands(key) != null)
                print("op_hands", key, cfg.getOpHands(key).getKey(), cfg.getOpHands(key).getValue());
            for(Integer childId : cfg.getChildren(key))
                print("cfg_succ", key, childId);
            if(cfg.getFuncCallArgs(key) != null)
                for(Integer argId : cfg.getFuncCallArgs(key))
                    print("func_call_arg", key, cfg.getFuncCallArgs(key).indexOf(argId), argId);
            if(cfg.getFuncDefParam(key) != null)
                for(Integer argId : cfg.getFuncDefParam(key))
                    print("func_def_param", key, cfg.getFuncDefParam(key).indexOf(argId), argId);
            if(cfg.getEntryFuncName(key) != null)
                print("entry_func_name", key, cfg.getEntryFuncName(key));
            if(cfg.getPositions(key) != null) {
                print("line_begin",   key, cfg.getPositions(key)[0]);
                print("line_end",     key, cfg.getPositions(key)[1]);
                print("column_begin", key, cfg.getPositions(key)[2]);
                print("column_end",   key, cfg.getPositions(key)[3]);
                print("token_begin",  key, cfg.getPositions(key)[4]);
                print("token_end",    key, cfg.getPositions(key)[5]);
            }
        }
    }

}
