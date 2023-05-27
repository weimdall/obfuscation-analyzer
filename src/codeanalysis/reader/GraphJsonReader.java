/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.reader;

import org.json.JSONArray;
import org.json.JSONException;
import org.polymtl.codeanalysis.exceptions.*;
import org.polymtl.codeanalysis.model.*;

import java.io.*;
import java.util.zip.GZIPInputStream;

abstract public class GraphJsonReader implements defsInt {
    protected BufferedReader read_file(String filename) throws IOException, JSONException {
        if(filename.endsWith(".gz"))
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
        return new BufferedReader(new FileReader(filename));
    }

    protected boolean read_line(Graph graph, JSONArray array) {
        switch ((String) array.get(0)) {
            case "node_root":
                if (!(array.get(1) instanceof Integer))
                    throw new ASTJsonException("Wrong node root: " + array);
                graph.setRoot((Integer) array.get(1));
                break;
            case "type":
                readLine_type(graph, array);
                break;
            case "ast_succ":
            case "cfg_succ":
            case "succ":
                readLine_succ(graph, array);
                break;
            case "token":
            case "image":
                readLine_token(graph, array);
                break;
            case "line_begin":
            case "line_end":
            case "column_begin":
            case "column_end":
            case "token_begin":
            case "token_end":
                readLine_position(graph, array);
                break;
            case "filename":
                readLine_filename(graph, array);
                break;
            default:
                return false;
        }
        return true;
    }

    protected void readLine_type(Graph graph, JSONArray list) throws ASTJsonException {
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

    protected void readLine_succ(Graph graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'succ' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                    !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'succ' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'succ' have missing values - " + list.toString());
        }

        int[] idNode = { (Integer) list.get(1), (Integer) list.get(2) };
        graph.addEdge(idNode[0], idNode[1]);
    }

    protected void readLine_filename(Graph graph, JSONArray list) throws ASTJsonException {
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

    protected void readLine_position(Graph graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'position' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                    !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'"+list.get(0)+"' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'"+list.get(0)+"' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        int pos = (Integer) list.get(2);
        Integer[] posList = {null, null, null, null, null, null};
        if(graph.getPositions(idNode) != null)
            posList = graph.getPositions(idNode);

        switch ((String) list.get(0)) {
            case "line_begin":
                posList[0] = pos;
                break;
            case "line_end":
                posList[1] = pos;
                break;
            case "column_begin":
                posList[2] = pos;
                break;
            case "column_end":
                posList[3] = pos;
                break;
            case "token_begin":
                posList[4] = pos;
                break;
            case "token_end":
                posList[5] = pos;
                break;
            default:
                throw new ASTJsonException("Logic error, position expected : " + list);
        }
        graph.setNodePosition(idNode, posList);
    }

    protected void readLine_token(Graph graph, JSONArray list) throws ASTJsonException {
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
}
