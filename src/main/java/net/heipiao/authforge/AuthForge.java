package net.heipiao.authforge;

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.world.WorldEvent;
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

import org.mindrot.jbcrypt.BCrypt;

@Mod("authforge")
public class AuthForge
{
    // private static final Logger LOGGER = LogManager.getLogger();
    public Connection conn;
    public List<PlayerEntity> playerNotLoggedIn = new ArrayList<>();
    private double spawnX, spawnY, spawnZ;
    public static AuthForge instance;

    public AuthForge() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:users.db");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        MinecraftForge.EVENT_BUS.addListener(this::playerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::regCmd);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::getSpawn);
        instance = this;
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
                                // "SALT TEXT NOT NULL,"+
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
    private void regCmd(RegisterCommandsEvent event){
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("reg")
                            .then(Commands.argument("password", StringArgumentType.word())
                            .executes((CommandContext<CommandSource> context)->{
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
                                        playerNotLoggedIn.remove(context.getSource().getPlayerOrException());
                                    }else{
                                        throw new CommandException(new StringTextComponent("请登录"));
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
                                try {
                                    PreparedStatement stmt = conn.prepareStatement("SELECT PASSWORD FROM USERS WHERE NAME=?;");
                                    stmt.setString(1, context.getSource().getPlayerOrException().getName().getString());
                                    ResultSet rs=stmt.executeQuery();
                                    String password=rs.getString("PASSWORD");
                                    if(!BCrypt.checkpw(StringArgumentType.getString(context, "password"), password)){
                                        throw new CommandException(new StringTextComponent("登录错误"));
                                    }else{
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
}
