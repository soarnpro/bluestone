package com.kdrag0n.bluestone.modules;

import com.j256.ormlite.dao.Dao;
import com.kdrag0n.bluestone.*;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.sql.GuildPrefix;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.types.Perm;
import com.kdrag0n.bluestone.util.Paginator;
import com.kdrag0n.bluestone.util.Strings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static com.kdrag0n.bluestone.util.Strings.str;

public class CoreModule extends Module {
    static final Collection<Permission> PERMS_NEEDED = Permission.getPermissions(473295957L);

    private static final String LIST_ITEM = Strings.EMPTY + ' ' + Strings.EMPTY + ' ' + Strings.EMPTY + ' ' + Strings.EMPTY + " \u2022 ";
    private static final String INFO_LINKS = LIST_ITEM + "Use my [invite link]([invite]) to take me to another server\n" +
            LIST_ITEM + "[Donate](https://paypal.me/dragon5232) to help keep me alive\n" +
            LIST_ITEM + "Go to [my website](https://khronodragon.com/goldmine/) for help\n" +
            LIST_ITEM + "Join my [support server](https://discord.gg/sYkwfxA) for even more help";

    private static final EnumSet<Perm> PREFIX_PERMS = EnumSet.of(
            Perm.MANAGE_SERVER,
            Perm.MANAGE_CHANNEL,
            Perm.MESSAGE_MANAGE
    );

    private final Dao<GuildPrefix, Long> prefixDao = setupDao(GuildPrefix.class);

    public CoreModule(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Core";
    }

    @Command(name = "test", desc = "Make sure I work.")
    public void cmdTest(Context ctx) {
        ctx.message.addReaction("\uD83D\uDC4D").queue();
    }

    @Command(name = "ping", desc = "Pong!")
    public void cmdPing(Context ctx) {
        String msg = "🏓 WebSockets: " + (int) Math.round(ctx.bot.manager.getAverageGatewayPing()) + "ms";
        long beforeTime = System.currentTimeMillis();

        ctx.send(msg).queue(message1 -> message1.editMessage(msg + ", message: calculating...")
                .queue(message2 -> {
                    double msgPing = (System.currentTimeMillis() - beforeTime) / 2.0;
                    message2.editMessage(msg + ", message: " + (int) Math.round(msgPing) + "ms").queue();
                })
        );
    }

    @Command(name = "faq", desc = "Get the Frequently Asked Questions list.")
    public void cmdFaq(Context ctx) {
        ctx.send("You can find the FAQ at <https://khronodragon.com/goldmine/faq>.").queue();
    }

    @Command(name = "owner", desc = "Become the bot owner..?", aliases = {"bot_owner"})
    public void cmdOwnerInfo(Context ctx) {
        ctx.send("My owner is **" + bot.ownerTag +
                "**. The bot owner is a role that applies globally to the entire bot, and is the person who owns the actual bot, **not** the owner of a server or anything like that.\n" +
        "**No**, you may not have bot owner, because it allows full bot control. In your server, being **server owner** is sufficient, and grants you permission to perform all the actions you will need to, automatically.\n\n" +
        "**__TL;DR Server owner is enough, you can't have bot owner because that's one person (the actual owner of the bot) and offers unlimited control.__**").queue();
    }

    private void addHelpEntry(com.kdrag0n.bluestone.Command cmd, Map<String, List<String>> fields) {
        String cName = cmd.module.getDisplayName();
        String entry = "\u2022 **" + cmd.name + "**: " + cmd.description;

        if (fields.containsKey(cName)) {
            fields.get(cName).add(entry);
        } else {
            final LinkedList<String> newList = new LinkedList<>();
            newList.add(entry);

            fields.put(cName, newList);
        }
    }

    @Command(name = "help", desc = "List the available commands and their usage.", usage = "{commands and/or modules}",
            aliases = {"halp", "commands"})
    public void cmdHelp(Context ctx) {
        int charLimit = ctx.jda.getSelfUser().isBot() ? MessageEmbed.EMBED_MAX_LENGTH_BOT : MessageEmbed.EMBED_MAX_LENGTH_CLIENT;
        boolean isOwner = ctx.author.getIdLong() == bot.ownerId;

        List<MessageEmbed> pages = new ArrayList<>();
        Map<String, List<String>> fields = new HashMap<>();

        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor("Bot Help", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setColor(randomColor());

        if (ctx.args.length < 1) {
            for (com.kdrag0n.bluestone.Command cmd: new HashSet<>(bot.commands.values())) {
                if ((!cmd.hidden || isOwner) && !(cmd.requiresOwner && !isOwner))
                    addHelpEntry(cmd, fields);
            }
        } else {
            for (int i = 0; i < ctx.args.length && i < 24; i++) {
                String item = ctx.args.get(i);
                String litem = item.toLowerCase();
                boolean done = false;

                if (bot.modules.containsKey(item)) {
                    Module module = bot.modules.get(item);

                    for (com.kdrag0n.bluestone.Command cmd: new HashSet<>(bot.commands.values())) {
                        if (cmd.module == module && (!cmd.hidden || isOwner) && !(cmd.requiresOwner && !isOwner))
                            addHelpEntry(cmd, fields);
                    }
                    done = true;
                }

                if (bot.commands.containsKey(litem)) {
                    com.kdrag0n.bluestone.Command cmd = bot.commands.get(litem);
                    StringBuilder field = new StringBuilder("`");

                    if (cmd.aliases.length < 1) {
                        field.append(ctx.prefix)
                                .append(cmd.name);
                    } else {
                        field.append(ctx.prefix)
                                .append(cmd.name)
                                .append('/')
                                .append(String.join("/", cmd.aliases));
                    }

                    field.append(' ')
                            .append(cmd.usage)
                            .append("`\n\n")
                            .append(cmd.description);
                    fields.put(litem, Collections.singletonList(field.toString()));
                    done = true;
                }

                if (!done) {
                    fields.put(item, Collections.singletonList("Not found."));
                }
            }
        }

        int chars = embedAuthorChars(ctx);
        for (String module: fields.keySet()) {
            List<String> field = fields.get(module);
            String content = String.join("\n", field);
            if (content.length() < 1) {
                content = "No visible commands.";
            }

            int preLen = content.length() + module.length();
            if (chars + preLen > charLimit) {
                pages.add(emb.build());
                emb = newEmbedWithAuthor(ctx)
                        .setColor(randomColor());
                chars = embedAuthorChars(ctx);
            }

            if (content.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
                emb.addField(module, content, false);
            } else {
                Paginator pager = new Paginator(1024);
                for (String s: field) pager.addLine(s);

                for (String page: pager.getPages()) {
                    emb.addField(module, page, true);
                }
            }
            chars += preLen;
        }
        pages.add(emb.build());

        for (MessageEmbed page: pages) {
            ctx.author.openPrivateChannel().complete().sendMessage(page).queue(null, exp -> {
                if (exp instanceof ErrorResponseException) {
                    if (((ErrorResponseException) exp).getErrorCode() != 50007) {
                        RestAction.getDefaultFailure().accept(exp);
                    } else {
                        try {
                            ctx.send(Emotes.getFailure() +
                                    " I couldn't send you help! Make sure you haven't blocked me, and have direct messages from this server turned on.").queue();
                        } catch (InsufficientPermissionException ignored) {}
                    }
                }
            });
        }

        if (ctx.guild != null) {
            try {
                ctx.message.addReaction("✅").queue(null, e -> {});
            } catch (PermissionException ignored) {
                ctx.success("Check your DMs!");
            }
        }
    }

    private int embedAuthorChars(Context ctx) {
        if (ctx.guild != null) {
            return ctx.guild.getSelfMember().getEffectiveName().length();
        } else {
            return ctx.jda.getSelfUser().getName().length();
        }
    }

    @Command(name = "uptime", desc = "See how long I've been running.", aliases = {"memory", "ram"})
    public void cmdUptime(Context ctx) {
        ctx.send("I've been up for **" + bot.formatUptime() +
                "**, and am using **" + Strings.formatMemory() + "** of memory.").queue();
    }

    @Command(name = "info", desc = "Get some info about me.", aliases = {"about", "stats", "statistics", "status"})
    public void cmdInfo(Context ctx) {
        EmbedBuilder emb = newEmbedWithAuthor(ctx, "https://khronodragon.com/goldmine")
                .setColor(randomColor())
                .setDescription("A bot by **" + bot.ownerTag + "** made with ❤")
                .addField("Servers", str(bot.getGuildCount()), true)
                .addField("Uptime", bot.formatUptime(), true)
                .addField("Threads", str(Thread.activeCount()), true)
                .addField("Memory Used", Strings.formatMemory(), true)
                .addField("Users", str(bot.getUserCount()), true)
                .addField("Channels", str(bot.getTotalChannelCount()), true)
                .addField("Revision", BuildConfig.GIT_SHORT_COMMIT, true)
                .addField("Music Tracks Loaded", str(bot.getTrackCount()), true)
                .addField("Playing Music in", bot.getStreamCount() + " channels", true)
                .addField("Links", StringUtils.replace(INFO_LINKS, "[invite]", ctx.jda.getInviteUrl(PERMS_NEEDED)), false)
                .setFooter("Serving you from shard " + ctx.jda.getShardInfo().getShardId() + 1, null)
                .setTimestamp(Instant.now());

        ctx.send(emb.build()).queue();
    }

    @Command(name = "prefix", desc = "Get or set the command prefix.", aliases = {"setprefix", "pset"}, guildOnly = true)
    public void cmdPrefix(Context ctx) throws SQLException {
        if (!ctx.args.empty) {
            Perm.checkThrow(ctx, PREFIX_PERMS);

            if (ctx.rawArgs.length() > 32) {
                ctx.fail("Prefix too long!");
            } else {
                String rawPrefix = ctx.rawArgs;
                if (rawPrefix.equals(ctx.guild.getSelfMember().getAsMention())) {
                    rawPrefix += ' ';
                }

                GuildPrefix prefix = new GuildPrefix(ctx.guild.getIdLong(), rawPrefix);
                prefixDao.createOrUpdate(prefix);
                bot.prefixStore.cache.put(ctx.guild.getIdLong(), rawPrefix);

                ctx.success("Prefix set.");
            }
        } else {
            if (ctx.prefix.equals(ctx.guild.getSelfMember().getAsMention() + " ")) {
                ctx.send("My prefix is: " + ctx.prefix + "").queue();
            } else {
                ctx.send("My prefix is: `" + ctx.prefix + "`").queue();
            }
        }
    }
}