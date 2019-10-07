package jp.mzw.adamu.adaptation.knowledge;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;

public class Stats extends KnowledgeBase implements DataBase {

    protected static Stats instance = null;
    public static Stats getInstance() {
        if (instance == null) {
            instance = new Stats();
        }
        return instance;
    }

    
    private static Connection conn = null;
    
    @Override
    public Connection getConnection() throws SQLException {
         if (conn == null) {
             Log.getLatestDir();
              conn = DriverManager.getConnection("jdbc:sqlite:logs/latest/stats.db");
         }
         return conn;
    }
    
    @Override
    public void init() throws SQLException {
        Statement stmt = getConnection().createStatement();
        stmt.executeUpdate("drop table if exists stats");
        stmt.executeUpdate("create table stats (time integer, key string, value string)");
        stmt.close();
    }
    
    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    private synchronized void insert(String sql) throws SQLException {
         try {
              Statement stmt = getConnection().createStatement();
              stmt.executeUpdate(sql);
              stmt.close();
         } catch (SQLException e) {
              e.printStackTrace();
         }
    }

    private void insert(String key, String value) throws SQLException {
        insert("insert into stats values (" + System.currentTimeMillis() + ",'" + key + "','" + value + "')");
    }
    
    public void insert(Label label, String value) throws SQLException {
        insert(label.name(), value);
    }
    
    public void insert(Label label, int value) throws SQLException {
        insert(label.name(), Integer.toString(value));
    }
    
    public void insert(Label label, long value) throws SQLException {
        insert(label.name(), Long.toString(value));
    }
    
    public static enum Label {
         StartTime,
         Burnin,
         Suggest,
         Quit,
         Finish,
    }
    
    public long getStartTime() throws SQLException {
        Statement stmt = getConnection().createStatement();
        ResultSet results = stmt.executeQuery("select value from stats where key='" + Label.StartTime + "'");
        long start = Long.parseLong(results.getString(1));
        results.close();
        stmt.close();
        return start;
    }
    
    public int getNumMutantsBurnin() {
        try {
            Statement stmt = getConnection().createStatement();
            ResultSet results = stmt.executeQuery("select value from stats where key='" + Label.Burnin + "'");
            int ret = results.getInt(1);
            results.close();
            stmt.close();
            return ret;
        } catch (SQLException e) {
            return -1;
        }
    }
    
    public int getNumMutantsStop() {
        try {
            Statement stmt = getConnection().createStatement();
            ResultSet results = stmt.executeQuery("select value from stats where key='" + Label.Quit + "'");
            int ret = results.getInt(1);
            results.close();
            stmt.close();
            return ret;
        } catch (SQLException e) {
            return -1;
        }
    }
    
    @Override
    public void output() {
        try {
            Long start = null;
            Long end = null;
            {
                Statement stmt = getConnection().createStatement();
                ResultSet results = stmt.executeQuery("select value from stats where key='" + Label.StartTime + "'");
                start = Long.parseLong(results.getString(1));
                results.close();
                stmt.close();
            }
            {
                Statement stmt = getConnection().createStatement();
                ResultSet results = stmt.executeQuery("select value from stats where key='" + Label.Finish + "'");
                end = Long.parseLong(results.getString(1));
                results.close();
                stmt.close();
            }
            Long elapsed_time = end - start;
            FileUtils.write(new File(Log.getLatestDir(), "elapsed_time.csv"),  elapsed_time.toString());
            
            {
                StringBuilder builder = new StringBuilder();
                Statement stmt = getConnection().createStatement();
                ResultSet results = stmt.executeQuery("select time, key, value from stats");
                while (results.next()) {
                    int time = (int) (results.getInt(1) - start);
                    String key = results.getString(2);
                    String value = results.getString(3);
                    builder.append(time).append(COMMA)
                        .append(key).append(COMMA)
                        .append(value).append(BR);
                }
                results.close();
                stmt.close();
                FileUtils.write(new File(Log.getLatestDir(), "stats.csv"), builder.toString());
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
