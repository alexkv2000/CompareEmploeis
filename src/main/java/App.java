import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class App {

    // Данные для подключения к базе данных
    private static final String DB_URL = "jdbc:mysql://doc-test:3306/sql-cdv";
    private static final String USER = "root";
    private static final String PASS = "G@{MsEmeXl~Gsu8~{Kcv";

    // Названия таблиц
    private static final String MAIN_TABLE = "dEmployes";
    private static final String TEMP_TABLE = "temp_table";
    private static final String DEL_MAIN_TABLE = "del_main_table"; // Для удаляемых строк
    public enum AnsiColor {
        RESET("\033[0m"),

        // Текстовые цвета
        BLACK("\033[30m"),
        RED("\033[31m"),
        GREEN("\033[32m"),
        YELLOW("\033[33m"),
        BLUE("\033[34m"),
        PURPLE("\033[35m"),
        CYAN("\033[36m"),
        WHITE("\033[37m"),

        // Яркие цвета
        BRIGHT_BLACK("\033[90m"),
        BRIGHT_RED("\033[91m"),
        BRIGHT_GREEN("\033[92m"),
        BRIGHT_YELLOW("\033[93m"),
        BRIGHT_BLUE("\033[94m"),
        BRIGHT_PURPLE("\033[95m"),
        BRIGHT_CYAN("\033[96m"),
        BRIGHT_WHITE("\033[97m"),

        // Фоновые цвета
        BG_BLACK("\033[40m"),
        BG_RED("\033[41m"),
        BG_GREEN("\033[42m"),
        BG_YELLOW("\033[43m"),
        BG_BLUE("\033[44m"),
        BG_PURPLE("\033[45m"),
        BG_CYAN("\033[46m"),
        BG_WHITE("\033[47m"),

        // Яркие фоновые цвета
        BG_BRIGHT_BLACK("\033[100m"),
        BG_BRIGHT_RED("\033[101m"),
        BG_BRIGHT_GREEN("\033[102m"),
        BG_BRIGHT_YELLOW("\033[103m"),
        BG_BRIGHT_BLUE("\033[104m"),
        BG_BRIGHT_PURPLE("\033[105m"),
        BG_BRIGHT_CYAN("\033[106m"),
        BG_BRIGHT_WHITE("\033[107m"),

        // Стили
        BOLD("\033[1m"),
        UNDERLINE("\033[4m"),
        REVERSED("\033[7m");

        private final String code;

        AnsiColor(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }
    }

    // Класс для представления строки таблицы
    static class Row {
        int ID;
        String EMPLOYEEID; // ID
        String LASTNAMERUS; // Фамилия
        String NAMERUS; // Имя
        String MIDDLENAMERUS; // Отчество
        String TABNOM; // таб.номер
        String JOBTITLERUS; // Должность
        String LOGINNAME; // Логин
        String email; // почта
        String IPPHONE; // Телефон
        String WORKPHONE; // Рабочий телефон
        String TYPE_WORK; // `Основное` место работы
        String DEPARTMENTID; // Подразделение
        String MANAGERID; // Менеджер ID
        String USER_SID; // SID КИСУ
        String date_create; //

        Row(int ID, String EMPLOYEEID, String LASTNAMERUS, String NAMERUS, String MIDDLENAMERUS, String TABNOM, String JOBTITLERUS, String LOGINNAME, String email, String IPPHONE, String WORKPHONE, String TYPE_WORK, String DEPARTMENTID, String MANAGERID, String USER_SID, String date_create) {
            this.ID = ID;
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

        // Вычислить MD5-хэш уникальных полей (теперь включает date_create для полноты) Убрал!!!
        String getMD5() throws NoSuchAlgorithmException {
            String data = EMPLOYEEID + "|" + LASTNAMERUS + "|" + NAMERUS + "|" + MIDDLENAMERUS + "|" + TABNOM + "|" + JOBTITLERUS + "|" + LOGINNAME + "|" + email + "|" + IPPHONE + "|" + WORKPHONE + "|" + TYPE_WORK + "|" + DEPARTMENTID + "|" + MANAGERID + "|" + USER_SID; // Конкатенация с разделителем
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Row row = (Row) obj;
            return ID == row.ID
                    && Objects.equals(EMPLOYEEID, row.EMPLOYEEID)
                    && Objects.equals(LASTNAMERUS, row.LASTNAMERUS)
                    && Objects.equals(NAMERUS, row.NAMERUS)
                    && Objects.equals(MIDDLENAMERUS, row.MIDDLENAMERUS)
                    && Objects.equals(TABNOM, row.TABNOM)
                    && Objects.equals(JOBTITLERUS, row.JOBTITLERUS)
                    && Objects.equals(LOGINNAME, row.LOGINNAME)
                    && Objects.equals(email, row.email)
                    && Objects.equals(IPPHONE, row.IPPHONE)
                    && Objects.equals(WORKPHONE, row.WORKPHONE)
                    && Objects.equals(TYPE_WORK, row.TYPE_WORK)
                    && Objects.equals(DEPARTMENTID, row.DEPARTMENTID)
                    && Objects.equals(MANAGERID, row.MANAGERID)
                    && Objects.equals(USER_SID, row.USER_SID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ID, EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID);
        }
    }

    public static void main(String[] args) {
        LocalDateTime dStart = LocalDateTime.now();
        System.out.println("Старт : " + dStart);
        // Сравнение двух таблиц.
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Проверка, пуста ли temp_table (КИСУ)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + TEMP_TABLE)) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count == 0) {
                        System.out.println(AnsiColor.BG_YELLOW + "temp_table пустая — обмен данными не выполняется." + AnsiColor.RESET);
                        return; // Выход из main, ничего не меняем
                    }
                }
            }
            // Создать DEL_MAIN_TABLE, если не существует
            createDelTableIfNotExists(conn);
            // Шаг 1: Загрузить MD5-хэши из main_table в List для быстрого поиска
            List<Row> mainRows = loadRows(conn, MAIN_TABLE, false); // false для MAIN_TABLE (не используем NOW())
            // Шаг 2: Загрузить строки из temp_table в SET
            Set<String> tempHashes = loadHashes(conn);
            // Шаг 3: Определить строки для удаления (совпадающие или старые)
            List<Row> rowsToDelete = new ArrayList<>();
            for (Row row : mainRows) {
                String hash = row.getMD5();
                if (!tempHashes.contains(hash)) { // Старые (не в temp)
                    rowsToDelete.add(row);
                }
            }
            // Шаг 4: Выгрузить удаляемые строки в del_main_table
            insertRowsToDel(conn, rowsToDelete);
            // Шаг 5: Удалить эти строки из main_table
            deleteRowsFromMain(conn, rowsToDelete);
            // Шаг 6: Загрузить строки из temp_table и вставить только новые
            List<Row> tempRows = loadRows(conn, TEMP_TABLE, true); // true для TEMP_TABLE (используем NOW())
            Set<String> originalMainHashes = new HashSet<>();
            for (Row row : mainRows) {
                originalMainHashes.add(row.getMD5());
            }
            insertNewRows(conn, tempRows, originalMainHashes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LocalDateTime dStop = LocalDateTime.now();
        System.out.println("Стоп : " + dStop);

        Duration duration = Duration.between(dStart, dStop);
        long seconds = duration.getSeconds();
        //Описание итогов
        System.out.printf(String.valueOf(AnsiColor.GREEN) + AnsiColor.BOLD +"Время выгрузки %d секунд.\n" + AnsiColor.RESET, seconds);
        System.out.printf(String.valueOf(AnsiColor.GREEN) + AnsiColor.BOLD + "Синхронизация завершена успешно!" + AnsiColor.RESET + AnsiColor.BLUE + " \nУдаляемые строки выгружены в `" + DEL_MAIN_TABLE.toUpperCase() + "`. В `" + MAIN_TABLE.toUpperCase() + "` остались только новые записи. 😀✨\n" + AnsiColor.RESET);
        System.out.printf(AnsiColor.BLUE + "Для проверки подключитесь к базе MySQL(%s). \nВыполните запросы:\n %s", "sql_cdv", "select * FROM temp_table;     - проверка данных из КИСУ.\n select * FROM demployes;      - проверка данных актуальных пользователей.\n select * from del_main_table; - проверка удаленных данных. \n" + AnsiColor.RESET);
        System.out.printf(AnsiColor.BLUE + "Для проверки последних загруженных данных, добавить условие:\n %s","select * FROM demployes where " + AnsiColor.RESET + AnsiColor.BRIGHT_BLUE + AnsiColor.UNDERLINE + "date_create=(SELECT MAX(date_create)" + AnsiColor.RESET + AnsiColor.BLUE + ");                             - записи с последнего обновления.\n select * FROM demployes where " + AnsiColor.RESET + AnsiColor.BRIGHT_BLUE + AnsiColor.UNDERLINE + "CAST(date_create AS DATE)=(SELECT MAX(CAST(date_create AS DATE))" + AnsiColor.RESET + AnsiColor.BLUE + "); - записи с последнего обновления в течении дня.\n" + AnsiColor.RESET);

    }

    // Создать del_main_table, если не существует
    private static void createDelTableIfNotExists(Connection conn) throws SQLException {
        // Создать таблицу как копию структуры main_table
        String createSQL = "CREATE TABLE " + DEL_MAIN_TABLE + " LIKE " + MAIN_TABLE;
        // Добавить столбец date_delete с типом DATETIME и дефолтом NOW() (исправлено для соответствия NOW() в insert)
        String alterSQL = "ALTER TABLE " + DEL_MAIN_TABLE + " ADD COLUMN date_delete DATETIME DEFAULT NOW()";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createSQL);
            stmt.executeUpdate(alterSQL);
        } catch (SQLException e) {
            // Таблица уже существует, игнорируем ошибку
            if (!e.getSQLState().equals("42S01") && !e.getMessage().contains("already exists")) { // Код ошибки для MySQL
                throw e;
            }
        }
    }

    // Загрузить MD5-хэши из таблицы (теперь использует loadRows для объединения логики)
    private static Set<String> loadHashes(Connection conn) throws SQLException, NoSuchAlgorithmException {
        Set<String> hashes = new HashSet<>();
        List<Row> rows = loadRows(conn, App.TEMP_TABLE, false); // Объединение: используем loadRows без NOW()
        for (Row row : rows) {
            hashes.add(row.getMD5());
        }
        return hashes;
    }

    // Загрузить строки из таблицы (объединенный метод: isTempTable=true для TEMP_TABLE, чтобы использовать NOW() для date_create)
    private static List<Row> loadRows(Connection conn, String tableName, boolean isTempTable) throws SQLException {
        List<Row> rows = new ArrayList<>();
        String query = isTempTable
                ? "SELECT ID, EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, NOW() as date_create FROM " + tableName
                : "SELECT ID, EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create FROM " + tableName;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int ID = rs.getInt("ID");
                String EMPLOYEEID = rs.getString("EMPLOYEEID");
                String LASTNAMERUS = rs.getString("LASTNAMERUS");
                String NAMERUS = rs.getString("NAMERUS");
                String MIDDLENAMERUS = rs.getString("MIDDLENAMERUS");
                String TABNOM = rs.getString("TABNOM");
                String JOBTITLERUS = rs.getString("JOBTITLERUS");
                String LOGINNAME = rs.getString("LOGINNAME");
                String email = rs.getString("email");
                String IPPHONE = rs.getString("IPPHONE");
                String WORKPHONE = rs.getString("WORKPHONE");
                String TYPE_WORK = rs.getString("TYPE_WORK");
                String DEPARTMENTID = rs.getString("DEPARTMENTID");
                String MANAGERID = rs.getString("MANAGERID");
                String USER_SID = rs.getString("USER_SID");
                String date_create = rs.getString("date_create");
                rows.add(new Row(ID, EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create));
            }
        }
        return rows;
    }

    // Вставить новые строки в main_table
    private static void insertNewRows(Connection conn, List<Row> tempRows, Set<String> mainHashes) throws SQLException, NoSuchAlgorithmException {
        String insertSQL = "INSERT INTO " + MAIN_TABLE + " (EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Настройте для авто-генерации id, если нужно
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (Row row : tempRows) {
                String hash = row.getMD5();
                if (!mainHashes.contains(hash)) {
                    pstmt.setString(1, row.EMPLOYEEID);
                    pstmt.setString(2, row.LASTNAMERUS);
                    pstmt.setString(3, row.NAMERUS);
                    pstmt.setString(4, row.MIDDLENAMERUS);
                    pstmt.setString(5, row.TABNOM);
                    pstmt.setString(6, row.JOBTITLERUS);
                    pstmt.setString(7, row.LOGINNAME);
                    pstmt.setString(8, row.email);
                    pstmt.setString(9, row.IPPHONE);
                    pstmt.setString(10, row.WORKPHONE);
                    pstmt.setString(11, row.TYPE_WORK);
                    pstmt.setString(12, row.DEPARTMENTID);
                    pstmt.setString(13, row.MANAGERID);
                    pstmt.setString(14, row.USER_SID);
                    pstmt.setString(15, row.date_create);
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
        }
    }

    // Вставить строки в del_main_table
    private static void insertRowsToDel(Connection conn, List<Row> rows) throws SQLException {
        String insertSQL = "INSERT INTO " + DEL_MAIN_TABLE + " (ID, EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create, date_delete) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (Row row : rows) {
                pstmt.setInt(1, row.ID);
                pstmt.setString(2, row.EMPLOYEEID);
                pstmt.setString(3, row.LASTNAMERUS);
                pstmt.setString(4, row.NAMERUS);
                pstmt.setString(5, row.MIDDLENAMERUS);
                pstmt.setString(6, row.TABNOM);
                pstmt.setString(7, row.JOBTITLERUS);
                pstmt.setString(8, row.LOGINNAME);
                pstmt.setString(9, row.email);
                pstmt.setString(10, row.IPPHONE);
                pstmt.setString(11, row.WORKPHONE);
                pstmt.setString(12, row.TYPE_WORK);
                pstmt.setString(13, row.DEPARTMENTID);
                pstmt.setString(14, row.MANAGERID);
                pstmt.setString(15, row.USER_SID);
                pstmt.setString(16, row.date_create);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    // Удалить строки из main_table
    private static void deleteRowsFromMain(Connection conn, List<Row> rows) throws SQLException {
        String deleteSQL = "DELETE FROM " + MAIN_TABLE + " WHERE ID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            for (Row row : rows) {
                pstmt.setInt(1, row.ID);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}
