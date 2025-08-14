package eu.kotori.justTeams.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class DatabaseStorage implements IDataStorage {

    private final JustTeams plugin;
    private HikariDataSource hikari;
    private final String storageType;

    public DatabaseStorage(JustTeams plugin) {
        this.plugin = plugin;
        this.storageType = plugin.getConfig().getString("storage.type", "h2").toLowerCase();
    }

    @Override
    public boolean init() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("DonutTeams-Pool");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);

        boolean useMySQL = storageType.equals("mysql") && plugin.getConfig().getBoolean("storage.mysql.enabled", false);

        if (useMySQL) {
            plugin.getLogger().info("Attempting to connect to MySQL database...");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&serverTimezone=UTC",
                    plugin.getConfig().getString("storage.mysql.host"),
                    plugin.getConfig().getInt("storage.mysql.port"),
                    plugin.getConfig().getString("storage.mysql.database"));
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(plugin.getConfig().getString("storage.mysql.username"));
            config.setPassword(plugin.getConfig().getString("storage.mysql.password"));
        } else {
            plugin.getLogger().info("MySQL not enabled or configured. Falling back to H2 file-based storage.");
            File dataFolder = new File(plugin.getDataFolder(), "data");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            config.setDriverClassName("org.h2.Driver");
            config.setJdbcUrl("jdbc:h2:" + dataFolder.getAbsolutePath() + "/teams;MODE=MySQL;AUTO_SERVER=TRUE");
        }

        try {
            this.hikari = new HikariDataSource(config);
            runMigrationsAndSchemaChecks();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database connection pool for " + storageType + "!");
            plugin.getLogger().severe("Error: " + e.getMessage());
            return false;
        }
    }

    private void runMigrationsAndSchemaChecks() throws SQLException {
        plugin.getLogger().info("Verifying database schema...");
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS `donut_teams` (" +
                    "`id` INT AUTO_INCREMENT, " +
                    "`name` VARCHAR(16) NOT NULL UNIQUE, " +
                    "`tag` VARCHAR(6) NOT NULL, " +
                    "`owner_uuid` VARCHAR(36) NOT NULL, " +
                    "`home_location` VARCHAR(255), " +
                    "`home_server` VARCHAR(255), " +
                    "`pvp_enabled` BOOLEAN DEFAULT TRUE, " +
                    "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "`description` VARCHAR(64) DEFAULT NULL, " +
                    "`balance` DOUBLE DEFAULT 0.0, " +
                    "`kills` INT DEFAULT 0, " +
                    "`deaths` INT DEFAULT 0, " +
                    "PRIMARY KEY (`id`));");

            stmt.execute("CREATE TABLE IF NOT EXISTS `donut_team_members` (" +
                    "`player_uuid` VARCHAR(36) NOT NULL, " +
                    "`team_id` INT NOT NULL, " +
                    "`role` VARCHAR(16) NOT NULL, " +
                    "`join_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "`can_withdraw` BOOLEAN DEFAULT FALSE, " +
                    "`can_use_enderchest` BOOLEAN DEFAULT TRUE, " +
                    "`can_set_home` BOOLEAN DEFAULT FALSE, " +
                    "`can_use_home` BOOLEAN DEFAULT TRUE, " +
                    "PRIMARY KEY (`player_uuid`), " +
                    "FOREIGN KEY (`team_id`) REFERENCES `donut_teams`(`id`) ON DELETE CASCADE);");

            stmt.execute("CREATE TABLE IF NOT EXISTS `donut_team_enderchest` (" +
                    "`team_id` INT NOT NULL, " +
                    "`inventory_data` TEXT, " +
                    "PRIMARY KEY (`team_id`), " +
                    "FOREIGN KEY (`team_id`) REFERENCES `donut_teams`(`id`) ON DELETE CASCADE);");

            stmt.execute("CREATE TABLE IF NOT EXISTS `donut_pending_teleports` (" +
                    "`player_uuid` VARCHAR(36) NOT NULL, " +
                    "`destination_server` VARCHAR(255) NOT NULL, " +
                    "`destination_location` VARCHAR(255) NOT NULL, " +
                    "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY (`player_uuid`));");

            stmt.execute("CREATE TABLE IF NOT EXISTS `donut_servers` (" +
                    "`server_name` VARCHAR(64) PRIMARY KEY, " +
                    "`last_heartbeat` TIMESTAMP NOT NULL);");

            plugin.getLogger().info("Database schema verified successfully.");
        }
    }


    @Override
    public void shutdown() {
        if (isConnected()) {
            hikari.close();
        }
    }

    @Override
    public boolean isConnected() {
        return hikari != null && !hikari.isClosed();
    }

    private Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }

    @Override
    public Optional<Team> createTeam(String name, String tag, UUID ownerUuid, boolean defaultPvp) {
        String insertTeamSQL = "INSERT INTO donut_teams (name, tag, owner_uuid, pvp_enabled) VALUES (?, ?, ?, ?)";
        String insertMemberSQL = "INSERT INTO donut_team_members (player_uuid, team_id, role, join_date, can_withdraw, can_use_enderchest, can_set_home, can_use_home) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement teamStmt = conn.prepareStatement(insertTeamSQL, Statement.RETURN_GENERATED_KEYS)) {
                teamStmt.setString(1, name);
                teamStmt.setString(2, tag);
                teamStmt.setString(3, ownerUuid.toString());
                teamStmt.setBoolean(4, defaultPvp);
                teamStmt.executeUpdate();

                ResultSet generatedKeys = teamStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int teamId = generatedKeys.getInt(1);
                    try (PreparedStatement memberStmt = conn.prepareStatement(insertMemberSQL)) {
                        memberStmt.setString(1, ownerUuid.toString());
                        memberStmt.setInt(2, teamId);
                        memberStmt.setString(3, TeamRole.OWNER.name());
                        memberStmt.setTimestamp(4, Timestamp.from(Instant.now()));
                        memberStmt.setBoolean(5, true);
                        memberStmt.setBoolean(6, true);
                        memberStmt.setBoolean(7, true);
                        memberStmt.setBoolean(8, true);
                        memberStmt.executeUpdate();
                    }
                    conn.commit();
                    return findTeamById(teamId);
                } else {
                    conn.rollback();
                    return Optional.empty();
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create team in database: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void deleteTeam(int teamId) {
        String sql = "DELETE FROM donut_teams WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not delete team with ID " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public void addMemberToTeam(int teamId, UUID playerUuid) {
        String sql = "INSERT INTO donut_team_members (player_uuid, team_id, role, join_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, teamId);
            stmt.setString(3, TeamRole.MEMBER.name());
            stmt.setTimestamp(4, Timestamp.from(Instant.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not add member " + playerUuid + " to team " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public void removeMemberFromTeam(UUID playerUuid) {
        String sql = "DELETE FROM donut_team_members WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not remove member " + playerUuid + ": " + e.getMessage());
        }
    }

    @Override
    public Optional<Team> findTeamByPlayer(UUID playerUuid) {
        String sql = "SELECT t.* FROM donut_teams t JOIN donut_team_members tm ON t.id = tm.team_id WHERE tm.player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error finding team by player: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Team> findTeamByName(String name) {
        String sql = "SELECT * FROM donut_teams WHERE name = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error finding team by name: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Team> findTeamById(int id) {
        String sql = "SELECT * FROM donut_teams WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error finding team by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Team> getAllTeams() {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM donut_teams ORDER BY created_at DESC";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                teams.add(mapTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting all teams: " + e.getMessage());
        }
        return teams;
    }

    @Override
    public List<TeamPlayer> getTeamMembers(int teamId) {
        List<TeamPlayer> members = new ArrayList<>();
        String sql = "SELECT * FROM donut_team_members WHERE team_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teamId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                members.add(mapPlayerFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting team members: " + e.getMessage());
        }
        return members;
    }

    @Override
    public void setTeamHome(int teamId, Location location, String serverName) {
        String sql = "UPDATE donut_teams SET home_location = ?, home_server = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, serializeLocation(location));
            stmt.setString(2, serverName);
            stmt.setInt(3, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not set team home for team " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public Optional<TeamHome> getTeamHome(int teamId) {
        String sql = "SELECT home_location, home_server FROM donut_teams WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teamId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String locStr = rs.getString("home_location");
                String server = rs.getString("home_server");
                Location loc = deserializeLocation(locStr);
                if (loc != null && server != null && !server.isEmpty()) {
                    return Optional.of(new TeamHome(loc, server));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not retrieve team home for team " + teamId + ": " + e.getMessage());
        }
        return Optional.empty();
    }


    @Override
    public void setTeamTag(int teamId, String tag) {
        String sql = "UPDATE donut_teams SET tag = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tag);
            stmt.setInt(2, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not set team tag for team " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public void setTeamDescription(int teamId, String description) {
        String sql = "UPDATE donut_teams SET description = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, description);
            stmt.setInt(2, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not set team description for team " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public void transferOwnership(int teamId, UUID newOwnerUuid, UUID oldOwnerUuid) {
        String updateTeamOwner = "UPDATE donut_teams SET owner_uuid = ? WHERE id = ?";
        String updateNewOwnerRole = "UPDATE donut_team_members SET role = ?, can_withdraw = TRUE, can_use_enderchest = TRUE, can_set_home = TRUE, can_use_home = TRUE WHERE player_uuid = ?";
        String updateOldOwnerRole = "UPDATE donut_team_members SET role = ?, can_withdraw = FALSE, can_use_enderchest = TRUE, can_set_home = FALSE, can_use_home = TRUE WHERE player_uuid = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement teamStmt = conn.prepareStatement(updateTeamOwner);
                 PreparedStatement newOwnerStmt = conn.prepareStatement(updateNewOwnerRole);
                 PreparedStatement oldOwnerStmt = conn.prepareStatement(updateOldOwnerRole)) {

                teamStmt.setString(1, newOwnerUuid.toString());
                teamStmt.setInt(2, teamId);
                teamStmt.executeUpdate();

                newOwnerStmt.setString(1, TeamRole.OWNER.name());
                newOwnerStmt.setString(2, newOwnerUuid.toString());
                newOwnerStmt.executeUpdate();

                oldOwnerStmt.setString(1, TeamRole.MEMBER.name());
                oldOwnerStmt.setString(2, oldOwnerUuid.toString());
                oldOwnerStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not transfer team ownership for team " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public void setPvpStatus(int teamId, boolean status) {
        String sql = "UPDATE donut_teams SET pvp_enabled = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, status);
            stmt.setInt(2, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not set pvp status for team " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public void updateTeamBalance(int teamId, double balance) {
        String sql = "UPDATE donut_teams SET balance = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, balance);
            stmt.setInt(2, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update balance for team " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public void updateTeamStats(int teamId, int kills, int deaths) {
        String sql = "UPDATE donut_teams SET kills = ?, deaths = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, kills);
            stmt.setInt(2, deaths);
            stmt.setInt(3, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update stats for team " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public void saveEnderChest(int teamId, String serializedInventory) {
        String sql;
        if ("mysql".equals(storageType)) {
            sql = "INSERT INTO donut_team_enderchest (team_id, inventory_data) VALUES (?, ?) ON DUPLICATE KEY UPDATE inventory_data = VALUES(inventory_data)";
        } else {
            sql = "MERGE INTO donut_team_enderchest KEY(team_id) VALUES (?, ?)";
        }

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teamId);
            stmt.setString(2, serializedInventory);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save ender chest for team " + teamId + ": " + e.getMessage());
        }
    }

    @Override
    public String getEnderChest(int teamId) {
        String sql = "SELECT inventory_data FROM donut_team_enderchest WHERE team_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teamId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("inventory_data");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load ender chest for team " + teamId + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public void updateMemberPermissions(int teamId, UUID memberUuid, boolean canWithdraw, boolean canUseEnderChest, boolean canSetHome, boolean canUseHome) {
        String sql = "UPDATE donut_team_members SET can_withdraw = ?, can_use_enderchest = ?, can_set_home = ?, can_use_home = ? WHERE player_uuid = ? AND team_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, canWithdraw);
            stmt.setBoolean(2, canUseEnderChest);
            stmt.setBoolean(3, canSetHome);
            stmt.setBoolean(4, canUseHome);
            stmt.setString(5, memberUuid.toString());
            stmt.setInt(6, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update permissions for member " + memberUuid + ": " + e.getMessage());
        }
    }

    @Override
    public void updateMemberRole(int teamId, UUID memberUuid, TeamRole role) {
        String sql;
        if (role == TeamRole.CO_OWNER) {
            sql = "UPDATE donut_team_members SET role = ?, can_withdraw = TRUE, can_use_enderchest = TRUE, can_set_home = TRUE, can_use_home = TRUE WHERE player_uuid = ? AND team_id = ?";
        } else {
            sql = "UPDATE donut_team_members SET role = ?, can_withdraw = FALSE, can_use_enderchest = TRUE, can_set_home = FALSE, can_use_home = TRUE WHERE player_uuid = ? AND team_id = ?";
        }

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.name());
            stmt.setString(2, memberUuid.toString());
            stmt.setInt(3, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update role for member " + memberUuid + ": " + e.getMessage());
        }
    }

    private Map<Integer, Team> getTopTeams(String orderBy, int limit) {
        Map<Integer, Team> topTeams = new LinkedHashMap<>();
        String sql = "SELECT * FROM donut_teams ORDER BY " + orderBy + " DESC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            int rank = 1;
            while (rs.next()) {
                topTeams.put(rank++, mapTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get top teams: " + e.getMessage());
        }
        return topTeams;
    }

    @Override
    public Map<Integer, Team> getTopTeamsByKills(int limit) {
        return getTopTeams("kills", limit);
    }

    @Override
    public Map<Integer, Team> getTopTeamsByBalance(int limit) {
        return getTopTeams("balance", limit);
    }

    @Override
    public Map<Integer, Team> getTopTeamsByMembers(int limit) {
        Map<Integer, Team> topTeams = new LinkedHashMap<>();
        String sql = "SELECT t.*, COUNT(tm.player_uuid) as member_count " +
                "FROM donut_teams t JOIN donut_team_members tm ON t.id = tm.team_id " +
                "GROUP BY t.id ORDER BY member_count DESC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            int rank = 1;
            while (rs.next()) {
                topTeams.put(rank++, mapTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get top teams by members: " + e.getMessage());
        }
        return topTeams;
    }

    @Override
    public void updateServerHeartbeat(String serverName) {
        String sql;
        if ("mysql".equals(storageType)) {
            sql = "INSERT INTO donut_servers (server_name, last_heartbeat) VALUES (?, NOW()) ON DUPLICATE KEY UPDATE last_heartbeat = NOW()";
        } else {
            sql = "MERGE INTO donut_servers KEY(server_name) VALUES (?, NOW())";
        }
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, serverName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update server heartbeat for " + serverName + ": " + e.getMessage());
        }
    }

    @Override
    public Map<String, Timestamp> getActiveServers() {
        Map<String, Timestamp> activeServers = new HashMap<>();
        String sql = "SELECT server_name, last_heartbeat FROM donut_servers WHERE last_heartbeat > NOW() - INTERVAL 2 MINUTE";
        if (!"mysql".equals(storageType)) {
            sql = "SELECT server_name, last_heartbeat FROM donut_servers WHERE last_heartbeat > DATEADD('MINUTE', -2, NOW())";
        }
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                activeServers.put(rs.getString("server_name"), rs.getTimestamp("last_heartbeat"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not retrieve active servers: " + e.getMessage());
        }
        return activeServers;
    }

    @Override
    public void addPendingTeleport(UUID playerUuid, String serverName, Location location) {
        String sql;
        if ("mysql".equals(storageType)) {
            sql = "INSERT INTO donut_pending_teleports (player_uuid, destination_server, destination_location) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE destination_server = VALUES(destination_server), destination_location = VALUES(destination_location)";
        } else {
            sql = "MERGE INTO donut_pending_teleports KEY(player_uuid) VALUES (?, ?, ?)";
        }
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverName);
            stmt.setString(3, serializeLocation(location));
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not add pending teleport for " + playerUuid + ": " + e.getMessage());
        }
    }

    @Override
    public Optional<Location> getAndRemovePendingTeleport(UUID playerUuid, String currentServer) {
        String selectSql = "SELECT destination_location FROM donut_pending_teleports WHERE player_uuid = ? AND destination_server = ?";
        String deleteSql = "DELETE FROM donut_pending_teleports WHERE player_uuid = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, playerUuid.toString());
                selectStmt.setString(2, currentServer);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    Location loc = deserializeLocation(rs.getString("destination_location"));
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setString(1, playerUuid.toString());
                        deleteStmt.executeUpdate();
                    }
                    conn.commit();
                    return Optional.ofNullable(loc);
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error handling pending teleport for " + playerUuid + ": " + e.getMessage());
        }
        return Optional.empty();
    }


    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location deserializeLocation(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(",");
        if (parts.length != 6) return null;
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return new Location(w, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Team mapTeamFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String tag = rs.getString("tag");
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        boolean pvpEnabled = rs.getBoolean("pvp_enabled");
        String description = rs.getString("description");
        double balance = rs.getDouble("balance");
        int kills = rs.getInt("kills");
        int deaths = rs.getInt("deaths");

        Team team = new Team(id, name, tag, ownerUuid, pvpEnabled);
        team.setHomeLocation(deserializeLocation(rs.getString("home_location")));
        team.setHomeServer(rs.getString("home_server"));
        if (description != null) {
            team.setDescription(description);
        }
        team.setBalance(balance);
        team.setKills(kills);
        team.setDeaths(deaths);
        return team;
    }

    private TeamPlayer mapPlayerFromResultSet(ResultSet rs) throws SQLException {
        UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
        TeamRole role = TeamRole.valueOf(rs.getString("role"));
        Instant joinDate = rs.getTimestamp("join_date").toInstant();
        boolean canWithdraw = rs.getBoolean("can_withdraw");
        boolean canUseEnderChest = rs.getBoolean("can_use_enderchest");
        boolean canSetHome = rs.getBoolean("can_set_home");
        boolean canUseHome = rs.getBoolean("can_use_home");
        return new TeamPlayer(playerUuid, role, joinDate, canWithdraw, canUseEnderChest, canSetHome, canUseHome);
    }
}