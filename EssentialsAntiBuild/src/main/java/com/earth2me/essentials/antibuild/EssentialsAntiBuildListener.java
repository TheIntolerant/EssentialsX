package com.earth2me.essentials.antibuild;

import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.EnumUtil;
import com.earth2me.essentials.utils.VersionUtil;
import net.ess3.api.IEssentials;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

public class EssentialsAntiBuildListener implements Listener {
    final private transient IAntiBuild prot;
    final private transient IEssentials ess;

    EssentialsAntiBuildListener(final IAntiBuild parent) {
        this.prot = parent;
        this.ess = prot.getEssentialsConnect().getEssentials();

        if (isEntityPickupEvent()) {
            ess.getServer().getPluginManager().registerEvents(new EntityPickupItemListener(), prot);
        } else {
            ess.getServer().getPluginManager().registerEvents(new PlayerPickupItemListener(), prot);
        }
    }

    private static boolean isEntityPickupEvent() {
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    private boolean metaPermCheck(final User user, final String action, final Block block) {
        if (block == null) {
            if (ess.getSettings().isDebug()) {
                prot.getLogger().log(Level.INFO, "AntiBuild permission check failed, invalid block.");
            }
            return false;
        }
        if (VersionUtil.PRE_FLATTENING) {
            return metaPermCheck(user, action, block.getType(), block.getData());
        }
        return metaPermCheck(user, action, block.getType());
    }

    private boolean metaPermCheck(final User user, final String action, final ItemStack item) {
        if (item == null) {
            if (ess.getSettings().isDebug()) {
                prot.getLogger().log(Level.INFO, "AntiBuild permission check failed, invalid item.");
            }
            return false;
        }
        if (VersionUtil.PRE_FLATTENING) {
            return metaPermCheck(user, action, item.getType(), item.getDurability());
        }
        return metaPermCheck(user, action, item.getType());
    }

    public boolean metaPermCheck(final User user, final String action, final Material material) {
        final String blockPerm = "essentials.build." + action + "." + material;
        return user.isAuthorized(blockPerm);
    }

    private boolean metaPermCheck(final User user, final String action, final Material material, final short data) {
        final String blockPerm = "essentials.build." + action + "." + material;
        final String dataPerm = blockPerm + ":" + data;

        if (VersionUtil.PRE_FLATTENING) {
            if (user.getBase().isPermissionSet(dataPerm)) {
                return user.isAuthorized(dataPerm);
            } else {
                if (ess.getSettings().isDebug()) {
                    prot.getLogger().log(Level.INFO, "DataValue perm on " + user.getName() + " is not directly set: " + dataPerm);
                }
            }
        }

        return user.isAuthorized(blockPerm);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final User user = ess.getUser(event.getPlayer());
        final Block block = event.getBlockPlaced();
        final Material type = block.getType();

        // Happens with blocks like eyes of ender, we shouldn't treat state changes as a block place.
        if (type.equals(event.getBlockReplacedState().getType())) {
            return;
        }

        if (prot.getSettingBool(AntiBuildConfig.disable_build) && !user.canBuild() && !metaPermCheck(user, "place", block)) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildPlace", EssentialsAntiBuild.getNameForType(type));
            }
            event.setCancelled(true);
            return;
        }

        if (prot.checkProtectionItems(AntiBuildConfig.blacklist_placement, type) && !user.isAuthorized("essentials.protect.exemptplacement")) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildPlace", EssentialsAntiBuild.getNameForType(type));
            }
            event.setCancelled(true);
            return;
        }

        if (prot.checkProtectionItems(AntiBuildConfig.alert_on_placement, type) && !user.isAuthorized("essentials.protect.alerts.notrigger")) {
            prot.getEssentialsConnect().alert(user, EssentialsAntiBuild.getNameForType(type), "alertPlaced");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final User user = ess.getUser(event.getPlayer());
        final Block block = event.getBlock();
        final Material type = block.getType();

        if (prot.getSettingBool(AntiBuildConfig.disable_build) && !user.canBuild() && !metaPermCheck(user, "break", block)) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildBreak", EssentialsAntiBuild.getNameForType(type));
            }
            event.setCancelled(true);
            return;
        }

        if (prot.checkProtectionItems(AntiBuildConfig.blacklist_break, type) && !user.isAuthorized("essentials.protect.exemptbreak")) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildBreak", EssentialsAntiBuild.getNameForType(type));
            }
            event.setCancelled(true);
            return;
        }

        if (prot.checkProtectionItems(AntiBuildConfig.alert_on_break, type) && !user.isAuthorized("essentials.protect.alerts.notrigger")) {
            prot.getEssentialsConnect().alert(user, EssentialsAntiBuild.getNameForType(type), "alertBroke");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(final HangingBreakByEntityEvent event) {
        final Entity entity = event.getRemover();
        if (entity instanceof Player) {
            final User user = ess.getUser((Player) entity);
            final EntityType type = event.getEntity().getType();
            final boolean warn = ess.getSettings().warnOnBuildDisallow();
            if (prot.getSettingBool(AntiBuildConfig.disable_build) && !user.canBuild()) {
                if (type == EntityType.PAINTING && !metaPermCheck(user, "break", Material.PAINTING)) {
                    if (warn) {
                        user.sendTl("antiBuildBreak", Material.PAINTING.toString());
                    }
                    event.setCancelled(true);
                } else if (type == EntityType.ITEM_FRAME && !metaPermCheck(user, "break", Material.ITEM_FRAME)) {
                    if (warn) {
                        user.sendTl("antiBuildBreak", Material.ITEM_FRAME.toString());
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemFrameInteract(final PlayerInteractEntityEvent event) {
        if (event.getPlayer().hasMetadata("NPC")) {
            return;
        }

        final User user = ess.getUser(event.getPlayer());

        if (!(event.getRightClicked() instanceof ItemFrame)) {
            return;
        }

        if (prot.getSettingBool(AntiBuildConfig.disable_build) && !user.canBuild() && !user.isAuthorized("essentials.build") && !metaPermCheck(user, "place", Material.ITEM_FRAME)) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildPlace", Material.ITEM_FRAME.toString());
            }
            event.setCancelled(true);
            return;
        }

        if (prot.checkProtectionItems(AntiBuildConfig.blacklist_placement, Material.ITEM_FRAME) && !user.isAuthorized("essentials.protect.exemptplacement")) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildPlace", Material.ITEM_FRAME.toString());
            }
            event.setCancelled(true);
            return;
        }

        if (prot.checkProtectionItems(AntiBuildConfig.alert_on_placement, Material.ITEM_FRAME) && !user.isAuthorized("essentials.protect.alerts.notrigger")) {
            prot.getEssentialsConnect().alert(user, Material.ITEM_FRAME.toString(), "alertPlaced");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandInteract(final PlayerInteractAtEntityEvent event) {
        if (event.getPlayer().hasMetadata("NPC")) {
            return;
        }

        final User user = ess.getUser(event.getPlayer());

        if (!(event.getRightClicked() instanceof ArmorStand)) {
            return;
        }

        if (prot.getSettingBool(AntiBuildConfig.disable_build) && !user.canBuild() && !user.isAuthorized("essentials.build") && !metaPermCheck(user, "place", Material.ARMOR_STAND)) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildPlace", Material.ARMOR_STAND.toString());
            }
            event.setCancelled(true);
            return;
        }

        if (prot.checkProtectionItems(AntiBuildConfig.blacklist_placement, Material.ARMOR_STAND) && !user.isAuthorized("essentials.protect.exemptplacement")) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildPlace", Material.ARMOR_STAND.toString());
            }
            event.setCancelled(true);
            return;
        }

        if (prot.checkProtectionItems(AntiBuildConfig.alert_on_placement, Material.ARMOR_STAND) && !user.isAuthorized("essentials.protect.alerts.notrigger")) {
            prot.getEssentialsConnect().alert(user, Material.ARMOR_STAND.toString(), "alertPlaced");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockEntityDamage(final EntityDamageByEntityEvent event) {
        final Player player;

        if (event.getDamager() instanceof Player) {
            player = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player) {
            player = (Player) ((Projectile) event.getDamager()).getShooter();
        } else {
            return;
        }

        final User user = ess.getUser(player);
        final Material type;

        if (event.getEntity() instanceof ItemFrame) {
            type = Material.ITEM_FRAME;
        } else if (event.getEntity() instanceof ArmorStand) {
            type = Material.ARMOR_STAND;
        } else if (event.getEntity() instanceof EnderCrystal) {
            // There is no Material for Ender Crystals before 1.9.
            type = EnumUtil.getMaterial("END_CRYSTAL");
        } else {
            return;
        }

        if (prot.getSettingBool(AntiBuildConfig.disable_build) && !user.canBuild() && !user.isAuthorized("essentials.build")) {
            final boolean permCheck = type == null ? user.isAuthorized("essentials.build.break.END_CRYSTAL") : metaPermCheck(user, "break", type);
            if (!permCheck) {
                if (ess.getSettings().warnOnBuildDisallow()) {
                    user.sendTl("antiBuildBreak", type != null ? type.toString() : "END_CRYSTAL");
                }
                event.setCancelled(true);
                return;
            }
        }

        final boolean blacklistCheck = type == null ? prot.checkProtectionItems(AntiBuildConfig.blacklist_break, "END_CRYSTAL") : prot.checkProtectionItems(AntiBuildConfig.blacklist_break, type);
        if (blacklistCheck && !user.isAuthorized("essentials.protect.exemptbreak")) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildBreak", type != null ? type.toString() : "END_CRYSTAL");
            }
            event.setCancelled(true);
            return;
        }

        final boolean alertCheck = type == null ? prot.checkProtectionItems(AntiBuildConfig.alert_on_break, "END_CRYSTAL") : prot.checkProtectionItems(AntiBuildConfig.alert_on_break, type);
        if (alertCheck && !user.isAuthorized("essentials.protect.alerts.notrigger")) {
            prot.getEssentialsConnect().alert(user, type != null ? type.toString() : "END_CRYSTAL", "alertBroke");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
        for (final Block block : event.getBlocks()) {
            if (prot.checkProtectionItems(AntiBuildConfig.blacklist_piston, block.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
        if (!event.isSticky()) {
            return;
        }
        for (final Block block : event.getBlocks()) {
            if (prot.checkProtectionItems(AntiBuildConfig.blacklist_piston, block.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getPlayer().hasMetadata("NPC")) {
            return;
        }

        // Do not return if cancelled, because the interact event has 2 cancelled states.
        final User user = ess.getUser(event.getPlayer());
        final ItemStack item = event.getItem();

        if (item != null && prot.checkProtectionItems(AntiBuildConfig.blacklist_usage, item.getType()) && !user.isAuthorized("essentials.protect.exemptusage")) {
            if (ess.getSettings().warnOnBuildDisallow()) {
                user.sendTl("antiBuildUse", item.getType().toString());
            }
            event.setCancelled(true);
            return;
        }

        if (item != null && prot.checkProtectionItems(AntiBuildConfig.alert_on_use, item.getType()) && !user.isAuthorized("essentials.protect.alerts.notrigger")) {
            prot.getEssentialsConnect().alert(user, item.getType().toString(), "alertUsed");
        }

        if (prot.getSettingBool(AntiBuildConfig.disable_use) && !user.canBuild()) {
            if (event.hasItem() && !metaPermCheck(user, "interact", item)) {
                event.setCancelled(true);
                if (ess.getSettings().warnOnBuildDisallow()) {
                    user.sendTl("antiBuildUse", item.getType().toString());
                }
                return;
            }
            if (event.hasBlock() && !metaPermCheck(user, "interact", event.getClickedBlock())) {
                event.setCancelled(true);
                if (ess.getSettings().warnOnBuildDisallow()) {
                    user.sendTl("antiBuildInteract", event.getClickedBlock().getType().toString());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCraftItemEvent(final CraftItemEvent event) {
        final HumanEntity entity = event.getWhoClicked();

        if (entity instanceof Player) {
            final User user = ess.getUser((Player) entity);
            final ItemStack item = event.getRecipe().getResult();

            if (prot.getSettingBool(AntiBuildConfig.disable_use) && !user.canBuild()) {
                if (!metaPermCheck(user, "craft", item)) {
                    event.setCancelled(true);
                    if (ess.getSettings().warnOnBuildDisallow()) {
                        user.sendTl("antiBuildCraft", item.getType().toString());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {

        final User user = ess.getUser(event.getPlayer());
        final ItemStack item = event.getItemDrop().getItemStack();

        if (prot.getSettingBool(AntiBuildConfig.disable_use) && !user.canBuild()) {
            if (!metaPermCheck(user, "drop", item)) {
                event.setCancelled(true);
                user.getBase().updateInventory();
                if (ess.getSettings().warnOnBuildDisallow()) {
                    user.sendTl("antiBuildDrop", item.getType().toString());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(final BlockDispenseEvent event) {
        final ItemStack item = event.getItem();
        if (prot.checkProtectionItems(AntiBuildConfig.blacklist_dispenser, item.getType())) {
            event.setCancelled(true);
        }
    }

    private class EntityPickupItemListener implements Listener {
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onPlayerPickupItem(final EntityPickupItemEvent event) {
            if (!(event.getEntity() instanceof Player)) return;

            final User user = ess.getUser((Player) event.getEntity());
            final ItemStack item = event.getItem().getItemStack();

            if (prot.getSettingBool(AntiBuildConfig.disable_use) && !user.canBuild()) {
                if (!metaPermCheck(user, "pickup", item)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private class PlayerPickupItemListener implements Listener {
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
            if (event.getPlayer().hasMetadata("NPC")) {
                return;
            }

            final User user = ess.getUser(event.getPlayer());
            final ItemStack item = event.getItem().getItemStack();

            if (prot.getSettingBool(AntiBuildConfig.disable_use) && !user.canBuild()) {
                if (!metaPermCheck(user, "pickup", item)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
