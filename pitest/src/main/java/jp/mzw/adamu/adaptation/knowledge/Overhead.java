package jp.mzw.adamu.adaptation.knowledge;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class Overhead extends KnowledgeBase implements DataBase {

    protected static Overhead instance = null;
    public static Overhead getInstance() {
        if (instance == null) {
            instance = new Overhead();
        }
        return instance;
    }

    private static Connection conn = null;

    @Override
    public Connection getConnection() throws SQLException {
        if (conn == null) {
            Log.getLatestDir();
            conn = DriverManager.getConnection("jdbc:sqlite:logs/latest/overheads.db");
       }
       return conn;
    }

    @Override
    public void init() throws SQLException {
        Statement stmt = getConnection().createStatement();
        stmt.executeUpdate("drop table if exists overhead");
        stmt.executeUpdate("create table overhead (time integer, source string, overhead string)");
        stmt.close();
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    public static enum Type {
    	TestExecOrder,
        Forecast,
        Converge,
        StopDecision,
    }

    public synchronized void insert(Type type, long time) {
         try {
              Statement stmt = getConnection().createStatement();
              stmt.executeUpdate("insert into overhead values (" + System.currentTimeMillis() + ",'" + type.name() + "','" + time + "')");
              stmt.close();
         } catch (SQLException e) {
              e.printStackTrace();
         }
    }

    @Override
    public void output() {
        try {
            // for forecasting
            {
                Statement stmt = getConnection().createStatement();
				ResultSet results = stmt.executeQuery("select overhead from overhead where source = '" + Overhead.Type.Forecast + "'");
				List<String> lines = new ArrayList<>();
		        while (results.next()) {
		        	lines.add(results.getString(1));
		        }
	            FileUtils.writeLines(new File(Log.getLatestDir(), "overhead.forecast.csv"),  lines);
	            results.close();
	            stmt.close();
            }
            // for converging
            {
                Statement stmt = getConnection().createStatement();
				ResultSet results = stmt.executeQuery("select overhead from overhead where source = '" + Overhead.Type.Converge + "'");
				List<String> lines = new ArrayList<>();
		        while (results.next()) {
		        	lines.add(results.getString(1));
		        }
	            FileUtils.writeLines(new File(Log.getLatestDir(), "overhead.converge.csv"),  lines);
	            results.close();
	            stmt.close();
            }
            // for making stop decision
            {
                Statement stmt = getConnection().createStatement();
				ResultSet results = stmt.executeQuery("select overhead from overhead where source = '" + Overhead.Type.StopDecision + "'");
				List<String> lines = new ArrayList<>();
		        while (results.next()) {
		        	lines.add(results.getString(1));
		        }
	            FileUtils.writeLines(new File(Log.getLatestDir(), "overhead.stop.csv"),  lines);
	            results.close();
	            stmt.close();
            }
            // for manipulating test execution order
            {
                Statement stmt = getConnection().createStatement();
				ResultSet results = stmt.executeQuery("select overhead from overhead where source = '" + Overhead.Type.TestExecOrder + "'");
				List<String> lines = new ArrayList<>();
		        while (results.next()) {
		        	lines.add(results.getString(1));
		        }
	            FileUtils.writeLines(new File(Log.getLatestDir(), "overhead.order.csv"),  lines);
	            results.close();
	            stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
