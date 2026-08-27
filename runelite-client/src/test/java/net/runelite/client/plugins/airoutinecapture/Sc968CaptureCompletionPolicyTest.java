package net.runelite.client.plugins.airoutinecapture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Sc968CaptureCompletionPolicyTest
{
	@Test
	public void fishingActionNeedsPostActionTicks()
	{
		Sc968CaptureCompletionPolicy policy = new Sc968CaptureCompletionPolicy(5);
		policy.observeAction(100, "Net", "Fishing spot");
		assertFalse(policy.satisfied(104));
		assertTrue(policy.satisfied(105));
	}

	@Test
	public void unrelatedMenuActionDoesNotComplete()
	{
		Sc968CaptureCompletionPolicy policy = new Sc968CaptureCompletionPolicy(5);
		policy.observeAction(100, "Talk-to", "Banker");
		assertFalse(policy.satisfied(200));
	}
}
