package com.earth2me.essentials.signs;

import com.earth2me.essentials.ChargeException;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.Trade.OverflowType;
import com.earth2me.essentials.Trade.TradeType;
import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.Inventories;
import com.earth2me.essentials.utils.MaterialUtil;
import com.earth2me.essentials.utils.NumberUtil;
import net.ess3.api.IEssentials;
import net.ess3.api.MaxMoneyException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Map;

public class SignTrade extends EssentialsSign {
    private static final int MAX_STOCK_LINE_LENGTH = 15;

    public SignTrade() {
        super("Trade");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, ChargeException {
        validateTrade(sign, 1, false, ess);
        validateTrade(sign, 2, true, ess);
        final Trade trade = getTrade(sign, 2, AmountType.ROUNDED, true, true, ess);
        final Trade charge = getTrade(sign, 1, AmountType.ROUNDED, false, true, ess);
        if (trade.getType() == charge.getType() && (trade.getType() != TradeType.ITEM || trade.getItemStack().isSimilar(charge.getItemStack()))) {
            throw new SignException("tradeSignSameType");
        }
        trade.isAffordableFor(player);
        setOwner(ess, player, sign, 3, "§8");
        trade.charge(player);
        Trade.log("Sign", "Trade", "Create", username, trade, username, null, sign.getBlock().getLocation(), player.getMoney(), ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, ChargeException, MaxMoneyException {
        if (isOwner(ess, player, sign, 3, "§8")) {
            final Trade store = rechargeSign(sign, ess, player);
            final Trade stored;
            try {
                stored = getTrade(sign, 1, AmountType.TOTAL, true, true, ess);
                subtractAmount(sign, 1, stored, ess, false);

                final Map<Integer, ItemStack> withdraw = stored.pay(player, OverflowType.RETURN);

                if (withdraw == null) {
                    Trade.log("Sign", "Trade", "Withdraw", username, store, username, null, sign.getBlock().getLocation(), player.getMoney(), ess);
                } else {
                    setAmount(sign, 1, BigDecimal.valueOf(withdraw.get(0).getAmount()), ess, false);
                    Trade.log("Sign", "Trade", "Withdraw", username, stored, username, new Trade(withdraw.get(0), ess), sign.getBlock().getLocation(), player.getMoney(), ess);
                }
            } catch (final SignException e) {
                if (store == null) {
                    throw new SignException(e, "tradeSignEmptyOwner");
                }
            }
            Trade.log("Sign", "Trade", "Deposit", username, store, username, null, sign.getBlock().getLocation(), player.getMoney(), ess);
        } else {
            final Trade charge = getTrade(sign, 1, AmountType.COST, false, true, ess);
            final Trade trade = getTrade(sign, 2, AmountType.COST, true, true, ess);
            charge.isAffordableFor(player);

            // validate addAmount + subtractAmount first to ensure they both do not throw exceptions
            addAmount(sign, 1, charge, ess, true);
            subtractAmount(sign, 2, trade, ess, true);

            addAmount(sign, 1, charge, ess, false);
            subtractAmount(sign, 2, trade, ess, false);
            if (!trade.pay(player)) {
                subtractAmount(sign, 1, charge, ess, false);
                addAmount(sign, 2, trade, ess, false);
                throw new ChargeException("inventoryFull");
            }
            charge.charge(player);
            Trade.log("Sign", "Trade", "Interact", sign.getLine(3).substring(2), charge, username, trade, sign.getBlock().getLocation(), player.getMoney(), ess);
        }
        sign.updateSign();
        return true;
    }

    private Trade rechargeSign(final ISign sign, final IEssentials ess, final User player) throws SignException, ChargeException {
        final Trade trade = getTrade(sign, 2, AmountType.COST, false, true, ess);
        ItemStack stack = Inventories.getItemInHand(player.getBase());
        if (trade.getItemStack() != null && stack != null && !MaterialUtil.isAir(stack.getType()) && trade.getItemStack().getType() == stack.getType() && MaterialUtil.getDamage(trade.getItemStack()) == MaterialUtil.getDamage(stack) && trade.getItemStack().getEnchantments().equals(stack.getEnchantments())) {
            if (MaterialUtil.isPotion(trade.getItemStack().getType()) && !trade.getItemStack().isSimilar(stack)) {
                return null;
            }
            final int amount = trade.getItemStack().getAmount();
            if (Inventories.containsAtLeast(player.getBase(), trade.getItemStack(), amount)) {
                stack = stack.clone();
                stack.setAmount(amount);
                final Trade store = new Trade(stack, ess);
                addAmount(sign, 2, store, ess, false);
                store.charge(player);
                return store;
            }
        }
        return null;
    }

    @Override
    protected boolean onSignBreak(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, MaxMoneyException {
        final String signOwner = sign.getLine(3);

        final boolean isOwner = isOwner(ess, player, sign, 3, "§8");
        final boolean canBreak = isOwner || player.isAuthorized("essentials.signs.trade.override");
        final boolean canCollect = isOwner || player.isAuthorized("essentials.signs.trade.override.collect");

        if (canBreak) {
            try {
                final Trade stored1 = getTrade(sign, 1, AmountType.TOTAL, false, true, ess);
                final Trade stored2 = getTrade(sign, 2, AmountType.TOTAL, false, true, ess);

                if (!canCollect) {
                    Trade.log("Sign", "Trade", "Destroy", signOwner.substring(2), stored2, username, stored1, sign.getBlock().getLocation(), player.getMoney(), ess);
                    return true;
                }

                final Map<Integer, ItemStack> withdraw1 = stored1.pay(player, OverflowType.DROP);
                final Map<Integer, ItemStack> withdraw2 = stored2.pay(player, OverflowType.DROP);

                if (withdraw1 == null && withdraw2 == null) {
                    Trade.log("Sign", "Trade", "Break", signOwner.substring(2), stored2, username, stored1, sign.getBlock().getLocation(), player.getMoney(), ess);
                    return true;
                }

                setAmount(sign, 1, BigDecimal.valueOf(withdraw1 == null ? 0L : withdraw1.get(0).getAmount()), ess, false);
                Trade.log("Sign", "Trade", "Withdraw", signOwner.substring(2), stored1, username, withdraw1 == null ? null : new Trade(withdraw1.get(0), ess), sign.getBlock().getLocation(), player.getMoney(), ess);

                setAmount(sign, 2, BigDecimal.valueOf(withdraw2 == null ? 0L : withdraw2.get(0).getAmount()), ess, false);
                Trade.log("Sign", "Trade", "Withdraw", signOwner.substring(2), stored2, username, withdraw2 == null ? null : new Trade(withdraw2.get(0), ess), sign.getBlock().getLocation(), player.getMoney(), ess);

                sign.updateSign();
            } catch (final SignException e) {
                if (player.isAuthorized("essentials.signs.trade.override")) {
                    return true;
                }
                throw e;
            }
            return false;
        } else {
            return false;
        }
    }

    private void validateSignLength(final String newLine) throws SignException {
        if (newLine.length() > MAX_STOCK_LINE_LENGTH) {
            throw new SignException("tradeSignFull");
        }
    }

    protected final void validateTrade(final ISign sign, final int index, final boolean amountNeeded, final IEssentials ess) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("emptySignLine", index + 1);
        }
        final String[] split = line.split("[ :]+");

        if (split.length == 1 && !amountNeeded) {
            final BigDecimal money = getMoney(split[0], ess);
            if (money != null) {
                final String newLine = NumberUtil.shortCurrency(money, ess) + ":0";
                validateSignLength(newLine);
                sign.setLine(index, newLine);
                return;
            }
        }

        if (split.length == 2 && amountNeeded) {
            final BigDecimal money = getMoney(split[0], ess);
            BigDecimal amount = getBigDecimalPositive(split[1], ess);
            if (money != null && amount != null) {
                amount = amount.subtract(amount.remainder(money));
                if (amount.compareTo(MINTRANSACTION) < 0 || money.compareTo(MINTRANSACTION) < 0) {
                    throw new SignException("moreThanZero");
                }
                final String newLine = NumberUtil.shortCurrency(money, ess) + ":" + NumberUtil.formatAsCurrency(amount);
                validateSignLength(newLine);
                sign.setLine(index, newLine);
                return;
            }
        }

        if (split.length == 2 && !amountNeeded) {
            final int amount = getIntegerPositive(split[0]);

            if (amount < 1) {
                throw new SignException("moreThanZero");
            }
            if (!(split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) && getItemStack(split[1], amount, ess).getType() == Material.AIR) {
                throw new SignException("moreThanZero");
            }
            final String newline = amount + " " + split[1] + ":0";
            validateSignLength(newline);
            sign.setLine(index, newline);
            return;
        }

        if (split.length == 3 && amountNeeded) {
            final int stackamount = getIntegerPositive(split[0]);
            int amount = getIntegerPositive(split[2]);
            amount -= amount % stackamount;
            if (amount < 1 || stackamount < 1) {
                throw new SignException("moreThanZero");
            }
            if (!(split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) && getItemStack(split[1], stackamount, ess).getType() == Material.AIR) {
                throw new SignException("moreThanZero");
            }
            final String newline = stackamount + " " + split[1] + ":" + amount;
            validateSignLength(newline);
            sign.setLine(index, newline);
            return;
        }
        throw new SignException("invalidSignLine", index + 1);
    }

    protected final Trade getTrade(final ISign sign, final int index, final AmountType amountType, final boolean notEmpty, final IEssentials ess) throws SignException {
        return getTrade(sign, index, amountType, notEmpty, false, ess);
    }

    protected final Trade getTrade(final ISign sign, final int index, final AmountType amountType, final boolean notEmpty, final boolean allowId, final IEssentials ess) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("emptySignLine", index + 1);
        }
        final String[] split = line.split("[ :]+");

        if (split.length == 2) {
            try {
                final BigDecimal money = getMoney(split[0], ess);
                final BigDecimal amount = notEmpty ? getBigDecimalPositive(split[1], ess) : getBigDecimal(split[1], ess);
                if (money != null && amount != null) {
                    return new Trade(amountType == AmountType.COST ? money : amount, ess);
                }
            } catch (final SignException e) {
                throw new SignException(e, "tradeSignEmpty");
            }
        }

        if (split.length == 3) {
            final int stackAmount = getIntegerPositive(split[0]);
            if (split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) {
                int amount = getInteger(split[2]);
                if (amountType == AmountType.ROUNDED) {
                    amount -= amount % stackAmount;
                }
                if (notEmpty && (amount < 1 || stackAmount < 1)) {
                    throw new SignException("tradeSignEmpty");
                }
                return new Trade(amountType == AmountType.COST ? stackAmount : amount, ess);
            } else {
                final ItemStack item = getItemStack(split[1], stackAmount, allowId, ess);
                int amount = getInteger(split[2]);
                if (amountType == AmountType.ROUNDED) {
                    amount -= amount % stackAmount;
                }
                if (notEmpty && (amount < 1 || stackAmount < 1 || item.getType() == Material.AIR || amount < stackAmount)) {
                    throw new SignException("tradeSignEmpty");
                }
                item.setAmount(amountType == AmountType.COST ? stackAmount : amount);
                return new Trade(item, ess);
            }
        }
        throw new SignException("invalidSignLine", index + 1);
    }

    protected final void subtractAmount(final ISign sign, final int index, final Trade trade, final IEssentials ess, final boolean validationRun) throws SignException {
        final BigDecimal money = trade.getMoney();
        if (money != null) {
            changeAmount(sign, index, money.negate(), ess, validationRun);
        }
        final ItemStack item = trade.getItemStack();
        if (item != null) {
            changeAmount(sign, index, BigDecimal.valueOf(-item.getAmount()), ess, validationRun);
        }
        final Integer exp = trade.getExperience();
        if (exp != null) {
            changeAmount(sign, index, BigDecimal.valueOf(-exp), ess, validationRun);
        }
    }

    protected final void addAmount(final ISign sign, final int index, final Trade trade, final IEssentials ess, final boolean validationRun) throws SignException {
        final BigDecimal money = trade.getMoney();
        if (money != null) {
            changeAmount(sign, index, money, ess, validationRun);
        }
        final ItemStack item = trade.getItemStack();
        if (item != null) {
            changeAmount(sign, index, BigDecimal.valueOf(item.getAmount()), ess, validationRun);
        }
        final Integer exp = trade.getExperience();
        if (exp != null) {
            changeAmount(sign, index, BigDecimal.valueOf(exp), ess, validationRun);
        }
    }

    private void changeAmount(final ISign sign, final int index, final BigDecimal value, final IEssentials ess, final boolean validationRun) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("emptySignLine", index + 1);
        }
        final String[] split = line.split("[ :]+");

        if (split.length == 2) {
            final BigDecimal amount = getBigDecimal(split[1], ess).add(value);
            setAmount(sign, index, amount, ess, validationRun);
            return;
        }
        if (split.length == 3) {
            final BigDecimal amount = getBigDecimal(split[2], ess).add(value);
            setAmount(sign, index, amount, ess, validationRun);
            return;
        }
        throw new SignException("invalidSignLine", index + 1);
    }

    private void setAmount(final ISign sign, final int index, final BigDecimal value, final IEssentials ess, final boolean validationRun) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("emptySignLine", index + 1);
        }
        final String[] split = line.split("[ :]+");

        if (split.length == 2) {
            final BigDecimal money = getMoney(split[0], ess);
            final BigDecimal amount = getBigDecimal(split[1], ess);
            if (money != null && amount != null) {
                final String newline = NumberUtil.shortCurrency(money, ess) + ":" + NumberUtil.formatAsCurrency(value);
                validateSignLength(newline);
                if (!validationRun) {
                    sign.setLine(index, newline);
                }
                return;
            }
        }

        if (split.length == 3) {
            final int stackAmount = getIntegerPositive(split[0]);
            if (split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) {
                final String newline = stackAmount + " " + split[1] + ":" + value.intValueExact();
                validateSignLength(newline);
                if (!validationRun) {
                    sign.setLine(index, newline);
                }
            } else {
                getItemStack(split[1], stackAmount, ess);
                final String newline = stackAmount + " " + split[1] + ":" + value.intValueExact();
                validateSignLength(newline);
                if (!validationRun) {
                    sign.setLine(index, newline);
                }
            }
            return;
        }
        throw new SignException("invalidSignLine", index + 1);
    }

    public enum AmountType {
        TOTAL,
        ROUNDED,
        COST
    }
}
