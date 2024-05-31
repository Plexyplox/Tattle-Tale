package edu.policy.manager;

import edu.policy.model.constraint.Cell;
import edu.policy.model.cue.CueSet;

import java.util.HashMap;
import java.util.Map;

public class ExplicitParentage {
    private Cell cellIdentity;
    private CueSet cuesetIdentity;
    private Cell parentCell;
    private CueSet parentCueset;
    private Integer cellLevel;
    private Integer cuesetLevel;

    public ExplicitParentage(Cell cellIdentity, CueSet cuesetIdentity, Cell parentCell, CueSet parentCuset, Integer cellLevel, Integer cusetLevel){
        this.cellIdentity = cellIdentity;
        this.cuesetIdentity = cuesetIdentity;
        this.parentCell = parentCell;
        this.parentCueset = parentCuset;
        this.cellLevel = cellLevel;
        this.cuesetLevel = cusetLevel;
    }
    public Cell getCellIdentity(){
        return this.cellIdentity;
    }
    public CueSet getCuesetIdentity(){
        return this.cuesetIdentity;
    }
    public Cell getParentCell(){
        return this.parentCell;
    }
    public CueSet getParentCueset(){
        return this.parentCueset;
    }
    public Integer getCellLevel(){
        return this.cellLevel;
    }
    public Integer getCuesetLevel(){
        return this.cuesetLevel;
    }
    public void setParentage(Cell parentCell, CueSet parentCuset, Integer cellLevel, Integer cusetLevel){
        this.parentCell = parentCell;
        this.parentCueset = parentCuset;
        this.cellLevel = cellLevel;
        this.cuesetLevel = cusetLevel;
    }
}