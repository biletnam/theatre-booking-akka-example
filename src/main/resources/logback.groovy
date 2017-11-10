import ch.qos.logback.classic.encoder.PatternLayoutEncoder

appender(name = "CONSOLE", clazz = ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "name=Theatre date=%date{ISO8601} level=%level class=%logger{36} actor=%X{akkaSource} message=%msg\n"
    }
}

root(level = INFO, appenderNames = ["CONSOLE"])
