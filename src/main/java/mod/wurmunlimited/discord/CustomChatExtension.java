package mod.wurmunlimited.discord;

import com.wurmonline.server.Message;
import com.wurmonline.server.Servers;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.players.Player;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.spirangle.wurm.ChatHandler;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.nyxcode.wurm.discordrelay.DiscordRelay;

import java.util.HashMap;
import java.util.Map;

public class CustomChatExtension extends ListenerAdapter implements WurmServerMod, Initable, PlayerMessageListener {
    private final Map<String, String> names = new HashMap<>();

    public CustomChatExtension() {
        names.put(discordifyName(ChatHandler.TRADE), ChatHandler.TRADE);
        names.put(discordifyName(ChatHandler.WORLD), ChatHandler.WORLD);
    }

    @Override
    public void init() {
        try {
            JDA jda = ReflectionUtil.getPrivateField(null, DiscordRelay.class.getDeclaredField("jda"));
            jda.addEventListener(this);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendToCustomChat(String channel, final String message){
        if (message.trim().length() > 1) {
            if (channel.equals(ChatHandler.WORLD))
                ChatHandler.getInstance().sendWorldMessage(null, Message.KINGDOM, channel, message, -1, -1, -1);
            else if (channel.equals(ChatHandler.TRADE))
                ChatHandler.getInstance().sendTradeMessage(null, Message.TRADE, channel, message, -1, -1, -1);
        }
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        Player player = communicator.getPlayer();
        if (!player.isMute()) {
            // Skip commands
            if (message.startsWith("!") || message.startsWith("/") || message.startsWith("#")){
                return MessagePolicy.PASS;
            }

            if (player.isDead() || (Servers.localServer.entryServer && !player.isReallyPaying() && !player.mayMute()) ||
                        communicator.isInvulnerable() || (title.equals(ChatHandler.TRADE) && !player.isTradeChannel())) {
                return MessagePolicy.PASS;
            } else if (title.equals(ChatHandler.TRADE) || title.equals(ChatHandler.WORLD)) {
                DiscordRelay.sendToDiscord(title, "<" + player.getName() + "> " + message, false);
            }
        }

        return MessagePolicy.PASS;
    }

    @Override
    public boolean onPlayerMessage(Communicator communicator, String s) {
        return false;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);
        if (event.isFromType(ChannelType.TEXT) && !event.getAuthor().isBot()) {
            String wurmChannelName = names.get(event.getTextChannel().getName());

            if (wurmChannelName != null) {
                sendToCustomChat(wurmChannelName, "<@" + event.getMember().getEffectiveName() + "> " + event.getMessage().getContent());
            }
        }
    }

    private String discordifyName(String name) {
        return name.toLowerCase().replace(" ", "");
    }
}
