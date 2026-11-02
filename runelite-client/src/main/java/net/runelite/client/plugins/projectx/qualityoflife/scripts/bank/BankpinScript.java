package net.runelite.client.plugins.projectx.qualityoflife.scripts.bank;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.qualityoflife.QoLConfig;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.security.Encryption;
import net.runelite.client.plugins.projectx.util.security.Login;
import net.runelite.client.plugins.projectx.util.security.LoginManager;

import java.util.concurrent.TimeUnit;

public class BankpinScript extends Script {
    public boolean run(QoLConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn()) return;
                if (!super.run()) return;
                if (!config.useBankPin()) return;
                if ((LoginManager.getActiveProfile().getBankPin() == null || LoginManager.getActiveProfile().getBankPin().isEmpty()) || LoginManager.getActiveProfile().getBankPin().equalsIgnoreCase("**bankpin**")) return;

                Rs2Bank.handleBankPin(Encryption.decrypt(LoginManager.getActiveProfile().getBankPin()));

            } catch(Exception ex) {
                ProjectX.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }
}
