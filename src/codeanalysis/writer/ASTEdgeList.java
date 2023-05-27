package org.polymtl.codeanalysis.writer;

import org.polymtl.codeanalysis.util.Pair;
import org.polymtl.codeanalysis.model.CFG;
import org.polymtl.codeanalysis.model.Graph;
import org.json.JSONArray;
import org.polymtl.codeanalysis.exceptions.ASTJsonException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class ASTEdgeList implements Printer  {
    protected Map<Pair<String, String>, Integer> edgeFreqTable = new HashMap<>();
    protected Map<Integer, String> compIdTable = new HashMap<>();
    protected Graph tree;
    private PrintWriter writer = null;
    //private int comp_n = 0;
    //static final private List<String> listComp = Arrays.asList("CompoundStatement", "Block", "StartOfFunction");
    private List<Integer> visited = null;
    private boolean showToken;

    public ASTEdgeList(Graph parAstTable, boolean token, File f) throws FileNotFoundException {
        tree = parAstTable;
        showToken = token;
        writer = new PrintWriter(f);
    }

    private void print(Object... args) {
        JSONArray array = new JSONArray();
        for (Object arg : args) {
            array.put(arg.toString());
        }
        writer.write("  "+(new JSONArray(array)).toString()+",\n");
    }


    @Override
    public void print() {
        visited = new ArrayList<>();
        writer.write("[\n");
        visit(tree.getRoot());
        writer.write("]\n");
        writer.flush();
        writer.close();
    }


    public void visit(Integer currNode) throws ASTJsonException {
        if(visited.contains(currNode))
            return;
        visited.add(currNode);

        if(showToken && tree.getImage(currNode) != null)
            print("node", currNode, tree.getType(currNode), tree.getImage(currNode));
        else
            print("node", currNode, tree.getType(currNode));

        List<Integer> children = tree.getChildren(currNode);
        if(children != null) {
            for (Integer childNode : children)
                print("edge", currNode, childNode);
            for (Integer childNode : children)
                visit(childNode);
        }

        /*List<Integer> parent = tree.getParent(currNode); // Dead code
        if (parent != null) {
            for (Integer parentId : parent) {
                visit(parentId);
            }
        }*/

        if(tree instanceof CFG) {
            Integer callEnd = ((CFG) tree).getCallEnd(currNode);
            if (callEnd != null)
                visit(callEnd);
        }
    }

}
