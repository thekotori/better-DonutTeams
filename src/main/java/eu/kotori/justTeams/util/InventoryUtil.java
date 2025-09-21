package eu.kotori.justTeams.util;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
public class InventoryUtil {
    public static String serializeInventory(Inventory inventory) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeInt(inventory.getSize());
        for (int i = 0; i < inventory.getSize(); i++) {
            dataOutput.writeObject(inventory.getItem(i));
        }
        dataOutput.close();
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }
    public static void deserializeInventory(Inventory inventory, String data) throws IOException {
        if (data == null || data.isEmpty()) return;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        inventory.clear();
        int size = dataInput.readInt();
        for (int i = 0; i < size; i++) {
            try {
                ItemStack item = (ItemStack) dataInput.readObject();
                if (item != null) {
                    inventory.setItem(i, item);
                }
            } catch (ClassNotFoundException e) {
                throw new IOException("Unable to decode class type.", e);
            }
        }
        dataInput.close();
    }
}
