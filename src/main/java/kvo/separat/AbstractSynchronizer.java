package kvo.separat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractSynchronizer<T extends SyncEntity> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSynchronizer.class);
    protected final MeterRegistry registry;

    protected AbstractSynchronizer(MeterRegistry registry) {
        this.registry = registry;
    }

    // --- Абстрактные методы (реализуются в наследниках) ---
    protected abstract String getMainTableName();
    protected abstract String getDelTableName();
    protected abstract String getIdColumn();
    protected abstract String getInsertSQL();
    protected abstract String getDelInsertSQL();
    protected abstract String getLocalQuery();
    protected abstract String getOracleQuery();

    protected abstract Counter getSyncCounter();
    protected abstract Counter getErrorCounter();
    protected abstract Timer getSyncTimer();
    protected abstract Counter getRowsProcessedCounter();
    protected abstract Counter getRowsInsertedCounter();
    protected abstract Counter getRowsDeletedCounter();

    protected abstract List<T> loadLocal(Connection conn) throws SQLException;
    protected abstract List<T> loadOracle(Connection conn) throws SQLException;
    protected abstract void setInsertParams(PreparedStatement ps, T entity) throws SQLException;
    protected abstract void setDelInsertParams(PreparedStatement ps, T entity) throws SQLException;

    // --- Общий алгоритм синхронизации (Template Method) ---
    public void sync(Connection conn) {
        Timer.Sample sample = Timer.start(registry);
        getSyncCounter().increment();
        try {
            createDelTableIfNotExists(conn);

            List<T> localRows = loadLocal(conn);
            logger.info("Loaded from {} : {} rows", getMainTableName(), localRows.size());

            List<T> oracleRows = loadOracle(conn);
            logger.info("Loaded from Oracle: {} rows", oracleRows.size());

            getRowsProcessedCounter().increment(oracleRows.size());

            Set<String> oracleHashes = oracleRows.stream().map(this::getHash).collect(Collectors.toSet());

            List<T> rowsToDelete = localRows.stream()
                    .filter(row -> !oracleHashes.contains(getHash(row)))
                    .toList();

            insertRowsToDelAndDeleteMain(conn, rowsToDelete);
            getRowsDeletedCounter().increment(rowsToDelete.size());

            Set<String> localHashes = localRows.stream().map(this::getHash).collect(Collectors.toSet());
            int inserted = insertNewRows(conn, oracleRows, localHashes);
            getRowsInsertedCounter().increment(inserted);

        } catch (Exception e) {
            getErrorCounter().increment();
            logger.error("Error in synchronization for {}", getMainTableName(), e);
        } finally {
            sample.stop(getSyncTimer());
        }
    }

    private String getHash(T entity) {
        try {
            return HashUtil.getMD5(entity.getDataForHashing());
        } catch (Exception e) {
            throw new SyncException("Failed to generate MD5 for entity: " + entity.getId(), e);
        }
    }

    @SuppressWarnings("java:S2077")
    private void createDelTableIfNotExists(Connection conn) throws SQLException {
        String newTable = getDelTableName();
        String mainTable = getMainTableName();
        String sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name = '" + newTable + "' AND xtype='U') BEGIN SELECT TOP 0 * INTO " + newTable + " FROM " + mainTable + "; ALTER TABLE " + newTable + " ADD date_delete DATETIME DEFAULT GETDATE(); END";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            if (!e.getSQLState().equals("42S01") && !e.getMessage().contains("already exists")) throw e;
        }
    }
    @SuppressWarnings("java:S2077")
    private int insertNewRows(Connection conn, List<T> oracleRows, Set<String> localHashes) throws SQLException {
        List<T> toInsert = oracleRows.stream().filter(r -> !localHashes.contains(getHash(r))).toList();
        if (toInsert.isEmpty()) return 0;

        try (PreparedStatement ps = conn.prepareStatement(getInsertSQL())) {
            for (T row : toInsert) {
                setInsertParams(ps, row);
                ps.addBatch();
            }
            ps.executeBatch();
            logger.info("Inserted a new rows : {}", toInsert.size());
        }
        return toInsert.size();
    }
    @SuppressWarnings("java:S2077")
    private void insertRowsToDelAndDeleteMain(Connection conn, List<T> rowsToDelete) throws SQLException {
        if (rowsToDelete.isEmpty()) return;

        try (PreparedStatement ps = conn.prepareStatement(getDelInsertSQL())) {
            for (T row : rowsToDelete) {
                setDelInsertParams(ps, row);
                ps.addBatch();
            }
            ps.executeBatch();
            logger.info("Unloaded in {}: {} rows", getDelTableName(), rowsToDelete.size());
        }

        String deleteSQL = "DELETE FROM " + getMainTableName() + " WHERE " + getIdColumn() + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
            for (T row : rowsToDelete) {
                ps.setString(1, row.getId());
                ps.addBatch();
            }
            ps.executeBatch();
            logger.info("Delete from {} : {} rows.", getMainTableName(), rowsToDelete.size());
        }
    }
}