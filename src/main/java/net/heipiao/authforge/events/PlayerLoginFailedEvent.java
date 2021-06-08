package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.entity.player.PlayerEntity;
/* 在玩家登录失败时在AuthForge.AUTHFORGE_BUS发出 */
public class PlayerLoginFailedEvent extends AfPlayerEvent{
    public PlayerLoginFailedEvent(PlayerEntity player, AuthForge af) {
        super(player, af);
    }
}
