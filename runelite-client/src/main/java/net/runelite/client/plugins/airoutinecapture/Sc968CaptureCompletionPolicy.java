package net.runelite.client.plugins.airoutinecapture;

import java.util.Locale;

/** Acceptance-only predicate: fishing observation followed by a post-action tick window. */
public final class Sc968CaptureCompletionPolicy
{
	private final int postActionTicks;
	private int fishingTick = -1;

	public Sc968CaptureCompletionPolicy(int postActionTicks)
	{
		if (postActionTicks < 1)
		{
			throw new IllegalArgumentException("postActionTicks");
		}
		this.postActionTicks = postActionTicks;
	}

	public void observeAction(int tick, String option, String target)
	{
		if (option != null && target != null
			&& (option.toLowerCase(Locale.ROOT).contains("net")
				|| option.toLowerCase(Locale.ROOT).contains("fish"))
			&& target.toLowerCase(Locale.ROOT).contains("fishing"))
		{
			fishingTick = tick;
		}
	}

	public void reset()
	{
		fishingTick = -1;
	}

	public boolean satisfied(int currentTick)
	{
		return fishingTick >= 0 && currentTick >= fishingTick + postActionTicks;
	}
}
