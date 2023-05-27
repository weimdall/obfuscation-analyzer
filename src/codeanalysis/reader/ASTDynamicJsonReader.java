/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.reader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.polymtl.codeanalysis.exceptions.*;
import org.polymtl.codeanalysis.model.*;

import java.io.BufferedReader;
import java.io.IOException;

public class ASTDynamicJsonReader extends ASTJsonReader {

    public ASTDynamic read(String filename) throws IOException {
        return this.read(this.read_file(filename));
    }
    public ASTDynamic read(BufferedReader reader) throws IOException, JSONException {
        ASTDynamic graph = new ASTDynamic();
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

    protected boolean read_line(ASTDynamic graph, JSONArray array) {
        if(super.read_line(graph, array))
            return true;
        switch ((String) array.get(0)) {
            case "dynres_succ":
                readLine_dynres_succ(graph, array);
                break;
            case "datares_succ":
                readLine_datares_succ(graph, array);
                break;
            case "parse_succ":
                readLine_parse_succ(graph, array);
                break;
            case "eval_code":
                readLine_eval_code(graph, array);
                break;
            case "eval_pattern":
                readLine_eval_pattern(graph, array);
                break;
            default:
                return false;
        }
        return true;
    }

    protected void readLine_eval_pattern(ASTDynamic graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'eval_pattern' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                            !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'eval_pattern' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'eval_pattern' have missing values - " + list.toString());
        }

        int[] idNode = { (Integer) list.get(1), (Integer) list.get(2) };
        graph.addPatternMatched(idNode[0], idNode[1]);
    }

    protected void readLine_eval_code(ASTDynamic graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'eval_code' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                            !(list.get(2) instanceof String)
            ) throw new ASTJsonException("'eval_code' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'eval_code' have missing values - " + list.toString());
        }

        int idNode = (Integer) list.get(1);
        if(graph.getEvalString(idNode) != null)
            throw new ASTJsonException("Duplicate 'eval_code' for node id " + idNode);
        String code = (String) list.get(2);
        graph.setEvalString(idNode, code);
    }

    protected void readLine_dynres_succ(ASTDynamic graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'dynres_succ' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                            !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'dynres_succ' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'dynres_succ' have missing values - " + list.toString());
        }

        int[] idNode = { (Integer) list.get(1), (Integer) list.get(2) };
        graph.addDynamicResolution(idNode[0], idNode[1]);
    }

    protected void readLine_datares_succ(ASTDynamic graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'datares_succ' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                            !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'datares_succ' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'datares_succ' have missing values - " + list.toString());
        }

        int[] idNode = { (Integer) list.get(1), (Integer) list.get(2) };
        graph.addDataflowResolution(idNode[0], idNode[1]);
    }

    protected void readLine_parse_succ(ASTDynamic graph, JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 3)
                throw new ASTJsonException("'parser_succ' should have 2 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof Integer) ||
                            !(list.get(2) instanceof Integer)
            ) throw new ASTJsonException("'parser_succ' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'parser_succ' have missing values - " + list.toString());
        }

        int[] idNode = { (Integer) list.get(1), (Integer) list.get(2) };
        graph.addParseEdge(idNode[0], idNode[1]);
    }

}
