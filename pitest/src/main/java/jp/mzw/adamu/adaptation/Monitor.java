package jp.mzw.adamu.adaptation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import jp.mzw.adamu.adaptation.knowledge.Stats;
import jp.mzw.adamu.adaptation.knowledge.RtMS;
import jp.mzw.adamu.adaptation.knowledge.TestResult;

import org.pitest.mutationtest.DetectionStatus;
import org.pitest.mutationtest.MutationStatusTestPair;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitor function of MAPE-K control loop implemented in AdaMu
 * @author Yuta Maezawa
 */
public class Monitor {
    static Logger logger = LoggerFactory.getLogger(Monitor.class);
    
    /**
     * Incrementally count the number of available mutants
     * @param availableMutations
     * @throws SQLException
     */
    public static void getAailableMutations(Collection<MutationDetails> availableMutations) {
        try {
            int num = availableMutations.size();
            Stats.getInstance().insertNumMutants(num);
            logger.info("Available mutations: {}", num);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Store start time in Unix time
     * @throws SQLException
     */
    public static void startAdaMuLogger() {
        try {
            long time = System.currentTimeMillis();
            Stats.getInstance().insert(Stats.Label.StartTime, time);
            logger.info("Start: {}", time);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
     
    public static void monitorMutationResult(MutationIdentifier mutationId, MutationStatusTestPair result) {
        StringBuilder builder = new StringBuilder();
        builder.append(mutationId.getClassName())
             .append("#").append(mutationId.getLocation().getMethodName())
             .append(":").append("lineno")
             .append("<").append(mutationId.getMutator());
        TestResult.getInstance().insert(builder.toString(), result.getStatus().toString());
    }

    public static void monitorMutationsResult(Collection<MutationDetails> mutations, DetectionStatus status) {
        if (!status.equals(DetectionStatus.NOT_STARTED) && !status.equals(DetectionStatus.STARTED)) {
            for (MutationDetails mutation: mutations) {
                StringBuilder builder = new StringBuilder();
                builder.append(mutation.getClassName())
                        .append("#").append(mutation.getMethod())
                        .append(":").append(mutation.getLineNumber())
                        .append("<").append(mutation.getMutator());
                TestResult.getInstance().insert(builder.toString(), status.toString());
                try {
                    measureRuntimeMutationScore();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void measureRuntimeMutationScore() throws SQLException, InstantiationException, IllegalAccessException {
        int numExaminedMutants = 0;
        int numKilledMutants = 0;
        Statement stmt = TestResult.getInstance().getConnection().createStatement();
        ResultSet results = stmt.executeQuery("select status from test_results");
        while (results.next()) {
            numExaminedMutants += 1;
            String status = results.getString(1);
            if (status.equals(DetectionStatus.KILLED.name())
                    || status.equals(DetectionStatus.MEMORY_ERROR.name())
                    || status.equals(DetectionStatus.TIMED_OUT.name())
                    || status.equals(DetectionStatus.RUN_ERROR.name())) {
                numKilledMutants += 1;
            }  
        }
        RtMS rtms = new RtMS(numKilledMutants, numExaminedMutants);
        RtMS.getInstance().insert(rtms.getScore());
        logger.info("Runtime mutation score: {}", rtms.getScore());
        Analyzer.analyzeApproximateMutationScore(rtms);
    }
    
}
