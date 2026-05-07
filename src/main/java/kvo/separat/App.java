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
//    @Value("${server.port}")
//    private static int port;
//    @Value("${server.address}")
//    private static String server_address;
//    private static final int port = 8080;
//    private static final String server_address = "W17-007926";
//    private static HTTPServer server;
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


    // Класс для представления строки таблицы (используем EMPLOYEEID как ключ)
    static class RowEmploee {
        String EMPLOYEEID; // Уникальный ID (строка)
        String LASTNAMERUS;
        String NAMERUS;
        String MIDDLENAMERUS;
        String TABNOM;
        String JOBTITLERUS;
        String LOGINNAME;
        String email;
        String IPPHONE;
        String WORKPHONE;
        String TYPE_WORK;
        String DEPARTMENTID;
        String MANAGERID;
        String USER_SID;
        String date_create;

        @SuppressWarnings("java:S107")
        RowEmploee(String employeeID, String lastnameRUS, String nameRUS, String middleNameRUS, String tabNom, String jobTitleRUS, String loginName, String email, String ipPhone, String workPhone, String typeWork, String departmentID, String managerID, String userSID, String dateCreate) {
            this.EMPLOYEEID = employeeID;
            this.LASTNAMERUS = lastnameRUS;
            this.NAMERUS = nameRUS;
            this.MIDDLENAMERUS = middleNameRUS;
            this.TABNOM = tabNom;
            this.JOBTITLERUS = jobTitleRUS;
            this.LOGINNAME = loginName;
            this.email = email;
            this.IPPHONE = ipPhone;
            this.WORKPHONE = workPhone;
            this.TYPE_WORK = typeWork;
            this.DEPARTMENTID = departmentID;
            this.MANAGERID = managerID;
            this.USER_SID = userSID;
            this.date_create = dateCreate;
        }

        // MD5-хэш уникальных полей
        @SuppressWarnings("java:S4790")
        String getMD5() throws NoSuchAlgorithmException {
            String data = EMPLOYEEID + "|" + LASTNAMERUS + "|" + NAMERUS + "|" + MIDDLENAMERUS + "|" + TABNOM + "|" + JOBTITLERUS + "|" + LOGINNAME + "|" + email + "|" + IPPHONE + "|" + WORKPHONE + "|" + TYPE_WORK + "|" + DEPARTMENTID + "|" + MANAGERID + "|" + USER_SID;
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
            RowEmploee rowEmploee = (RowEmploee) obj;
            return Objects.equals(EMPLOYEEID, rowEmploee.EMPLOYEEID) &&
                    Objects.equals(LASTNAMERUS, rowEmploee.LASTNAMERUS) &&
                    Objects.equals(NAMERUS, rowEmploee.NAMERUS) &&
                    Objects.equals(MIDDLENAMERUS, rowEmploee.MIDDLENAMERUS) &&
                    Objects.equals(TABNOM, rowEmploee.TABNOM) &&
                    Objects.equals(JOBTITLERUS, rowEmploee.JOBTITLERUS) &&
                    Objects.equals(LOGINNAME, rowEmploee.LOGINNAME) &&
                    Objects.equals(email, rowEmploee.email) &&
                    Objects.equals(IPPHONE, rowEmploee.IPPHONE) &&
                    Objects.equals(WORKPHONE, rowEmploee.WORKPHONE) &&
                    Objects.equals(TYPE_WORK, rowEmploee.TYPE_WORK) &&
                    Objects.equals(DEPARTMENTID, rowEmploee.DEPARTMENTID) &&
                    Objects.equals(MANAGERID, rowEmploee.MANAGERID) &&
                    Objects.equals(USER_SID, rowEmploee.USER_SID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID);
        }
    }
    static class RowDepartments {
        String DepartmentID;
        String NAME;
        String MANAGERID;
        String MANAGERLOGINNAME;
        String PARENTID;
        String TYPE_NAME;
        String CODE;
        String B_DATE;
        String E_DATE;
        String DATA_INTEG;
        String E_DOC;
        String ID_DEPT_OWN;
        String date_create;

        @SuppressWarnings("java:S107")
        RowDepartments(String departmentID, String name, String managerID, String managerLoginName, String parentID, String typeName, String code, String bDate, String eDate, String dataIntegration, String eDoc, String idDeptOwn, String dateCreate) {
            this.DepartmentID = departmentID;
            this.NAME = name;
            this.MANAGERID = managerID;
            this.MANAGERLOGINNAME = managerLoginName;
            this.PARENTID = parentID;
            this.TYPE_NAME = typeName;
            this.CODE = code;
            this.B_DATE = bDate;
            this.E_DATE = eDate;
            this.DATA_INTEG = dataIntegration;
            this.E_DOC = eDoc;
            this.ID_DEPT_OWN = idDeptOwn;
            this.date_create = dateCreate;
        }

        // MD5-хэш уникальных полей
        @SuppressWarnings("java:S4790")
        String getMD5() throws NoSuchAlgorithmException {
            String data = DepartmentID + "|" + NAME + "|" + MANAGERID + "|" + MANAGERLOGINNAME + "|" + PARENTID + "|" + TYPE_NAME + "|" + CODE + "|" + B_DATE + "|" + E_DATE + "|" + E_DOC + "|" + ID_DEPT_OWN;
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
            RowDepartments RowDepartment = (RowDepartments) obj;
            return Objects.equals(DepartmentID, RowDepartment.DepartmentID) &&
                    Objects.equals(NAME, RowDepartment.NAME) &&
                    Objects.equals(MANAGERID, RowDepartment.MANAGERID) &&
                    Objects.equals(MANAGERLOGINNAME, RowDepartment.MANAGERLOGINNAME) &&
                    Objects.equals(PARENTID, RowDepartment.PARENTID) &&
                    Objects.equals(TYPE_NAME, RowDepartment.TYPE_NAME) &&
                    Objects.equals(CODE, RowDepartment.CODE) &&
                    Objects.equals(B_DATE, RowDepartment.B_DATE) &&
                    Objects.equals(E_DATE, RowDepartment.E_DATE) &&
                    Objects.equals(DATA_INTEG, RowDepartment.DATA_INTEG) &&
                    Objects.equals(E_DOC, RowDepartment.E_DOC) &&
                    Objects.equals(ID_DEPT_OWN, RowDepartment.ID_DEPT_OWN);
        }
        @Override
        public int hashCode() {
            return Objects.hash(DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, E_DOC, ID_DEPT_OWN);
        }
    }
    public static void main(String[] args) throws IOException {
        // Инициализация мониторинга
        initializeMonitoring();
        // Добавляем обработчик завершения работы приложения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (jvmGcMetrics != null) {
                jvmGcMetrics.close();
            }
        }));

        currentDir = System.getProperty("user.dir");
        String configPath = currentDir + "\\src\\main\\java\\config\\settingSynDictionary.txt";
        String syncDic = "department";

        for (String arg : args) {
            if (arg.startsWith("config.path=")) {
                configPath = arg.substring("config.path=".length());
            }
            if (arg.startsWith("dictionary=")) {
                syncDic = arg.substring("dictionary=".length());
            }
        }

        ConfigLoader configLoader = new ConfigLoader(configPath);
        dbUrl = configLoader.getProperty("DB_URL");
        user = configLoader.getProperty("USER");
        pass = configLoader.getProperty("PASS");
//        int departmentDefHour = Integer.parseInt(configLoader.getProperty("departmentDefHour"));
//        int departmentDefMinutes = Integer.parseInt(configLoader.getProperty("departmentDefMinutes"));
//        int departmentRestartHours = Integer.parseInt(configLoader.getProperty("departmentRestartHours"));
//
//        int employeeDefHour = Integer.parseInt(configLoader.getProperty("employeeDefHour"));
//        int employeeDefMinutes = Integer.parseInt(configLoader.getProperty("employeeDefMinutes"));
//        int employeeRestartHours = Integer.parseInt(configLoader.getProperty("employeeRestartHours"));

        // Запуск синхронизации
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
            // Создание Prometheus registry
            registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

            // Инициализация метрик
            employeeSyncCounter = Counter.builder("employee_sync_total")
                    .description("Total number of employee synchronizations")
                    .tag(APPLICATION,SYNCAPP)
                    .register(registry);

            departmentSyncCounter = Counter.builder("department_sync_total")
                    .description("Total number of department synchronizations")
                    .tag(APPLICATION,SYNCAPP)
                    .register(registry);

            employeeSyncErrorCounter = Counter.builder("employee_sync_errors_total")
                    .description("Total number of employee synchronization errors")
                    .tag(APPLICATION,SYNCAPP)
                    .register(registry);

            departmentSyncErrorCounter = Counter.builder("department_sync_errors_total")
                    .description("Total number of department synchronization errors")
                    .tag(APPLICATION,"sync-app")
                    .register(registry);

            employeeSyncTimer = Timer.builder("employee_sync_duration_seconds")
                    .description("Employee synchronization duration in seconds")
                    .tag(APPLICATION,SYNCAPP)
                    .register(registry);

            departmentSyncTimer = Timer.builder("department_sync_duration_seconds")
                    .description("Department synchronization duration in seconds")
                    .tag(APPLICATION,SYNCAPP)
                    .register(registry);

            rowsProcessedCounter = Counter.builder("rows_processed_total")
                    .description("Total number of rows processed")
                    .tag(APPLICATION,SYNCAPP)
                    .register(registry);

            rowsInsertedCounter = Counter.builder("rows_inserted_total")
                    .description("Total number of rows inserted")
                    .tag(APPLICATION,SYNCAPP)
                    .register(registry);

            rowsDeletedCounter = Counter.builder("rows_deleted_total")
                    .description("Total number of rows deleted")
                    .tag(APPLICATION,SYNCAPP)
                    .register(registry);

            // Биндеры для мониторинга JVM
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

//            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
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
                for (RowDepartments rowDepartment : oracleRowDepartments) {
                    oracleHashes.add(rowDepartment.getMD5());
                }

                List<RowDepartments> rowsToDelete = new ArrayList<>();
                for (RowDepartments rowDepartment : mainRowDepartments) {
                    String hash = rowDepartment.getMD5();
                    if (!oracleHashes.contains(hash)) {
                        rowsToDelete.add(rowDepartment);
                    }
                }

                insertRowsToDelDepartments(conn, rowsToDelete);
                deleteRowsFromMainDepartments(conn, rowsToDelete);
                rowsDeletedCounter.increment(rowsToDelete.size());

                Set<String> originalMainHashes = new HashSet<>();
                for (RowDepartments rowDepartment : mainRowDepartments) {
                    originalMainHashes.add(rowDepartment.getMD5());
                }

                int inserted = insertNewRowsDepartments(conn, oracleRowDepartments, originalMainHashes);
                rowsInsertedCounter.increment(inserted);

            }
//            catch (Exception e) {
//                departmentSyncErrorCounter.increment();
//                logger.error("Error during department synchronization", e);
//            }

//            LocalDateTime dStop = LocalDateTime.now();
//            logger.info("Stop {}", dStop);
//            Duration duration = Duration.between(dStart, dStop);
//            long seconds = duration.getSeconds();
//            logger.info("Duration of unloading time {} seconds.", seconds);
//            logger.info("===========================================================================================");

        } catch (Exception e) {
            departmentSyncErrorCounter.increment();
            logger.error("Error in department synchronization", e);
        } finally {
            sample.stop(departmentSyncTimer);
        }
    }

//    private static void SyncEmployee(int defHour, int defMinutes, Integer periodRestartHour) {
//        int RestartHour = (periodRestartHour == null || periodRestartHour == 0) ? 1 : periodRestartHour;
//
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime nextRun = calculateNextRunTime(now, defHour, defMinutes, RestartHour);
//        Duration initialDelay = Duration.between(now, nextRun);
//        long initialDelaySeconds = initialDelay.getSeconds();
//
//        System.out.printf("\nПлановое время обновления Oracle (Employees): %s", nextRun);
//
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        scheduler.scheduleAtFixedRate(App::syncEmployees, initialDelaySeconds, RestartHour * 60 * 60, TimeUnit.SECONDS);
//
//        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
//    }

    private static void syncEmployees() {
        Timer.Sample sample = Timer.start(registry);
        employeeSyncCounter.increment();

        try {
//            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(dbUrl, user, pass)) {
                createDelTableIfNotExists(conn, MAIN_TABLE_EMPLOYEES, DEL_MAIN_TABLE_EMPLOYEES);

                List<RowEmploee> mainRowEmploees = loadRowsFromLocalEmployee(conn);
                String mainTableEmployeesUp = MAIN_TABLE_EMPLOYEES.toUpperCase();
                logger.info("Downloaded from {}: {} {}", mainTableEmployeesUp, mainRowEmploees.size(), ROWS);

                List<RowEmploee> oracleRowEmployees = loadRowsFromOracleEmployee(conn);
                logger.info("Download from Oracle ({}): {} {}", "sl.doc_emp_vw".toUpperCase(), oracleRowEmployees.size(), ROWS);

                rowsProcessedCounter.increment(oracleRowEmployees.size());

                Set<String> oracleHashes = new HashSet<>();
                for (RowEmploee rowEmploee : oracleRowEmployees) {
                    oracleHashes.add(rowEmploee.getMD5());
                }

                List<RowEmploee> rowsToDelete = new ArrayList<>();
                for (RowEmploee rowEmploee : mainRowEmploees) {
                    String hash = rowEmploee.getMD5();
                    if (!oracleHashes.contains(hash)) {
                        rowsToDelete.add(rowEmploee);
                    }
                }

                insertRowsToDelEmployees(conn, rowsToDelete);
                deleteRowsFromMainEmployees(conn, rowsToDelete);
                rowsDeletedCounter.increment(rowsToDelete.size());

                Set<String> originalMainHashes = new HashSet<>();
                for (RowEmploee rowEmploee : mainRowEmploees) {
                    originalMainHashes.add(rowEmploee.getMD5());
                }

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

    // Метод для вычисления следующего времени запуска
//    private static LocalDateTime calculateNextRunTime(LocalDateTime now, int hour, int minute, int restartHours) {
//        // Базовое время: текущий день в hour:minute
//        LocalDateTime baseTime = now.toLocalDate().atTime(hour, minute, 0, 0);
//
//        if (now.isBefore(baseTime)) {
//            // Если сейчас до базового времени, следующий запуск в базовое время
//            return baseTime;
//        } else {
//            // Вычислить разницу в часах от базового времени до now
//            long hoursSinceBase = Duration.between(baseTime, now).toHours();
//
//            // Найти следующий слот: базовое + (floor(hoursSinceBase / restartHours) + 1) * restartHours
//            long slotsPassed = (hoursSinceBase / restartHours) + 1;
//            long nextHourOffset = slotsPassed * restartHours;
//
//            // Следующее время: базовое + nextHourOffset часов
//            // LocalDateTime автоматически обработает переход на следующий день
//            return baseTime.plusHours(nextHourOffset);
//        }
//    }

    // Создать del_dEmployes, если не существует
    @SuppressWarnings("java:S2077")
    private static void createDelTableIfNotExists(Connection conn, String MAINTABLE, String NEWTABLE) throws SQLException {
        String sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name = '" + NEWTABLE + "' AND xtype='U') BEGIN " +
                "SELECT TOP 0 * INTO " + NEWTABLE + " FROM " + MAINTABLE + "; " +
                "ALTER TABLE " + NEWTABLE + " ADD date_delete DATETIME DEFAULT GETDATE(); " +
                "END";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            if (!e.getSQLState().equals("42S01") && !e.getMessage().contains("already exists")) {
                throw e;
            }
        }
    }

    // Загрузить строки из локальной таблицы
    private static List<RowEmploee> loadRowsFromLocalEmployee(Connection conn) throws SQLException {
        List<RowEmploee> rowEmploees = new ArrayList<>();
        String query = "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID, DATE_CREATE FROM " + App.MAIN_TABLE_EMPLOYEES;
        logger.info(">>Running query : " + App.MAIN_TABLE_EMPLOYEES);
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String employeeID = rs.getString("EMPLOYEEID");
                String lastnameRUS = rs.getString("LASTNAMERUS");
                String nameRUS = rs.getString("NAMERUS");
                String middleNameRUS = rs.getString("MIDDLENAMERUS");
                String tabNom = rs.getString("TABNOM");
                String jobTitleRUS = rs.getString("JOBTITLERUS");
                String loginName = rs.getString("LOGINNAME");
                String email = rs.getString("EMAIL");
                String ipPhone = rs.getString("IPPHONE");
                String workPhone = rs.getString("WORKPHONE");
                String typeWork = rs.getString("TYPE_WORK");
                String departmentID = rs.getString("DEPARTMENTID");
                String managerId = rs.getString(MANAGER);
                String userSID = rs.getString("USER_SID");
                String dateCreate = rs.getString(DATECREATE);
                rowEmploees.add(new RowEmploee(employeeID, lastnameRUS, nameRUS, middleNameRUS, tabNom, jobTitleRUS, loginName, email, ipPhone, workPhone, typeWork, departmentID, managerId, userSID, dateCreate));
            }
        }
        return rowEmploees;
    }
    @SuppressWarnings("java:S2077")
    private static List<RowDepartments> loadRowsFromLocalDepartments(Connection conn) throws SQLException {
        List<RowDepartments> rowDepartments = new ArrayList<>();
        String query = "SELECT DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE  FROM " + App.MAIN_TABLE_DEPARTMENTS;
        logger.info(">>Request running : DB {}", App.MAIN_TABLE_DEPARTMENTS);
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String DepartmentID = rs.getString("DepartmentID");
                String NAME = rs.getString("NAME");
                String MANAGERID = rs.getString(MANAGER);
                String MANAGERLOGINNAME = rs.getString("MANAGERLOGINNAME");
                String PARENTID = rs.getString("PARENTID");
                String TYPE_NAME = rs.getString("TYPE_NAME");
                String CODE = rs.getString("CODE");
                String B_DATE = rs.getString("B_DATE");
                String E_DATE = rs.getString("E_DATE");
                String DATA_INTEG = rs.getString("DATA_INTEG");
                String E_DOC = rs.getString("E_DOC");
                String ID_DEPT_OWN = rs.getString("ID_DEPT_OWN");
                String date_create = rs.getString(DATECREATE);
                rowDepartments.add(new RowDepartments(DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, date_create));
            }
        }
        return rowDepartments;
    }
    // Загрузить строки из Oracle (ОДИН ЗАПРОС)
    private static List<RowEmploee> loadRowsFromOracleEmployee(Connection conn) throws SQLException {
        List<RowEmploee> rowEmploees = new ArrayList<>();
        String query = "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID, GETDATE() AS DATE_CREATE FROM OPENQUERY(oraclecis, 'SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID FROM SL.DOC_EMP_VW')";
        logger.info(">>Request running Oracle (Employees) : SL.DOC_EMP_VW");
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String EMPLOYEEID = rs.getString("EMPLOYEEID");
                String LASTNAMERUS = rs.getString("LASTNAMERUS");
                String NAMERUS = rs.getString("NAMERUS");
                String MIDDLENAMERUS = rs.getString("MIDDLENAMERUS");
                String TABNOM = rs.getString("TABNOM");
                String JOBTITLERUS = rs.getString("JOBTITLERUS");
                String LOGINNAME = rs.getString("LOGINNAME");
                String email = rs.getString("EMAIL");
                String IPPHONE = rs.getString("IPPHONE");
                String WORKPHONE = rs.getString("WORKPHONE");
                String TYPE_WORK = rs.getString("TYPE_WORK");
                String DEPARTMENTID = rs.getString("DEPARTMENTID");
                String MANAGERID = rs.getString(MANAGER);
                String USER_SID = rs.getString("USER_SID");
                String date_create = rs.getString(DATECREATE);
                rowEmploees.add(new RowEmploee(EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create));
            }
        }
        return rowEmploees;
    }
    private static List<RowDepartments> loadRowsFromOracleDepartments(Connection conn) throws SQLException {
        List<RowDepartments> rowDepartments = new ArrayList<>();
        String query = "SELECT ID as DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, GETDATE() AS DATE_CREATE FROM OPENQUERY(oraclecis, 'SELECT ID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN FROM sl.doc_dpt_vw')";
        logger.info(">>Request running Oracle (Departments): SL.DOC_DPT_VW");
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String DepartmentID = rs.getString("DepartmentID");
                String NAME = rs.getString("NAME");
                String MANAGERID = rs.getString(MANAGER);
                String MANAGERLOGINNAME = rs.getString("MANAGERLOGINNAME");
                String PARENTID = rs.getString("PARENTID");
                String TYPE_NAME = rs.getString("TYPE_NAME");
                String CODE = rs.getString("CODE");
                String B_DATE = rs.getString("B_DATE");
                String E_DATE = rs.getString("E_DATE");
                String DATA_INTEG = rs.getString("DATA_INTEG");
                String E_DOC = rs.getString("E_DOC");
                String ID_DEPT_OWN = rs.getString("ID_DEPT_OWN");
                String date_create = rs.getString(DATECREATE);
                rowDepartments.add(new RowDepartments(DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, date_create));
            }
        }
        return rowDepartments;
    }
    // Вставить новые строки в main_table
    @SuppressWarnings("java:S1192")
    private static int insertNewRowsEmployees(Connection conn, List<RowEmploee> oracleRowEmploees, Set<String> mainHashes) throws SQLException, NoSuchAlgorithmException {
        String insertSQL = "INSERT INTO " + MAIN_TABLE_EMPLOYEES + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, DATE_CREATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            int inserted = 0;
            for (RowEmploee rowEmploee : oracleRowEmploees) {
                String hash = rowEmploee.getMD5();
                if (!mainHashes.contains(hash)) {
                    // ... существующий код вставки
                    pstmt.setString(1, rowEmploee.EMPLOYEEID);
                    pstmt.setString(2, rowEmploee.LASTNAMERUS);
                    pstmt.setString(3, rowEmploee.NAMERUS);
                    pstmt.setString(4, rowEmploee.MIDDLENAMERUS);
                    pstmt.setString(5, rowEmploee.TABNOM);
                    pstmt.setString(6, rowEmploee.JOBTITLERUS);
                    pstmt.setString(7, rowEmploee.LOGINNAME);
                    pstmt.setString(8, rowEmploee.email);
                    pstmt.setString(9, rowEmploee.IPPHONE);
                    pstmt.setString(10, rowEmploee.WORKPHONE);
                    pstmt.setString(11, rowEmploee.TYPE_WORK);
                    pstmt.setString(12, rowEmploee.DEPARTMENTID);
                    pstmt.setString(13, rowEmploee.MANAGERID);
                    pstmt.setString(14, rowEmploee.USER_SID);
                    pstmt.setString(15, rowEmploee.date_create);
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
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            int inserted = 0;
            for (RowDepartments rowDepartment : oracleDepartments) {
                String hash = rowDepartment.getMD5();
                if (!mainHashes.contains(hash)) {
                    // ... существующий код вставки
                    pstmt.setString(1, rowDepartment.DepartmentID);
                    pstmt.setString(2, rowDepartment.NAME);
                    pstmt.setString(3, rowDepartment.MANAGERID);
                    pstmt.setString(4, rowDepartment.MANAGERLOGINNAME);
                    pstmt.setString(5, rowDepartment.PARENTID);
                    pstmt.setString(6, rowDepartment.TYPE_NAME);
                    pstmt.setString(7, rowDepartment.CODE);
                    pstmt.setString(8, rowDepartment.B_DATE);
                    pstmt.setString(9, rowDepartment.E_DATE);
                    pstmt.setString(10, rowDepartment.DATA_INTEG);
                    pstmt.setString(11, rowDepartment.E_DOC);
                    pstmt.setString(12, rowDepartment.ID_DEPT_OWN);
                    pstmt.setString(13, rowDepartment.date_create);
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

    // Вставить строки в del_main_table
    private static void insertRowsToDelEmployees(Connection conn, List<RowEmploee> rowEmploees) throws SQLException {
        String insertSQL = "INSERT INTO " + DEL_MAIN_TABLE_EMPLOYEES + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, DATE_CREATE, DATE_DELETE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (RowEmploee rowEmploee : rowEmploees) {
                pstmt.setString(1, rowEmploee.EMPLOYEEID);
                pstmt.setString(2, rowEmploee.LASTNAMERUS);
                pstmt.setString(3, rowEmploee.NAMERUS);
                pstmt.setString(4, rowEmploee.MIDDLENAMERUS);
                pstmt.setString(5, rowEmploee.TABNOM);
                pstmt.setString(6, rowEmploee.JOBTITLERUS);
                pstmt.setString(7, rowEmploee.LOGINNAME);
                pstmt.setString(8, rowEmploee.email);
                pstmt.setString(9, rowEmploee.IPPHONE);
                pstmt.setString(10, rowEmploee.WORKPHONE);
                pstmt.setString(11, rowEmploee.TYPE_WORK);
                pstmt.setString(12, rowEmploee.DEPARTMENTID);
                pstmt.setString(13, rowEmploee.MANAGERID);
                pstmt.setString(14, rowEmploee.USER_SID);
                pstmt.setString(15, rowEmploee.date_create);
                pstmt.addBatch();
            }
            if (!rowEmploees.isEmpty()) {
                pstmt.executeBatch();
                logger.info("   Unloaded in {}: {} {}",DEL_MAIN_TABLE_EMPLOYEES, rowEmploees.size(), ROWS);
            }
        }
    }
    private static void insertRowsToDelDepartments(Connection conn, List<RowDepartments> rowDepartments) throws SQLException {
        String insertSQL = "INSERT INTO " + DEL_MAIN_TABLE_DEPARTMENTS + " (DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE, DATE_DELETE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (RowDepartments rowDepartment : rowDepartments) {
                pstmt.setString(1, rowDepartment.DepartmentID);
                pstmt.setString(2, rowDepartment.NAME);
                pstmt.setString(3, rowDepartment.MANAGERID);
                pstmt.setString(4, rowDepartment.MANAGERLOGINNAME);
                pstmt.setString(5, rowDepartment.PARENTID);
                pstmt.setString(6, rowDepartment.TYPE_NAME);
                pstmt.setString(7, rowDepartment.CODE);
                pstmt.setString(8, rowDepartment.B_DATE);
                pstmt.setString(9, rowDepartment.E_DATE);
                pstmt.setString(10, rowDepartment.DATA_INTEG);
                pstmt.setString(11, rowDepartment.E_DOC);
                pstmt.setString(12, rowDepartment.ID_DEPT_OWN);
                pstmt.setString(13, rowDepartment.date_create);
                pstmt.addBatch();
            }
            if (!rowDepartments.isEmpty()) {
                pstmt.executeBatch();
                logger.info("   Unloaded in {}: {} {}", DEL_MAIN_TABLE_DEPARTMENTS, rowDepartments.size(), ROWS);
            }
        }
    }
    // Удалить строки из main_table
    private static void deleteRowsFromMainEmployees(Connection conn, List<RowEmploee> rowEmployees) throws SQLException {
        String deleteSQL = "DELETE FROM " + MAIN_TABLE_EMPLOYEES + " WHERE EMPLOYEEID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            for (RowEmploee rowEmploee : rowEmployees) {
                pstmt.setString(1, rowEmploee.EMPLOYEEID);
                pstmt.addBatch();
            }
            if (!rowEmployees.isEmpty()) {
                pstmt.executeBatch();
                logger.info("   Delete from {}: {} {}", MAIN_TABLE_EMPLOYEES, rowEmployees.size(), ROWS);
            }
        }
    }
    @SuppressWarnings("java:S2077")
    private static void deleteRowsFromMainDepartments(Connection conn, List<RowDepartments> rowDepartments) throws SQLException {
        String deleteSQL = "DELETE FROM " + MAIN_TABLE_DEPARTMENTS + " WHERE DepartmentID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            for (RowDepartments rowDepartment : rowDepartments) {
                pstmt.setString(1, rowDepartment.DepartmentID);
                pstmt.addBatch();
            }
            if (!rowDepartments.isEmpty()) {
                pstmt.executeBatch();
                logger.info("   Delete from {} : {} rows.",MAIN_TABLE_EMPLOYEES, rowDepartments.size());
            }
        }
    }
    private static void safeSleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
        }
    }
    private static void logSyncExecution(Runnable syncTask) {
        LocalDateTime  dStart = LocalDateTime.now();
        logger.info("Start: {}", dStart);

        syncTask.run();

        LocalDateTime  dStop = LocalDateTime.now();
        logger.info("Stop {}", LocalDateTime.now());

        Duration duration = Duration.between(dStart, dStop);
        long seconds = duration.getSeconds();
        logger.info("Duration of unloading time {} seconds", seconds);
        logger.info("===========================================================================================");
    }
}