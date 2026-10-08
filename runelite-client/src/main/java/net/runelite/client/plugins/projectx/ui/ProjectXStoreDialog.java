package net.runelite.client.plugins.projectx.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXAccount;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXEntitlements;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXSite;
import net.runelite.client.util.LinkBrowser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Trial or buy a script without leaving the client: what it costs, what you own
 * and until when, your balance, and the buttons. Every purchase is confirmed first,
 * the same as on the website, and goes through the same code on the site.
 */
@Slf4j
final class ProjectXStoreDialog
{
    private ProjectXStoreDialog()
    {
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    /** Result of a call: the store state, or what to tell the player. */
    private static final class Reply
    {
        final JsonObject body;
        final String error;

        Reply(JsonObject body, String error)
        {
            this.body = body;
            this.error = error;
        }
    }

    static void show(Component parent, String internalName)
    {
        if (ProjectXAccount.token() == null)
        {
            JOptionPane.showMessageDialog(parent,
                "Sign in to Project X first (in the launcher, or Project X settings → Project X account).",
                "Project X store", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Project X store", Dialog.ModalityType.MODELESS);
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        body.add(new JLabel("Loading..."));
        dialog.setContentPane(body);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        load(dialog, body, internalName, null);
    }

    private static void load(JDialog dialog, JPanel body, String internalName, String flash)
    {
        background(() ->
        {
            Reply reply = call("GET", internalName, null);
            SwingUtilities.invokeLater(() -> render(dialog, body, internalName, reply, flash));
        });
    }

    private static void render(JDialog dialog, JPanel body, String internalName, Reply reply, String flash)
    {
        body.removeAll();
        if (reply.body == null)
        {
            body.add(label(reply.error));
            dialog.pack();
            return;
        }
        JsonObject s = reply.body;
        String name = str(s, "name");
        dialog.setTitle("Project X store: " + name);

        body.add(label("<b style='font-size:110%'>" + esc(name) + "</b>"));
        if (flash != null)
        {
            body.add(label("<span style='color:#7bd37b'>" + esc(flash) + "</span>"));
        }
        boolean free = bool(s, "free");
        boolean comped = bool(s, "comped");
        boolean broken = bool(s, "broken");
        boolean available = bool(s, "available");
        int balance = s.get("balance").getAsInt();
        String ownedUntil = str(s, "ownedUntil");
        JsonObject trial = s.getAsJsonObject("trial");
        String trialKind = str(trial, "kind");

        if (broken)
        {
            body.add(label("<span style='color:#e8a046'>Not working right now. Buying and trials are paused until it's fixed;"
                + " owners get the downtime added back.</span>"));
        }
        if (ownedUntil != null)
        {
            body.add(label("You own it until <b>" + DATE.format(Instant.parse(ownedUntil)) + "</b>."));
        }
        else if ("running".equals(trialKind))
        {
            Duration left = Duration.between(Instant.now(), Instant.parse(str(trial, "endsAt")));
            body.add(label("Free trial running: <b>" + Math.max(1, left.toMinutes()) + " minutes</b> left."));
        }
        else if ("pending".equals(trialKind))
        {
            body.add(label("Free trial requested: your hour starts as soon as the client picks it up."));
        }
        body.add(label("Your balance: <b>" + balance + " X Tokens</b>" + (comped ? " (admin: free)" : "")));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
        buttons.setOpaque(false);
        if (!free && available)
        {
            if ("available".equals(trialKind) && ownedUntil == null)
            {
                buttons.add(button("Start free 1-hour trial", () -> act(dialog, body, internalName, "trial", null, null)));
            }
            else if ("cooldown".equals(trialKind) || "tooNew".equals(trialKind))
            {
                String at = str(trial, "availableAt");
                buttons.add(disabled("Free trial again " + (at == null ? "later" : DATE.format(Instant.parse(at)))));
            }
            JsonArray prices = s.getAsJsonArray("prices");
            for (JsonElement el : prices)
            {
                JsonObject p = el.getAsJsonObject();
                String tier = str(p, "tier");
                String tierLabel = str(p, "label");
                int tokens = p.get("tokens").getAsInt();
                String verb = ownedUntil == null ? "Buy " : "Add ";
                JButton b = button(verb + tierLabel + " for " + tokens + " X", () ->
                {
                    int cost = comped ? 0 : tokens;
                    String question = "<html>" + verb + "<b>" + esc(tierLabel) + "</b> of " + esc(name) + " for <b>" + cost + " X Tokens</b>?"
                        + "<br>You have " + balance + "; you'll have " + (balance - cost) + " left."
                        + "<br>It doesn't renew on its own. All sales are final.</html>";
                    if (JOptionPane.showConfirmDialog(dialog, question, "Confirm purchase", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION)
                    {
                        act(dialog, body, internalName, "buy", tier, "Bought " + tierLabel + ". It's ready to start.");
                    }
                });
                b.setEnabled(comped || balance >= tokens);
                if (!b.isEnabled())
                {
                    b.setToolTipText("Not enough X Tokens: top up first.");
                }
                buttons.add(b);
            }
        }
        else if (free)
        {
            body.add(label("This script is free."));
        }
        buttons.add(button("Top up X Tokens", () -> LinkBrowser.browse(ProjectXSite.tokens().toString())));
        buttons.add(button("Open store page", () -> LinkBrowser.browse(str(s, "storeUrl"))));
        body.add(buttons);

        JPanel close = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        close.setOpaque(false);
        close.add(button("Close", dialog::dispose));
        body.add(close);
        body.revalidate();
        dialog.pack();
    }

    private static void act(JDialog dialog, JPanel body, String internalName, String action, String tier, String success)
    {
        body.setEnabled(false);
        background(() ->
        {
            JsonObject json = new JsonObject();
            json.addProperty("action", action);
            if (tier != null)
            {
                json.addProperty("tier", tier);
            }
            Reply reply = call("POST", internalName, json);
            if (reply.body != null && reply.error == null)
            {
                // Pick the new access up now rather than at the next scheduled check.
                try
                {
                    ProjectX.getInjector().getInstance(ProjectXEntitlements.class).refresh();
                }
                catch (Exception e)
                {
                    log.debug("Entitlement refresh after purchase failed: {}", e.getMessage());
                }
            }
            String flash = reply.error != null ? null
                : "trial".equals(action) ? "Trial started. Your hour begins as soon as the client picks it up (within a minute)."
                : success;
            SwingUtilities.invokeLater(() ->
            {
                if (reply.error != null)
                {
                    JOptionPane.showMessageDialog(dialog, reply.error, "Project X store", JOptionPane.WARNING_MESSAGE);
                }
                if (reply.body != null)
                {
                    render(dialog, body, internalName, reply, flash);
                }
                else
                {
                    load(dialog, body, internalName, null);
                }
            });
        });
    }

    private static Reply call(String method, String internalName, JsonObject json)
    {
        String token = ProjectXAccount.token();
        if (token == null)
        {
            return new Reply(null, "You're not signed in to Project X.");
        }
        Request.Builder rb = new Request.Builder()
            .url(ProjectXSite.api("store", internalName))
            .header("Authorization", "Bearer " + token);
        if ("POST".equals(method))
        {
            rb.post(RequestBody.create(JSON, json.toString()));
        }
        OkHttpClient http = ProjectX.getInjector().getInstance(OkHttpClient.class);
        try (Response response = http.newCall(rb.build()).execute())
        {
            String text = response.body() == null ? "" : response.body().string();
            JsonObject body = null;
            try
            {
                body = new JsonParser().parse(text).getAsJsonObject();
            }
            catch (Exception ignored)
            {
                // Not JSON: fall through to the status code.
            }
            if (response.isSuccessful())
            {
                return new Reply(body, null);
            }
            String message = body != null && body.has("message") ? body.get("message").getAsString()
                : response.code() == 404 ? "This script isn't on the store."
                : "The store couldn't do that right now (" + response.code() + ").";
            // A refusal can still carry the current state, so the window stays up to date.
            return new Reply(body != null && body.has("prices") ? body : null, message);
        }
        catch (Exception e)
        {
            return new Reply(null, "Couldn't reach xclient.dev. Check your connection and try again.");
        }
    }

    private static void background(Runnable r)
    {
        Thread t = new Thread(r, "projectx-store");
        t.setDaemon(true);
        t.start();
    }

    private static JLabel label(String html)
    {
        JLabel l = new JLabel("<html><div style='width:300px'>" + html + "</div></html>");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return l;
    }

    private static JButton button(String text, Runnable action)
    {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JButton disabled(String text)
    {
        JButton b = new JButton(text);
        b.setEnabled(false);
        return b;
    }

    private static String str(JsonObject o, String key)
    {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static boolean bool(JsonObject o, String key)
    {
        return o != null && o.has(key) && !o.get(key).isJsonNull() && o.get(key).getAsBoolean();
    }

    private static String esc(String s)
    {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
