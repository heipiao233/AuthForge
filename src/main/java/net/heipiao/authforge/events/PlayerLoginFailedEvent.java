package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerLoginFailedEvent extends AfPlayerEvent{
    public PlayerLoginFailedEvent(PlayerEntity player, AuthForge af) {
        super(player, af);
    }
}
