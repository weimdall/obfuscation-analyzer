package org.polymtl.codeanalysis.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Copyright (C) 2021 Cassagne Julien - All rights reserved
 */

public class ASTMatcher<nodeIndexType, matchValType, matchTokType> extends ASTNavigatorCl<nodeIndexType, matchValType> {

    private Map<nodeIndexType, matchTokType> astTokenTable = null;


    public ASTMatcher(Map<nodeIndexType, List<nodeIndexType>> parAdjTable,
                      Map<nodeIndexType, matchValType> parTypeTable,
                      Map<nodeIndexType, matchTokType> parTokenTable) {
        super(parAdjTable, parTypeTable);
        astTokenTable = parTokenTable;
    }

    public nodeIndexType matchPattern(nodeIndexType parRoot,
                                Map<nodeIndexType, List<nodeIndexType>> parAdjTable,
                                Map<nodeIndexType, matchValType> parTypeTable,
                                Map<nodeIndexType, matchTokType> parTokenTable) {
        for(nodeIndexType nodeId : astTypeTable.keySet()) {
            if(matchSubTree(nodeId, parRoot, parAdjTable, parTypeTable, parTokenTable))
                return nodeId;
        }
        return null;
    }

    public boolean matchSubTree(nodeIndexType node,
                                nodeIndexType parRoot,
                                Map<nodeIndexType, List<nodeIndexType>> parAdjTable,
                                Map<nodeIndexType, matchValType> parTypeTable,
                                Map<nodeIndexType, matchTokType> parTokenTable) {
        if(parTypeTable.get(parRoot).equals("*"))
            return true;

        if(parTypeTable.get(parRoot).toString().endsWith("*")) { // End with *
            String type = parTypeTable.get(parRoot).toString();
            type = type.substring(0, type.length() - 1);
            if(astTypeTable.get(node).toString().startsWith(type))
                return true;
        }

        if(parTypeTable.get(parRoot).toString().endsWith("*")) { // Start with *
            String type = parTypeTable.get(parRoot).toString();
            type = type.substring(1);
            if(astTypeTable.get(node).toString().endsWith(type))
                return true;
        }

        if(!astTypeTable.get(node).equals(parTypeTable.get(parRoot)))
            return false;

        if(parTokenTable.get(parRoot) != null && astTokenTable.get(node) != null &&
                !astTokenTable.get(node).equals(parTokenTable.get(parRoot)))
            return false;

        if(parTypeTable.get(parRoot).equals("PostfixExpression"))
            return true;

        List<nodeIndexType> children = getChildren(node);
        List<nodeIndexType> childrenPattern = getChildren(parRoot, parAdjTable);

        if(children == null ^ childrenPattern == null)
            return false;

        if(children != null) {
            if (children.size() != childrenPattern.size())
                return false;

            for (int i = 0; i < children.size(); i++) {
                if (!matchSubTree(children.get(i), childrenPattern.get(i), parAdjTable, parTypeTable, parTokenTable))
                    return false;
            }
        }

        return true;
    }

    public List<nodeIndexType> getChildren(nodeIndexType node, Map<nodeIndexType, List<nodeIndexType>> parAdjTable) {
        return parAdjTable.get(getIndexedNode(node, Collections.emptyList(), parAdjTable));
    }





}
