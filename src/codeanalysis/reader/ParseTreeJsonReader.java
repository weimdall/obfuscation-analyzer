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

public class ParseTreeJsonReader extends GraphJsonReader implements defsInt {
    public ParseTree read(String filename) throws IOException {
        return this.read(this.read_file(filename));
    }
    public ParseTree read(BufferedReader reader) throws IOException, JSONException {
        ParseTree graph = new ParseTree();
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

    protected boolean read_line(ParseTree graph, JSONArray array) {
        if(super.read_line(graph, array))
            return true;
        switch ((String) array.get(0)) {
            case "id_set":
                break;
            default:
                return false;
        }
        return true;
    }
}
