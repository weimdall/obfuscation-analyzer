/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.model;

import org.polymtl.codeanalysis.exceptions.ASTJsonException;

import java.util.*;

public class ASTDynamic extends AST {
    protected Map<Integer, Set<Integer>>    nodePatternsMatched     = new HashMap<>();
    protected Map<Integer, String>          nodeStringResolved      = new HashMap<>();
    protected Map<Integer, Integer>         dataDependency          = new HashMap<>(); // Keep track of dd through replacement
    protected Map<Integer, List<Integer>>   edgeDynamicResolution   = new HashMap<>();
    protected Map<Integer, List<Integer>>   edgeParse               = new HashMap<>();
    protected Map<Integer, List<Integer>>   edgeDataflowResolution  = new HashMap<>();
    protected Map<Integer, List<Integer>>   edgeDynamicResolutionRev = new HashMap<>();
    protected Map<Integer, List<Integer>>   edgeParseRev             = new HashMap<>();
    protected Map<Integer, List<Integer>>   edgeDataflowResolutionRev = new HashMap<>();

    public ASTDynamic(){super();}
    public ASTDynamic(AST copyFrom){super(copyFrom);}
    public ASTDynamic(ASTDynamic copyFrom){
        super(copyFrom);
        this.nodePatternsMatched    = new HashMap<>(copyFrom.nodePatternsMatched);
        this.nodeStringResolved        = new HashMap<>(copyFrom.nodeStringResolved);
        this.dataDependency        = new HashMap<>(copyFrom.dataDependency);

        for(Integer nodeId : copyFrom.edgeDynamicResolution.keySet())
            this.edgeDynamicResolution.put(nodeId, new ArrayList<>(copyFrom.edgeDynamicResolution.get(nodeId)));
        for(Integer nodeId : copyFrom.edgeDataflowResolution.keySet())
            this.edgeDataflowResolution.put(nodeId, new ArrayList<>(copyFrom.edgeDataflowResolution.get(nodeId)));
        for(Integer nodeId : copyFrom.edgeParse.keySet())
            this.edgeParse.put(nodeId, new ArrayList<>(copyFrom.edgeParse.get(nodeId)));
        for(Integer nodeId : copyFrom.edgeDynamicResolutionRev.keySet())
            this.edgeDynamicResolutionRev.put(nodeId, new ArrayList<>(copyFrom.edgeDynamicResolutionRev.get(nodeId)));
        for(Integer nodeId : copyFrom.edgeDataflowResolutionRev.keySet())
            this.edgeDataflowResolutionRev.put(nodeId, new ArrayList<>(copyFrom.edgeDataflowResolutionRev.get(nodeId)));
        for(Integer nodeId : copyFrom.edgeParseRev.keySet())
            this.edgeParseRev.put(nodeId, new ArrayList<>(copyFrom.edgeParseRev.get(nodeId)));
   }

    public Set<Integer> getPatternsMatched(Integer nodeId) {
        return nodePatternsMatched.getOrDefault(nodeId, null);
    }
    public void addPatternMatched(Integer nodeId, Integer patternId) {/// TODO: If parent has several children -> new inter node
        if(!nodePatternsMatched.containsKey(nodeId))
            nodePatternsMatched.put(nodeId, new HashSet<>());
        nodePatternsMatched.get(nodeId).add(patternId);
    }

    public Map<Integer, Integer> getDataDependency() {
        return dataDependency;
    }
    public Integer getDataDependency(Integer nodeId) {
        return dataDependency.get(nodeId);
    }

    public List<Integer> getDynamicResolution(Integer nodeId) {
        return edgeDynamicResolution.getOrDefault(nodeId, new ArrayList<>());
    }
    public List<Integer> getDynamicResolutionRev(Integer nodeId) {
        return edgeDynamicResolutionRev.getOrDefault(nodeId, new ArrayList<>());
    }
    public Integer addDynamicResolution(Integer nodeId, AST ast) { return  this.addDynamicResolution(nodeId, ast, ast.getRoot()); }
    public Integer addDynamicResolution(Integer nodeId, AST ast, Integer rootId) { return this.addDynamicResolution(nodeId, new ASTDynamic(ast), rootId); }
    public Integer addDynamicResolution(Integer nodeId, ASTDynamic ast) { return this.addDynamicResolution(nodeId, ast, ast.getRoot()); }
    public Integer addDynamicResolution(Integer nodeId, ASTDynamic ast, Integer rootId) {
        Integer newNode = this.importSubTree(ast, rootId);
        addDynamicResolution(nodeId, newNode);
        return newNode;
    }
    public void addDynamicResolution(Integer parentNodeId, Integer childNodeId) {
        if(parentNodeId == null || childNodeId == null)
            return;
        if(parentNodeId == defsInt.UNDEF_VAL || childNodeId == defsInt.UNDEF_VAL)
            throw new ASTJsonException("Undefined edge.");
        Graph.addEdge(childNodeId, parentNodeId, edgeDynamicResolution);
        Graph.addEdge(parentNodeId, childNodeId, edgeDynamicResolutionRev);
    }

    public List<Integer> getDataflowResolution(Integer nodeId) {
        return edgeDataflowResolution.getOrDefault(nodeId, new ArrayList<>());
    }
    public List<Integer> getDataflowResolutionRev(Integer nodeId) {
        return edgeDataflowResolutionRev.getOrDefault(nodeId, new ArrayList<>());
    }
    public Integer addDataflowResolution(Integer nodeId, AST ast) { return this.addDataflowResolution(nodeId, ast, ast.getRoot()); }
    public Integer addDataflowResolution(Integer nodeId, AST ast, Integer rootId) { return this.addDataflowResolution(nodeId, new ASTDynamic(ast), rootId); }
    public Integer addDataflowResolution(Integer nodeId, ASTDynamic ast) { return this.addDataflowResolution(nodeId, ast, ast.getRoot()); }
    public Integer addDataflowResolution(Integer nodeId, ASTDynamic ast, Integer rootId) {
        Integer newNode = this.importSubTree(ast, rootId);
        addDataflowResolution(nodeId, newNode);
        if(!rootId.equals(newNode))
            dataDependency.put(rootId, newNode);
        return newNode;
    }
    public void addDataflowResolution(Integer parentNodeId, Integer childNodeId) {
        if(parentNodeId == null || childNodeId == null)
            return;
        if(parentNodeId == defsInt.UNDEF_VAL || childNodeId == defsInt.UNDEF_VAL)
            throw new ASTJsonException("Undefined edge.");
        Graph.addEdge(childNodeId, parentNodeId, edgeDataflowResolution);
        Graph.addEdge(parentNodeId, childNodeId, edgeDataflowResolutionRev);
    }

    public List<Integer> getParseEdge(Integer nodeId) {
        return edgeParse.getOrDefault(nodeId, new ArrayList<>());
    }
    public List<Integer> getParseEdgeRev(Integer nodeId) {
        return edgeParseRev.getOrDefault(nodeId, new ArrayList<>());
    }
    public Integer addParseEdge(Integer nodeId, AST ast) { return this.addParseEdge(nodeId, ast, ast.getRoot()); }
    public Integer addParseEdge(Integer nodeId, AST ast, Integer rootId) { return this.addParseEdge(nodeId, new ASTDynamic(ast), rootId); }
    public Integer addParseEdge(Integer nodeId, ASTDynamic ast) { return this.addParseEdge(nodeId, ast, ast.getRoot()); }
    public Integer addParseEdge(Integer nodeId, ASTDynamic ast, Integer rootId) {
        Integer newNode = this.importSubTree(ast, rootId);
        addParseEdge(nodeId, newNode);
        return newNode;
    }
    public void addParseEdge(Integer parentNodeId, Integer childNodeId) {
        if(parentNodeId == null || childNodeId == null)
            return;
        if(parentNodeId == defsInt.UNDEF_VAL || childNodeId == defsInt.UNDEF_VAL)
            throw new ASTJsonException("Undefined edge.");
        Graph.addEdge(childNodeId, parentNodeId, edgeParse);
        Graph.addEdge(parentNodeId, childNodeId, edgeParseRev);
    }

    public String getEvalString(Integer nodeId) {
        return nodeStringResolved.getOrDefault(nodeId, null);
    }
    public void setEvalString(Integer nodeId, String str) {
        nodeStringResolved.put(nodeId, str);
    }

    public Integer getFirstParent(int id) {
        List<Integer> parentList = predTable.getOrDefault(id, null);
        if(parentList == null)
            parentList = edgeDynamicResolutionRev.getOrDefault(id, null);
        if(parentList == null)
            parentList = edgeDataflowResolutionRev.getOrDefault(id, null);
        if(parentList == null && dataDependency.containsKey(id))
            return getFirstParent(dataDependency.get(id));

        return (parentList == null) ? null : parentList.get(0);
    }

    public void deleteSubTree(Integer nodeId) {
        List<Integer> children = new ArrayList<>(this.getDynamicResolution(nodeId));
        for (Integer child : children)
            this.deleteSubTree(child);

        children = new ArrayList<>(this.getDataflowResolution(nodeId));
        for (Integer child : children)
            this.deleteSubTree(child);

        children = new ArrayList<>(this.getParseEdge(nodeId));
        for (Integer child : children)
            this.deleteSubTree(child);

        children = new ArrayList<>(this.getChildren(nodeId));
        for (Integer child : children)
            this.deleteSubTree(child);

        this.deleteNode(nodeId);
    }

    public Integer importSubTree(ASTDynamic srcGraph, Integer rootId) {
        Integer newNode = this.getNextNodeId();
        this.copyNode(newNode, srcGraph, rootId);
        List<Integer> children = new ArrayList<>(srcGraph.getChildren(rootId));
        for (Integer child : children) {
            Integer newChild = this.importSubTree(srcGraph, child);
            this.addEdge(newNode, newChild);
        }

        children = new ArrayList<>(srcGraph.getDynamicResolution(rootId));
        for (Integer child : children)
            this.addDynamicResolution(newNode, srcGraph, child);

        children = new ArrayList<>(srcGraph.getDataflowResolution(rootId));
        for (Integer child : children)
            this.addDataflowResolution(newNode, srcGraph, child);

        children = new ArrayList<>(srcGraph.getParseEdge(rootId));
        for (Integer child : children)
            this.addParseEdge(newNode, srcGraph, child);

        return newNode;
    }

    @Override
    public void deleteNode(Integer astNodeId) {
        nodePatternsMatched.remove(astNodeId);
        nodeStringResolved.remove(astNodeId);
        dataDependency.remove(astNodeId);

        if(edgeDynamicResolution.get(astNodeId) != null)
            for(Integer node : edgeDynamicResolution.get(astNodeId))
                edgeDynamicResolutionRev.get(node).remove(astNodeId);
        edgeDynamicResolution.remove(astNodeId);

        if(edgeDynamicResolutionRev.get(astNodeId) != null)
            for(Integer node : edgeDynamicResolutionRev.get(astNodeId))
                edgeDynamicResolution.get(node).remove(astNodeId);
        edgeDynamicResolutionRev.remove(astNodeId);

        if(edgeDataflowResolution.get(astNodeId) != null)
            for(Integer node : edgeDataflowResolution.get(astNodeId))
                edgeDataflowResolutionRev.get(node).remove(astNodeId);
        edgeDataflowResolution.remove(astNodeId);

        if(edgeDataflowResolutionRev.get(astNodeId) != null)
            for(Integer node : edgeDataflowResolutionRev.get(astNodeId))
                edgeDataflowResolution.get(node).remove(astNodeId);
        edgeDataflowResolutionRev.remove(astNodeId);

        if(edgeParse.get(astNodeId) != null)
            for(Integer node : edgeParse.get(astNodeId))
                edgeParseRev.get(node).remove(astNodeId);
        edgeParse.remove(astNodeId);

        if(edgeParseRev.get(astNodeId) != null)
            for(Integer node : edgeParseRev.get(astNodeId))
                edgeParse.get(node).remove(astNodeId);
        edgeParseRev.remove(astNodeId);
        
        super.deleteNode(astNodeId);
    }

    public void copyNode(Integer dstNodeId, ASTDynamic srcGraph, Integer srcNodeId) {
        super.copyNode(dstNodeId, srcGraph, srcNodeId);
        if(srcGraph.nodePatternsMatched.containsKey(srcNodeId))
            this.nodePatternsMatched.put(dstNodeId, new HashSet<>(srcGraph.nodePatternsMatched.get(srcNodeId)));
        if(srcGraph.nodeStringResolved.containsKey(srcNodeId))
            this.nodeStringResolved.put(dstNodeId, srcGraph.nodeStringResolved.get(srcNodeId));
        if(srcGraph.dataDependency.containsKey(srcNodeId))
            this.dataDependency.put(dstNodeId, srcGraph.dataDependency.get(srcNodeId));
    }
}
