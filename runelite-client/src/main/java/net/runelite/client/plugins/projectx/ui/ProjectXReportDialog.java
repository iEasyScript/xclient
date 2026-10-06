package net.runelite.client.plugins.projectx.ui;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.ProjectXLogBuffer;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXAccount;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXSite;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * "Report a problem" for one script: the player says what went wrong, and the
 * report goes to the Project X support inbox with the script's name and version,
 * the client version, and -- if they leave the box ticked -- the script's recent
 * log and its settings, with anything secret masked out.
 */
@Slf4j
final class ProjectXReportDialog
{
    private ProjectXReportDialog()
    {
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int LOG_MINUTES = 30;
    private static final int LOG_LINES = 1500;

    /** Settings whose values must never leave the machine. */
    private static final Pattern SECRET_KEY = Pattern.compile("(?i)(password|passwd|token|secret|pin|webhook|apikey|api_key|auth|cookie|session)");
    /** Long opaque strings in log lines: tokens, keys, webhook paths. */
    private static final Pattern SECRET_VALUE = Pattern.compile("[A-Za-z0-9_\\-]{40,}|discord(app)?\\.com/api/webhooks/\\S+");

    static void show(Component parent, Plugin plugin, ConfigDescriptor configDescriptor)
    {
        PluginDescriptor descriptor = plugin == null ? null : plugin.getClass().getAnnotation(PluginDescriptor.class);
        String scriptName = descriptor == null ? "Project X client" : descriptor.name().replaceAll("<[^>]*>", "").trim();
        String version = descriptor == null ? "" : descriptor.version();

        if (ProjectXAccount.token() == null)
        {
            JOptionPane.showMessageDialog(parent,
                "Sign in to Project X first (in the launcher, or Project X settings → Project X account),\n"
                    + "so we know who to reply to.",
                "Report a problem", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Report a problem: " + scriptName, java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel intro = new JLabel("<html><div style='width:340px'>What went wrong? What was the script doing, and what did you expect it to do?"
            + " We reply by Discord DM.</div></html>");
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(intro);

        JTextArea note = new JTextArea(7, 36);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        JScrollPane noteScroll = new JScrollPane(note);
        noteScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        noteScroll.setPreferredSize(new Dimension(380, 140));
        body.add(noteScroll);

        JCheckBox include = new JCheckBox("Include the last " + LOG_MINUTES + " minutes of this script's log and its settings");
        include.setSelected(true);
        include.setAlignmentX(Component.LEFT_ALIGNMENT);
        include.setToolTipText("Helps us find the problem much faster. Passwords, tokens and webhooks are masked.");
        body.add(include);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton send = new JButton("Send report");
        buttons.add(cancel);
        buttons.add(send);

        cancel.addActionListener(e -> dialog.dispose());
        send.addActionListener(e ->
        {
            String text = note.getText().trim();
            if (text.length() < 3)
            {
                JOptionPane.showMessageDialog(dialog, "Say a little about what went wrong first.", "Report a problem", JOptionPane.WARNING_MESSAGE);
                return;
            }
            send.setEnabled(false);
            send.setText("Sending...");
            boolean withDetails = include.isSelected();
            String packagePrefix = plugin == null ? null : plugin.getClass().getPackage().getName();
            String internalName = plugin == null ? null : plugin.getClass().getSimpleName();
            Thread sender = new Thread(() ->
            {
                String result = submit(text, internalName, scriptName, version,
                    withDetails ? scrub(ProjectXLogBuffer.recent(packagePrefix, LOG_MINUTES, LOG_LINES)) : null,
                    withDetails ? settings(configDescriptor) : null);
                SwingUtilities.invokeLater(() ->
                {
                    if (result == null)
                    {
                        dialog.dispose();
                        JOptionPane.showMessageDialog(parent,
                            "Thanks, your report has been sent. We'll reply by Discord DM.",
                            "Report sent", JOptionPane.INFORMATION_MESSAGE);
                    }
                    else
                    {
                        send.setEnabled(true);
                        send.setText("Send report");
                        JOptionPane.showMessageDialog(dialog, result, "Report not sent", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }, "projectx-report");
            sender.setDaemon(true);
            sender.start();
        });

        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(body, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /** Sends the report. Returns null when it was accepted, otherwise what to tell the player. */
    private static String submit(String note, String internalName, String scriptName, String version, String logText, String settings)
    {
        String token = ProjectXAccount.token();
        if (token == null)
        {
            return "You're not signed in to Project X.";
        }
        JsonObject json = new JsonObject();
        json.addProperty("note", note);
        json.addProperty("scriptInternalName", internalName);
        json.addProperty("scriptName", scriptName);
        json.addProperty("scriptVersion", version);
        json.addProperty("clientVersion", RuneLiteProperties.getProjectXVersion());
        if (logText != null && !logText.isEmpty())
        {
            json.addProperty("log", logText);
        }
        if (settings != null && !settings.isEmpty())
        {
            json.addProperty("settings", settings);
        }

        Request request = new Request.Builder()
            .url(ProjectXSite.api("reports"))
            .header("Authorization", "Bearer " + token)
            .post(RequestBody.create(JSON, json.toString()))
            .build();
        OkHttpClient http = ProjectX.getInjector().getInstance(OkHttpClient.class);
        try (Response response = http.newCall(request).execute())
        {
            if (response.isSuccessful())
            {
                return null;
            }
            if (response.code() == 429)
            {
                return "You've sent a lot of reports today. Try again tomorrow, or message us on Discord.";
            }
            if (response.code() == 401)
            {
                return "The site didn't recognise your sign-in. Sign in again in the launcher and try again.";
            }
            return "The site couldn't take the report right now (" + response.code() + "). Try again in a minute.";
        }
        catch (Exception e)
        {
            log.warn("Problem report could not be sent: {}", e.getMessage());
            return "Couldn't reach xclient.dev. Check your connection and try again.";
        }
    }

    /** The script's settings as "Name (key) = value", secrets masked. */
    private static String settings(ConfigDescriptor descriptor)
    {
        if (descriptor == null || descriptor.getGroup() == null)
        {
            return null;
        }
        ConfigManager configManager = ProjectX.getInjector().getInstance(ConfigManager.class);
        String group = descriptor.getGroup().value();
        StringBuilder out = new StringBuilder();
        for (ConfigItemDescriptor item : descriptor.getItems())
        {
            String key = item.getItem().keyName();
            String value = configManager.getConfiguration(group, key);
            boolean secret = item.getItem().secret() || SECRET_KEY.matcher(key).find()
                || SECRET_KEY.matcher(item.getItem().name().toLowerCase(Locale.ROOT)).find();
            out.append(item.getItem().name()).append(" (").append(key).append(") = ")
                .append(value == null ? "(default)" : secret ? "[hidden]" : value)
                .append('\n');
        }
        return out.toString();
    }

    private static String scrub(String logText)
    {
        return logText == null ? null : SECRET_VALUE.matcher(logText).replaceAll("[redacted]");
    }
}
