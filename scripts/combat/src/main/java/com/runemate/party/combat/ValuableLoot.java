package com.runemate.party.combat;

public enum ValuableLoot {
    // Common early game and free-to-play loot
    COWHIDE("Cowhide", 150),
    BONES("Bones", 50),
    BIG_BONES("Big bones", 300),
    IRON_ORE("Iron ore", 120),
    STEEL_BAR("Steel bar", 500),
    COINS("Coins", 1000), // Custom threshold

    // Runite and dragon-related loot
    RUNE_SCIMITAR("Rune scimitar", 15000),
    RUNE_FULL_HELM("Rune full helm", 20000),
    RUNE_KITESHIELD("Rune kiteshield", 30000),
    RUNE_AXE("Rune axe", 10000),
    RUNE_2H_SWORD("Rune 2h sword", 35000),
    DRAGON_MED_HELM("Dragon med helm", 50000),
    DRAGON_BONES("Dragon bones", 3000),
    LAVA_DRAGON_BONES("Lava dragon bones", 4000),

    // Herbs (commonly farmed via chaos druids or Aberrant Spectres)
    GUAM_LEAF("Guam leaf", 200),
    RANARR_WEED("Ranarr weed", 7000),
    SNAPDRAGON("Snapdragon", 8000),
    TORSTOL("Torstol", 9000),
    AVANTOE("Avantoe", 2000),
    KWUARM("Kwuarm", 3000),
    CADANTINE("Cadantine", 2500),
    LANTADYME("Lantadyme", 3500),
    DWARF_WEED("Dwarf weed", 4000),

    // Stackable loot
    FEATHER("Feather", 2),
    ARROW_SHAFT("Arrow shaft", 1),
    FIRE_RUNE("Fire rune", 4),
    CHAOS_RUNE("Chaos rune", 90),
    DEATH_RUNE("Death rune", 200),
    NATURE_RUNE("Nature rune", 180),
    LAW_RUNE("Law rune", 300),
    BLOOD_RUNE("Blood rune", 400),

    // Ensouled heads (used for prayer training)
    ENSOULED_GOBLIN_HEAD("Ensouled goblin head", 600),
    ENSOULED_DRAGON_HEAD("Ensouled dragon head", 5000),
    ENSOULED_GIANT_HEAD("Ensouled giant head", 1200),

    // Alchables
    BLACK_KNIFE("Black knife", 500),
    MITHRIL_SQ_SHIELD("Mithril sq shield", 1500),
    ADAMANT_FULL_HELM("Adamant full helm", 3000),
    ADAMANTITE_BAR("Adamantite bar", 2000),
    MITHRIL_BAR("Mithril bar", 800),
    DRAGON_LONGSWORD("Dragon longsword", 60000),

    // Miscellaneous loot
    MAGIC_LOGS("Magic logs", 1000),
    CLUE_SCROLL_EASY("Clue scroll (easy)", 0),
    CLUE_SCROLL_MEDIUM("Clue scroll (medium)", 0),
    CLUE_SCROLL_HARD("Clue scroll (hard)", 0);

    private final String itemName;
    private final int estimatedValue;

    ValuableLoot(String itemName, int estimatedValue) {
        this.itemName = itemName;
        this.estimatedValue = estimatedValue;
    }

    public String getItemName() {
        return itemName;
    }

    public int getEstimatedValue() {
        return estimatedValue;
    }

    public static int getEstimatedValueByName(String name) {
        for (ValuableLoot loot : values()) {
            if (loot.itemName.equalsIgnoreCase(name)) {
                return loot.estimatedValue;
            }
        }
        return 0;
    }
}
