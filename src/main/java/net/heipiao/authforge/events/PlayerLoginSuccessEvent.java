package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.entity.player.PlayerEntity;

/* 在玩家登录成功时在AuthForge.AUTHFORGE_BUS发出 */
public class PlayerLoginSuccessEvent extends AfPlayerEvent{
    public PlayerLoginSuccessEvent(PlayerEntity player, AuthForge af) {
        super(player, af);
    }
}
