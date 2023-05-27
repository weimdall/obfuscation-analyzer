/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.util;

import org.polymtl.codeanalysis.model.AST;

import java.util.List;

public class ASTPatternMatcher {



    public static boolean matchPattern(AST source, AST pattern) {
        return matchSubTree(source.getRoot(), pattern.getRoot(), source, pattern);

        /*for(Integer nodeId : source.getTypeTable().keySet())
            if(matchSubTree(nodeId, pattern.getRoot(), source, pattern))
                return true;
        return false;*/
    }

    public static boolean matchPattern(Integer sourceNodeId, AST source, AST pattern) {
        return matchSubTree(sourceNodeId, pattern.getRoot(), source, pattern);

        /*for(Integer nodeId : source.getTypeTable().keySet())
            if(matchSubTree(nodeId, pattern.getRoot(), source, pattern))
                return true;
        return false;*/
    }


    public static boolean matchSubTree(Integer sourceNodeId, Integer patternNodeId, AST source, AST pattern) {
        if(pattern.getType(patternNodeId).equals("*"))
            return true;

        if(pattern.getType(patternNodeId).toString().endsWith("*")) { // End with *
            String type = pattern.getType(patternNodeId).toString();
            type = type.substring(0, type.length() - 1);
            if(!source.getType(sourceNodeId).toString().startsWith(type))
                return false;
        }
        else if(pattern.getType(patternNodeId).toString().startsWith("*")) { // Start with *
            String type = pattern.getType(patternNodeId).toString();
            type = type.substring(1);
            if(!source.getType(sourceNodeId).toString().endsWith(type))
                return false;
        }
        else if(!source.getType(sourceNodeId).equals(pattern.getType(patternNodeId)))
            return false;

        if(pattern.getImage(patternNodeId) != null) {
            if (source.getImage(sourceNodeId) == null)
                return false;
            else if (pattern.getImage(patternNodeId).toString().endsWith("*")) { // End with *
                String img = pattern.getImage(patternNodeId).toString();
                img = img.substring(0, img.length() - 1);
                if (!source.getImage(sourceNodeId).toString().startsWith(img))
                    return false;
            } else if (pattern.getImage(patternNodeId).toString().startsWith("*")) { // Start with *
                String img = pattern.getImage(patternNodeId).toString();
                img = img.substring(1);
                if (!source.getImage(sourceNodeId).toString().endsWith(img))
                    return false;
            } else if (!source.getImage(sourceNodeId).equals(pattern.getImage(patternNodeId)))
                return false;
        }

        List<Integer> childrenSource = source.getChildren(sourceNodeId);
        List<Integer> childrenPattern = pattern.getChildren(patternNodeId);

        if(childrenSource == null ^ childrenPattern == null)
            return false;

        if(childrenSource != null) {
            if (childrenSource.size() != childrenPattern.size())
                return false;

            for (int i = 0; i < childrenSource.size(); i++) {
                if (!matchSubTree(childrenSource.get(i), childrenPattern.get(i), source, pattern))
                    return false;
            }
        }

        return true;
    }





}
