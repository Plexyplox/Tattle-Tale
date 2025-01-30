package edu.policy.execution;

import edu.policy.helper.Utils;
import edu.policy.manager.*;
import edu.policy.model.constraint.Cell;
import edu.policy.model.data.Session;
import edu.policy.model.test.ExpOut;
import edu.policy.model.test.TestCase;
import edu.policy.model.test.TestFileParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Experiment {

    private static List<TestCase> testCases;

    private static final File testCaseDir = new File("C:\\Users\\Nick\\Documents\\GitHub\\Tattle-Tale\\testdata\\testcases");

    private static final File[] testFileNames = testCaseDir.listFiles();

    private static final Logger logger = LogManager.getLogger(Experiment.class);

    public static void main(String[] args) throws Exception {

        assert testFileNames != null;
        for (File testFileName: testFileNames) {
            logger.info(String.format("Test file name: %s", testFileName));
            test(testFileName.toString());
        }
    }

    static void test(String testFileName) throws Exception {

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
        String outputFileName = "report(" + timeStamp + ").csv";

        ExpOut exp_report;

        int DBSize = 0;

        try {
            testCases = Arrays.asList(TestFileParser.testParser(testFileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (TestCase testCase: testCases) {

            Session session = Utils.createSessionFromTestCase(testCase);

            // load dependencies
            loadDeps(session);

            DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            // run algo in wrapper
            TattleTaleWrapper wrapper = new TattleTaleWrapper(session);

            long startTime = new Date().getTime();

            wrapper.run();

            long endTime = new Date().getTime();

            long timeElapsed = endTime - startTime;

            logger.info(String.format("Finished executing the algorithm for the current testcase; use time: %d ms.", timeElapsed));

            // exporting report
            exp_report = new ExpOut(session, wrapper.getTotalCuesetSize(), wrapper.getHideCells().size(), wrapper.getHideCells(),
                    formatter.format(timeElapsed), wrapper.getHiddenCellsFanOut(), wrapper.getCueSetsFanOut(), outputFileName,
                    session.getTestOblCueset(), session.getPagination());

            try {

                String dir = System.getProperty("user.dir");
                String reportDir = String.valueOf(Paths.get(dir, "plot", "eva", testCase.getTestname()));

                exp_report.toFile(reportDir);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static void loadDeps(Session session) throws Exception {

        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        long startTime = new Date().getTime();

        // Parse dependencies
        Parser parser = new Parser(session);

        parser.loadDependencies(System.getProperty("user.dir") + session.getDCDir());
        parser.parse();

        long endTime = new Date().getTime();
        long timeElapsed = endTime - startTime;

        logger.info(String.format("Finished parsing all dependencies; use time: %d ms.", timeElapsed));

    }
}
