/*
 * Copyright (C) 2020-2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.model;

import java.util.*;

public class CFGforDD extends Graph {
    //nodeTypeTable: node_type
    //succTable: cfg_succ
    //rootId: entry

    protected final Map<Integer, List<Object>>      entryFuncName = new HashMap<>(); // entry_func_name
    protected final Map<String, String>             attr = new HashMap<>(); // cfg_attr
    protected final Map<Integer, List<Object>>      access = new HashMap<>(); // access
    protected final Map<Integer, Integer>           callBegins = new HashMap<Integer, Integer>(); // call_begin
    protected final Map<Integer, Integer>           callEnds = new HashMap<Integer, Integer>(); // call_end
    protected final Map<Integer, Integer>           includeBegins = new HashMap<Integer, Integer>(); // include_begin
    protected final Map<Integer, Integer>           includeEnds = new HashMap<Integer, Integer>(); // include_end
    protected final Map<Integer, String>            callBeginsFuncName = new HashMap<>(); // call_begin_func_name
    protected final Map<Integer, String>            callEndsFuncName = new HashMap<>(); // call_end_func_name
    protected final Map<Integer, Integer>           rhs = new HashMap<>(); // rhs
    protected final Map<Integer, Integer>           lhs = new HashMap<>(); // rhs
    protected final Map<Integer, Integer>           cfgEntry = new HashMap<>(); // entry
    protected final Map<Integer, Integer>           cfgExit = new HashMap<>(); // exit
    protected final Map<Integer, Integer>           actRet = new HashMap<Integer, Integer>(); // act_ret
    protected final Map<Integer, List<Integer>>     exprArg = new HashMap<Integer, List<Integer>>(); // expr_arg
    protected final Map<Integer, Integer>           formRet = new HashMap<>(); // form_ret
    protected final Map<Integer, List<Integer[]>>   formArg = new HashMap<>(); // form_arg
    protected final Map<Integer, List<Integer[]>>   actArg = new HashMap<>(); // act_arg

    public List<Integer> getNodeIdsList() {
        List<Integer> list = new ArrayList<>(nodeTypeTable.keySet());
        for(Integer k : succTable.keySet())
            for(Integer v : succTable.get(k))
                list.add(v);

        return list;
    }

    @Override
    public void deleteNode(Integer cfgNodeId) {
        entryFuncName.remove(cfgNodeId);
        access.remove(cfgNodeId);
        callBegins.remove(cfgNodeId);
        callEnds.remove(cfgNodeId);
        callBeginsFuncName.remove(cfgNodeId);
        callEndsFuncName.remove(cfgNodeId);
        rhs.remove(cfgNodeId);
        lhs.remove(cfgNodeId);
        cfgEntry.remove(cfgNodeId);
        cfgExit.remove(cfgNodeId);
        actRet.remove(cfgNodeId);
        exprArg.remove(cfgNodeId);
        formArg.remove(cfgNodeId);
        formRet.remove(cfgNodeId);
        super.deleteNode(cfgNodeId);
    }

    public Map<String, String> getAttr() {
        return attr;
    }
    public List<Object> getEntryFuncName(Integer id) {
        return entryFuncName.get(id);
    }
    public Set<Integer> getEntryFuncIds() {
        return entryFuncName.keySet();
    }
    public List<Object> getAccess(Integer id) {
        return access.get(id);
    }
    public Integer getCallBegins(Integer id) {
        return callBegins.get(id);
    }
    public Integer getCallEnds(Integer id) {
        return callEnds.get(id);
    }
    public Integer getIncludeBegins(Integer id) {
        return includeBegins.get(id);
    }
    public Integer getIncludeEnds(Integer id) {
        return includeEnds.get(id);
    }
    public String getCallBeginsFuncName(Integer id) {
        return callBeginsFuncName.get(id);
    }
    public String getCallEndsFuncName(Integer id) {
        return callEndsFuncName.get(id);
    }
    public Integer getRhs(Integer id) {
        return rhs.get(id);
    }
    public Integer getLhs(Integer id) {
        return lhs.get(id);
    }
    public Integer getCfgEntry(Integer id) {
        return cfgEntry.get(id);
    }
    public Integer getCfgExit(Integer id) {
        return cfgExit.get(id);
    }
    public Integer getActRet(Integer id) {
        return actRet.get(id);
    }
    public List<Integer[]> getActArg(Integer id) {
        return actArg.get(id);
    }
    public List<Integer> getExprArg(Integer id) {
        return exprArg.get(id);
    }
    public List<Integer[]> getFormArg(Integer id) {
        return formArg.get(id);
    }
    public Integer getFormRet(Integer id) {
        return formRet.get(id);
    }

    public void addAttr(String s1, String s2) {
        attr.put(s1, s2);
    }
    public void addEntryFuncName(Integer id, List<Object> l) {
        entryFuncName.put(id, l);
    }
    public void addAccess(Integer id, List<Object> l) {
        access.put(id, l);
    }
    public void addIncludeBegins(Integer id, Integer v) {
        includeBegins.put(id, v);
    }
    public void addIncludeEnds(Integer id, Integer v) {
        includeEnds.put(id, v);
    }
    public void addCallBegins(Integer id, Integer v) {
        callBegins.put(id, v);
    }
    public void addCallEnds(Integer id, Integer v) {
        callEnds.put(id, v);
    }
    public void addCallBeginsFuncName(Integer id, String v) {
        callBeginsFuncName.put(id, v);
    }
    public void addCallEndsFuncName(Integer id, String v) {
        callEndsFuncName.put(id, v);
    }
    public void addRhs(Integer id, Integer v) {
        rhs.put(id, v);
    }
    public void addLhs(Integer id, Integer v) {
        lhs.put(id, v);
    }
    public void addCfgEntry(Integer id, Integer v) {
        cfgEntry.put(id, v);
    }
    public void addCfgExit(Integer id, Integer v) {
        cfgExit.put(id, v);
    }
    public void addActRet(Integer id, Integer v) {
        actRet.put(id, v);
    }
    public void addExprArg(Integer id, Integer v) {
        if(!exprArg.containsKey(id))
            exprArg.put(id, new ArrayList<>());
        exprArg.get(id).add(v);
    }
    public void addFormArg(Integer id, Integer[] v) {
        if(!formArg.containsKey(id))
            formArg.put(id, new ArrayList<>());
        formArg.get(id).add(v);
    }
    public void addActArg(Integer id, Integer[] v) {
        if(!actArg.containsKey(id))
            actArg.put(id, new ArrayList<>());
        actArg.get(id).add(v);
    }
    public void addFormRet(Integer id, Integer v) {
        formRet.put(id, v);
    }

    public void copyNode(Integer dstNodeId, CFGforDD srcGraph, Integer srcNodeId) {
        super.copyNode(dstNodeId, srcGraph, srcNodeId);
        if(srcGraph.access.containsKey(srcNodeId))
            this.access.put(dstNodeId, srcGraph.access.get(srcNodeId));
        if(srcGraph.exprArg.containsKey(srcNodeId))
            this.exprArg.put(dstNodeId, srcGraph.exprArg.get(srcNodeId));
        if(srcGraph.actRet.containsKey(srcNodeId))
            this.actRet.put(dstNodeId, srcGraph.actRet.get(srcNodeId));
        if(srcGraph.cfgEntry.containsKey(srcNodeId))
            this.cfgEntry.put(dstNodeId, srcGraph.cfgEntry.get(srcNodeId));
        if(srcGraph.cfgExit.containsKey(srcNodeId))
            this.cfgExit.put(dstNodeId, srcGraph.cfgExit.get(srcNodeId));
        if(srcGraph.rhs.containsKey(srcNodeId))
            this.rhs.put(dstNodeId, srcGraph.rhs.get(srcNodeId));
        if(srcGraph.lhs.containsKey(srcNodeId))
            this.lhs.put(dstNodeId, srcGraph.lhs.get(srcNodeId));
        if(srcGraph.entryFuncName.containsKey(srcNodeId))
            this.entryFuncName.put(dstNodeId, srcGraph.entryFuncName.get(srcNodeId));
        if(srcGraph.callBegins.containsKey(srcNodeId))
            this.callBegins.put(dstNodeId, srcGraph.callBegins.get(srcNodeId));
        if(srcGraph.callEnds.containsKey(srcNodeId))
            this.callEnds.put(dstNodeId, srcGraph.callEnds.get(srcNodeId));
        if(srcGraph.callBeginsFuncName.containsKey(srcNodeId))
            this.callBeginsFuncName.put(dstNodeId, srcGraph.callBeginsFuncName.get(srcNodeId));
        if(srcGraph.callEndsFuncName.containsKey(srcNodeId))
            this.callEndsFuncName.put(dstNodeId, srcGraph.callEndsFuncName.get(srcNodeId));
        if(srcGraph.formArg.containsKey(srcNodeId))
            this.formArg.put(dstNodeId, srcGraph.formArg.get(srcNodeId));
        if(srcGraph.formRet.containsKey(srcNodeId))
            this.formRet.put(dstNodeId, srcGraph.formRet.get(srcNodeId));
    }

}
