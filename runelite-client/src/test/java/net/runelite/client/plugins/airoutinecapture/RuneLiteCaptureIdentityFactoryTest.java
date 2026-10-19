package net.runelite.client.plugins.airoutinecapture;

import static org.junit.Assert.*;
import com.chromascape.contracts.capture.CaptureIdentity;
import org.junit.Test;

public class RuneLiteCaptureIdentityFactoryTest {
  private CaptureIdentity identity(String snapshot,String manifest) {
    return new RuneLiteCaptureIdentityFactory().initial("FISHING_TRAINING",
        "sha256:"+"0".repeat(64),"profile","sha256:"+"1".repeat(64),
        "sha256:"+"2".repeat(64),"human-capture",snapshot,manifest);
  }
  @Test public void humanCaptureDoesNotInventTaskSourceProvenance() {
    var json=identity(null,null).json();
    assertFalse(json.has("source"));
    assertEquals("RUNELITE_CAPTURE",json.getAsJsonObject("client").get("client_kind").getAsString());
    assertFalse(json.get("control_allowed").getAsBoolean());
  }
  @Test public void partialOrUnattestedTaskSourceClaimsRefuse() {
    for(String[] source:new String[][]{{"srcsnap_0123456789abcdef01234567",null},
        {null,"sha256:"+"3".repeat(64)},{"srcsnap_0123456789abcdef01234567","sha256:"+"3".repeat(64)}}) {
      try {
        identity(source[0],source[1]);
        fail("unattested task source must refuse");
      } catch (IllegalArgumentException expected) {
        assertEquals("RUNELITE_CAPTURE_TASK_SOURCE_UNSUPPORTED", expected.getMessage());
      }
    }
  }
}
