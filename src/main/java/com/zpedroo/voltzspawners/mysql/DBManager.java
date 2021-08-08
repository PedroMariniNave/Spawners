package com.zpedroo.voltzspawners.mysql;

import com.zpedroo.voltzspawners.managers.SpawnerManager;
import com.zpedroo.voltzspawners.spawner.Spawner;
import com.zpedroo.voltzspawners.objects.Manager;
import com.zpedroo.voltzspawners.spawner.PlayerSpawner;
import org.bukkit.Location;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DBManager {

    private SpawnerManager manager;

    public DBManager() {
        this.manager = new SpawnerManager();
    }

    public void saveSpawner(PlayerSpawner spawner) {
        if (contains(getManager().serializeLocation(spawner.getLocation()), "location")) {
            String query = "UPDATE `" + DBConnection.TABLE + "` SET" +
                    "`location`='" + getManager().serializeLocation(spawner.getLocation()) + "', " +
                    "`uuid`='" + spawner.getOwnerUUID().toString() + "', " +
                    "`stack`='" + spawner.getStack().toString() + "', " +
                    "`energy`='" + spawner.getEnergy().toString() + "', " +
                    "`drops`='" + spawner.getDrops().toString() + "', " +
                    "`integrity`='" + spawner.getIntegrity().toString() + "', " +
                    "`type`='" + spawner.getSpawner().getType() + "', " +
                    "`managers`='" + getManager().serializeManagers(spawner.getManagers()) + "', " +
                    "`infinite`='" + (spawner.isInfinite() ? 1 : 0) + "' " +
                    "WHERE `location`='" + getManager().serializeLocation(spawner.getLocation()) + "';";
            executeUpdate(query);
            return;
        }

        String query = "INSERT INTO `" + DBConnection.TABLE + "` (`location`, `uuid`, `stack`, `energy`, `drops`, `integrity`, `type`, `managers`, `infinite`) VALUES " +
                "('" + getManager().serializeLocation(spawner.getLocation()) + "', " +
                "'" + spawner.getOwnerUUID().toString() + "', " +
                "'" + spawner.getStack().toString() + "', " +
                "'" + spawner.getEnergy().toString() + "', " +
                "'" + spawner.getDrops().toString() + "', " +
                "'" + spawner.getIntegrity().toString() + "', " +
                "'" + spawner.getSpawner().getType() + "', " +
                "'" + getManager().serializeManagers(spawner.getManagers()) + "', " +
                "'" + (spawner.isInfinite() ? 1 : 0) + "');";
        executeUpdate(query);
    }

    public void deleteSpawner(String location) {
        String query = "DELETE FROM `" + DBConnection.TABLE + "` WHERE `location`='" + location + "';";
        executeUpdate(query);
    }

    public HashMap<Location, PlayerSpawner> getPlacedSpawners() {
        HashMap<Location, PlayerSpawner> spawners = new HashMap<>(5120);

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet result = null;
        String query = "SELECT * FROM `" + DBConnection.TABLE + "`;";

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(query);
            result = preparedStatement.executeQuery();

            while (result.next()) {
                Location location = getManager().deserializeLocation(result.getString(1));
                UUID ownerUUID = UUID.fromString(result.getString(2));
                BigDecimal stack = result.getBigDecimal(3);
                BigDecimal energy = result.getBigDecimal(4);
                BigDecimal drops = result.getBigDecimal(5);
                Integer integrity = result.getInt(6);
                Spawner spawner = getManager().getSpawner(result.getString(7));
                List<Manager> managers = getManager().deserializeManagers(result.getString(8));
                Boolean infinite = result.getBoolean(9);
                PlayerSpawner playerSpawner = new PlayerSpawner(location, ownerUUID, stack.toBigInteger(), energy.toBigInteger(), drops.toBigInteger(), integrity, spawner, managers, infinite);

                spawners.put(location, playerSpawner);

                List<PlayerSpawner> spawnersList = getManager().getDataCache().getPlayerSpawnersByUUID(ownerUUID);
                spawnersList.add(playerSpawner);

                getManager().getDataCache().setUUIDSpawners(ownerUUID, spawnersList);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(connection, result, preparedStatement, null);
        }

        return spawners;
    }

    private Boolean contains(String value, String column) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet result = null;
        String query = "SELECT `" + column + "` FROM `" + DBConnection.TABLE + "` WHERE `" + column + "`='" + value + "';";
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(query);
            result = preparedStatement.executeQuery();
            return result.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(connection, result, preparedStatement, null);
        }

        return false;
    }

    private void executeUpdate(String query) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(connection, null, null, statement);
        }
    }

    private void closeConnection(Connection connection, ResultSet resultSet, PreparedStatement preparedStatement, Statement statement) {
        try {
            if (connection != null) connection.close();
            if (resultSet != null) resultSet.close();
            if (preparedStatement != null) preparedStatement.close();
            if (statement != null) statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    protected void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS `" + DBConnection.TABLE + "` (`location` VARCHAR(255) NOT NULL, `uuid` VARCHAR(255) NOT NULL, `stack` DECIMAL(40,0) NOT NULL, `energy` DECIMAL(40,0) NOT NULL, `drops` DECIMAL(40,0) NOT NULL, `integrity` INTEGER NOT NULL, `type` VARCHAR(32) NOT NULL, `managers` LONGTEXT NOT NULL, `infinite` BOOLEAN NOT NULL, PRIMARY KEY(`location`));";
        executeUpdate(query);
    }

    private Connection getConnection() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    private SpawnerManager getManager() {
        return manager;
    }
}