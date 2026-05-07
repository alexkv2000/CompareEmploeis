package kvo.separat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

// Micrometer imports
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    static final String ROWS = " rows";
    static final String DATECREATE = "DATE_CREATE";
    static final String MANAGER = "MANAGERID";

    // Данные для подключения к базе данных
    private static String dbUrl;
    private static String user;
    private static String pass;

    // Названия таблиц
    private static final String MAIN_TABLE_EMPLOYEES = "dEmployes";
    private static final String DEL_MAIN_TABLE_EMPLOYEES = "del_Employes"; // Для удаляемых строк
    private static final String MAIN_TABLE_DEPARTMENTS = "dDepartments".toUpperCase();
    private static final String DEL_MAIN_TABLE_DEPARTMENTS = "del_Departments"; // Для удаляемых строк

    // Micrometer metrics
    private static PrometheusMeterRegistry registry;
    private static Counter employeeSyncCounter;
    private static Counter departmentSyncCounter;
    private static Counter employeeSyncErrorCounter;
    private static Counter departmentSyncErrorCounter;
    private static Timer employeeSyncTimer;
    private static Timer departmentSyncTimer;
    private static Counter rowsProcessedCounter;
    private static Counter rowsInsertedCounter;
    private static Counter rowsDeletedCounter;
    private static JvmGcMetrics jvmGcMetrics;

    public enum AnsiColor {
        RESET("\033[0m"), BLACK("\033[30m"), RED("\033[31m"), GREEN("\033[32m"), YELLOW("\033[33m"), BLUE("\033[34m"), PURPLE("\033[35m"), CYAN("\033[36m"), WHITE("\033[37m"),
        BRIGHT_BLACK("\033[90m"), BRIGHT_RED("\033[91m"), BRIGHT_GREEN("\033[92m"), BRIGHT_YELLOW("\033[93m"), BRIGHT_BLUE("\033[94m"), BRIGHT_PURPLE("\033[95m"), BRIGHT_CYAN("\033[96m"), BRIGHT_WHITE("\033[97m"),
        BG_BLACK("\033[40m"), BG_RED("\033[41m"), BG_GREEN("\033[42m"), BG_YELLOW("\033[43m"), BG_BLUE("\033[44m"), BG_PURPLE("\033[45m"), BG_CYAN("\033[46m"), BG_WHITE("\033[47m"),
        BG_BRIGHT_BLACK("\033[100m"), BG_BRIGHT_RED("\033[101m"), BG_BRIGHT_GREEN("\033[102m"), BG_BRIGHT_YELLOW("\033[103m"), BG_BRIGHT_BLUE("\033[104m"), BG_BRIGHT_PURPLE("\033[105m"), BG_BRIGHT_CYAN("\033[106m"), BG_BRIGHT_WHITE("\033[107m"),
        BOLD("\033[1m"), UNDERLINE("\033[4m"), REVERSED("\033[7m");

        private final String code;

        AnsiColor(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }
    }

    static String currentDir;

    // Класс для представления строки таблицы
    @SuppressWarnings("java:S107") // Отключаем предупреждение о количестве параметров (15 шт)
    static class RowEmployee {
        String employeeID;
        String lastnameRUS;
        String nameRUS;
        String middleNameRUS;
        String tabNom;
        String jobTitleRUS;
        String loginName;
        String email;
        String ipPhone;
        String workPhone;
        String typeWork;
        String departmentID;
        String managerID;
        String userSID;
        String dateCreate;

        RowEmployee(String employeeID, String lastnameRUS, String nameRUS, String middleNameRUS, String tabNom, String jobTitleRUS, String loginName, String email, String ipPhone, String workPhone, String typeWork, String departmentID, String managerID, String userSID, String dateCreate) {
            this.employeeID = employeeID;
            this.lastnameRUS = lastnameRUS;
            this.nameRUS = nameRUS;
            this.middleNameRUS = middleNameRUS;
            this.tabNom = tabNom;
            this.jobTitleRUS = jobTitleRUS;
            this.loginName = loginName;
            this.email = email;
            this.ipPhone = ipPhone;
            this.workPhone = workPhone;
            this.typeWork = typeWork;
            this.departmentID = departmentID;
            this.managerID = managerID;
            this.userSID = userSID;
            this.dateCreate = dateCreate;
        }

        @SuppressWarnings("java:S4790")
        String getMD5() throws NoSuchAlgorithmException {
            String data = employeeID + "|" + lastnameRUS + "|" + nameRUS + "|" + middleNameRUS + "|" + tabNom + "|" + jobTitleRUS + "|" + loginName + "|" + email + "|" + ipPhone + "|" + workPhone + "|" + typeWork + "|" + departmentID + "|" + managerID + "|" + userSID;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RowEmployee other = (RowEmployee) obj;
            return Objects.equals(employeeID, other.employeeID) && Objects.equals(lastnameRUS, other.lastnameRUS) && Objects.equals(nameRUS, other.nameRUS) && Objects.equals(middleNameRUS, other.middleNameRUS) && Objects.equals(tabNom, other.tabNom) && Objects.equals(jobTitleRUS, other.jobTitleRUS) && Objects.equals(loginName, other.loginName) && Objects.equals(email, other.email) && Objects.equals(ipPhone, other.ipPhone) && Objects.equals(workPhone, other.workPhone) && Objects.equals(typeWork, other.typeWork) && Objects.equals(departmentID, other.departmentID) && Objects.equals(managerID, other.managerID) && Objects.equals(userSID, other.userSID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(employeeID, lastnameRUS, nameRUS, middleNameRUS, tabNom, jobTitleRUS, loginName, email, ipPhone, workPhone, typeWork, departmentID, managerID, userSID);
        }
    }

    @SuppressWarnings("java:S107") // Отключаем предупреждение о количестве параметров
    static class RowDepartments {
        String departmentID;
        String name;
        String managerID;
        String managerLoginName;
        String parentID;
        String typeName;
        String code;
        String bDate;
        String eDate;
        String dataIntegration;
        String eDoc;
        String idDeptOwn;
        String dateCreate;

        RowDepartments(String departmentID, String name, String managerID, String managerLoginName, String parentID, String typeName, String code, String bDate, String eDate, String dataIntegration, String eDoc, String idDeptOwn, String dateCreate) {
            this.departmentID = departmentID;
            this.name = name;
            this.managerID = managerID;
            this.managerLoginName = managerLoginName;
            this.parentID = parentID;
            this.typeName = typeName;
            this.code = code;
            this.bDate = bDate;
            this.eDate = eDate;
            this.dataIntegration = dataIntegration;
            this.eDoc = eDoc;
            this.idDeptOwn = idDeptOwn;
            this.dateCreate = dateCreate;
        }

        @SuppressWarnings("java:S4790")
        String getMD5() throws NoSuchAlgorithmException {
            String data = departmentID + "|" + name + "|" + managerID + "|" + managerLoginName + "|" + parentID + "|" + typeName + "|" + code + "|" + bDate + "|" + eDate + "|" + eDoc + "|" + idDeptOwn;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RowDepartments other = (RowDepartments) obj;
            return Objects.equals(departmentID, other.departmentID) && Objects.equals(name, other.name) && Objects.equals(managerID, other.managerID) && Objects.equals(managerLoginName, other.managerLoginName) && Objects.equals(parentID, other.parentID) && Objects.equals(typeName, other.typeName) && Objects.equals(code, other.code) && Objects.equals(bDate, other.bDate) && Objects.equals(eDate, other.eDate) && Objects.equals(dataIntegration, other.dataIntegration) && Objects.equals(eDoc, other.eDoc) && Objects.equals(idDeptOwn, other.idDeptOwn);
        }

        @Override
        public int hashCode() {
            return Objects.hash(departmentID, name, managerID, managerLoginName, parentID, typeName, code, bDate, eDate, eDoc, idDeptOwn);
        }
    }

    public static void main(String[] args) throws IOException {
        initializeMonitoring();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (jvmGcMetrics != null) jvmGcMetrics.close();
        }));

        currentDir = System.getProperty("user.dir");
        String configPath = currentDir + "\\src\\main\\java\\config\\settingSynDictionary.txt";
        String syncDic = "department";

        for (String arg : args) {
            if (arg.startsWith("config.path=")) configPath = arg.substring("config.path=".length());
            if (arg.startsWith("dictionary=")) syncDic = arg.substring("dictionary=".length());
        }

        ConfigLoader configLoader = new ConfigLoader(configPath);
        dbUrl = configLoader.getProperty("DB_URL");
        user = configLoader.getProperty("USER");
        pass = configLoader.getProperty("PASS");

        switch (syncDic.toLowerCase()) {
            case "department" -> {
                logSyncExecution(App::syncDepartment);
                safeSleep();
            }
            case "employees" -> {
                logSyncExecution(App::syncEmployees);
                safeSleep();
            }
            default -> {
                logSyncExecution(App::syncDepartment);
                safeSleep();
                logSyncExecution(App::syncEmployees);
                safeSleep();
            }
        }
    }

    private static void initializeMonitoring() {
        final String APPLICATION = "application";
        final String SYNCAPP = "sync-app";
        try {
            registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            employeeSyncCounter = Counter.builder("employee_sync_total").description("Total number of employee synchronizations").tag(APPLICATION, SYNCAPP).register(registry);
            departmentSyncCounter = Counter.builder("department_sync_total").description("Total number of department synchronizations").tag(APPLICATION, SYNCAPP).register(registry);
            employeeSyncErrorCounter = Counter.builder("employee_sync_errors_total").description("Total number of employee synchronization errors").tag(APPLICATION, SYNCAPP).register(registry);
            departmentSyncErrorCounter = Counter.builder("department_sync_errors_total").description("Total number of department synchronization errors").tag(APPLICATION, SYNCAPP).register(registry);
            employeeSyncTimer = Timer.builder("employee_sync_duration_seconds").description("Employee synchronization duration in seconds").tag(APPLICATION, SYNCAPP).register(registry);
            departmentSyncTimer = Timer.builder("department_sync_duration_seconds").description("Department synchronization duration in seconds").tag(APPLICATION, SYNCAPP).register(registry);
            rowsProcessedCounter = Counter.builder("rows_processed_total").description("Total number of rows processed").tag(APPLICATION, SYNCAPP).register(registry);
            rowsInsertedCounter = Counter.builder("rows_inserted_total").description("Total number of rows inserted").tag(APPLICATION, SYNCAPP).register(registry);
            rowsDeletedCounter = Counter.builder("rows_deleted_total").description("Total number of rows deleted").tag(APPLICATION, SYNCAPP).register(registry);

            new JvmMemoryMetrics().bindTo(registry);
            jvmGcMetrics = new JvmGcMetrics();
            jvmGcMetrics.bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
            new UptimeMetrics().bindTo(registry);
        } catch (Exception e) {
            logger.error("Failed to initialize monitoring in sync-app", e);
        }
    }

    private static void syncDepartment() {
        Timer.Sample sample = Timer.start(registry);
        departmentSyncCounter.increment();
        try {
            try (Connection conn = DriverManager.getConnection(dbUrl, user, pass)) {
                createDelTableIfNotExists(conn, MAIN_TABLE_DEPARTMENTS, DEL_MAIN_TABLE_DEPARTMENTS);
                List<RowDepartments> mainRowDepartments = loadRowsFromLocalDepartments(conn);
                logger.info("Loaded from {} : {} {}", MAIN_TABLE_DEPARTMENTS, mainRowDepartments.size(), ROWS);
                List<RowDepartments> oracleRowDepartments = loadRowsFromOracleDepartments(conn);
                if (logger.isInfoEnabled()) {
                    logger.info("Loaded from Oracle ({}): {} {}", "sl.doc_dpt_vw".toUpperCase(), oracleRowDepartments.size(), ROWS);
                }
                rowsProcessedCounter.increment(oracleRowDepartments.size());

                Set<String> oracleHashes = new HashSet<>();
                for (RowDepartments row : oracleRowDepartments) oracleHashes.add(row.getMD5());

                List<RowDepartments> rowsToDelete = new ArrayList<>();
                for (RowDepartments row : mainRowDepartments) {
                    if (!oracleHashes.contains(row.getMD5())) rowsToDelete.add(row);
                }

                insertRowsToDelDepartments(conn, rowsToDelete);
                deleteRowsFromMainDepartments(conn, rowsToDelete);
                rowsDeletedCounter.increment(rowsToDelete.size());

                Set<String> originalMainHashes = new HashSet<>();
                for (RowDepartments row : mainRowDepartments) originalMainHashes.add(row.getMD5());
                int inserted = insertNewRowsDepartments(conn, oracleRowDepartments, originalMainHashes);
                rowsInsertedCounter.increment(inserted);
            }
        } catch (Exception e) {
            departmentSyncErrorCounter.increment();
            logger.error("Error in department synchronization", e);
        } finally {
            sample.stop(departmentSyncTimer);
        }
    }

    private static void syncEmployees() {
        Timer.Sample sample = Timer.start(registry);
        employeeSyncCounter.increment();
        try {
            try (Connection conn = DriverManager.getConnection(dbUrl, user, pass)) {
                createDelTableIfNotExists(conn, MAIN_TABLE_EMPLOYEES, DEL_MAIN_TABLE_EMPLOYEES);
                List<RowEmployee> mainRowsEmployee = loadRowsFromLocalEmployee(conn);
                if (logger.isInfoEnabled()) {
                    logger.info("Downloaded from {}: {} {}", MAIN_TABLE_EMPLOYEES.toUpperCase(), mainRowsEmployee.size(), ROWS);
                }
                List<RowEmployee> oracleRowEmployees = loadRowsFromOracleEmployee(conn);
                if (logger.isInfoEnabled()) {
                    logger.info("Download from Oracle ({}): {} {}", "sl.doc_emp_vw".toUpperCase(), oracleRowEmployees.size(), ROWS);
                }
                rowsProcessedCounter.increment(oracleRowEmployees.size());

                Set<String> oracleHashes = new HashSet<>();
                for (RowEmployee row : oracleRowEmployees) oracleHashes.add(row.getMD5());

                List<RowEmployee> rowsToDelete = new ArrayList<>();
                for (RowEmployee row : mainRowsEmployee) {
                    if (!oracleHashes.contains(row.getMD5())) rowsToDelete.add(row);
                }

                insertRowsToDelEmployees(conn, rowsToDelete);
                deleteRowsFromMainEmployees(conn, rowsToDelete);
                rowsDeletedCounter.increment(rowsToDelete.size());

                Set<String> originalMainHashes = new HashSet<>();
                for (RowEmployee row : mainRowsEmployee) originalMainHashes.add(row.getMD5());
                int inserted = insertNewRowsEmployees(conn, oracleRowEmployees, originalMainHashes);
                rowsInsertedCounter.increment(inserted);
            }
        } catch (Exception e) {
            employeeSyncErrorCounter.increment();
            logger.error("Error in employee synchronization", e);
        } finally {
            sample.stop(employeeSyncTimer);
        }
    }

    @SuppressWarnings("java:S2077")
    private static void createDelTableIfNotExists(Connection conn, String mainTable, String newTable) throws SQLException {
        String sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name = '" + newTable + "' AND xtype='U') BEGIN SELECT TOP 0 * INTO " + newTable + " FROM " + mainTable + "; ALTER TABLE " + newTable + " ADD date_delete DATETIME DEFAULT GETDATE(); END";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            if (!e.getSQLState().equals("42S01") && !e.getMessage().contains("already exists")) throw e;
        }
    }

    // Извлекаем маппинг в отдельный метод, чтобы избавиться от дублирования кода
    private static RowEmployee mapRowToEmployee(ResultSet rs) throws SQLException {
        return new RowEmployee(
                rs.getString("EMPLOYEEID"), rs.getString("LASTNAMERUS"), rs.getString("NAMERUS"),
                rs.getString("MIDDLENAMERUS"), rs.getString("TABNOM"), rs.getString("JOBTITLERUS"),
                rs.getString("LOGINNAME"), rs.getString("EMAIL"), rs.getString("IPPHONE"),
                rs.getString("WORKPHONE"), rs.getString("TYPE_WORK"), rs.getString("DEPARTMENTID"),
                rs.getString(MANAGER), rs.getString("USER_SID"), rs.getString(DATECREATE)
        );
    }

    @SuppressWarnings("java:S2077")
    private static List<RowEmployee> loadRowsFromLocalEmployee(Connection conn) throws SQLException {
        List<RowEmployee> list = new ArrayList<>();
        String query = "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID, DATE_CREATE FROM " + MAIN_TABLE_EMPLOYEES;
        logger.info(">>Running query : " + MAIN_TABLE_EMPLOYEES);
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) list.add(mapRowToEmployee(rs));
        }
        return list;
    }

    private static List<RowEmployee> loadRowsFromOracleEmployee(Connection conn) throws SQLException {
        List<RowEmployee> list = new ArrayList<>();
        String query = "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID, GETDATE() AS DATE_CREATE FROM OPENQUERY(oraclecis, 'SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID FROM SL.DOC_EMP_VW')";
        logger.info(">>Request running Oracle (Employees) : SL.DOC_EMP_VW");
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) list.add(mapRowToEmployee(rs));
        }
        return list;
    }

    // Извлекаем маппинг в отдельный метод для Departments
    private static RowDepartments mapRowToDepartment(ResultSet rs) throws SQLException {
        return new RowDepartments(
                rs.getString("DepartmentID"), rs.getString("NAME"), rs.getString(MANAGER),
                rs.getString("MANAGERLOGINNAME"), rs.getString("PARENTID"), rs.getString("TYPE_NAME"),
                rs.getString("CODE"), rs.getString("B_DATE"), rs.getString("E_DATE"),
                rs.getString("DATA_INTEG"), rs.getString("E_DOC"), rs.getString("ID_DEPT_OWN"),
                rs.getString(DATECREATE)
        );
    }

    @SuppressWarnings("java:S2077")
    private static List<RowDepartments> loadRowsFromLocalDepartments(Connection conn) throws SQLException {
        List<RowDepartments> list = new ArrayList<>();
        String query = "SELECT DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE FROM " + MAIN_TABLE_DEPARTMENTS;
        logger.info(">>Request running : DB {}", MAIN_TABLE_DEPARTMENTS);
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) list.add(mapRowToDepartment(rs));
        }
        return list;
    }

    private static List<RowDepartments> loadRowsFromOracleDepartments(Connection conn) throws SQLException {
        List<RowDepartments> list = new ArrayList<>();
        String query = "SELECT ID as DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, GETDATE() AS DATE_CREATE FROM OPENQUERY(oraclecis, 'SELECT ID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN FROM sl.doc_dpt_vw')";
        logger.info(">>Request running Oracle (Departments): SL.DOC_DPT_VW");
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) list.add(mapRowToDepartment(rs));
        }
        return list;
    }

    @SuppressWarnings("java:S1192")
    private static int insertNewRowsEmployees(Connection conn, List<RowEmployee> oracleRowEmployees, Set<String> mainHashes) throws SQLException, NoSuchAlgorithmException {
        String insertSQL = "INSERT INTO " + MAIN_TABLE_EMPLOYEES + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, DATE_CREATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            int inserted = 0;
            for (RowEmployee row : oracleRowEmployees) {
                if (!mainHashes.contains(row.getMD5())) {
                    pstmt.setString(1, row.employeeID);
                    pstmt.setString(2, row.lastnameRUS);
                    pstmt.setString(3, row.nameRUS);
                    pstmt.setString(4, row.middleNameRUS);
                    pstmt.setString(5, row.tabNom);
                    pstmt.setString(6, row.jobTitleRUS);
                    pstmt.setString(7, row.loginName);
                    pstmt.setString(8, row.email);
                    pstmt.setString(9, row.ipPhone);
                    pstmt.setString(10, row.workPhone);
                    pstmt.setString(11, row.typeWork);
                    pstmt.setString(12, row.departmentID);
                    pstmt.setString(13, row.managerID);
                    pstmt.setString(14, row.userSID);
                    pstmt.setString(15, row.dateCreate);
                    pstmt.addBatch();
                    inserted++;
                }
            }
            if (inserted > 0) {
                pstmt.executeBatch();
                logger.info("Inserted a new rows : {}", inserted);
            }
            return inserted;
        }
    }

    @SuppressWarnings("java:S2077")
    private static int insertNewRowsDepartments(Connection conn, List<RowDepartments> oracleDepartments, Set<String> mainHashes) throws SQLException, NoSuchAlgorithmException {
        String insertSQL = "INSERT INTO " + MAIN_TABLE_DEPARTMENTS + " (DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement prepareStatement = conn.prepareStatement(insertSQL)) {
            int inserted = 0;
            for (RowDepartments row : oracleDepartments) {
                if (!mainHashes.contains(row.getMD5())) {
                    prepareStatement.setString(1, row.departmentID);
                    prepareStatement.setString(2, row.name);
                    prepareStatement.setString(3, row.managerID);
                    prepareStatement.setString(4, row.managerLoginName);
                    prepareStatement.setString(5, row.parentID);
                    prepareStatement.setString(6, row.typeName);
                    prepareStatement.setString(7, row.code);
                    prepareStatement.setString(8, row.bDate);
                    prepareStatement.setString(9, row.eDate);
                    prepareStatement.setString(10, row.dataIntegration);
                    prepareStatement.setString(11, row.eDoc);
                    prepareStatement.setString(12, row.idDeptOwn);
                    prepareStatement.setString(13, row.dateCreate);
                    prepareStatement.addBatch();
                    inserted++;
                }
            }
            if (inserted > 0) {
                prepareStatement.executeBatch();
                logger.info("Inserted a new rows : {}", inserted);
            }
            return inserted;
        }
    }

    private static void insertRowsToDelEmployees(Connection conn, List<RowEmployee> rowEmployees) throws SQLException {
        String insertSQL = "INSERT INTO " + DEL_MAIN_TABLE_EMPLOYEES + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, DATE_CREATE, DATE_DELETE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
        try (PreparedStatement prepareStatement = conn.prepareStatement(insertSQL)) {
            for (RowEmployee row : rowEmployees) {
                prepareStatement.setString(1, row.employeeID);
                prepareStatement.setString(2, row.lastnameRUS);
                prepareStatement.setString(3, row.nameRUS);
                prepareStatement.setString(4, row.middleNameRUS);
                prepareStatement.setString(5, row.tabNom);
                prepareStatement.setString(6, row.jobTitleRUS);
                prepareStatement.setString(7, row.loginName);
                prepareStatement.setString(8, row.email);
                prepareStatement.setString(9, row.ipPhone);
                prepareStatement.setString(10, row.workPhone);
                prepareStatement.setString(11, row.typeWork);
                prepareStatement.setString(12, row.departmentID);
                prepareStatement.setString(13, row.managerID);
                prepareStatement.setString(14, row.userSID);
                prepareStatement.setString(15, row.dateCreate);
                prepareStatement.addBatch();
            }
            if (!rowEmployees.isEmpty()) {
                prepareStatement.executeBatch();
                logger.info("   Unloaded in {}: {} {}", DEL_MAIN_TABLE_EMPLOYEES, rowEmployees.size(), ROWS);
            }
        }
    }

    private static void insertRowsToDelDepartments(Connection conn, List<RowDepartments> rowDepartments) throws SQLException {
        String insertSQL = "INSERT INTO " + DEL_MAIN_TABLE_DEPARTMENTS + " (DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE, DATE_DELETE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (RowDepartments row : rowDepartments) {
                pstmt.setString(1, row.departmentID);
                pstmt.setString(2, row.name);
                pstmt.setString(3, row.managerID);
                pstmt.setString(4, row.managerLoginName);
                pstmt.setString(5, row.parentID);
                pstmt.setString(6, row.typeName);
                pstmt.setString(7, row.code);
                pstmt.setString(8, row.bDate);
                pstmt.setString(9, row.eDate);
                pstmt.setString(10, row.dataIntegration);
                pstmt.setString(11, row.eDoc);
                pstmt.setString(12, row.idDeptOwn);
                pstmt.setString(13, row.dateCreate);
                pstmt.addBatch();
            }
            if (!rowDepartments.isEmpty()) {
                pstmt.executeBatch();
                logger.info("   Unloaded in {}: {} {}", DEL_MAIN_TABLE_DEPARTMENTS, rowDepartments.size(), ROWS);
            }
        }
    }

    private static void deleteRowsFromMainEmployees(Connection conn, List<RowEmployee> rowEmployees) throws SQLException {
        String deleteSQL = "DELETE FROM " + MAIN_TABLE_EMPLOYEES + " WHERE EMPLOYEEID = ?";
        try (PreparedStatement prepareStatement = conn.prepareStatement(deleteSQL)) {
            for (RowEmployee row : rowEmployees) {
                prepareStatement.setString(1, row.employeeID);
                prepareStatement.addBatch();
            }
            if (!rowEmployees.isEmpty()) {
                prepareStatement.executeBatch();
                logger.info("   Delete from {}: {} {}", MAIN_TABLE_EMPLOYEES, rowEmployees.size(), ROWS);
            }
        }
    }

    @SuppressWarnings("java:S2077")
    private static void deleteRowsFromMainDepartments(Connection conn, List<RowDepartments> rowDepartments) throws SQLException {
        String deleteSQL = "DELETE FROM " + MAIN_TABLE_DEPARTMENTS + " WHERE DepartmentID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            for (RowDepartments row : rowDepartments) {
                pstmt.setString(1, row.departmentID);
                pstmt.addBatch();
            }
            if (!rowDepartments.isEmpty()) {
                pstmt.executeBatch();
                // БАГ ИСПРАВЛЕН: тут был MAIN_TABLE_EMPLOYEES вместо MAIN_TABLE_DEPARTMENTS
                logger.info("   Delete from {} : {} rows.", MAIN_TABLE_DEPARTMENTS, rowDepartments.size());
            }
        }
    }

    private static void safeSleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void logSyncExecution(Runnable syncTask) {
        LocalDateTime dStart = LocalDateTime.now();
        logger.info("Start: {}", dStart);

        syncTask.run();

        LocalDateTime dStop = LocalDateTime.now();
        logger.info("Stop {}", dStop);

        Duration duration = Duration.between(dStart, dStop);
        logger.info("Duration of unloading time {} seconds", duration.getSeconds());
        logger.info("===========================================================================================");
    }
}