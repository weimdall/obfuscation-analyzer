/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.model;

import java.util.HashMap;
import java.util.Map;

public abstract class GraphIndexedVariable extends Graph {
    protected Map<Integer, Integer>         scopeIdTable        = new HashMap<>();
    protected Map<Integer, Integer>         varIdTable          = new HashMap<>();
    protected Map<Integer, Integer>         varScopeTable       = new HashMap<>();

    public GraphIndexedVariable() {super();}
    public GraphIndexedVariable(GraphIndexedVariable copyFrom) {
        super(copyFrom);
        this.scopeIdTable       = new HashMap<>(copyFrom.scopeIdTable);
        this.varIdTable         = new HashMap<>(copyFrom.varIdTable);
        this.varScopeTable      = new HashMap<>(copyFrom.varScopeTable);
    }

    @Override
    public void deleteNode(Integer id) {
        scopeIdTable.remove(id);
        varIdTable.remove(id);
        varScopeTable.remove(id);
        super.deleteNode(id);
    }

    public void copyNode(Integer dstNodeId, GraphIndexedVariable srcGraph, Integer srcNodeId) {
        super.copyNode(dstNodeId, srcGraph, srcNodeId);
        if(srcGraph.getScopeId(srcNodeId) != null)
            this.setScopeId(dstNodeId, srcGraph.getScopeId(srcNodeId));
        if(srcGraph.getVarId(srcNodeId) != null)
            this.setVarId(dstNodeId, srcGraph.getVarId(srcNodeId));
        if(srcGraph.getVarScope(srcNodeId) != null)
            this.setVarScope(dstNodeId, srcGraph.getVarScope(srcNodeId));
    }

    public Integer getScopeId(int id) {
        return scopeIdTable.getOrDefault(id, null);
    }
    public Integer getVarId(int id) {
        return varIdTable.getOrDefault(id, null);
    }
    public Integer getVarScope(int id) {
        return varScopeTable.getOrDefault(id, null);
    }

    public Integer setScopeId(Integer nodeId, Integer scopeId) {
        return scopeIdTable.put(nodeId, scopeId);
    }
    public Integer setVarId(Integer nodeId, Integer varId) {
        return varIdTable.put(nodeId, varId);
    }
    public Integer setVarScope(Integer nodeId, Integer scopeId) {
        return varScopeTable.put(nodeId, scopeId);
    }
}
