package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerLoginSuccessEvent extends AfPlayerEvent{
    public PlayerLoginSuccessEvent(PlayerEntity player, AuthForge af) {
        super(player, af);
    }
}
