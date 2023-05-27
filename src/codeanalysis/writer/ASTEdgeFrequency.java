package org.polymtl.codeanalysis.writer;

import org.json.JSONArray;
import org.polymtl.codeanalysis.exceptions.ASTJsonException;
import org.polymtl.codeanalysis.model.Graph;
import org.polymtl.codeanalysis.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class ASTEdgeFrequency implements Printer  {
    protected Map<Pair<String, String>, Integer> edgeFreqTable = new HashMap<>();
    protected Map<Integer, String> compIdTable = new HashMap<>();
    protected Graph tree;
    private PrintWriter writer = null;
    //private int comp_n = 0;
    //static final private List<String> listComp = Arrays.asList("CompoundStatement", "Block", "StartOfFunction");
    private List<Integer> visited = null;

    public ASTEdgeFrequency(Graph parAstTable, File f) throws FileNotFoundException {
        tree = parAstTable;
        writer = new PrintWriter(f);
    }

    public Map<Pair<String, String>, Integer> getEdgeFreqTable() {
        return edgeFreqTable;
    }

    public void print(int root) {
        visit(root);
        // edgeFreq edgestartnode edgeendnode freq
        writer.write("[\n");
        getEdgeFreqTable().forEach((k,v) -> {
            writer.write("  "+(new JSONArray(Arrays.asList("edgeFreq", k.getKey(), k.getValue(), v))).toString()+",\n");
        });
        writer.write("]\n");
        writer.flush();
        writer.close();
    }

    @Override
    public void print() {
        visited = new ArrayList<>();
        print(tree.getRoot());
    }

    public String transformedType(Integer nodeId) {
        return tree.getType(nodeId);
        /*String type = tree.getType(nodeId);
        if(listComp.contains(type))
        {
            if(compIdTable.containsKey(nodeId))
                return compIdTable.get(nodeId);
            type += "_" + comp_n;
            comp_n += 1;
            compIdTable.put(nodeId, type);
        }
        return type;*/
    }

    //
    // dispatch visit
    //
    public void visit(Integer currNode) throws ASTJsonException {
        if(visited.contains(currNode))
            return;
        visited.add(currNode);

        List<Integer> children = tree.getChildren(currNode);
        if (children == null || children.size() <= 0)
            return;

        for (Integer childNode : children)
            visit(childNode);

        Pair<String, String> pair;
        int freq;
        for (Integer childNode : children) {
            pair = new Pair<>(transformedType(currNode), transformedType(childNode));
            freq = edgeFreqTable.getOrDefault(pair, 0);
            edgeFreqTable.put(pair, freq + 1);
        }
    }

}
