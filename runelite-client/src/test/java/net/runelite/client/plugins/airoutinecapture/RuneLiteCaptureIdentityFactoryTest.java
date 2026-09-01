package net.runelite.client.plugins.airoutinecapture;

import static org.junit.Assert.assertEquals;

import com.chromascape.contracts.capture.CaptureIdentity;
import com.google.gson.JsonObject;
import org.junit.Test;

public class RuneLiteCaptureIdentityFactoryTest
{
	@Test
	public void includesConfiguredSourceProvenance()
	{
		CaptureIdentity identity = new RuneLiteCaptureIdentityFactory().initial(
			"FISHING_TRAINING",
			"sha256:0000000000000000000000000000000000000000000000000000000000000000",
			"profile",
			"sha256:1111111111111111111111111111111111111111111111111111111111111111",
			"sha256:2222222222222222222222222222222222222222222222222222222222222222",
			"runelite-human-capture",
			"srcsnap_0123456789abcdef01234567",
			"sha256:3333333333333333333333333333333333333333333333333333333333333333");

		JsonObject source = identity.json().getAsJsonObject("source");
		assertEquals("srcsnap_0123456789abcdef01234567", source.get("source_snapshot_id").getAsString());
		assertEquals(
			"sha256:3333333333333333333333333333333333333333333333333333333333333333",
			source.get("source_manifest_hash").getAsString());
	}
}
