package org.polymtl.codeanalysis.util;

import org.polymtl.codeanalysis.model.Graph;
import org.polymtl.codeanalysis.model.defsInt;

import java.util.*;

/**
 * Copyright (C) 2014, 2015, 2016, 2017, 2019 Ettore Merlo - All rights reserved
 */

public class ASTNavigatorCl<nodeIndexType, matchValType> implements defsInt {

    protected Map<nodeIndexType, List<nodeIndexType>> astAdjTable = null;
    //private HashMap<nodeIndexType, matchValType> astTypeTable = null;
    protected Map<nodeIndexType, matchValType> astTypeTable = null;


    //astNavigatorCl(HashMap<nodeIndexType,
    //		   ArrayList<nodeIndexType>> parAdjTable,
    public ASTNavigatorCl(Map<nodeIndexType,
            //Collection<nodeIndexType>> parAdjTable,
            List<nodeIndexType>> parAdjTable,
                   //HashMap<nodeIndexType,
                   Map<nodeIndexType,
                           matchValType> parTypeTable) {

        astAdjTable = parAdjTable;
        astTypeTable = parTypeTable;
    }


    public int getChildrenSize(nodeIndexType node) {
        if (astAdjTable.containsKey(node)) {
            return (astAdjTable.get(node).size());
        } else {
            return (0);
        }
    }


    public boolean match(nodeIndexType node,
                         matchValType val) {

        return (match(node,
                val,
                astTypeTable));
    }

    public boolean match(nodeIndexType node,
                         matchValType val,
                         //HashMap<nodeIndexType,
                         Map<nodeIndexType,
                                 matchValType> parTypeTable) {

        boolean res = false;

        if (node != null) {
            if (parTypeTable.containsKey(node)) {
                matchValType nodeType = parTypeTable.get(node);
                if (nodeType.equals(val)) {
                    res = true;
                } else {
                    res = false;
                }
            } else {
                res = false;
            }
        } else {
            res = false;
        }

        return (res);
    }

    public boolean match(nodeIndexType node, matchValType val, List<Integer> path) {
        return match(getIndexedNode(node, path), val);
    }


    public List<nodeIndexType> getChildren(nodeIndexType node) {
        return getChildren(node, Collections.emptyList());
        //return astAdjTable.get(node);
    }

    public List<nodeIndexType> getChildren(nodeIndexType node, List<Integer> path) {
        return astAdjTable.get(getIndexedNode(node, path));
    }

    public boolean matchChildrenTypes(nodeIndexType node, List<matchValType> types, List<Integer> path) {
        return matchChildrenTypes(getIndexedNode(node, path), types);
    }

    public boolean matchChildrenTypes(nodeIndexType node, List<matchValType> types) {

        if (types.size() != this.getChildrenSize(node))
            return false;

        for (int i = 0 ; i < types.size() ; i++)
            if (!match(getChild(node, i), types.get(i)))
                return false;

        return true;
    }


    public boolean matchIndexedAdj(nodeIndexType node,
                                   int index,
                                   matchValType adjType) {

        return (matchIndexedAdj(node,
                index,
                astAdjTable,
                adjType,
                astTypeTable));
    }

    public boolean matchIndexedAdj(nodeIndexType node,
                                   int index,
                                   Map<nodeIndexType,
                                           List<nodeIndexType>> parAdjTable,
                                   matchValType val,
                                   Map<nodeIndexType,
                                           matchValType> parTypeTable) {

        boolean res = false;
        nodeIndexType adjNode = null;

        if (node != null) {

            try {
                adjNode = parAdjTable.get(node).get(index);
            } catch (IndexOutOfBoundsException e) {
                return (false);
            }

            if (parTypeTable.containsKey(adjNode)) {

                matchValType nodeType = parTypeTable.get(adjNode);

                if (nodeType.equals(val)) {
                    res = true;
                } else {
                    res = false;
                }
            } else {
                res = false;
            }
        } else {
            res = false;
        }

        return (res);
    }

    public nodeIndexType getChild(nodeIndexType node,
                                  int index) {

        return (getChild(node,
                index,
                astAdjTable));
    }

    public nodeIndexType getChild(nodeIndexType node,
                                  int index,
                                  //HashMap<nodeIndexType,
                                  //ArrayList<nodeIndexType>> parAdjTable) {
                                  Map<nodeIndexType,
                                          //Collection<nodeIndexType>> parAdjTable) {
                                          //ArrayList<nodeIndexType>> parAdjTable) {
                                          List<nodeIndexType>> parAdjTable) {

        nodeIndexType childNode = null;

        if (node == null)
            return (null);

        if (parAdjTable.containsKey(node)) {

            try {
                return (parAdjTable.get(node).get(index));
            } catch (IndexOutOfBoundsException e) {
                return (null);
            }

        } else {
            return (null);
        }
    }

    public nodeIndexType getIndexedAdj(nodeIndexType node,
                                       int index) {
        return (getIndexedAdj(node,
                index,
                astAdjTable));
    }

    public nodeIndexType getIndexedAdj(nodeIndexType node,
                                       int index,
                                       //HashMap<nodeIndexType,
                                       Map<nodeIndexType,
                                               //ArrayList<nodeIndexType>> parAdjTable) {
                                               List<nodeIndexType>> parAdjTable) {

        nodeIndexType adjNode = null;

        if (node == null)
            return (null);

        if (parAdjTable.containsKey(node)) {

            try {
                adjNode = parAdjTable.get(node).get(index);
            } catch (IndexOutOfBoundsException e) {
                return (null);
            }

        } else {
            adjNode = null;
        }

        return (adjNode);
    }

    public nodeIndexType getIndexedMatchedAdj(nodeIndexType node,
                                              int index,
                                              matchValType adjType) {

        return (getIndexedMatchedAdj(node,
                index,
                astAdjTable,
                adjType,
                astTypeTable));
    }

    public nodeIndexType getIndexedMatchedAdj(nodeIndexType node,
                                              int index,
                                              //HashMap<nodeIndexType,
                                              Map<nodeIndexType,
                                                      //ArrayList<nodeIndexType>> parAdjTable,
                                                      List<nodeIndexType>> parAdjTable,
                                              matchValType adjType,
                                              //HashMap<nodeIndexType,
                                              Map<nodeIndexType,
                                                      matchValType> parTypeTable) {

        nodeIndexType adjNode = null;

        if (node == null)
            return (null);

        if (parAdjTable.containsKey(node)) {

            try {
                adjNode = parAdjTable.get(node).get(index);
            } catch (IndexOutOfBoundsException e) {
                return (null);
            }

            matchValType adjNodeType = parTypeTable.get(adjNode);

            if (adjNodeType == null) {
                System.err.println("ERROR: missing type for adjacent node " +
                        adjNode +
                        " from " +
                        node);
                System.exit(1);
            }

            if (!adjNodeType.equals(adjType)) {
                adjNode = null;
            }

        } else {
            adjNode = null;
        }

        return (adjNode);
    }

    public nodeIndexType getFirstMatchedAdj(nodeIndexType node,
                                            matchValType adjType) {

        return (getFirstMatchedAdj(node,
                adjType,
                astAdjTable,
                astTypeTable));
    }

    public nodeIndexType getFirstMatchedAdj(nodeIndexType node,
                                            matchValType adjType,
                                            //HashMap<nodeIndexType,
                                            Map<nodeIndexType,
                                                    //ArrayList<nodeIndexType>> parAdjTable,
                                                    List<nodeIndexType>> parAdjTable,
                                            //HashMap<nodeIndexType,
                                            Map<nodeIndexType,
                                                    matchValType> parTypeTable) {

        nodeIndexType retNode = null;
        nodeIndexType adjNode = null;
        matchValType adjNodeType = null;

        if (node == null)
            return (null);

        if (parAdjTable.containsKey(node)) {

            Iterator<nodeIndexType> adjIt = parAdjTable.get(node).iterator();

            boolean found = false;
            while ((!found) && (adjIt.hasNext())) {

                adjNode = adjIt.next();
                adjNodeType = parTypeTable.get(adjNode);

                if (adjNodeType == null) {
                    System.err.println("ERROR: missing type for adjacent node " +
                            adjNode +
                            " from " +
                            node);
                    System.exit(1);
                }

                if (adjNodeType.equals(adjType)) {

                    found = true;
                    retNode = adjNode;

                } // end if adj type matches
            } // end adjs loop
        } else {

            // no adjs

            retNode = null;
        }

        return (retNode);
    }

    public nodeIndexType getMatchedAdj(nodeIndexType node,
                                       matchValType adjType) {

        return (getMatchedAdj(node,
                adjType,
                astAdjTable,
                astTypeTable));
    }

    public nodeIndexType getMatchedAdj(nodeIndexType node,
                                       matchValType adjType,
                                       //HashMap<nodeIndexType,
                                       Map<nodeIndexType,
                                               //ArrayList<nodeIndexType>> parAdjTable,
                                               List<nodeIndexType>> parAdjTable,
                                       //HashMap<nodeIndexType,
                                       Map<nodeIndexType,
                                               matchValType> parTypeTable) {

        nodeIndexType adjNode = null;
        nodeIndexType retNode = null;
        matchValType adjNodeType = null;

        if (node == null)
            return (null);

        if (parAdjTable.containsKey(node)) {

            //
            // traverse children
            //

            Iterator<nodeIndexType> adjIt = parAdjTable.get(node).iterator();

            boolean found = false;

            while (adjIt.hasNext()) {

                adjNode = adjIt.next();
                adjNodeType = parTypeTable.get(adjNode);

                if (adjNodeType == null) {
                    System.err.println("ERROR: missing type for adjacent node " +
                            adjNode +
                            " from " +
                            node);
                    System.exit(1);
                }

                if (adjNodeType.equals(adjType)) {

                    if (found) {
                        System.err.println("WARNING: multiple node " +
                                adjNode +
                                " of type " +
                                adjType +
                                " found from " +
                                node);
                    } else {
                        found = true;
                        retNode = adjNode;
                    }
                } // end if adj type matches
            } // end adjs loop
        } else {

            // no adjs

            retNode = null;
        }

        return (retNode);
    }

    public int getFirstMatchedAdjIndex(nodeIndexType node,
                                       matchValType adjType) {

        return (getFirstMatchedAdjIndex(node,
                adjType,
                astAdjTable,
                astTypeTable));
    }

    public int getFirstMatchedAdjIndex(nodeIndexType node,
                                       matchValType adjType,
                                       //HashMap<nodeIndexType,
                                       Map<nodeIndexType,
                                               //ArrayList<nodeIndexType>> parAdjTable,
                                               List<nodeIndexType>> parAdjTable,
                                       //HashMap<nodeIndexType,
                                       Map<nodeIndexType,
                                               matchValType> parTypeTable) {

        nodeIndexType adjNode = null;
        matchValType adjNodeType = null;
        int retIndex = UNDEF_VAL;

        if (node == null)
            return (UNDEF_VAL);

        if (parAdjTable.containsKey(node)) {

            //
            // traverse adjs
            //

            //Iterator<String> adjIt = parAdjTable.getRangeIterator(node);

            boolean found = false;

            int index = 0;
            int adjSize = parAdjTable.size();

            while ((!found) && (index < adjSize)) {

                try {
                    adjNode = parAdjTable.get(node).get(index);
                } catch (IndexOutOfBoundsException e) {
                    return (UNDEF_VAL);
                }

                adjNodeType = parTypeTable.get(adjNode);

                if (adjNodeType == null) {
                    System.err.println("ERROR: missing type for adjacent node " +
                            adjNode +
                            " from " +
                            node);
                    System.exit(1);
                }

                if (adjNodeType.equals(adjType)) {

                    found = true;
                    retIndex = index;

                } // end if adj type matches
                else {
                    index++;
                }

            } // end adjs loop
        } else {
            // node has no adjs

            //adjNode = null;
            retIndex = UNDEF_VAL;
        }

        //return(adjNode);
        return (retIndex);
    }

    public int getMatchedAdjIndex(nodeIndexType node,
                                  matchValType adjType) {

        return (getMatchedAdjIndex(node,
                adjType,
                astAdjTable,
                astTypeTable));
    }

    public int getMatchedAdjIndex(nodeIndexType node,
                                  matchValType adjType,
                                  //HashMap<nodeIndexType,
                                  Map<nodeIndexType,
                                          //ArrayList<nodeIndexType>> parAdjTable,
                                          List<nodeIndexType>> parAdjTable,
                                  //HashMap<nodeIndexType,
                                  Map<nodeIndexType,
                                          matchValType> parTypeTable) {

        nodeIndexType adjNode = null;
        nodeIndexType retNode = null;
        matchValType adjNodeType = null;
        int retIndex = UNDEF_VAL;

        if (node == null)
            return (UNDEF_VAL);

        if (parAdjTable.containsKey(node)) {

            //
            // traverse adjs
            //

            //Iterator<String> adjIt = parAdjTable.getRangeIterator(node);

            boolean found = false;

            int index = 0;
            int adjSize = parAdjTable.get(node).size();

            //while (adjIt.hasNext()) {
            while (index < adjSize) {

                //System.out.println("DEBUG INDEX: " + index);

                //adjNode = adjIt.next();
                try {
                    adjNode = parAdjTable.get(node).get(index);
                } catch (IndexOutOfBoundsException e) {
                    return (UNDEF_VAL);
                }

                //System.out.println("DEBUG ADJ NODE: " + adjNode);

                adjNodeType = parTypeTable.get(adjNode);

                if (adjNodeType == null) {
                    System.err.println("ERROR: missing type for adjacent node " +
                            adjNode +
                            " from " +
                            node);
                    System.exit(1);
                }

                //System.out.println("DEBUG ADJ NODE TYPE: " + adjNodeType);

                if (adjNodeType.equals(adjType)) {

                    //System.out.println("DEBUG ADJ NODE TYPE MATCH");

                    if (found) {
                        System.err.println("WARNING: multiple node " +
                                adjNode +
                                " of type " +
                                adjType +
                                " found from " +
                                node);
                    } else {
                        //System.out.println("DEBUG ADJ NODE TYPE FIRST MATCH");

                        found = true;
                        //retNode = adjNode;
                        retIndex = index;
                    }

                } // end if adj type matches

                index++;

            } // end adjs loop

        } else {

            // node has no adjs

            //retNode = null;
            retIndex = UNDEF_VAL;
        }

        //return(retNode);
        return (retIndex);
    }

    public nodeIndexType getIndexedNode(nodeIndexType node, List<Integer> path) {
        return (getIndexedNode(node,
                path,
                astAdjTable));
    }

    public nodeIndexType getIndexedNode(nodeIndexType node,
                                        List<Integer> path,
                                        //HashMap<nodeIndexType,
                                        Map<nodeIndexType,
                                                //ArrayList<nodeIndexType>> parAdjTable) {
                                                List<nodeIndexType>> parAdjTable) {

        if (node == null)
            return (null);

        nodeIndexType nextNode = node;

        for (Integer item : path) {
            if (parAdjTable.containsKey(nextNode)) {

                try {
                    nextNode = parAdjTable.get(nextNode).get(item);
                } catch (IndexOutOfBoundsException e) {
                    return (null);
                }

            } else {
                return (null);
            }
        }

        return (nextNode);
    }

    public nodeIndexType getIndexedMatchedNode(nodeIndexType node,
                                               ArrayList<Integer> path,
                                               ArrayList<matchValType> pathType) {

        return (getIndexedMatchedNode(node,
                path,
                pathType,
                astAdjTable,
                astTypeTable));
    }

    public nodeIndexType getIndexedMatchedNode(nodeIndexType node,
                                               ArrayList<Integer> path,
                                               ArrayList<matchValType> pathType,
                                               //HashMap<nodeIndexType,
                                               Map<nodeIndexType,
                                                       //ArrayList<nodeIndexType>> parAdjTable,
                                                       List<nodeIndexType>> parAdjTable,
                                               //HashMap<nodeIndexType,
                                               Map<nodeIndexType,
                                                       matchValType> parTypeTable) {

        nodeIndexType nextNode = node;
        //matchValType nextType = node;
        matchValType nextType = null;

        if (node == null)
            return (null);

        if (path.size() != pathType.size()) {
            System.err.println("ERROR: path size " +
                    path.size() +
                    " doesn't match type size " +
                    pathType.size());
            System.exit(1);
        }

        for (int i = 0; i < path.size(); i++) {

            Integer item = path.get(i);
            matchValType refType = pathType.get(i);

	    /*
	    System.out.println("DEBUG I: " +
			       i +
			       " PATH INDEX: " +
			       item +
			       " TYPE: " +
			       refType);
	    */

            //for (Integer item: path) {
            if (parAdjTable.containsKey(nextNode)) {

                try {
                    nextNode = parAdjTable.get(nextNode).get(item);
                } catch (IndexOutOfBoundsException e) {
                    return (null);
                }

                nextType = parTypeTable.get(nextNode);

                if (!nextType.equals(refType)) {
                    return (null);
                }

            } else {
                return (null);
            }
        }

        return (nextNode);
    }

    public nodeIndexType getFirstMatchedNode(nodeIndexType node,
                                             ArrayList<matchValType> pathType) {

        return (getFirstMatchedNode(node,
                pathType,
                astAdjTable,
                astTypeTable));
    }

    public nodeIndexType getFirstMatchedNode(nodeIndexType node,
                                             ArrayList<matchValType> pathType,
                                             //HashMap<nodeIndexType,
                                             Map<nodeIndexType,
                                                     //ArrayList<nodeIndexType>> parAdjTable,
                                                     List<nodeIndexType>> parAdjTable,
                                             //HashMap<nodeIndexType,
                                             Map<nodeIndexType,
                                                     matchValType> parTypeTable) {

        nodeIndexType nextNode = null;
        matchValType nextType = null;
        nodeIndexType childNode = null;
        matchValType childNodeType = null;

        if (node == null)
            return (null);

        nextNode = node;

        for (int i = 0; i < pathType.size(); i++) {

            matchValType refType = pathType.get(i);

            //for (Integer item: path) {
            if (parAdjTable.containsKey(nextNode)) {

                //
                // get typed child (inlined)
                //

                //
                // traverse children
                //

                Iterator<nodeIndexType> adjIt = parAdjTable.get(nextNode).iterator();

                boolean found = false;
                nextNode = null;

                while ((!found) && (adjIt.hasNext())) {

                    childNode = adjIt.next();
                    childNodeType = parTypeTable.get(childNode);

                    if (childNodeType == null) {
                        System.err.println("ERROR: missing type for child node " +
                                childNode +
                                " from " +
                                node);
                        System.exit(1);
                    }

                    if (childNodeType.equals(refType)) {

                        found = true;
                        nextNode = childNode;

                    } // end if child type matches

                } // end children loop

                if (!found)
                    return (null);

            } else {
                // node has no children
                return (null);
            }
        }

        return (nextNode);
    }

    public nodeIndexType getSingleMatchedNode(nodeIndexType node,
                                              ArrayList<matchValType> pathType) {

        return (getSingleMatchedNode(node,
                pathType,
                astAdjTable,
                astTypeTable));
    }

    public nodeIndexType getSingleMatchedNode(nodeIndexType node,
                                              ArrayList<matchValType> pathType,
                                              //HashMap<nodeIndexType,
                                              Map<nodeIndexType,
                                                      //ArrayList<nodeIndexType>> parAdjTable,
                                                      List<nodeIndexType>> parAdjTable,
                                              //HashMap<nodeIndexType,
                                              Map<nodeIndexType,
                                                      matchValType> parTypeTable) {

        // PREVIOUS: public String getSingleMatchedNode(String node,


        nodeIndexType nextNode = null;
        matchValType nextType = null;
        nodeIndexType childNode = null;
        matchValType childNodeType = null;

        if (node == null)
            return (null);

        nextNode = node;

        for (int i = 0; i < pathType.size(); i++) {

            matchValType refType = pathType.get(i);

            //for (Integer item: path) {
            if (parAdjTable.containsKey(nextNode)) {

                //
                // get typed child (inlined)
                //

                //
                // traverse children
                //

                Iterator<nodeIndexType> adjIt = parAdjTable.get(nextNode).iterator();

                nodeIndexType msgNode = nextNode;

                boolean found = false;
                nextNode = null;

                while (adjIt.hasNext()) {

                    childNode = adjIt.next();
                    childNodeType = parTypeTable.get(childNode);

                    if (childNodeType == null) {
                        System.err.println("ERROR: missing type for child node " +
                                childNode +
                                " from " +
                                node);
                        System.exit(1);
                    }

                    if (childNodeType.equals(refType)) {

                        if (found) {
                            System.err.println("WARNING: multiple node " +
                                    childNode +
                                    " of type " +
                                    refType +
                                    " found from " +
                                    msgNode);
                        } else {
                            found = true;
                            nextNode = childNode;
                        }

                    } // end if child type matches

                } // end children loop

                if (!found)
                    return (null);

            } else {
                // node has no children
                return (null);
            }
        }

        return (nextNode);
    }


}
