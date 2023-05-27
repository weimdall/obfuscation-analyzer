/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CFGWithDD extends CFG {
    protected Map<Integer, List<Integer>> globalDefUseSucc = new HashMap<>();
    protected Map<Integer, List<Integer>> globalDefUsePred = new HashMap<>();

    protected Map<Integer, List<Integer>> intraDefUseSucc = new HashMap<>();
    protected Map<Integer, List<Integer>> intraDefUsePred = new HashMap<>();

    protected Map<Integer, Integer> cmdPropagSucc = new HashMap<>();
    protected Map<Integer, List<Integer>> cmdPropagPred = new HashMap<>();

    public CFGWithDD(){super();}
    public CFGWithDD(CFGWithDD copyFrom){
        super(copyFrom);

        for(Integer nodeId : copyFrom.globalDefUseSucc.keySet())
            this.globalDefUseSucc.put(nodeId, new ArrayList<>(copyFrom.globalDefUseSucc.get(nodeId)));
        for(Integer nodeId : copyFrom.globalDefUsePred.keySet())
            this.globalDefUsePred.put(nodeId, new ArrayList<>(copyFrom.globalDefUsePred.get(nodeId)));

        for(Integer nodeId : copyFrom.intraDefUseSucc.keySet())
            this.intraDefUseSucc.put(nodeId, new ArrayList<>(copyFrom.intraDefUseSucc.get(nodeId)));
        for(Integer nodeId : copyFrom.intraDefUsePred.keySet())
            this.intraDefUsePred.put(nodeId, new ArrayList<>(copyFrom.intraDefUsePred.get(nodeId)));

        this.cmdPropagSucc   = new HashMap<>(copyFrom.cmdPropagSucc);
        for(Integer nodeId : copyFrom.cmdPropagPred.keySet())
            this.cmdPropagPred.put(nodeId, new ArrayList<>(copyFrom.cmdPropagPred.get(nodeId)));
    }

    public List<Integer> getDefUse(Integer nodeId) {
        if(globalDefUseSucc.containsKey(nodeId))
            return globalDefUseSucc.get(nodeId);
        return intraDefUseSucc.get(nodeId);
    }
    public List<Integer> getDefUseRev(Integer nodeId) {
        if(globalDefUsePred.containsKey(nodeId))
            return globalDefUsePred.get(nodeId);
        return intraDefUsePred.get(nodeId);
    }

    public List<Integer> getGlobalDefUse(Integer nodeId) {
        return globalDefUseSucc.get(nodeId);
    }
    public List<Integer> getGlobalDefUseRev(Integer nodeId) {
        return globalDefUsePred.get(nodeId);
    }

    public List<Integer> getIntraDefUse(Integer nodeId) {
        return intraDefUseSucc.get(nodeId);
    }
    public List<Integer> getIntraDefUseRev(Integer nodeId) {
        return intraDefUsePred.get(nodeId);
    }

    public Integer getCmdPropag(Integer nodeId) {
        return cmdPropagSucc.get(nodeId);
    }
    public List<Integer> getCmdPropagRev(Integer nodeId) {
        return cmdPropagPred.get(nodeId);
    }

    public void addGlobalDefUse(Integer nodeIdSource, Integer nodeIdDest) {
        if(!globalDefUseSucc.containsKey(nodeIdSource))
            globalDefUseSucc.put(nodeIdSource, new ArrayList<>());
        globalDefUseSucc.get(nodeIdSource).add(nodeIdDest);

        if(!globalDefUsePred.containsKey(nodeIdDest))
            globalDefUsePred.put(nodeIdDest, new ArrayList<>());
        globalDefUsePred.get(nodeIdDest).add(nodeIdSource);
    }
    public void addIntraDefUse(Integer nodeIdSource, Integer nodeIdDest) {
        if(!intraDefUseSucc.containsKey(nodeIdSource))
            intraDefUseSucc.put(nodeIdSource, new ArrayList<>());
        intraDefUseSucc.get(nodeIdSource).add(nodeIdDest);

        if(!intraDefUsePred.containsKey(nodeIdDest))
            intraDefUsePred.put(nodeIdDest, new ArrayList<>());
        intraDefUsePred.get(nodeIdDest).add(nodeIdSource);
    }
    public void setCmdPropag(Integer nodeIdSource, Integer nodeIdDest) {
        cmdPropagSucc.put(nodeIdSource, nodeIdDest);
        if(!cmdPropagPred.containsKey(nodeIdDest))
            cmdPropagPred.put(nodeIdDest, new ArrayList<>());
        cmdPropagPred.get(nodeIdDest).add(nodeIdSource);
    }

    @Override
    public void deleteNode(Integer nodeId) {
        if(globalDefUseSucc.get(nodeId) != null)
            for(Integer node : globalDefUsePred.get(nodeId))
                globalDefUsePred.get(node).remove(nodeId);
        globalDefUseSucc.remove(nodeId);

        if(globalDefUsePred.get(nodeId) != null)
            for(Integer node : globalDefUseSucc.get(nodeId))
                globalDefUseSucc.get(node).remove(nodeId);
        globalDefUsePred.remove(nodeId);

        if(intraDefUseSucc.get(nodeId) != null)
            for(Integer node : intraDefUsePred.get(nodeId))
                intraDefUsePred.get(node).remove(nodeId);
        intraDefUseSucc.remove(nodeId);

        if(intraDefUsePred.get(nodeId) != null)
            for(Integer node : intraDefUseSucc.get(nodeId))
                intraDefUseSucc.get(node).remove(nodeId);
        intraDefUsePred.remove(nodeId);

        if(cmdPropagSucc.get(nodeId) != null)
            for(Integer node : cmdPropagPred.get(nodeId))
                cmdPropagPred.get(node).remove(nodeId);
        cmdPropagSucc.remove(nodeId);
        cmdPropagPred.remove(nodeId);
        super.deleteNode(nodeId);
    }

}
