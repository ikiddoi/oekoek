package net.runelite.client.plugins.airoutinecapture;

import com.chromascape.contracts.capture.CaptureCompletion;
import com.chromascape.contracts.capture.CaptureIdentity;
import com.chromascape.contracts.capture.CaptureSessionClient;
import com.chromascape.contracts.capture.CaptureSessionSnapshot;
import com.chromascape.contracts.capture.CaptureSessionState;
import com.chromascape.contracts.capture.CaptureStart;
import com.chromascape.contracts.capture.CaptureTransportConfig;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

@PluginDescriptor(
	name = "AI Routine Capture",
	description = "Disabled-by-default observation-only routine capture",
	tags = {"ai", "routine", "capture"},
	enabledByDefault = false
)
public final class AiRoutineCapturePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private AiRoutineCaptureConfig config;

	private final ExecutorService lifecycle = Executors.newSingleThreadExecutor(r ->
	{
		Thread thread = new Thread(r, "ai-routine-capture-lifecycle");
		thread.setDaemon(true);
		return thread;
	});
	private final ScheduledExecutorService statusWriter = Executors.newSingleThreadScheduledExecutor(r ->
	{
		Thread thread = new Thread(r, "ai-routine-capture-status");
		thread.setDaemon(true);
		return thread;
	});
	private final RuneLiteCaptureIdentityFactory identities = new RuneLiteCaptureIdentityFactory();
	private final RuneLiteTickMapper ticks = new RuneLiteTickMapper();
	private final RuneLiteActionMapper actions = new RuneLiteActionMapper();
	private final Sc968CaptureCompletionPolicy completionPolicy = new Sc968CaptureCompletionPolicy(5);
	private volatile CaptureSessionClient capture;
	private volatile boolean recording;
	private volatile boolean completionRequested;
	private volatile int lastTick;
	private volatile Path statusPath;

	@com.google.inject.Provides
	AiRoutineCaptureConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(AiRoutineCaptureConfig.class);
	}

	@Override
	protected void startUp()
	{
		String url = env("SC968_CHROMASCAPE_URL", config.baseUrl());
		String token = env("SC968_CAPTURE_TOKEN", "");
		String spool = env("SC968_CAPTURE_SPOOL", config.spoolPath());
		String configuredStatusPath = System.getenv("SC968_CAPTURE_STATUS_FILE");
		statusPath = configuredStatusPath == null || configuredStatusPath.isBlank()
			? null : Paths.get(configuredStatusPath).toAbsolutePath().normalize();
		capture = new CaptureSessionClient(CaptureTransportConfig.defaults(
			URI.create(url), () -> token, Paths.get(spool)));
		publishStatus();
		if (statusPath != null)
		{
			statusWriter.scheduleWithFixedDelay(this::publishStatus, 0, 250, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	protected void shutDown()
	{
		lifecycle.submit(() -> stopCapture(true));
		statusWriter.shutdownNow();
		lifecycle.shutdown();
		try
		{
			lifecycle.awaitTermination(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		CaptureSessionClient current = capture;
		if (current != null)
		{
			current.close();
		}
		publishStatus();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && enabled() && !recording)
		{
			lifecycle.submit(this::startCapture);
		}
		else if (event.getGameState() != GameState.LOGGED_IN && recording)
		{
			lifecycle.submit(() -> stopCapture(false));
		}
	}

	private void startCapture()
	{
		try
		{
			CaptureSessionClient current = capture;
			if (current == null)
			{
				return;
			}
			completionPolicy.reset();
			CaptureIdentity bound = current.createRun(identities.initial(
				env("SC968_TASK_KEY", "FISHING_TRAINING"),
				env("SC968_SETTINGS_HASH", ""),
				env("SC968_PROFILE_ID", ""),
				env("SC968_PROFILE_HASH", ""),
				env("SC968_CATALOG_HASH", ""),
				env("SC968_CLIENT_ID", "runelite-capture")));
			current.startSession(bound, new CaptureStart(client.getTickCount(), null, "CHROMASCAPE_WINDOW"));
			long deadline = System.nanoTime() + Duration.ofSeconds(config.startTimeoutSeconds()).toNanos();
			while (System.nanoTime() < deadline && current.state() == CaptureSessionState.STARTING)
			{
				current.status();
				Thread.sleep(100);
			}
			recording = current.state() == CaptureSessionState.RECORDING;
			completionRequested = false;
			lastTick = client.getTickCount();
			publishStatus();
		}
		catch (Exception ignored)
		{
			recording = false;
			publishStatus();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!recording || !enabled())
		{
			return;
		}
		lastTick = client.getTickCount();
		capture.submitTicks(List.of(ticks.map(client, lastTick)));
		if (!completionRequested && completionPolicy.satisfied(lastTick))
		{
			completionRequested = true;
			lifecycle.submit(() -> stopCapture(true));
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!recording || !enabled())
		{
			return;
		}
		// The welcome screen is dismissed after the capture window opens; that click is login
		// ceremony, not routine evidence, and recording it makes the graph unreplayable.
		if (RuneLiteActionMapper.isLoginCeremony(event.getParam1()))
		{
			return;
		}
		completionPolicy.observeAction(client.getTickCount(), event.getMenuOption(), event.getMenuTarget());
		capture.submitActions(List.of(actions.map(client, event)));
		publishStatus();
	}

	private void stopCapture(boolean complete)
	{
		CaptureSessionClient current = capture;
		if (current == null)
		{
			return;
		}
		if (!recording || !complete)
		{
			current.abort();
			recording = false;
			publishStatus();
			return;
		}
		try
		{
			CaptureSessionSnapshot snapshot = current.snapshot();
			current.complete(new CaptureCompletion(
				lastTick,
				snapshot.nextTickSequence() - 1,
				snapshot.nextActionSequence() - 1,
				null));
			long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
			while (System.nanoTime() < deadline && current.state() == CaptureSessionState.COMPLETING)
			{
				current.reconcile();
				Thread.sleep(100);
			}
			recording = false;
			publishStatus();
		}
		catch (Exception ignored)
		{
			publishStatus();
		}
	}

	public CaptureSessionState captureState()
	{
		return capture == null ? CaptureSessionState.IDLE : capture.state();
	}

	private boolean enabled()
	{
		String value = System.getenv("SC968_CAPTURE_ENABLED");
		return value == null || value.isBlank() ? config.enabled() : Boolean.parseBoolean(value);
	}

	private void publishStatus()
	{
		Path target = statusPath;
		CaptureSessionClient current = capture;
		if (target == null || current == null)
		{
			return;
		}
		try
		{
			CaptureSessionSnapshot snapshot = current.snapshot();
			JsonObject value = new JsonObject();
			value.addProperty("schema", "sc968-capture-status/v1");
			value.addProperty("state", snapshot.state().name());
			value.addProperty("run_id", snapshot.runId());
			value.addProperty("session_id", snapshot.sessionId());
			value.addProperty("next_tick_sequence", snapshot.nextTickSequence());
			value.addProperty("next_action_sequence", snapshot.nextActionSequence());
			value.addProperty("queued_events", snapshot.queuedEvents());
			value.addProperty("updated_at", java.time.Instant.now().toString());
			if (snapshot.failure() != null)
			{
				JsonObject failure = new JsonObject();
				failure.addProperty("code", snapshot.failure().code());
				failure.addProperty("http_status", snapshot.failure().httpStatus());
				failure.addProperty("retry_classification", snapshot.failure().retryClassification().name());
				value.add("failure", failure);
			}
			Path parent = target.getParent();
			if (parent != null)
			{
				Files.createDirectories(parent);
			}
			Path temporary = target.resolveSibling(target.getFileName() + ".part");
			Files.writeString(temporary, value.toString(), java.nio.charset.StandardCharsets.UTF_8,
				java.nio.file.StandardOpenOption.CREATE,
				java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
				java.nio.file.StandardOpenOption.WRITE);
			try
			{
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (java.nio.file.AtomicMoveNotSupportedException unsupported)
			{
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (Exception ignored)
		{
			// The sidecar is evidence-only and must never interfere with client callbacks.
		}
	}

	private static String env(String name, String fallback)
	{
		String value = System.getenv(name);
		return value == null || value.isBlank() ? fallback : value;
	}
}
