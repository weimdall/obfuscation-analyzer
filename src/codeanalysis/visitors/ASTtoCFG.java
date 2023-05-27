/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 * @author Ettore Merlo <ettore.merlo@polymtl.ca>
 */

package org.polymtl.codeanalysis.visitors;

import org.polymtl.codeanalysis.exceptions.*;
import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ASTtoCFG {
    static class Context {
        /*private Integer nextId = 0;
        public Integer getNewContext() {
            return nextId++;
        }*/
        Integer stopId = null;
        Integer scope = null;
        Integer scopeEnd = null;
        Integer parent = null; //cfgPredId
        Integer startId = null; //cfgStartId
        Integer endId = null; //cfgEndId
        Integer deadId = null;
        Integer funcCallId = null;
        Integer lastCaseId = null;
        List<Integer> openCases = new ArrayList<>();
        Integer defaultCaseId = null;
        Integer switchId = null;
        List<Integer> catchesId = new ArrayList<>();
        public Context clone() {
            Context ctx = new Context();
            ctx.stopId = this.stopId;
            ctx.deadId = this.deadId;
            ctx.scope = this.scope;
            ctx.scopeEnd = this.scopeEnd;
            ctx.parent = this.parent;
            ctx.startId = null;
            ctx.endId = this.endId;
            ctx.funcCallId = this.funcCallId;
            ctx.lastCaseId = this.lastCaseId;
            ctx.openCases = new ArrayList<>(this.openCases);
            ctx.defaultCaseId = this.defaultCaseId;
            ctx.switchId = this.switchId;
            ctx.catchesId = new ArrayList<>(this.catchesId);
            return ctx;
        }
    }
    protected AST ast = null;
    protected CFG cfg = null;
    protected ASTNavigatorCl<Integer, String> navigator;

    protected static int iNextNode = 0;
    protected static int iNextSwitchValue= 0;
    protected static int iNextTernaryValue= 0;

    public void resetIdsGen() {
        iNextNode = 0;
    }


    public ASTtoCFG(AST ast) {
        this.ast        = ast;
        this.cfg        = new CFG();
        cfg.setFilename(ast.getFilename());
        cfg.setRoot(ast.getRoot());
        this.navigator  = new ASTNavigatorCl<>(ast.getSuccTable(), ast.getTypeTable());
    }


    private Integer getNewNode() {
        return ++iNextNode;
    }


    private void transferNodeType(Integer astNodeId, Integer cfgNodeId) {
        String curType = ast.getType(astNodeId);
        if (curType == null)
            throw new ASTJsonException("Missing node type for node " + astNodeId);

        String oldVal = cfg.setNodeType(cfgNodeId, curType);
        if (oldVal != null) {
            throw new ASTJsonException("ERROR: invalid multiple node type " +
                    curType + " for node " + cfgNodeId + " (old val: " + oldVal + "- new value: "+curType+")");
        }
    }

    private void transferNodePositions(Integer astNodeId, Integer cfgNodeId) {
        Integer[] pos = ast.getPositions(astNodeId);
        if (pos == null)
            return;

        Integer[] oldVal = cfg.setNodePosition(cfgNodeId, pos);
        if (oldVal != null) {
            throw new ASTJsonException("ERROR: invalid multiple node type " +
                    Arrays.toString(pos) + " for node " + cfgNodeId + " (old val: " + Arrays.toString(oldVal) + ")");
        }
    }

    private void transferNodeToken(Integer astNodeId, Integer cfgNodeId) {
        String curToken = ast.getImage(astNodeId);
        if (curToken != null) {
            String oldVal = cfg.setNodeImage(cfgNodeId, curToken);
            if (oldVal != null) {
                throw new ASTJsonException("ERROR: invalid multiple node type " +
                        curToken + " for node " + cfgNodeId + " (old val: " + oldVal + ")");
            }
        }
    }

    private void transferVarData(Integer astNodeId, Integer cfgNodeId) {
        Integer curVarId = ast.getVarId(astNodeId);
        if (curVarId != null) {
            Integer oldVal = cfg.setVarId(cfgNodeId, ast.getVarId(astNodeId));
            cfg.setVarScope(cfgNodeId, ast.getVarScope(astNodeId));
            if (oldVal != null) {
                throw new ASTJsonException("ERROR: invalid multiple var id " +
                        curVarId + " for node " + cfgNodeId + " (old val: " + oldVal + ")");
            }
        }
    }

    private void transferScopeId(Integer astNodeId, Integer cfgNodeId) {
        Integer curVarId = ast.getScopeId(astNodeId);
        if (curVarId != null) {
            Integer oldVal = cfg.setScopeId(cfgNodeId, ast.getScopeId(astNodeId));
            if (oldVal != null) {
                throw new ASTJsonException("ERROR: invalid multiple scope id " +
                        curVarId + " for node " + cfgNodeId + " (old val: " + oldVal + ")");
            }
        }
    }

    private boolean isEmptyBody(Integer astNodeId, Context ctx) {
        if(navigator.getChildrenSize(astNodeId) == 0)
            return true;
        if(navigator.matchChildrenTypes(astNodeId, Arrays.asList("Block"))) {
            if(navigator.getChildrenSize(navigator.getChild(astNodeId, 0)) == 0)
                return true;

            // Function defined in a block .. (if ?)
            // TODO: how to handle that properly ?
            if(navigator.matchChildrenTypes(navigator.getChild(astNodeId, 0), Arrays.asList("FunctionStatement"))) {
                visit(navigator.getChild(navigator.getChild(astNodeId, 0), 0), ctx);
                return true;
            }
        }
        return false;
    }

    public Integer visit_GENERIC(Integer astNodeId, Context ctx) {
        Integer cfgNodeId = getNewNode();
        cfg.setNodePtr(astNodeId, cfgNodeId);
        transferNodeType(astNodeId, cfgNodeId);
        transferNodeToken(astNodeId, cfgNodeId);
        transferNodePositions(astNodeId, cfgNodeId);
        transferScopeId(astNodeId, cfgNodeId);
        transferVarData(astNodeId, cfgNodeId);
        cfg.addEdge(ctx.parent, cfgNodeId);

        //Update endId if necessary
        ctx.endId = cfgNodeId;
        if(ctx.startId == null)
            ctx.startId = cfgNodeId;

        //Visit children
        if(navigator.getChildrenSize(astNodeId) != 0) {
            Context newCtx = ctx.clone();
            newCtx.parent = cfgNodeId;
            navigator.getChildren(astNodeId).forEach(i -> {
                visit(i, newCtx);
                if (newCtx.endId != null)
                    newCtx.parent = newCtx.endId;
            });
            ctx.endId = newCtx.endId;
        }
        return cfgNodeId;
    }

    public void visit_BLOCK(Integer astNodeId, Context ctx) {
        Context newCtx = ctx.clone();
        newCtx.endId = ctx.parent;
        newCtx.startId = null;
        for (int i = 0; i < navigator.getChildrenSize(astNodeId); i++) {
            visit(navigator.getChild(astNodeId, i), newCtx);
            /*if (newCtx.endId == null) // Remove dead code from graph
                break;*/
            newCtx.parent = newCtx.endId;
            if(ctx.startId == null)
                ctx.startId = newCtx.startId;
        }
        ctx.endId = newCtx.endId;
    }

    public void visit_BinOP(Integer astNodeId, Context ctx) {
        int n_children = navigator.getChildrenSize(astNodeId);
        if(n_children != 2)
            throw new ASTJsonException("ERROR: Binary OP ("+astNodeId+") needs two children.");
        Integer left, right;
        Integer cfgNodeId = getNewNode();
        cfg.setNodePtr(astNodeId, cfgNodeId);
        transferNodeType(astNodeId, cfgNodeId);
        transferNodeToken(astNodeId, cfgNodeId);
        transferNodePositions(astNodeId, cfgNodeId);

        Context newCtx = ctx.clone();
        newCtx.startId = null;
        if(Arrays.asList("=", "+=", "-=", "*=", "/=").contains(cfg.getImage(cfgNodeId))) {
            Integer astRightChild = ast.getChildren(astNodeId).get(1);
            visit(astRightChild, newCtx);
            right = (newCtx.endId == null) ? newCtx.startId : newCtx.endId;
            ctx.startId = newCtx.startId;

            newCtx.parent = newCtx.endId;
            newCtx.startId = null;
            visit(ast.getChildren(astNodeId).get(0), newCtx);
            left = (newCtx.endId == null) ? newCtx.startId : newCtx.endId;
            cfg.addEdge(newCtx.endId, cfgNodeId);
            ctx.endId = cfgNodeId;
        } else {
            visit(ast.getChildren(astNodeId).get(0), newCtx);
            left = (newCtx.endId == null) ? newCtx.startId : newCtx.endId;
            ctx.startId = newCtx.startId;

            newCtx.parent = newCtx.endId;
            newCtx.startId = null;
            visit(ast.getChildren(astNodeId).get(1), newCtx);
            right = (newCtx.endId == null) ? newCtx.startId : newCtx.endId;
            cfg.addEdge(newCtx.endId, cfgNodeId);
            ctx.endId = cfgNodeId;

            if (Arrays.asList("and", "&&", "or", "||").contains(cfg.getImage(cfgNodeId)))
                cfg.addEdge(left, cfgNodeId);
        }

        cfg.setOpHands(cfgNodeId, new Pair<>(left, right));
    }

    public void visit_ARRAYEXPRESSION(Integer astNodeId, Context ctx) {
        int n_children = navigator.getChildrenSize(astNodeId);
        if(n_children > 2)
            throw new ASTJsonException("ERROR: ArrayExpression ("+astNodeId+") needs less than two children.");
        Integer left, right;
        Integer cfgNodeId = getNewNode();
        cfg.setNodePtr(astNodeId, cfgNodeId);
        transferNodeType(astNodeId, cfgNodeId);
        transferNodeToken(astNodeId, cfgNodeId);
        transferNodePositions(astNodeId, cfgNodeId);

        Context newCtx = ctx.clone();
        newCtx.startId = null;

        visit(navigator.getChild(astNodeId, 0), newCtx);
        left = (newCtx.endId == null) ? newCtx.startId : newCtx.endId;
        ctx.startId = newCtx.startId;

        if(ast.getChildren(astNodeId).size() == 2) {
            newCtx.parent = newCtx.endId;
            newCtx.startId = null;
            visit(navigator.getChild(astNodeId, 1), newCtx);

            right = (newCtx.endId == null) ? newCtx.startId : newCtx.endId;

        }
        else
            right = null;

        cfg.addEdge(newCtx.endId, cfgNodeId);
        ctx.endId = cfgNodeId;
        cfg.setOpHands(cfgNodeId, new Pair<>(left, right));
    }

    public void visit_UnOP(Integer astNodeId, Context ctx) {
        int n_children = navigator.getChildrenSize(astNodeId);
        if(n_children != 1)
            throw new ASTJsonException("ERROR: A unary OP ("+astNodeId+") needs one child.");
        visit_OP(astNodeId, ctx);
    }

    public void visit_OP(Integer astNodeId, Context ctx) {
        Integer cfgNodeId = getNewNode();
        cfg.setNodePtr(astNodeId, cfgNodeId);
        transferNodeType(astNodeId, cfgNodeId);
        transferNodeToken(astNodeId, cfgNodeId);
        transferNodePositions(astNodeId, cfgNodeId);

        Context newCtx = ctx.clone();
        if(navigator.getChildrenSize(astNodeId) != 0)
            navigator.getChildren(astNodeId).forEach(i -> {
                visit(i, newCtx);
                if(ctx.startId == null)
                    ctx.startId = newCtx.startId;
                newCtx.parent = newCtx.endId;
            });

        ctx.endId = cfgNodeId;
        cfg.addEdge(newCtx.endId, cfgNodeId);
    }

    public void visit_ARRAYINIT(Integer astNodeId, Context ctx) {
        Integer cfgNodeId = getNewNode();
        cfg.setNodePtr(astNodeId, cfgNodeId);
        transferNodeType(astNodeId, cfgNodeId);
        transferNodeToken(astNodeId, cfgNodeId);
        transferNodePositions(astNodeId, cfgNodeId);

        Context newCtx = ctx.clone();
        if(navigator.getChildrenSize(astNodeId) != 0)
            navigator.getChildren(astNodeId).forEach(i -> {
                visit(i, newCtx);
                if(ctx.startId == null)
                    ctx.startId = newCtx.startId;
                newCtx.parent = newCtx.endId;
                cfg.addFuncCallArg(cfgNodeId, newCtx.endId);
            });

        ctx.endId = cfgNodeId;
        cfg.addEdge(newCtx.endId, cfgNodeId);
    }

    public void visit_CONDITION(Integer astNodeId, Context ctx) {
        if(navigator.getChildrenSize(astNodeId) != 1)
            throw new ASTJsonException("ERROR: Found a condition with more than one children");
        visit(navigator.getChild(astNodeId, 0), ctx);
        Integer cfgNodeId = getNewNode();
        transferNodeType(astNodeId, cfgNodeId);
        cfg.setNodePtr(astNodeId, cfgNodeId);
        cfg.addEdge(ctx.endId, cfgNodeId);
        ctx.endId = cfgNodeId;
    }

    public void visit_WHILE(Integer astNodeId, Context ctx) {
        boolean doWhile;
        if(ast.getType(astNodeId).equals("While"))
            doWhile = false;
        else if(ast.getType(astNodeId).equals("DoWhile"))
            doWhile = true;
        else
            throw new ASTJsonException("ERROR: Bad while statement (Not a While nor a DoWhile) : " + ast.getType(astNodeId));
        if(!navigator.matchChildrenTypes(astNodeId, Arrays.asList("Condition", "StatementBody"))) {
            ast.getChildren(astNodeId).forEach(i -> System.out.println(ast.getType(i)));
            throw new ASTJsonException("ERROR: Bad while statement (required Condition&StatementBody)");
        }

        Integer startNodeId = getNewNode();
        Integer endNodeId = getNewNode();
        Integer condNodeId = null;
        if(ctx.startId == null)
            ctx.startId = startNodeId;
        ctx.endId = endNodeId;
        cfg.setNodeType(endNodeId, ast.getType(astNodeId)+"End");
        transferNodeType(astNodeId, startNodeId);
        cfg.setNodePtr(astNodeId, startNodeId);
        cfg.addEdge(ctx.parent, startNodeId);

        Context newCtx = ctx.clone();
        newCtx.scope = startNodeId;
        newCtx.scopeEnd = endNodeId;
        newCtx.parent = startNodeId;

        if(!doWhile) {
            visit(navigator.getChild(astNodeId, 0), newCtx);
            condNodeId = newCtx.endId;
            newCtx.parent = condNodeId;
            visit(navigator.getChild(astNodeId, 1), newCtx);
            cfg.addEdge(newCtx.endId, startNodeId);
        } else {
            visit(navigator.getChild(astNodeId, 1), newCtx);
            newCtx.parent = newCtx.endId;
            visit(navigator.getChild(astNodeId, 0), newCtx);
            condNodeId = newCtx.endId;
            cfg.addEdge(condNodeId, startNodeId);
        }
        cfg.addEdge(condNodeId, endNodeId);
    }

    public void visit_FOR(Integer astNodeId, Context ctx) {
        if(!navigator.matchChildrenTypes(astNodeId, Arrays.asList("Init", "Condition", "Increment", "StatementBody")))
            throw new ASTJsonException("ERROR: Bad for statement (required Init&Condition&Increment&StatementBody)");

        Context newCtx = ctx.clone();
        Integer startCfgNodeId = getNewNode();
        Integer endCfgNodeId = getNewNode();
        Integer initCfgNodeId = getNewNode();
        Integer condCfgNodeId = null;
        Integer incCfgNodeId = getNewNode();
        Integer startCond = null;

        if(ctx.startId == null)
            ctx.startId = startCfgNodeId;
        ctx.endId = endCfgNodeId;

        cfg.setNodeType(startCfgNodeId, "For");
        cfg.setNodeType(endCfgNodeId, "ForEnd");
        cfg.setNodeType(initCfgNodeId, "ForInitEnd");
        cfg.addEdge(ctx.parent, startCfgNodeId);
        cfg.setNodePtr(astNodeId, startCfgNodeId);

        //Init
        cfg.setNodePtr(navigator.getChild(astNodeId,0), initCfgNodeId);
        newCtx.scope = initCfgNodeId;
        newCtx.parent = startCfgNodeId;
        visit(navigator.getChild(astNodeId, 0), newCtx);
        cfg.addEdge(newCtx.endId, initCfgNodeId);


        //Condition
        newCtx.scope = incCfgNodeId;
        newCtx.scopeEnd = endCfgNodeId;
        newCtx.parent = initCfgNodeId;
        newCtx.startId = null;
        visit(navigator.getChild(astNodeId, 1), newCtx);
        condCfgNodeId = newCtx.endId;
        startCond = newCtx.startId;

        //Increment
        transferNodeType(navigator.getChild(astNodeId,2), incCfgNodeId);
        cfg.setNodePtr(navigator.getChild(astNodeId,2), incCfgNodeId);
        newCtx.scope = incCfgNodeId;
        newCtx.parent = incCfgNodeId;
        newCtx.startId = null;
        visit(navigator.getChild(astNodeId, 2), newCtx);
        cfg.addEdge(newCtx.endId, startCond);

        //Body
        newCtx.scope = incCfgNodeId;
        newCtx.parent = condCfgNodeId;
        newCtx.startId = null;
        newCtx.endId = condCfgNodeId;
        visit(navigator.getChild(astNodeId, 3), newCtx);
        cfg.addEdge(newCtx.endId, incCfgNodeId);

        cfg.addEdge(condCfgNodeId, endCfgNodeId);
    }

    public void visit_FOREACH(Integer astNodeId, Context ctx) {
        int n_children = navigator.getChildrenSize(astNodeId);
        if(n_children != 3 && n_children != 4)
            throw new ASTJsonException("ERROR: A foreach needs 3/4 children");

        Context newCtx = ctx.clone();
        Integer startCfgNodeId = getNewNode();
        Integer endCfgNodeId = getNewNode();

        if(ctx.startId == null)
            ctx.startId = startCfgNodeId;
        ctx.endId = endCfgNodeId;
        cfg.setNodeType(startCfgNodeId, "ForEach");
        cfg.setNodeType(endCfgNodeId, "ForEachEnd");

        cfg.setNodePtr(astNodeId, startCfgNodeId);

        newCtx.scope = startCfgNodeId;
        newCtx.scopeEnd = endCfgNodeId;
        newCtx.parent = startCfgNodeId;
        newCtx.startId = null;
        visit_BLOCK(astNodeId, newCtx);

        cfg.addEdge(ctx.parent, startCfgNodeId);
        cfg.addEdge(startCfgNodeId, endCfgNodeId);
        cfg.addEdge(newCtx.endId, startCfgNodeId);

    }

    @SuppressWarnings("fallthrough")
    public void visit_IFELSE(Integer astNodeId, Context ctx) {
        switch (ast.getType(astNodeId)) {
            case "IfThenStatement":
                if(!navigator.matchChildrenTypes(astNodeId, Arrays.asList("Condition", "StatementBody")))
                    throw new ASTJsonException("ERROR: Bad IF statement (required Condition&StatementBody)");
                break;
            case "IfThenElseStatement":
                if(!navigator.matchChildrenTypes(astNodeId, Arrays.asList("Condition", "StatementBody", "StatementBody")))
                    throw new ASTJsonException("ERROR: Bad IFELSE statement (required Condition&StatementBody&StatementBody)");
                break;
            case "IfThenElifStatement":
                if(!navigator.matchChildrenTypes(astNodeId, Arrays.asList("Condition", "StatementBody", "ElseIfList")))
                    throw new ASTJsonException("ERROR: Bad IFELIF statement (required Condition&StatementBody&ElseIfList)");
                break;
            case "IfThenElifElseStatement":
                if(!navigator.matchChildrenTypes(astNodeId, Arrays.asList("Condition", "StatementBody", "ElseIfList", "StatementBody")))
                    throw new ASTJsonException("ERROR: Bad IFELIFELSE statement (required Condition&StatementBody&ElseIfList&StatementBody)");
                break;
        }
        Integer startCfgNodeId = getNewNode();
        Integer condCfgNodeId = null;
        Integer endCfgNodeId = getNewNode();
        boolean emptyIfOrElse = false;
        ctx.endId = endCfgNodeId;
        if(ctx.startId == null)
            ctx.startId = startCfgNodeId;

        Context newCtx = ctx.clone();
        newCtx.parent = startCfgNodeId;

        cfg.setNodeType(startCfgNodeId, "If");
        cfg.setNodeType(endCfgNodeId, "IfEnd");

        cfg.setNodePtr(astNodeId, startCfgNodeId);

        visit(navigator.getChild(astNodeId, 0), newCtx);
        cfg.addEdge(ctx.parent, startCfgNodeId);
        condCfgNodeId = newCtx.endId;

        newCtx.parent = condCfgNodeId;
        newCtx.startId = null;
        if(isEmptyBody(navigator.getIndexedNode(astNodeId, Arrays.asList(1)), ctx)) // Empty block ?
            emptyIfOrElse = true;
        else {
            visit(navigator.getChild(astNodeId, 1), newCtx);
            cfg.addEdge(newCtx.endId, endCfgNodeId);
        }

        // Handle ElseIf
        switch (ast.getType(astNodeId)) {
            case "IfThenElifStatement":
            case "IfThenElifElseStatement":
                Integer last_condition = condCfgNodeId;
                // For each elseif
                for(Integer i = 0 ; i < navigator.getChildrenSize(navigator.getChild(astNodeId, 2)) ; i++) {
                    Integer cfg_next_condition = null;
                    newCtx.parent = last_condition;
                    visit(navigator.getIndexedNode(astNodeId, Arrays.asList(2, i, 0)), newCtx);
                    cfg_next_condition = newCtx.endId;

                    newCtx.startId = null;
                    newCtx.parent = cfg_next_condition;
                    if(isEmptyBody(navigator.getIndexedNode(astNodeId, Arrays.asList(2, i, 1)), ctx))
                        cfg.addEdge(cfg_next_condition, endCfgNodeId);
                    else {
                        visit(navigator.getIndexedNode(astNodeId, Arrays.asList(2, i, 1)), newCtx);
                        cfg.addEdge(newCtx.endId, endCfgNodeId);
                    }
                    last_condition = cfg_next_condition;

                }
                newCtx.parent = last_condition;
                break;
        }

        // Handle Else Statement
        int posElse = -1;
        switch (ast.getType(astNodeId)) {
            case "IfThenElseStatement":
                posElse = (posElse == -1) ? 2 : posElse;
            case "IfThenElifElseStatement":
                posElse = (posElse == -1) ? 3 : posElse;
                newCtx.startId = null;
                if(isEmptyBody(navigator.getIndexedNode(astNodeId, Arrays.asList(posElse)), ctx)) // Empty block ?
                    emptyIfOrElse = true;
                else {
                    visit(navigator.getChild(astNodeId, posElse), newCtx);
                    cfg.addEdge(newCtx.endId, endCfgNodeId);
                }
                break;
            default:
                if(!emptyIfOrElse)
                    cfg.addEdge(newCtx.parent, endCfgNodeId);
        }
        if(emptyIfOrElse)
            cfg.addEdge(condCfgNodeId, endCfgNodeId);
    }

    public void visit_TERNARY(Integer astNodeId, Context ctx) {
        int n_children = navigator.getChildrenSize(astNodeId);
        if(n_children != 3)
            throw new ASTJsonException("ERROR: A ternary needs 3 children ("+astNodeId+")");

        Integer ternaryId = iNextTernaryValue++;
        Integer startCfgNodeId = getNewNode();
        Integer condCfgNodeId = null;
        Integer endCfgNodeId = getNewNode();
        cfg.setNodeType(startCfgNodeId, "Ternary");
        cfg.setNodeType(endCfgNodeId, "Variable");
        cfg.setNodeImage(endCfgNodeId, "#ternary_value_"+ternaryId);
        cfg.setVarScope(endCfgNodeId, 0);
        cfg.setNodePtr(astNodeId, endCfgNodeId);
        cfg.addEdge(ctx.parent, startCfgNodeId);
        ctx.endId = startCfgNodeId;

        //Visit Condition
        Context newCtx = ctx.clone();
        newCtx.parent = startCfgNodeId;
        visit(navigator.getChild(astNodeId, 0), newCtx);
        condCfgNodeId = newCtx.endId;
        if(newCtx.startId == null) // empty
            cfg.addEdge(condCfgNodeId, endCfgNodeId);

        //Visit if true
        newCtx = ctx.clone();
        newCtx.parent = condCfgNodeId;
        visit(navigator.getChild(astNodeId, 1), newCtx);
        if(newCtx.startId == null) // Since PHP 5.3, you can omit the on true expression. (condition) ?: (on false) returns the result of condition if the condition is true.
            newCtx.endId = condCfgNodeId;

        // Add intermediate tmp variable and Assignation
        Integer tmpVarNodeId = getNewNode();
        cfg.setNodeType(tmpVarNodeId, "Variable");
        cfg.setNodeImage(tmpVarNodeId, "#ternary_value_"+ternaryId);
        cfg.setVarScope(tmpVarNodeId, 0);
        cfg.addEdge(newCtx.endId, tmpVarNodeId);

        Integer tmpAssignNodeId = getNewNode();
        cfg.setNodeType(tmpAssignNodeId, "BinOP");
        cfg.setNodeImage(tmpAssignNodeId, "=");
        cfg.addEdge(tmpVarNodeId, tmpAssignNodeId);
        cfg.setOpHands(tmpAssignNodeId, new Pair<>(tmpVarNodeId, (newCtx.endId.equals(condCfgNodeId)) ? cfg.getParent(condCfgNodeId).get(0) : newCtx.endId ));
        cfg.addEdge(tmpAssignNodeId, endCfgNodeId);

        //Visit if false
        newCtx = ctx.clone();
        newCtx.parent = condCfgNodeId;
        visit(navigator.getChild(astNodeId, 2), newCtx);
        //cfg.addEdge(newCtx.endId, endCfgNodeId);

        // Add intermediate tmp variable and Assignation
        tmpVarNodeId = getNewNode();
        cfg.setNodeType(tmpVarNodeId, "Variable");
        cfg.setNodeImage(tmpVarNodeId, "#ternary_value_"+ternaryId);
        cfg.setVarScope(tmpVarNodeId, 0);
        cfg.addEdge(newCtx.endId, tmpVarNodeId);

        tmpAssignNodeId = getNewNode();
        cfg.setNodeType(tmpAssignNodeId, "BinOP");
        cfg.setNodeImage(tmpAssignNodeId, "=");
        cfg.addEdge(tmpVarNodeId, tmpAssignNodeId);
        cfg.setOpHands(tmpAssignNodeId, new Pair<>(tmpVarNodeId, newCtx.endId));
        cfg.addEdge(tmpAssignNodeId, endCfgNodeId);

        if(ctx.startId == null)
            ctx.startId = startCfgNodeId;
        ctx.endId = endCfgNodeId;
    }

    public void visit_FUNCTIONCALL(Integer astNodeId, Context ctx) {
        if(!navigator.match(navigator.getChild(astNodeId, 1), "ArgumentList"))
            throw new ASTJsonException("ERROR: Bad function call (required ArgumentList)");
        if(Arrays.asList("die", "exit").contains(ast.getImage(astNodeId))) {
            visit_EXIT(astNodeId, ctx);
            return;
        }

        Integer funcNode = getNewNode();
        Integer startNode = getNewNode();
        Integer endNode = getNewNode();
        Integer idNode = null;
        Integer exprNode = null;
        cfg.setNodePtr(astNodeId, funcNode);
        transferNodeType(astNodeId, funcNode);
        transferNodeToken(astNodeId, funcNode);
        transferScopeId(astNodeId, funcNode);
        cfg.addEdge(ctx.parent, funcNode);

        //Update endId if necessary
        ctx.endId = funcNode;
        if(ctx.startId == null)
            ctx.startId = funcNode;

        Context newCtx = ctx.clone();
        newCtx.funcCallId = startNode;
        newCtx.parent = funcNode;
        visit(navigator.getChild(astNodeId, 0), newCtx);
        exprNode = newCtx.endId;
        idNode = newCtx.endId;

        newCtx = ctx.clone();
        newCtx.funcCallId = startNode;
        newCtx.parent = idNode;
        visit(navigator.getChild(astNodeId, 1), newCtx);

        cfg.setNodeType(startNode, "CallBegin");
        transferNodeToken(astNodeId, startNode);
        if(newCtx.endId == null)
            cfg.addEdge(funcNode, startNode);
        else
            cfg.addEdge(newCtx.endId, startNode);

        cfg.setNodeType(endNode, "CallEnd");
        transferNodeToken(astNodeId, endNode);
        cfg.addCall(startNode, endNode);
        cfg.addCallExpr(startNode, exprNode);

        Integer retNode = getNewNode();
        cfg.setNodeType(retNode, "RetValue");
        cfg.addEdge(endNode, retNode);

        ctx.endId = retNode;
    }

    public void visit_FUNCTION_STATEMENT(Integer astNodeId, Context ctx) {
        Integer cfgEntryId = getNewNode();
        Integer cfgExitId = getNewNode();
        cfg.setNodeType(cfgEntryId, "Entry");
        cfg.setNodeType(cfgExitId, "Exit");

        Context newCtx = new Context();
        newCtx.startId = cfgEntryId;
        newCtx.stopId = cfgExitId;
        newCtx.deadId = ctx.deadId;
        newCtx.scope = cfgEntryId;
        newCtx.scopeEnd = cfgExitId;
        newCtx.parent = cfgEntryId;
        Integer cfgNodeId = visit_GENERIC(astNodeId, newCtx);
        cfg.addEdge(newCtx.endId, cfgExitId);
        transferNodeToken(astNodeId, cfgEntryId);
        cfg.addFuncEntryNode(cfgEntryId, cfg.getImage(cfgEntryId));

        if(ast.getChildren(astNodeId).size() > 2)
            for (Integer childId : ast.getChildren(ast.getChildren(astNodeId).get(1)))
                cfg.addFuncDefParam(cfgNodeId, cfg.getNodeCfgPtr(childId));
    }

    public void visit_LAMBDA_FUNCTION_STATEMENT(Integer astNodeId, Context ctx) {
        visit_FUNCTION_STATEMENT(astNodeId, ctx);

        Integer cfgDefNode = getNewNode();
        cfg.setNodeType(cfgDefNode, "LambdaFunction");
        cfg.setNodeImage(cfgDefNode, ast.getImage(astNodeId));
        transferNodePositions(astNodeId, cfgDefNode);

        cfg.addEdge(ctx.parent, cfgDefNode);
        ctx.endId = cfgDefNode;
    }

    public void visit_SWITCH(Integer astNodeId, Context ctx) {
        if(!navigator.match(navigator.getChild(astNodeId, 1), "CaseList"))
            throw new ASTJsonException("ERROR: Bad SWITCH  (required CaseList)");

        Integer startNodeId = getNewNode();
        Integer endNodeId = getNewNode();
        Integer switchId = iNextSwitchValue++;
        ctx.endId = endNodeId;
        if(ctx.startId == null)
            ctx.startId = startNodeId;

        cfg.setNodePtr(astNodeId, startNodeId);
        transferNodeType(astNodeId, startNodeId);
        transferNodeToken(astNodeId, startNodeId);
        cfg.setNodeType(startNodeId, "Switch");
        cfg.setNodeType(endNodeId, "EndSwitch");
        cfg.addEdge(ctx.parent, startNodeId);


        //Condition
        Context newCtx = ctx.clone();
        newCtx.scopeEnd = endNodeId;
        newCtx.scope = startNodeId;
        newCtx.parent = startNodeId;
        newCtx.startId = null;
        visit(navigator.getChild(astNodeId, 0), newCtx);

        // Add intermediate tmp variable and Assignation
        Integer tmpVarNodeId = getNewNode();
        cfg.setNodeType(tmpVarNodeId, "Variable");
        cfg.setNodeImage(tmpVarNodeId, "#switch_value_"+switchId);
        cfg.setVarScope(tmpVarNodeId, 0);
        cfg.addEdge(newCtx.endId, tmpVarNodeId);

        Integer tmpAssignNodeId = getNewNode();
        cfg.setNodeType(tmpAssignNodeId, "BinOP");
        cfg.setNodeImage(tmpAssignNodeId, "=");
        cfg.addEdge(tmpVarNodeId, tmpAssignNodeId);
        cfg.setOpHands(tmpAssignNodeId, new Pair<>(tmpVarNodeId, newCtx.endId));


        //Cases
        newCtx = ctx.clone();
        newCtx.scopeEnd = endNodeId;
        newCtx.scope = startNodeId;
        newCtx.startId = null;
        newCtx.parent = null;
        newCtx.switchId = switchId;
        newCtx.lastCaseId = tmpAssignNodeId;
        newCtx.openCases = new ArrayList<>();
        visit(navigator.getChild(astNodeId, 1), newCtx);
        if(newCtx.endId != null)
            cfg.addEdge(newCtx.endId, endNodeId);
        if(newCtx.defaultCaseId == null)
            cfg.addEdge(newCtx.lastCaseId, endNodeId);
        else if(cfg.getChildren(newCtx.defaultCaseId).size() == 0)
            cfg.addEdge(newCtx.defaultCaseId, endNodeId);



    }

    public void visit_CASE(Integer astNodeId, Context ctx) {
        int n_children = navigator.getChildrenSize(astNodeId);
        if(n_children != 2)
            throw new ASTJsonException("ERROR: A CASE requires 2 children (" + astNodeId + ")");

        boolean isDefault = ast.getType(ast.getChildren(astNodeId).get(0)).equals("Default");

        Integer caseId = getNewNode();
        Integer tmpVarNodeId = getNewNode();
        cfg.setNodeType(caseId, (isDefault) ? "CaseDefault" : "CaseCondition");
        if(isDefault) ctx.defaultCaseId = caseId;
        else {
            cfg.setNodeType(tmpVarNodeId, "Variable");
            cfg.setNodeImage(tmpVarNodeId, "#switch_value_"+ctx.switchId);
            cfg.setVarScope(tmpVarNodeId, 0);
            cfg.addEdge(ctx.lastCaseId, tmpVarNodeId);
            ctx.lastCaseId = tmpVarNodeId;
        }

        Context ctxCondition = ctx.clone();
        ctxCondition.parent = ctx.lastCaseId; // Connect this case condition to the last one
        ctxCondition.startId = null;
        visit(navigator.getChild(astNodeId, 0), ctxCondition);
        if(ctxCondition.startId == null) cfg.addEdge(ctxCondition.parent, caseId); // If empty condition, connect case node to previous one (Usually "Default" node scenario)
        if(ctxCondition.endId != null) {
            cfg.addEdge(ctxCondition.endId, caseId);
            cfg.setOpHands(caseId, new Pair<>(tmpVarNodeId, ctxCondition.endId));
        }

        Context ctxBody = ctx.clone();
        ctxBody.startId = null;
        ctxBody.endId = caseId;
        visit(navigator.getChild(astNodeId, 1), ctxBody);

        if(ctxBody.startId == null)
            ctx.openCases.add(caseId); // No body, then it is a chain of cases that needs to be connected to the next body
        else {
            cfg.addEdge(caseId, ctxBody.startId);
            for (Integer id : ctx.openCases) { // Connect chain of cases to the current body
                Integer tmp = cfg.getChildren(id).get(0); // hack to get right True/False order
                cfg.removeEdge(id, tmp);
                cfg.addEdge(id, ctxBody.startId);
                cfg.addEdge(id, tmp);
            }
            ctx.openCases.clear();
        }

        ctx.lastCaseId = caseId;
        ctx.endId = ctxBody.endId;
    }
    public void visit_CASELIST(Integer astNodeId, Context ctx) {
        for (int i = 0; i < navigator.getChildrenSize(astNodeId); i++) {
            ctx.startId = null;
            ctx.endId = null;
            visit(navigator.getChild(astNodeId, i), ctx);
            ctx.parent = ctx.endId;
        }
    }

    public void visit_TRYCATCH(Integer astNodeId, Context ctx) {
        int n_children = navigator.getChildrenSize(astNodeId);
        if(n_children < 2)
            throw new ASTJsonException("ERROR: A TRYCATCH requires at least 2 children (" + astNodeId + ")");

        Integer startCfgNodeId = getNewNode();
        Integer endCfgNodeId = getNewNode();

        if(ctx.startId == null)
            ctx.startId = startCfgNodeId;
        ctx.endId = endCfgNodeId;
        cfg.setNodeType(startCfgNodeId, "TryCatch");
        cfg.setNodeType(endCfgNodeId, "TryCatchEnd");
        cfg.setNodePtr(astNodeId, startCfgNodeId);

        Context newCtx = ctx.clone();
        cfg.addEdge(ctx.parent, startCfgNodeId);
        for (int i = navigator.getChildrenSize(astNodeId)-1; i >= 0 ; i--) { // Invert order -> find catches before visit try
            newCtx.scope = startCfgNodeId;
            newCtx.scopeEnd = endCfgNodeId;
            newCtx.startId = null;
            newCtx.parent = (i==0) ? startCfgNodeId : null; // Catches should not be connected

            visit(navigator.getChild(astNodeId, i), newCtx);
            if (newCtx.endId != null)
                cfg.addEdge(newCtx.endId, endCfgNodeId);
        }
    }

    public void visit_CATCH(Integer astNodeId, Context ctx) {
        Integer cfgNodeId = visit_GENERIC(astNodeId, ctx);
        ctx.catchesId.add(cfgNodeId);
    }


    public void visit_THROW(Integer astNodeId, Context ctx) {
        Context newCtx = ctx.clone();
        visit_OP(astNodeId, newCtx);

        if(ctx.catchesId.isEmpty())
            cfg.addEdge(newCtx.endId, ctx.deadId);
        else {
            for (Integer catchId : ctx.catchesId)
                cfg.addEdge(newCtx.endId, catchId);
        }

        ctx.endId = null;
    }

    public void visit_BREAK(Integer astNodeId, Context ctx) {
        Integer cfgNodeId = visit_GENERIC(astNodeId, ctx);
        if(ctx.scopeEnd == null)
            throw new ASTJsonException("ERROR: Node " + astNodeId + " no-where to break");
        cfg.addEdge(cfgNodeId, ctx.scopeEnd);
        ctx.endId = null;
    }

    public void visit_CONTINUE(Integer astNodeId, Context ctx) {
        Integer cfgNodeId = visit_GENERIC(astNodeId, ctx);
        if(ctx.scope == null)
            throw new ASTJsonException("ERROR: Node " + astNodeId + " no-where to continue");
        cfg.addEdge(cfgNodeId, ctx.scope);
        ctx.endId = null;
    }

    public void visit_EXIT(Integer astNodeId, Context ctx) {
        Context newCtx = ctx.clone();
        visit_GENERIC(astNodeId, newCtx);

        cfg.addEdge(newCtx.endId, ctx.deadId);
        ctx.endId = null;
    }

    public void visit_RETURN(Integer astNodeId, Context ctx) {
        Context newCtx = ctx.clone();
        visit_OP(astNodeId, newCtx);

        cfg.addEdge(newCtx.endId, ctx.stopId);
        ctx.endId = null;
    }

    public void visit_ARGUMENTLIST(Integer astNodeId, Context ctx) {
        Integer cfgNodeId = getNewNode();
        cfg.setNodePtr(astNodeId, cfgNodeId);
        transferNodeType(astNodeId, cfgNodeId);
        transferNodeToken(astNodeId, cfgNodeId);
        transferNodePositions(astNodeId, cfgNodeId);
        cfg.addEdge(ctx.parent, cfgNodeId);

        //Update endId if necessary
        ctx.endId = cfgNodeId;
        if(ctx.startId == null)
            ctx.startId = cfgNodeId;

        //Visit children
        if(navigator.getChildrenSize(astNodeId) != 0) {
            Context newCtx = ctx.clone();
            newCtx.parent = cfgNodeId;
            navigator.getChildren(astNodeId).forEach(i -> {
                visit(i, newCtx);
                newCtx.parent = newCtx.endId;
                cfg.addFuncCallArg(ctx.funcCallId, newCtx.endId);
            });
            ctx.endId = newCtx.endId;
        }
    }

    public void visit_INCLUDE(Integer astNodeId, Context ctx) {
        Integer cfgStartId = getNewNode();
        Integer cfgEndId = getNewNode();
        Integer cfgExprId = null;

        cfg.setNodeType(cfgStartId, "IncludeBegin");
        cfg.setNodeType(cfgEndId, "IncludeEnd");

        Context newCtx = ctx.clone();
        if(navigator.getChildrenSize(astNodeId) != 0)
            navigator.getChildren(astNodeId).forEach(i -> {
                visit(i, newCtx);
                if(ctx.startId == null)
                    ctx.startId = newCtx.startId;
                newCtx.parent = newCtx.endId;
            });
        cfgExprId = newCtx.endId;
        cfg.addEdge(cfgExprId, cfgStartId);
        cfg.addCall(cfgStartId, cfgEndId);
        cfg.addCallExpr(cfgStartId, cfgExprId);
        //cfg.addFuncCallArg(cfgStartId, cfgNodeId); // link to Statement type
        cfg.setNodeImage(cfgStartId, cfg.getImage(cfgExprId));
        cfg.setNodeImage(cfgEndId, cfg.getImage(cfgExprId));
        ctx.endId = cfgEndId;

    }

    public void visit_passthrough(Integer astNodeId, Context ctx) {
        if(navigator.getChildrenSize(astNodeId) != 0)
            navigator.getChildren(astNodeId).forEach(i -> {
                visit(i, ctx);
            });
    }

    public void visit_ROOT() {
        Integer startNodeId = getNewNode();
        Integer stopNodeId = getNewNode();
        Integer deadNodeId = getNewNode();
        Integer rootAST = ast.getRoot();
        cfg.setRoot(startNodeId);

        cfg.setNodeType(startNodeId, "Entry");
        cfg.setNodeImage(startNodeId, "main");
        cfg.setNodeType(stopNodeId, "Exit");
        cfg.setNodeType(deadNodeId, "Dead");

        Context ctx = new Context();
        ctx.parent = startNodeId;
        ctx.scope = startNodeId;
        ctx.stopId = stopNodeId;
        ctx.deadId = deadNodeId;
        if(ast.getType(rootAST).equals("Start"))
            cfg.setNodePtr(rootAST, startNodeId);
        visit(rootAST, ctx);
        cfg.addEdge(ctx.endId, stopNodeId);
    }

    public CFG visit() throws ASTJsonException {
        visit_ROOT();
        return cfg;
    }

    public void visit(Integer astNodeId, Context ctx) throws ASTJsonException {
        String curType = ast.getType(astNodeId);
        if (curType == null)
            throw new ASTJsonException("ERROR: missing node " + astNodeId);

        switch (curType) {
            case "Start":
            case "Block":
            case "Init":
            case "Increment":
            case "PostfixExpression":
            case "ParameterList":
            case "VariableStatement":
            case "ExpressionStatement":
                visit_BLOCK(astNodeId, ctx);
                break;
            case "Return":
                visit_RETURN(astNodeId, ctx);
                break;
            case "Break":
                visit_BREAK(astNodeId, ctx);
                break;
            case "Continue":
                visit_CONTINUE(astNodeId, ctx);
                break;
            case "Condition":
                visit_CONDITION(astNodeId, ctx);
                break;
            case "IfThenStatement":
            case "IfThenElseStatement":
            case "IfThenElifStatement":
            case "IfThenElifElseStatement":
                visit_IFELSE(astNodeId, ctx);
                break;
            case "ConditionalExpression":
                visit_TERNARY(astNodeId, ctx);
                break;
            case "Switch":
                visit_SWITCH(astNodeId, ctx);
                break;
            case "While":
            case "DoWhile":
                visit_WHILE(astNodeId, ctx);
                break;
            case "For":
                visit_FOR(astNodeId, ctx);
                break;
            case "ForEach":
                visit_FOREACH(astNodeId, ctx);
                break;
            case "FunctionCall":
            case "MethodCall":
                visit_FUNCTIONCALL(astNodeId, ctx);
                break;
            case "TryCatch":
                visit_TRYCATCH(astNodeId, ctx);
                break;
            case "Throw":
                visit_THROW(astNodeId, ctx);
                break;
            case "Catch":
                visit_CATCH(astNodeId, ctx);
                break;
            case "Case":
                visit_CASE(astNodeId, ctx);
                break;
            case "CaseList":
                visit_CASELIST(astNodeId, ctx);
                break;
            case "IncludeStatement":
            case "IncludeOnceStatement":
            case "RequireStatement":
            case "RequireOnceStatement":
                visit_INCLUDE(astNodeId, ctx);
                break;
            case "Id":
                if(ast.getImage(astNodeId).equals("die") && !ast.getType(ast.getParent(astNodeId).get(0)).equals("FunctionCall"))
                    visit_EXIT(astNodeId, ctx);
                else
                    visit_GENERIC(astNodeId, ctx);
                break;

            case "EchoStatement":
            case "PrintStatement":
            case "DeclareStatement":
            case "ClassStatement":
            case "ClassName":
            case "ClassInstanciation":
            case "Html":
            case "UnsetStatement":
            case "PreIncrement":
            case "PostIncrement":
            case "StringExpression":
            case "StringLiteral":
            case "ExecString":
            case "IntegerLiteral":
            case "HexLiteral":
            case "DoubleLiteral":
            case "LegalChar":
            case "Array":
            case "True":
            case "False":
            case "Variable":
            case "Null":
            case "HeredocFlow":
            case "EId":
            case "NamespaceName":
            case "New":
            case "Global":
            case "Public":
            case "Private":
            case "Protected":
            case "Clone":
            case "Int":
            case "Integer":
            case "Double":
            case "Bool":
            case "Boolean":
            case "Float":
            case "Object":
            case "String":
            case "Static":
            case "Implements":
            case "MemberDeclaration":
            case "ConstMemberDeclaration":
            case "PublicMemberDeclaration":
            case "PrivateMemberDeclaration":
            case "ProtectedMemberDeclaration":
                visit_GENERIC(astNodeId, ctx);
                break;
            case "BinOP":
            case "RelOP":
            case "LogicOP":
            case "ClassAccess":
            case "CastExpression":
                visit_BinOP(astNodeId, ctx);
                break;
            case "ArrayExpression":
                visit_ARRAYEXPRESSION(astNodeId, ctx);
                break;
            case "UnaryOP":
                visit_UnOP(astNodeId, ctx);
                break;
            case "ArgumentList":
                visit_ARGUMENTLIST(astNodeId, ctx);
                break;
            case "FunctionStatement":
            case "MethodStatement":
            case "PublicMethodStatement":
            case "PrivateMethodStatement":
            case "ProtectedMethodStatement":
            case "AbstractMethodStatement":
            case "AbstractPublicMethodStatement":
            case "AbstractPrivateMethodStatement":
            case "AbstractProtectedMethodStatement":
                visit_FUNCTION_STATEMENT(astNodeId, ctx);
                break;
            case "LambdaFunctionStatement":
                visit_LAMBDA_FUNCTION_STATEMENT(astNodeId, ctx);
                break;
            case "VariableExpression":
            case "ParentClassName":
            case "NamespaceStatement":
            case "InterfaceStatement":
            case "TraitStatement":
            case "UseStatement":
            case "UseInsteadOf":
            case "UseTraitDeclaration":
            case "ValueParameter":
            case "OptValueParameter": // Todo add expr arg
            case "TypedValueParameter":
            case "OptTypedValueParameter":
            case "ReferenceParameter":
            case "OptReferenceParameter":
            case "TypedReferenceParameter":
            case "OptTypedReferenceParameter":
                visit_OP(astNodeId, ctx);
                break;
            case "ArrayInitialisation":
                visit_ARRAYINIT(astNodeId, ctx);
            break;
            case "Argument":
            case "Arobas":
            case "Default":
            case "StatementBody":
            case "ReturnReferenceFunction":
            case "ReturnReferenceMethod":
            case "ReturnValueFunction":
            case "ReturnValueMethod":
            case "ConditionalFalse":
            case "ConditionalTrue":
                visit_passthrough(astNodeId, ctx);
                break;
            default:
                throw new ASTJsonException("ERROR: invalid type " + curType +
                        " for node " + astNodeId);
                /*System.out.println("WARN: Unknown type " + curType);
                visit_passthrough(astNodeId, ctx);*/
        }

    }

}
