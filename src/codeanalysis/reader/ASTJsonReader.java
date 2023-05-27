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

public class ASTJsonReader extends GraphJsonReader {

    public AST read(String filename) throws IOException {
        return this.read(this.read_file(filename));
    }
    public AST read(BufferedReader reader) throws IOException, JSONException {
        AST graph = new AST();
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

    protected boolean read_line(AST graph, JSONArray array) {
        if(super.read_line(graph, array))
            return true;
        switch ((String) array.get(0)) {
            case "parsetree_pt":
                readLine_parsetree_pt(graph, array);
                break;
            case "scope_id":
                readLine_ast_scope(graph, array);
                break;
            case "var_id":
                readLine_ast_var_id(graph, array);
                break;
            case "var_scope":
                readLine_ast_var_scope(graph, array);
                break;
            default:
                return false;
        }
        return true;
    }

    protected void readLine_ast_scope(AST graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'scope_id' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'scope_id' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'scope_id' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getScopeId(idNode) != null)
            throw new ASTJsonException("Duplicate 'scope_id' for node id " + idNode);
        Integer scope = (Integer) list.get(2);
        graph.setScopeId(idNode, scope);
    }

    protected void readLine_ast_var_id(AST graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'var_id' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'var_id' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'var_id' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getVarId(idNode) != null)
            throw new ASTJsonException("Duplicate 'var_id' for node id " + idNode);
        Integer var_id = (Integer) list.get(2);
        graph.setVarId(idNode, var_id);
    }

    protected void readLine_ast_var_scope(AST graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'var_scope' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'var_scope' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'var_scope' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getVarScope(idNode) != null)
            throw new ASTJsonException("Duplicate 'var_scope' for node id " + idNode);
        Integer var_scope = (Integer) list.get(2);
        graph.setVarScope(idNode, var_scope);
    }

    protected void readLine_parsetree_pt(AST graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'parsetree_pt' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) || !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'parsetree_pt' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'parsetree_pt' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getNodePTPtr(idNode) != defsInt.UNDEF_VAL)
            throw new ASTJsonException("Duplicate 'parsetree_pt' for node id " + idNode);
        Integer ptr = (Integer) list.get(2);
        graph.setNodePtr(ptr, idNode);
    }
}
