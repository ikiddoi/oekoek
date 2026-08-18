package net.runelite.client.plugins.airoutinecapture;

import com.chromascape.contracts.capture.CaptureTick;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public final class RuneLiteTickMapper {
  public CaptureTick map(Client client, long tick) {
    JsonObject value = new JsonObject(); value.addProperty("tick", tick); value.addProperty("game_state", client.getGameState().name()); value.addProperty("world", client.getWorld());
    Player player = client.getLocalPlayer(); if (player != null && player.getWorldLocation() != null) { WorldPoint point = player.getWorldLocation(); JsonObject entry = new JsonObject(); entry.addProperty("x", point.getX()); entry.addProperty("y", point.getY()); entry.addProperty("plane", point.getPlane()); entry.addProperty("animation", player.getAnimation()); value.add("player", entry); }
    JsonArray inventory = new JsonArray(); ItemContainer container = client.getItemContainer(InventoryID.INVENTORY); if (container != null) for (int slot = 0; slot < container.getItems().length; slot++) { Item item = container.getItems()[slot]; if (item != null && item.getId() > 0) { JsonObject entry = new JsonObject(); entry.addProperty("slot", slot); entry.addProperty("item_id", item.getId()); entry.addProperty("quantity", item.getQuantity()); inventory.add(entry); } } value.add("inventory", inventory);
    List<NPC> npcs = new ArrayList<>(client.getNpcs()); npcs.removeIf(npc -> npc == null || npc.getName() == null || npc.getWorldLocation() == null); npcs.sort(Comparator.comparingInt(NPC::getIndex)); JsonArray nearby = new JsonArray(); for (NPC npc : npcs) { WorldPoint point = npc.getWorldLocation(); JsonObject entry = new JsonObject(); entry.addProperty("index", npc.getIndex()); entry.addProperty("id", npc.getId()); entry.addProperty("name", npc.getName()); entry.addProperty("x", point.getX()); entry.addProperty("y", point.getY()); nearby.add(entry); } value.add("nearby_npcs", nearby); return new CaptureTick(value);
  }
}
