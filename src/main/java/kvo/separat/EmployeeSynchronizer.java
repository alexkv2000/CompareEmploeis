package kvo.separat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;

import java.sql.*;

public class EmployeeSynchronizer extends AbstractSynchronizer<RowEmployee> {

    public EmployeeSynchronizer(MeterRegistry registry) {
        super(registry);
    }

    @Override protected String getMainTableName() { return "dEmployes"; }
    @Override protected String getDelTableName() { return "del_Employes"; }
    @Override protected String getIdColumn() { return "EMPLOYEEID"; }

    // НОВЫЙ МЕТОД
    @Override protected String getOracleViewName() { return "SL.DOC_EMP_VW"; }

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

    // --- Метрики ---
    @Override protected Counter getSyncCounter() { return App.employeeSyncCounter; }
    @Override protected Counter getErrorCounter() { return App.employeeSyncErrorCounter; }
    @Override protected Timer getSyncTimer() { return App.employeeSyncTimer; }
    // (getRowsProcessedCounter, getRowsInsertedCounter, getRowsDeletedCounter удалены отсюда)

    // --- НОВЫЙ МЕТОД: Маппинг ResultSet -> Объект ---
    @Override
    protected RowEmployee mapRow(ResultSet rs) throws SQLException {
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
        setInsertParams(ps, r);
    }
}