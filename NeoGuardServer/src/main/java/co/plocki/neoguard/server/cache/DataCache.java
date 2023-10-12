package co.plocki.neoguard.server.cache;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.RunScript;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class DataCache {

    public void init() {
    }

    private JdbcConnectionPool openPool(String file) {
        // Initialize the H2 database and create the necessary table
        JdbcConnectionPool connectionPool = JdbcConnectionPool.create("jdbc:h2:mem:" + file + ";DB_CLOSE_DELAY=-1", "", "");
        try (Connection conn = connectionPool.getConnection()) {

            File f;
            InputStream scriptStream;

            if((f =  new File("structure" + File.separator + file + ".jdat")).exists()) {
                scriptStream = new FileInputStream(f);
            } else {
                scriptStream = getClass().getResourceAsStream("/schema.sql");
            }

            if (scriptStream != null) {
                RunScript.execute(conn, new InputStreamReader(scriptStream));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing H2 database", e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return connectionPool;
    }

    private void save(String file, JdbcConnectionPool pool) {
        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement();
             PrintWriter writer = new PrintWriter("structure" + File.separator + file + ".jdat", StandardCharsets.UTF_8)) {

            // Export schema
            ResultSet schemaResultSet = stmt.executeQuery("SCRIPT");
            while (schemaResultSet.next()) {
                String line = schemaResultSet.getString(1);
                writer.println(line);
            }

            // Export data
            ResultSet dataResultSet = stmt.executeQuery("SELECT * FROM cache");
            while (dataResultSet.next()) {
                String insertStatement = generateInsertStatement(dataResultSet);
                writer.println(insertStatement);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateInsertStatement(ResultSet resultSet) throws Exception {
        StringBuilder insertStatement = new StringBuilder("INSERT INTO cache VALUES (");
        int columnCount = resultSet.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            Object value = resultSet.getObject(i);
            if (value == null) {
                insertStatement.append("NULL");
            } else if (value instanceof String) {
                insertStatement.append("'").append(value).append("'");
            } else {
                insertStatement.append(value);
            }
            if (i < columnCount) {
                insertStatement.append(", ");
            }
        }
        insertStatement.append(");");
        return insertStatement.toString();
    }

    public Object getData(String file) {

        JdbcConnectionPool pool = openPool(file);

        try (Connection conn = pool.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT data FROM cache WHERE file = ?")) {
            pstmt.setString(1, file);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    pool.dispose();
                    return rs.getObject("data");
                }
            }
        } catch (SQLException e) {
            pool.dispose();
            throw new RuntimeException("Error retrieving data from the database", e);
        }

        pool.dispose();
        return null;
    }

    public void updateData(String file, Object data) {

        JdbcConnectionPool pool = openPool(file);

        try (Connection conn = pool.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("MERGE INTO cache (file, data) VALUES (?, ?)")) {
            pstmt.setString(1, file);
            pstmt.setObject(2, data);
            pstmt.executeUpdate();

            save(file, pool);
        } catch (SQLException e) {
            pool.dispose();
            throw new RuntimeException("Error updating data in the database", e);
        }

        pool.dispose();
    }

    public void deleteData(String file) {

        JdbcConnectionPool pool = openPool(file);

        try (Connection conn = pool.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM cache WHERE file = ?")) {
            pstmt.setString(1, file);
            pstmt.executeUpdate();

            save(file, pool);
        } catch (SQLException e) {
            pool.dispose();
            throw new RuntimeException("Error deleting data from the database", e);
        }
        pool.dispose();
    }
}
