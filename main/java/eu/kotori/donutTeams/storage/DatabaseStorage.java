package eu.kotori.donutTeams.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamPlayer;
import eu.kotori.donutTeams.team.TeamRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class DatabaseStorage implements IDataStorage {

    private final DonutTeams plugin;
    private HikariDataSource hikari;
    private final String storageType;

    public DatabaseStorage(DonutTeams plugin) {
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
            runMigrations();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database connection pool for " + storageType + "!");
            plugin.getLogger().severe("Error: " + e.getMessage());
            return false;
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

    private void runMigrations() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS `db_version` (`id` INT PRIMARY KEY, `version` INT);");

            ResultSet rs = stmt.executeQuery("SELECT version FROM `db_version` WHERE id = 1;");
            int currentVersion = 0;
            if (rs.next()) {
                currentVersion = rs.getInt("version");
            } else {
                stmt.execute("INSERT INTO `db_version` (id, version) VALUES (1, 0);");
            }

            if (currentVersion < 1) {
                String createTeamsTable = "CREATE TABLE IF NOT EXISTS `donut_teams` (" +
                        "`id` INT AUTO_INCREMENT, " +
                        "`name` VARCHAR(16) NOT NULL UNIQUE, " +
                        "`tag` VARCHAR(6) NOT NULL, " +
                        "`owner_uuid` VARCHAR(36) NOT NULL, " +
                        "`home_location` VARCHAR(255), " +
                        "`pvp_enabled` BOOLEAN DEFAULT TRUE, " +
                        "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (`id`));";

                String createMembersTable = "CREATE TABLE IF NOT EXISTS `donut_team_members` (" +
                        "`player_uuid` VARCHAR(36) NOT NULL, " +
                        "`team_id` INT NOT NULL, " +
                        "`role` VARCHAR(16) NOT NULL, " +
                        "`join_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (`player_uuid`), " +
                        "FOREIGN KEY (`team_id`) REFERENCES `donut_teams`(`id`) ON DELETE CASCADE);";

                stmt.execute(createTeamsTable);
                stmt.execute(createMembersTable);
                stmt.execute("UPDATE `db_version` SET version = 1 WHERE id = 1;");
                plugin.getLogger().info("Database migrated to version 1.");
                currentVersion = 1;
            }
            if (currentVersion < 2) {
                stmt.execute("ALTER TABLE `donut_teams` ADD COLUMN `description` VARCHAR(64) DEFAULT NULL;");
                stmt.execute("ALTER TABLE `donut_teams` ADD COLUMN `balance` DOUBLE DEFAULT 0.0;");
                stmt.execute("ALTER TABLE `donut_teams` ADD COLUMN `kills` INT DEFAULT 0;");
                stmt.execute("ALTER TABLE `donut_teams` ADD COLUMN `deaths` INT DEFAULT 0;");

                String createEnderChestTable = "CREATE TABLE IF NOT EXISTS `donut_team_enderchest` (" +
                        "`team_id` INT NOT NULL, " +
                        "`inventory_data` TEXT, " +
                        "PRIMARY KEY (`team_id`), " +
                        "FOREIGN KEY (`team_id`) REFERENCES `donut_teams`(`id`) ON DELETE CASCADE);";
                stmt.execute(createEnderChestTable);

                stmt.execute("ALTER TABLE `donut_team_members` ADD COLUMN `can_withdraw` BOOLEAN DEFAULT FALSE;");
                stmt.execute("ALTER TABLE `donut_team_members` ADD COLUMN `can_use_enderchest` BOOLEAN DEFAULT TRUE;");
                stmt.execute("UPDATE `db_version` SET version = 2 WHERE id = 1;");
                plugin.getLogger().info("Database migrated to version 2.");
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }

    @Override
    public Optional<Team> createTeam(String name, String tag, UUID ownerUuid, boolean defaultPvp) {
        String insertTeamSQL = "INSERT INTO donut_teams (name, tag, owner_uuid, pvp_enabled) VALUES (?, ?, ?, ?)";
        String insertMemberSQL = "INSERT INTO donut_team_members (player_uuid, team_id, role, join_date, can_withdraw, can_use_enderchest) VALUES (?, ?, ?, ?, ?, ?)";

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

    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location deserializeLocation(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(",");
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);
        return new Location(w, x, y, z, yaw, pitch);
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
        if (description != null) {
            team.setDescription(description);
        }
        team.setBalance(balance);
        team.setKills(kills);
        team.setDeaths(deaths);
        return team;
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
    public List<TeamPlayer> getTeamMembers(int teamId) {
        List<TeamPlayer> members = new ArrayList<>();
        String sql = "SELECT * FROM donut_team_members WHERE team_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teamId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                TeamRole role = TeamRole.valueOf(rs.getString("role"));
                Instant joinDate = rs.getTimestamp("join_date").toInstant();
                boolean canWithdraw = rs.getBoolean("can_withdraw");
                boolean canUseEnderChest = rs.getBoolean("can_use_enderchest");
                members.add(new TeamPlayer(playerUuid, role, joinDate, canWithdraw, canUseEnderChest));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting team members: " + e.getMessage());
        }
        return members;
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
    public void setTeamHome(int teamId, Location location) {
        String sql = "UPDATE donut_teams SET home_location = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, serializeLocation(location));
            stmt.setInt(2, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not set team home for team " + teamId + ": " + e.getMessage());
        }
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
        String updateNewOwnerRole = "UPDATE donut_team_members SET role = ?, can_withdraw = TRUE WHERE player_uuid = ?";
        String updateOldOwnerRole = "UPDATE donut_team_members SET role = ?, can_withdraw = FALSE WHERE player_uuid = ?";

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
    public void updateMemberRole(int teamId, UUID memberUuid, TeamRole role) {
        String sql = "UPDATE donut_team_members SET role = ? WHERE player_uuid = ? AND team_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.name());
            stmt.setString(2, memberUuid.toString());
            stmt.setInt(3, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update role for member " + memberUuid + ": " + e.getMessage());
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
        String sql = "INSERT INTO donut_team_enderchest (team_id, inventory_data) VALUES (?, ?) ON DUPLICATE KEY UPDATE inventory_data = VALUES(inventory_data)";
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
    public void updateMemberPermissions(int teamId, UUID memberUuid, boolean canWithdraw, boolean canUseEnderChest) {
        String sql = "UPDATE donut_team_members SET can_withdraw = ?, can_use_enderchest = ? WHERE player_uuid = ? AND team_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, canWithdraw);
            stmt.setBoolean(2, canUseEnderChest);
            stmt.setString(3, memberUuid.toString());
            stmt.setInt(4, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update permissions for member " + memberUuid + ": " + e.getMessage());
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
}