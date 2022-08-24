package me.maartin0.interactions;

import me.maartin0.Rift;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ManagementCommandsListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getCommandPath()) {
            case "create" -> {
                Rift rift = new Rift();
            } case "join" -> {

            } case "leave" -> {

            } case "modify/global/name" -> {

            } case "modify/global/description" -> {

            } case "modify/prefix" -> {

            } case "modify/description" -> {

            } case "modify/invite" -> {

            }
        }
    }
}
