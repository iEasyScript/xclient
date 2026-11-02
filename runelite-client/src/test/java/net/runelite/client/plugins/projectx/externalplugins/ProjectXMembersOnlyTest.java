package net.runelite.client.plugins.projectx.externalplugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import org.junit.Test;

public class ProjectXMembersOnlyTest
{
	private static ProjectXAccount.Identity identity(String json)
	{
		return new Gson().fromJson(json, ProjectXAccount.Identity.class);
	}

	@Test
	public void noIdentityIsNotAllowed()
	{
		assertFalse(ProjectXMembersOnly.isAllowed(null));
	}

	@Test
	public void anyLiveMembershipIsAllowed()
	{
		for (String tier : new String[]{"BRONZE", "RUNE", "DRAGON"})
		{
			assertTrue(tier, ProjectXMembersOnly.isAllowed(identity(
				"{\"role\":\"USER\",\"membership\":{\"tier\":\"" + tier + "\"}}")));
		}
	}

	@Test
	public void userWithoutMembershipIsNotAllowed()
	{
		assertFalse(ProjectXMembersOnly.isAllowed(identity("{\"role\":\"USER\"}")));
		assertFalse(ProjectXMembersOnly.isAllowed(identity("{\"role\":\"USER\",\"membership\":null}")));
	}

	@Test
	public void developerWithoutMembershipIsNotAllowed()
	{
		assertFalse(ProjectXMembersOnly.isAllowed(identity("{\"role\":\"DEVELOPER\"}")));
	}

	@Test
	public void adminIsAllowedWithoutMembership()
	{
		assertTrue(ProjectXMembersOnly.isAllowed(identity("{\"role\":\"ADMIN\"}")));
		assertTrue(ProjectXMembersOnly.isAllowed(identity("{\"role\":\"admin\"}")));
	}

	@Test
	public void deniedMessageNamesTheFeatureAndWhereToJoin()
	{
		assertEquals("QoL is for Project X members (any tier). Get a membership at xclient.dev/membership.",
			ProjectXMembersOnly.deniedMessage("QoL"));
	}
}
