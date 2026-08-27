package net.runelite.client.plugins.airoutinecapture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class RuneLiteCaptureSafetyTest {
  @Test public void adapterIsDisabledByDefaultAndSourceHasNoControlApiNames() throws Exception {
    assertFalse(new AiRoutineCaptureConfig() { }.enabled());
    Path source = Path.of("src/main/java/net/runelite/client/plugins/airoutinecapture/AiRoutineCapturePlugin.java");
    if (!Files.exists(source)) source = Path.of("runelite-client").resolve(source);
    String text = Files.readString(source);
    assertFalse(text.contains("invokeMenuAction"));
    assertFalse(text.contains("MouseEvent"));
    assertFalse(text.contains("KeyEvent"));
    assertTrue(text.contains("submitTicks"));
    assertTrue(text.contains("SC968_CAPTURE_STATUS_FILE"));
    assertTrue(text.contains("sc968-capture-status/v1"));
    assertTrue(text.contains("CaptureCompletion"));
  }
}
