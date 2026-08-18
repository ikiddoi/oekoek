package net.runelite.client.plugins.airoutinecapture;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("airoutinecapture")
public interface AiRoutineCaptureConfig extends Config {
  @ConfigItem(keyName = "enabled", name = "Capture enabled", description = "Observe and upload read-only routine events")
  default boolean enabled() { return false; }
  @ConfigItem(keyName = "baseUrl", name = "ChromaScape URL", description = "Capture API base URL")
  default String baseUrl() { return "http://127.0.0.1:8090"; }
  @ConfigItem(keyName = "spoolPath", name = "Capture spool path", description = "Durable local capture spool")
  default String spoolPath() { return System.getProperty("user.home") + "/.chromascape/capture-spool"; }
}
