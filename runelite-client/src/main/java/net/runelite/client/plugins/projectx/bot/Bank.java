package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;

/**
 * The bank: open it, deposit, withdraw.
 *
 * <pre>{@code
 * if (Bank.openBank()) {
 *     Bank.depositAll();
 * }
 * }</pre>
 *
 * <p>Every static method and field of {@link Rs2Bank} is available here under the shorter name,
 * so {@code Rs2Bank.anything(...)} can be written {@code Bank.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Bank extends Rs2Bank
{
    private Bank()
    {
    }
}
