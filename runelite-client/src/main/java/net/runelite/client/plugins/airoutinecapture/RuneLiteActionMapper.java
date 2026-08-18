package net.runelite.client.plugins.airoutinecapture;

import com.chromascape.contracts.capture.CaptureAction;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;

public final class RuneLiteActionMapper {
  public CaptureAction map(Client client, MenuOptionClicked event) { JsonObject action = new JsonObject(); action.addProperty("tick", client.getTickCount()); action.addProperty("kind", "MENU_OPTION_CLICKED"); action.addProperty("option", event.getMenuOption()); action.addProperty("target", event.getMenuTarget()); action.addProperty("identifier", event.getId()); action.addProperty("menu_action", event.getMenuAction().name()); action.addProperty("param0", event.getParam0()); action.addProperty("param1", event.getParam1()); return new CaptureAction(action); }
}
