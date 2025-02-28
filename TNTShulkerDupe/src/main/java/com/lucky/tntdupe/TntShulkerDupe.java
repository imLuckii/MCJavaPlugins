package com.lucky.tntdupe;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TntShulkerDupe extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onTntExplode(EntityExplodeEvent event) {
        List<Block> affectedBlocks = event.blockList();

        for (Block block : affectedBlocks) {
            if (block.getState() instanceof ShulkerBox) {
                ShulkerBox shulkerBox = (ShulkerBox) block.getState();

                ItemStack shulkerBoxItem = new ItemStack(block.getType());

                BlockStateMeta blockStateMeta = (BlockStateMeta) shulkerBoxItem.getItemMeta();

                blockStateMeta.setBlockState(shulkerBox);
                shulkerBoxItem.setItemMeta(blockStateMeta);

                block.getWorld().dropItemNaturally(block.getLocation(), shulkerBoxItem);

            }
        }
    }
}
