package com.example.duplicationmod; 

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class DuplicationMod implements ModInitializer {

    // Identificatori per la mod
    private static final Identifier DUPLICATE_PACKET_ID = new Identifier("duplicationmod", "duplicate_item");
    private static KeyBinding duplicateKey;

    @Override
    public void onInitialize() {
        // --- 1. REGISTRAZIONE DEL TASTO (Client-Side) ---
        duplicateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.duplicationmod.duplicate", 
                InputUtil.Type.KEYSYM, 
                GLFW.GLFW_KEY_K, // Tasto predefinito: K
                "category.duplicationmod.general" 
        ));

        // --- 2. GESTIONE DEI PACCHETTI (Server-Side) ---
        // Quando il server riceve il pacchetto, esegue l'azione handleDuplication
        ServerPlayNetworking.registerGlobalReceiver(DUPLICATE_PACKET_ID, (server, player, handler, buf, sender) -> {
            server.execute(() -> handleDuplication(player));
        });

        // --- 3. GESTIONE DEL TASTO PREMUTO (Client-Side) ---
        // Registra un evento per la pressione del tasto
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
             if (duplicateKey.wasPressed()) {
                 // Invia un pacchetto vuoto al Server, indicando che il tasto è stato premuto
                 ClientPlayNetworking.send(DUPLICATE_PACKET_ID, new PacketByteBuf(io.netty.buffer.Unpooled.EMPTY_BUFFER));
             }
        });
    }

    // --- 4. LOGICA DI DUPLICAZIONE (Server-Side) ---
    private void handleDuplication(ServerPlayerEntity player) {
        // Ottiene l'oggetto principale tenuto in mano
        ItemStack itemStack = player.getMainHandStack();

        // Controlla se l'oggetto è valido (non vuoto, stackable, e non ha raggiunto il massimo)
        if (!itemStack.isEmpty() && itemStack.getMaxCount() > 1 && itemStack.getCount() < itemStack.getMaxCount()) {
            
            // Crea una copia esatta dell'oggetto
            ItemStack duplicatedItem = itemStack.copy();
            
            // Imposta la quantità a 1
            duplicatedItem.setCount(1);

            // Tenta di aggiungere l'oggetto all'inventario del giocatore
            if (player.getInventory().insertStack(duplicatedItem)) {
                player.sendMessage(Text.literal("§a[DuplicationMod] Oggetto duplicato!"), true);
            } else {
                player.sendMessage(Text.literal("§c[DuplicationMod] Inventario pieno!"), true);
            }
        } else {
            player.sendMessage(Text.literal("§e[DuplicationMod] Non duplicabile (vuoto o stack completo)."), true);
        }
    }
}
