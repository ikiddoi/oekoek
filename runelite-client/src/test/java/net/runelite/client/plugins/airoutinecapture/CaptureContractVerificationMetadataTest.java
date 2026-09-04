package net.runelite.client.plugins.airoutinecapture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Keeps the checked-in dependency-verification hash tied to the clean Contracts artifact used by
 * the SC-968 native gate. A stale hash can otherwise pass source review and fail only at the
 * RuneLite compile boundary after a fresh worktree is created.
 */
public class CaptureContractVerificationMetadataTest
{
	@Test
	public void verificationMetadataMatchesAcceptanceContractArtifact() throws Exception
	{
		String repository = System.getenv("SC968_MAVEN_REPO");
		assumeTrue("SC968_MAVEN_REPO is only available in the integrated acceptance", repository != null && !repository.isBlank());

		Path root = projectRoot();
		Properties lock = new Properties();
		try (InputStream input = Files.newInputStream(root.resolve("capture-contract.lock")))
		{
			lock.load(input);
		}
		String version = lock.getProperty("module_version");
		assertTrue("capture-contract.lock must contain module_version", version != null && !version.isBlank());

		String artifactName = "ai-routine-capture-contract-" + version + ".jar";
		Path artifact = Path.of(repository, "com", "chromascape", "contracts", "ai-routine-capture-contract", version, artifactName);
		assertTrue("acceptance Maven repository is missing " + artifactName, Files.isRegularFile(artifact));

		String metadata = Files.readString(root.resolve("gradle/verification-metadata.xml"));
		Pattern entryPattern = Pattern.compile(
			"(?s)<component group=\\\"com\\.chromascape\\.contracts\\\" name=\\\"ai-routine-capture-contract\\\" version=\\\""
				+ Pattern.quote(version)
				+ "\\\">.*?<artifact name=\\\""
				+ Pattern.quote(artifactName)
				+ "\\\">.*?<sha256 value=\\\"([0-9a-f]{64})\\\""
		);
		Matcher matcher = entryPattern.matcher(metadata);
		assertTrue("verification metadata has no exact capture-contract entry", matcher.find());
		assertEquals(matcher.group(1), sha256(artifact));
	}

	private static Path projectRoot()
	{
		Path current = Path.of("").toAbsolutePath();
		while (current != null && !Files.isRegularFile(current.resolve("capture-contract.lock")))
		{
			current = current.getParent();
		}
		if (current == null)
		{
			throw new IllegalStateException("Could not locate capture-contract.lock from the test working directory");
		}
		return current;
	}

	private static String sha256(Path path) throws Exception
	{
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
		StringBuilder result = new StringBuilder(digest.length * 2);
		for (byte value : digest)
		{
			result.append(String.format("%02x", value & 0xff));
		}
		return result.toString();
	}
}
