package dank.net.autoMiner;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DbManager {
    private static final String DATABASE_FILE_NAME = "autominers.db";

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS auto_miners (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                user_id TEXT NOT NULL,
                world_name TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                yaw REAL NOT NULL,
                pitch REAL NOT NULL,
                miner_type TEXT NOT NULL
            )
            """;

    private static final String CREATE_USERNAME_INDEX_SQL = """
            CREATE INDEX IF NOT EXISTS idx_auto_miners_username
            ON auto_miners(username)
            """;

    private static final String CREATE_LOCATION_INDEX_SQL = """
            CREATE INDEX IF NOT EXISTS idx_auto_miners_chunk_lookup
            ON auto_miners(world_name, x, z)
            """;

    private static final String CREATE_PLAYER_LOCATION_INDEX_SQL = """
            CREATE INDEX IF NOT EXISTS idx_auto_miners_player_location_lookup
            ON auto_miners(user_id, world_name, x, y, z)
            """;

    private static final String INSERT_SQL = """
            INSERT INTO auto_miners (username, user_id, world_name, x, y, z, yaw, pitch, miner_type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_ALL_SQL = """
            SELECT id, username, user_id, world_name, x, y, z, yaw, pitch, miner_type
            FROM auto_miners
            ORDER BY id
            """;

    private static final String SELECT_BY_ID_SQL = """
            SELECT id, username, user_id, world_name, x, y, z, yaw, pitch, miner_type
            FROM auto_miners
            WHERE id = ?
            """;

    private static final String SELECT_BY_USERNAME_SQL = """
            SELECT id, username, user_id, world_name, x, y, z, yaw, pitch, miner_type
            FROM auto_miners
            WHERE username = ?
            ORDER BY id
            """;

    private static final String SELECT_BY_CHUNK_SQL = """
            SELECT id, username, user_id, world_name, x, y, z, yaw, pitch, miner_type
            FROM auto_miners
            WHERE world_name = ?
              AND x >= ?
              AND x < ?
              AND z >= ?
              AND z < ?
            ORDER BY id
            """;

    private static final String SELECT_BY_LOCATION_AND_PLAYER_ID_SQL = """
            SELECT id, username, user_id, world_name, x, y, z, yaw, pitch, miner_type
            FROM auto_miners
            WHERE user_id = ?
              AND world_name = ?
              AND x = ?
              AND y = ?
              AND z = ?
            LIMIT 1
            """;

    private static final String UPDATE_SQL = """
            UPDATE auto_miners
            SET username = ?, user_id = ?, world_name = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?, miner_type = ?
            WHERE id = ?
            """;

    private static final String DELETE_BY_ID_SQL = """
            DELETE FROM auto_miners
            WHERE id = ?
            """;

    private final Path databasePath;
    private final String jdbcUrl;

    public DbManager(final JavaPlugin plugin) {
        this(plugin.getDataFolder().toPath().resolve(DATABASE_FILE_NAME));
    }

    public DbManager(final Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath");
        this.jdbcUrl = "jdbc:sqlite:" + this.databasePath.toAbsolutePath();
    }

    public void initialize() {
        try {
            final Path parent = this.databasePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Connection connection = this.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE_TABLE_SQL);
                statement.executeUpdate(CREATE_USERNAME_INDEX_SQL);
                statement.executeUpdate(CREATE_LOCATION_INDEX_SQL);
                statement.executeUpdate(CREATE_PLAYER_LOCATION_INDEX_SQL);
            }
        } catch (final IOException | SQLException exception) {
            throw new IllegalStateException("Failed to initialize SQLite database at " + this.databasePath, exception);
        }
    }

    public AutoMinerDbRecord create(final AutoMinerDbRecord record) {
        final AutoMinerDbRecord validatedRecord = this.requireValidRecord(record);

        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            this.bindRecord(statement, validatedRecord);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    validatedRecord.setId(generatedKeys.getLong(1));
                }
            }

            return validatedRecord;
        } catch (final SQLException exception) {
            throw new IllegalStateException("Failed to insert autominer record", exception);
        }
    }

    public List<AutoMinerDbRecord> getAll() {
        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            return this.mapRecords(resultSet);
        } catch (final SQLException exception) {
            throw new IllegalStateException("Failed to fetch autominer records", exception);
        }
    }

    public Optional<AutoMinerDbRecord> getById(final long id) {
        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(this.mapRecord(resultSet));
            }
        } catch (final SQLException exception) {
            throw new IllegalStateException("Failed to fetch autominer record with id " + id, exception);
        }
    }

    public List<AutoMinerDbRecord> getByUsername(final String username) {
        final String validatedUsername = this.requireUsername(username);

        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_USERNAME_SQL)) {
            statement.setString(1, validatedUsername);

            try (ResultSet resultSet = statement.executeQuery()) {
                return this.mapRecords(resultSet);
            }
        } catch (final SQLException exception) {
            throw new IllegalStateException("Failed to fetch autominer records for username " + validatedUsername, exception);
        }
    }

    public List<AutoMinerDbRecord> getByChunk(final Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        return this.getByChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    public List<AutoMinerDbRecord> getByChunk(final World world, final int chunkX, final int chunkZ) {
        final World validatedWorld = Objects.requireNonNull(world, "world");
        final double minX = chunkX * 16.0D;
        final double maxX = minX + 16.0D;
        final double minZ = chunkZ * 16.0D;
        final double maxZ = minZ + 16.0D;

        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_CHUNK_SQL)) {
            statement.setString(1, validatedWorld.getName());
            statement.setDouble(2, minX);
            statement.setDouble(3, maxX);
            statement.setDouble(4, minZ);
            statement.setDouble(5, maxZ);

            try (ResultSet resultSet = statement.executeQuery()) {
                return this.mapRecords(resultSet);
            }
        } catch (final SQLException exception) {
            throw new IllegalStateException(
                    "Failed to fetch autominer records for chunk "
                            + validatedWorld.getName() + ":" + chunkX + "," + chunkZ,
                    exception
            );
        }
    }

    public Optional<AutoMinerDbRecord> getByLocationAndPlayerId(final Location location, final UUID playerId) {
        final Location validatedLocation = this.requireLocation(location);
        final UUID validatedPlayerId = Objects.requireNonNull(playerId, "playerId");
        final World world = Objects.requireNonNull(validatedLocation.getWorld(), "location world must be loaded");

        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_LOCATION_AND_PLAYER_ID_SQL)) {
            statement.setString(1, validatedPlayerId.toString());
            statement.setString(2, world.getName());
            statement.setDouble(3, validatedLocation.getX());
            statement.setDouble(4, validatedLocation.getY());
            statement.setDouble(5, validatedLocation.getZ());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(this.mapRecord(resultSet));
            }
        } catch (final SQLException exception) {
            throw new IllegalStateException(
                    "Failed to fetch autominer record for player "
                            + validatedPlayerId + " at "
                            + world.getName() + " "
                            + validatedLocation.getBlockX() + ","
                            + validatedLocation.getBlockY() + ","
                            + validatedLocation.getBlockZ(),
                    exception
            );
        }
    }

    public boolean update(final AutoMinerDbRecord record) {
        final AutoMinerDbRecord validatedRecord = this.requireValidRecord(record);
        final Long id = Objects.requireNonNull(validatedRecord.getId(), "record id must be set before update");

        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            this.bindRecord(statement, validatedRecord);
            statement.setLong(10, id);
            return statement.executeUpdate() == 1;
        } catch (final SQLException exception) {
            throw new IllegalStateException("Failed to update autominer record with id " + id, exception);
        }
    }

    public boolean deleteById(final long id) {
        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (final SQLException exception) {
            throw new IllegalStateException("Failed to delete autominer record with id " + id, exception);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl);
    }

    private void bindRecord(final PreparedStatement statement, final AutoMinerDbRecord record) throws SQLException {
        final String username = this.requireUsername(record.getUsername());
        final UUID userId = Objects.requireNonNull(record.getUserId(), "userId must be set");
        final Location location = this.requireLocation(record.getLocation());
        final AutoMinerDbRecord.MinerType minerType = Objects.requireNonNull(record.getMinerType(), "minerType must be set");
        final World world = Objects.requireNonNull(location.getWorld(), "location world must be loaded");

        statement.setString(1, username);
        statement.setString(2, userId.toString());
        statement.setString(3, world.getName());
        statement.setDouble(4, location.getX());
        statement.setDouble(5, location.getY());
        statement.setDouble(6, location.getZ());
        statement.setFloat(7, location.getYaw());
        statement.setFloat(8, location.getPitch());
        statement.setString(9, minerType.name());
    }

    private AutoMinerDbRecord requireValidRecord(final AutoMinerDbRecord record) {
        Objects.requireNonNull(record, "record");
        this.requireUsername(record.getUsername());
        Objects.requireNonNull(record.getUserId(), "userId must be set");
        this.requireLocation(record.getLocation());
        Objects.requireNonNull(record.getMinerType(), "minerType must be set");
        return record;
    }

    private String requireUsername(final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must be set");
        }

        return username.trim();
    }

    private Location requireLocation(final Location location) {
        Objects.requireNonNull(location, "location must be set");
        Objects.requireNonNull(location.getWorld(), "location world must be loaded");
        return location;
    }

    private List<AutoMinerDbRecord> mapRecords(final ResultSet resultSet) throws SQLException {
        final List<AutoMinerDbRecord> records = new ArrayList<>();
        while (resultSet.next()) {
            records.add(this.mapRecord(resultSet));
        }
        return records;
    }

    private AutoMinerDbRecord mapRecord(final ResultSet resultSet) throws SQLException {
        final String worldName = resultSet.getString("world_name");
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException("World " + worldName + " is not loaded for autominer record " + resultSet.getLong("id"));
        }

        final Location location = new Location(
                world,
                resultSet.getDouble("x"),
                resultSet.getDouble("y"),
                resultSet.getDouble("z"),
                resultSet.getFloat("yaw"),
                resultSet.getFloat("pitch")
        );

        return new AutoMinerDbRecord(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                UUID.fromString(resultSet.getString("user_id")),
                location,
                AutoMinerDbRecord.MinerType.valueOf(resultSet.getString("miner_type"))
        );
    }
}
