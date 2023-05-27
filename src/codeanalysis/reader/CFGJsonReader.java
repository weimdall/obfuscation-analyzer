/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.reader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.polymtl.codeanalysis.exceptions.ASTJsonException;
import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;

public class CFGJsonReader extends GraphJsonReader implements defsInt {
    public CFG read(String filename) throws IOException {
        return this.read(this.read_file(filename));
    }
    public CFG read(BufferedReader reader) throws IOException, JSONException {
        CFG graph = new CFG();
        JSONTokener jsonparser;
        JSONArray array;
        String line = reader.readLine();
        while (line != null ) {
            if( line.equals("[") ) {
                line = reader.readLine();
                continue;
            }
            if( line.equals("]") )
                break;

            jsonparser = new JSONTokener(line);
            array = new JSONArray(jsonparser);

            if(!read_line(graph, array))
                throw new ASTJsonException("Unknown line type : " + line);

            line = reader.readLine();
        }
        if(graph.getRoot() == UNDEF_VAL)
            throw new ASTJsonException("No node root set");
        reader.close();
        return graph;
    }

    protected boolean read_line(CFG graph, JSONArray array) {
        if(super.read_line(graph, array))
            return true;
        switch ((String) array.get(0)) {
            case "call_end":
                readLine_call_end(graph, array);
                break;
            case "call_expr":
                readLine_call_expr(graph, array);
                break;
            case "op_hands":
                readLine_op_hands(graph, array);
                break;
            case "ast_pt":
                readLine_ast_pt(graph, array);
                break;
            case "func_call_arg":
                readLine_func_call_arg(graph, array);
                break;
            case "func_def_param":
                readLine_func_def_param(graph, array);
                break;
            case "var_id":
                readLine_var_id(graph, array);
                break;
            case "var_scope":
                readLine_var_scope(graph, array);
                break;
            case "scope_id":
                readLine_scope_id(graph, array);
                break;
            case "entry_func_name":
                readLine_entry_func_name(graph, array);
                break;
            default:
                return false;
        }
        return true;
    }

    protected void readLine_entry_func_name(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'entry_func_name' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) &&
                    !(list.get(2) instanceof String)
            ) throw new ASTJsonException("'entry_func_name' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'entry_func_name' have missing values - " + list.toString());
        }
        Integer idNode   = (Integer) list.get(1);
        String  funcName = (String)  list.get(2);
        graph.addFuncEntryNode(idNode, funcName);
    }

    protected void readLine_ast_pt(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'ast_pt' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'ast_pt' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'ast_pt' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getNodeAstPtr(idNode) != defsInt.UNDEF_VAL && graph.getNodeAstPtr(idNode) != null)
            throw new ASTJsonException("Duplicate 'ast_pt' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.setNodePtr(ptr, idNode);
    }

    protected void readLine_call_end(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'call_end' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'call_end' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'call_end' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getCallEnd(idNode) != null)
            throw new ASTJsonException("Duplicate 'call_end' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addCall(idNode, ptr);
    }
    protected void readLine_call_expr(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'call_expr' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'call_expr' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'call_expr' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getCallExpr(idNode) != null)
            throw new ASTJsonException("Duplicate 'call_expr' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addCallExpr(idNode, ptr);
    }

    protected void readLine_op_hands(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 4)
                throw new ASTJsonException("'op_hands' should have 3 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer) || (list.get(3) != JSONObject.NULL && !(list.get(3) instanceof Integer))
            ) throw new ASTJsonException("'op_hands' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'op_hands' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getCallEnd(idNode) != null)
            throw new ASTJsonException("Duplicate 'op_hands' for node id " + idNode);
        Integer ptr1 = (Integer) list.get(2);
        Integer ptr2;
        if(list.get(3) != JSONObject.NULL)
            ptr2 = (Integer) list.get(3);
        else ptr2 = null;
        graph.setOpHands(idNode, new Pair<>(ptr1, ptr2));
    }

    protected void readLine_func_call_arg(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 4)
                throw new ASTJsonException("'func_call_arg' should have 3 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer || list.get(3) instanceof Integer)
            ) throw new ASTJsonException("'func_call_arg' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'func_call_arg' have missing values - " + list.toString());
        }

        Integer idNode = (Integer) list.get(1);
        Integer argId = (Integer) list.get(2);
        Integer argNode = (Integer) list.get(3);
        if((graph.getFuncCallArgs(idNode) == null && argId != 0) || (graph.getFuncCallArgs(idNode) != null && graph.getFuncCallArgs(idNode).size() != argId)) {
            throw new ASTJsonException("Wrong argument for 'func_call_arg' index id " + idNode);
        }
        if(graph.getFuncCallArgs(idNode) != null && graph.getFuncCallArgs(idNode).contains(argNode))
            throw new ASTJsonException("Duplicate 'func_call_arg' argument for node id " + idNode);
        graph.addFuncCallArg(idNode, argNode);
    }

    protected void readLine_func_def_param(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 4)
                throw new ASTJsonException("'func_def_param' should have 3 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer || list.get(3) instanceof Integer)
            ) throw new ASTJsonException("'func_def_param' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'func_def_param' have missing values - " + list.toString());
        }

        Integer idNode = (Integer) list.get(1);
        Integer argId = (Integer) list.get(2);
        Integer argNode = (Integer) list.get(3);
        if((graph.getFuncDefParam(idNode) == null && argId != 0) || (graph.getFuncDefParam(idNode) != null && graph.getFuncDefParam(idNode).size() != argId)) {
            throw new ASTJsonException("Wrong argument for 'func_def_param' index id " + idNode);
        }
        if(graph.getFuncDefParam(idNode) != null && graph.getFuncDefParam(idNode).contains(argNode))
            throw new ASTJsonException("Duplicate 'func_def_param' argument for node id " + idNode);
        graph.addFuncDefParam(idNode, argNode);
    }

    protected void readLine_var_id(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'var_id' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'var_id' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'var_id' have missing values - " + list.toString());
        }

        Integer idNode = (Integer) list.get(1);
        Integer var_id = (Integer) list.get(2);

        if(graph.getVarId(idNode) != null)
            throw new ASTJsonException("Duplicate 'var_id' argument for node id " + idNode);

        graph.setVarId(idNode, var_id);
    }
    protected void readLine_var_scope(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'var_scope' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'var_scope' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'var_scope' have missing values - " + list.toString());
        }

        Integer idNode = (Integer) list.get(1);
        Integer scope_id = (Integer) list.get(2);

        if(graph.getVarScope(idNode) != null)
            throw new ASTJsonException("Duplicate 'var_scope' argument for node id " + idNode);

        graph.setVarScope(idNode, scope_id);
    }

    protected void readLine_scope_id(CFG graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'scope_id' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'scope_id' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'scope_id' have missing values - " + list.toString());
        }

        Integer idNode = (Integer) list.get(1);
        Integer scope_id = (Integer) list.get(2);

        if(graph.getScopeId(idNode) != null)
            throw new ASTJsonException("Duplicate 'scope_id' argument for node id " + idNode);

        graph.setScopeId(idNode, scope_id);
    }
}
