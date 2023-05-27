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

public class CFGAndDDJsonReader extends CFGJsonReader implements defsInt {
    private CFGWithDD graph = null;

    public CFGWithDD read_cfg(String filename_cfg, String filename_dd) throws IOException, JSONException {
        graph = new CFGWithDD();
        read(filename_cfg);
        read(filename_dd);
        return graph;
    }
    public CFGWithDD read(String filename) throws IOException {
        return this.read(this.read_file(filename));
    }
    public CFGWithDD read(BufferedReader reader) throws IOException, JSONException {
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
            case "dd_succ":
                readLine_dd_succ(array);
                break;
            default:
                return false;
        }
        return true;
    }

    protected void readLine_dd_succ(JSONArray list) throws ASTJsonException {
        try {
            if (list.length() != 6)
                throw new ASTJsonException("'dd_succ' should have 6 parameters - " + list.toString());
            if (
                    !(list.get(1) instanceof String) || !(list.get(2) instanceof Integer) || !(list.get(3) instanceof Integer) || !(list.get(4) instanceof Integer) || !(list.get(5) instanceof Integer)
            ) throw new ASTJsonException("'dd_succ' have wrong arguments type - " + list.toString());
        } catch (IndexOutOfBoundsException e) {
            throw new ASTJsonException("'dd_succ' have missing values - " + list.toString());
        }

        Integer idNode = (Integer) list.get(3);
        Integer idNode2 = (Integer) list.get(5);

        switch ((String) list.get(1)) {
            case "global_def_use":
                graph.addGlobalDefUse(idNode, idNode2);
                break;
            case "intra_def_use":
                graph.addIntraDefUse(idNode, idNode2);
                break;

            case "cmd_propag":
                if(graph.getCmdPropag(idNode) != null)
                    throw new ASTJsonException("Duplicate 'dd_succ' 'cmd_propag' for node id " + idNode);
                graph.setCmdPropag(idNode, idNode2);
                break;

            default:
                throw new ASTJsonException("Unknown 'dd_succ' type " + list.get(1));
        }
    }
}
