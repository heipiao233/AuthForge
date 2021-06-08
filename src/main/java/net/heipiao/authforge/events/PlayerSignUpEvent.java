package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerSignUpEvent extends AfPlayerEvent{
    public PlayerSignUpEvent(PlayerEntity player, AuthForge af) {
        super(player, af);
    }
}
