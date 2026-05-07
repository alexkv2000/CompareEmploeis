package kvo.separat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeSynchronizer extends AbstractSynchronizer<RowEmployee> {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeSynchronizer.class);

    public EmployeeSynchronizer(MeterRegistry registry) {
        super(registry);
    }

    // --- Реализация абстрактных методов (метаданные таблиц) ---
    @Override protected String getMainTableName() { return "dEmployes"; }
    @Override protected String getDelTableName() { return "del_Employes"; }
    @Override protected String getIdColumn() { return "EMPLOYEEID"; }

    // --- SQL Запросы ---
    @Override
    protected String getInsertSQL() {
        return "INSERT INTO " + getMainTableName() + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, DATE_CREATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getDelInsertSQL() {
        return "INSERT INTO " + getDelTableName() + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, DATE_CREATE, DATE_DELETE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
    }

    @Override
    protected String getLocalQuery() {
        return "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID, DATE_CREATE FROM " + getMainTableName();
    }

    @Override
    protected String getOracleQuery() {
        return "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID, GETDATE() AS DATE_CREATE FROM OPENQUERY(oraclecis, 'SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID FROM SL.DOC_EMP_VW')";
    }

    // --- Метрики (берем их из статических полей класса App) ---
    @Override protected Counter getSyncCounter() { return App.employeeSyncCounter; }
    @Override protected Counter getErrorCounter() { return App.employeeSyncErrorCounter; }
    @Override protected Timer getSyncTimer() { return App.employeeSyncTimer; }
    @Override protected Counter getRowsProcessedCounter() { return App.rowsProcessedCounter; }
    @Override protected Counter getRowsInsertedCounter() { return App.rowsInsertedCounter; }
    @Override protected Counter getRowsDeletedCounter() { return App.rowsDeletedCounter; }

    // --- Загрузка данных из БД ---
    @Override
    protected List<RowEmployee> loadLocal(Connection conn) throws SQLException {
        List<RowEmployee> list = new ArrayList<>();
        logger.info(">>Running query : {}", getMainTableName());
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(getLocalQuery())) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    protected List<RowEmployee> loadOracle(Connection conn) throws SQLException {
        List<RowEmployee> list = new ArrayList<>();
        logger.info(">>Request running Oracle (Employees) : SL.DOC_EMP_VW");
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(getOracleQuery())) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // Вспомогательный метод маппинга ResultSet -> Объект
    private RowEmployee mapRow(ResultSet rs) throws SQLException {
        return new RowEmployee(
                rs.getString("EMPLOYEEID"), rs.getString("LASTNAMERUS"), rs.getString("NAMERUS"),
                rs.getString("MIDDLENAMERUS"), rs.getString("TABNOM"), rs.getString("JOBTITLERUS"),
                rs.getString("LOGINNAME"), rs.getString("EMAIL"), rs.getString("IPPHONE"),
                rs.getString("WORKPHONE"), rs.getString("TYPE_WORK"), rs.getString("DEPARTMENTID"),
                rs.getString("MANAGERID"), rs.getString("USER_SID"), rs.getString("DATE_CREATE")
        );
    }

    // --- Настройка параметров (PreparedStatement) ---
    @Override
    protected void setInsertParams(PreparedStatement ps, RowEmployee r) throws SQLException {
        ps.setString(1, r.getEmployeeID());
        ps.setString(2, r.getLastnameRUS());
        ps.setString(3, r.getNameRUS());
        ps.setString(4, r.getMiddleNameRUS());
        ps.setString(5, r.getTabNom());
        ps.setString(6, r.getJobTitleRUS());
        ps.setString(7, r.getLoginName());
        ps.setString(8, r.getEmail());
        ps.setString(9, r.getIpPhone());
        ps.setString(10, r.getWorkPhone());
        ps.setString(11, r.getTypeWork());
        ps.setString(12, r.getDepartmentID());
        ps.setString(13, r.getManagerID());
        ps.setString(14, r.getUserSID());
        ps.setString(15, r.getDateCreate());
    }

    @Override
    protected void setDelInsertParams(PreparedStatement ps, RowEmployee r) throws SQLException {
        // Параметры идентичны вставке в основную таблицу
        setInsertParams(ps, r);
    }
}