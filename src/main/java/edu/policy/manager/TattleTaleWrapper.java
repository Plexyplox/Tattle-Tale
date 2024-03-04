package edu.policy.manager;

import edu.policy.model.constraint.Cell;
import edu.policy.model.data.Session;

import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;

public class TattleTaleWrapper {

    Session session;

    GreedyAlgorithm algo;

    Set<Cell> hideCellsWrapper = new HashSet<>();
    private static String checkerBear;
    private static String checkerBooBoo;
    public TattleTaleWrapper(Session session) {
        this.session = session;
    }

    public void run() {
        if (!session.getPagination() || session.getBinning_size() <= 1) {
            // store sensitive policies
            List<Cell> senCellsSave = session.getPolicies();

            runMain(session);

            // save back policies
            session.setPolicies(senCellsSave);
        }
        else
            binningAndMerging(session);

        hideCellsWrapper = session.getHideCells();
    }
    private static void testIssuesKDen(GreedyAlgorithm testAlgo,Set<Cell> testCells, String check1, String check2, long time, List<Cell> senCell){
        switch (testAlgo.usingAlgorithm){
            case "Perfect Deniability":
                try{
                    File perfFile = new File("C:\\Users\\Nick\\Desktop\\perfectdeniabilitycues.txt");
                    if(perfFile.createNewFile()){
                        FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\perfectdeniabilitycues.txt",true);
                        writer.write("Last run: ");
                        writer.write(check1);
                        writer.write(System.lineSeparator());
                        if(check2 != null){
                            writer.write("This run: ");
                            writer.write(check2);
                            writer.write(System.lineSeparator());
                        }
                        writer.write("Execution time: ");
                        writer.write(Long.toString(time));
                        writer.write(" ms");
                        writer.write(System.lineSeparator());
                        writer.write("Sensitive Cell Count: ");
                        writer.write(Integer.toString(senCell.size()));
                        writer.write(System.lineSeparator());
                        writer.write("Cell Count: ");
                        writer.write(Integer.toString(testCells.size()));
                        writer.write(System.lineSeparator());
                        writer.write("Sensitive Cells: ");
                        writer.write(System.lineSeparator());
                        Iterator it1 = senCell.iterator();
                        while (it1.hasNext()){
                            writer.write(it1.next().toString());
                            writer.write(System.lineSeparator());
                        }
                        Iterator it = testCells.iterator();
                        writer.write("Cueset Cells: ");
                        writer.write(System.lineSeparator());
                        while(it.hasNext()){
                            writer.write(it.next().toString());
                            writer.write(System.lineSeparator());
                        }
                        writer.close();
                    }
                    else{
                        FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\perfectdeniabilitycues.txt",true);
                        writer.write(System.lineSeparator());
                        writer.write("% NEW FULL DENIABILITY DATA %");
                        writer.write(System.lineSeparator());
                        writer.write("Last run: ");
                        writer.write(check1);
                        writer.write(System.lineSeparator());
                        if(check2 != null){
                            writer.write("This run: ");
                            writer.write(check2);
                            writer.write(System.lineSeparator());
                        }
                        writer.write("Execution time: ");
                        writer.write(Long.toString(time));
                        writer.write(" ms");
                        writer.write(System.lineSeparator());
                        writer.write("Sensitive Cell Count: ");
                        writer.write(Integer.toString(senCell.size()));
                        writer.write(System.lineSeparator());
                        writer.write("Cell Count: ");
                        writer.write(Integer.toString(testCells.size()));
                        writer.write(System.lineSeparator());
                        writer.write("Sensitive Cells: ");
                        writer.write(System.lineSeparator());
                        Iterator it1 = senCell.iterator();
                        while (it1.hasNext()){
                            writer.write(it1.next().toString());
                            writer.write(System.lineSeparator());
                        }
                        writer.write("Cueset Cells: ");
                        writer.write(System.lineSeparator());
                        Iterator it = testCells.iterator();
                        while(it.hasNext()){
                            writer.write(it.next().toString());
                            writer.write(System.lineSeparator());
                        }
                        writer.close();
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                break;
            case "K-value Deniability":
                try{
                    File kFile = new File("C:\\Users\\Nick\\Desktop\\kdeniabilitycues.txt");
                    if(kFile.createNewFile()){
                        FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\kdeniabilitycues.txt",true);
                        writer.write("Last run: ");
                        writer.write(check1);
                        writer.write(System.lineSeparator());
                        if(check2 != null){
                            writer.write("This run: ");
                            writer.write(check2);
                            writer.write(System.lineSeparator());
                        }
                        writer.write("Execution time: ");
                        writer.write(Long.toString(time));
                        writer.write(" ms");
                        writer.write(System.lineSeparator());
                        writer.write("Sensitive Cell Count: ");
                        writer.write(Integer.toString(senCell.size()));
                        writer.write(System.lineSeparator());
                        writer.write("Cell Count: ");
                        writer.write(Integer.toString(testCells.size()));
                        writer.write(System.lineSeparator());
                        writer.write("k-value: ");
                        writer.write(Double.toString(((GreedyKSecrecy) testAlgo).k_percentage));
                        writer.write(System.lineSeparator());
                        writer.write("Sensitive Cells: ");
                        writer.write(System.lineSeparator());
                        Iterator it1 = senCell.iterator();
                        while (it1.hasNext()){
                            writer.write(it1.next().toString());
                            writer.write(System.lineSeparator());
                        }
                        writer.write("Cueset Cells: ");
                        writer.write(System.lineSeparator());
                        Iterator it = testCells.iterator();
                        while(it.hasNext()){
                            writer.write(it.next().toString());
                            writer.write(System.lineSeparator());
                        }
                        writer.close();
                    }
                    else{
                        FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\kdeniabilitycues.txt",true);
                        writer.write(System.lineSeparator());
                        writer.write("% NEW K DENIABILITY DATA %");
                        writer.write(System.lineSeparator());
                        writer.write("Last run: ");
                        writer.write(check1);
                        writer.write(System.lineSeparator());
                        if(check2 != null){
                            writer.write("This run: ");
                            writer.write(check2);
                            writer.write(System.lineSeparator());
                        }
                        writer.write("Execution time: ");
                        writer.write(Long.toString(time));
                        writer.write(" ms");
                        writer.write(System.lineSeparator());
                        writer.write("Sensitive Cell Count: ");
                        writer.write(Integer.toString(senCell.size()));
                        writer.write(System.lineSeparator());
                        writer.write("Cell Count: ");
                        writer.write(Integer.toString(testCells.size()));
                        writer.write(System.lineSeparator());
                        writer.write("k-value: ");
                        writer.write(Double.toString(((GreedyKSecrecy) testAlgo).k_percentage));
                        writer.write(System.lineSeparator());
                        writer.write("Sensitive Cells: ");
                        writer.write(System.lineSeparator());
                        Iterator it1 = senCell.iterator();
                        while (it1.hasNext()){
                            writer.write(it1.next().toString());
                            writer.write(System.lineSeparator());
                        }
                        writer.write("Cueset Cells: ");
                        writer.write(System.lineSeparator());
                        Iterator it = testCells.iterator();
                        while(it.hasNext()){
                            writer.write(it.next().toString());
                            writer.write(System.lineSeparator());
                        }
                        writer.close();
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }

    }
    private  static  void whoChecksOnCheckerBear(GreedyAlgorithm testAlgo, String check1,String check2, Set<Cell> cues, List<Cell> senCells){
        try{
            File cFile = new File("C:\\Users\\Nick\\Desktop\\checkcues.txt");
            if(cFile.createNewFile()){
                FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\checkcues.txt",true);
                writer.write("Last run: ");
                writer.write(check1);
                writer.write(System.lineSeparator());
                if(check2 != null){
                    writer.write("This run: ");
                    writer.write(check2);
                    writer.write(System.lineSeparator());
                }
                writer.write(testAlgo.usingAlgorithm);
                writer.write(System.lineSeparator());
                writer.write("Sensitive Cell Count: ");
                writer.write(Integer.toString(senCells.size()));
                writer.write(System.lineSeparator());
                writer.write("Cell Count: ");
                writer.write(Integer.toString(cues.size()));
                writer.write(System.lineSeparator());
                Iterator it = cues.iterator();
                while(it.hasNext()){
                    writer.write(it.next().toString());
                    writer.write(System.lineSeparator());
                }
                writer.close();
            }
            else{
                FileWriter writer = new FileWriter("C:\\Users\\Nick\\Desktop\\checkcues.txt",true);
                writer.write(System.lineSeparator());
                writer.write("% NEW RUN DATA %");
                writer.write(System.lineSeparator());
                writer.write("Last run: ");
                writer.write(check1);
                writer.write(System.lineSeparator());
                if(check2 != null){
                    writer.write("This run: ");
                    writer.write(check2);
                    writer.write(System.lineSeparator());
                }
                writer.write(testAlgo.usingAlgorithm);
                writer.write(System.lineSeparator());
                writer.write("Cell Count: ");
                writer.write(Integer.toString(cues.size()));
                writer.write(System.lineSeparator());
                Iterator it = cues.iterator();
                while(it.hasNext()){
                    writer.write(it.next().toString());
                    writer.write(System.lineSeparator());
                }
                writer.close();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }
    Session runMain(Session curSession) {
        int checker = 0;

        Set<Cell> hideCells = new HashSet<>();
        if (checkerBear != null){
            if (checkerBear.isBlank()) {
                checkerBear = new String(String.valueOf((Objects.hash(curSession))));
            }
            else checker = 1;
        }
        if (curSession.getAlgo().equals("full-den")) {
            long initial,pre,hold,after;
            if (checker != 0){
                checkerBooBoo = new String(String.valueOf((Objects.hash(curSession))));
            }
            else {
                checkerBear = new String(String.valueOf((Objects.hash(curSession))));
            }
            List<Cell> senCell = curSession.getPolicies();
            initial = System.currentTimeMillis();
            algo = new GreedyPerfectSecrecy(curSession);
            algo.setUsingAlgorithm("Perfect Deniability");
            pre = System.currentTimeMillis() - initial;
            whoChecksOnCheckerBear(algo,checkerBear,checkerBooBoo,hideCells,senCell);
            hold = System.currentTimeMillis();
            hideCells.addAll(((GreedyPerfectSecrecy) algo).greedyHolisticPerfectDen());
            after = System.currentTimeMillis() - hold;
            testIssuesKDen(algo,hideCells,checkerBear,checkerBooBoo,pre+after,senCell);
            whoChecksOnCheckerBear(algo,checkerBear,checkerBooBoo,hideCells,senCell);
        }
        else if (curSession.getAlgo().equals("k-den")) {
            long initial,pre,hold,after;
            if (checker != 0){
                checkerBooBoo = new String(String.valueOf((Objects.hash(curSession))));
            }
            else {
                checkerBear = new String(String.valueOf((Objects.hash(curSession))));
            }
            List<Cell> senCell = curSession.getPolicies();
            initial = System.currentTimeMillis();
            algo = new GreedyKSecrecy(curSession);
            algo.setUsingAlgorithm("K-value Deniability");
            pre = System.currentTimeMillis() - initial;
            whoChecksOnCheckerBear(algo,checkerBear,checkerBooBoo,hideCells,senCell);
            hold = System.currentTimeMillis();
            hideCells.addAll(((GreedyKSecrecy) algo).greedyHolisticKDen());
            after = System.currentTimeMillis() - hold;
            testIssuesKDen(algo,hideCells,checkerBear,checkerBooBoo,pre+after,senCell);
            whoChecksOnCheckerBear(algo,checkerBear,checkerBooBoo,hideCells,senCell);
        }
        else if (curSession.getAlgo().equals("full-modified")) {
            algo = new GreedyPerfectSecrecyModified(curSession);
            algo.setUsingAlgorithm("Full Modified");
            hideCells.addAll(((GreedyPerfectSecrecyModified) algo).greedyHolisticPerfectDen());
        }

        curSession.setHideCells(hideCells);

        List<Cell> hiddenCellsToPolicies = new ArrayList<>(hideCells);
        curSession.setPolicies(hiddenCellsToPolicies);
        return curSession;
    }

    void binningAndMerging(Session session) {
        Queue<Session> binQueue = binning(session.getBinning_size());
        Queue<Session> mergeQueue = new LinkedList<>();

        while (binQueue.size() != 1 || !mergeQueue.isEmpty()) {
            Session curBin = binQueue.poll();
            assert curBin != null;
            mergeQueue.add(runMain(curBin));

            if (mergeQueue.size() >= session.getMerging_size() || binQueue.isEmpty()) {
                Session mergedBin = merging(mergeQueue);
                binQueue.add(runMain(mergedBin));
                mergeQueue.clear();
            }
        }

        session.setHideCells(binQueue.poll().getHideCells());
    }

    Queue<Session> binning(int bs) {

        Queue<Session> binQueue = new LinkedList<>();
        int DBSize = session.getLimit();
        for (int i=0; i<bs; i++)
            binQueue.add(new Session(session, calcStartTupleIndex(i, bs, DBSize), calcEndTupleIndex(i, bs, DBSize),
                    filterPolicies(i, bs, DBSize, session.getPolicies())));
        return binQueue;
    }

    Session merging(Queue<Session> mergeQueue) {
        int startTupleID = 0;
        int endTupleID = 0;
        Set<Cell> mergedPolicies = new HashSet<>();

        for (Session curSession: mergeQueue) {
            if (curSession.getTupleStart() < startTupleID)
                startTupleID = curSession.getTupleStart();
            if (curSession.getTupleEnd() > endTupleID)
                endTupleID = curSession.getTupleEnd();
            mergedPolicies.addAll(curSession.getHideCells());
        }

        List<Cell> mergedPoliciesList = new ArrayList<>(mergedPolicies);

        return new Session(session, startTupleID, endTupleID, mergedPoliciesList);
    }

    int calcStartTupleIndex(int i, int bs, int DBSize) {
        return (DBSize / bs) * i;
    }

    int calcEndTupleIndex(int i, int bs, int DBSize) {
        return (DBSize / bs) * (i + 1);
    }

    List<Cell> filterPolicies(int i, int bs, int DBSize, List<Cell> policies) {
        return policies.stream().filter(cell -> (cell.getTupleID() >= (DBSize / bs) * i) &&
                (cell.getTupleID() < (DBSize / bs) * (i+1)) ).collect(Collectors.toList());
    }

    public int getTotalCuesetSize() {
        return algo.getTotalCuesetSize();
    }

    public List<Integer> getHiddenCellsFanOut() {
        return algo.getHiddenCellsFanOut();
    }

    public List<Integer> getCueSetsFanOut() {
        return algo.getCueSetsFanOut();
    }

    public Set<Cell> getHideCells() {
        return hideCellsWrapper;
    }

}
