/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.reader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.exceptions.ASTJsonException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CFGforDDJsonReader extends GraphJsonReader implements defsInt {
    private CFGforDD graph = null;
    public CFGforDD read(String filename) throws IOException {
        return this.read(this.read_file(filename));
    }
    public CFGforDD read(BufferedReader reader) throws IOException, JSONException {
        graph = new CFGforDD();
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


            switch ((String) array.get(0)) {
                case "filename":
                    readLine_filename(array);
                    break;
                case "node_type":
                    readLine_type(array);
                    break;
                case "dot_label":
                    readLine_token(array);
                    break;
                case "entry_func_name":
                    readLine_entry_func_name(array);
                    break;
                case "entry":
                    readLine_entry(array);
                    break;
                case "exit":
                    readLine_exit(array);
                    break;
                case "include_begin":
                    readLine_include_begin(array);
                    break;
                case "include_end":
                    readLine_include_end(array);
                    break;
                case "call_begin":
                    readLine_call_begin(array);
                    break;
                case "call_end":
                    readLine_call_end(array);
                    break;
                case "call_begin_func_name":
                    readLine_call_begin_func_name(array);
                    break;
                case "call_end_func_name":
                    readLine_call_end_func_name(array);
                    break;
                case "rhs":
                    readLine_rhs(array);
                    break;
                case "lhs":
                    readLine_lhs(array);
                    break;
                case "act_arg":
                    readLine_act_arg(array);
                    break;
                case "expr_arg":
                    readLine_expr_arg(array);
                    break;
                case "form_arg":
                    readLine_form_arg(array);
                    break;
                case "act_ret":
                    readLine_act_ret(array);
                    break;
                case "form_ret":
                    readLine_form_ret(array);
                    break;
                case "access":
                    readLine_access(array);
                    break;
                case "cfg_succ":
                    readLine_cfg_succ(array);
                    break;
                case "cfg_attr":
                    readLine_cfg_attr(array);
                    break;
                default:
                    // If unknown -> continue
                    throw new ASTJsonException("Unknown line type : " + line);
            }

            line = reader.readLine();
        }
        reader.close();
        return graph;
    }

    protected void readLine_type(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'type' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                    !(list.get(2) instanceof String)
            ) throw new ASTJsonException("'type' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'type' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        String type = (String) list.get(2);
        if(graph.getTypeTable().containsKey(idNode))
            throw new ASTJsonException("Duplicate 'type' for node id " + idNode);
        graph.setNodeType(idNode, type);
    }


    protected void readLine_cfg_attr(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'cfg_attr' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof String) ||
                            !(list.get(2) instanceof String)
            ) throw new ASTJsonException("'cfg_attr' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'cfg_attr' have missing values - " + list.toString());
        }

        String id = (String) list.get(1);
        String value = (String) list.get(2);
        if(graph.getAttr().containsKey(id))
            throw new ASTJsonException("Duplicate 'cfg_attr' for node id " + id);
        graph.addAttr(id, value);
    }


    protected void readLine_cfg_succ(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'ast_succ' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                    !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'ast_succ' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'ast_succ' have missing values - " + list.toString());
        }

        int[] idNode = { (Integer) list.get(1), (Integer) list.get(2) };
        graph.addEdge(idNode[0], idNode[1]);
    }

    protected void readLine_filename(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 2)
                throw new ASTJsonException("'filename' should have 1 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof String)
            ) throw new ASTJsonException("'filename' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'filename' have missing values - " + list.toString());
        }
        graph.setFilename((String) list.get(1));
    }

    protected void readLine_entry_func_name(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 4)
                throw new ASTJsonException("'entry_func_name' should have 3 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) &&
                    !(list.get(2) instanceof Integer) &&
                    !(list.get(3) instanceof String)
            ) throw new ASTJsonException("'entry_func_name' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'entry_func_name' have missing values - " + list.toString());
        }
        Integer idNode   = (Integer) list.get(1);
        List<Object> funcName = Arrays.asList(list.get(2), list.get(3));
        graph.addEntryFuncName(idNode, funcName);
    }

    protected void readLine_token(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'token' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof String)
            ) throw new ASTJsonException("'token' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'token' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getImage(idNode) != null)
            throw new ASTJsonException("Duplicate 'token/image' for node id " + idNode);
        String token = (String) list.get(2);
        graph.setNodeImage(idNode, token);
    }

    protected void readLine_entry(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'entry' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                            !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'entry' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'entry' have missing values - " + list.toString());
        }

        int[] idNode = { (Integer) list.get(1), (Integer) list.get(2) };
        graph.addCfgEntry(idNode[0], idNode[1]);
    }

    protected void readLine_exit(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'exit' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                            !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'exit' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'exit' have missing values - " + list.toString());
        }

        int[] idNode = { (Integer) list.get(1), (Integer) list.get(2) };
        graph.addCfgEntry(idNode[0], idNode[1]);
    }

    protected void readLine_include_begin(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'include_begin' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'include_begin' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'include_begin' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getCallBegins(idNode) != null)
            throw new ASTJsonException("Duplicate 'include_begin' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addIncludeBegins(idNode, ptr);
    }

    protected void readLine_include_end(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'include_end' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'include_end' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'include_end' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getCallEnds(idNode) != null)
            throw new ASTJsonException("Duplicate 'include_end' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addIncludeEnds(idNode, ptr);
    }

    protected void readLine_call_begin(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'call_begin' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'call_begin' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'call_begin' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getCallBegins(idNode) != null)
            throw new ASTJsonException("Duplicate 'call_begin' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addCallBegins(idNode, ptr);
    }

    protected void readLine_call_end(JSONArray list) throws ASTJsonException {
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
        if(graph.getCallEnds(idNode) != null)
            throw new ASTJsonException("Duplicate 'call_end' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addCallEnds(idNode, ptr);
    }

    protected void readLine_call_begin_func_name(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'call_begin_func_name' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof String)
            ) throw new ASTJsonException("'call_begin_func_name' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'call_begin_func_name' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getCallBeginsFuncName(idNode) != null)
            throw new ASTJsonException("Duplicate 'call_begin_func_name' for node id " + idNode);
        String name = (String) list.get(2);
        graph.addCallBeginsFuncName(idNode, name);
    }

    protected void readLine_call_end_func_name(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'call_end_func_name' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof String)
            ) throw new ASTJsonException("'call_end_func_name' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'call_end_func_name' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getCallEndsFuncName(idNode) != null)
            throw new ASTJsonException("Duplicate 'call_end_func_name' for node id " + idNode);
        String name = (String) list.get(2);
        graph.addCallEndsFuncName(idNode, name);
    }

    protected void readLine_rhs(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'rhs' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'rhs' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'rhs' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getRhs(idNode) != null)
            throw new ASTJsonException("Duplicate 'rhs' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addRhs(idNode, ptr);
    }

    protected void readLine_lhs(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'lhs' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'lhs' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'lhs' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getLhs(idNode) != null)
            throw new ASTJsonException("Duplicate 'lhs' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addLhs(idNode, ptr);
    }

    protected void readLine_act_ret(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'act_ret' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'act_ret' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'act_ret' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getActRet(idNode) != null)
            throw new ASTJsonException("Duplicate 'act_ret' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addActRet(idNode, ptr);
    }

    protected void readLine_form_ret(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'form_ret' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'form_ret' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'form_ret' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getFormRet(idNode) != null)
            throw new ASTJsonException("Duplicate 'form_ret' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.addFormRet(idNode, ptr);
    }

    protected void readLine_act_arg(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 4)
                throw new ASTJsonException("'act_arg' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                    !(list.get(2) instanceof Integer) ||
                    !(list.get(3) instanceof Integer)
            ) throw new ASTJsonException("'act_arg' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'act_arg' have missing values - " + list.toString());
        }

        int index = (Integer) list.get(2);
        int idNode = (Integer) list.get(3);
        Integer key = (Integer) list.get(1);
        graph.addActArg(key, new Integer[]{index, idNode});
    }

    protected void readLine_form_arg(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 4)
                throw new ASTJsonException("'form_arg' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                            !(list.get(2) instanceof Integer) ||
                            !(list.get(3) instanceof Integer)
            ) throw new ASTJsonException("'form_arg' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'form_arg' have missing values - " + list.toString());
        }

        int index = (Integer) list.get(2);
        int idNode = (Integer) list.get(3);
        Integer key = (Integer) list.get(1);
        graph.addFormArg(key, new Integer[]{index, idNode});
    }

    protected void readLine_expr_arg(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'expr_arg' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                    !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'expr_arg' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'expr_arg' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(2);
        Integer key = (Integer) list.get(1);
        graph.addExprArg(key, idNode);
    }

    protected void readLine_access(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 6)
                throw new ASTJsonException("'access' should have 5 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                    !(list.get(2) instanceof Integer) ||
                    !(list.get(3) instanceof Integer) ||
                    !(list.get(4) instanceof Integer) ||
                    !(list.get(5) instanceof String)
            ) throw new ASTJsonException("'access' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'access' have missing values - " + list.toString());
        }

        graph.addAccess((Integer) list.get(1), Arrays.asList(list.get(2), list.get(3), list.get(4), list.get(5)));
    }
}
