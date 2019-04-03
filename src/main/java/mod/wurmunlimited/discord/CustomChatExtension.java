package mod.wurmunlimited.discord;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.wurmonline.server.Message;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.players.Player;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.nyxcode.wurm.discordrelay.DiscordRelay;

import java.util.*;

public class CustomChatExtension extends ListenerAdapter implements WurmServerMod, Configurable, PlayerMessageListener {
    private final BiMap<String, String> names = HashBiMap.create();

    @Override
    public void configure(Properties properties) {
        String channelNames = properties.getProperty("channel_names");
        for (String name : channelNames.split(","))
            names.put(name, discordifyName(name));
    }

    private void sendToCustomChat(String channel, final String message){
        final Message mess = new Message(null, Message.SAY, channel, message);
        if (message.trim().length() > 1) {
            Server.getInstance().addMessage(mess);
        }
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        // Skip commands
        if (message.startsWith("!") || message.startsWith("/") || message.startsWith("#")){
            return MessagePolicy.PASS;
        }

        if (names.containsKey(title)) {
            Player player = communicator.getPlayer();
            DiscordRelay.sendToDiscord(title, "<" + (player != null ? player.getName() : "???") + "> " + message, false);
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
            String wurmChannelName = names.inverse().get(event.getTextChannel().getName());

            if (wurmChannelName != null) {
                sendToCustomChat(wurmChannelName, "<@" + event.getMember().getEffectiveName() + "> " + event.getMessage().getContent());
            }
        }
    }

    private String discordifyName(String name) {
        return name.toLowerCase().replace(" ", "");
    }
}
