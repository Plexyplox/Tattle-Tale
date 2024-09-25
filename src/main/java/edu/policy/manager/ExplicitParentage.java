package edu.policy.manager;

import edu.policy.model.constraint.Cell;
import edu.policy.model.cue.CueSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExplicitParentage {
    private Cell cellIdentity;
    private CueSet cuesetIdentity;
    private Cell seCell;
    private List<Cell> parentCell = new ArrayList<>();
    private CueSet parentCueset;
    private Integer cellLevel;
    private Integer cuesetLevel;

    public ExplicitParentage(Cell cellIdentity, CueSet cuesetIdentity,Cell seCell, CueSet parentCueset, Integer cellLevel, Integer cuesetLevel){
        this.cellIdentity = cellIdentity;
        this.cuesetIdentity = cuesetIdentity;
        this.seCell = seCell;
        this.parentCueset = parentCueset;
        this.cellLevel = cellLevel;
        this.cuesetLevel = cuesetLevel;
    }
    public Cell getCellIdentity(){
        return this.cellIdentity;
    }
    public CueSet getCuesetIdentity(){
        return this.cuesetIdentity;
    }
    public Cell getSenCell(){return this.seCell;}
    public List<Cell> getParentCell(){
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
    public void setParentage(Cell seCell, CueSet parentCueset, Integer cellLevel, Integer cuesetLevel){
        this.seCell = seCell;
        this.parentCueset = parentCueset;
        this.cellLevel = cellLevel;
        this.cuesetLevel = cuesetLevel;
    }
    public void addParentCell(Cell cell){
        this.parentCell.add(cell);
    }
}