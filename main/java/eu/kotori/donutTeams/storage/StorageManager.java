package eu.kotori.donutTeams.storage;

import eu.kotori.donutTeams.DonutTeams;

public class StorageManager {

    private final IDataStorage storage;

    public StorageManager(DonutTeams plugin) {
        this.storage = new DatabaseStorage(plugin);
    }

    public boolean init() {
        return storage.init();
    }

    public void shutdown() {
        storage.shutdown();
    }

    public IDataStorage getStorage() {
        return storage;
    }

    public boolean isConnected() {
        return storage.isConnected();
    }
}