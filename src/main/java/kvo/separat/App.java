package kvo.separat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

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

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    static final String ROWS = " rows";
    static final String DATECREATE = "DATE_CREATE";
    static final String MANAGER = "MANAGERID";

    // Исправлено: добавлено слово final. Они будут инициализированы один раз в статическом блоке ниже.
    public static final Counter employeeSyncCounter;
    public static final Counter departmentSyncCounter;
    public static final Counter employeeSyncErrorCounter;
    public static final Counter departmentSyncErrorCounter;
    public static final Timer employeeSyncTimer;
    public static final Timer departmentSyncTimer;
    public static final Counter rowsProcessedCounter;
    public static final Counter rowsInsertedCounter;
    public static final Counter rowsDeletedCounter;

    // Исправлено: public статические поля без final запрещены. Сделан приватным.
    private static final String CURRENT_DIR = System.getProperty("user.dir");

    private static final PrometheusMeterRegistry registry;
    private static JvmGcMetrics jvmGcMetrics;

    public enum AnsiColor {
        RESET("\033[0m"), BLACK("\033[30m"), RED("\033[31m"), GREEN("\033[32m"), YELLOW("\033[33m"), BLUE("\033[34m"), PURPLE("\033[35m"), CYAN("\033[36m"), WHITE("\033[37m"),
        BRIGHT_BLACK("\033[90m"), BRIGHT_RED("\033[91m"), BRIGHT_GREEN("\033[92m"), BRIGHT_YELLOW("\033[93m"), BRIGHT_BLUE("\033[94m"), BRIGHT_PURPLE("\033[95m"), BRIGHT_CYAN("\033[96m"), BRIGHT_WHITE("\033[97m"),
        BG_BLACK("\033[40m"), BG_RED("\033[41m"), BG_GREEN("\033[42m"), BG_YELLOW("\033[43m"), BG_BLUE("\033[44m"), BG_PURPLE("\033[45m"), BG_CYAN("\033[46m"), BG_WHITE("\033[47m"),
        BG_BRIGHT_BLACK("\033[100m"), BG_BRIGHT_RED("\033[101m"), BG_BRIGHT_GREEN("\033[102m"), BG_BRIGHT_YELLOW("\033[103m"), BG_BRIGHT_BLUE("\033[104m"), BG_BRIGHT_PURPLE("\033[105m"), BG_BRIGHT_CYAN("\033[106m"), BG_BRIGHT_WHITE("\033[107m"),
        BOLD("\033[1m"), UNDERLINE("\033[4m"), REVERSED("\033[7m");

        private final String code;
        AnsiColor(String code) { this.code = code; }
        @Override public String toString() { return code; }
    }

    // Статический блок инициализации. Выполняется ровно 1 раз при загрузке класса в память.
    static {
        final String APPLICATION = "application";
        final String SYNCAPP = "sync-app";
        PrometheusMeterRegistry localRegistry = null;
        JvmGcMetrics localGcMetrics = null;

        try {
            localRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            localGcMetrics = new JvmGcMetrics();

            // Инициализируем константы
            employeeSyncCounter = Counter.builder("employee_sync_total").description("Total number of employee synchronizations").tag(APPLICATION, SYNCAPP).register(localRegistry);
            departmentSyncCounter = Counter.builder("department_sync_total").description("Total number of department synchronizations").tag(APPLICATION, SYNCAPP).register(localRegistry);

            employeeSyncErrorCounter = Counter.builder("employee_sync_errors_total").description("Total number of employee synchronization errors").tag(APPLICATION, SYNCAPP).register(localRegistry);
            departmentSyncErrorCounter = Counter.builder("department_sync_errors_total").description("Total number of department synchronization errors").tag(APPLICATION, SYNCAPP).register(localRegistry);

            employeeSyncTimer = Timer.builder("employee_sync_duration_seconds").description("Employee synchronization duration in seconds").tag(APPLICATION, SYNCAPP).register(localRegistry);
            departmentSyncTimer = Timer.builder("department_sync_duration_seconds").description("Department synchronization duration in seconds").tag(APPLICATION, SYNCAPP).register(localRegistry);

            rowsProcessedCounter = Counter.builder("rows_processed_total").description("Total number of rows processed").tag(APPLICATION, SYNCAPP).register(localRegistry);
            rowsInsertedCounter = Counter.builder("rows_inserted_total").description("Total number of rows inserted").tag(APPLICATION, SYNCAPP).register(localRegistry);
            rowsDeletedCounter = Counter.builder("rows_deleted_total").description("Total number of rows deleted").tag(APPLICATION, SYNCAPP).register(localRegistry);

            new JvmMemoryMetrics().bindTo(localRegistry);
            localGcMetrics.bindTo(localRegistry);
            new JvmThreadMetrics().bindTo(localRegistry);
            new ProcessorMetrics().bindTo(localRegistry);
            new UptimeMetrics().bindTo(localRegistry);

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e); // Прерываем работу приложения, если метрики не смогли создаться
        }

        registry = localRegistry;
        jvmGcMetrics = localGcMetrics;
    }

    public static void main(String[] args) throws IOException {
        // Убираем вызов initializeMonitoring(), так как статический блок выше уже всё инициализировал!

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (jvmGcMetrics != null) jvmGcMetrics.close();
        }));

        String configPath = CURRENT_DIR + "\\src\\main\\java\\config\\settingSynDictionary.txt";
        String syncDic = "";

        for (String arg : args) {
            if (arg.startsWith("config.path=")) configPath = arg.substring("config.path=".length());
            if (arg.startsWith("dictionary=")) syncDic = arg.substring("dictionary=".length());
        }

        ConfigLoader configLoader = new ConfigLoader(configPath);
        String dbUrl = configLoader.getProperty("DB_URL");
        String user = configLoader.getProperty("USER");
        String pass = configLoader.getProperty("PASS");

        try (Connection conn = DriverManager.getConnection(dbUrl, user, pass)) {
            switch (syncDic.toLowerCase()) {
                case "department" -> {
                    logSyncExecution(() -> new DepartmentSynchronizer(registry).sync(conn));
                    safeSleep();
                }
                case "employees" -> {
                    logSyncExecution(() -> new EmployeeSynchronizer(registry).sync(conn));
                    safeSleep();
                }
                default -> {
                    logSyncExecution(() -> new DepartmentSynchronizer(registry).sync(conn));
                    safeSleep();
                    logSyncExecution(() -> new EmployeeSynchronizer(registry).sync(conn));
                    safeSleep();
                }
            }
        } catch (SQLException e) {
            logger.error("Database connection error", e);
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

    private static void safeSleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}