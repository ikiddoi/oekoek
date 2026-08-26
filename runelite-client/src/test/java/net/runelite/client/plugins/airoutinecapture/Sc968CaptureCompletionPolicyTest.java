package net.runelite.client.plugins.airoutinecapture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Sc968CaptureCompletionPolicyTest
{
	@Test
	void fishingActionNeedsPostActionTicks()
	{
		Sc968CaptureCompletionPolicy policy = new Sc968CaptureCompletionPolicy(5);
		policy.observeAction(100, "Net", "Fishing spot");
		assertFalse(policy.satisfied(104));
		assertTrue(policy.satisfied(105));
	}

	@Test
	void unrelatedMenuActionDoesNotComplete()
	{
		Sc968CaptureCompletionPolicy policy = new Sc968CaptureCompletionPolicy(5);
		policy.observeAction(100, "Talk-to", "Banker");
		assertFalse(policy.satisfied(200));
	}
}
