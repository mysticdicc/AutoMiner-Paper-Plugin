package dank.net.autoMiner;

import org.bukkit.entity.Player;

public final class PermissionsHandling {
    private PermissionsHandling() {
    }

    private enum MinerLimitPlacePermission {
        LIMIT_1("autominer.limit.1", 1),
        LIMIT_2("autominer.limit.2", 2),
        LIMIT_3("autominer.limit.3", 3),
        LIMIT_4("autominer.limit.4", 4),
        LIMIT_5("autominer.limit.5", 5);

        private final String permission;
        private final int limit;

        MinerLimitPlacePermission(final String permission, final int limit) {
            this.permission = permission;
            this.limit = limit;
        }

        public String getPermission() {
            return this.permission;
        }

        public int getLimit() {
            return this.limit;
        }

        public static int getMinerLimit(final Player player) {
            int highest = 0;

            for (MinerLimitPlacePermission permission : values()) {
                if (player.hasPermission(permission.permission)) {
                    highest = Math.max(highest, permission.limit);
                }
            }

            return highest;
        }

        public static boolean playerBelowLimit(long current, Player player) {
            if (player.hasPermission("autominer.limit.unlimited")) return true;

            var limit = getMinerLimit(player);
            return current < limit;
        }
    }

    private enum MinerTypeLimitPermission {
        LIMIT_WOOD("autominer.type.wood", AutoMinerDbRecord.MinerType.WOOD),
        LIMIT_STONE("autominer.type.stone", AutoMinerDbRecord.MinerType.STONE),
        LIMIT_COPPER("autominer.type.copper", AutoMinerDbRecord.MinerType.COPPER),
        LIMIT_IRON("autominer.type.iron", AutoMinerDbRecord.MinerType.IRON),
        LIMIT_GOLD("autominer.type.gold",  AutoMinerDbRecord.MinerType.GOLD),
        LIMIT_DIAMOND("autominer.type.diamond", AutoMinerDbRecord.MinerType.DIAMOND);

        private final String permission;
        private final AutoMinerDbRecord.MinerType minerType;

        MinerTypeLimitPermission(final String permission, final AutoMinerDbRecord.MinerType minerType) {
            this.permission = permission;
            this.minerType = minerType;
        }

        public String getPermission() {
            return this.permission;
        }

        public AutoMinerDbRecord.MinerType getMinerType() {
            return this.minerType;
        }

        public static boolean canPlaceType (final Player player, AutoMinerDbRecord.MinerType minerType) {
            if (player.hasPermission("autominer.type.any")) return true;
            if (player.hasPermission("autominer.type.wood") && minerType == AutoMinerDbRecord.MinerType.WOOD) return true;
            if (player.hasPermission("autominer.type.stone") && minerType == AutoMinerDbRecord.MinerType.STONE) return true;
            if (player.hasPermission("autominer.type.copper") && minerType == AutoMinerDbRecord.MinerType.COPPER) return true;
            if (player.hasPermission("autominer.type.iron") && minerType == AutoMinerDbRecord.MinerType.IRON) return true;
            if (player.hasPermission("autominer.type.gold") && minerType == AutoMinerDbRecord.MinerType.GOLD) return true;
            if (player.hasPermission("autominer.type.diamond")  && minerType == AutoMinerDbRecord.MinerType.DIAMOND) return true;
            return false;
        }
    }

    public static int canPlaceAutoMiner(final Player player, final long currentMiners, AutoMinerDbRecord.MinerType minerType) {
        if (player.hasPermission("autominer.admin")) return 0;
        if (!player.hasPermission("autominer.place")) return 1;
        if (!MinerLimitPlacePermission.playerBelowLimit(currentMiners, player)) return 2;
        if (!MinerTypeLimitPermission.canPlaceType(player, minerType)) return 3;
        return 0;
    }
}
