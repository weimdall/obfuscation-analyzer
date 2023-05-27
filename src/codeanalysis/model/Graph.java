/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.model;

import org.polymtl.codeanalysis.exceptions.ASTJsonException;
import org.polymtl.codeanalysis.util.CustomLogger;

import java.util.*;
import java.util.logging.Logger;

public abstract class Graph implements defsInt {
    private static final Logger LOGGER = CustomLogger.getLogger(Graph.class.getName());

    protected String                        filename            = null;
    protected Integer                       rootId              = UNDEF_VAL;
    protected Map<Integer, List<Integer>>   predTable           = new HashMap<>();
    protected Map<Integer, List<Integer>>   succTable           = new HashMap<>();
    protected Map<Integer, String>          nodeTypeTable       = new HashMap<>();
    protected Map<Integer, String>          nodeImageTable      = new HashMap<>();
    protected Map<Integer, Integer[]>       nodePositionTable   = new HashMap<>();

    public Graph() {super();}
    public Graph(Graph copyFrom) {
        for(Integer nodeId : copyFrom.predTable.keySet())
            this.predTable.put(nodeId, new ArrayList<>(copyFrom.predTable.get(nodeId)));
        for(Integer nodeId : copyFrom.succTable.keySet())
            this.succTable.put(nodeId, new ArrayList<>(copyFrom.succTable.get(nodeId)));
        this.rootId             = copyFrom.rootId;
        this.filename           = copyFrom.filename;
        this.nodeTypeTable      = new HashMap<>(copyFrom.nodeTypeTable);
        this.nodeImageTable     = new HashMap<>(copyFrom.nodeImageTable);
        this.nodePositionTable  = new HashMap<>(copyFrom.nodePositionTable);
    }


    public Set<Integer> getNodeIds() {
        return nodeTypeTable.keySet();
    }
    public String getType(int id) {
        return nodeTypeTable.get(id);
    }
    public String getImage(int id) {
        return nodeImageTable.getOrDefault(id, null);
    }
    public List<Integer> getChildren(int id) {
        return succTable.getOrDefault(id, new ArrayList<>());
    }
    public List<Integer> getParent(int id) {
        return predTable.getOrDefault(id, new ArrayList<>());
    }
    public Integer getRoot() { return this.rootId; }
    public void setRoot(Integer rootId) { this.rootId = rootId; }
    public String getFilename() { return this.filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public Map<Integer, List<Integer>> getSuccTable() { return succTable;}
    public Map<Integer, List<Integer>> getPredTable() { return predTable;}
    public Map<Integer, String> getTypeTable() { return nodeTypeTable;}
    public Map<Integer, String> getImageTable() { return nodeImageTable;}
    public Integer getNextNodeId() {
        if(nodeImageTable.size() == 0)
            return 0;
        return Collections.max(nodeTypeTable.keySet())+1;
    }



    static void addEdge(Integer node1, Integer node2, Map<Integer, List<Integer>> table) {
        if(table.containsKey(node2)) {
            if (table.get(node2).contains(node1))
                throw new ASTJsonException("Duplicate ast edge ' : " + node2 + '-' + node1);
            table.get(node2).add(node1);
        }
        else {
            table.put(node2, new ArrayList<>());
            table.get(node2).add(node1);
        }
    }
    public void addEdge(Integer parentNodeId, Integer childNodeId) {
        if(parentNodeId == null || childNodeId == null)
            return;
        if(parentNodeId == defsInt.UNDEF_VAL || childNodeId == defsInt.UNDEF_VAL)
            throw new ASTJsonException("Undefined edge.");
        addEdge(childNodeId, parentNodeId, succTable);
        addEdge(parentNodeId, childNodeId, predTable);
    }
    public void removeEdge(Integer parentNodeId, Integer childNodeId) {
        if(parentNodeId == null || childNodeId == null)
            return;
        if(succTable.get(parentNodeId) == null || predTable.get(childNodeId) == null)
            return;
        succTable.get(parentNodeId).remove(childNodeId);
        predTable.get(childNodeId).remove(parentNodeId);
    }
    public Integer[] getPositions(int id) {
        return nodePositionTable.get(id);
    }
    public Integer getLeftBrother(Integer nodeId) {
        if(getParent(nodeId) == null)
            return null;
        Integer parent = getParent(nodeId).get(0);
        for(int i = 1 ; i < getChildren(parent).size() ; i++) {
            if(getChildren(parent).get(i).equals(nodeId))
                return getChildren(parent).get(i-1);
        }
        return null;
    }

    public void deleteNode(Integer astNodeId) {
        nodeTypeTable.remove(astNodeId);
        nodePositionTable.remove(astNodeId);
        nodeImageTable.remove(astNodeId);
        if(succTable.get(astNodeId) != null)
            for(Integer node : succTable.get(astNodeId))
                predTable.get(node).remove(astNodeId);
        succTable.remove(astNodeId);
        if(predTable.get(astNodeId) != null)
            for(Integer node : predTable.get(astNodeId))
                succTable.get(node).remove(astNodeId);
        predTable.remove(astNodeId);
    }

    public void deleteSubTree(Integer nodeId) {
        List<Integer> children = new ArrayList<>(this.getChildren(nodeId));
        for (Integer child : children)
            deleteSubTree(child);

        this.deleteNode(nodeId);
    }

    public void deleteSubTreeAbove(Integer nodeId) {
        List<Integer> parents = new ArrayList<>(this.getParent(nodeId));
        for (Integer parent : parents) {
            this.removeEdge(parent, nodeId);
            this.deleteSubTreeAbove(parent);
            this.deleteSubTree(parent);
        }

    }

    public Integer importSubGraph(Graph srcGraph, Integer rootId, Map<Integer, Integer> dd) {
        return importSubGraph(srcGraph, rootId, dd, new ArrayList<>());
    }
    public Integer importSubGraph(Graph srcGraph, Integer rootId, Map<Integer, Integer> dd, List<Integer> imported) {
        if(imported.contains(rootId))
            return null;
        imported.add(rootId);
        Integer newNode = rootId;
        if(this.getType(newNode) != null) {
            LOGGER.finer("Create new node while importing a tree"); // Cause link pt_ptr, ast_ptr to break - cfg may fail
            newNode = this.getNextNodeId();
            if(srcGraph.getType(rootId).equals("Variable")) { // Keep DD track
                if (dd != null && !rootId.equals(newNode))
                    dd.put(newNode, rootId);
                else
                    LOGGER.warning("Needs DD from " + newNode + " to " + rootId);
            }
        }
        this.copyNode(newNode, srcGraph, rootId);
        List<Integer> children = new ArrayList<>(srcGraph.getChildren(rootId));
        for (Integer child : children) {
            Integer newChild = this.importSubGraph(srcGraph, child, dd, imported);
            if(newChild != null)
                this.addEdge(newNode, newChild);
        }

        return newNode;
    }

    public void copyNode(Integer dstNodeId, Graph srcGraph, Integer srcNodeId) {
        this.setNodeType(dstNodeId, srcGraph.getType(srcNodeId));
        if(srcGraph.getImage(srcNodeId) != null)
            this.setNodeImage(dstNodeId, srcGraph.getImage(srcNodeId));
        if(srcGraph.getPositions(srcNodeId) != null)
            this.setNodePosition(dstNodeId, srcGraph.getPositions(srcNodeId));
    }

    public String setNodeType(Integer nodeId, String parType) {
        return nodeTypeTable.put(nodeId, parType);
    }
    public Integer[] setNodePosition(Integer nodeId, Integer[] pos) {
        return nodePositionTable.put(nodeId, pos);
    }
    public String setNodeImage(Integer nodeId, String token) {
        return nodeImageTable.put(nodeId, token);
    }
}
