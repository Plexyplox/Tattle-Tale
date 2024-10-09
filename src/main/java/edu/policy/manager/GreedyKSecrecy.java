package edu.policy.manager;

import edu.policy.helper.LeakageCalculator;
import edu.policy.helper.MinimumSetCover;
import edu.policy.helper.Utils;
import edu.policy.model.AttributeType;
import edu.policy.model.constraint.*;
import edu.policy.model.cue.CueSet;
import edu.policy.model.data.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Assert;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class GreedyKSecrecy extends GreedyAlgorithm {

    float k_percentage; // percentage of the domain

    private static final Logger logger = LogManager.getLogger(GreedyKSecrecy.class);
    

    public GreedyKSecrecy(Session session) {
        super(session);
    }

    public List<Cell> greedyHolisticKDen () {

        logger.info("Start executing the greedy k-deniability algorithm.");

        List<Cell> senCells = session.getPolicies();

        logger.info(String.format("Sensitive cells marked by policies: %s", senCells));

        cuesetDetectorInvokeCounter = 0;

        totalCuesetSize = 0;

        this.k_percentage = session.getK_value();

        useMVC = session.getUseMVC();

        hideCells = new ArrayList<>();
        cuesets = new ArrayList<>();

        for (Cell senCell: senCells) {
            if (!hideCells.contains(senCell))
                hideCells.add(senCell);
        }

        return new ArrayList<>(greedyKDenBreadthFirst(senCells));
    }

    /**
     * Breadth-first search version of k-deniability algorithm.
     *
     * Simulate the breadth-first search version of full-deniability algorithm,
     * and use K_prune algorithm to open up some hidden cells.
     *
     * @param senCells a list of sensitive cells
     * @return truehide list of hidden cells
     */
    public List<Cell> greedyKDenBreadthFirst(List<Cell> senCells) {
        byte[] array = new byte[7]; // length is bounded by 7
        new Random().nextBytes(array);
        String generatedString = new String(array, Charset.forName("UTF-8"));
        String check = new String(String.valueOf((Objects.hash(generatedString))));
        if (!hideCells.containsAll(senCells))
            hideCells = Utils.unionCells(hideCells, senCells);

        List<DataDependency> schemaDependencies = session.getDcs();
        List<Provenance> schemaPBDs = session.getPbds();

        // Cueset detection
        List<CueSet> onDetect = cueDetector.detect(schemaDependencies, senCells);
        Set<CueSet> pbdOnDetect = new HashSet<>();
        if (!schemaPBDs.isEmpty())
            pbdOnDetect = pbdCueDetector.detect(schemaPBDs, senCells);
        cuesetDetectorInvokeCounter += 1;
        logger.info(String.format("The %d-th time invoking cueset detector.", cuesetDetectorInvokeCounter));

        if (!onDetect.isEmpty()) {
            cuesets.addAll(onDetect);

            if (!pbdOnDetect.isEmpty()) {
                cuesets.addAll(pbdOnDetect);
                totalCuesetSize += pbdOnDetect.size();
            }

            totalCuesetSize += onDetect.size();

            cueSetsFanOut.add(totalCuesetSize);
            logger.info(String.format("%d cuesets being detected.", onDetect.size() + pbdOnDetect.size()));
        }

        int level = 1;
        // main while loop
        List<Cell> foundCells = new ArrayList<>(senCells);
        while (!cuesets.isEmpty()) {

            if (testFanOut)
                if (cuesetDetectorInvokeCounter >= 6)
                    break;

            List<Cell> toHide = new ArrayList<>();
            if (level == 1){
                if (useMVC) {
                    List<CueSet> cuesets2HSP = new ArrayList<>();
                    long startTime_MVC = new Date().getTime();
                    // test MVC
                    cuesets.removeIf(cueSet -> hasIntersection(cueSet.getCells(), hideCells));
                    for (Cell s: senCells){
                        List<CueSet> potentialLeaks = cuesets.stream().filter(cueSet -> cueSet.getSenCell().equals(s)).collect(Collectors.toList());
                        if (!potentialLeaks.isEmpty()){
                            List<CueSet> knownLeaks = KPrune(s,potentialLeaks);
                            if (knownLeaks != null){
                                cuesets2HSP.addAll(knownLeaks);
                            }
                        }
                    }
                    toHide.addAll(MinimumSetCover.greedyHeuristic(cuesets2HSP));
                    long endTime_MVC = new Date().getTime();
                    long timeElapsed = endTime_MVC - startTime_MVC;
                    logger.info(String.format("Finished executing MVC; use time: %d ms.", timeElapsed));

                }
                else {
                    for (CueSet cueSet: cuesets) {

                        cueSet.setChosenToBeHidden(Boolean.TRUE);

                        if (!hasIntersection(cueSet.getCells(), toHide)
                                && !hasIntersection(cueSet.getCells(), hideCells) ) { // no cell in cueset is hidden

                            Cell cell;

                            if (randomHiddenCellChoosing)
                                cell = cueSet.getCells().get(rand.nextInt(cueSet.getCells().size()));
                            else
                                cell = cueSet.getCells().get(0); // get the first cell in the cueset

                            toHide.add(cell); // for detect the cuesets of this cell

                        }
                    }
                }
            }
            else {
                if (useMVC) {

                    long startTime_MVC = new Date().getTime();
                    // test MVC
                    cuesets.removeIf(cueSet -> hasIntersection(cueSet.getCells(), hideCells));
                    //List<CueSet> test = new ArrayList<>(cuesets);
                    List<Cell> iterList = new ArrayList<>(hideCells);
                    iterList.removeAll(foundCells);
                    List<CueSet> cuesets2HSP = new ArrayList<>();

                    for (Cell ite: iterList){
                        List<CueSet> potentialLeaks = cuesets.stream().filter(cueSet -> cueSet.getSenCell().equals(ite)).collect(Collectors.toList());
                        //test.removeAll(potentialLeaks);
                        if (!potentialLeaks.isEmpty()){
                            List<CueSet> knownLeaks = KPrune(ite,potentialLeaks);
                            if (knownLeaks != null){
                                cuesets2HSP.addAll(knownLeaks);
                            }
                        }
                    }
                    toHide.addAll(MinimumSetCover.greedyHeuristic(cuesets2HSP));
                    foundCells.addAll(hideCells);
                    Set<Cell> copy = new HashSet<>(foundCells);
                    foundCells.clear();
                    foundCells.addAll(copy);
                    //toHide.addAll(MinimumSetCover.greedyHeuristic(cuesets));
                    long endTime_MVC = new Date().getTime();
                    long timeElapsed = endTime_MVC - startTime_MVC;
                    logger.info(String.format("Finished executing MVC; use time: %d ms.", timeElapsed));

                }
                else {
                    for (CueSet cueSet: cuesets) {

                        cueSet.setChosenToBeHidden(Boolean.TRUE);

                        if (!hasIntersection(cueSet.getCells(), toHide)
                                && !hasIntersection(cueSet.getCells(), hideCells) ) { // no cell in cueset is hidden

                            Cell cell;

                            if (randomHiddenCellChoosing)
                                cell = cueSet.getCells().get(rand.nextInt(cueSet.getCells().size()));
                            else
                                cell = cueSet.getCells().get(0); // get the first cell in the cueset

                            toHide.add(cell); // for detect the cuesets of this cell

                        }
                    }
                }
            }
            
            if (!toHide.isEmpty()) {

                hideCells.addAll(toHide); // hide this cell
                hiddenCellsFanOut.add(hideCells.size());
                logger.info(String.format("%d cells are hidden at the %d-th level.", hideCells.size(), cuesetDetectorInvokeCounter));

                // Get the cuesets of tohide cell lists
                onDetect = cueDetector.detect(schemaDependencies, toHide);
                cuesetDetectorInvokeCounter += 1;
                if (!schemaPBDs.isEmpty())
                    pbdOnDetect = pbdCueDetector.detect(schemaPBDs, toHide);
                logger.info(String.format("The %d-th time invoking cueset detector.", cuesetDetectorInvokeCounter));

                // remove the current cueset from the cueset list
                cuesets.clear();

                if (!onDetect.isEmpty()){
                    cuesets.addAll(onDetect);
                    if (!pbdOnDetect.isEmpty()) {
                        cuesets.addAll(pbdOnDetect);
                        totalCuesetSize += pbdOnDetect.size();
                    }

                    totalCuesetSize += onDetect.size();

                    cueSetsFanOut.add(totalCuesetSize);
                    logger.info(String.format("%d cuesets being detected.", onDetect.size() + pbdOnDetect.size()));
                }
            }
            else if (toHide.isEmpty()){
                cuesets.clear();
            }
            level++;
        }
        return hideCells;
    }
    private List<CueSet> minusCells(Cell senCell, List<CueSet> cueSetsOfSenCell) {
        return null;
    }
    private List<CueSet> KPrune(Cell senCell, List<CueSet> cueSetsOfSenCell) {

        LeakageCalculator.joint_state(senCell, cueSetsOfSenCell, session);

        if (isDeniable(senCell, k_percentage))
            return null;

        // Optimization 1: cannot open cuesets which can lead to full leakage
        List<CueSet> bestCueSets = cueSetsOfSenCell.stream().filter(cueSet -> cueSet.getLeakageToParent() == 1)
                                                            .collect(Collectors.toList());
        //HashSet<CueSet> hashCue = new HashSet<>(cueSetsOfSenCell);
        //HashSet<CueSet> hashBest = new HashSet<>(bestCueSets);
        //hashCue.removeAll(hashBest);
        cueSetsOfSenCell.removeAll(bestCueSets);
        if (cueSetsOfSenCell.isEmpty()){
            return bestCueSets;
        }
        //cueSetsOfSenCell.clear();
        //cueSetsOfSenCell = new ArrayList<>(hashCue);
        if (senCell.getCellType().equals(AttributeType.INTEGER) || senCell.getCellType().equals(AttributeType.DOUBLE)) {
            // Optimization for continuous domain attributes:
            // sort the cueset list in **descending order** w.r.t the leakage to the parent
            cueSetsOfSenCell.sort(Comparator.comparing(CueSet::getLeakageToParent).reversed());

            while (!isDeniable(senCell, k_percentage)) {

                logger.debug(String.format("Starting iteration, current cueset list size: %d", cueSetsOfSenCell.size()));

                // greedy: find the cueset that leads to the largest possible leakage
                CueSet lcs = cueSetsOfSenCell.get(0);

                assert lcs!= null;
                bestCueSets.add(lcs);

                cueSetsOfSenCell.remove(lcs);

                logger.debug(String.format("After remove lcs from cueset list, current cueset list size: %d", cueSetsOfSenCell.size()));

                LeakageCalculator.joint_state(senCell, cueSetsOfSenCell, session);

            }
        }
        else if (senCell.getCellType().equals(AttributeType.STRING)) {
            HashSet<CueSet> nullSet = new HashSet<>();
            for (CueSet c : cueSetsOfSenCell){
                String check = c.getMinusString();
                if (check == null){
                    nullSet.add(c);
                    //cueSetsOfSenCell.remove(c);
                }
            }
            if(!nullSet.isEmpty()){
                List<CueSet> nullList = new ArrayList<>(nullSet);
                cueSetsOfSenCell.removeAll(nullList);
                nullList.sort(Comparator.comparing(CueSet::getLeakageToParent).reversed());
                while (!isDeniable(senCell,k_percentage)){
                    if (nullList.isEmpty()){
                        System.err.println("Abandon all hope ye who enter here");
                    }
                    CueSet low = nullList.get(0);
                    assert low!= null;
                    bestCueSets.add(low);
                    nullList.remove(low);
                    LeakageCalculator.joint_state(senCell, nullList, session);
                }
                if (cueSetsOfSenCell.isEmpty()){
                    return bestCueSets;
                }
            }
            // Optimization for discrete domain attributes:

            Map<String, Long> minusStringOcc = cueSetsOfSenCell.stream().collect(
                    Collectors.groupingBy(CueSet::getMinusString, Collectors.counting()));

            while (!isDeniable(senCell, minusStringOcc.size(), k_percentage)) {

                logger.debug(String.format("Starting iteration, current cueset list size: %d", cueSetsOfSenCell.size()));
                String minusStringMinOcc = Collections.min(minusStringOcc.entrySet(), Map.Entry.comparingByValue()).getKey();
                List<CueSet> toAddBestCuesets = cueSetsOfSenCell.stream().filter(cueSet -> cueSet.getMinusString().equals(minusStringMinOcc)).collect(Collectors.toList());

                bestCueSets.addAll(toAddBestCuesets);
                //hashBest.addAll(toAddBestCuesets);
                cueSetsOfSenCell.removeAll(toAddBestCuesets);
                //hashCue.removeAll(toAddBestCuesets);
                //cueSetsOfSenCell.clear();
                //cueSetsOfSenCell = new ArrayList<>(hashCue);
                minusStringOcc.remove(minusStringMinOcc);
            }
            //bestCueSets.clear();
            //bestCueSets = new ArrayList<>(hashBest);
        }

        LeakageCalculator.joint_state(senCell, cueSetsOfSenCell, session);
        return bestCueSets;
    }
//NEW ADDITION FOR PARTIAL DENIABILITY sorry about the all caps
    private List<CueSet> KPruneMod(Cell senCell, List<CueSet> cueSetsOfSenCell, String kP) {

        float k = Float.parseFloat(kP);
        LeakageCalculator.joint_state(senCell, cueSetsOfSenCell, session);

        if (isDeniable(senCell, k))
            return null;

        // Optimization 1: cannot open cuesets which can lead to full leakage
        List<CueSet> bestCueSets = cueSetsOfSenCell.stream().filter(cueSet -> cueSet.getLeakageToParent() == 1)
                .collect(Collectors.toList());
        //HashSet<CueSet> hashCue = new HashSet<>(cueSetsOfSenCell);
        //HashSet<CueSet> hashBest = new HashSet<>(bestCueSets);
        //hashCue.removeAll(hashBest);
        cueSetsOfSenCell.removeAll(bestCueSets);
        if (cueSetsOfSenCell.isEmpty()){
            return bestCueSets;
        }
        //cueSetsOfSenCell.clear();
        //cueSetsOfSenCell = new ArrayList<>(hashCue);
        if (senCell.getCellType().equals(AttributeType.INTEGER) || senCell.getCellType().equals(AttributeType.DOUBLE)) {
            // Optimization for continuous domain attributes:
            // sort the cueset list in **descending order** w.r.t the leakage to the parent
            cueSetsOfSenCell.sort(Comparator.comparing(CueSet::getLeakageToParent).reversed());

            while (!isDeniable(senCell, k)) {

                logger.debug(String.format("Starting iteration, current cueset list size: %d", cueSetsOfSenCell.size()));

                // greedy: find the cueset that leads to the largest possible leakage
                CueSet lcs = cueSetsOfSenCell.get(0);

                assert lcs!= null;
                bestCueSets.add(lcs);

                cueSetsOfSenCell.remove(lcs);

                logger.debug(String.format("After remove lcs from cueset list, current cueset list size: %d", cueSetsOfSenCell.size()));

                LeakageCalculator.joint_state(senCell, cueSetsOfSenCell, session);

            }
        }
        else if (senCell.getCellType().equals(AttributeType.STRING)) {
            HashSet<CueSet> nullSet = new HashSet<>();
            for (CueSet c : cueSetsOfSenCell){
                String check = c.getMinusString();
                if (check == null){
                    nullSet.add(c);
                }
            }
            if(!nullSet.isEmpty()){
                List<CueSet> nullList = new ArrayList<>(nullSet);
                cueSetsOfSenCell.removeAll(nullList);
                nullList.sort(Comparator.comparing(CueSet::getLeakageToParent).reversed());
                while (!isDeniable(senCell,k_percentage)){
                    if (nullList.isEmpty()){
                        System.err.println("Abandon all hope ye who enter here");
                    }
                    CueSet low = nullList.get(0);
                    assert low!= null;
                    bestCueSets.add(low);
                    nullList.remove(low);
                    LeakageCalculator.joint_state(senCell, nullList, session);
                }
                if (cueSetsOfSenCell.isEmpty()){
                    return bestCueSets;
                }
            }
            // Optimization for discrete domain attributes:
            Map<String, Long> minusStringOcc = cueSetsOfSenCell.stream().collect(
                    Collectors.groupingBy(CueSet -> CueSet.getMinusString() == null ? "nullobjectrefenceremove" : CueSet.getMinusString(), Collectors.counting()));
            while (!isDeniable(senCell, minusStringOcc.size(), k)) {

                logger.debug(String.format("Starting iteration, current cueset list size: %d", cueSetsOfSenCell.size()));

                String minusStringMinOcc = Collections.min(minusStringOcc.entrySet(), Map.Entry.comparingByValue()).getKey();
                List<CueSet> toAddBestCuesets = cueSetsOfSenCell.stream().filter(cueSet -> cueSet.getMinusString().equals(minusStringMinOcc)).collect(Collectors.toList());

                bestCueSets.addAll(toAddBestCuesets);
                cueSetsOfSenCell.removeAll(toAddBestCuesets);
                    //hashBest.addAll(toAddBestCuesets);
                    //hashCue.removeAll(toAddBestCuesets);
                    //cueSetsOfSenCell.clear();
                    //cueSetsOfSenCell = new ArrayList<>(hashCue);
                minusStringOcc.remove(minusStringMinOcc);
            }
                //bestCueSets.clear();
                //bestCueSets = new ArrayList<>(hashBest);


        }

        LeakageCalculator.joint_state(senCell, cueSetsOfSenCell, session);
        return bestCueSets;
    }
    /**
     * Check if exists intersection between list1 and list2.
     * optimization: https://stackoverflow.com/questions/58320338/which-is-the-fastest-way-for-a-containsany-check
     *
     * @param list1 a list of cells
     * @param list2 a list of cells
     * @return return True if list1 and list2 have intersection; return False otherwise.
     */
    boolean hasIntersection(List<Cell> list1, List<Cell> list2) {
        return !Collections.disjoint(list1, list2);
    }

    List<Cell> intersection(List<Cell> list1, List<Cell> list2) {
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

    // https://stackoverflow.com/questions/5283047/intersection-and-union-of-arraylists-in-java
    List<Cell> union(List<Cell> list1, List<Cell> list2) {
        Set<Cell> set = new HashSet();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<>(set);
    }

    List<Cell> cueSetsToListOfCells(List<CueSet> cueSets) {
        List<Cell> retCells = new ArrayList<>();

        for (CueSet cueSet: cueSets) {
            retCells.addAll(cueSet.getCells());
        }
        return retCells;
    }

    /**
     * Check if the k-deniability requirement satisfies.
     *
     * If k = 1, the algorithm degrades to full-deniability;
     * if k = 0, the privacy leakage is unconstrained.
     *
     * @param senCell the sensitive/target cell
     * @param k_percentage the k percentile experimental parameter
     * @return True if the deniability requirement satisfies; False otherwise.
     */
    boolean isDeniable(Cell senCell, float k_percentage) {

        State state = senCell.getCurState();

        if (state.isNoLeakage())
            return Boolean.TRUE;

        double currStateSize;
        double domSize = senCell.getAttrDomSize();

        switch (senCell.getCellType()) {
            case STRING:
                currStateSize = domSize - state.getMinusSetString().size();
                break;
            case DOUBLE:
            case INTEGER:
                currStateSize = state.getHigh() - state.getLow();  // double and integer current state size
                break;
            case TIMESTAMP:
            case DATE:
            case TIME:
                throw new IllegalStateException("GreedyKSecrecy: Unsupported attribute type " + senCell.getCellType());
            default:
                throw new IllegalStateException("GreedyKSecrecy: Unexpected attribute type " + senCell.getCellType());
        }


        if (currStateSize >= domSize *  k_percentage )
            return Boolean.TRUE;

        return Boolean.FALSE;
    }

    boolean isDeniable(Cell senCell, int minusSetSize, float k_percentage) {

        double domSize;
        if (senCell.getCellType().equals(AttributeType.STRING)) {
            domSize = senCell.getAttrDomSize();
        }
        else {
            throw new IllegalStateException("This optimization is only for discrete domain attributes.");
        }

        if (minusSetSize <= domSize * (1 - k_percentage))
            return Boolean.TRUE;

        return Boolean.FALSE;

    }

    public int getTotalCuesetSize() {
        return totalCuesetSize;
    }

    public boolean isRandomCuesetChoosing() {
        return randomCuesetChoosing;
    }

    public void setRandomCuesetChoosing(boolean randomCuesetChoosing) {
        this.randomCuesetChoosing = randomCuesetChoosing;
    }

    public Boolean getRandomHiddenCellChoosing() {
        return randomHiddenCellChoosing;
    }

    public void setRandomHiddenCellChoosing(Boolean randomHiddenCellChoosing) {
        this.randomHiddenCellChoosing = randomHiddenCellChoosing;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
        this.rand = new Random(randomSeed);
    }

}
