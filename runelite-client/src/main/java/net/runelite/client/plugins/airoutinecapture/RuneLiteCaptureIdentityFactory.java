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
  public CaptureIdentity initial(String taskKey, String settingsHash, String profileId,
      String profileHash, String catalogHash, String clientId, String sourceSnapshotId,
      String sourceManifestHash) {
    JsonObject identity = new JsonObject();
    identity.addProperty("schema", "current-slice-identity/v1");
    JsonObject task = new JsonObject();
    task.addProperty("task_key", required(taskKey, "task_key"));
    task.addProperty("settings_hash", required(settingsHash, "settings_hash"));
    task.addProperty("profile_id", required(profileId, "profile_id"));
    task.addProperty("profile_hash", required(profileHash, "profile_hash"));
    task.addProperty("catalog_hash", required(catalogHash, "catalog_hash"));
    identity.add("task", task);
    JsonObject client = new JsonObject();
    client.addProperty("client_id", required(clientId, "client_id")); client.addProperty("client_kind", "RUNELITE_CAPTURE"); client.addProperty("client_build_id", RuneLiteProperties.getCommit() == null ? RuneLiteProperties.getVersion() : RuneLiteProperties.getCommit()); client.addProperty("artifact_digest", artifactDigest()); client.addProperty("adapter_contract", "runelite-capture/v1"); client.addProperty("domain", "RUNELITE");
    identity.add("client", client);
    JsonObject source = new JsonObject();
    source.addProperty("source_snapshot_id", required(sourceSnapshotId, "source_snapshot_id"));
    source.addProperty("source_manifest_hash", required(sourceManifestHash, "source_manifest_hash"));
    identity.add("source", source);
    identity.addProperty("control_allowed", false);
    return CaptureIdentity.forCreation(JsonCaptureCodec.encode(identity));
  }
  private static String required(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException("IDENTITY_" + field.toUpperCase() + "_REQUIRED"); return value; }
  public String artifactDigest() {
    try {
      Path location = Path.of(RuneLiteCaptureIdentityFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI()); MessageDigest digest = MessageDigest.getInstance("SHA-256");
      if (Files.isRegularFile(location)) { try (InputStream input = Files.newInputStream(location)) { byte[] buffer = new byte[8192]; int count; while ((count = input.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count); } }
      else digest.update(location.toAbsolutePath().normalize().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder value = new StringBuilder("sha256:"); for (byte b : digest.digest()) value.append(String.format("%02x", b)); return value.toString();
    } catch (Exception e) { throw new IllegalStateException("RUNNING_ARTIFACT_DIGEST_UNAVAILABLE", e); }
  }
}
