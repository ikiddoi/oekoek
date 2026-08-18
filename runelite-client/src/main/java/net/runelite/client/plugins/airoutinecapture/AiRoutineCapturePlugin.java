package net.runelite.client.plugins.airoutinecapture;

import com.chromascape.contracts.capture.CaptureIdentity;
import com.chromascape.contracts.capture.CaptureSessionClient;
import com.chromascape.contracts.capture.CaptureSessionState;
import com.chromascape.contracts.capture.CaptureStart;
import com.chromascape.contracts.capture.CaptureCompletion;
import com.chromascape.contracts.capture.CaptureTransportConfig;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(name = "AI Routine Capture", description = "Disabled-by-default observation-only routine capture", tags = {"ai", "routine", "capture"}, enabledByDefault = false)
public final class AiRoutineCapturePlugin extends Plugin {
  @Inject private Client client;
  @Inject private AiRoutineCaptureConfig config;
  private final ExecutorService lifecycle = Executors.newSingleThreadExecutor(r -> { Thread thread = new Thread(r, "ai-routine-capture-lifecycle"); thread.setDaemon(true); return thread; });
  private final RuneLiteCaptureIdentityFactory identities = new RuneLiteCaptureIdentityFactory();
  private final RuneLiteTickMapper ticks = new RuneLiteTickMapper();
  private final RuneLiteActionMapper actions = new RuneLiteActionMapper();
  private volatile CaptureSessionClient capture;
  private volatile boolean recording;

  @Provides AiRoutineCaptureConfig provideConfig(ConfigManager manager) { return manager.getConfig(AiRoutineCaptureConfig.class); }
  @Override protected void startUp() { String url = env("SC968_CHROMASCAPE_URL", config.baseUrl()); String token = env("SC968_CAPTURE_TOKEN", ""); capture = new CaptureSessionClient(CaptureTransportConfig.defaults(URI.create(url), () -> token, Paths.get(env("SC968_CAPTURE_SPOOL", config.spoolPath())))); }
  @Override protected void shutDown() { lifecycle.submit(() -> { if (recording && capture != null) { try { capture.complete(new CaptureCompletion((int) client.getTickCount(), capture.snapshot().nextTickSequence() - 1, capture.snapshot().nextActionSequence() - 1, null)); } catch (Exception ignored) {} } }); lifecycle.shutdown(); try { lifecycle.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } if (capture != null) capture.close(); }
  @Subscribe public void onGameStateChanged(GameStateChanged event) { if (event.getGameState() == GameState.LOGGED_IN && config.enabled() && !recording) lifecycle.submit(this::startCapture); else if (event.getGameState() != GameState.LOGGED_IN && recording) lifecycle.submit(this::completeCapture); }
  private void startCapture() { try { CaptureIdentity bound = capture.createRun(identities.initial(env("SC968_CAPTURE_IDENTITY", ""))); capture.startSession(bound, new CaptureStart(client.getTickCount(), null, "CHROMASCAPE_WINDOW")); recording = capture.state() == CaptureSessionState.RECORDING; } catch (Exception ignored) { recording = false; } }
  private void completeCapture() { try { capture.complete(new CaptureCompletion((int) client.getTickCount(), capture.snapshot().nextTickSequence() - 1, capture.snapshot().nextActionSequence() - 1, null)); } catch (Exception ignored) {} recording = false; }
  @Subscribe public void onGameTick(GameTick event) { if (recording && config.enabled()) capture.submitTicks(List.of(ticks.map(client, client.getTickCount()))); }
  @Subscribe public void onMenuOptionClicked(MenuOptionClicked event) { if (recording && config.enabled()) capture.submitActions(List.of(actions.map(client, event))); }
  public CaptureSessionState captureState() { return capture == null ? CaptureSessionState.IDLE : capture.state(); }
  private static String env(String name, String fallback) { String value = System.getenv(name); return value == null || value.isBlank() ? fallback : value; }
}
