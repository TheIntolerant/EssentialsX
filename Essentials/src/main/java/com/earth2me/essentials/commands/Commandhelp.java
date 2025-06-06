package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.textreader.HelpInput;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.TextInput;
import com.earth2me.essentials.textreader.TextPager;
import com.earth2me.essentials.utils.AdventureUtil;
import com.earth2me.essentials.utils.NumberUtil;
import net.ess3.provider.KnownCommandsProvider;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

public class Commandhelp extends EssentialsCommand {
    public Commandhelp() {
        super("help");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final IText output;
        String pageStr = args.length > 0 ? args[0] : null;
        String chapterPageStr = args.length > 1 ? args[1] : null;
        String command = commandLabel;
        final IText input = new TextInput(user.getSource(), "help", false, ess);

        if (input.getLines().isEmpty()) {
            if (pageStr != null && pageStr.startsWith("/")) {
                final String cmd = pageStr.substring(1);
                for (final Map.Entry<String, Command> knownCmd : ess.provider(KnownCommandsProvider.class).getKnownCommands().entrySet()) {
                    if (knownCmd.getKey().equalsIgnoreCase(cmd)) {
                        final Command bukkit = knownCmd.getValue();
                        final boolean isEssCommand = bukkit instanceof PluginIdentifiableCommand && ((PluginIdentifiableCommand) bukkit).getPlugin().equals(ess);
                        final IEssentialsCommand essCommand = isEssCommand ? ess.getCommandMap().get(bukkit.getName()) : null;
                        user.sendTl("commandHelpLine1", cmd);
                        String description = bukkit.getDescription();
                        if (essCommand != null) {
                            try {
                                description = user.playerTl(bukkit.getName() + "CommandDescription");
                            } catch (MissingResourceException ignored) {}
                        }
                        user.sendTl("commandHelpLine2", description);
                        user.sendTl("commandHelpLine4", bukkit.getAliases().toString());
                        user.sendTl("commandHelpLine3");
                        if (essCommand != null && !essCommand.getUsageStrings().isEmpty()) {
                            for (Map.Entry<String, String> usage : essCommand.getUsageStrings().entrySet()) {
                                user.sendTl("commandHelpLineUsage", AdventureUtil.parsed(usage.getKey().replace("<command>", cmd)), AdventureUtil.parsed(usage.getValue()));
                            }
                        } else {
                            user.sendMessage(bukkit.getUsage());
                        }
                        return;
                    }
                }
            }

            if (NumberUtil.isInt(pageStr) || pageStr == null) {
                output = new HelpInput(user, "", ess);
            } else {
                if (pageStr.length() > 26) {
                    pageStr = pageStr.substring(0, 25);
                }
                output = new HelpInput(user, pageStr.toLowerCase(Locale.ENGLISH), ess);
                command = command.concat(" ").concat(pageStr);
                pageStr = chapterPageStr;
            }
            chapterPageStr = null;
        } else {
            user.setDisplayNick();
            output = new KeywordReplacer(input, user.getSource(), ess);
        }
        final TextPager pager = new TextPager(output);
        pager.showPage(pageStr, chapterPageStr, command, user.getSource());
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        sender.sendTl("helpConsole");
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> suggestions = new ArrayList<>(getCommands(server));
            suggestions.addAll(getPlugins(server));
            return suggestions;
        } else {
            return Collections.emptyList();
        }
    }
}
