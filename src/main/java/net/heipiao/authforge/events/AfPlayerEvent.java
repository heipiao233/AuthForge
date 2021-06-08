package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class AfPlayerEvent extends PlayerEvent{
    private AuthForge afInst;
    public AfPlayerEvent(PlayerEntity player, AuthForge af){
        super(player);
        afInst=af;
    }
    public AuthForge getAfInst() {
        return afInst;
    }
}
