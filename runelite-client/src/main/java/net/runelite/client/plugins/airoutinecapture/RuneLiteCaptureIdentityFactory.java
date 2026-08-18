package net.runelite.client.plugins.airoutinecapture;

import com.chromascape.contracts.capture.CaptureIdentity;
import com.chromascape.contracts.capture.JsonCaptureCodec;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import net.runelite.client.RuneLiteProperties;

public final class RuneLiteCaptureIdentityFactory {
  public CaptureIdentity initial(String configuredJson) {
    if (configuredJson == null || configuredJson.isBlank()) throw new IllegalArgumentException("capture identity is required");
    JsonObject identity = JsonCaptureCodec.parse(configuredJson).getAsJsonObject();
    JsonObject client = identity.getAsJsonObject("client");
    client.addProperty("client_kind", "RUNELITE_CAPTURE"); client.addProperty("adapter_contract", "runelite-capture/v1"); client.addProperty("domain", "RUNELITE");
    client.addProperty("client_build_id", RuneLiteProperties.getCommit() == null ? RuneLiteProperties.getVersion() : RuneLiteProperties.getCommit()); client.addProperty("artifact_digest", artifactDigest());
    identity.remove("run"); identity.remove("candidate"); identity.addProperty("control_allowed", false);
    return CaptureIdentity.forCreation(JsonCaptureCodec.encode(identity));
  }
  public String artifactDigest() {
    try {
      Path location = Path.of(RuneLiteCaptureIdentityFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI()); MessageDigest digest = MessageDigest.getInstance("SHA-256");
      if (Files.isRegularFile(location)) { try (InputStream input = Files.newInputStream(location)) { byte[] buffer = new byte[8192]; int count; while ((count = input.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count); } }
      else digest.update(location.toAbsolutePath().normalize().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder value = new StringBuilder("sha256:"); for (byte b : digest.digest()) value.append(String.format("%02x", b)); return value.toString();
    } catch (Exception e) { throw new IllegalStateException("RUNNING_ARTIFACT_DIGEST_UNAVAILABLE", e); }
  }
}
