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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    // Данные для подключения к базе данных
    private static String DB_URL;
    private static String USER;
    private static String PASS;

    // Названия таблиц
    private static final String MAIN_TABLE_EMPLOYEES = "dEmployes";
    private static final String DEL_MAIN_TABLE_EMPLOYEES = "del_Employes"; // Для удаляемых строк
    private static final String MAIN_TABLE_DEPARTMENTS = "dDepartments";
    private static final String DEL_MAIN_TABLE_DEPARTMENTS = "del_Departments"; // Для удаляемых строк
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

        RowEmploee(String EMPLOYEEID, String LASTNAMERUS, String NAMERUS, String MIDDLENAMERUS, String TABNOM, String JOBTITLERUS, String LOGINNAME, String email, String IPPHONE, String WORKPHONE, String TYPE_WORK, String DEPARTMENTID, String MANAGERID, String USER_SID, String date_create) {
            this.EMPLOYEEID = EMPLOYEEID;
            this.LASTNAMERUS = LASTNAMERUS;
            this.NAMERUS = NAMERUS;
            this.MIDDLENAMERUS = MIDDLENAMERUS;
            this.TABNOM = TABNOM;
            this.JOBTITLERUS = JOBTITLERUS;
            this.LOGINNAME = LOGINNAME;
            this.email = email;
            this.IPPHONE = IPPHONE;
            this.WORKPHONE = WORKPHONE;
            this.TYPE_WORK = TYPE_WORK;
            this.DEPARTMENTID = DEPARTMENTID;
            this.MANAGERID = MANAGERID;
            this.USER_SID = USER_SID;
            this.date_create = date_create;
        }

        // MD5-хэш уникальных полей
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

        RowDepartments(String DepartmentID, String NAME, String MANAGERID, String MANAGERLOGINNAME, String PARENTID, String TYPE_NAME, String CODE, String B_DATE, String E_DATE, String DATA_INTEG, String E_DOC, String ID_DEPT_OWN, String date_create) {
            this.DepartmentID = DepartmentID;
            this.NAME = NAME;
            this.MANAGERID = MANAGERID;
            this.MANAGERLOGINNAME = MANAGERLOGINNAME;
            this.PARENTID = PARENTID;
            this.TYPE_NAME = TYPE_NAME;
            this.CODE = CODE;
            this.B_DATE = B_DATE;
            this.E_DATE = E_DATE;
            this.DATA_INTEG = DATA_INTEG;
            this.E_DOC = E_DOC;
            this.ID_DEPT_OWN = ID_DEPT_OWN;
            this.date_create = date_create;
        }

        // MD5-хэш уникальных полей
        String getMD5() throws NoSuchAlgorithmException {
//            String data = DepartmentID + "|" + NAME + "|" + MANAGERID + "|" + MANAGERLOGINNAME + "|" + PARENTID + "|" + TYPE_NAME + "|" + CODE + "|" + B_DATE + "|" + E_DATE + "|" + DATA_INTEG + "|" + E_DOC + "|" + ID_DEPT_OWN;
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
//            return Objects.hash(DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN);
            return Objects.hash(DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, E_DOC, ID_DEPT_OWN);
        }
    }
    public static void main(String[] args) throws IOException {
        currentDir = System.getProperty("user.dir");
//        String configPath = currentDir + "\\src\\main\\java\\config\\settingSynDictionary.txt";
        String configPath = currentDir + "\\config\\settingSynDictionary.txt";
        DB_URL = "jdbc:sqlserver://docprod\\sqlprod;databaseName=GAZ;encrypt=false;trustServerCertificate=true;";
        USER = "DVSQL";
        PASS = "DV_Cthdbc14@";

        for (String arg : args) {
            if (arg.startsWith("config.path=")) {
                configPath = arg.substring("config.path=".length());
            }
        }

        ConfigLoader configLoader = new ConfigLoader(configPath);
        DB_URL = configLoader.getProperty("DB_URL");
        USER = configLoader.getProperty("USER");
        PASS =  configLoader.getProperty("PASS");
        int departmentDefHour = Integer.parseInt(configLoader.getProperty("departmentDefHour"));
        int departmentDefMinutes = Integer.parseInt(configLoader.getProperty("departmentDefMinutes"));
        int departmentRestartHours = Integer.parseInt(configLoader.getProperty("departmentRestartHours"));

        int employeeDefHour = Integer.parseInt(configLoader.getProperty("employeeDefHour"));
        int employeeDefMinutes = Integer.parseInt(configLoader.getProperty("employeeDefMinutes"));
        int employeeRestartHours = Integer.parseInt(configLoader.getProperty("employeeRestartHours"));

        // Пример : Если нужен Запуск с 4:15 с периодичностью `periodRestartHour`= 2 часа : SyncEmploee(4,15,2);
        SyncDepartment(departmentDefHour, departmentDefMinutes, departmentRestartHours); // синхронизация справочника Подразделений (каждые 4 часа)
        SyncEmployee(employeeDefHour, employeeDefMinutes, employeeRestartHours); // синхронизация справочника Сотрудников (каждый час)

    }
    private static void SyncDepartment(int defHour, int defMinutes, Integer periodRestartHour) {
        int RestartHour ;
        if (periodRestartHour == null || periodRestartHour == 0) {
            RestartHour = 1;
        } else RestartHour = periodRestartHour;
        // Вычислить начальную задержку до следующего запуска в 5:00 или ближайшего 4-часового слота
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = calculateNextRunTime(now,defHour,defMinutes, RestartHour);
        Duration initialDelay = Duration.between(now, nextRun);
        long initialDelaySeconds = initialDelay.getSeconds();
        System.out.printf("\nПлановое время обновления Oracle (Departments): %s", nextRun);

        // Создать планировщик
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime dStart = LocalDateTime.now();
                System.out.println("\nСтарт : " + dStart);
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                    // Создать DEL_MAIN_TABLE, если не существует
                    createDelTableIfNotExists(conn, MAIN_TABLE_DEPARTMENTS, DEL_MAIN_TABLE_DEPARTMENTS);
                    // Шаг 1: Загрузить строки из main_table_Departments (dDepartments)
                    List<RowDepartments> mainRowDepartments = loadRowsFromLocalDepartments(conn);
                    System.out.println("Загружено из " + MAIN_TABLE_DEPARTMENTS.toUpperCase() + ": " + mainRowDepartments.size() + " строк");
                    // Шаг 2: Загрузить строки из Oracle (ОДИН ЗАПРОС)
                    List<RowDepartments> oracleRowDepartments = loadRowsFromOracleDepartments(conn);
                    System.out.println("Загружено из Oracle (" + "sl.doc_dpt_vw".toUpperCase() + "): " + oracleRowDepartments.size() + " строк");
                    // Шаг 3: Вычислить хэши из Oracle строк
                    Set<String> oracleHashes = new HashSet<>();
                    for (RowDepartments rowDepartment : oracleRowDepartments) {
                        oracleHashes.add(rowDepartment.getMD5());
                    }
                    System.out.println("Хэшей из Oracle: " + oracleHashes.size());
                    // Шаг 4: Определить строки для удаления
                    List<RowDepartments> rowsToDelete = new ArrayList<>();
                    for (RowDepartments rowDepartment : mainRowDepartments) {
                        String hash = rowDepartment.getMD5();
                        if (!oracleHashes.contains(hash)) {
                            rowsToDelete.add(rowDepartment);
                        }
                    }
                    System.out.println("Строк для удаления (Departments): " + rowsToDelete.size());
                    // Шаг 5: Выгрузить удаляемые строки в del_employes
                    insertRowsToDelDepartments(conn, rowsToDelete);
                    // Шаг 6: Удалить из main_table (dDepartment)
                    deleteRowsFromMainDepartments(conn, rowsToDelete);
                    // Шаг 7: Вставить новые строки из Oracle
                    Set<String> originalMainHashes = new HashSet<>();
                    for (RowDepartments rowDepartment : mainRowDepartments) {
                        originalMainHashes.add(rowDepartment.getMD5());
                    }
                    insertNewRowsDepartments(conn, oracleRowDepartments, originalMainHashes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LocalDateTime dStop = LocalDateTime.now();
                System.out.println("Стоп : " + dStop);
                Duration duration = Duration.between(dStart, dStop);
                long seconds = duration.getSeconds();
                System.out.printf("Время выгрузки %d секунд.\n", seconds);
//                System.out.println("Синхронизация завершена успешно!\nУдаляемые строки выгружены в " + DEL_MAIN_TABLE_DEPARTMENTS.toUpperCase() + ". В " + MAIN_TABLE_DEPARTMENTS.toUpperCase() + " остались только новые записи.\n");
//                System.out.printf("Для проверки подключитесь к базе MSSQL(%s). \nВыполните запросы:\n %s", "DocProd\\SQLPROD (GAZ)", "select * FROM dDepartments; - проверка данных актуальных пользователей.\n select * FROM del_Departments; - проверка удаленных данных. \n");
//                System.out.printf("Для проверки последних загруженных данных, добавить условие:\n %s", "select * FROM dDepartments where date_create=(SELECT MAX(date_create) FROM dDepartments); - записи с последнего обновления.\n select * FROM dDepartments where CAST(date_create AS DATE)=(SELECT MAX(CAST(date_create AS DATE)) FROM dDepartments); - записи с последнего обновления в течении дня.\n" );
                System.out.println("===========================================================================================");
            } catch (Exception e) {
                logger.error("Error in ConsumerServer.startProcessing.MSSQLConnection.deleteBinMoreSevenDay ", e);
            }
//            System.out.printf(AnsiColor.GREEN + "Следующее время обновления: %s" + AnsiColor.RESET, calculateNextRunTime(nextRun,defHour,defMinutes, RestartHour));
        }, initialDelaySeconds, RestartHour * 60 * 60, TimeUnit.SECONDS); // Каждый dRestartHour=4 часа
        // Для предотвращения завершения программы
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
    }
    private static void SyncEmployee(int defHour, int defMinutes, Integer periodRestartHour) {
        int RestartHour ;
        if (periodRestartHour == null || periodRestartHour == 0) {
            RestartHour = 1;
        } else RestartHour = periodRestartHour;
        // Вычислить начальную задержку до следующего запуска в 5:00 или ближайшего 4-часового слота
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = calculateNextRunTime(now,defHour,defMinutes, RestartHour);
        Duration initialDelay = Duration.between(now, nextRun);
        long initialDelaySeconds = initialDelay.getSeconds();
        System.out.printf("\nПлановое время обновления Oracle (Employees): %s", nextRun);

        // Создать планировщик
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime dStart = LocalDateTime.now();
                System.out.println("\nСтарт : " + dStart);
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                    // Создать DEL_MAIN_TABLE, если не существует
                    createDelTableIfNotExists(conn, MAIN_TABLE_EMPLOYEES, DEL_MAIN_TABLE_EMPLOYEES);
                    // Шаг 1: Загрузить строки из main_table (dEmployes)
                    List<RowEmploee> mainRowEmploees = loadRowsFromLocalEmployee(conn);
                    System.out.println("Загружено из " + MAIN_TABLE_EMPLOYEES.toUpperCase() + ": " + mainRowEmploees.size() + " строк");
                    // Шаг 2: Загрузить строки из Oracle (ОДИН ЗАПРОС)
                    List<RowEmploee> oracleRowEmployees = loadRowsFromOracleEmployee(conn);
                    System.out.println("Загружено из Oracle (" + "sl.doc_emp_vw".toUpperCase() + "): " + oracleRowEmployees.size() + " строк");
                    // Шаг 3: Вычислить хэши из Oracle строк
                    Set<String> oracleHashes = new HashSet<>();
                    for (RowEmploee rowEmploee : oracleRowEmployees) {
                        oracleHashes.add(rowEmploee.getMD5());
                    }
                    System.out.println("Хэшей из Oracle: " + oracleHashes.size());
                    // Шаг 4: Определить строки для удаления
                    List<RowEmploee> rowsToDelete = new ArrayList<>();
                    for (RowEmploee rowEmploee : mainRowEmploees) {
                        String hash = rowEmploee.getMD5();
                        if (!oracleHashes.contains(hash)) {
                            rowsToDelete.add(rowEmploee);
                        }
                    }
                    System.out.println("Строк для удаления: " + rowsToDelete.size());
                    // Шаг 5: Выгрузить удаляемые строки в del_employes
                    insertRowsToDelEmployees(conn, rowsToDelete);
                    // Шаг 6: Удалить из main_table (dEmployes)
                    deleteRowsFromMainEmployees(conn, rowsToDelete);
                    // Шаг 7: Вставить новые строки из Oracle
                    Set<String> originalMainHashes = new HashSet<>();
                    for (RowEmploee rowEmploee : mainRowEmploees) {
                        originalMainHashes.add(rowEmploee.getMD5());
                    }
                    insertNewRowsEmployees(conn, oracleRowEmployees, originalMainHashes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LocalDateTime dStop = LocalDateTime.now();
                System.out.println("Стоп : " + dStop);
                Duration duration = Duration.between(dStart, dStop);
                long seconds = duration.getSeconds();
                System.out.printf("Время выгрузки %d секунд.\n", seconds);
//                System.out.println("Синхронизация завершена успешно!\nУдаляемые строки выгружены в " + DEL_MAIN_TABLE_EMPLOYEES.toUpperCase() + ". В " + MAIN_TABLE_EMPLOYEES.toUpperCase() + " остались только новые записи. 😀✨\n");
//                System.out.println("Для проверки подключитесь к базе MSSQL. \nВыполните запросы (DocProd\\SQLPROD (GAZ)) :\n  select * FROM demployes; - проверка данных актуальных пользователей.\n  select * FROM del_main_table; - проверка удаленных данных. \n");
//                System.out.println("Для проверки последних загруженных данных, добавить условие:\n  select * FROM demployes where date_create=(SELECT MAX(date_create) FROM demployes); - записи с последнего обновления.\n  select * FROM demployes where CAST(date_create AS DATE)=(SELECT MAX(CAST(date_create AS DATE)) FROM demployes); - записи с последнего обновления в течении дня.\n");
                System.out.println("===========================================================================================");
            } catch (Exception e) {
                logger.error("Error in ConsumerServer.startProcessing.MSSQLConnection.deleteBinMoreSevenDay ", e);
            }
        }, initialDelaySeconds, RestartHour * 60 * 60, TimeUnit.SECONDS); // Каждый dRestartHour=4 часа
        // Для предотвращения завершения программы
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
    }

    // Метод для вычисления следующего времени запуска
    private static LocalDateTime calculateNextRunTime(LocalDateTime now, int hour, int minute, int restartHours) {
        // Базовое время: текущий день в hour:minute
        LocalDateTime baseTime = now.toLocalDate().atTime(hour, minute, 0, 0);

        if (now.isBefore(baseTime)) {
            // Если сейчас до базового времени, следующий запуск в базовое время
            return baseTime;
        } else {
            // Вычислить разницу в часах от базового времени до now
            long hoursSinceBase = Duration.between(baseTime, now).toHours();

            // Найти следующий слот: базовое + (floor(hoursSinceBase / restartHours) + 1) * restartHours
            long slotsPassed = (hoursSinceBase / restartHours) + 1;
            long nextHourOffset = slotsPassed * restartHours;

            // Следующее время: базовое + nextHourOffset часов
            // LocalDateTime автоматически обработает переход на следующий день
            return baseTime.plusHours(nextHourOffset);
        }
    }

    // Создать del_dEmployes, если не существует
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
        System.out.println("Выполняется запрос: " + query);
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
                String MANAGERID = rs.getString("MANAGERID");
                String USER_SID = rs.getString("USER_SID");
                String date_create = rs.getString("DATE_CREATE");
                rowEmploees.add(new RowEmploee(EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create));
            }
        }
        return rowEmploees;
    }
    private static List<RowDepartments> loadRowsFromLocalDepartments(Connection conn) throws SQLException {
        List<RowDepartments> rowDepartments = new ArrayList<>();
        String query = "SELECT DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE  FROM " + App.MAIN_TABLE_DEPARTMENTS;
        System.out.println("Выполняется запрос: " + query);
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String DepartmentID = rs.getString("DepartmentID");
                String NAME = rs.getString("NAME");
                String MANAGERID = rs.getString("MANAGERID");
                String MANAGERLOGINNAME = rs.getString("MANAGERLOGINNAME");
                String PARENTID = rs.getString("PARENTID");
                String TYPE_NAME = rs.getString("TYPE_NAME");
                String CODE = rs.getString("CODE");
                String B_DATE = rs.getString("B_DATE");
                String E_DATE = rs.getString("E_DATE");
                String DATA_INTEG = rs.getString("DATA_INTEG");
                String E_DOC = rs.getString("E_DOC");
                String ID_DEPT_OWN = rs.getString("ID_DEPT_OWN");
                String date_create = rs.getString("DATE_CREATE");
                rowDepartments.add(new RowDepartments(DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, date_create));
            }
        }
        return rowDepartments;
    }
    // Загрузить строки из Oracle (ОДИН ЗАПРОС)
    private static List<RowEmploee> loadRowsFromOracleEmployee(Connection conn) throws SQLException {
        List<RowEmploee> rowEmploees = new ArrayList<>();
        String query = "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID, GETDATE() AS DATE_CREATE FROM OPENQUERY(oraclecis, 'SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID FROM SL.DOC_EMP_VW')";
        System.out.println("Выполняется запрос к Oracle (Employees): " + query);
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
                String MANAGERID = rs.getString("MANAGERID");
                String USER_SID = rs.getString("USER_SID");
                String date_create = rs.getString("DATE_CREATE");
                rowEmploees.add(new RowEmploee(EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create));
            }
        }
        return rowEmploees;
    }
    private static List<RowDepartments> loadRowsFromOracleDepartments(Connection conn) throws SQLException {
        List<RowDepartments> rowDepartments = new ArrayList<>();
        String query = "SELECT ID as DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, GETDATE() AS DATE_CREATE FROM OPENQUERY(oraclecis, 'SELECT ID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN FROM sl.doc_dpt_vw')";
        System.out.println("Выполняется запрос к Oracle (Departments): " + query);
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String DepartmentID = rs.getString("DepartmentID");
                String NAME = rs.getString("NAME");
                String MANAGERID = rs.getString("MANAGERID");
                String MANAGERLOGINNAME = rs.getString("MANAGERLOGINNAME");
                String PARENTID = rs.getString("PARENTID");
                String TYPE_NAME = rs.getString("TYPE_NAME");
                String CODE = rs.getString("CODE");
                String B_DATE = rs.getString("B_DATE");
                String E_DATE = rs.getString("E_DATE");
                String DATA_INTEG = rs.getString("DATA_INTEG");
                String E_DOC = rs.getString("E_DOC");
                String ID_DEPT_OWN = rs.getString("ID_DEPT_OWN");
                String date_create = rs.getString("DATE_CREATE");
                rowDepartments.add(new RowDepartments(DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, date_create));
            }
        }
        return rowDepartments;
    }
    // Вставить новые строки в main_table
    private static void insertNewRowsEmployees(Connection conn, List<RowEmploee> oracleRowEmploees, Set<String> mainHashes) throws SQLException, NoSuchAlgorithmException {
        String insertSQL = "INSERT INTO " + MAIN_TABLE_EMPLOYEES + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, DATE_CREATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            int inserted = 0;
            for (RowEmploee rowEmploee : oracleRowEmploees) {
                String hash = rowEmploee.getMD5();
                if (!mainHashes.contains(hash)) {
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
                System.out.println("Вставлено новых строк: " + inserted);
            }
        }
    }
    private static void insertNewRowsDepartments(Connection conn, List<RowDepartments> oracleDepartments, Set<String> mainHashes) throws SQLException, NoSuchAlgorithmException {
        String insertSQL = "INSERT INTO " + MAIN_TABLE_DEPARTMENTS + " (DepartmentID, NAME, MANAGERID, MANAGERLOGINNAME, PARENTID, TYPE_NAME, CODE, B_DATE, E_DATE, DATA_INTEG, E_DOC, ID_DEPT_OWN, DATE_CREATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            int inserted = 0;
            for (RowDepartments rowDepartment : oracleDepartments) {
                String hash = rowDepartment.getMD5();
                if (!mainHashes.contains(hash)) {
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
                System.out.println("Вставлено новых строк: " + inserted);
            }
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
                System.out.println("Выгружено в " + DEL_MAIN_TABLE_EMPLOYEES + ": " + rowEmploees.size() + " строк.");
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
                System.out.println("Выгружено в " + DEL_MAIN_TABLE_DEPARTMENTS + ": " + rowDepartments.size() + " строк.");
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
                System.out.println("Удалено из " + MAIN_TABLE_EMPLOYEES + ": " + rowEmployees.size() + " строк.");
            }
        }
    }
    private static void deleteRowsFromMainDepartments(Connection conn, List<RowDepartments> rowDepartments) throws SQLException {
        String deleteSQL = "DELETE FROM " + MAIN_TABLE_DEPARTMENTS + " WHERE DepartmentID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            for (RowDepartments rowDepartment : rowDepartments) {
                pstmt.setString(1, rowDepartment.DepartmentID);
                pstmt.addBatch();
            }
            if (!rowDepartments.isEmpty()) {
                pstmt.executeBatch();
                System.out.println("Удалено из " + MAIN_TABLE_DEPARTMENTS + ": " + rowDepartments.size() + " строк.");
            }
        }
    }
}