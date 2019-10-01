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

import jp.mzw.adamu.adaptation.knowledge.data.Mutation;

import org.apache.commons.io.FileUtils;

public class Mutations extends KnowledgeBase implements DataBase {

	protected static Mutations instance = null;

	public static Mutations getInstance() {
		if (instance == null) {
			instance = new Mutations();
		}
		return instance;
	}

	private static Connection conn = null;

	@Override
	public Connection getConnection() throws SQLException {
		if (conn == null) {
			Log.getLatestDir();
			conn = DriverManager.getConnection("jdbc:sqlite:logs/latest/mutations.db");
		}
		return conn;
	}

	@Override
	public void init() throws SQLException {
		Statement stmt = getConnection().createStatement();
		stmt.executeUpdate("drop table if exists mutations");
		stmt.executeUpdate("create table mutations (hashcode integer, class_name string, method_name string, lineno integer, mutation_operator string)");
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

	public void insert(int hashcode, String className, String methodName, int lineno, String mutator) throws SQLException {
		StringBuilder query = new StringBuilder();
		query.append("insert into mutations values (");
		query.append(hashcode);
		query.append(",");
		query.append("'").append(className).append("'");
		query.append(",");
		query.append("'").append(methodName).append("'");
		query.append(",");
		query.append(lineno);
		query.append(",");
		query.append("'").append(mutator).append("'");
		query.append(")");
		insert(query.toString());
	}

	protected static List<Mutation> mutationList = null;

	public List<Mutation> getMutations() throws SQLException {
		if (mutationList != null) {
			return mutationList;
		}
		List<Mutation> mutationList = new ArrayList<>();
		Statement stmt = getConnection().createStatement();
		ResultSet results = stmt.executeQuery("select hashcode, class_name, method_name, lineno, mutation_operator from mutations");
		while (results.next()) {
			int hashcode = results.getInt(1);
			String class_name = results.getString(2);
			String method_name = results.getString(3);
			int lineno = results.getInt(4);
			String mutator = results.getString(5);
			mutationList.add(new Mutation(hashcode, class_name, method_name, lineno, mutator));
		}
		results.close();
		stmt.close();
		return mutationList;
	}

	@Override
	public void output() {
		try {
			StringBuilder builder = new StringBuilder();
			Statement stmt = getConnection().createStatement();
			ResultSet results = stmt.executeQuery("select hashcode, class_name, method_name, lineno, mutation_operator from mutations");
			while (results.next()) {
				int hashcode = results.getInt(1);
				String class_name = results.getString(2);
				String method_name = results.getString(3);
				int lineno = results.getInt(4);
				String mutator = results.getString(5);
				
				builder.append(hashcode).append(",")
					.append(class_name).append(",")
					.append(method_name).append(",")
					.append(lineno).append(",")
					.append(mutator).append("\n");
			}
			results.close();
			stmt.close();
			FileUtils.write(new File(Log.getLatestDir(), "mutations.csv"), builder.toString());
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
