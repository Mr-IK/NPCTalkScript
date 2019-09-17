package jp.mkserver.npctalkscript;

import jp.mkserver.npctalkscript.util.Util;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class NPCScript implements Listener, CommandExecutor {

    NPCTalkScript plugin;

    HashMap<String, FileConfiguration> scripts = new HashMap<>();
    HashMap<String, String> waitingNPCTalk = new HashMap<>();

    HashMap<String, String> npcFlag = new HashMap<>();
    HashMap<UUID, List<String>> selectData = new HashMap<>();

    public NPCScript(NPCTalkScript plugin){
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this,plugin);
        plugin.getCommand("npct").setExecutor(this);
        loadNPCFile();
    }

    /*
    NPCScriptはNPCと分岐ありのトークができたりコマンドを実行させられたりできるものです。

    PlaceHolder(PH)一覧
    <player>: プレイヤー名
    <select_count>: セレクトしたカウンターの数字

    スクリプト仕様
    ・上から順に実行されます。
    ・一部、実行名にできない単語があるようです。(yes,noなど)

    スクリプト一覧
    ※スクリプトから別スクリプトを実行した場合、元スクリプトはその時点で終了します

    say ○○: チャットをクリックしたプレイヤーに送信します。PH使用可能。
    delay 数字: 1につき1tick遅延します。
    select セレクト不可時メッセージ 選択肢1:選んだ場合の実行するスクリプト名 選択肢2:選んだ場合の実行するスクリプト名 選択肢3:選んだ場合の実行するスクリプト名 …
    =>条件分岐を作成できます。
    command ○○ △△: コマンドをコンソールから実行できます。PH使用可能。
    hasflag:フラグ名 持っていた場合の実行するスクリプト名   => フラグが立っていた場合スクリプトを中断して別のスクリプトを起動します
    addflag フラグ名: フラグ名のフラグを立てます
    removeflag フラグ名: フラグ名のフラグを下げます
    randomexecute 実行スクリプト名1 実行スクリプト名2 ...
    => ランダムでスクリプトを実行します。
    execute 実行スクリプト名: スクリプトを実行します

　　☆便利で無駄なカウンターのスクリプト(多い)☆
    counter add 名前 数字: カウンターが使えます。これはカウンターに数字を追加するスクリプト=デス
    counter take 名前 数字:  これはカウンターから数字を引くスクリプト=デス
    counter is 名前 数字 スクリプト名: カウンターの数字が数字ならスクリプト実行
    counter up 名前 数字 スクリプト名: カウンターの数字が指定した数字より多いならスクリプト実行
    counter isup 名前 数字 スクリプト名: カウンターの数字が指定した数字と同じか多いならスクリプト実行
    counter down 名前 数字 スクリプト名: カウンターの数字が指定した数字より少ないならスクリプト実行
    counter isdown 名前 数字 スクリプト名: カウンターの数字が指定した数字と同じか少ないならスクリプト実行
    counter div 名前 数字 スクリプト名: カウンターの数字が指定した数字で割り切れるならスクリプト実行
    counter clear 名前: カウンターを削除

    ☆お金関係☆
    claim 金額 成功時スクリプト 失敗スクリプト: お金を請求します。成功/失敗時に実行するスクリプトと値段。
    givemoney 金額: お金をあげちゃう❤

    ☆アイテムあげちゃう❤☆
    useitem アイテム名(noneで未指定):マテリアル名:データ:数 成功時スクリプト 失敗時スクリプト
    => アイテムを使う

    その他
    select count: カウンター名 => PHの<select_count>の数字を指定します。
    unlockrenda: 連打対策を無効化します。通常は最後まで話を聞くまで再度話しかけられません。

    使用例

    main:
    - 'say 知ってますか？'
    - 'delay 10'
    - 'select 他の人と話してるようですね。 はい:hai いいえ:wakarann'

    hai:
    - 'say やはり、そうでしたか。'

    wakarann:
    - 'say それを しらないなんて とんでもない！'
    - 'delay 20'
    - 'command kill <player>'
     */


    public void loadNPCFile(){
        scripts.clear();
        File folder = new File(plugin.getDataFolder(), File.separator + "npc");
        if(!folder.exists()){
            folder.mkdir();
        }
        for(String s: folder.list()){
            File f = new File(folder, File.separator + s);
            FileConfiguration data = YamlConfiguration.loadConfiguration(f);

            if (f.exists()) {
                String npcid = s.replaceFirst(".yml","").split("_",2)[0];
                scripts.put(npcid,data);
            }
        }
    }

    public FileConfiguration getNPCFile(String name){
        if(scripts.containsKey(name)){
            return scripts.get(name);
        }
        return null;
    }

    public List<String> getNPCScript(FileConfiguration config, String id){
        return config.getStringList(id);
    }


    public void executeScript(String npcname, List<String> list, Player p){
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            if(waitingNPCTalk.containsKey(p.getUniqueId().toString()+":"+npcname)){
                String script = waitingNPCTalk.get(p.getUniqueId().toString()+":"+npcname);
                if(!script.equalsIgnoreCase("none")){
                    if(script.startsWith("select ")){
                        script = script.replaceFirst("select ","");
                        String[] args = script.split(" ");
                        List<String> uuids = new ArrayList<>();
                        for(int ii = 1;ii<args.length;ii++){
                            String data = args[ii];
                            String[] datas = data.split(":");
                            data = datas[0];
                            String privateid = UUID.randomUUID().toString();
                            Util.sendHoverText(p,data,null,"/NPCDataChat: "+privateid);
                            npcFlag.put(privateid,npcname+" "+datas[1]);
                            uuids.add(privateid);
                        }
                        waitingNPCTalk.put(p.getUniqueId().toString()+":"+npcname,waitingNPCTalk.get(p.getUniqueId().toString()+":"+npcname));
                        selectData.put(p.getUniqueId(),uuids);
                        return;
                    }
                }
                return;
            }
            waitingNPCTalk.put(p.getUniqueId().toString()+":"+npcname,"none");
            int select_count = 0;
            for(int i = 0;i<list.size();i++) {

                String script = list.get(i);
                if (!p.isOnline()) {
                    break;
                }

                if (script.equalsIgnoreCase("unlockrenda")) {
                    waitingNPCTalk.remove(p.getUniqueId().toString() + ":" + npcname);
                }

                if(script.startsWith("select count: ")){
                    script = script.replaceFirst("select count: ","");
                    select_count = plugin.flag.getCount(p,script);
                }

                if(script.startsWith("say ")){
                    script = script.replaceFirst("say ","");
                    p.sendMessage(script.replace("<player>",p.getName()).replace("<select_count>",select_count+""));
                }else if(script.startsWith("delay ")) {
                    script = script.replaceFirst("delay ", "");
                    int delay = 0;
                    try {
                        delay = Integer.parseInt(script);
                        Thread.sleep(delay * 50);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("NPCScript Error! type: delay is not Number.\n" +
                                "File: " + npcname + ".yml Line ?「" + script + "」");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else if(script.startsWith("command ")){
                    script = script.replaceFirst("command ","");
                    int finalSelect_count = select_count;
                    String finalScript = script;
                    Bukkit.getScheduler().runTask(plugin,()-> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalScript.replace("<player>", p.getName()).replace("<select_count>", finalSelect_count + ""));
                    });
                }else if(script.startsWith("addflag ")){
                    script = script.replaceFirst("addflag ","");
                    plugin.flag.addPlayerFlag(p,script);
                }else if(script.startsWith("removeflag ")){
                    script = script.replaceFirst("removeflag ","");
                    plugin.flag.removePlayerFlag(p,script);
                }else if(script.startsWith("execute ")){
                    script = script.replaceFirst("execute ","");
                    waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                    executeScript(npcname,getNPCScript(getNPCFile(npcname),script),p);
                    return;
                }else if(script.startsWith("randomexecute ")){
                    script = script.replaceFirst("randomexecute ","");
                    waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                    String[] scripts = script.split(" ");
                    Random rnd = new Random();
                    int s = rnd.nextInt(scripts.length);
                    waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                    executeScript(npcname,getNPCScript(getNPCFile(npcname),scripts[s]),p);
                    return;
                }else if(script.startsWith("hasflag:")){
                    script = script.replaceFirst("hasflag:","");
                    String[] arg = script.split(" ",2);
                    if(plugin.flag.isPlayerHasFlag(p,arg[0])){
                        waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                        executeScript(npcname,getNPCScript(getNPCFile(npcname),arg[1]),p);
                        return;
                    }
                }else if(script.startsWith("claim ")){
                    script = script.replaceFirst("claim ","");
                    String[] arg = script.split(" ",3);
                    long value = 0;
                    try {
                        value = Long.parseLong(arg[0]);
                        if(plugin.vault.getBalance(p.getUniqueId())>=value){
                            plugin.vault.withdraw(p.getUniqueId(),value);
                            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                            executeScript(npcname,getNPCScript(getNPCFile(npcname),arg[1]),p);
                            return;
                        }else{
                            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                            executeScript(npcname,getNPCScript(getNPCFile(npcname),arg[2]),p);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("NPCScript Error! type: value is not Number.\n" +
                                "File: " + npcname + ".yml Line ?「" + script + "」");
                    }
                }else if(script.startsWith("givemoney ")){
                    script = script.replaceFirst("givemoney ","");
                    long value = 0;
                    try {
                        value = Long.parseLong(script);
                        plugin.vault.deposit(p.getUniqueId(),value);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("NPCScript Error! type: value is not Number.\n" +
                                "File: " + npcname + ".yml Line ?「" + script + "」");
                    }
                }else if(script.startsWith("useitem ")){
                    script = script.replaceFirst("useitem ","");
                    String[] arg = script.split(" ",3);
                    String[] item = arg[0].split(":",4);
                    String itemname = item[0];
                    String itemtype = item[1];
                    if(Material.getMaterial(itemtype)!=null){
                        float data = 0;
                        int value = 1;
                        try {
                            data = Float.parseFloat(item[2]);
                            value = Integer.parseInt(item[3]);
                            if(value>=1&&value<=64){
                                ItemStack mainhand = p.getInventory().getItemInMainHand();
                                if(itemname.equalsIgnoreCase("none")||(mainhand!=null&&mainhand.getItemMeta()!=null&&mainhand.getItemMeta().getDisplayName()!=null&&mainhand.getItemMeta().getDisplayName().equals(itemname))){
                                    if(mainhand.getType().equals(Material.getMaterial(itemtype))&&mainhand.getDurability()==data){
                                        if(mainhand.getAmount()>=value){
                                            if(mainhand.getAmount() - value==0){
                                                p.getInventory().setItemInMainHand(null);
                                            }else{
                                                p.getInventory().getItemInMainHand().setAmount(mainhand.getAmount() - value);
                                            }
                                            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                                            executeScript(npcname,getNPCScript(getNPCFile(npcname),arg[1]),p);
                                            return;
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("NPCScript Error! type: value is not Number.\n" +
                                    "File: " + npcname + ".yml Line ?「" + script + "」");
                        }
                    }
                    waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                    executeScript(npcname,getNPCScript(getNPCFile(npcname),arg[2]),p);
                    return;
                }else if(script.startsWith("select ")){
                    script = script.replaceFirst("select ","");
                    String[] args = script.split(" ");
                    if(selectData.containsKey(p.getUniqueId())){
                        executeScript(npcname,getNPCScript(getNPCFile(npcname),args[0]),p);
                        break;
                    }
                    List<String> uuids = new ArrayList<>();
                    for(int ii = 1;ii<args.length;ii++){
                        String data = args[ii];
                        String[] datas = data.split(":");
                        data = datas[0];
                        String privateid = UUID.randomUUID().toString();
                        Util.sendHoverText(p,data,null,"/NPCDataChat: "+privateid);
                        npcFlag.put(privateid,npcname+" "+datas[1]);
                        uuids.add(privateid);
                    }
                    waitingNPCTalk.put(p.getUniqueId().toString()+":"+npcname,list.get(i));
                    selectData.put(p.getUniqueId(),uuids);
                    return;
                }else if(script.startsWith("counter ")){
                    script = script.replaceFirst("counter ","");

                    if(script.startsWith("add ")){
                        script = script.replaceFirst("add ","");
                        String[] args = script.split(" ");
                        int old = plugin.flag.getCount(p,args[0]);
                        if(old==-1){
                            old = 0;
                        }
                        int add = 0;
                        try {
                            add = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("NPCScript Error! type: number is not Number.\n" +
                                    "File: " + npcname + ".yml Line ?「" + script + "」");
                        }
                        old += add;
                        plugin.flag.countUpdate(p,args[0],old);
                    }

                    if(script.startsWith("take ")){
                        script = script.replaceFirst("take ","");
                        String[] args = script.split(" ");
                        int old = plugin.flag.getCount(p,args[0]);
                        if(old==-1){
                            old = 0;
                        }
                        int add = 0;
                        try {
                            add = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("NPCScript Error! type: number is not Number.\n" +
                                    "File: " + npcname + ".yml Line ?「" + script + "」");
                        }
                        old -= add;
                        plugin.flag.countUpdate(p,args[0],old);
                    }

                    if(script.startsWith("is ")){
                        script = script.replaceFirst("is ","");
                        String[] args = script.split(" ");
                        int old = plugin.flag.getCount(p,args[0]);
                        if(old==-1){
                            old = 0;
                        }
                        int check = 0;
                        try {
                            check = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("NPCScript Error! type: number is not Number.\n" +
                                    "File: " + npcname + ".yml Line ?「" + script + "」");
                        }
                        if(old == check){
                            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                            executeScript(npcname,getNPCScript(getNPCFile(npcname),args[2]),p);
                            return;
                        }
                    }

                    if(script.startsWith("up ")){
                        script = script.replaceFirst("up ","");
                        String[] args = script.split(" ");
                        int old = plugin.flag.getCount(p,args[0]);
                        if(old==-1){
                            old = 0;
                        }
                        int check = 0;
                        try {
                            check = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("NPCScript Error! type: number is not Number.\n" +
                                    "File: " + npcname + ".yml Line ?「" + script + "」");
                        }
                        if(old > check){
                            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                            executeScript(npcname,getNPCScript(getNPCFile(npcname),args[2]),p);
                            return;
                        }
                    }

                    if(script.startsWith("isup ")){
                        script = script.replaceFirst("isup ","");
                        String[] args = script.split(" ");
                        int old = plugin.flag.getCount(p,args[0]);
                        if(old==-1){
                            old = 0;
                        }
                        int check = 0;
                        try {
                            check = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("NPCScript Error! type: number is not Number.\n" +
                                    "File: " + npcname + ".yml Line ?「" + script + "」");
                        }
                        if(old >= check){
                            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                            executeScript(npcname,getNPCScript(getNPCFile(npcname),args[2]),p);
                            return;
                        }
                    }

                    if(script.startsWith("down ")){
                        script = script.replaceFirst("down ","");
                        String[] args = script.split(" ");
                        int old = plugin.flag.getCount(p,args[0]);
                        if(old==-1){
                            old = 0;
                        }
                        int check = 0;
                        try {
                            check = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("NPCScript Error! type: number is not Number.\n" +
                                    "File: " + npcname + ".yml Line ?「" + script + "」");
                        }
                        if(old < check){
                            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                            executeScript(npcname,getNPCScript(getNPCFile(npcname),args[2]),p);
                            return;
                        }
                    }

                    if(script.startsWith("isdown ")){
                        script = script.replaceFirst("isdown ","");
                        String[] args = script.split(" ");
                        int old = plugin.flag.getCount(p,args[0]);
                        if(old==-1){
                            old = 0;
                        }
                        int check = 0;
                        try {
                            check = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("NPCScript Error! type: number is not Number.\n" +
                                    "File: " + npcname + ".yml Line ?「" + script + "」");
                        }
                        if(old <= check){
                            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                            executeScript(npcname,getNPCScript(getNPCFile(npcname),args[2]),p);
                            return;
                        }
                    }

                    if(script.startsWith("div ")){
                        script = script.replaceFirst("div ","");
                        String[] args = script.split(" ");
                        int old = plugin.flag.getCount(p,args[0]);
                        if(old==-1){
                            old = 0;
                        }
                        int check = 0;
                        try {
                            check = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("NPCScript Error! type: number is not Number.\n" +
                                    "File: " + npcname + ".yml Line ?「" + script + "」");
                        }
                        if(old % check == 0){
                            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
                            executeScript(npcname,getNPCScript(getNPCFile(npcname),args[2]),p);
                            return;
                        }
                    }

                    if(script.startsWith("clear ")){
                        script = script.replaceFirst("clear ","");
                        plugin.flag.countDelete(p,script);
                    }
                }
            }
            waitingNPCTalk.remove(p.getUniqueId().toString()+":"+npcname);
        });
    }

    @EventHandler
    public void onChat(PlayerCommandPreprocessEvent e){
        if(e.getMessage().startsWith("/NPCDataChat: ")){
            e.setCancelled(true);
            e.setMessage(e.getMessage().replace("/NPCDataChat: ",""));
            if(npcFlag.containsKey(e.getMessage())){
                String flagdata = npcFlag.get(e.getMessage());
                npcFlag.remove(e.getMessage());
                if(selectData.get(e.getPlayer().getUniqueId())==null){
                    return;
                }
                for(String str: selectData.get(e.getPlayer().getUniqueId())){
                    npcFlag.remove(str);
                }
                selectData.remove(e.getPlayer().getUniqueId());
                String[] flag = flagdata.split(" ",2);
                FileConfiguration file = getNPCFile(flag[0]);
                if(file==null){
                    plugin.getLogger().warning("NPCScript Error! type: not exist file.\n" +
                            "File: "+flag[0]+".yml");
                    return;
                }
                List<String> list = getNPCScript(file,flag[1]);
                if(list==null){
                    plugin.getLogger().warning("NPCScript Error! type: not exist func.\n" +
                            "File: "+flag[0]+".yml 「"+flag[1]+"」");
                    return;
                }
                waitingNPCTalk.remove(e.getPlayer().getUniqueId().toString()+":"+flag[0]);
                executeScript(flag[0],list,e.getPlayer());
            }
        }
    }

    @EventHandler
    public void click(NPCRightClickEvent event){
        //Handle a click on a NPC. The event has a getNPC() method.
        //Be sure to check event.getNPC() == this.getNPC() so you only handle clicks on this NPC!
        FileConfiguration file = getNPCFile(event.getNPC().getId()+"");
        if(file==null){
            return;
        }
        executeScript(event.getNPC().getId()+"",getNPCScript(file,"main"),event.getClicker());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0){
            loadNPCFile();
        }
        return true;
    }
}
