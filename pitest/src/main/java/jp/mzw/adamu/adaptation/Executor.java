package jp.mzw.adamu.adaptation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.concurrent.FutureTask;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.adamu.adaptation.knowledge.AMS;
import jp.mzw.adamu.adaptation.knowledge.Knowledge;
import jp.mzw.adamu.adaptation.knowledge.KnowledgeBase;
import jp.mzw.adamu.adaptation.knowledge.Log;
import jp.mzw.adamu.adaptation.knowledge.Overhead;
import jp.mzw.adamu.adaptation.knowledge.Stats;
import jp.mzw.adamu.adaptation.knowledge.data.Mutation;
import jp.mzw.adamu.adaptation.knowledge.data.TestResult;

/**
 * Execute function of MAPE-K control loop implemented in AdaMu
 * 
 * @author Yuta Maezawa
 */
public class Executor extends MAPE {
    static Logger logger = LoggerFactory.getLogger(Executor.class);

    /**
     * Execute to interrupt mutation testing while forecasting AMS
     * 
     * @throws SQLException
     */
    public static void execute(List<TestResult> testResultList, List<Mutation> mutationList) throws SQLException {
        int num_total_mutants = mutationList.size();
        int num_examined_mutants = testResultList.size();
        
        // for making faster
        if (skip(num_examined_mutants, num_total_mutants)) {
//            logger.info("Skip by interval design... #Examined: {}", num_examined_mutants);
            return;
        }

        // forecast
        long start = System.currentTimeMillis();
        double ams = forecastAms(testResultList, mutationList);
        if (ams < 0) {
            return;
        }
        boolean consumed = consumed(testResultList, mutationList, ams);
        AMS.getInstance().insert(num_examined_mutants, ams);
        logger.info("Approximate mutation score: {} @ {}", ams, num_examined_mutants);
        long end = System.currentTimeMillis();
        Overhead.getInstance().insert(Overhead.Type.Forecast, end - start);

        if (consumed) {
            _finalize();
        }
        
    }
    
    /**
     * Here AdaMu users describe to validate whether their limited computational resources are consumed or not
     * @param testResultList
     * @param mutationList
     * @param ams
     * @return true if consumed, otherwise false
     */
    public static boolean consumed(List<TestResult> testResultList, List<Mutation> mutationList, double ams) {
        boolean consumed = false;

        // For exampled AMS validation
        int num_total_mutants = mutationList.size();
        int num_examined_mutants = testResultList.size();
        int num_killed_mutants = getNumKilledMutants(testResultList);
        consumed = consumed && isValid(ams, num_examined_mutants, num_killed_mutants, num_total_mutants);
        
        return consumed;
    }

    /**
     * Get the number of killed mutants so far
     * 
     * @param testResultList
     *            a list of test execution results on each mutant so far
     * @return the number of killed mutants so far
     */
    public static int getNumKilledMutants(List<TestResult> testResultList) {
        int num_killed_mutants = 0;
        for (TestResult test_result : testResultList) {
            if (KnowledgeBase.isKilled(test_result.getStatus())) {
                num_killed_mutants++;
            }
        }
        return num_killed_mutants;
    }

    /**
     * Validates an approximate mutation score (AMS)
     * 
     * @param ams
     *            an AMS
     * @param numExaminedMutants
     *            the number of examined mutants so far
     * @param numKilledMutants
     *            the number of killed mutants so far
     * @param numTotalMutants
     *            the number of created mutants
     * @return true if an AMS is in a valid score range
     */
    public static boolean isValid(double ams, int numExaminedMutants, int numKilledMutants, int numTotalMutants) {
        int numRemainingMutants = numTotalMutants - numExaminedMutants;
        int numMaxKilledMutants = numKilledMutants + numRemainingMutants;
        double maxMutationScore = (double) numMaxKilledMutants / (double) numTotalMutants;
        double minMutationScore = (double) numKilledMutants / (double) numTotalMutants;
        if (minMutationScore <= ams && ams <= maxMutationScore) {
            return true;
        }
        return false;
    }

    /**
     * Forecasts an approximate mutation score (AMS)
     * 
     * @param testResultList
     *            a list of test execution results on each mutant so far
     * @param mutationList
     *            a list of created mutants
     * @return an AMS
     */
    private static double forecastAms(List<TestResult> testResultList, List<Mutation> mutationList) {
        // collect facts
        Map<String, List<TestResult>> method_result_map = new HashMap<>();
        double num_killed_mutants = 0;
        List<Integer> examined_mutants_hashcode_list = new ArrayList<>();
        for (TestResult test_result : testResultList) {
            String key = getClassMethodKey(test_result);
            List<TestResult> value = method_result_map.get(key);
            if (value == null) {
                value = new ArrayList<>();
            }
            value.add(test_result);
            method_result_map.put(key, value);
            examined_mutants_hashcode_list.add(test_result.getHashcode());
            if (KnowledgeBase.isKilled(test_result.getStatus())) {
                num_killed_mutants += 1.0;
            }
        }
        // forecast
        for (Mutation mutation : mutationList) {
            if (!examined_mutants_hashcode_list.contains(mutation.getHashcode())) {
                String key = getClassMethodKey(mutation);
                List<TestResult> corr_examined_mutants = method_result_map.get(key);
                if (corr_examined_mutants == null) {
                    return -1.0;
                }

                int num_corr_killed_mutants = 0;
                int num_corr_examined_mutants = 0;
                for (TestResult test_result : corr_examined_mutants) {
                    num_corr_examined_mutants++;
                    if (KnowledgeBase.isKilled(test_result.getStatus())) {
                        num_corr_killed_mutants++;
                    }
                }
                double forecasted_score = (double) num_corr_killed_mutants / (double) (num_corr_examined_mutants);
                num_killed_mutants += 1.0 * forecasted_score;
            }
        }
        // approximate mutation score
        return num_killed_mutants / (double) mutationList.size();
    }

    /**
     * Get a key string based on names of class and method where PIT applies a
     * mutation operator
     * 
     * @param mutation
     *            a mutants created by PIT
     * @return a key string
     */
    private static String getClassMethodKey(Mutation mutation) {
        return mutation.getClassName() + "#" + mutation.getMethodName();
    }

    public static void _finalize() {
        Knowledge.output();
        Knowledge.closeDataBases();
        Log.copyResultsWithTimestamp();
    }

    /**
     * Interrupt running PIT when developers decide to accept suggestions from
     * running AdaMu
     * 
     * @author Yuta Maezawa
     */
    public static class Interrupter extends Thread {
        /**
         * To possess thread pool in running PIT
         */
        ThreadPoolExecutor executor;

        /**
         * To interrupt running PIT from running AdaMu
         * 
         * @param executor
         *            Thread pool in running PIT
         */
        public Interrupter(ThreadPoolExecutor executor) {
            this.executor = executor;
        }

        /**
         * Wait for make decision from developers or finished AdaMu
         */
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Stats.Label label = decideQuit();
                    if (label != null) {
                        if (Stats.Label.Quit.equals(label)) {
                            List<Runnable> workers = executor.shutdownNow();
                            for (Runnable worker : workers) {
                                FutureTask<?> task = (FutureTask<?>) worker;
                                task.cancel(true);
                            }
                        }
                        Executor._finalize();
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    public static Stats.Label decideQuit() {
        try {
            Connection conn = Stats.class.newInstance().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet results = stmt.executeQuery("select key from stats");
            while (results.next()) {
                String key = results.getString(1);
                if (Stats.Label.Quit.name().equals(key) || Stats.Label.Finish.name().equals(key)) {
                    results.close();
                    stmt.close();
                    return Stats.Label.valueOf(key);
                }
            }
            results.close();
            stmt.close();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
