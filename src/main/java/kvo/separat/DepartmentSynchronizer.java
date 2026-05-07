package kvo.separat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentSynchronizer extends AbstractSynchronizer<RowDepartments> {
    private static final Logger logger = LoggerFactory.getLogger(DepartmentSynchronizer.class);

    public DepartmentSynchronizer(MeterRegistry registry) {
        super(registry);
    }

    @Override protected String getMainTableName() { return "DDEPARTMENTS"; }
    @Override protected String getDelTableName() { return "del_Departments"; }
    @Override protected String getIdColumn() { return "DepartmentID"; }

    @Override
    protected String getInsertSQL() {
        return "INSERT INTO " + getMainTableName() + " (DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getDelInsertSQL() {
        return "INSERT INTO " + getDelTableName() + " (DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE, DATE_DELETE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
    }

    @Override protected String getLocalQuery() {
        return "SELECT DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE FROM " + getMainTableName();
    }

    @Override protected String getOracleQuery() {
        return "SELECT ID as DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, GETDATE() AS DATE_CREATE FROM OPENQUERY(oraclecis, 'SELECT ID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN FROM sl.doc_dpt_vw')";
    }

    @Override protected Counter getSyncCounter() { return App.departmentSyncCounter; }
    @Override protected Counter getErrorCounter() { return App.departmentSyncErrorCounter; }
    @Override protected Timer getSyncTimer() { return App.departmentSyncTimer; }
    @Override protected Counter getRowsProcessedCounter() { return App.rowsProcessedCounter; }
    @Override protected Counter getRowsInsertedCounter() { return App.rowsInsertedCounter; }
    @Override protected Counter getRowsDeletedCounter() { return App.rowsDeletedCounter; }

    @Override
    protected List<RowDepartments> loadLocal(Connection conn) throws SQLException {
        List<RowDepartments> list = new ArrayList<>();
        logger.info(">>Running query : {}", getMainTableName());
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(getLocalQuery())) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    protected List<RowDepartments> loadOracle(Connection conn) throws SQLException {
        List<RowDepartments> list = new ArrayList<>();
        logger.info(">>Request running Oracle (Department) : SL.DOC_DPT_VW");
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(getOracleQuery())) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private RowDepartments mapRow(ResultSet rs) throws SQLException {
        return new RowDepartments(
                rs.getString("DepartmentID"), rs.getString("NAME"), rs.getString("MANAGERID"),
                rs.getString("MANAGERLOGINNAME"), rs.getString("PARENTID"), rs.getString("TYPE_NAME"),
                rs.getString("CODE"), rs.getString("B_DATE"), rs.getString("E_DATE"),
                rs.getString("DATA_INTEG"), rs.getString("E_DOC"), rs.getString("ID_DEPT_OWN"),
                rs.getString("DATE_CREATE")
        );
    }

    @Override
    protected void setInsertParams(PreparedStatement ps, RowDepartments r) throws SQLException {
        ps.setString(1, r.getId()); ps.setString(2, r.getName()); ps.setString(3, r.getManagerID());
        ps.setString(4, r.getManagerLoginName()); ps.setString(5, r.getParentID()); ps.setString(6, r.getTypeName());
        ps.setString(7, r.getCode()); ps.setString(8, r.getbDate()); ps.setString(9, r.geteDate());
        ps.setString(10, r.getDataIntegration()); ps.setString(11, r.geteDoc()); ps.setString(12, r.getIdDeptOwn());
        ps.setString(13, r.getDateCreate());
    }

    @Override
    protected void setDelInsertParams(PreparedStatement ps, RowDepartments r) throws SQLException {
        setInsertParams(ps, r); // Точно такие же параметры, просто в таблицу del_*
    }
}