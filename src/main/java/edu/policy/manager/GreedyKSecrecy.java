package edu.policy.manager;

import edu.policy.helper.LeakageCalculator;
import edu.policy.helper.MinimumSetCover;
import edu.policy.helper.Utils;
import edu.policy.model.AttributeType;
import edu.policy.model.constraint.*;
import edu.policy.model.cue.CueSet;
import edu.policy.model.data.Session;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        //HashMap<Cell,Integer> cellHolder = new HashMap<>();
        //HashMap<List<Cell>, Integer> cueHolder = new HashMap<>();
        HashSet<HashMap<Cell,HashMap<CueSet,Integer>>> notDomain = new HashSet<>();
        HashSet<HashMap<CueSet,HashMap<CueSet,Integer>>> pedigree = new HashSet<>();
        int level = 1;
        List<Cell> trueHide = new ArrayList<>(senCells); // true hide list of all time
        List<Cell> trackTrueHide = new ArrayList<>(); // true hide for each level

        List<CueSet> onDetectTrueHide = null;
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


        // main while loop
        while (!cuesets.isEmpty()) {

            if (testFanOut)
                if (cuesetDetectorInvokeCounter >= 6)
                    break;

            List<Cell> toHide = new ArrayList<>();  // local hide cells of each level from full-den

            if (useMVC) {

                long startTime_MVC = new Date().getTime();
                // test MVC
                cuesets.removeIf(cueSet -> hasIntersection(cueSet.getCells(), hideCells));

                toHide.addAll(MinimumSetCover.greedyHeuristic(cuesets));

                if (onDetectTrueHide != null) {
                    onDetectTrueHide.removeIf(cueSet -> hasIntersection(cueSet.getCells(), trueHide));
                    //trackTrueHide.addAll(MinimumSetCover.greedyHeuristic(onDetectTrueHide));
                    //trueHide.addAll(trackTrueHide);
                }

                long endTime_MVC = new Date().getTime();
                long timeElapsed = endTime_MVC - startTime_MVC;
                logger.info(String.format("Finished executing MVC; use time: %d ms.", timeElapsed));

            }
            else {
                for (CueSet cueSet: cuesets) {

                    if (!hasIntersection(cueSet.getCells(), toHide)
                            && !hasIntersection(cueSet.getCells(), hideCells) ) { // no cell in cueset is hidden

                        Cell cell;

                        logger.debug(String.format("Number of cells in cueSet: %d", cueSet.getCells().size()));

                        if (randomHiddenCellChoosing)
                            cell = cueSet.getCells().get(rand.nextInt(cueSet.getCells().size()));
                        else
                            cell = cueSet.getCells().get(0); // get the first cell in the cueset

                        toHide.add(cell); // for detect the cuesets of this cell

                    }
                }

                // for true hide list
                if (onDetectTrueHide != null) {
                    for (CueSet cueSet: onDetectTrueHide) {

                        if (!hasIntersection(cueSet.getCells(), trackTrueHide)
                                && !hasIntersection(cueSet.getCells(), trueHide) ) { // no cell in cueset is hidden

                            Cell cell;

                            logger.debug(String.format("Number of cells in cueSet: %d", cueSet.getCells().size()));

                            if (randomHiddenCellChoosing)
                                cell = cueSet.getCells().get(rand.nextInt(cueSet.getCells().size()));
                            else
                                cell = cueSet.getCells().get(0); // get the first cell in the cueset

                            trackTrueHide.add(cell); // for detect the cuesets of this cell

                        }
                    }
                    trueHide.addAll(trackTrueHide);

                }

            }
            //List<CueSet> pruneHolder = new ArrayList<>();
            //List<Cell> thePruned = new ArrayList<>();

            if (level == 1) {
                List<CueSet> bestCueSets = new ArrayList<>();
                for (Cell cell: trueHide) {
                    List<CueSet> cueSetsToPrune = cuesets.stream().filter(cueSet -> cueSet.getSenCell().equals(cell)).collect(Collectors.toList());
                    List<CueSet> prunedCueSets = KPrune(cell, cueSetsToPrune);
                    if (prunedCueSets != null){
                        bestCueSets.addAll(prunedCueSets);
                        for (CueSet bC: prunedCueSets){
                            HashMap<CueSet,Integer> d = new HashMap<>();
                            d.put(bC,level);
                            HashMap<Cell,HashMap<CueSet,Integer>> afterD = new HashMap<>();
                            afterD.put(cell,d);
                            notDomain.add(afterD);
                        }
                    }

                    //pruneHolder.addAll(cueSetsToPrune);
                    //pruneHolder.removeAll(bestCueSets);
                }
                List<Cell> flattenBestCueSets = bestCueSets.stream().flatMap(cueSet -> cueSet.getCells().stream()).collect(Collectors.toList());
                trackTrueHide.addAll(intersection(toHide, flattenBestCueSets));
                trueHide.addAll(trackTrueHide);
            }
            if (level > 1){
                List<CueSet> bestCueSets = new ArrayList<>();
                List<CueSet> notRealListTestingOnly = new ArrayList<>();
                for (Cell cell: trueHide){
                    List<CueSet> cueSetsToPrune = cuesets.stream().filter(cueSet -> cueSet.getSenCell().equals(cell)).collect(Collectors.toList());
                    List<CueSet> prunedCueSets = KPrune(cell, cueSetsToPrune);
                    if (prunedCueSets != null){
                        for (CueSet pC: prunedCueSets){
                            notRealListTestingOnly.add(pC);
                        }
                    }
                }
                System.out.println("bombdiggity");
            }
/*
            if ((pruneHolder != null) && (level == 1)){

                pruneHolder.forEach(p -> thePruned.addAll(p.getCells()));
                List<Cell> cleanPrune = new ArrayList<>(new HashSet<>(thePruned));
                cleanPrune.forEach(c -> cellHolder.put(c,0));
                pruneHolder.forEach(p -> cueHolder.put(p.getCells(),0));
                try{
                    FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\spinMeRound.txt",true);
                    writer.write(System.lineSeparator());
                    writer.write("% K-Percentile Data %");
                    writer.write(System.lineSeparator());
                    writer.write("k-value = ");
                    writer.write(Double.toString(k_percentage));
                    writer.write(System.lineSeparator());
                    writer.write("Run: ");
                    writer.write(check);
                    writer.write(System.lineSeparator());
                    writer.write("Pruned Cuesize: ");
                    writer.write(Integer.toString(pruneHolder.size()));
                    writer.write(System.lineSeparator());
                    writer.write("Pruned Cell size: ");
                    writer.write(Integer.toString(thePruned.size()));
                    writer.write(System.lineSeparator());
                    writer.write("Unique Cell Number: ");
                    writer.write(Integer.toString(cellHolder.size()));
                    writer.write(System.lineSeparator());
                    writer.write("TrueHide: ");
                    writer.write(Integer.toString(trueHide.size()));
                    writer.write(System.lineSeparator());
                    writer.write("Full Set of Cues: ");
                    writer.write(System.lineSeparator());

                    writer.write(System.lineSeparator());
                    writer.write(System.lineSeparator());
                    writer.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
            */
            logger.info(String.format("%d-th level: %d cells in the true hide set.", cuesetDetectorInvokeCounter, trueHide.size()));
            hiddenCellsFanOut.add(trueHide.size());
            level += 1;
            /*
            try{
                FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\onTheLevel.txt",true);
                writer.write("K-Den Run");
                writer.write(System.lineSeparator());
                writer.write("Run: ");
                writer.write(check);
                writer.write(System.lineSeparator());
                writer.write("Level: ");
                writer.write(Integer.toString(level));
                writer.write(System.lineSeparator());
                writer.write(System.lineSeparator());
                writer.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
             */
            if (!toHide.isEmpty()) {

                hideCells.addAll(toHide); // hide this cell
                logger.info(String.format("%d cells are hidden at the %d-th level.", hideCells.size(), cuesetDetectorInvokeCounter));

                // Get the cuesets of tohide cell lists
                onDetect = cueDetector.detect(schemaDependencies, toHide);
                /*
                if (level > 1){

                    List<Cell> flatDetect = new ArrayList<>();
                    onDetect.forEach(o -> flatDetect.addAll(o.getCells()));
                    List<Cell> cleanDetect = new ArrayList<>(new HashSet<>(flatDetect));
                    int cnt = 0;
                    int rnt = 0;
                    try{
                        FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\kUSeeIt.txt",true);
                        writer.write(System.lineSeparator());
                        writer.write("% K-Percentile Data %");
                        writer.write(System.lineSeparator());
                        writer.write("k-value = ");
                        writer.write(Double.toString(k_percentage));
                        writer.write(System.lineSeparator());
                        for (int i = 0; i < cleanDetect.size(); i++){
                            for (int j = 0; j < cellHolder.size(); j++){
                                if (cellHolder.containsKey(cleanDetect.get(i))){
                                    cellHolder.replace(cleanDetect.get(i),cellHolder.get(cleanDetect.get(i)) + 1);
                                    cnt++;
                                }
                            }
                        }
                        writer.write("Run: ");
                        writer.write(check);
                        writer.write(System.lineSeparator());
                        writer.write("At level ");
                        writer.write(Integer.toString(level));
                        writer.write(" cell was redetected: ");
                        writer.write(Integer.toString(cnt));
                        writer.write(" times");
                        writer.write(System.lineSeparator());
                        writer.write(System.lineSeparator());
                        writer.close();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }

                    try{
                        FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\cueUSeeIt.txt",true);
                        writer.write(System.lineSeparator());
                        writer.write("% K-Percentile Data %");
                        writer.write(System.lineSeparator());
                        writer.write("k-value = ");
                        writer.write(Double.toString(k_percentage));
                        writer.write(System.lineSeparator());
                        for (int i = 0; i < onDetect.size(); i++){
                            for (Map.Entry<List<Cell>, Integer> entry : cueHolder.entrySet()) {
                                if (Utils.listEqualsIgnoreOrder(onDetect.get(i).getCells(),entry.getKey())){
                                    cueHolder.replace(entry.getKey(),cueHolder.get(entry.getKey()) + 1);
                                    rnt++;
                                }
                            }
                        }
                        writer.write("Run: ");
                        writer.write(check);
                        writer.write(System.lineSeparator());
                        writer.write("At level ");
                        writer.write(Integer.toString(level));
                        writer.write(" cue was redetected: ");
                        writer.write(Integer.toString(rnt));
                        writer.write(" times");
                        writer.write(System.lineSeparator());
                        writer.write(System.lineSeparator());
                        writer.close();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }

                }

                try {
                    FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\cueReCount.txt",true);
                    writer.write("Cue Redetect Data");
                    writer.write(System.lineSeparator());
                    writer.write("Run: ");
                    writer.write(check);
                    writer.write(System.lineSeparator());
                    writer.write("Level: ");
                    writer.write(Integer.toString(level));
                    writer.write(System.lineSeparator());
                    for (Map.Entry<List<Cell>, Integer> entry : cueHolder.entrySet()) {
                        writer.write("Key: " + entry.getKey() + ", Redetect: " + entry.getValue());
                        writer.write(System.lineSeparator());
                    }
                    writer.write(System.lineSeparator());
                    writer.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                try {
                    FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\cellReCount.txt",true);
                    writer.write("Cell Redetect Data");
                    writer.write(System.lineSeparator());
                    writer.write("Run: ");
                    writer.write(check);
                    writer.write(System.lineSeparator());
                    writer.write("Level: ");
                    writer.write(Integer.toString(level));
                    writer.write(System.lineSeparator());
                    for (Map.Entry<Cell, Integer> entry : cellHolder.entrySet()) {
                        writer.write("Key: " + entry.getKey() + ", Redetect: " + entry.getValue());
                        writer.write(System.lineSeparator());
                    }
                    writer.write(System.lineSeparator());
                    writer.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
 */
                /*
                for (Map.Entry<Cell, Integer> entry : cellHolder.entrySet()) {
                    cellHolder.replace(entry.getKey(),0);
                }

                 */
                if (Utils.listEqualsIgnoreOrder(toHide, trackTrueHide))
                    onDetectTrueHide = onDetect;
                else
                    onDetectTrueHide = cueDetector.detect(schemaDependencies, trackTrueHide);
                if (!schemaPBDs.isEmpty()) {
                    pbdOnDetect = pbdCueDetector.detect(schemaPBDs, toHide);
                    if (Utils.listEqualsIgnoreOrder(toHide, trackTrueHide))
                        onDetectTrueHide = onDetect;
                    else
                        onDetectTrueHide.addAll(pbdCueDetector.detect(schemaPBDs, trackTrueHide));
                }
                cuesetDetectorInvokeCounter += 1;
                logger.info(String.format("The %d-th time invoking cueset detector.", cuesetDetectorInvokeCounter));

                // remove the current cueset from the cueset list
                cuesets.clear();
                trackTrueHide.clear();

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

        }

        return trueHide;

    }

    private List<CueSet> KPrune(Cell senCell, List<CueSet> cueSetsOfSenCell) {

        LeakageCalculator.joint_state(senCell, cueSetsOfSenCell, session);

        if (isDeniable(senCell, k_percentage))
            return null;

        // Optimization 1: cannot open cuesets which can lead to full leakage
        List<CueSet> bestCueSets = cueSetsOfSenCell.stream().filter(cueSet -> cueSet.getLeakageToParent() == 1)
                                                            .collect(Collectors.toList());

        cueSetsOfSenCell.removeAll(bestCueSets);

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

            // Optimization for discrete domain attributes:

            Map<String, Long> minusStringOcc = cueSetsOfSenCell.stream().collect(
                    Collectors.groupingBy(CueSet::getMinusString, Collectors.counting()));

            while (!isDeniable(senCell, minusStringOcc.size(), k_percentage)) {

                logger.debug(String.format("Starting iteration, current cueset list size: %d", cueSetsOfSenCell.size()));
                String minusStringMinOcc = Collections.min(minusStringOcc.entrySet(), Map.Entry.comparingByValue()).getKey();
                List<CueSet> toAddBestCuesets = cueSetsOfSenCell.stream().filter(cueSet -> cueSet.getMinusString().equals(minusStringMinOcc)).collect(Collectors.toList());

                bestCueSets.addAll(toAddBestCuesets);

                cueSetsOfSenCell.removeAll(toAddBestCuesets);

                minusStringOcc.remove(minusStringMinOcc);
            }
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
