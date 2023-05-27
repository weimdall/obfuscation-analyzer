/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.writer;

import org.json.JSONArray;
import org.polymtl.codeanalysis.model.CFGforDD;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class CFGforDDJsonPrint implements Printer {
    private PrintWriter writer = null;
    private CFGforDD cfg = null;
    private List<Integer> visited = null;
    private boolean firstline = true;

    public CFGforDDJsonPrint(CFGforDD cfg, PrintWriter pw) throws FileNotFoundException {
        writer = pw;
        this.cfg = cfg;
    }
    public CFGforDDJsonPrint(CFGforDD cfg, File f) throws FileNotFoundException {
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
        visited = new ArrayList<>();
        for(String key : cfg.getAttr().keySet())
            print("cfg_attr", key, cfg.getAttr().get(key));
        visit(cfg.getRoot());
        for(Integer key : cfg.getEntryFuncIds())
            visit(key);
        writer.write("\n]\n");
        writer.flush();
        writer.close();
    }

    public void visit(Integer key) {
        if(visited.contains(key))
            return;
        visited.add(key);

        if(cfg.getType(key) != null)
            print("node_type", key, cfg.getType(key));
        if(cfg.getImage(key) != null)
            print("dot_label", key, cfg.getImage(key));
        if(cfg.getEntryFuncName(key) != null)
            print("entry_func_name", key, cfg.getEntryFuncName(key).get(0), cfg.getEntryFuncName(key).get(1)); // scope, name
        if(cfg.getCfgEntry(key) != null)
            print("entry", key, cfg.getCfgEntry(key));
        if(cfg.getCfgExit(key) != null)
            print("exit", key, cfg.getCfgExit(key));
        if(cfg.getCallBegins(key) != null)
            print("call_begin", key, cfg.getCallBegins(key));
        if(cfg.getCallEnds(key) != null)
            print("call_end", key, cfg.getCallEnds(key));
        if(cfg.getIncludeBegins(key) != null)
            print("include_begin", key, cfg.getIncludeBegins(key));
        if(cfg.getIncludeEnds(key) != null)
            print("include_end", key, cfg.getIncludeEnds(key));
        if(cfg.getCallBeginsFuncName(key) != null)
            print("call_begin_func_name", key, cfg.getCallBeginsFuncName(key));
        if(cfg.getCallEndsFuncName(key) != null)
            print("call_end_func_name", key, cfg.getCallEndsFuncName(key));
        if(cfg.getRhs(key) != null)
            print("rhs", key, cfg.getRhs(key));
        if(cfg.getLhs(key) != null)
            print("lhs", key, cfg.getLhs(key));
        if(cfg.getActArg(key) != null) {
            for(Integer[] id : cfg.getActArg(key))
                print("act_arg", key, id[0], id[1]); // index, nodeId
        }
        if(cfg.getExprArg(key) != null) {
            for(Integer id : cfg.getExprArg(key))
                print("expr_arg", key, id);
        }
        if(cfg.getFormArg(key) != null) {
            for(Integer[] id : cfg.getFormArg(key))
                print("form_arg", key, id[0], id[1]); // index, nodeId
        }
        if(cfg.getActRet(key) != null)
            print("act_ret", key, cfg.getActRet(key));
        if(cfg.getFormRet(key) != null)
            print("form_ret", key, cfg.getFormRet(key));
        if(cfg.getAccess(key) != null)
            print("access", key, cfg.getAccess(key).get(0), cfg.getAccess(key).get(1), cfg.getAccess(key).get(2), cfg.getAccess(key).get(3)); // type, scope, astptr, varname


        for (Integer childId : cfg.getChildren(key))
            print("cfg_succ", key, childId);
        for (Integer childId : cfg.getChildren(key))
            visit(childId);

        if (cfg.getCallEnds(key) != null)
            visit(cfg.getCallEnds(key));
        if (cfg.getIncludeEnds(key) != null)
            visit(cfg.getIncludeEnds(key));
    }

}
