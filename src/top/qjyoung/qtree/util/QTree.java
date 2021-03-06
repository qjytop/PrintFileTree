
package top.qjyoung.qtree.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QTree {
    private static final String SPACE = "  "; // 间隔
    private static final boolean COLLAPSE = true; // true 折叠 false 不折叠
    
    private static String ENTER = System.getProperty("line.separator");
    
    public static void main(String args[]) throws IOException {
        long time_start = System.currentTimeMillis();
        System.out.println("processing...");
        String dir = "";
        //		dir = "D:/";
        dir = "."; // 当前工程目录
        
        //----------------从外部接收数据 通过命令行传参数--------------------------------------
        //---命令行传过来的参数 String args[]
        //---(注意文件夹中不能出现空格,否则你的路径名会被split(" ")切割放进数组里,所以这里要拼接一下,以防万一,)
        //		for (String s : args) {
        //			dir += s.replace("\\", "\\\\") + " ";
        //		}
        //		dir = dir.trim();
        //------------------------------------------------------
        FileWrapper fileWrapper = new FileWrapper(dir);
        String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis()));
        File targetFile = new File(dir + "/tree-" + date + ".txt");
        
        PrintWriter print = new PrintWriter(new OutputStreamWriter(new FileOutputStream(targetFile), "utf-8"), true);
        print(fileWrapper, print);
        
        System.out.println("---finish---");
        
        long time_end = System.currentTimeMillis();
        long timeElapsed = time_end - time_start;
        
        long ms = timeElapsed % 1000;
        long s = (timeElapsed / 1000) % 60;
        long min = timeElapsed / 1000 / 60;
        
        String timeElapsedStr = ENTER + ENTER + ENTER + min + " min" + ENTER + s + " s" + ENTER + ms + " ms" + ENTER + ENTER;
        System.out.println(timeElapsedStr);
        print.print(timeElapsedStr);
        print.close();
    }
    
    /**
     * ─ 横线用来指示子节点（可以多个）
     * <p>
     * │ 竖向延长线，当中间孩子节点过多时用来连接更末尾的孩子节点
     * <p>
     * ├ 指示中间文件夹节点，通常后面会跟一个或多个第一种符号
     * <p>
     * └ 指示末尾孩子节点，通常后面会跟一个或多个第一种符号
     * 例子（─ 乘1 ─）
     * 父亲
     * │
     * ├─ 子1（中间）
     * │   └─ 孙子
     * └─ 子2（末尾）
     * 例子2（─ 乘5 ─────）
     * 父亲
     * │
     * ├───── 子1（中间）
     * │    └───── 孙子
     * └───── 子2（末尾）
     */
    @SuppressWarnings("unused")
    private static void print(FileWrapper fileWrapper, PrintWriter print) {
        // 初始化 得到分类好的孩子
        fileWrapper.init();
        
        List<File> files = fileWrapper.getFiles();
        List<FileWrapper> dirs = fileWrapper.getDirs();
        
        int filesSize = files.size(); // file个数
        int dirsSize = dirs.size(); // dir个数
        
        // 获得祖上传下来的信息(标记)
        List<Boolean> flags = new ArrayList<>(fileWrapper.getFlags());
        boolean hasAnOnlyChildAndIsDir = dirsSize == 1 && filesSize == 0; // 是否独子，折叠时候用到
        
        //---------打印自己(当前文件夹)----------------------------------------------
        if (COLLAPSE && fileWrapper.isSingle()) { // 折叠且当前文件对象是独子, 不打印prefix+
            if (hasAnOnlyChildAndIsDir) { // 控制换行：有且只有一个孩子, 并且是文件夹类型, 不换行, 否则换行
                print.print("/" + fileWrapper.getName());
            } else {
                print.println("/" + fileWrapper.getName());
            }
        } else { // 不折叠或当前文件对象非独子, 打印prefix+fileName
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i <= flags.size() - 1; i++) { // 这块比较难理解，也是核心代码！！！
                prefix.append(SPACE);
                if (i == flags.size() - 1) { // 特殊处理: 最后一个记录, 也就是当前文件对象
                    if (flags.get(i)) { // 中间孩子节点 true
                        prefix.append("├");
                    } else {
                        prefix.append("└"); // 小儿子 false
                    }
                    break; // 循环结束
                }
                if (flags.get(i)) { // (祖先)中间节点true非小儿子
                    prefix.append("│");
                } else { // (祖先)小儿子节点false
                    prefix.append(" ");
                }
            }
            prefix.append("─");
            // 有且只有一个孩子,并且是文件夹类型-->不换行,否则换行
            if (COLLAPSE && hasAnOnlyChildAndIsDir) {
                print.print(prefix.toString() + fileWrapper.getName());
            } else {
                print.println(prefix.toString() + fileWrapper.getName());
            }
        }
        
        //---------子文件(先打印)----------------------------------------------
        
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i <= flags.size() - 1; i++) {
            prefix.append(SPACE);
            if (flags.get(i)) { // 他的爸爸是中间节点
                prefix.append("│");
            } else {
                prefix.append(" "); // 他的爸爸是小儿子
            }
        }
        prefix.append(SPACE);
        if (dirsSize != 0) { // 如果文件夹下还有子文件夹
            prefix.append("│");
        }
        
        for (File file : files) {
            print.println(prefix.toString() + " " + file.getName());
        }
        
        //---------遍历子文件夹(传递标记到孩子)----------------------------------------------
        
        for (int j = 0; j < dirsSize; j++) {
            FileWrapper fileWrapperSon = dirs.get(j);
            List<Boolean> flagsTemp = new ArrayList<>(flags);
            
            // 如果折叠, 并且是独子(dirsSize==1), 下面不执行(不需标记),
            // 反之执行(原因:将多级单一文件夹看成一个, 整体标记跟随此行第一个文件夹)
            if (!(COLLAPSE && fileWrapperSon.isSingle())) {
                if (j == dirsSize - 1) { // 最后一个，即小儿子
                    flagsTemp.add(false);
                } else {
                    flagsTemp.add(true); // 中间儿子
                }
            }
            fileWrapperSon.setFlags(flagsTemp);
            print(fileWrapperSon, print);
        }
    }
}
