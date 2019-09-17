package jp.mkserver.npctalkscript;

import jp.mkserver.npctalkscript.api.MySQLManagerV2;
import jp.mkserver.npctalkscript.api.VaultAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class NPCTalkScript extends JavaPlugin {

    MySQLManagerV2 mysql;
    FlagManager flag;
    VaultAPI vault;

    FileConfiguration config;

    NPCScript script;

    @Override
    public void onEnable() {
        // Plugin startup logic

        // Config load
        saveDefaultConfig();
        config = getConfig();

        // init APIs
        mysql = new MySQLManagerV2(this,"NPCTalkScript");
        vault = new VaultAPI(this);
        flag = new FlagManager(this);
        script = new NPCScript(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
