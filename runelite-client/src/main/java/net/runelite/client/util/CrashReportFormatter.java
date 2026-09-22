/*
 * Copyright (c) 2024
 * All rights reserved.
 */
package net.runelite.client.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class CrashReportFormatter
{
    private CrashReportFormatter()
    {
    }

    public static String summarize(Throwable throwable)
    {
        if (throwable == null)
        {
            return "";
        }

        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root)
        {
            root = root.getCause();
        }

        StringBuilder summary = new StringBuilder(root.getClass().getName());
        String message = root.getMessage();
        if (message != null && !message.isBlank())
        {
            summary.append(": ").append(message);
        }

        return summary.toString();
    }

    public static String stackTrace(Throwable throwable)
    {
        if (throwable == null)
        {
            return "";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    public static String buildReport(Throwable throwable)
    {
        if (throwable == null)
        {
            return "";
        }

        String summary = summarize(throwable);
        String trace = stackTrace(throwable);

        if (summary.isEmpty())
        {
            return trace;
        }

        if (trace.isEmpty())
        {
            return summary;
        }

        return summary + "\n\nStack trace:\n" + trace;
    }
}
