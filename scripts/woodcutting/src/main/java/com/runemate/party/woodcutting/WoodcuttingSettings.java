import com.runemate.ui.setting.annotation.open.Setting;
import com.runemate.ui.setting.annotation.open.SettingsGroup;
import com.runemate.ui.setting.annotation.open.Option;

import com.runemate.game.api.script.framework.LoopingBot;

@SettingsGroup(name = "Woodcutting Settings")
public class WoodcutAndBurnBot extends LoopingBot {

    @Setting(name = "Tree Type", description = "Select the type of tree to chop")
    @Option(values = {"Tree", "Oak", "Willow"})
    private String selectedTree = "Tree";

    @Setting(name = "Action on Logs", description = "Choose what to do when inventory is full")
    @Option(values = {"Burn", "Drop"})
    private String logAction = "Burn";

    @Setting(name = "Loop Delay Min (ms)")
    private int delayMin = 300;

    @Setting(name = "Loop Delay Max (ms)")
    private int delayMax = 600;
