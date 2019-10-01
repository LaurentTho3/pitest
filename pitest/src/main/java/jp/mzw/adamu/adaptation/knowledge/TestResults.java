package jp.mzw.adamu.adaptation.knowledge;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;

public class TestResults extends KnowledgeBase implements DataBase {

	protected static TestResults instance = null;

	public static TestResults getInstance() {
		if (instance == null) {
			instance = new TestResults();
		}
		return instance;
	}

	private static Connection conn = null;

	@Override
	public Connection getConnection() throws SQLException {
		if (conn == null) {
			Log.getLatestDir();
			conn = DriverManager.getConnection("jdbc:sqlite:logs/latest/test_results.db");
		}
		return conn;
	}

	@Override
	public void init() throws SQLException {
		Statement stmt = getConnection().createStatement();
		stmt.executeUpdate("drop table if exists test_results");
		stmt.executeUpdate("create table test_results (time integer, hashcode integer, class_name string, method_name string, lineno integer, mutator string, status string)");
		stmt.close();
	}

	@Override
	public void close() throws SQLException {
		if (conn != null && !conn.isClosed()) {
			conn.close();
		}
	}

	public synchronized void insert(int hashcode, String className, String methodName, int lineno, String mutator, String status) {
		try {
			StringBuilder query = new StringBuilder();
			query.append("insert into test_results values (");
			query.append(System.currentTimeMillis());
			query.append(",");
			query.append(hashcode);
			query.append(",");
			query.append("'").append(className).append("'");
			query.append(",");
			query.append("'").append(methodName).append("'");
			query.append(",");
			query.append(lineno);
			query.append(",");
			query.append("'").append(mutator).append("'");
			query.append(",");
			query.append("'").append(status).append("'");
			query.append(")");
			
			Statement stmt = getConnection().createStatement();
			stmt.executeUpdate(query.toString());
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void output() {
		try {
			long start = Stats.class.newInstance().getStartTime();
			StringBuilder builder = new StringBuilder();

			Statement stmt = getConnection().createStatement();
			ResultSet results = stmt.executeQuery("select time, hashcode, class_name, method_name, lineno, mutator, status from test_results");
			while (results.next()) {
				int time = (int) (results.getInt(1) - start);
				int hashcode = results.getInt(2);
				String class_name = results.getString(3);
				String method_name = results.getString(4);
				int lineno = results.getInt(5);
				String mutator = results.getString(6);
				String status = results.getString(7);
				
				
				builder.append(time).append(COMMA)
					.append(hashcode).append(COMMA)
					.append(class_name).append(COMMA)
					.append(method_name).append(COMMA)
					.append(lineno).append(COMMA)
					.append(mutator).append(COMMA)
					.append(status)
					.append(BR);
			}
			results.close();
			stmt.close();

			FileUtils.write(new File(Log.getLatestDir(), "test_results.csv"), builder.toString());
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
