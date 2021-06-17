package net.heipiao.authforge;

import net.heipiao.authforge.events.PlayerLoginFailedEvent;
import net.heipiao.authforge.events.PlayerLoginSuccessEvent;
import net.heipiao.authforge.events.PlayerSignUpEvent;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.EventBusErrorMessage;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.fml.common.Mod;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
@Mod("authforge")
public class AuthForge
{
    // private static final Logger LOGGER = LogManager.getLogger();
    public Connection conn;
    public List<PlayerEntity> playerNotLoggedIn = new ArrayList<>();
    private double spawnX, spawnY, spawnZ;
    public static final IEventBus AUTHFORGE_BUS = BusBuilder.builder().setExceptionHandler(AuthForge::handleException).build();
    public static AuthForge AfInstance;
    private static final Logger LOGGER = LogManager.getLogger();

    public AuthForge() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:users.db");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        MinecraftForge.EVENT_BUS.addListener(this::playerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::playerLeave);
        MinecraftForge.EVENT_BUS.addListener(this::regCmd);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::getSpawn);
        MinecraftForge.EVENT_BUS.addListener(this::breakBlock);
        MinecraftForge.EVENT_BUS.addListener(this::placeBlock);
        MinecraftForge.EVENT_BUS.addListener(this::pickupItem);
        MinecraftForge.EVENT_BUS.addListener(this::useItem);
        MinecraftForge.EVENT_BUS.addListener(this::useHoe);
        MinecraftForge.EVENT_BUS.addListener(this::breakFarmland);
        MinecraftForge.EVENT_BUS.addListener(this::interact);
        AfInstance=this;
    }

    private static void handleException(IEventBus bus, Event event, IEventListener[] listeners, int index, Throwable throwable){
        LOGGER.error(new EventBusErrorMessage(event, index, listeners, throwable));
    }

    private void getSpawn(WorldEvent.Load event){
        spawnX = event.getWorld().getLevelData().getXSpawn();
        spawnY = event.getWorld().getLevelData().getYSpawn();
        spawnZ = event.getWorld().getLevelData().getZSpawn();
    }

    private void playerJoin(final PlayerLoggedInEvent event)
    {
        // event.getPlayer().sendMessage(new TranslationTextComponent("authforge.text.pleaselogin"), UUID.randomUUID());
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS USERS("+
                                // "ID INT PRIMARY KEY NOT NULL,"+
                                "NAME TEXT NOT NULL,"+
                                "SALT BOOL NOT NULL,"+
                                "PASSWORD TEXT NOT NULL);"
            );
            String hasUserSql = "SELECT * FROM USERS WHERE NAME=?;";
            PreparedStatement hasUserStmt = conn.prepareStatement(hasUserSql);
            hasUserStmt.setString(1, event.getPlayer().getName().getString());
            ResultSet hasUserRS = hasUserStmt.executeQuery();
            if(hasUserRS.next()){
                event.getPlayer().sendMessage(new StringTextComponent("请登录"), UUID.randomUUID());
            }else{
                event.getPlayer().sendMessage(new StringTextComponent("请注册（不要用大网站密码，因为传输过程有风险）"), UUID.randomUUID());
            }
            playerNotLoggedIn.add(event.getPlayer());
            hasUserStmt.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void playerLeave(final PlayerLoggedOutEvent event){
        playerNotLoggedIn.remove(event.getPlayer());
    }
    private void regCmd(RegisterCommandsEvent event){
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("reg")
                            .then(Commands.argument("password", StringArgumentType.word())
                            .executes((CommandContext<CommandSource> context)->{
                                if(!playerNotLoggedIn.contains(context.getSource().getPlayerOrException())){
                                    throw new CommandException(new StringTextComponent("你已登录"));
                                }
                                try {
                                    PreparedStatement checkStmt = conn.prepareStatement("select * from users where name=?");
                                    checkStmt.setString(1, context.getSource().getPlayerOrException().getName().getString());
                                    if(!checkStmt.executeQuery().next()){
                                        PreparedStatement stmt = conn.prepareStatement("INSERT INTO USERS (NAME,PASSWORD) VALUES (?,?);");
                                        stmt.setString(1, context.getSource().getPlayerOrException().getName().getString());
                                        String salt = BCrypt.gensalt();
                                        stmt.setString(2, BCrypt.hashpw(StringArgumentType.getString(context, "password"), salt));
                                        stmt.executeUpdate();
                                        stmt.close();
                                        context.getSource().getPlayerOrException().sendMessage(new StringTextComponent("注册成功"), UUID.randomUUID());
                                        AUTHFORGE_BUS.post(new PlayerSignUpEvent(context.getSource().getPlayerOrException(), this));
                                        playerNotLoggedIn.remove(context.getSource().getPlayerOrException());
                                    }else{
                                        throw new CommandException(new StringTextComponent("你已注册"));
                                    }
                                    checkStmt.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                                return 0;
                            })));
        dispatcher.register(Commands.literal("login")
                            .then(Commands.argument("password", StringArgumentType.word())
                            .executes((CommandContext<CommandSource> context)->{
                                if(!playerNotLoggedIn.contains(context.getSource().getPlayerOrException())){
                                    throw new CommandException(new StringTextComponent("你已登录"));
                                }
                                try {
                                    PreparedStatement stmt = conn.prepareStatement("SELECT PASSWORD FROM USERS WHERE NAME=?;");
                                    stmt.setString(1, context.getSource().getPlayerOrException().getName().getString());
                                    ResultSet rs=stmt.executeQuery();
                                    String password=rs.getString("PASSWORD");
                                    if(!BCrypt.checkpw(StringArgumentType.getString(context, "password"), password)){
                                        AUTHFORGE_BUS.post(new PlayerLoginFailedEvent(context.getSource().getPlayerOrException(), this));
                                        throw new CommandException(new StringTextComponent("登录错误"));
                                    }else{
                                        context.getSource().getPlayerOrException().sendMessage(new StringTextComponent("登录成功"), UUID.randomUUID());
                                        AUTHFORGE_BUS.post(new PlayerLoginSuccessEvent(context.getSource().getPlayerOrException(), this));
                                        playerNotLoggedIn.remove(context.getSource().getPlayerOrException());
                                    }
                                    stmt.close();
                                    return 0;
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                                return 0;
                            })));
    }
    private void tick(TickEvent.PlayerTickEvent event){
        try{
            if(event.side.isServer()&&playerNotLoggedIn.contains(event.player))
                event.player.teleportTo(spawnX, spawnY, spawnZ);
        }catch(Throwable error){
            error.printStackTrace();
        }
    }
    private void breakBlock(BlockEvent.BreakEvent event){
        if(playerNotLoggedIn.contains(event.getPlayer())){
            event.setCanceled(true);
        }
    }
    private void placeBlock(BlockEvent.EntityPlaceEvent event){
        if(playerNotLoggedIn.contains(event.getEntity())){
            event.setCanceled(true);
        }
    }
    private void pickupItem(ItemPickupEvent event){
        if(playerNotLoggedIn.contains(event.getPlayer())){
            event.setCanceled(true);
        }
    }
    private void useItem(LivingEntityUseItemEvent event){
        if(playerNotLoggedIn.contains(event.getEntity())){
            event.setCanceled(true);
        }
    }
    private void useHoe(UseHoeEvent event){
        if(playerNotLoggedIn.contains(event.getPlayer())){
            event.setCanceled(true);
        }
    }
    private void breakFarmland(BlockEvent.FarmlandTrampleEvent event){
        if(playerNotLoggedIn.contains(event.getEntity())){
            event.setCanceled(true);
        }
    }
    private void interact(PlayerInteractEvent event){
        if(playerNotLoggedIn.contains(event.getEntity())){
            event.setCanceled(true);
        }
    }
}
