package net.heipiao.authforge.events;

import net.heipiao.authforge.AuthForge;
import net.minecraft.world.entity.player.Player;

/* 在玩家注册成功时在AuthForge.AUTHFORGE_BUS发出 */
public class PlayerSignUpEvent extends AfPlayerEvent{
    public PlayerSignUpEvent(Player player, AuthForge af) {
        super(player, af);
    }
}
