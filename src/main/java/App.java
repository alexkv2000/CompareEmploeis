
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    // Класс для представления строки таблицы
    static class Row {
        int ID;
        String EMPLOYEEID; // ID
        String LASTNAMERUS; // Фамилия
        String NAMERUS; // Имя
        String MIDDLENAMERUS; // Отчество
        String TABNOM; // таб.номер
        String JOBTITLERUS; //Должность
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

        // Вычислить MD5-хэш уникальных полей (исключая id)
        String getMD5() throws NoSuchAlgorithmException {
            String data = EMPLOYEEID + "|" + LASTNAMERUS + "|" + NAMERUS + "|" + MIDDLENAMERUS + "|" + TABNOM + "|" + JOBTITLERUS + "|" + LOGINNAME + "|" + email + "|" + IPPHONE + "|" + WORKPHONE + "|" + TYPE_WORK + "|" + DEPARTMENTID + "|" + MANAGERID +"|" + USER_SID; // Конкатенация с разделителем
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
        //сравнение двух таблиц.
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Создать DEL_MAIN_TABLE, если не существует
            createDelTableIfNotExists(conn);
            // Шаг 1: Загрузить MD5-хэши из main_table в List для быстрого поиска
            List<Row> mainRows = loadRowsEmploeis(conn, MAIN_TABLE);
            // Шаг 2: Загрузить строки из temp_table в SET
            Set<String> tempHashes  = loadHashes(conn, TEMP_TABLE);
            // Шаг 3: Определить строки для удаления (совпадающие или старые)
            List<Row> rowsToDelete = new ArrayList<>();
            for (Row row : mainRows) {
                String hash = row.getMD5();
                if (tempHashes.contains(hash)) { // Совпадающие
                    //rowsToDelete.add(row);
                } else { // Старые (не в temp)
                    rowsToDelete.add(row);
                }
            }
            // Шаг 4: Выгрузить удаляемые строки в del_main_table
            insertRowsToDel(conn, rowsToDelete);
            // Шаг 5: Удалить эти строки из main_table
            deleteRowsFromMain(conn, rowsToDelete);
            // Шаг 6: Загрузить строки из temp_table и вставить только новые
            List<Row> tempRows = loadRows(conn, TEMP_TABLE);
            Set<String> originalMainHashes = new HashSet<>();
            for (Row row : mainRows) {
                originalMainHashes.add(row.getMD5());
            }
            insertNewRows(conn, tempRows, originalMainHashes);

            System.out.println("Синхронизация завершена! Удаляемые строки выгружены в DEL_MAIN_TABLE. В MAIN_TABLE остались только новые записи. 😀✨");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Создать del_main_table, если не существует
    private static void createDelTableIfNotExists(Connection conn) throws SQLException {
        // Создать таблицу как копию структуры main_table
        String createSQL = "CREATE TABLE " + DEL_MAIN_TABLE + " LIKE " + MAIN_TABLE;
        // Добавить столбец date_delete с типом VARCHAR(20) и дефолтом
        String alterSQL = "ALTER TABLE " + DEL_MAIN_TABLE + " ADD COLUMN date_delete VARCHAR(20) DEFAULT ''";

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
    // Загрузить MD5-хэши из таблицы
    private static Set<String> loadHashes(Connection conn, String tableName) throws SQLException, NoSuchAlgorithmException {
        Set<String> hashes = new HashSet<>();
        String query = "SELECT EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create FROM " + tableName; // Настройте поля
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
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
                Row tempRow = new Row(0, EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create); // id не нужен для хэша
                hashes.add(tempRow.getMD5());
            }
        }
        return hashes;
    }

    // Загрузить строки из таблицы
    private static List<Row> loadRowsEmploeis(Connection conn, String tableName) throws SQLException {
        List<Row> rows = new ArrayList<>();
        String query = "SELECT ID, EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, date_create FROM " + tableName;
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
    // Загрузить строки из таблицы temp
    private static List<Row> loadRows(Connection conn, String tableName) throws SQLException {
        List<Row> rows = new ArrayList<>();
        String query = "SELECT ID, EMPLOYEEID, LASTNAMERUS, NAMERUS, MIDDLENAMERUS, TABNOM, JOBTITLERUS, LOGINNAME, email, IPPHONE, WORKPHONE, TYPE_WORK, DEPARTMENTID, MANAGERID, USER_SID, NOW() as date_create FROM " + tableName;
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

