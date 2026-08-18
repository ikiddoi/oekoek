package net.runelite.client.plugins.airoutinecapture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RuneLiteCaptureSafetyTest {
  @Test void adapterIsDisabledByDefaultAndSourceHasNoControlApiNames() throws Exception {
    assertFalse(new AiRoutineCaptureConfig() { }.enabled());
    Path source = Path.of("src/main/java/net/runelite/client/plugins/airoutinecapture/AiRoutineCapturePlugin.java");
    if (!Files.exists(source)) source = Path.of("runelite-client").resolve(source);
    String text = Files.readString(source);
    assertFalse(text.contains("invokeMenuAction"));
    assertFalse(text.contains("MouseEvent"));
    assertFalse(text.contains("KeyEvent"));
    assertTrue(text.contains("submitTicks"));
  }
}
