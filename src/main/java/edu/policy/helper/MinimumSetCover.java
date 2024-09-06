package edu.policy.helper;

import edu.policy.model.constraint.Cell;
import edu.policy.model.cue.CueSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


public class MinimumSetCover {

    private static final Logger logger = LogManager.getLogger(MinimumSetCover.class);

    /**
     * Implementation of the greedy heuristic for minimum vertex cover problem.
     * Take in a list of cuesets,
     * find a minimum list of cells to hide that cover all the cuesets.
     *
     * @param cueSetList
     * @return
     */
    public static List<Cell> greedyHeuristic(List<CueSet> cueSetList) {

        List<Cell> retCellList = new ArrayList<>();

        List<CueSet> cueSetListCopy = new ArrayList<>(cueSetList);

        while (!cueSetListCopy.isEmpty()) {
            List<Cell> flattenCuesetList = cueSetListCopy.stream().flatMap(cueSet -> cueSet.getCells().stream()).collect(Collectors.toList());

            // get the frequency of the cells in the cueset list
            Map<Cell, Long> cellOccurrence = flattenCuesetList.stream().collect(Collectors.groupingBy(cell-> cell, Collectors.counting()));

            // add the max occurrence to the retCellList
            // from https://stackoverflow.com/questions/43616422/find-the-most-common-attribute-value-from-a-list-of-objects-using-stream
            Cell cellMaxOcc = Collections.max(cellOccurrence.entrySet(), Map.Entry.comparingByValue()).getKey();

            logger.debug(String.format("Max Frequency of the cell: %s, %s", cellOccurrence.get(cellMaxOcc), cellMaxOcc.toString()));

            retCellList.add(cellMaxOcc);

            // delete the cuesets with this cell
            cueSetListCopy.removeIf(cueSet -> cueSet.getCells().contains(cellMaxOcc));
        }

        return retCellList;


    }
    public static List<Cell> greedyEdgeVC(List<CueSet> cueSetsList){
        List<Cell> retVal = new ArrayList<>();
        List<CueSet> copyList = new ArrayList<>(cueSetsList);
        List<Cell> flattenCuesetList = copyList.stream().flatMap(cueSet -> cueSet.getCells().stream()).collect(Collectors.toList());
        Map<Cell, Long> cellOccurrence = flattenCuesetList.stream().collect(Collectors.groupingBy(cell-> cell, Collectors.counting()));
        List<CueSet> iter = new ArrayList<>(copyList);
        List<CueSet> addCopyList = new ArrayList<>(copyList);
        for (CueSet cue: copyList){
            if (!hasIntersection(cue.getCells(),retVal)){
                Cell champ = null;
                for (Cell c: cue.getCells()){
                    if (champ == null) champ = c;
                    else{
                        if (cellOccurrence.get(c) > cellOccurrence.get(champ)) champ = c;
                    }
                }
                retVal.add(champ);
            }
        }
        iter.removeIf(cueSet -> !hasIntersection(cueSet.getCells(),retVal));
        Map<Cell, Integer> lossCnt = new HashMap<>();
        for (Cell c: retVal){
            lossCnt.put(c,0);
        }
        //for
        return retVal;
    }
    public static List<Cell> expCover(List<CueSet> cueSetsList){
        List<Cell> retVal = new ArrayList<>();
        Instant initial,end,loopEnt,loop;
        long construct,probe;
        initial = Instant.now();
        List<Cell> primer = MinimumSetCover.greedyEdgeVC(cueSetsList);
        end = Instant.now();
        construct = 4 * Duration.between(initial,end).toMillis();
        loopEnt = Instant.now();
        loop = Instant.now();
        probe = Duration.between(loopEnt,loop).toMillis();
        while (probe<construct){

            loop = Instant.now();
            probe = Duration.between(loopEnt,loop).toMillis();
        }
        return retVal;
    }
    private static boolean hasIntersection(List<Cell> list1, List<Cell> list2) {
        return !Collections.disjoint(list1, list2);
    }

    private static List<Cell> intersection(List<Cell> list1, List<Cell> list2) {
        Set<Cell> list = new HashSet<>();

        for (Cell t1 : list1) {
            for (Cell t2: list2) {
                if (t1.equals(t2)) { // since we have customized equals and hashcode implementation
                    list.add(t1);
                }
            }
        }

        return new ArrayList<>(list);
    }
}
