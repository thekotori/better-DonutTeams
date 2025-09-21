package eu.kotori.justTeams.storage;
import eu.kotori.justTeams.JustTeams;
public class StorageManager {
    private final IDataStorage storage;
    public StorageManager(JustTeams plugin) {
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
