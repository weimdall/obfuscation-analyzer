/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.model;

import org.polymtl.codeanalysis.exceptions.ASTJsonException;

import java.util.HashMap;
import java.util.Map;

public class AST extends GraphIndexedVariable {
    protected Map<Integer, Integer> astNodePtr = new HashMap<Integer, Integer>();
    protected Map<Integer, Integer> ptNodePtr = new HashMap<Integer, Integer>();

    public AST(){super();}
    public AST(AST copyFrom){
        super(copyFrom);
        this.astNodePtr        = new HashMap<>(copyFrom.astNodePtr);
        this.ptNodePtr         = new HashMap<>(copyFrom.ptNodePtr);
    }

    public Integer getNodeASTPtr(Integer ptNodeId) {
        Integer retVal = astNodePtr.get(ptNodeId);
        if (retVal == null) {
            retVal = defsInt.UNDEF_VAL;
        }

        return (retVal);
    }
    public Integer getNodePTPtr(Integer astNodeId) {
        Integer retVal = ptNodePtr.get(astNodeId);
        if (retVal == null) {
            retVal = defsInt.UNDEF_VAL;
        }

        return (retVal);
    }
    public void setNodePtr(Integer ptNode, Integer astNode) {
        Integer oldVal = astNodePtr.put(ptNode, astNode);
        ptNodePtr.put(astNode, ptNode);
        if(astNode == defsInt.UNDEF_VAL)
            throw new ASTJsonException("Not allowed operation : pointer to undefined");
        if (oldVal != null)
            throw new ASTJsonException("Invalid multiple ast node ptr " +
                    ptNode + " for node " + astNode + " (old ptr: " + oldVal + ")");
    }

    @Override
    public void deleteNode(Integer astNodeId) {
        astNodePtr.remove(ptNodePtr.get(astNodeId));
        ptNodePtr.remove(astNodeId);
        super.deleteNode(astNodeId);
    }

    /*public void copyNode(Integer dstNodeId, AST srcGraph, Integer srcNodeId) {
        super.copyNode(dstNodeId, srcGraph, srcNodeId);
        if(srcGraph.ptNodePtr.containsKey(srcNodeId))
            this.setNodePtr(srcGraph.getNodePTPtr(srcNodeId), dstNodeId);
    }*/
}
