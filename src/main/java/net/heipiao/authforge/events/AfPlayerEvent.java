package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

public abstract class AfPlayerEvent extends PlayerEvent{
    private final AuthForge afInst;
    public AfPlayerEvent(Player player, AuthForge af){
        super(player);
        afInst=af;
    }
    public AuthForge getAfInst() {
        return afInst;
    }
}
