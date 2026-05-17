package com.oshaklya.logger;

import jdk.jfr.Threshold;

import javax.print.attribute.standard.Destination;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

enum LogLevel {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4);

    final int value;

    LogLevel(int value) {
        this.value = value;
    }

}

class LogRecord {
    String message;
    LocalDateTime time;
    String thread;
    LogLevel logLevel;

    LogRecord(String message, LogLevel logLevel) {
        this.logLevel = logLevel;
        this.message = message;
        this.time = LocalDateTime.now();
        this.thread = Thread.currentThread().getName();
    }

    @Override
    public String toString() {
        return "LogRecord{" +
                "message='" + message + '\'' +
                ", time=" + time +
                ", thread='" + thread + '\'' +
                ", logLevel=" + logLevel +
                '}';
    }
}

interface LogDestination {
    ReentrantLock lock = new ReentrantLock();

    void handleLogRecord(LogRecord logRecord);

    boolean thresholdAllowed(LogLevel logLevel);
}

class FileAppender implements LogDestination {
    LogLevel threshold;

    FileAppender(LogLevel threshold) {
        this.threshold = threshold;
    }

    @Override
    public void handleLogRecord(LogRecord logRecord) {
        try {
            lock.lock();
            System.out.println("[FILE LOG]"+logRecord);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean thresholdAllowed(LogLevel logLevel) {
        return logLevel.value >= threshold.value;
    }
}
class ConsoleAppender implements LogDestination {
    LogLevel threshold;

    ConsoleAppender(LogLevel threshold) {
        this.threshold = threshold;
    }

    @Override
    public void handleLogRecord(LogRecord logRecord) {
        try {
            lock.lock();
            System.out.println("[CONSOLE LOG]"+logRecord);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean thresholdAllowed(LogLevel logLevel) {
        return logLevel.value >= threshold.value;
    }
}

class Logger {
    private static volatile Logger logger = null;
    List<LogDestination> destinationList = new ArrayList<>();

    private Logger(List<LogDestination> destinationList) {
        this.destinationList = destinationList;
    }

    public static Logger getLogger(List<LogDestination> destinationList) {
        if (logger == null) {
            System.out.println("Creating a new logger");
            synchronized (Logger.class) {
                if (logger == null) {
                    logger = new Logger(destinationList);
                }
            }
        }
        return logger;
    }

    void log(String message, LogLevel logLevel) {
        destinationList.stream().filter(a -> a.thresholdAllowed(logLevel))
                .forEach(a -> a.handleLogRecord(new LogRecord(message, logLevel)));
    }

    void debug(String message) {
        log(message, LogLevel.DEBUG);
    }

    void info(String message) {
        log(message, LogLevel.INFO);
    }

    void error(String message) {
        log(message, LogLevel.ERROR);
    }

    void warn(String message) {
        log(message, LogLevel.WARN);
    }
}

public class Main {
    public static void main(String[] args) {
        FileAppender fileAppender = new FileAppender(LogLevel.INFO);
        ConsoleAppender consoleLog = new ConsoleAppender(LogLevel.TRACE);
        Logger logger = Logger.getLogger(List.of(fileAppender, consoleLog));
        logger.debug("this is a debug log");
        logger.warn("this is a warn log");
        logger.info("this is a info log");
    }
}
