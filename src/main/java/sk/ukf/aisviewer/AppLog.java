package sk.ukf.aisviewer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * Logovanie aplikácie (java.util.logging – bez ďalších knižníc).
 *
 * <p>Bežne sa vypisujú úrovne INFO a vyššie. Podrobné diagnostické výpisy (FINE),
 * napr. rozhodovanie študijného plánu, sa zapnú spustením s {@code -Dais.debug=true}.
 *
 * <p>Výstup: {@code 16:32:21 INFO    [AisSession] Prihlasovanie...}
 */
public final class AppLog {

    private static final String ROOT = "sk.ukf.aisviewer";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    static {
        Logger root = Logger.getLogger(ROOT);
        Level level = Boolean.getBoolean("ais.debug") ? Level.FINE : Level.INFO;
        root.setLevel(level);
        root.setUseParentHandlers(false);

        // Bežné správy na System.out (v IntelliJ čierne), chyby na System.err (červené).
        StreamHandler out = new StreamHandler(System.out, new LineFormatter()) {
            @Override
            public synchronized void publish(LogRecord record) {
                if (record.getLevel().intValue() < Level.WARNING.intValue()) {
                    super.publish(record);
                    flush();
                }
            }
        };
        out.setLevel(level);
        ConsoleHandler err = new ConsoleHandler();
        err.setFormatter(new LineFormatter());
        err.setLevel(Level.WARNING);
        root.addHandler(out);
        root.addHandler(err);
    }

    private AppLog() {
    }

    public static Logger get(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

    private static final class LineFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String logger = record.getLoggerName();
            String shortName = logger == null ? "" : logger.substring(logger.lastIndexOf('.') + 1);
            StringBuilder line = new StringBuilder()
                    .append(LocalTime.now().format(TIME)).append(' ')
                    .append(String.format("%-7s", record.getLevel().getName())).append(' ')
                    .append('[').append(shortName).append("] ")
                    .append(formatMessage(record))
                    .append(System.lineSeparator());
            if (record.getThrown() != null) {
                StringWriter trace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(trace));
                line.append(trace);
            }
            return line.toString();
        }
    }
}
