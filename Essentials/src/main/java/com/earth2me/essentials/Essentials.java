/*
 * Essentials - a bukkit plugin
 * Copyright (C) 2011  Essentials Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.earth2me.essentials;

import com.earth2me.essentials.commands.EssentialsCommand;
import com.earth2me.essentials.commands.IEssentialsCommand;
import com.earth2me.essentials.commands.NoChargeException;
import com.earth2me.essentials.commands.NotEnoughArgumentsException;
import com.earth2me.essentials.commands.PlayerNotFoundException;
import com.earth2me.essentials.commands.QuietAbortException;
import com.earth2me.essentials.economy.EconomyLayers;
import com.earth2me.essentials.economy.vault.VaultEconomyProvider;
import com.earth2me.essentials.items.AbstractItemDb;
import com.earth2me.essentials.items.CustomItemResolver;
import com.earth2me.essentials.items.FlatItemDb;
import com.earth2me.essentials.items.LegacyItemDb;
import com.earth2me.essentials.metrics.MetricsWrapper;
import com.earth2me.essentials.perm.PermissionsDefaults;
import com.earth2me.essentials.perm.PermissionsHandler;
import com.earth2me.essentials.signs.SignBlockListener;
import com.earth2me.essentials.signs.SignEntityListener;
import com.earth2me.essentials.signs.SignPlayerListener;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.SimpleTextInput;
import com.earth2me.essentials.updatecheck.UpdateChecker;
import com.earth2me.essentials.userstorage.ModernUserMap;
import com.earth2me.essentials.utils.AdventureUtil;
import com.earth2me.essentials.utils.FormatUtil;
import com.earth2me.essentials.utils.VersionUtil;
import io.papermc.lib.PaperLib;
import net.ess3.api.Economy;
import net.ess3.api.IEssentials;
import net.ess3.api.IItemDb;
import net.ess3.api.IJails;
import net.ess3.api.ISettings;
import net.ess3.api.TranslatableException;
import net.ess3.nms.refl.providers.ReflDataWorldInfoProvider;
import net.ess3.nms.refl.providers.ReflFormattedCommandAliasProvider;
import net.ess3.nms.refl.providers.ReflKnownCommandsProvider;
import net.ess3.nms.refl.providers.ReflOnlineModeProvider;
import net.ess3.nms.refl.providers.ReflPersistentDataProvider;
import net.ess3.nms.refl.providers.ReflServerStateProvider;
import net.ess3.nms.refl.providers.ReflSpawnEggProvider;
import net.ess3.nms.refl.providers.ReflSpawnerBlockProvider;
import net.ess3.nms.refl.providers.ReflSyncCommandsProvider;
import net.ess3.provider.InventoryViewProvider;
import net.ess3.provider.KnownCommandsProvider;
import net.ess3.provider.PlayerLocaleProvider;
import net.ess3.provider.ProviderListener;
import net.ess3.provider.ServerStateProvider;
import net.ess3.provider.providers.BaseBannerDataProvider;
import net.ess3.provider.providers.BaseInventoryViewProvider;
import net.ess3.provider.providers.BlockMetaSpawnerItemProvider;
import net.ess3.provider.providers.BukkitMaterialTagProvider;
import net.ess3.provider.providers.BukkitSpawnerBlockProvider;
import net.ess3.provider.providers.FixedHeightWorldInfoProvider;
import net.ess3.provider.providers.FlatSpawnEggProvider;
import net.ess3.provider.providers.LegacyBannerDataProvider;
import net.ess3.provider.providers.LegacyDamageEventProvider;
import net.ess3.provider.providers.LegacyInventoryViewProvider;
import net.ess3.provider.providers.LegacyItemUnbreakableProvider;
import net.ess3.provider.providers.LegacyPlayerLocaleProvider;
import net.ess3.provider.providers.LegacyPotionMetaProvider;
import net.ess3.provider.providers.LegacySpawnEggProvider;
import net.ess3.provider.providers.ModernDamageEventProvider;
import net.ess3.provider.providers.ModernDataWorldInfoProvider;
import net.ess3.provider.providers.ModernItemUnbreakableProvider;
import net.ess3.provider.providers.ModernPersistentDataProvider;
import net.ess3.provider.providers.ModernPlayerLocaleProvider;
import net.ess3.provider.providers.ModernPotionMetaProvider;
import net.ess3.provider.providers.ModernSignDataProvider;
import net.ess3.provider.providers.ModernSyncCommandsProvider;
import net.ess3.provider.providers.PaperBiomeKeyProvider;
import net.ess3.provider.providers.PaperContainerProvider;
import net.ess3.provider.providers.PaperKnownCommandsProvider;
import net.ess3.provider.providers.PaperMaterialTagProvider;
import net.ess3.provider.providers.PaperRecipeBookListener;
import net.ess3.provider.providers.PaperSerializationProvider;
import net.ess3.provider.providers.PaperServerStateProvider;
import net.ess3.provider.providers.PaperTickCountProvider;
import net.ess3.provider.providers.PrehistoricPotionMetaProvider;
import net.essentialsx.api.v2.services.BalanceTop;
import net.essentialsx.api.v2.services.mail.MailService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.earth2me.essentials.I18n.tlLiteral;
import static com.earth2me.essentials.I18n.tlLocale;

public class Essentials extends JavaPlugin implements net.ess3.api.IEssentials {
    private static final Logger BUKKIT_LOGGER = Logger.getLogger("Essentials");
    private static Logger LOGGER = null;
    public static boolean TESTING = false;
    private final transient TNTExplodeListener tntListener = new TNTExplodeListener();
    private final transient Set<String> vanishedPlayers = new LinkedHashSet<>();
    private final transient Map<String, IEssentialsCommand> commandMap = new HashMap<>();
    private final transient ProviderFactory providerFactory = new ProviderFactory(this);
    private transient ISettings settings;
    private transient Jails jails;
    private transient Warps warps;
    private transient Worth worth;
    private transient List<IConf> confList;
    private transient Backup backup;
    private transient AbstractItemDb itemDb;
    private transient CustomItemResolver customItemResolver;
    private transient PermissionsHandler permissionsHandler;
    private transient AlternativeCommandsHandler alternativeCommandsHandler;
    @Deprecated
    private transient UserMap legacyUserMap;
    private transient ModernUserMap userMap;
    private transient BalanceTopImpl balanceTop;
    private transient ExecuteTimer execTimer;
    private transient MailService mail;
    private transient I18n i18n;
    private transient MetricsWrapper metrics;
    private transient EssentialsTimer timer;
    private transient ProviderListener recipeBookEventProvider;
    private transient Kits kits;
    private transient RandomTeleport randomTeleport;
    private transient UpdateChecker updateChecker;
    private transient BukkitAudiences bukkitAudience;

    static {
        EconomyLayers.init();
    }

    @Override
    public ISettings getSettings() {
        return settings;
    }

    @Override
    public void onLoad() {
        try {
            // Vault registers their Essentials provider at low priority, so we have to use normal priority here
            Class.forName("net.milkbowl.vault.economy.Economy");
            getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new VaultEconomyProvider(this), this, ServicePriority.Normal);
        } catch (final ClassNotFoundException ignored) {
            // Probably safer than fetching for the plugin as bukkit may not have marked it as enabled at this point in time
        }
    }

    @Override
    public void onEnable() {
        try {
            if (BUKKIT_LOGGER != super.getLogger()) {
                BUKKIT_LOGGER.setParent(super.getLogger());
            }
            LOGGER = EssentialsLogger.getLoggerProvider(this);
            EssentialsLogger.updatePluginLogger(this);

            execTimer = new ExecuteTimer();
            execTimer.start();

            final EssentialsUpgrade upgrade = new EssentialsUpgrade(this);
            upgrade.upgradeLang();
            execTimer.mark("AdventureUpgrade");

            i18n = new I18n(this);
            i18n.onEnable();
            execTimer.mark("I18n1");

            Console.setInstance(this);

            switch (VersionUtil.getServerSupportStatus()) {
                case NMS_CLEANROOM:
                    getLogger().severe(AdventureUtil.miniToLegacy(tlLiteral("serverUnsupportedCleanroom")));
                    break;
                case DANGEROUS_FORK:
                    getLogger().severe(AdventureUtil.miniToLegacy(tlLiteral("serverUnsupportedDangerous")));
                    break;
                case STUPID_PLUGIN:
                    getLogger().severe(AdventureUtil.miniToLegacy(tlLiteral("serverUnsupportedDumbPlugins")));
                    break;
                case UNSTABLE:
                    getLogger().severe(AdventureUtil.miniToLegacy(tlLiteral("serverUnsupportedMods")));
                    break;
                case OUTDATED:
                    getLogger().severe(AdventureUtil.miniToLegacy(tlLiteral("serverUnsupported")));
                    break;
                case LIMITED:
                    getLogger().info(AdventureUtil.miniToLegacy(tlLiteral("serverUnsupportedLimitedApi")));
                    break;
            }

            if (VersionUtil.getSupportStatusClass() != null) {
                getLogger().info(AdventureUtil.miniToLegacy(tlLiteral("serverUnsupportedClass", VersionUtil.getSupportStatusClass())));
            }

            final PluginManager pm = getServer().getPluginManager();
            for (final Plugin plugin : pm.getPlugins()) {
                if (plugin.getDescription().getName().startsWith("Essentials") && !plugin.getDescription().getVersion().equals(this.getDescription().getVersion()) && !plugin.getDescription().getName().equals("EssentialsAntiCheat")) {
                    getLogger().warning(AdventureUtil.miniToLegacy(tlLiteral("versionMismatch", plugin.getDescription().getName())));
                }
            }

            upgrade.beforeSettings();
            execTimer.mark("Upgrade");

            confList = new ArrayList<>();
            settings = new Settings(this);
            confList.add(settings);
            execTimer.mark("Settings");

            upgrade.preModules();
            execTimer.mark("Upgrade2");

            mail = new MailServiceImpl(this);
            execTimer.mark("Init(Mail)");

            userMap = new ModernUserMap(this);
            legacyUserMap = new UserMap(userMap);
            execTimer.mark("Init(Usermap)");

            balanceTop = new BalanceTopImpl(this);
            execTimer.mark("Init(BalanceTop)");

            kits = new Kits(this);
            confList.add(kits);
            upgrade.convertKits();
            execTimer.mark("Kits");

            randomTeleport = new RandomTeleport(this);
            confList.add(randomTeleport);
            execTimer.mark("Init(RandomTeleport)");

            upgrade.afterSettings();
            execTimer.mark("Upgrade3");

            warps = new Warps(this.getDataFolder());
            confList.add(warps);
            execTimer.mark("Init(Warp)");

            worth = new Worth(this.getDataFolder());
            confList.add(worth);
            execTimer.mark("Init(Worth)");

            itemDb = getItemDbFromConfig();
            confList.add(itemDb);
            execTimer.mark("Init(ItemDB)");

            customItemResolver = new CustomItemResolver(this);
            try {
                itemDb.registerResolver(this, "custom_items", customItemResolver);
                confList.add(customItemResolver);
            } catch (final Exception e) {
                e.printStackTrace();
                customItemResolver = null;
            }
            execTimer.mark("Init(CustomItemResolver)");

            jails = new Jails(this);
            confList.add(jails);
            execTimer.mark("Init(Jails)");

            EconomyLayers.onEnable(this);
            execTimer.mark("Init(EconomyLayers)");

            // Spawner item provider only uses one, but it's here for legacy...
            providerFactory.registerProvider(BlockMetaSpawnerItemProvider.class);

            // Spawner block providers
            providerFactory.registerProvider(ReflSpawnerBlockProvider.class, BukkitSpawnerBlockProvider.class);

            // Spawn Egg Providers
            providerFactory.registerProvider(LegacySpawnEggProvider.class, ReflSpawnEggProvider.class, FlatSpawnEggProvider.class);

            //Potion Meta Provider
            providerFactory.registerProvider(PrehistoricPotionMetaProvider.class, LegacyPotionMetaProvider.class, ModernPotionMetaProvider.class);

            //Banner Meta Provider
            providerFactory.registerProvider(LegacyBannerDataProvider.class, BaseBannerDataProvider.class);

            // Server State Provider
            providerFactory.registerProvider(ReflServerStateProvider.class, PaperServerStateProvider.class);

            // Container Provider
            providerFactory.registerProvider(PaperContainerProvider.class);

            // Serialization Provider
            providerFactory.registerProvider(PaperSerializationProvider.class);

            // Known Commands Provider
            providerFactory.registerProvider(ReflKnownCommandsProvider.class, PaperKnownCommandsProvider.class);

            // Command Aliases Provider
            providerFactory.registerProvider(ReflFormattedCommandAliasProvider.class);

            // Material Tag Providers
            providerFactory.registerProvider(BukkitMaterialTagProvider.class, PaperMaterialTagProvider.class);

            // Sync Commands Provider
            providerFactory.registerProvider(ReflSyncCommandsProvider.class, ModernSyncCommandsProvider.class);

            // Persistent Data Provider
            providerFactory.registerProvider(ReflPersistentDataProvider.class, ModernPersistentDataProvider.class);

            // Online Mode Provider
            providerFactory.registerProvider(ReflOnlineModeProvider.class);

            // Unbreakable Provider
            providerFactory.registerProvider(LegacyItemUnbreakableProvider.class, ModernItemUnbreakableProvider.class);

            // World Info Provider
            providerFactory.registerProvider(FixedHeightWorldInfoProvider.class, ReflDataWorldInfoProvider.class, ModernDataWorldInfoProvider.class);

            // Sign Data Provider
            providerFactory.registerProvider(ModernSignDataProvider.class);

            // Player Locale Provider
            providerFactory.registerProvider(ModernPlayerLocaleProvider.class, LegacyPlayerLocaleProvider.class);

            // Damage Event Provider
            providerFactory.registerProvider(ModernDamageEventProvider.class, LegacyDamageEventProvider.class);

            // Inventory View Provider
            providerFactory.registerProvider(LegacyInventoryViewProvider.class, BaseInventoryViewProvider.class);

            // Biome Key Provider
            providerFactory.registerProvider(PaperBiomeKeyProvider.class);

            // Tick Count Provider
            providerFactory.registerProvider(PaperTickCountProvider.class);

            if (!TESTING) {
                providerFactory.finalizeRegistration();
            }

            // Event Providers
            if (PaperLib.isPaper()) {
                try {
                    Class.forName("com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent");
                    recipeBookEventProvider = new PaperRecipeBookListener(event -> {
                        if (this.getUser(((PlayerEvent) event).getPlayer()).isRecipeSee()) {
                            ((Cancellable) event).setCancelled(true);
                        }
                    });
                    if (getSettings().isDebug()) {
                        LOGGER.log(Level.INFO, "Registered Paper Recipe Book Event Listener");
                    }
                } catch (final ClassNotFoundException ignored) {
                }
            }

            execTimer.mark("Init(Providers)");
            reload();

            // The item spawn blacklist is loaded with all other settings, before the item
            // DB, but it depends on the item DB, so we need to reload it again here:
            ((Settings) settings)._lateLoadItemSpawnBlacklist();
            backup = new Backup(this);
            permissionsHandler = new PermissionsHandler(this, settings.useBukkitPermissions());
            alternativeCommandsHandler = new AlternativeCommandsHandler(this);

            timer = new EssentialsTimer(this);
            scheduleSyncRepeatingTask(timer, 1000, 50);

            Economy.setEss(this);
            execTimer.mark("RegHandler");

            // Register /hat and /back default permissions
            PermissionsDefaults.registerAllBackDefaults();
            PermissionsDefaults.registerAllHatDefaults();

            if (!TESTING) {
                updateChecker = new UpdateChecker(this);
                runTaskAsynchronously(() -> {
                    getLogger().log(Level.INFO, AdventureUtil.miniToLegacy(tlLiteral("versionFetching")));
                    for (final Component component : updateChecker.getVersionMessages(false, true, new CommandSource(this, Bukkit.getConsoleSender()))) {
                        getLogger().log(getSettings().isUpdateCheckEnabled() ? Level.WARNING : Level.INFO, AdventureUtil.adventureToLegacy(component));
                    }
                });

                metrics = new MetricsWrapper(this, 858, true);
            }

            execTimer.mark("Init(External)");

            final String timeroutput = execTimer.end();
            if (getSettings().isDebug()) {
                LOGGER.log(Level.INFO, "Essentials load " + timeroutput);
            }
        } catch (final NumberFormatException ex) {
            handleCrash(ex);
        } catch (final Error ex) {
            handleCrash(ex);
            throw ex;
        }
        if (!TESTING) {
            getBackup().setPendingShutdown(false);
        }
    }

    // Returns our provider logger if available
    public static Logger getWrappedLogger() {
        if (LOGGER != null) {
            return LOGGER;
        }

        return BUKKIT_LOGGER;
    }

    @Override
    public void saveConfig() {
        // We don't use any of the bukkit config writing, as this breaks our config file formatting.
    }

    private void registerListeners(final PluginManager pm) {
        HandlerList.unregisterAll(this);

        if (getSettings().isDebug()) {
            LOGGER.log(Level.INFO, "Registering Listeners");
        }

        final EssentialsPluginListener pluginListener = new EssentialsPluginListener(this);
        pm.registerEvents(pluginListener, this);
        confList.add(pluginListener);

        final EssentialsPlayerListener playerListener = new EssentialsPlayerListener(this);
        playerListener.registerEvents();

        final EssentialsBlockListener blockListener = new EssentialsBlockListener(this);
        pm.registerEvents(blockListener, this);

        final SignBlockListener signBlockListener = new SignBlockListener(this);
        pm.registerEvents(signBlockListener, this);

        final SignPlayerListener signPlayerListener = new SignPlayerListener(this);
        pm.registerEvents(signPlayerListener, this);

        final SignEntityListener signEntityListener = new SignEntityListener(this);
        pm.registerEvents(signEntityListener, this);

        final EssentialsEntityListener entityListener = new EssentialsEntityListener(this);
        pm.registerEvents(entityListener, this);

        final EssentialsWorldListener worldListener = new EssentialsWorldListener(this);
        pm.registerEvents(worldListener, this);

        final EssentialsServerListener serverListener = new EssentialsServerListener(this);
        pm.registerEvents(serverListener, this);

        pm.registerEvents(tntListener, this);

        if (recipeBookEventProvider != null) {
            pm.registerEvents(recipeBookEventProvider, this);
        }

        jails.resetListener();
    }

    @Override
    public ProviderFactory getProviders() {
        return providerFactory;
    }

    @Override
    public void onDisable() {
        if (bukkitAudience != null) {
            bukkitAudience.close();
        }

        final boolean stopping = TESTING || provider(ServerStateProvider.class).isStopping();
        if (!stopping) {
            LOGGER.log(Level.SEVERE, AdventureUtil.miniToLegacy(tlLiteral("serverReloading")));
        }

        if (!TESTING) {
            getBackup().setPendingShutdown(true);
        }

        for (final User user : getOnlineUsers()) {
            if (user.isVanished()) {
                user.setVanished(false);
                user.sendTl("unvanishedReload");
            }
            if (stopping) {
                user.setLogoutLocation();
                if (!user.isHidden()) {
                    user.setLastLogout(System.currentTimeMillis());
                }
                user.cleanup();
            } else {
                user.stopTransaction();
            }
        }
        cleanupOpenInventories();
        if (!TESTING && getBackup().getTaskLock() != null && !getBackup().getTaskLock().isDone()) {
            LOGGER.log(Level.SEVERE, AdventureUtil.miniToLegacy(tlLiteral("backupInProgress")));
            getBackup().getTaskLock().join();
        }
        if (i18n != null) {
            i18n.onDisable();
        }
        if (backup != null) {
            backup.stopTask();
        }

        if (!TESTING) {
            this.getPermissionsHandler().unregisterContexts();
        }

        Economy.setEss(null);
        AdventureUtil.setEss(null);
        Trade.closeLog();
        getUsers().shutdown();

        HandlerList.unregisterAll(this);
    }

    @Override
    public void reload() {
        Trade.closeLog();

        if (bukkitAudience != null) {
            bukkitAudience.close();
            bukkitAudience = null;
        }

        for (final IConf iConf : confList) {
            iConf.reloadConfig();
            execTimer.mark("Reload(" + iConf.getClass().getSimpleName() + ")");
        }

        i18n.updateLocale(settings.getLocale());
        for (final String commandName : this.getDescription().getCommands().keySet()) {
            final Command command = this.getCommand(commandName);
            if (command != null) {
                command.setDescription(AdventureUtil.miniToLegacy(tlLiteral(commandName + "CommandDescription")));
                command.setUsage(AdventureUtil.miniToLegacy(tlLiteral(commandName + "CommandUsage")));
            }
        }

        final PluginManager pm = getServer().getPluginManager();
        registerListeners(pm);

        AdventureUtil.setEss(this);
        bukkitAudience = BukkitAudiences.create(this);
    }

    private IEssentialsCommand loadCommand(final String path, final String name, final IEssentialsModule module, final ClassLoader classLoader) throws Exception {
        if (commandMap.containsKey(name)) {
            return commandMap.get(name);
        }
        final IEssentialsCommand cmd = (IEssentialsCommand) classLoader.loadClass(path + name).getDeclaredConstructor().newInstance();
        cmd.setEssentials(this);
        cmd.setEssentialsModule(module);
        commandMap.put(name, cmd);
        return cmd;
    }

    public Map<String, IEssentialsCommand> getCommandMap() {
        return commandMap;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String commandLabel, final String[] args) {
        return onTabCompleteEssentials(sender, command, commandLabel, args, Essentials.class.getClassLoader(),
            "com.earth2me.essentials.commands.Command", "essentials.", null);
    }

    @Override
    public List<String> onTabCompleteEssentials(final CommandSender cSender, final Command command, final String commandLabel, final String[] args,
                                                final ClassLoader classLoader, final String commandPath, final String permissionPrefix,
                                                final IEssentialsModule module) {
        if (!getSettings().isCommandOverridden(command.getName()) && (!commandLabel.startsWith("e") || commandLabel.equalsIgnoreCase(command.getName()))) {
            final Command pc = alternativeCommandsHandler.getAlternative(commandLabel);
            if (pc instanceof PluginCommand) {
                try {
                    final TabCompleter completer = ((PluginCommand) pc).getTabCompleter();
                    if (completer != null) {
                        return completer.onTabComplete(cSender, command, commandLabel, args);
                    }
                } catch (final Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }

        try {
            // Note: The tab completer is always a player, even when tab-completing in a command block
            User user = null;
            if (cSender instanceof Player) {
                user = getUser((Player) cSender);
            }

            final CommandSource sender = new CommandSource(this, cSender);

            // Check for disabled commands
            if (getSettings().isCommandDisabled(commandLabel)) {
                final Command newCmd = provider(KnownCommandsProvider.class).getKnownCommands().get(commandLabel);
                if (newCmd != null && (!(newCmd instanceof PluginIdentifiableCommand) || ((PluginIdentifiableCommand) newCmd).getPlugin() != this)) {
                    return newCmd.tabComplete(cSender, commandLabel, args);
                }
                return Collections.emptyList();
            }

            final IEssentialsCommand cmd;
            try {
                cmd = loadCommand(commandPath, command.getName(), module, classLoader);
            } catch (final Exception ex) {
                sender.sendTl("commandNotLoaded", commandLabel);
                LOGGER.log(Level.SEVERE, AdventureUtil.miniToLegacy(tlLiteral("commandNotLoaded", commandLabel)), ex);
                return Collections.emptyList();
            }

            // Check authorization
            if (user != null && !user.isAuthorized(cmd, permissionPrefix)) {
                return Collections.emptyList();
            }

            if (user != null && user.isJailed() && !user.isAuthorized(cmd, "essentials.jail.allow.")) {
                return Collections.emptyList();
            }

            // Run the command
            try {
                if (user == null) {
                    return cmd.tabComplete(getServer(), sender, commandLabel, command, args);
                } else {
                    return cmd.tabComplete(getServer(), user, commandLabel, command, args);
                }
            } catch (final Exception ex) {
                showError(sender, ex, commandLabel);
                // Tab completion shouldn't fail
                LOGGER.log(Level.SEVERE, AdventureUtil.miniToLegacy(tlLiteral("commandFailed", commandLabel)), ex);
                return Collections.emptyList();
            }
        } catch (final Throwable ex) {
            LOGGER.log(Level.SEVERE, AdventureUtil.miniToLegacy(tlLiteral("commandFailed", commandLabel)), ex);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String commandLabel, final String[] args) {
        metrics.markCommand(command.getName(), true);
        return onCommandEssentials(sender, command, commandLabel, args, Essentials.class.getClassLoader(), "com.earth2me.essentials.commands.Command", "essentials.", null);
    }

    @Override
    public boolean onCommandEssentials(final CommandSender cSender, final Command command, final String commandLabel, final String[] args, final ClassLoader classLoader, final String commandPath, final String permissionPrefix, final IEssentialsModule module) {
        // Allow plugins to override the command via onCommand
        if (!getSettings().isCommandOverridden(command.getName()) && (!commandLabel.startsWith("e") || commandLabel.equalsIgnoreCase(command.getName()))) {
            if (getSettings().isDebug()) {
                LOGGER.log(Level.INFO, "Searching for alternative to: " + commandLabel);
            }
            final Command pc = alternativeCommandsHandler.getAlternative(commandLabel);
            if (pc != null) {
                alternativeCommandsHandler.executed(commandLabel, pc);
                try {
                    pc.execute(cSender, commandLabel, args);
                } catch (final Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    if (cSender instanceof Player) {
                        final PlayerLocaleProvider localeProvider = provider(PlayerLocaleProvider.class);
                        getBukkitAudience().sender(cSender).sendMessage(AdventureUtil.miniMessage().deserialize(tlLocale(I18n.getLocale(localeProvider.getLocale((Player) cSender)), "internalError")));
                    } else {
                        cSender.sendMessage(tlLiteral("internalError"));
                    }
                }
                return true;
            }
        }

        try {

            User user = null;
            Block bSenderBlock = null;
            if (cSender instanceof Player) {
                user = getUser((Player) cSender);
            } else if (cSender instanceof BlockCommandSender) {
                final BlockCommandSender bsender = (BlockCommandSender) cSender;
                bSenderBlock = bsender.getBlock();
            }

            if (bSenderBlock != null) {
                if (getSettings().logCommandBlockCommands()) {
                    LOGGER.log(Level.INFO, "CommandBlock at " + bSenderBlock.getX() + "," + bSenderBlock.getY() + "," + bSenderBlock.getZ() + " issued server command: /" + commandLabel + " " + EssentialsCommand.getFinalArg(args, 0));
                }
            } else if (user == null) {
                LOGGER.log(Level.INFO, cSender.getName()+ " issued server command: /" + commandLabel + " " + EssentialsCommand.getFinalArg(args, 0));
            }

            final CommandSource sender = new CommandSource(this, cSender);

            // New mail notification
            if (user != null && !getSettings().isCommandDisabled("mail") && !command.getName().equals("mail") && user.isAuthorized("essentials.mail")) {
                user.notifyOfMail();
            }

            //Print version even if admin command is not available #easteregg
            if (commandLabel.equalsIgnoreCase("essversion")) {
                sender.sendMessage("This server is running Essentials " + getDescription().getVersion());
                return true;
            }

            // Check for disabled commands
            if (getSettings().isCommandDisabled(commandLabel)) {
                final Command newCmd = provider(KnownCommandsProvider.class).getKnownCommands().get(commandLabel);
                if (newCmd != null && (!(newCmd instanceof PluginIdentifiableCommand) || !isEssentialsPlugin(((PluginIdentifiableCommand) newCmd).getPlugin()))) {
                    return newCmd.execute(cSender, commandLabel, args);
                }
                sender.sendTl("commandDisabled", commandLabel);
                return true;
            }

            final IEssentialsCommand cmd;
            try {
                cmd = loadCommand(commandPath, command.getName(), module, classLoader);
            } catch (final Exception ex) {
                sender.sendTl("commandNotLoaded", commandLabel);
                LOGGER.log(Level.SEVERE, AdventureUtil.miniToLegacy(tlLiteral("commandNotLoaded", commandLabel)), ex);
                return true;
            }

            // Check authorization
            if (user != null && !user.isAuthorized(cmd, permissionPrefix)) {
                LOGGER.log(Level.INFO, AdventureUtil.miniToLegacy(tlLiteral("deniedAccessCommand", user.getName())));
                user.sendTl("noAccessCommand");
                return true;
            }

            if (user != null && user.isJailed() && !user.isAuthorized(cmd, "essentials.jail.allow.")) {
                if (user.getJailTimeout() > 0) {
                    user.sendTl("playerJailedFor", user.getName(), user.getFormattedJailTime());
                } else {
                    user.sendTl("jailMessage");
                }
                return true;
            }

            // Run the command
            try {
                if (user == null) {
                    cmd.run(getServer(), sender, commandLabel, command, args);
                } else {
                    cmd.run(getServer(), user, commandLabel, command, args);
                }
                return true;
            } catch (final NoChargeException | QuietAbortException ex) {
                return true;
            } catch (final NotEnoughArgumentsException ex) {
                if (getSettings().isVerboseCommandUsages() && !cmd.getUsageStrings().isEmpty()) {
                    sender.sendTl("commandHelpLine1", commandLabel);
                    String description = command.getDescription();
                    try {
                        description = sender.tl(command.getName() + "CommandDescription");
                    } catch (MissingResourceException ignored) {}
                    sender.sendTl("commandHelpLine2", description);
                    sender.sendTl("commandHelpLine3");
                    for (Map.Entry<String, String> usage : cmd.getUsageStrings().entrySet()) {
                        sender.sendTl("commandHelpLineUsage", AdventureUtil.parsed(usage.getKey().replace("<command>", commandLabel)), AdventureUtil.parsed(usage.getValue()));
                    }
                } else {
                    sender.sendMessage(command.getDescription());
                    sender.sendMessage(command.getUsage().replace("<command>", commandLabel));
                }
                if (!ex.getMessage().isEmpty()) {
                    sender.sendComponent(AdventureUtil.miniMessage().deserialize(ex.getMessage()));
                }
                if (ex.getCause() != null && settings.isDebug()) {
                    ex.getCause().printStackTrace();
                }
                return true;
            } catch (final Exception ex) {
                showError(sender, ex, commandLabel);
                if (settings.isDebug()) {
                    ex.printStackTrace();
                }
                return true;
            }
        } catch (final Throwable ex) {
            LOGGER.log(Level.SEVERE, AdventureUtil.miniToLegacy(tlLiteral("commandFailed", commandLabel)), ex);
            return true;
        }
    }

    private boolean isEssentialsPlugin(Plugin plugin) {
        return plugin.getDescription().getMain().contains("com.earth2me.essentials") || plugin.getDescription().getMain().contains("net.essentialsx");
    }

    public void cleanupOpenInventories() {
        final InventoryViewProvider provider = provider(InventoryViewProvider.class);
        for (final User user : getOnlineUsers()) {
            if (user.isRecipeSee()) {
                final InventoryView view = user.getBase().getOpenInventory();

                provider.getTopInventory(view).clear();
                provider.close(view);
                user.setRecipeSee(false);
            }
            if (user.isInvSee() || user.isEnderSee()) {
                provider.close(user.getBase().getOpenInventory());
                user.setInvSee(false);
                user.setEnderSee(false);
            }
        }
    }

    @Override
    public void showError(final CommandSource sender, final Throwable exception, final String commandLabel) {
        if (exception instanceof TranslatableException) {
            final String tlMessage = sender.tl(((TranslatableException) exception).getTlKey(), ((TranslatableException) exception).getArgs());
            sender.sendTl("errorWithMessage", AdventureUtil.parsed(tlMessage));
        } else {
            sender.sendTl("errorWithMessage", exception.getMessage());
        }
        if (getSettings().isDebug()) {
            LOGGER.log(Level.INFO, AdventureUtil.miniToLegacy(tlLiteral("errorCallingCommand", commandLabel)), exception);
        }
    }

    @Override
    public BukkitScheduler getScheduler() {
        return this.getServer().getScheduler();
    }

    @Override
    public IJails getJails() {
        return jails;
    }

    @Override
    public Warps getWarps() {
        return warps;
    }

    @Override
    public Worth getWorth() {
        return worth;
    }

    @Override
    public Backup getBackup() {
        return backup;
    }

    @Override
    public Kits getKits() {
        return kits;
    }

    @Override
    public RandomTeleport getRandomTeleport() {
        return randomTeleport;
    }

    @Override
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    @Deprecated
    @Override
    public User getUser(final Object base) {
        if (base instanceof Player) {
            return getUser((Player) base);
        }
        if (base instanceof org.bukkit.OfflinePlayer) {
            return getUser(((org.bukkit.OfflinePlayer) base).getUniqueId());
        }
        if (base instanceof UUID) {
            return getUser((UUID) base);
        }
        if (base instanceof String) {
            return getOfflineUser((String) base);
        }
        return null;
    }

    //This will return null if there is not a match.
    @Override
    public User getUser(final String base) {
        return getOfflineUser(base);
    }

    //This will return null if there is not a match.
    @Override
    public User getUser(final UUID base) {
        return userMap.getUser(base);
    }

    //This will return null if there is not a match.
    @Override
    public User getOfflineUser(final String name) {
        return userMap.getUser(name);
    }

    @Override
    public User matchUser(final Server server, final User sourceUser, final String searchTerm, final Boolean getHidden, final boolean getOffline) throws PlayerNotFoundException {
        final User user;
        Player exPlayer;

        try {
            exPlayer = server.getPlayer(UUID.fromString(searchTerm));
        } catch (final IllegalArgumentException ex) {
            if (getOffline) {
                exPlayer = server.getPlayerExact(searchTerm);
            } else {
                exPlayer = server.getPlayer(searchTerm);
            }
        }

        if (exPlayer != null) {
            user = getUser(exPlayer);
        } else {
            user = getUser(searchTerm);
        }

        if (user != null) {
            if (!getOffline && !user.getBase().isOnline()) {
                throw new PlayerNotFoundException();
            }

            if (getHidden || canInteractWith(sourceUser, user)) {
                return user;
            } else { // not looking for hidden and cannot interact (i.e is hidden)
                if (getOffline && user.getName().equalsIgnoreCase(searchTerm)) { // if looking for offline and got an exact match
                    return user;
                }
            }
            throw new PlayerNotFoundException();
        }
        final List<Player> matches = server.matchPlayer(searchTerm);

        if (matches.isEmpty()) {
            final String matchText = searchTerm.toLowerCase(Locale.ENGLISH);
            for (final User userMatch : getOnlineUsers()) {
                if (getHidden || canInteractWith(sourceUser, userMatch)) {
                    final String displayName = FormatUtil.stripFormat(userMatch.getDisplayName()).toLowerCase(Locale.ENGLISH);
                    if (displayName.contains(matchText)) {
                        return userMatch;
                    }
                }
            }
        } else {
            for (final Player player : matches) {
                final User userMatch = getUser(player);
                if (userMatch.getDisplayName().startsWith(searchTerm) && (getHidden || canInteractWith(sourceUser, userMatch))) {
                    return userMatch;
                }
            }
            final User userMatch = getUser(matches.get(0));
            if (getHidden || canInteractWith(sourceUser, userMatch)) {
                return userMatch;
            }
        }
        throw new PlayerNotFoundException();
    }

    @Override
    public boolean canInteractWith(final CommandSource interactor, final User interactee) {
        if (interactor == null) {
            return !interactee.isHidden();
        }

        if (interactor.isPlayer()) {
            return canInteractWith(getUser(interactor.getPlayer()), interactee);
        }

        return true; // console
    }

    @Override
    public boolean canInteractWith(final User interactor, final User interactee) {
        if (interactor == null) {
            return !interactee.isHidden();
        }

        if (interactor.equals(interactee)) {
            return true;
        }

        return !interactee.isHiddenFrom(interactor.getBase());
    }

    //This will create a new user if there is not a match.
    @Override
    public User getUser(final Player base) {
        if (base == null) {
            return null;
        }

        if (userMap == null) {
            LOGGER.log(Level.WARNING, "Essentials userMap not initialized");
            return null;
        }

        final User user = userMap.getUser(base);

        if (base.getClass() != UUIDPlayer.class || user.getBase() == null) {
            user.update(base);
        }
        return user;
    }

    private void handleCrash(final Throwable exception) {
        final PluginManager pm = getServer().getPluginManager();
        getWrappedLogger().log(Level.SEVERE, exception.toString(), exception);
        pm.registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            public void onPlayerJoin(final PlayerJoinEvent event) {
                event.getPlayer().sendMessage("Essentials failed to load, read the log file.");
            }
        }, this);
        for (final Player player : getOnlinePlayers()) {
            player.sendMessage("Essentials failed to load, read the log file.");
        }
        this.setEnabled(false);
    }

    @Override
    public World getWorld(final String name) {
        if (name.matches("[0-9]+")) {
            final int worldId = Integer.parseInt(name);
            if (worldId < getServer().getWorlds().size()) {
                return getServer().getWorlds().get(worldId);
            }
        }
        return getServer().getWorld(name);
    }

    @Override
    public void addReloadListener(final IConf listener) {
        confList.add(listener);
    }

    @Override
    public int broadcastMessage(final String message) {
        return broadcastMessage(null, null, message, true, null);
    }

    @Override
    public int broadcastMessage(final IUser sender, final String message) {
        return broadcastMessage(sender, null, message, false, null);
    }

    @Override
    public int broadcastMessage(final IUser sender, final String message, final Predicate<IUser> shouldExclude) {
        return broadcastMessage(sender, null, message, false, shouldExclude);
    }

    @Override
    public int broadcastMessage(final String permission, final String message) {
        return broadcastMessage(null, permission, message, false, null);
    }

    private int broadcastMessage(final IUser sender, final String permission, final String message, final boolean keywords, final Predicate<IUser> shouldExclude) {
        if (sender != null && sender.isHidden()) {
            return 0;
        }

        IText broadcast = new SimpleTextInput(message);

        final Collection<Player> players = getOnlinePlayers();
        for (final Player player : players) {
            final User user = getUser(player);
            if ((permission == null && (sender == null || !user.isIgnoredPlayer(sender))) || (permission != null && user.isAuthorized(permission))) {
                if (shouldExclude != null && shouldExclude.test(user)) {
                    continue;
                }
                if (keywords) {
                    broadcast = new KeywordReplacer(broadcast, new CommandSource(this, player), this, false);
                }
                for (final String messageText : broadcast.getLines()) {
                    user.sendMessage(messageText);
                }
            }
        }

        return players.size();
    }

    @Override
    public void broadcastTl(final String tlKey, final Object... args) {
        broadcastTl(null, null, false, tlKey, args);
    }

    @Override
    public void broadcastTl(final IUser sender, final String tlKey, final Object... args) {
        broadcastTl(sender, null, false, tlKey, args);
    }

    @Override
    public void broadcastTl(final IUser sender, final String permission, final String tlKey, final Object... args) {
        broadcastTl(sender, u -> !u.isAuthorized(permission), false, tlKey, args);
    }

    @Override
    public void broadcastTl(IUser sender, Predicate<IUser> shouldExclude, String tlKey, Object... args) {
        broadcastTl(sender, shouldExclude, false, tlKey, args);
    }

    @Override
    public void broadcastTl(final IUser sender, final Predicate<IUser> shouldExclude, final boolean parseKeywords, final String tlKey, final Object... args) {
        if (sender != null && sender.isHidden()) {
            return;
        }

        for (final User user : getOnlineUsers()) {
            if (sender != null && user.isIgnoredPlayer(sender)) {
                continue;
            }

            if (shouldExclude != null && shouldExclude.test(user)) {
                continue;
            }

            final Object[] processedArgs;
            if (parseKeywords) {
                processedArgs = I18n.mutateArgs(args, s -> new KeywordReplacer(new SimpleTextInput(s.toString()), new CommandSource(this, user.getBase()), this, false).getLines().get(0));
            } else {
                processedArgs = args;
            }

            user.sendTl(tlKey, processedArgs);
        }
    }

    @Override
    public BukkitTask runTaskAsynchronously(final Runnable run) {
        return this.getScheduler().runTaskAsynchronously(this, run);
    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(final Runnable run, final long delay) {
        return this.getScheduler().runTaskLaterAsynchronously(this, run, delay);
    }

    @Override
    public BukkitTask runTaskTimerAsynchronously(final Runnable run, final long delay, final long period) {
        return this.getScheduler().runTaskTimerAsynchronously(this, run, delay, period);
    }

    @Override
    public int scheduleSyncDelayedTask(final Runnable run) {
        return this.getScheduler().scheduleSyncDelayedTask(this, run);
    }

    @Override
    public int scheduleSyncDelayedTask(final Runnable run, final long delay) {
        return this.getScheduler().scheduleSyncDelayedTask(this, run, delay);
    }

    @Override
    public int scheduleSyncRepeatingTask(final Runnable run, final long delay, final long period) {
        return this.getScheduler().scheduleSyncRepeatingTask(this, run, delay, period);
    }

    @Override
    public PermissionsHandler getPermissionsHandler() {
        return permissionsHandler;
    }

    @Override
    public AlternativeCommandsHandler getAlternativeCommandsHandler() {
        return alternativeCommandsHandler;
    }

    @Override
    public IItemDb getItemDb() {
        return itemDb;
    }

    @Override
    @Deprecated
    public UserMap getUserMap() {
        return legacyUserMap;
    }

    @Override
    public ModernUserMap getUsers() {
        return userMap;
    }

    @Override
    public BalanceTop getBalanceTop() {
        return balanceTop;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }

    @Override
    public EssentialsTimer getTimer() {
        return timer;
    }

    @Override
    public MailService getMail() {
        return mail;
    }

    @Override
    public List<String> getVanishedPlayers() {
        return Collections.unmodifiableList(new ArrayList<>(vanishedPlayers));
    }

    @Override
    public Collection<String> getVanishedPlayersNew() {
        return vanishedPlayers;
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return (Collection<Player>) getServer().getOnlinePlayers();
    }

    @Override
    public Iterable<User> getOnlineUsers() {
        final List<User> onlineUsers = new ArrayList<>();
        for (final Player player : getOnlinePlayers()) {
            onlineUsers.add(getUser(player));
        }
        return onlineUsers;
    }

    @Override
    public CustomItemResolver getCustomItemResolver() {
        return customItemResolver;
    }

    @Override
    public PluginCommand getPluginCommand(final String cmd) {
        return this.getCommand(cmd);
    }

    public BukkitAudiences getBukkitAudience() {
        return bukkitAudience;
    }

    private AbstractItemDb getItemDbFromConfig() {
        final String setting = settings.getItemDbType();

        if (setting.equalsIgnoreCase("json")) {
            return new FlatItemDb(this);
        } else if (setting.equalsIgnoreCase("csv")) {
            return new LegacyItemDb(this);
        } else {
            final VersionUtil.BukkitVersion version = VersionUtil.getServerBukkitVersion();

            if (version.isHigherThanOrEqualTo(VersionUtil.v1_13_0_R01)) {
                return new FlatItemDb(this);
            } else {
                return new LegacyItemDb(this);
            }
        }
    }

    private static class EssentialsWorldListener implements Listener, Runnable {
        private transient final IEssentials ess;

        EssentialsWorldListener(final IEssentials ess) {
            this.ess = ess;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onWorldLoad(final WorldLoadEvent event) {
            PermissionsDefaults.registerBackDefaultFor(event.getWorld());
        }

        @Override
        public void run() {
            ess.reload();
        }
    }
}
