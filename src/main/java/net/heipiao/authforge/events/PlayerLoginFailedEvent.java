package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.world.entity.player.Player;

/* 在玩家登录失败时在AuthForge.AUTHFORGE_BUS发出 */
public class PlayerLoginFailedEvent extends AfPlayerEvent{
    public PlayerLoginFailedEvent(Player player, AuthForge af) {
        super(player, af);
    }
}
