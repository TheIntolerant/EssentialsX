package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.FormatUtil;
import net.ess3.api.TranslatableException;
import net.ess3.api.events.NickChangeEvent;
import org.bukkit.ChatColor;
import org.bukkit.Server;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class Commandnick extends EssentialsLoopCommand {
    public Commandnick() {
        super("nick");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        if (args.length > 1 && user.isAuthorized("essentials.nick.others")) {
            loopOfflinePlayers(server, user.getSource(), false, true, args[0], formatNickname(user, args[1]).split(" "));
            user.sendTl("nickChanged");
        } else {
            updatePlayer(server, user.getSource(), user, formatNickname(user, args[0]).split(" "));
        }
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        loopOfflinePlayers(server, sender, false, true, args[0], formatNickname(null, args[1]).split(" "));
        sender.sendTl("nickChanged");
    }

    @Override
    protected void updatePlayer(final Server server, final CommandSource sender, final User target, final String[] args) throws NotEnoughArgumentsException {
        final String nick = args[0];
        if ("off".equalsIgnoreCase(nick)) {
            setNickname(server, sender, target, null);
            target.sendTl("nickNoMore");
        } else if (target.getName().equalsIgnoreCase(nick)) {
            setNickname(server, sender, target, nick);
            if (!target.getDisplayName().equalsIgnoreCase(target.getDisplayName())) {
                target.sendTl("nickNoMore");
            }
            target.sendTl("nickSet", ess.getSettings().changeDisplayName() ? target.getDisplayName() : nick);
        } else if (nickInUse(target, nick)) {
            throw new NotEnoughArgumentsException(sender.tl("nickInUse"));
        } else {
            setNickname(server, sender, target, nick);
            target.sendTl("nickSet", ess.getSettings().changeDisplayName() ? target.getDisplayName() : nick);
        }
    }

    private String formatNickname(final User user, final String nick) throws Exception {
        final String newNick = user == null ? FormatUtil.replaceFormat(nick) : FormatUtil.formatString(user, "essentials.nick", nick);
        if (!newNick.matches(ess.getSettings().getNickRegex()) && user != null && !user.isAuthorized("essentials.nick.allowunsafe")) {
            throw new TranslatableException("nickNamesAlpha");
        } else if (getNickLength(newNick) > ess.getSettings().getMaxNickLength()) {
            throw new TranslatableException("nickTooLong");
        } else if (FormatUtil.stripFormat(newNick).length() < 1) {
            throw new TranslatableException("nickNamesAlpha");
        } else if (user != null && user.isAuthorized("essentials.nick.changecolors") && !user.isAuthorized("essentials.nick.changecolors.bypass") && !FormatUtil.stripFormat(newNick).equals(user.getName()) && !nick.equalsIgnoreCase("off")) {
            throw new TranslatableException("nickNamesOnlyColorChanges");
        } else if (user != null && !user.isAuthorized("essentials.nick.blacklist.bypass") && isNickBanned(newNick)) {
            throw new TranslatableException("nickNameBlacklist", nick);
        }
        return newNick;
    }

    private boolean isNickBanned(final String newNick) {
        for (final Predicate<String> predicate : ess.getSettings().getNickBlacklist()) {
            if (predicate.test(newNick)) {
                return true;
            }
        }
        return false;
    }

    private int getNickLength(final String nick) {
        return ess.getSettings().ignoreColorsInMaxLength() ? ChatColor.stripColor(nick).length() : nick.length();
    }

    private boolean nickInUse(final User target, final String nick) {
        final String lowerNick = FormatUtil.stripFormat(nick.toLowerCase(Locale.ENGLISH));
        for (final User onlinePlayer : ess.getOnlineUsers()) {
            if (target.getBase().getName().equals(onlinePlayer.getName())) {
                continue;
            }

            final String matchNick = FormatUtil.stripFormat(onlinePlayer.getNickname());
            if ((matchNick != null && !matchNick.isEmpty() && lowerNick.equals(matchNick.toLowerCase(Locale.ENGLISH))) || lowerNick.equals(onlinePlayer.getName().toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        final User fetchedUser = ess.getUser(lowerNick);
        return fetchedUser != null && fetchedUser != target;
    }

    private void setNickname(final Server server, final CommandSource sender, final User target, final String nickname) {
        final NickChangeEvent nickEvent = new NickChangeEvent(sender.getUser(), target, nickname);
        server.getPluginManager().callEvent(nickEvent);
        if (!nickEvent.isCancelled()) {
            target.setNickname(nickname);
            target.setDisplayNick();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.nick.others")) {
            return getPlayers(server, sender);
        } else {
            return Collections.emptyList();
        }
    }
}
