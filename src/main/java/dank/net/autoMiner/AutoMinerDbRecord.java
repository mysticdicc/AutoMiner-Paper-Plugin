package dank.net.autoMiner;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;

public final class AutoMinerDbRecord {
    private Long id;
    private String username;
    private UUID userId;
    private Location location;
    private MinerType minerType;

    public AutoMinerDbRecord() {
    }

    public AutoMinerDbRecord(
            final Long id,
            final String username,
            final UUID userId,
            final Location location,
            final MinerType minerType
    ) {
        this.id = id;
        this.username = username;
        this.userId = userId;
        this.location = location;
        this.minerType = minerType;
    }

    public AutoMinerDbRecord(
            final String username,
            final UUID userId,
            final Location location,
            final MinerType minerType
    ) {
        this(null, username, userId, location, minerType);
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(final Location location) {
        this.location = location;
    }

    public MinerType getMinerType() {
        return this.minerType;
    }

    public void setMinerType(final MinerType minerType) {
        this.minerType = minerType;
    }

    public int getMinerSpeed() {
        return Objects.requireNonNull(this.minerType, "minerType must be set").getSpeed();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AutoMinerDbRecord that)) {
            return false;
        }

        if (this.id != null && that.id != null) {
            return this.id.equals(that.id);
        }

        return Objects.equals(this.username, that.username)
                && Objects.equals(this.userId, that.userId)
                && Objects.equals(this.location, that.location)
                && this.minerType == that.minerType;
    }

    @Override
    public int hashCode() {
        if (this.id != null) {
            return Objects.hash(this.id);
        }

        return Objects.hash(this.username, this.userId, this.location, this.minerType);
    }

    public enum MinerType {
        WOOD(1),
        STONE(2),
        COPPER(3),
        IRON(4),
        GOLD(5),
        DIAMOND(6);

        private final int speed;

        MinerType(final int speed) {
            this.speed = speed;
        }

        public int getSpeed() {
            return this.speed;
        }

        public static Optional<MinerType> fromName(final String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }

            final String normalized = value.trim().toUpperCase(Locale.ROOT);
            for (final MinerType minerType : values()) {
                if (minerType.name().equals(normalized)) {
                    return Optional.of(minerType);
                }
            }

            return Optional.empty();
        }

        public boolean canMineBlock(final Block targetBlock, AutoMinerDbRecord.MinerType minerType) {
            var blockType = targetBlock.getType();
            List<Material> materials = new ArrayList<>();

            if (minerType == MinerType.WOOD) materials = woodTypes;
            if (minerType == MinerType.STONE) materials = stoneTypes;
            if (minerType == MinerType.COPPER) materials = copperTypes;
            if (minerType == MinerType.IRON) materials = ironTypes;
            if (minerType == MinerType.GOLD) materials = goldTypes;
            if (minerType == MinerType.DIAMOND) materials = diamondTypes;

            return materials.contains(blockType);
        }
    }

    static private final List<Material> woodTypes = List.of(
        Material.STONE,
        Material.COBBLESTONE,
        Material.DEEPSLATE,
        Material.NETHERRACK,
        Material.COAL_ORE,
        Material.COAL_BLOCK,
        Material.DEEPSLATE_COAL_ORE,
        Material.GLOWSTONE,
        Material.NETHER_QUARTZ_ORE,
        Material.QUARTZ_BLOCK
    );

    static private final List<Material> stoneTypes = List.of(
        Material.COPPER_ORE,
        Material.IRON_ORE,
        Material.LAPIS_ORE,
        Material.COPPER_BLOCK,
        Material.IRON_BLOCK,
        Material.LAPIS_BLOCK,
        Material.DEEPSLATE_COPPER_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.NETHER_BRICK,
        Material.SEA_LANTERN,
        Material.STONE,
        Material.COBBLESTONE,
        Material.DEEPSLATE,
        Material.NETHERRACK,
        Material.COAL_ORE,
        Material.COAL_BLOCK,
        Material.DEEPSLATE_COAL_ORE,
        Material.GLOWSTONE
    );

    static private final List<Material> copperTypes = List.of(
        Material.COPPER_ORE,
        Material.IRON_ORE,
        Material.LAPIS_ORE,
        Material.COPPER_BLOCK,
        Material.IRON_BLOCK,
        Material.LAPIS_BLOCK,
        Material.DEEPSLATE_COPPER_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.NETHER_BRICK,
        Material.SEA_LANTERN,
        Material.STONE,
        Material.COBBLESTONE,
        Material.DEEPSLATE,
        Material.NETHERRACK,
        Material.COAL_ORE,
        Material.COAL_BLOCK,
        Material.DEEPSLATE_COAL_ORE,
        Material.GLOWSTONE
    );

    static private final List<Material> ironTypes = List.of(
        Material.GOLD_ORE,
        Material.REDSTONE_ORE,
        Material.EMERALD_ORE,
        Material.DIAMOND_ORE,
        Material.GOLD_BLOCK,
        Material.REDSTONE_BLOCK,
        Material.EMERALD_BLOCK,
        Material.DIAMOND_BLOCK,
        Material.DEEPSLATE_GOLD_ORE,
        Material.DEEPSLATE_REDSTONE_ORE,
        Material.DEEPSLATE_EMERALD_ORE,
        Material.DEEPSLATE_DIAMOND_ORE,
        Material.NETHER_GOLD_ORE,
        Material.COPPER_ORE,
        Material.IRON_ORE,
        Material.LAPIS_ORE,
        Material.COPPER_BLOCK,
        Material.IRON_BLOCK,
        Material.LAPIS_BLOCK,
        Material.DEEPSLATE_COPPER_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.NETHER_BRICK,
        Material.SEA_LANTERN,
        Material.STONE,
        Material.COBBLESTONE,
        Material.DEEPSLATE,
        Material.NETHERRACK,
        Material.COAL_ORE,
        Material.COAL_BLOCK,
        Material.DEEPSLATE_COAL_ORE,
        Material.GLOWSTONE
    );

    static private final List<Material> goldTypes = List.of(
        Material.STONE,
        Material.COBBLESTONE,
        Material.DEEPSLATE,
        Material.NETHERRACK,
        Material.COAL_ORE,
        Material.COAL_BLOCK,
        Material.DEEPSLATE_COAL_ORE,
        Material.GLOWSTONE
    );

    static private final List<Material> diamondTypes = List.of(
        Material.GOLD_ORE,
        Material.REDSTONE_ORE,
        Material.EMERALD_ORE,
        Material.DIAMOND_ORE,
        Material.GOLD_BLOCK,
        Material.REDSTONE_BLOCK,
        Material.EMERALD_BLOCK,
        Material.DIAMOND_BLOCK,
        Material.DEEPSLATE_GOLD_ORE,
        Material.DEEPSLATE_REDSTONE_ORE,
        Material.DEEPSLATE_EMERALD_ORE,
        Material.DEEPSLATE_DIAMOND_ORE,
        Material.NETHER_GOLD_ORE,
        Material.COPPER_ORE,
        Material.IRON_ORE,
        Material.LAPIS_ORE,
        Material.COPPER_BLOCK,
        Material.IRON_BLOCK,
        Material.LAPIS_BLOCK,
        Material.DEEPSLATE_COPPER_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.NETHER_BRICK,
        Material.SEA_LANTERN,
        Material.STONE,
        Material.COBBLESTONE,
        Material.DEEPSLATE,
        Material.NETHERRACK,
        Material.COAL_ORE,
        Material.COAL_BLOCK,
        Material.DEEPSLATE_COAL_ORE,
        Material.GLOWSTONE
    );
}
