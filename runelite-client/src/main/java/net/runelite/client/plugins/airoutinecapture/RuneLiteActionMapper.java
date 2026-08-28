package net.runelite.client.plugins.airoutinecapture;

import com.chromascape.contracts.capture.CaptureAction;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;

public final class RuneLiteActionMapper {
  public CaptureAction map(Client client, MenuOptionClicked event) { JsonObject action = new JsonObject(); action.addProperty("tick", client.getTickCount()); action.addProperty("kind", "MENU_OPTION_CLICKED"); action.addProperty("option", event.getMenuOption()); action.addProperty("target", event.getMenuTarget()); action.addProperty("identifier", event.getId()); action.addProperty("menu_action", event.getMenuAction().name()); action.addProperty("param0", event.getParam0()); action.addProperty("param1", event.getParam1()); return new CaptureAction(action); }

  /**
   * True when the click is client login ceremony rather than routine evidence.
   *
   * <p>{@code GameState.LOGGED_IN} fires while the welcome screen ("CLICK HERE TO PLAY") is still
   * up, so the capture window is already open when the operator dismisses it and the dismissal
   * arrives as an ordinary menu click. Recording it corrupts everything downstream: the generated
   * graph's first ACTION becomes the welcome-screen click instead of the demonstrated interaction,
   * and a policy replay compares the model against the raw recorded action list, so any graph that
   * omits the click can no longer replay. The capture must observe the routine, not the login.
   */
  public static boolean isLoginCeremony(int componentId) {
    return WidgetUtil.componentToInterface(componentId) == InterfaceID.WELCOME_SCREEN;
  }
}
