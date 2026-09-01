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
    assertTrue(text.contains("SC968_SOURCE_SNAPSHOT_ID"));
    assertTrue(text.contains("SC968_SOURCE_MANIFEST_HASH"));
    assertTrue(text.contains("current.state() == CaptureSessionState.STARTING"));
    assertTrue(text.contains("awaitRecording(current)"));
    assertTrue(text.contains("import net.runelite.client.eventbus.Subscribe;"));
    assertFalse(text.contains("import com.google.common.eventbus.Subscribe;"));
  }

  /**
   * The 2026-08-29 acceptance recorded the welcome screen's "CLICK HERE TO PLAY" click as the first
   * routine action, which made the generated graph unreplayable and would also have tripped the
   * shadow phase's "gameplay began before the shadow was observable" check.
   */
  @Test public void welcomeScreenDismissalIsNotRoutineEvidence() {
    assertTrue(RuneLiteActionMapper.isLoginCeremony((378 << 16) | 72));
    // World interactions carry no widget component; other interfaces stay routine evidence.
    assertFalse(RuneLiteActionMapper.isLoginCeremony(0));
    assertFalse(RuneLiteActionMapper.isLoginCeremony((149 << 16) | 0));
  }
  /**
   * The recording window must open when the harness says the demonstration begins, not at
   * LOGGED_IN, which fires while the welcome screen is up and several ticks before the operator can
   * act. Recording that gap put a menu dismissal and three steps toward the fishing spot into the
   * evidence, and would trip the shadow phase's check that gameplay had not begun yet.
   */
  @Test public void theRecordingWindowIsGatedOnTheRoutineNotOnLogin() throws Exception {
    Path source = Path.of("src/main/java/net/runelite/client/plugins/airoutinecapture/AiRoutineCapturePlugin.java");
    if (!Files.exists(source)) source = Path.of("runelite-client").resolve(source);
    String text = Files.readString(source);
    assertTrue(text.contains("SC968_CAPTURE_START_FILE"));
    assertTrue(text.contains("maybeStartCapture"));
    // The status sidecar has to distinguish "not logged in" from "logged in, awaiting the trigger".
    assertTrue(text.contains("logged_in"));
  }
}
