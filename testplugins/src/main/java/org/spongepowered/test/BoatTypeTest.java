package org.spongepowered.test;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.TreeType;
import org.spongepowered.api.data.type.TreeTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

@Plugin(id = "boat_type_test", name = "Boat Type Test", description = "Right click a boat to get the TreeType, run /makeboat <treetype> to make a boat.")
public class BoatTypeTest {

    @Listener
    public void onInit(GameInitializationEvent event) {
        Sponge.getCommandManager().register(this,
                CommandSpec.builder()
                        .description(Text.of("Gives you a boat of a specific TreeType"))
                        .arguments(GenericArguments.catalogedElement(Text.of("tree"), TreeType.class))
                        .executor((src, args) -> {
                            if (!(src instanceof Player)) {
                                src.sendMessage(Text.of("Only players can run this command"));
                                return CommandResult.empty();
                            }
                            Player player = (Player) src;
                            Boat boat = (Boat) player.getLocation().getExtent().createEntity(EntityTypes.BOAT, player.getLocation().getPosition());
                            boat.offer(Keys.TREE_TYPE, args.<TreeType>getOne("tree").orElse(TreeTypes.OAK));
                            return CommandResult.success();
                        })
                        .build(),
                "makeboat");
    }

    @Listener
    public void onInteractEntity(InteractEntityEvent.Secondary.MainHand event, @Getter("getTargetEntity") Boat boat, @First Player player) {
        player.sendMessage(Text.of("This boat is of type: " + boat.get(Keys.TREE_TYPE).orElse(TreeTypes.OAK).getName()));
    }
}
