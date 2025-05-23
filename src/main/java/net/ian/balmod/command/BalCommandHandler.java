package net.ian.balmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.*;

public class BalCommandHandler {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("bal")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    Scoreboard scoreboard = source.getServer().getScoreboard();
                    ScoreboardObjective objective = scoreboard.getNullableObjective("Bank");

                    if (objective == null) {
                        source.sendFeedback(() -> Text.literal("§cBank objective does not exist."), false);
                        return 0;
                    }

                    int balance = scoreboard.getOrCreateScore(player, objective).getScore();
                    source.sendFeedback(() -> Text.literal("§6You have §a$%,d".formatted(balance)), false);
                    return 1;
                })
                .then(literal("add")
                        .requires(source -> source.hasPermissionLevel(4)) // Admin only
                        .then(argument("target", EntityArgumentType.player())
                                .then(argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");

                                            Scoreboard scoreboard = source.getServer().getScoreboard();
                                            ScoreboardObjective objective = scoreboard.getNullableObjective("Bank");

                                            if (objective == null) {
                                                source.sendFeedback(() -> Text.literal("§cBank objective does not exist."), false);
                                                return 0;
                                            }

                                            int oldBalance = scoreboard.getOrCreateScore(target, objective).getScore();
                                            scoreboard.getOrCreateScore(target, objective).setScore(oldBalance + amount);

                                            source.sendFeedback(() -> Text.literal("§aAdded §a$%,d §ato §b%s§a's account.".formatted(amount, target.getName().getString())), true);
                                            target.sendMessage(Text.literal("§aAn admin added §a$%,d §ato your bank account.".formatted(amount)));
                                            return 1;
                                        }))))
                .then(literal("deduct")
                        .requires(source -> source.hasPermissionLevel(4)) // Admin only
                        .then(argument("target", EntityArgumentType.player())
                                .then(argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");

                                            Scoreboard scoreboard = source.getServer().getScoreboard();
                                            ScoreboardObjective objective = scoreboard.getNullableObjective("Bank");

                                            if (objective == null) {
                                                source.sendFeedback(() -> Text.literal("§cBank objective does not exist."), false);
                                                return 0;
                                            }

                                            int oldBalance = scoreboard.getOrCreateScore(target, objective).getScore();
                                            scoreboard.getOrCreateScore(target, objective).setScore(oldBalance - amount);

                                            source.sendFeedback(() -> Text.literal("§aDeducted §a$%,d §ain §b%s§a's account.".formatted(amount, target.getName().getString())), true);
                                            target.sendMessage(Text.literal("§aAn admin deducted §a$%,d §ain your bank account.".formatted(amount)));
                                            return 1;
                                        }))))

                // /bal pay <target> <amount>
                .then(literal("pay")
                        .then(argument("target", EntityArgumentType.player())
                                .then(argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ServerPlayerEntity sender = source.getPlayer();
                                            ServerPlayerEntity recipient = EntityArgumentType.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");

                                            Scoreboard scoreboard = source.getServer().getScoreboard();
                                            ScoreboardObjective objective = scoreboard.getNullableObjective("Bank");

                                            if (objective == null) {
                                                source.sendFeedback(() -> Text.literal("§cBank objective does not exist."), false);
                                                return 0;
                                            }

                                            int senderBalance = scoreboard.getOrCreateScore(sender, objective).getScore();

                                            if (senderBalance < amount) {
                                                source.sendFeedback(() -> Text.literal("§cYou don't have enough funds!"), false);
                                                return 0;
                                            }

                                            if (sender.equals(recipient)) {
                                                source.sendFeedback(() -> Text.literal("§cYou can't pay yourself."), false);
                                                return 0;
                                            }

                                            // Process transaction
                                            scoreboard.getOrCreateScore(sender, objective).setScore(senderBalance - amount);
                                            int recipientBalance = scoreboard.getOrCreateScore(recipient, objective).getScore();
                                            scoreboard.getOrCreateScore(recipient, objective).setScore(recipientBalance + amount);

                                            // Feedback
                                            sender.sendMessage(Text.literal("§6You paid §a$%,d §6to §b%s".formatted(amount, recipient.getName().getString())));
                                            recipient.sendMessage(Text.literal("§aYou received §a$%,d §afrom §b%s".formatted(amount, sender.getName().getString())));
                                            return 1;
                                        }))))

                // /bal set <target> <amount>
                .then(literal("set")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(argument("target", EntityArgumentType.player())
                                .then(argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");

                                            Scoreboard scoreboard = source.getServer().getScoreboard();
                                            ScoreboardObjective objective = scoreboard.getNullableObjective("Bank");

                                            if (objective == null) {
                                                source.sendFeedback(() -> Text.literal("§cBank objective does not exist."), false);
                                                return 0;
                                            }

                                            scoreboard.getOrCreateScore(target, objective).setScore(amount);
                                            source.sendFeedback(() -> Text.literal("§eSet balance of §b%s §eto §a$%,d".formatted(target.getName().getString(), amount)), true);
                                            return 1;
                                        }))))

                // /bal top
                .then(literal("list")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            Scoreboard scoreboard = source.getServer().getScoreboard();
                            ScoreboardObjective objective = scoreboard.getNullableObjective("Bank");

                            if (objective == null) {
                                source.sendFeedback(() -> Text.literal("§cBank objective does not exist."), false);
                                return 0;
                            }

                            List<ServerPlayerEntity> players = source.getServer().getPlayerManager().getPlayerList();

                            List<Object[]> top = players.stream()
                                    .map(player -> new Object[]{player, scoreboard.getOrCreateScore(player, objective).getScore()})
                                    .sorted((a, b) -> Integer.compare((Integer)b[1], (Integer)a[1]))
                                    .limit(10)
                                    .collect(Collectors.toList());

                            source.sendFeedback(() -> Text.literal("§6Top Balances:"), false);

                            for (int i = 0; i < top.size(); i++) {
                                var entry = top.get(i);
                                ServerPlayerEntity player = (ServerPlayerEntity) entry[0];
                                int balance = (Integer) entry[1];

                                final int index = i + 1;
                                final String playerName = player.getName().getString();
                                final int finalBalance = balance;

                                source.sendFeedback(() -> Text.literal("§7%d. §b%s: §a$%,d".formatted(index, playerName, finalBalance)), false);
                            }

                            return 1;
                        }))
        );
    }
}
