package kvo.separat;

/**
 * Кастомное исключение для ошибок в процессе синхронизации.
 * Наследуется от RuntimeException, чтобы не ломать Stream API.
 */
public class SyncException extends RuntimeException {

    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public SyncException(String message) {
        super(message);
    }
}