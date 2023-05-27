/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.util;

import org.polymtl.codeanalysis.model.ASTDynamic;

import java.util.List;

public class ASTDynMatcher {

    public enum TYPE {
        IDENTICAL,
        PARAMETRIC,
        DIFFERENT
    }
    protected TYPE type = null;

    public TYPE match(ASTDynamic ast1, ASTDynamic ast2) {
        this.type = TYPE.IDENTICAL;
        if(!matchSubTree(ast1.getRoot(), ast2.getRoot(), ast1, ast2))
            this.type = TYPE.DIFFERENT;
        return this.type;
    }

    protected boolean matchSubTree(Integer ast1NodeId, Integer ast2NodeId, ASTDynamic ast1, ASTDynamic ast2) {

        if(!ast1.getType(ast1NodeId).equals(ast2.getType(ast2NodeId)))
            return false;

        if(this.type == TYPE.IDENTICAL) {
            if (ast1.getImage(ast1NodeId) == null ^ ast2.getImage(ast2NodeId) == null)
                this.type=TYPE.PARAMETRIC;

            if (ast1.getImage(ast1NodeId) != null)
                if(!ast1.getImage(ast1NodeId).equals(ast2.getImage(ast2NodeId)))
                    this.type=TYPE.PARAMETRIC;
        }

        if(!matchChildren(ast1.getChildren(ast1NodeId),          ast2.getChildren(ast2NodeId),           ast1, ast2))
            return false;
        if(!matchChildren(ast1.getDynamicResolution(ast1NodeId), ast2.getDynamicResolution(ast2NodeId),  ast1, ast2))
            return false;
        if(!matchChildren(ast1.getDataflowResolution(ast1NodeId),ast2.getDataflowResolution(ast2NodeId), ast1, ast2))
            return false;
        if(!matchChildren(ast1.getParseEdge(ast1NodeId),         ast2.getParseEdge(ast2NodeId),          ast1, ast2))
            return false;

        return true;
    }

    private boolean matchChildren(List<Integer> children1, List<Integer> children2, ASTDynamic ast1, ASTDynamic ast2) {
        if(children1 == null ^ children2 == null)
            return false;
        if(children1 != null) {
            if (children1.size() != children2.size())
                return false;
            for (int i = 0; i < children1.size(); i++)
                if (!matchSubTree(children1.get(i), children2.get(i), ast1, ast2))
                    return false;
        }
        return true;
    }
}
