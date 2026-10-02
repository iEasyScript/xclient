package net.runelite.client.plugins.projectx;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import net.runelite.api.ChatMessageType;

public class GameChatAppender extends AppenderBase<ILoggingEvent> {
    private final PatternLayout layout = new PatternLayout();
    
    // Cache the current configuration to avoid config lookups during filtering
    private static volatile boolean loggingEnabled = true;
    private static volatile Level minimumLevel = Level.WARN;
    private static volatile boolean onlyProjectXLogging = true;

    public GameChatAppender(String pattern) {
        // Order matters! Level filter should run first to deny based on log level
        addFilter(new GameChatLevelFilter());
        addFilter(new OnlyProjectXLoggingFilter());
        layout.setPattern(pattern);
    }

    public GameChatAppender() {
        this("[%d{HH:mm:ss}] %msg%ex{0}%n"); // Default simple pattern
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        layout.setContext(context);
    }

    @Override
    public void start() {
        layout.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        layout.stop();
    }

    public void setPattern(String pattern) {
        final boolean started = layout.isStarted();
        if (started) layout.stop();
        layout.setPattern(pattern);
        if (started) layout.start();
    }
    
    /**
     * Updates the cached configuration for filtering
     */
    public static void updateConfiguration(boolean enabled, Level level, boolean projectxOnly) {
        loggingEnabled = enabled;
        minimumLevel = level;
        onlyProjectXLogging = projectxOnly;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!ProjectX.isLoggedIn()) return;

        final String formatted = layout.doLayout(event);
        // Fire and forget. The braces matter: addChatMessage returns a value, so an expression
        // lambda here resolves to invoke(Supplier), which waits up to 10s for the client thread
        // while logback holds this appender's lock. The client thread logging anything in that
        // window then blocks on the same lock -- a deadlock that froze the client at login.
        ProjectX.getClientThread().invoke(() -> {
            ProjectX.getClient().addChatMessage(ChatMessageType.ENGINE, "", formatted, "", false);
        });
    }

    /**
     * Filter to control which log levels appear in game chat based on configuration
     */
    private static class GameChatLevelFilter extends Filter<ILoggingEvent> {
        @Override
        public FilterReply decide(ILoggingEvent event) {
            // Check if logging is enabled
            if (!loggingEnabled) {
                return FilterReply.DENY;
            }
            
            // In debug mode, show all levels (overrides configuration)
            if (ProjectX.isDebug()) {
                return FilterReply.NEUTRAL;
            }
            
            // Use cached minimum level to filter (includes DEBUG if configured)
            return event.getLevel().isGreaterOrEqual(minimumLevel) ? FilterReply.NEUTRAL : FilterReply.DENY;
        }
    }

    private static class OnlyProjectXLoggingFilter extends Filter<ILoggingEvent> {
        @Override
        public FilterReply decide(ILoggingEvent event) {
            // If only projectx logging is disabled, accept all logs
            if (!onlyProjectXLogging) {
                return FilterReply.NEUTRAL;
            }
            
            // Otherwise, only accept projectx logs
            return event.getLoggerName().startsWith("net.runelite.client.plugins.projectx") ? FilterReply.NEUTRAL : FilterReply.DENY;
        }
    }
}
