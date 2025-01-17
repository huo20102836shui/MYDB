package top.guoziyang.mydb.backend;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import top.guoziyang.mydb.backend.dm.DataManager;
import top.guoziyang.mydb.backend.server.Server;
import top.guoziyang.mydb.backend.tbm.TableManager;
import top.guoziyang.mydb.backend.tm.TransactionManager;
import top.guoziyang.mydb.backend.tm.TransactionManagerImpl;
import top.guoziyang.mydb.backend.utils.Panic;
import top.guoziyang.mydb.backend.vm.VersionManager;
import top.guoziyang.mydb.backend.vm.VersionManagerImpl;
import top.guoziyang.mydb.common.Error;

import java.io.File;

public class Launcher {

    public static final int port = 9999;

    public static final long DEFALUT_MEM = (1<<20)*64;
    public static final long KB = 1 << 10;
	public static final long MB = 1 << 20;
	public static final long GB = 1 << 30;

    private static final String DBPath = "D:\\0githubProject\\java\\MYDB\\tmp\\mydb";

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("open", true, "-open DBPath");
        options.addOption("create", true, "-create DBPath");
        options.addOption("mem", true, "-mem 64MB");
        if(args.length == 0){
            // 命令行无法启动，原因1是路径问题(已解决)根据运行的操作系统更换路径，
            // 原因2(未解决)是：No plugin found for prefix '.args=-create D' in the current project and in the plugin groups [org.apache.maven.plugins, org.codehaus.mojo] available from the repositories
            // mvn exec:java -Dexec.mainClass="top.guoziyang.mydb.backend.Launcher" -Dexec.args="-open /tmp/mydb"
            // 原因3(已解决)是 create(新建数据库) || open(打开数据库) 关键字的问题，初次运行应该使用-create
            args = new String[2];
            if((new File(DBPath + TransactionManagerImpl.XID_SUFFIX)).exists()){
                args[0] = "-open";
            }else {
                args[0] = "-create";
            }
            args[1] = DBPath;
        }
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if(cmd.hasOption("open")) {
            openDB(cmd.getOptionValue("open"), parseMem(cmd.getOptionValue("mem")));
            return;
        }
        if(cmd.hasOption("create")) {
            createDB(cmd.getOptionValue("create"));
            return;
        }
        System.out.println("Usage: launcher (open|create) DBPath");
    }

    private static void createDB(String path) {
        TransactionManager tm = TransactionManager.create(path);
        DataManager dm = DataManager.create(path, DEFALUT_MEM, tm);
        VersionManager vm = new VersionManagerImpl(tm, dm);
        TableManager.create(path, vm, dm);
        tm.close();
        dm.close();
    }

    private static void openDB(String path, long mem) {
        TransactionManager tm = TransactionManager.open(path);
        DataManager dm = DataManager.open(path, mem, tm);
        VersionManager vm = new VersionManagerImpl(tm, dm);
        TableManager tbm = TableManager.open(path, vm, dm);
        new Server(port, tbm).start();
    }

    private static long parseMem(String memStr) {
        if(memStr == null || "".equals(memStr)) {
            return DEFALUT_MEM;
        }
        if(memStr.length() < 2) {
            Panic.panic(Error.InvalidMemException);
        }
        String unit = memStr.substring(memStr.length()-2);
        long memNum = Long.parseLong(memStr.substring(0, memStr.length()-2));
        switch(unit) {
            case "KB":
                return memNum*KB;
            case "MB":
                return memNum*MB;
            case "GB":
                return memNum*GB;
            default:
                Panic.panic(Error.InvalidMemException);
        }
        return DEFALUT_MEM;
    }
}
