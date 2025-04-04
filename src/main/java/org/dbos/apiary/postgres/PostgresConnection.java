package org.dbos.apiary.postgres;

import org.dbos.apiary.connection.ApiaryConnection;
import org.dbos.apiary.function.FunctionOutput;
import org.dbos.apiary.function.ApiaryContext;
import org.dbos.apiary.function.ProvenanceBuffer;
import org.dbos.apiary.procedures.postgres.GetApiaryClientID;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.dbos.apiary.utilities.ApiaryConfig.getApiaryClientID;

/**
 * A connection to a Postgres database.
 */
public class PostgresConnection implements ApiaryConnection {
    private static final Logger logger = LoggerFactory.getLogger(PostgresConnection.class);

    private final PGSimpleDataSource ds;
    private final ThreadLocal<Connection> connection;

    private final Map<String, Callable<PostgresFunction>> functions = new HashMap<>();

    /**
     * Create a connection to a Postgres database.
     * @param hostname the Postgres database hostname.
     * @param port the Postgres database port.
     * @param databaseName the Postgres database name.
     * @param databaseUsername the Postgres database username.
     * @param databasePassword the Postgres database password.
     * @throws SQLException
     */
    public PostgresConnection(String hostname, Integer port, String databaseName, String databaseUsername, String databasePassword) throws SQLException {
        this.ds = new PGSimpleDataSource();
        this.ds.setServerNames(new String[] {hostname});
        this.ds.setPortNumbers(new int[] {port});
        this.ds.setDatabaseName(databaseName);
        this.ds.setUser(databaseUsername);
        this.ds.setPassword(databasePassword);
        this.ds.setSsl(false);

        this.connection = ThreadLocal.withInitial(() -> {
           try {
               Connection conn = ds.getConnection();
               conn.setAutoCommit(false);
               return conn;
           } catch (SQLException e) {
               e.printStackTrace();
           }
           return null;
        });
        try {
            Connection testConn = ds.getConnection();
            testConn.close();
        } catch (SQLException e) {
            logger.info("Failed to connect to Postgres");
            throw new RuntimeException("Failed to connect to Postgres");
        }
        createTable("RecordedOutputs", "ExecID bigint, FunctionID bigint, StringOutput VARCHAR(1000), IntOutput integer, StringArrayOutput bytea, IntArrayOutput bytea, FutureOutput bigint, QueuedTasks bytea, PRIMARY KEY(ExecID, FunctionID)");
        createTable("FuncInvocations", "APIARY_TRANSACTION_ID BIGINT NOT NULL, APIARY_TIMESTAMP BIGINT NOT NULL, EXECUTIONID BIGINT NOT NULL, SERVICE VARCHAR(1024) NOT NULL, PROCEDURENAME VARCHAR(1024) NOT NULL");
        createTable("ApiaryMetadata", "Key VARCHAR(1024) NOT NULL, Value Integer, PRIMARY KEY(key)");
        registerFunction(getApiaryClientID, GetApiaryClientID::new);
    }

    /**
     * Register a PostgresFunction.
     * @param name The name of the function.
     * @param function The constructor of the function.
     */
    public void registerFunction(String name, Callable<PostgresFunction> function) { functions.put(name, function); }

    /**
     * Drop a table and its corresponding events table if they exist.
     * @param tableName the table to drop.
     * @throws SQLException
     */
    public void dropTable(String tableName) throws SQLException {
        Connection conn = ds.getConnection();
        Statement truncateTable = conn.createStatement();
        truncateTable.execute(String.format("DROP TABLE IF EXISTS %s;", tableName));
        truncateTable.execute(String.format("DROP TABLE IF EXISTS %sEvents;", tableName));
        truncateTable.close();
        conn.close();
    }

    /**
     * Create a table and a corresponding events table.
     * @param tableName the table to create.
     * @param specStr the schema of the table, in Postgres DDL.
     * @throws SQLException
     */
    public void createTable(String tableName, String specStr) throws SQLException {
        Connection conn = ds.getConnection();
        Statement s = conn.createStatement();
        s.execute(String.format("CREATE TABLE IF NOT EXISTS %s (%s);", tableName, specStr));
        if (!specStr.contains("APIARY_TRANSACTION_ID")) {
            ResultSet r = s.executeQuery(String.format("SELECT * FROM %s", tableName));
            ResultSetMetaData rsmd = r.getMetaData();
            StringBuilder provTable = new StringBuilder(String.format("CREATE TABLE IF NOT EXISTS %sEvents (APIARY_TRANSACTION_ID BIGINT NOT NULL, APIARY_TIMESTAMP BIGINT NOT NULL, APIARY_OPERATION_TYPE BIGINT NOT NULL", tableName));
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                provTable.append(",");
                provTable.append(rsmd.getColumnLabel(i + 1));
                provTable.append(" ");
                provTable.append(rsmd.getColumnTypeName(i + 1));
            }
            provTable.append(");");
            s.execute(provTable.toString());
        }
        s.close();
        conn.close();
    }

    @Override
    public FunctionOutput callFunction(ProvenanceBuffer provBuff, String service, long execID, long functionID, String name, Object... inputs) throws Exception {
        PostgresFunction function = functions.get(name).call();
        ApiaryContext ctxt = new PostgresContext(connection.get(), provBuff, service, execID, functionID);
        FunctionOutput f = null;
        try {
            f = function.apiaryRunFunction(ctxt, inputs);
            connection.get().commit();
        } catch (Exception e) {
            e.printStackTrace();
            connection.get().rollback();
        }
        return f;
    }

    @Override
    public void updatePartitionInfo() {
        // Nothing here.
        return;
    }

    @Override
    public int getNumPartitions() {
        return 1;
    }

    @Override
    public String getHostname(Object... input) {
        return "localhost";
    }

    @Override
    public Map<Integer, String> getPartitionHostMap() {
        return Map.of(0, "localhost");
    }

}
