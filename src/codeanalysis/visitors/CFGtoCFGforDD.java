/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.visitors;

import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class CFGtoCFGforDD {
    private static final Logger LOGGER = CustomLogger.getLogger(CFGtoCFGforDD.class.getName());
    private CFG cfg = null;
    private CFGforDD cfgdd = null;
    private List<Integer> visited = null;
    private Integer nextNodeId = 0;

    public CFGtoCFGforDD(CFG cfg) {
        this.cfg = cfg;
        this.nextNodeId = cfg.getNextNodeId();
        this.cfgdd = new CFGforDD();
    }

    public CFGforDD visit() {
        visited = new ArrayList<>();
        cfgdd.setRoot(cfg.getRoot());
        cfgdd.setFilename(cfg.getFilename());
        visit(cfg.getRoot(), cfg.getRoot());
        for(Integer key : cfg.getAllFuncEntryNode())
            visit(key, key);
        return cfgdd;
    }

    public void visit(Integer key, Integer root) {
        if(visited.contains(key))
            return;
        visited.add(key);

        switch (cfg.getType(key)) {
            case "Entry":
                //print("node_type", key, "entry");
                cfgdd.setNodeType(key, "entry");
                cfgdd.setNodeImage(key, cfg.getImage(key));
                String token = cfg.getImage(key);
                if(token == null) {
                    LOGGER.severe("Entry node requires token (main ? or function name ?)");
                    token = "unknown_func";
                }
                if(root.equals(cfg.getRoot())) { // main
                    cfgdd.addEntryFuncName(key, Arrays.asList(0, "main"));
                    cfgdd.addAttr("start_node", Integer.toString(key));
                }
                else {
                    Integer child = cfg.getChildren(key).get(0);

                    cfgdd.addEntryFuncName(key, Arrays.asList(cfg.getScopeId(child), token));
                }
                break;
            case "Exit":
                if(root.equals(cfg.getRoot())) // main
                    cfgdd.addAttr("stop_node", Integer.toString(key));
                cfgdd.setNodeType(key, "exit");
                cfgdd.addCfgEntry(key, root);
                cfgdd.addCfgExit(root, key);
                break;
            case "CallBegin":
                cfgdd.setNodeType(key, "call_begin");
                if(cfg.getImage(key) != null)
                    cfgdd.setNodeImage(key, cfg.getImage(key));
                if(cfg.getCallEnd(key) != null)
                    cfgdd.addCallEnds(key, cfg.getCallEnd(key));
                if(cfg.getImage(key) != null)
                    cfgdd.addCallBeginsFuncName(key, cfg.getImage(key));
                if(cfg.getCallExpr(key) != null)
                    cfgdd.addRhs(key, cfg.getCallExpr(key));
                if(cfg.getFuncCallArgs(key) != null)
                    for(Integer argId : cfg.getFuncCallArgs(key)) {
                        if(argId == defsInt.UNDEF_VAL)
                            continue;
                        Integer actArg = nextNodeId++;
                        cfgdd.setNodeType(actArg, "act_arg");
                        cfgdd.addActArg(key, new Integer[] { cfg.getFuncCallArgs(key).indexOf(argId), actArg});

                        argId = followRHSChain(argId);
                        cfgdd.addExprArg(actArg, argId);
                        cfgdd.addRhs(actArg, argId);


                    }
                break;
            case "CallEnd":
                //print("node_type", key, "call_end");
                cfgdd.setNodeType(key, "call_end");
                if(cfg.getImage(key) != null)
                    cfgdd.setNodeImage(key, cfg.getImage(key));
                if(cfg.getImage(key) != null) //                     print("call_end_func_name", key, cfg.getToken(key));
                    cfgdd.addCallEndsFuncName(key, cfg.getImage(key));
                if(cfg.getCallBegin(key) != null) //                  print("call_begin", key, cfg.getCallBegin(key));
                    cfgdd.addCallBegins(key, cfg.getCallBegin(key));
                break;
            case "IncludeBegin":
                cfgdd.setNodeType(key, "include_begin");
                if(cfg.getImage(key) != null)
                    cfgdd.setNodeImage(key, cfg.getImage(key));
                if(cfg.getCallEnd(key) != null)
                    cfgdd.addIncludeEnds(key, cfg.getCallEnd(key));
                if(cfg.getCallExpr(key) != null)
                    cfgdd.addRhs(key, cfg.getCallExpr(key));
                break;
            case "IncludeEnd":
                cfgdd.setNodeType(key, "include_end");
                if(cfg.getImage(key) != null)
                    cfgdd.setNodeImage(key, cfg.getImage(key));
                if(cfg.getCallBegin(key) != null)
                    cfgdd.addIncludeBegins(key, cfg.getCallBegin(key));
                break;
            case "FunctionStatement":
            case "MethodStatement":
                if(cfg.getFuncDefParam(key) != null)
                    for(Integer paramId : cfg.getFuncDefParam(key)) {
                        cfgdd.setNodeType(paramId, "form_arg");
                        cfgdd.addExprArg(paramId, cfg.getParent(paramId).get(0));
                        cfgdd.addFormArg(cfg.getParent(key).get(0), new Integer[] { cfg.getFuncDefParam(key).indexOf(paramId), paramId});
                    }
                break;
            case "RetValue":
                cfgdd.setNodeType(key, "act_ret");
                cfgdd.addActRet(cfg.getParent(key).get(0), key);
                cfgdd.addAccess(key, Arrays.asList(0, 0, cfg.getNodeAstPtr(key), "lambda_" + key));
                break;
            case "Return":
                cfgdd.setNodeType(key, "form_ret");
                cfgdd.addFormRet(cfg.getChildren(key).get(0), cfg.getParent(key).get(0));
                break;
            case "BinOP":
            case "RelOP":
            case "LogicOP":
            case "ClassAccess":
            case "ArrayExpression":
            case "CastExpression":
                if(cfg.getImage(key) != null && Arrays.asList("=", "+=", "-=", "*=", "/=", ".=").contains(cfg.getImage(key))) {
                    cfgdd.setNodeType(key, "CMD");
                    cfgdd.addLhs(key,  cfg.getOpHands(key).getKey());
                    cfgdd.addRhs(key,  followRHSChain(key));
                }
                else {
                    //TODO: call_expr if call_begin linked
                    cfgdd.setNodeType(key, "expr");
                    cfgdd.addExprArg(key,  followRHSChain(cfg.getOpHands(key).getKey()));
                    if(cfg.getOpHands(key).getValue() != null) // "or die()" or $a[]
                        cfgdd.addExprArg(key,  followRHSChain(cfg.getOpHands(key).getValue()));
                }
                cfgdd.setNodeImage(key, cfg.getImage(key));
                break;
            case "ArrayInitialisation":
                if(cfg.getFuncCallArgs(key) != null && cfg.getFuncCallArgs(key).size() != 0) {
                    cfgdd.setNodeType(key, "expr");
                    for(Integer arg : cfg.getFuncCallArgs(key))
                        cfgdd.addExprArg(key,  arg);
                    cfgdd.setNodeImage(key, cfg.getType(key));
                } else {
                    cfgdd.setNodeType(key, "TK_NODE");
                    cfgdd.addAccess(key, Arrays.asList(6, 0, cfg.getNodeAstPtr(key), "empty_array"));
                }
                break;
            case "UnaryOP":
                cfgdd.setNodeType(key, "expr");
                cfgdd.addExprArg(key, cfg.getParent(key).get(0));
                cfgdd.setNodeImage(key, cfg.getImage(key));
                break;
            case "VariableExpression":
                cfgdd.setNodeType(key, "expr");
                cfgdd.addExprArg(key, cfg.getParent(key).get(0));
                break;
            case "Variable":
                cfgdd.setNodeType(key, "TK_NODE");
                cfgdd.addAccess(key, Arrays.asList(0, cfg.getVarScope(key), cfg.getNodeAstPtr(key), cfg.getImage(key)));
                break;
            case "Id":
            case "EId":
            case "NamespaceName":
                cfgdd.setNodeType(key, "TK_NODE");
                cfgdd.addAccess(key, Arrays.asList(0, 0, cfg.getNodeAstPtr(key), cfg.getImage(key)));
                break;
            case "LambdaFunction":
                cfgdd.setNodeType(key, "TK_NODE");
                Integer cfgStatement = cfg.getChildren(cfg.getFuncEntryNode(cfg.getImage(key))).get(0);
                cfgdd.addAccess(key, Arrays.asList(3, 0, cfg.getNodeAstPtr(cfgStatement), cfg.getImage(key)));
                break;
            case "StringLiteral":
            case "StringExpression":
            case "IntegerLiteral":
            case "HexLiteral":
            case "DoubleLiteral":
            case "True":
            case "False":
            case "Null":
            case "Array":
            case "Html":
            case "Int":
            case "Integer":
            case "Double":
            case "Bool":
            case "Boolean":
            case "Float":
            case "Object":
            case "Static":
            case "String":
                cfgdd.setNodeType(key, "TK_NODE");
                String image = cfg.getImage(key);
                if(image == null) image = "null";
                cfgdd.addAccess(key, Arrays.asList(2, 0, cfg.getNodeAstPtr(key), image));
                break;
            case "Condition":
                cfgdd.setNodeType(key, "IF");
                cfgdd.addExprArg(key, cfg.getParent(key).get(0));
                break;
            case "CaseCondition":
                cfgdd.setNodeType(key, "IF");
                Integer expr = nextNodeId++;
                cfgdd.setNodeType(expr, "EXPR");
                /*Integer cpy = nextNodeId++;
                visited.add(expr);
                visited.add(cpy);

                cfgdd.setNodeType(expr, "EXPR");
                cfgdd.copyNode(cpy, cfgdd, cfg.getOpHands(key).getKey());
                cfgdd.addExprArg(expr, cpy);*/
                //Add exper + copy var
                cfgdd.addExprArg(key, expr);
                cfgdd.addExprArg(expr, cfg.getOpHands(key).getKey());
                cfgdd.addExprArg(expr, followRHSChain(key));

                break;
            //TODO: access const
        }

        for (Integer childId : cfg.getChildren(key)) //print("cfg_succ", key, childId);
            cfgdd.addEdge(key, childId);

        for (Integer childId : cfg.getChildren(key))
            visit(childId, root);
        if (cfg.getCallEnd(key) != null)
            visit(cfg.getCallEnd(key), root);
    }

    protected Integer followRHSChain(Integer nodeId) {
        if(cfg.getType(nodeId).equals("BinOP") && Arrays.asList("=", "+=", "-=", "*=", "/=", ".=").contains(cfg.getImage(nodeId)))
            return followRHSChain(cfg.getOpHands(nodeId).getValue());
        return nodeId;
    }
}
