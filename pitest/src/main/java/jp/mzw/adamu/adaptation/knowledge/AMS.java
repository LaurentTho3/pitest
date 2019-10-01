package jp.mzw.adamu.adaptation.knowledge;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

public class AMS extends KnowledgeBase implements DataBase {
    
    protected AMS() { /* NOP */ }
    protected static AMS instance = null;
    public static AMS getInstance() {
        if (instance == null) {
            instance = new AMS();
        }
        return instance;
    }

    private static Connection conn = null;
    
    @Override
    public Connection getConnection() throws SQLException {
         if (conn == null) {
             Log.getLatestDir();
              conn = DriverManager.getConnection("jdbc:sqlite:logs/latest/ams.db");
         }
         return conn;
    }
    
    @Override
    public void init() throws SQLException {
        Statement stmt = getConnection().createStatement();
        stmt.executeUpdate("drop table if exists ams");
        stmt.executeUpdate("create table ams (time integer, mutant_order integer, score real)");
        stmt.close();
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
    
    public synchronized void insert(int order, double score) throws SQLException {
        Statement stmt = getConnection().createStatement();
        stmt.executeUpdate("insert into ams values (" + System.currentTimeMillis() + "," + order + "," + score + ")");
        stmt.close();
    }
    
    private int[] timeArray;
    private double[] scoreArray;
    
    public AMS(int[] timeArray, double[] scoreArray) {
        this.timeArray = timeArray;
        this.scoreArray = scoreArray;
    }
    
    public int[] getTimeArray() {
        return this.timeArray;
    }
    
    public double[] getScoreArray() {
        return this.scoreArray;
    }
    
    public AMS getAmsArray() throws SQLException, InstantiationException, IllegalAccessException {
        Statement stmt = getConnection().createStatement();
        ResultSet results = stmt.executeQuery("select time, score from ams");
        // not long but integer for R
        long start = Stats.class.newInstance().getStartTime();
        ArrayList<Integer> timeList = new ArrayList<Integer>();
        ArrayList<Double> amsList = new ArrayList<Double>();
        while (results.next()) {
             int time = (int) (results.getLong(1) - start);
             double score = results.getDouble(2);
             timeList.add(time);
             amsList.add(score);
        }
        results.close();
        stmt.close();
        int[] timeArray = new int[timeList.size()];
        double[] amsArray = new double[amsList.size()];
        for (int i = 0; i < timeList.size(); i++) {
             timeArray[i] = timeList.get(i);
             amsArray[i] = amsList.get(i);
        }
        
        return new AMS(timeArray, amsArray);
    }

    @Override
    public void output() {
        try {
            long start = Stats.class.newInstance().getStartTime();
            StringBuilder builder = new StringBuilder();
            String delim = "";
            
            Statement stmt = getConnection().createStatement();
            ResultSet results = stmt.executeQuery("select time, mutant_order, score from ams");
            while (results.next()) {
                int time = (int)(results.getInt(1) - start);
                int order = results.getInt(2);
                double score = results.getDouble(3);
                builder.append(delim).append(time).append(COMMA).append(order).append(COMMA).append(score);
                delim = BR;
            }
            results.close();
            stmt.close();

            FileUtils.write(new File(Log.getLatestDir(), "ams.csv"),  builder.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
}
