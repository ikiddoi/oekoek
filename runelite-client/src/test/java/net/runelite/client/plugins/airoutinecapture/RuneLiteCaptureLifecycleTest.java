package net.runelite.client.plugins.airoutinecapture;

import com.chromascape.contracts.capture.CaptureSessionClient;
import com.chromascape.contracts.capture.CaptureSessionSnapshot;
import com.chromascape.contracts.capture.CaptureSessionState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Actual plugin callbacks/lifecycle queue with no network or game input. */
public class RuneLiteCaptureLifecycleTest
{
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private AiRoutineCapturePlugin plugin;
	private CaptureSessionClient capture;
	private ExecutorService lifecycle;
	private final AtomicReference<CaptureSessionState> state =
		new AtomicReference<>(CaptureSessionState.RECORDING);

	@Before public void setUp() throws Exception
	{
		plugin = new AiRoutineCapturePlugin();
		capture = mock(CaptureSessionClient.class);
		when(capture.state()).thenAnswer(ignored -> state.get());
		when(capture.snapshot()).thenAnswer(ignored -> new CaptureSessionSnapshot(
			state.get(), "run-test", "capture-test", 2, 2, 0, null));
		doAnswer(ignored -> { state.set(CaptureSessionState.ABORTED); return null; })
			.when(capture).abort();
		set("capture", capture);
		set("config", new AiRoutineCaptureConfig() { });
		set("recording", true);
		set("loggedIn", true);
		lifecycle = (ExecutorService) field("lifecycle").get(plugin);
	}

	@After public void cleanUp() throws Exception
	{
		lifecycle.shutdownNow();
		((ExecutorService) field("statusWriter").get(plugin)).shutdownNow();
		assertTrue(lifecycle.awaitTermination(5, TimeUnit.SECONDS));
	}

	@Test public void areaLoadingPreservesRecordingAndLoginThenResumes() throws Exception
	{
		change(GameState.LOADING);
		awaitQueue();
		assertEquals(CaptureSessionState.RECORDING, capture.state());
		assertTrue((boolean) field("loggedIn").get(plugin));
		change(GameState.LOGGED_IN);
		awaitQueue();
		assertEquals(CaptureSessionState.RECORDING, capture.state());
		verify(capture, never()).abort();
		verify(capture, never()).createRun(any());
	}

	@Test public void initialLoadingDoesNotInventAuthenticatedLogin() throws Exception
	{
		set("recording", false);
		set("loggedIn", false);
		state.set(CaptureSessionState.IDLE);
		change(GameState.LOADING);
		awaitQueue();
		assertFalse((boolean) field("loggedIn").get(plugin));
		assertEquals(CaptureSessionState.IDLE, capture.state());
		verify(capture, never()).createRun(any());
	}

	@Test public void logoutStillInterruptsCapture() throws Exception { assertInterrupted(GameState.LOGIN_SCREEN); }
	@Test public void connectionLossStillInterruptsCapture() throws Exception { assertInterrupted(GameState.CONNECTION_LOST); }
	@Test public void worldHopStillInterruptsCapture() throws Exception { assertInterrupted(GameState.HOPPING); }

	@Test public void loadingDoesNotPublishPartialWorldTicksOrMenuEvents() throws Exception
	{
		set("config", new AiRoutineCaptureConfig() { @Override public boolean enabled() { return true; } });
		change(GameState.LOADING);
		awaitQueue();
		plugin.onGameTick(new GameTick());
		plugin.onMenuOptionClicked(mock(MenuOptionClicked.class));
		verify(capture, never()).submitTicks(anyList());
		verify(capture, never()).submitActions(anyList());
		assertEquals(CaptureSessionState.RECORDING, capture.state());
	}

	@Test public void waitingCaptureDoesNotStartDuringAreaLoading() throws Exception
	{
		set("config", new AiRoutineCaptureConfig() { @Override public boolean enabled() { return true; } });
		set("recording", false);
		state.set(CaptureSessionState.IDLE);
		change(GameState.LOADING);
		java.lang.reflect.Method cadence = AiRoutineCapturePlugin.class.getDeclaredMethod("onStatusTick");
		cadence.setAccessible(true);
		cadence.invoke(plugin);
		awaitQueue();
		verify(capture, never()).createRun(any());
		assertEquals(CaptureSessionState.IDLE, capture.state());
	}

	@Test public void shutdownDoesNotDowngradeCompletedCapture() throws Exception
	{
		state.set(CaptureSessionState.COMPLETED);
		set("recording", false);
		plugin.shutDown();
		assertEquals(CaptureSessionState.COMPLETED, capture.state());
		verify(capture, never()).abort();
		verify(capture).close();
	}

	@Test public void queuedInterruptionDoesNotDowngradeCompletedCapture() throws Exception
	{
		CountDownLatch release = new CountDownLatch(1);
		lifecycle.submit(() -> { release.await(5, TimeUnit.SECONDS); return null; });
		try
		{
			change(GameState.LOGIN_SCREEN);
			state.set(CaptureSessionState.COMPLETED);
			set("recording", false);
		}
		finally { release.countDown(); }
		awaitQueue();
		assertEquals(CaptureSessionState.COMPLETED, capture.state());
		verify(capture, never()).abort();
	}

	@Test public void statusDistinguishesAreaLoadingFromLoginBoundaryInterruption() throws Exception
	{
		Path status = folder.getRoot().toPath().resolve("status.json");
		set("statusPath", status);
		change(GameState.LOADING);
		awaitQueue();
		JsonObject loading = new JsonParser().parse(Files.readString(status)).getAsJsonObject();
		assertTrue("status must report the observed game state", loading.has("game_state"));
		assertEquals("LOADING", loading.get("game_state").getAsString());
		assertEquals("RECORDING", loading.get("state").getAsString());
		assertTrue(loading.get("logged_in").getAsBoolean());
		change(GameState.LOGIN_SCREEN);
		awaitQueue();
		JsonObject stopped = new JsonParser().parse(Files.readString(status)).getAsJsonObject();
		assertEquals("ABORTED", stopped.get("state").getAsString());
		assertEquals("GAME_STATE_LOGIN_SCREEN", stopped.get("last_stop_reason").getAsString());
	}

	private void assertInterrupted(GameState gameState) throws Exception
	{
		change(gameState);
		awaitQueue();
		assertEquals(CaptureSessionState.ABORTED, capture.state());
		assertFalse((boolean) field("loggedIn").get(plugin));
		verify(capture).abort();
	}

	private void change(GameState gameState)
	{
		GameStateChanged event = new GameStateChanged();
		event.setGameState(gameState);
		plugin.onGameStateChanged(event);
	}

	private void awaitQueue() throws Exception { lifecycle.submit(() -> { }).get(5, TimeUnit.SECONDS); }
	private void set(String name, Object value) throws Exception { field(name).set(plugin, value); }
	private Field field(String name) throws Exception
	{
		Field field = AiRoutineCapturePlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
}
