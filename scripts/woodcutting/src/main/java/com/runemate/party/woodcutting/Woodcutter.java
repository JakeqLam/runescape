import com.runemate.game.api.client.embeddable.EmbeddableUI;
import com.runemate.game.api.script.framework.LoopingBot;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.framework.listeners.InventoryListener;
import com.runemate.game.api.script.framework.listeners.events.ItemEvent;
import com.runemate.game.api.script.framework.tree.TreeBot;
import com.runemate.game.api.script.framework.tree.TreeTask;

import static com.runemate.game.api.hybrid.local.hud.interfaces.Inventory.getItems;

public class WoodcutAndBurnBot extends LoopingBot {

    @Override
    public void onStart(String... args) {
        setLoopDelay(300, 600);
        getLogger().info("Woodcutting + Firemaking bot started!");
    }

    @Override
    public void onLoop() {
        // Check if inventory is full
        if (Inventory.isFull()) {
            burnLogs();
            return;
        }

        // Look for a nearby tree
        GameObject tree = GameObjects.newQuery().names("Tree").actions("Chop down").results().nearest();
        if (tree != null && tree.interact("Chop down")) {
            getLogger().info("Chopping tree...");
            sleepUntil(() -> !tree.isVisible() || !tree.isValid() || Inventory.isFull(), 10000);
        } else {
            getLogger().info("No tree found. Waiting...");
            sleep(1000);
        }
    }

    private void burnLogs() {
        getLogger().info("Burning logs...");
        var tinderbox = Inventory.getItems("Tinderbox").first();
        var logs = Inventory.getItems("Logs");

        if (tinderbox != null && logs != null && !logs.isEmpty()) {
            for (var log : logs) {
                if (log != null && tinderbox.interact("Use")) {
                    sleepUntil(() -> Inventory.isItemSelected(), 1000);
                    if (log.interact("Use")) {
                        sleepUntil(() -> !log.isValid(), 5000); // Wait until the log disappears (fire started)
                        sleep(Random.nextInt(1800, 2400)); // Let the animation play out
                    }
                }
            }
        } else {
            getLogger().warn("Missing tinderbox or logs.");
        }
    }
}
