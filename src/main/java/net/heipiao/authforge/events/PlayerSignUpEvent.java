package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.entity.player.PlayerEntity;

/* 在玩家注册成功时在AuthForge.AUTHFORGE_BUS发出 */
public class PlayerSignUpEvent extends AfPlayerEvent{
    public PlayerSignUpEvent(PlayerEntity player, AuthForge af) {
        super(player, af);
    }
}
