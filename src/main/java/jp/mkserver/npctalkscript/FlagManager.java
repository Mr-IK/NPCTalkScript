package jp.mkserver.npctalkscript;

import jp.mkserver.npctalkscript.api.MySQLManagerV2;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FlagManager {

    NPCTalkScript plugin;

    public FlagManager(NPCTalkScript  plugin){
        this.plugin = plugin;
    }

    public boolean isPlayerHasFlag(Player p, String name){
        MySQLManagerV2.Query query = plugin.mysql.query("SELECT * FROM flagdata WHERE uuid = '"+p.getUniqueId().toString()+"' AND flag = '"+name+"';");
        ResultSet rs = query.getRs();
        if(rs==null){
            query.close();
            return false;
        }
        try {
            if(rs.next()){
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        query.close();
        return false;
    }

    public int getCount(Player p, String name){
        MySQLManagerV2.Query query = plugin.mysql.query("SELECT * FROM counter WHERE uuid = '"+p.getUniqueId().toString()+"' AND name = '"+name+"';");
        ResultSet rs = query.getRs();
        if(rs==null){
            query.close();
            return -1;
        }
        try {
            if(rs.next()){
                int i = rs.getInt("count");
                query.close();
                return i;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        query.close();
        return  -1;
    }

    public void countUpdate(Player p, String name, int count){
        if(getCount(p,name)!=-1){
            plugin.mysql.execute("UPDATE counter SET count = "+count+" WHERE uuid = '"+p.getUniqueId().toString()+"' AND name = '"+name+"';");
            return;
        }
        plugin.mysql.execute("INSERT INTO counter (player,uuid,name,count)  VALUES ('"+p.getName()+"','"+p.getUniqueId().toString()+"','"+name+"',"+count+");");
    }

    public void countDelete(Player p, String name){
        plugin.mysql.execute("DELETE FROM counter WHERE uuid = '"+p.getUniqueId().toString()+"' AND name = '" + name + "';");
    }

    public void addPlayerFlag(Player p, String name){
        if(isPlayerHasFlag(p,name)){
            return;
        }
        plugin.mysql.execute("INSERT INTO flagdata (player,uuid,flag)  VALUES ('"+p.getName()+"','"+p.getUniqueId().toString()+"','"+name+"');");
    }

    public void removePlayerFlag(Player p, String name){
        plugin.mysql.execute("DELETE FROM flagdata WHERE uuid = '" + p.getUniqueId().toString() + "' AND flag = '"+name+"';");
    }
}
