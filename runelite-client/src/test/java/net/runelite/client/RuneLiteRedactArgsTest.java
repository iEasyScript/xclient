package net.runelite.client;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * The startup arguments go to client.log, which users send in for support.
 */
public class RuneLiteRedactArgsTest
{
	@Test
	public void hidesProxyCredentials()
	{
		assertEquals("-proxy=socks://***@1.2.3.4:1080",
			RuneLite.redactArgs(Arrays.asList("-proxy=socks://bob:s3cret@1.2.3.4:1080")));
		assertEquals("--proxy http://***@host:8080",
			RuneLite.redactArgs(Arrays.asList("--proxy", "http://bob:s3cret@host:8080")));
	}

	@Test
	public void hidesSecretProperties()
	{
		String logged = RuneLite.redactArgs(Arrays.asList(
			"-Dprojectx.accountToken=px_abcDEF123-_xyz",
			"-Dsome.password=hunter2",
			"-Dprojectx.discordWebhook=https://discord.com/api/webhooks/1/abc"));
		assertEquals("-Dprojectx.accountToken=*** -Dsome.password=*** -Dprojectx.discordWebhook=***", logged);
	}

	@Test
	public void hidesBareAccountTokens()
	{
		assertEquals("--token px_***", RuneLite.redactArgs(Arrays.asList("--token", "px_abcDEF123-_xyz")));
	}

	@Test
	public void leavesOrdinaryArgumentsAlone()
	{
		String args = "--profile main --debug -Xmx768m -Dprojectx.siteUrl=https://xclient.dev -XX:+UseG1GC";
		assertEquals(args, RuneLite.redactArgs(Arrays.asList(args.split(" "))));
		assertFalse(RuneLite.redactArgs(Arrays.asList("-proxy=socks://1.2.3.4:1080")).contains("***"));
	}
}
