package me.salman.websiteCaptcha.Database;

import me.salman.websiteCaptcha.Main;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicReference;

public class SQLiteManager {
    private static SQLiteManager instance;
    private final Plugin plugin;
    private final String dbPath;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicReference<Connection> connection = new AtomicReference<>(null);
    public static final int MAX_RETRIES = 10;
    public static final long RETRY_DELAY_MS = 1000;

    private SQLiteManager(Plugin plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder() + File.separator + "verification.db";
        plugin.getLogger().info("Initializing SQLiteManager with dbPath: " + dbPath + " at " + System.currentTimeMillis() + " on thread " + Thread.currentThread().getName());
        try {
            initializeDatabase();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLiteManager: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("SQLite initialization failed", e);
        }
    }

    public static SQLiteManager getInstance(Plugin plugin) {
        if (instance == null) {
            synchronized (SQLiteManager.class) {
                if (instance == null) {
                    instance = new SQLiteManager(plugin);
                }
            }
        }
        return instance;
    }

    private void initializeDatabase() throws SQLException {
        int retries = MAX_RETRIES;
        while (retries > 0) {
            lock.lock();
            try {
                plugin.getLogger().info("Starting database initialization (attempt " + (MAX_RETRIES - retries + 1) + "/" + MAX_RETRIES + ")");

                if (!plugin.getDataFolder().exists()) {
                    plugin.getLogger().info("Creating plugin data folder...");
                    if (!plugin.getDataFolder().mkdirs()) {
                        throw new SQLException("Failed to create plugin data folder!");
                    }
                }

                Path dbFile = Path.of(dbPath);
                boolean dbExists = Files.exists(dbFile);
                if (dbExists) {
                    try {
                        Files.newByteChannel(dbFile).close();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Database file is locked by another process: " + e.getMessage());
                        throw new SQLException("Database file is locked or inaccessible: " + e.getMessage(), e);
                    }
                }

                plugin.getLogger().info("Attempting to establish SQLite connection...");
                Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                conn.setAutoCommit(true);
                try (PreparedStatement stmt = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS player_verification (" +
                                "uuid TEXT PRIMARY KEY," +
                                "task_id INTEGER," +
                                "last_verification_time LONG," +
                                "is_verified INTEGER DEFAULT 0)")) {
                    plugin.getLogger().info("Creating or verifying player_verification table...");
                    stmt.executeUpdate();
                    plugin.getLogger().info("Player verification table " + (dbExists ? "already exists" : "created") + ".");
                }
                connection.set(conn);
                plugin.getLogger().info("SQLite connection established: " + isConnectionValid());
                return;
            } catch (SQLException e) {
                plugin.getLogger().warning("SQL error during database initialization (attempt " + (MAX_RETRIES - retries + 1) + "/" + MAX_RETRIES + "): " + e.getMessage());
                e.printStackTrace();
                if (e.getMessage().contains("database is locked")) {
                    retries--;
                    try {
                        forceCloseConnection();
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        plugin.getLogger().severe("Interrupted while retrying: " + ie.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                throw e;
            } finally {
                lock.unlock();
            }
        }
        throw new SQLException("Failed to initialize SQLite database after all retries.");
    }

    private Connection getConnection() throws SQLException {
        lock.lock();
        try {
            Connection conn = connection.get();
            if (conn == null || conn.isClosed()) {
                plugin.getLogger().warning("Re-establishing SQLite connection due to closure or null...");
                conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                conn.setAutoCommit(true);
                connection.set(conn);
                plugin.getLogger().info("New SQLite connection established: " + isConnectionValid());
            }
            return conn;
        } finally {
            lock.unlock();
        }
    }

    public void closeConnection() {
        lock.lock();
        try {
            Connection conn = connection.getAndSet(null);
            if (conn != null && !conn.isClosed()) {
                plugin.getLogger().info("Closing SQLite connection...");
                conn.close();
                plugin.getLogger().info("SQLite connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close SQLite connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void forceCloseConnection() {
        lock.lock();
        try {
            Connection conn = connection.getAndSet(null);
            if (conn != null && !conn.isClosed()) {
                plugin.getLogger().info("Forcing closure of SQLite connection...");
                conn.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to force close connection: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public void setVerificationTask(UUID uuid, int taskId) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO player_verification (uuid, task_id) VALUES (?, ?)")) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, taskId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set verification task for UUID " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Integer getVerificationTask(UUID uuid) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT task_id FROM player_verification WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("task_id");
                    return null;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get verification task for UUID " + uuid + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void removeVerificationTask(UUID uuid) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE player_verification SET task_id = NULL WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove verification task for UUID " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setLastVerificationTime(UUID uuid, long time) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO player_verification (uuid, last_verification_time) VALUES (?, ?)")) {
                stmt.setString(1, uuid.toString());
                stmt.setLong(2, time);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set last verification time for UUID " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Long getLastVerificationTime(UUID uuid) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT last_verification_time FROM player_verification WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getLong("last_verification_time");
                    return null;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get last verification time for UUID " + uuid + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void setPlayerVerified(UUID uuid, boolean verified, long time) {
        lock.lock();
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO player_verification (uuid, is_verified, last_verification_time) VALUES (?, ?, ?)")) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, verified ? 1 : 0);
                stmt.setLong(3, time);
                stmt.executeUpdate();
                conn.commit(); // Force commit
                plugin.getLogger().info("Set player verified status for UUID " + uuid + " to " + verified + " with time " + time);
            } finally {
                conn.setAutoCommit(true); // Restore auto-commit
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set player verified status for UUID " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public boolean isPlayerVerified(UUID uuid) {
        lock.lock();
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT is_verified FROM player_verification WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    boolean verified = rs.next() && rs.getInt("is_verified") == 1;
                    plugin.getLogger().info("Checked isPlayerVerified for UUID " + uuid + ": " + verified);
                    return verified;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check if player is verified for UUID " + uuid + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void clearPlayerVerification(UUID uuid) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE player_verification SET is_verified = 0, task_id = NULL, last_verification_time = NULL WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to clear player verification for UUID " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isConnectionValid() {
        lock.lock();
        try {
            Connection conn = connection.get();
            if (conn == null) {
                plugin.getLogger().warning("Connection is null.");
                return false;
            }
            return !conn.isClosed();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check connection validity: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }
    }
}