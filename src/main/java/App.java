import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String DB_URL = "jdbc:sqlserver://docprod\\sqlprod;databaseName=GAZ;encrypt=false;trustServerCertificate=true;";
    private static final String USER = "DVSQL";
    private static final String PASS = "DV_Cthdbc14@";

    // Названия таблиц
    private static final String MAIN_TABLE = "dEmployes";
    private static final String DEL_MAIN_TABLE = "del_Employes"; // Для удаляемых строк

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

    public static void main(String[] args) {

        SyncEmploee(5,0,1); // Запуск с 4:15 с реиодичностью `periodRestartHour`= 2 часа : SyncEmploee(4,15,2);

    }

    private static void SyncEmploee(int defHour, int defMinutes, Integer periodRestartHour) {
        int RestartHour ;
        if (periodRestartHour == null || periodRestartHour == 0) {
            RestartHour = 1;
        } else RestartHour = periodRestartHour;
        // Вычислить начальную задержку до следующего запуска в 5:00 или ближайшего 4-часового слота
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = calculateNextRunTime(now,defHour,defMinutes, RestartHour);
        Duration initialDelay = Duration.between(now, nextRun);
        long initialDelaySeconds = initialDelay.getSeconds();
        System.out.printf(AnsiColor.GREEN + "Плановое время обновления: %s" + AnsiColor.RESET, nextRun);

        // Создать планировщик
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime dStart = LocalDateTime.now();
                System.out.println("\nСтарт : " + dStart);
                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                    // Создать DEL_MAIN_TABLE, если не существует
                    createDelTableIfNotExists(conn, MAIN_TABLE, DEL_MAIN_TABLE);
                    // Шаг 1: Загрузить строки из main_table (dEmployes)
                    List<RowEmploee> mainRowEmploees = loadRowsFromLocal(conn);
                    System.out.println(AnsiColor.BLUE + "Загружено из " + MAIN_TABLE.toUpperCase() + ": " + mainRowEmploees.size() + " строк" + AnsiColor.RESET);
                    // Шаг 2: Загрузить строки из Oracle (ОДИН ЗАПРОС)
                    List<RowEmploee> oracleRowEmploees = loadRowsFromOracle(conn);
                    System.out.println(AnsiColor.BLUE + "Загружено из Oracle (" + "sl.doc_emp_vw".toUpperCase() + "): " + oracleRowEmploees.size() + " строк" + AnsiColor.RESET);
                    // Шаг 3: Вычислить хэши из Oracle строк
                    Set<String> oracleHashes = new HashSet<>();
                    for (RowEmploee rowEmploee : oracleRowEmploees) {
                        oracleHashes.add(rowEmploee.getMD5());
                    }
                    System.out.println(AnsiColor.BLUE + "Хэшей из Oracle: " + oracleHashes.size() + AnsiColor.RESET);
                    // Шаг 4: Определить строки для удаления
                    List<RowEmploee> rowsToDelete = new ArrayList<>();
                    for (RowEmploee rowEmploee : mainRowEmploees) {
                        String hash = rowEmploee.getMD5();
                        if (!oracleHashes.contains(hash)) {
                            rowsToDelete.add(rowEmploee);
                        }
                    }
                    System.out.println(AnsiColor.BLUE + "Строк для удаления: " + rowsToDelete.size() + AnsiColor.RESET);
                    // Шаг 5: Выгрузить удаляемые строки в del_employes
                    insertRowsToDel(conn, rowsToDelete);
                    // Шаг 6: Удалить из main_table (dEmployes)
                    deleteRowsFromMain(conn, rowsToDelete);
                    // Шаг 7: Вставить новые строки из Oracle
                    Set<String> originalMainHashes = new HashSet<>();
                    for (RowEmploee rowEmploee : mainRowEmploees) {
                        originalMainHashes.add(rowEmploee.getMD5());
                    }
                    insertNewRows(conn, oracleRowEmploees, originalMainHashes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LocalDateTime dStop = LocalDateTime.now();
                System.out.println("Стоп : " + dStop);
                Duration duration = Duration.between(dStart, dStop);
                long seconds = duration.getSeconds();
                System.out.printf(String.valueOf(AnsiColor.GREEN) + AnsiColor.BOLD + "Время выгрузки %d секунд.\n" + AnsiColor.RESET, seconds);
                System.out.printf(String.valueOf(AnsiColor.GREEN) + AnsiColor.BOLD + "Синхронизация завершена успешно!" + AnsiColor.RESET + AnsiColor.BLUE + " \nУдаляемые строки выгружены в " + DEL_MAIN_TABLE.toUpperCase() + ". В " + MAIN_TABLE.toUpperCase() + " остались только новые записи. 😀✨\n" + AnsiColor.RESET);
                System.out.printf(AnsiColor.BLUE + "Для проверки подключитесь к базе MSSQL(%s). \nВыполните запросы:\n %s", "DocProd\\SQLPROD (GAZ)", "select * FROM demployes; - проверка данных актуальных пользователей.\n select * FROM del_main_table; - проверка удаленных данных. \n" + AnsiColor.RESET);
                System.out.printf(AnsiColor.BLUE + "Для проверки последних загруженных данных, добавить условие:\n %s", "select * FROM demployes where " + AnsiColor.RESET + AnsiColor.BRIGHT_BLUE + AnsiColor.UNDERLINE + "date_create=(SELECT MAX(date_create) FROM demployes)" + AnsiColor.RESET + AnsiColor.BLUE + "; - записи с последнего обновления.\n select * FROM demployes where " + AnsiColor.RESET + AnsiColor.BRIGHT_BLUE + AnsiColor.UNDERLINE + "CAST(date_create AS DATE)=(SELECT MAX(CAST(date_create AS DATE)) FROM demployes)" + AnsiColor.RESET + AnsiColor.BLUE + "; - записи с последнего обновления в течении дня.\n" + AnsiColor.RESET);
            } catch (Exception e) {
                logger.error("Error in ConsumerServer.startProcessing.MSSQLConnection.deleteBinMoreSevenDay ", e);
            }
            System.out.printf(AnsiColor.GREEN + "Следующее время обновления: %s" + AnsiColor.RESET, calculateNextRunTime(nextRun,defHour,defMinutes, RestartHour));
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
        String sql = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='" + NEWTABLE + "' AND xtype='U') BEGIN " +
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
    private static List<RowEmploee> loadRowsFromLocal(Connection conn) throws SQLException {
        List<RowEmploee> rowEmploees = new ArrayList<>();
        String query = "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID, DATE_CREATE FROM " + App.MAIN_TABLE;
        System.out.println(AnsiColor.BRIGHT_BLACK + "Выполняется запрос: " + query + AnsiColor.RESET);
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

    // Загрузить строки из Oracle (ОДИН ЗАПРОС)
    private static List<RowEmploee> loadRowsFromOracle(Connection conn) throws SQLException {
        List<RowEmploee> rowEmploees = new ArrayList<>();
        String query = "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID, GETDATE() AS DATE_CREATE FROM OPENQUERY(oraclecis, 'SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, LOGINNAME, USER_SID FROM SL.DOC_EMP_VW')";
        System.out.println(AnsiColor.BRIGHT_BLACK + "Выполняется запрос к Oracle: " + query + AnsiColor.RESET);
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

    // Вставить новые строки в main_table
    private static void insertNewRows(Connection conn, List<RowEmploee> oracleRowEmploees, Set<String> mainHashes) throws SQLException, NoSuchAlgorithmException {
        String insertSQL = "INSERT INTO " + MAIN_TABLE + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, DATE_CREATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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

    // Вставить строки в del_main_table
    private static void insertRowsToDel(Connection conn, List<RowEmploee> rowEmploees) throws SQLException {
        String insertSQL = "INSERT INTO " + DEL_MAIN_TABLE + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, EMAIL, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, DATE_CREATE, DATE_DELETE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
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
                System.out.println(AnsiColor.BRIGHT_BLACK + "Выгружено в " + DEL_MAIN_TABLE + ": " + rowEmploees.size() + " строк." + AnsiColor.RESET);
            }
        }
    }

    // Удалить строки из main_table
    private static void deleteRowsFromMain(Connection conn, List<RowEmploee> rowEmploees) throws SQLException {
        String deleteSQL = "DELETE FROM " + MAIN_TABLE + " WHERE EMPLOYEEID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            for (RowEmploee rowEmploee : rowEmploees) {
                pstmt.setString(1, rowEmploee.EMPLOYEEID);
                pstmt.addBatch();
            }
            if (!rowEmploees.isEmpty()) {
                pstmt.executeBatch();
                System.out.println(AnsiColor.BRIGHT_BLACK + "Удалено из " + MAIN_TABLE + ": " + rowEmploees.size() + " строк." + AnsiColor.RESET);
            }
        }
    }
}