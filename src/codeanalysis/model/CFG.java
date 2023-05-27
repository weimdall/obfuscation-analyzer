/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.model;

import org.polymtl.codeanalysis.exceptions.ASTJsonException;
import org.polymtl.codeanalysis.util.CustomLogger;
import org.polymtl.codeanalysis.util.Pair;

import java.util.*;
import java.util.logging.Logger;

public class CFG extends GraphIndexedVariable {
    private static final Logger LOGGER = CustomLogger.getLogger(CFG.class.getName());
    protected Map<Integer, Integer> callBegins = new HashMap<Integer, Integer>();
    protected Map<Integer, Integer> callEnds = new HashMap<Integer, Integer>();
    protected Map<Integer, Integer> callExpr = new HashMap<Integer, Integer>();
    protected Map<Integer, Pair<Integer, Integer>> opHands = new HashMap<>();
    protected Map<Integer, Integer> astNodePtr = new HashMap<Integer, Integer>();
    protected Map<Integer, Integer> cfgNodePtr = new HashMap<Integer, Integer>();

    protected Map<Integer, String> funcEntryNode = new HashMap<>();
    protected Map<Integer, List<Integer>> funcCallArgList = new HashMap<>();
    protected Map<Integer, List<Integer>> funcDefParamList = new HashMap<>();

    public CFG(){super();}
    public CFG(CFG copyFrom){
        super(copyFrom);
        this.callBegins         = new HashMap<>(copyFrom.callBegins);
        this.callEnds           = new HashMap<>(copyFrom.callEnds);
        this.callExpr           = new HashMap<>(copyFrom.callExpr);
        this.opHands            = new HashMap<>(copyFrom.opHands);
        this.astNodePtr         = new HashMap<>(copyFrom.astNodePtr);
        this.cfgNodePtr         = new HashMap<>(copyFrom.cfgNodePtr);
        this.funcEntryNode      = new HashMap<>(copyFrom.funcEntryNode);
        this.funcCallArgList    = new HashMap<>(copyFrom.funcCallArgList);
        this.funcDefParamList   = new HashMap<>(copyFrom.funcDefParamList);
    }

    public Integer getNodeCfgPtr(Integer astNodeId) {
        Integer retVal = astNodePtr.get(astNodeId);
        return (retVal == null) ? defsInt.UNDEF_VAL : retVal;
    }

    public Integer getNodeAstPtr(Integer cfgNodeId) {
        Integer retVal = cfgNodePtr.get(cfgNodeId);
        return (retVal == null) ? defsInt.UNDEF_VAL : retVal;
    }

    public List<Integer> getAstChildren(Integer astNodeId) {
        List<Integer> astChildren = new ArrayList<>();
        Integer cfgNodeId = getNodeCfgPtr(astNodeId);
        if(cfgNodeId.equals(defsInt.UNDEF_VAL))
            return null;
        List<Integer> children = getChildren(cfgNodeId);
        if (children != null) {
            for(int i = 0 ; i < children.size() ; i++) {
                if(getNodeAstPtr(children.get(i)) != defsInt.UNDEF_VAL) {
                    astChildren.add(getNodeAstPtr(children.get(i)));
                    continue;
                }
                if(getChildren(children.get(i)).size() > 0)
                    children.addAll(getChildren(children.get(i)));
                else if(getCallEnd(children.get(i))!= null)
                    children.addAll(Collections.singleton(getCallEnd(children.get(i))));
            }
        }
        return astChildren;
    }

    public Integer getCallEnd(Integer nodeId) {
        return callEnds.get(nodeId);
    }
    public Integer getCallBegin(Integer nodeId) {
        return callBegins.get(nodeId);
    }
    public void addCall(Integer node1, Integer node2) {
        callEnds.put(node1, node2);
        callBegins.put(node2, node1);
    }
    public Integer getCallExpr(Integer nodeId) {
        return callExpr.get(nodeId);
    }
    public void addCallExpr(Integer node1, Integer node2) {
        callExpr.put(node1, node2);
    }
    public Pair<Integer, Integer> getOpHands(Integer astNodeId) {
        return opHands.get(astNodeId);
    }
    public Pair<Integer, Integer> setOpHands(Integer nodeId, Pair<Integer, Integer> pair) {
        return opHands.put(nodeId, pair);
    }

    public void setNodePtr(Integer astNodeId, Integer cfgNodeId) {
        Integer oldVal = astNodePtr.put(astNodeId, cfgNodeId);
        cfgNodePtr.put(cfgNodeId, astNodeId);
        if (oldVal != null)
            throw new ASTJsonException("Invalid multiple ast node ptr " +
                    astNodeId + " for node " + cfgNodeId + " (old ptr: " + oldVal + ")");
    }

    @Override
    public void deleteNode(Integer node) {
        cfgNodePtr.remove(astNodePtr.get(node));
        astNodePtr.remove(node);
        opHands.remove(node);
        callBegins.remove(node);
        callBegins.remove(callEnds.get(node));
        callEnds.remove(node);
        callEnds.remove(callBegins.get(node));
        callExpr.remove(node);
        funcCallArgList.remove(node);
        funcDefParamList.remove(node);
        super.deleteNode(node);
    }

    public void copyNode(Integer dstNodeId, CFG srcGraph, Integer srcNodeId) {
        super.copyNode(dstNodeId, srcGraph, srcNodeId);
        if(srcGraph.cfgNodePtr.containsKey(srcNodeId))
            this.cfgNodePtr.put(dstNodeId, srcGraph.cfgNodePtr.get(srcNodeId));
        if(srcGraph.astNodePtr.containsKey(srcNodeId))
            this.astNodePtr.put(dstNodeId, srcGraph.astNodePtr.get(srcNodeId));
        if(srcGraph.opHands.containsKey(srcNodeId))
            this.opHands.put(dstNodeId, new Pair<>(srcGraph.opHands.get(srcNodeId).getKey(), srcGraph.opHands.get(srcNodeId).getValue()));
        if(srcGraph.callBegins.containsKey(srcNodeId))
            this.callBegins.put(dstNodeId, srcGraph.callBegins.get(srcNodeId));
        if(srcGraph.callEnds.containsKey(srcNodeId))
            this.callEnds.put(dstNodeId, srcGraph.callEnds.get(srcNodeId));
        if(srcGraph.callExpr.containsKey(srcNodeId))
            this.callExpr.put(dstNodeId, srcGraph.callExpr.get(srcNodeId));
        if(srcGraph.funcEntryNode.containsKey(srcNodeId))
            this.funcEntryNode.put(dstNodeId, srcGraph.funcEntryNode.get(srcNodeId));
        if(srcGraph.funcDefParamList.containsKey(srcNodeId))
            this.funcDefParamList.put(dstNodeId, new ArrayList<>(srcGraph.funcDefParamList.get(srcNodeId)));
        if(srcGraph.funcCallArgList.containsKey(srcNodeId))
            this.funcCallArgList.put(dstNodeId, new ArrayList<>(srcGraph.funcCallArgList.get(srcNodeId)));
    }

    public Integer importSubGraph(CFG srcGraph, Integer rootId) {
        return importSubGraph(srcGraph, rootId, new ArrayList<>());
    }
    private Integer importSubGraph(CFG srcGraph, Integer rootId, List<Integer> imported) {
        if(imported.contains(rootId))
            return null;
        imported.add(rootId);
        Integer newNode = rootId;
        if(this.getType(newNode) != null) {
            LOGGER.finer("Create new node while importing a graph"); // Cause link ast_ptr to break - dd may fail
            newNode = this.getNextNodeId();
        }
        this.copyNode(newNode, srcGraph, rootId);
        List<Integer> children = new ArrayList<>(srcGraph.getChildren(rootId));
        for (Integer child : children) {
            Integer newChild = this.importSubGraph(srcGraph, child, imported);
            if(newChild != null)
                this.addEdge(newNode, newChild);
        }

        if(srcGraph.getCallEnd(rootId) != null) {
            Integer newChild = this.importSubGraph(srcGraph, srcGraph.getCallEnd(rootId), imported);
            if(newChild != null)
                this.addCall(newNode, newChild);
        }
        return newNode;
    }

    public List<Integer> getAnyParent(int id) {
        List<Integer> parents = new ArrayList<>(getParent(id));
        if(getCallBegin(id) != null)
            parents.add(getCallBegin(id));
        return parents;
    }

    public List<Integer> getAnyChildren(int id) {
        List<Integer> children = new ArrayList<>(getChildren(id));
        if(getCallEnd(id) != null)
            children.add(getCallEnd(id));
        return children;
    }


    public void addFuncCallArg(Integer node, Integer argNode) {
        if(!funcCallArgList.containsKey(node))
            funcCallArgList.put(node, new ArrayList<>());
        funcCallArgList.get(node).add(argNode);
    }

    public List<Integer> getFuncCallArgs(Integer node) {
        return funcCallArgList.get(node);
    }

    public void addFuncDefParam(Integer node, Integer paramNode) {
        if(!funcDefParamList.containsKey(node))
            funcDefParamList.put(node, new ArrayList<>());
        funcDefParamList.get(node).add(paramNode);
    }

    public List<Integer> getFuncDefParam(Integer node) {
        return funcDefParamList.get(node);
    }

    public void addFuncEntryNode(Integer cfgNodeId, String funcName) {
        String oldVal = funcEntryNode.put(cfgNodeId, funcName);
        if (oldVal != null)
            throw new ASTJsonException("Invalid multiple cfg node entry for func " + funcName + " (old ptr: " + oldVal + ")");
    }
    public Integer getFuncEntryNode(String funcName) {
        for (Map.Entry<Integer, String> entry : funcEntryNode.entrySet()) {
            if (entry.getValue().equals(funcName)) {
                return entry.getKey();
            }
        }
        return null;
    }
    public String getEntryFuncName(Integer node) {
        return funcEntryNode.get(node);
    }
    public List<Integer> getAllFuncEntryNode() {
        return new ArrayList<Integer>(funcEntryNode.keySet());
    }
    public List<String> getAllFuncName() {
        return new ArrayList<String>(funcEntryNode.values());
    }

}
