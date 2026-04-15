package dank.net.autoMiner;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AutoMiner extends JavaPlugin implements Listener {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final String MINER_HEADER = "[Mine]";
    private static final double DEFAULT_BASE_MINING_PROGRESS = 40.0D;
    private static final boolean WILL_BREAK_ANY = false;

    private final Set<AutoMinerDbRecord> miners = new HashSet<>();
    private final Map<Long, MiningState> miningStates = new HashMap<>();
    private DbManager dbManager;
    private BukkitTask miningTask;
    private double baseMiningProgress;
    private boolean willBreakAny = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPluginConfig();
        dbManager = new DbManager(this);
        dbManager.initialize();
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();

        for (final var world : getServer().getWorlds()) {
            for (final Chunk chunk : world.getLoadedChunks()) {
                loadMinerSignsFromChunk(chunk);
            }
        }

        startMiningTask();
        getLogger().info("Plugin has started.");
    }

    @Override
    public void onDisable() {
        if (this.miningTask != null) {
            this.miningTask.cancel();
            this.miningTask = null;
        }
    }

    public DbManager getDbManager() {
        return this.dbManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (!(block.getState() instanceof Sign sign)) return;

        final Component line0 = sign.getSide(Side.FRONT).line(0);
        if (!isMinerHeader(line0)) return;

        final var location = sign.getLocation();
        final var playerId = event.getPlayer().getUniqueId();
        final var dbRecord = getDbManager().getByLocationAndPlayerId(location, playerId);
        if (dbRecord.isEmpty()) return;

        miners.remove(dbRecord.get());
        clearMiningState(dbRecord.get());
        getDbManager().deleteById(dbRecord.get().getId());
        event.getPlayer().sendMessage("Miner has been removed.");
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        final Component line0 = event.line(0);
        if (!isMinerHeader(line0)) {
            return;
        }

        final var player = event.getPlayer();
        final Component pickType = event.line(1);
        final var minerType = AutoMinerDbRecord.MinerType.fromName(asPlainText(pickType));
        if (minerType.isEmpty()) {
            player.sendMessage("Invalid miner type!");
            return;
        }

        if (!player.hasPermission("autominer.place")) {
            player.sendMessage("You don't have permission to place auto miners!");
            return;
        }

        final var records = getDbManager().getByUsername(player.getName());
        final var count = records.size();

        var canPlace = PermissionsHandling.canPlaceAutoMiner(player, count, minerType.get());
        switch (canPlace) {
            case 1:
                player.sendMessage("You do not have permission to place auto miners.");
                getLogger().warning("Player " +  player.getName() + " tried to place an auto miner but does not have permission.");
                return;
            case 2:
                player.sendMessage("You are not allowed to place any more auto miners.");
                getLogger().info("Player " + player.getName() + " tried to place an auto miner but has reached their limit.");
                return;
            case 3:
                player.sendMessage("You are not allowed to place this kind of auto miner.");
                getLogger().info("Player " + player.getName() + " tried to place a " + minerType.get() + "miner but does not have permission.");
                return;
        }

        final var location = event.getBlock().getLocation();
        final var newRecord = new AutoMinerDbRecord(
                player.getName(),
                player.getUniqueId(),
                location,
                minerType.get()
        );

        final var dbRecord = getDbManager().create(newRecord);
        miners.add(dbRecord);
        event.getPlayer().sendMessage("Miner sign created.");
    }

    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent event) {
        loadMinerSignsFromChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        unloadMinerSignsFromChunk(event.getChunk());
    }

    private void loadMinerSignsFromChunk(final Chunk chunk) {
        var signs = getDbManager().getByChunk(chunk);
        miners.addAll(signs);
        if (signs.size() > 0) getLogger().info(signs.size() + " miners have been loaded in chunk " + chunk.getX() + " " + chunk.getZ() + ".");
    }

    private void unloadMinerSignsFromChunk(final Chunk chunk) {
        int before = miners.size();

        miners.removeIf(record -> {
            final var location = record.getLocation();
            final boolean shouldRemove = location != null
                    && location.getWorld() != null
                    && location.getWorld().equals(chunk.getWorld())
                    && location.getChunk().getX() == chunk.getX()
                    && location.getChunk().getZ() == chunk.getZ();

            if (shouldRemove) {
                clearMiningState(record);
            }

            return shouldRemove;
        });

        if (before != miners.size()) {
            getLogger().info(before - miners.size() + "miners have been unloaded in chunk " + chunk.getX() + " " + chunk.getZ() + ".");
        }
    }

    private void startMiningTask() {
        this.miningTask = getServer().getScheduler().runTaskTimer(this, this::tickAutoMiners, 1L, 1L);
    }

    private void registerCommands() {
        registerCommand(
                "autominer",
                "AutoMiner root command.",
                List.of("am"),
                new BasicCommand() {
                    @Override
                    public void execute(final CommandSourceStack source, final String[] args) {
                        handleAutoMinerCommand(source.getSender(), args);
                    }

                    @Override
                    public String permission() {
                        return "autominer.admin";
                    }

                    @Override
                    public Collection<String> suggest(final CommandSourceStack source, final String[] args) {
                        if (args.length <= 1) {
                            final String currentArg = args.length == 0 ? "" : args[0].toLowerCase();
                            return List.of("reload").stream()
                                    .filter(option -> option.startsWith(currentArg))
                                    .toList();
                        }

                        return List.of();
                    }
                }
        );
    }

    private void handleAutoMinerCommand(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /autominer reload");
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loadPluginConfig();
            sender.sendMessage("AutoMiner config reloaded.");
            return;
        }

        sender.sendMessage("Usage: /autominer reload");
    }

    private void tickAutoMiners() {
        for (final AutoMinerDbRecord miner : Set.copyOf(this.miners)) {
            tickMiner(miner);
        }
    }

    private void tickMiner(final AutoMinerDbRecord miner) {
        final Optional<Sign> sign = resolveMinerSign(miner);
        if (sign.isEmpty()) {
            this.miners.remove(miner);
            clearMiningState(miner);
            return;
        }

        final Optional<Block> targetBlock = resolveTargetBlock(sign.get());
        if (targetBlock.isEmpty()) {
            clearMiningState(miner);
            return;
        }

        final Block block = targetBlock.get();
        if (!canMineBlock(miner, block)) {
            clearMiningState(miner);
            return;
        }

        final MiningState miningState = getMiningState(miner, block);
        miningState.progress += getMiningProgressPerTick(miner);
        if (miningState.progress < getRequiredMiningProgress(block)) {
            return;
        }

        mineBlock(miner, block);
        clearMiningState(miner);
    }

    private Optional<Sign> resolveMinerSign(final AutoMinerDbRecord miner) {
        final var location = miner.getLocation();
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }

        final Block block = location.getBlock();
        if (!(block.getState() instanceof Sign sign)) {
            return Optional.empty();
        }

        return isMinerHeader(sign.getSide(Side.FRONT).line(0))
                ? Optional.of(sign)
                : Optional.empty();
    }

    private Optional<Block> resolveTargetBlock(final Sign sign) {
        final BlockFace facing = resolveSignFacing(sign.getBlock());
        if (facing == null) {
            return Optional.empty();
        }

        return Optional.of(sign.getBlock().getRelative(facing));
    }

    private BlockFace resolveSignFacing(final Block signBlock) {
        final BlockData blockData = signBlock.getBlockData();
        if (blockData instanceof Directional directional) {
            return directional.getFacing();
        }
        if (blockData instanceof Rotatable rotatable) {
            return rotatable.getRotation();
        }

        return null;
    }

    private boolean canMineBlock(final AutoMinerDbRecord miner, final Block targetBlock) {
        final Material type = targetBlock.getType();
        if (type == Material.BEDROCK || type == Material.BARRIER) {
            return false;
        }

        if (willBreakAny) return true;
        return miner.getMinerType().canMineBlock(targetBlock, miner.getMinerType());
    }

    private MiningState getMiningState(final AutoMinerDbRecord miner, final Block targetBlock) {
        final Long minerId = miner.getId();
        if (minerId == null) {
            throw new IllegalStateException("Miner must have an id before it can tick");
        }

        final MiningState existingState = this.miningStates.get(minerId);
        if (existingState != null && existingState.matches(targetBlock)) {
            return existingState;
        }

        final MiningState newState = new MiningState(targetBlock);
        this.miningStates.put(minerId, newState);
        return newState;
    }

    private void clearMiningState(final AutoMinerDbRecord miner) {
        final Long minerId = miner.getId();
        if (minerId != null) {
            this.miningStates.remove(minerId);
        }
    }

    private double getMiningProgressPerTick(final AutoMinerDbRecord miner) {
        return Math.max(0.1D, miner.getMinerSpeed());
    }

    private double getRequiredMiningProgress(final Block targetBlock) {
        return this.baseMiningProgress;
    }

    private void mineBlock(final AutoMinerDbRecord miner, final Block targetBlock) {
        targetBlock.breakNaturally();
    }

    private boolean isMinerHeader(final Component component) {
        return MINER_HEADER.equalsIgnoreCase(asPlainText(component));
    }

    private String asPlainText(final Component component) {
        return component == null ? "" : PLAIN_TEXT.serialize(component).trim();
    }

    private void loadPluginConfig() {
        this.baseMiningProgress = Math.max(
                0.1D,
                getConfig().getDouble("mining.base-progress", DEFAULT_BASE_MINING_PROGRESS)
        );

        getLogger().info("Config: base-progress = " + baseMiningProgress);

        this.willBreakAny = getConfig().getBoolean("mining.will-break-any", WILL_BREAK_ANY);

        getLogger().info("Config: will-break-any = " + willBreakAny);
    }

    private static final class MiningState {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final Material material;
        private double progress;

        private MiningState(final Block targetBlock) {
            final var world = targetBlock.getWorld();
            this.worldName = world.getName();
            this.x = targetBlock.getX();
            this.y = targetBlock.getY();
            this.z = targetBlock.getZ();
            this.material = targetBlock.getType();
            this.progress = 0.0D;
        }

        private boolean matches(final Block targetBlock) {
            return targetBlock.getWorld().getName().equals(this.worldName)
                    && targetBlock.getX() == this.x
                    && targetBlock.getY() == this.y
                    && targetBlock.getZ() == this.z
                    && targetBlock.getType() == this.material;
        }
    }
}
