package com.inke.library;

import android.content.Context;

import com.inke.library.utils.ArrayUtils;
import com.inke.library.utils.Constants;
import com.inke.library.utils.ReflectUtils;

import java.io.File;
import java.util.HashSet;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * 修复工具
 */
public class FixDexUtils {

    //需要修复的集合
    private static HashSet<File> loadedDex = new HashSet<>();

    static {
        //修复之前请求集合
        loadedDex.clear();
    }

    //加载热修复的Dex文件
    public static void loadFixedDex(Context context) {
        File fileDir = context.getDir(Constants.DEX_DIR, Context.MODE_PRIVATE);
        // 循环私有目录中的所有dex文件
        File[] listFiles = fileDir.listFiles();
        // 筛选dex文件，加入集合
        for(File file : listFiles) {
            if(file.getName().endsWith(Constants.DEX_SUFFIX) && !"classes.dex".equals(file.getName())) {
                //找到修复包的dex文件
                loadedDex.add(file);
            }
        }

        //开始上天，创建类加载器
        createDexClassLoader(context, fileDir);
    }

    //创建加载补丁的DexClassLoader类加载器
    private static void createDexClassLoader(Context context, File fileDir) {
        // 创建解压目录
        String optimizedDir = fileDir.getAbsolutePath() + File.separator + "opt_dex";

        //创建输出目录
        File fopt = new File(optimizedDir);
        if(!fopt.exists()) {
            //创建输出目录（层级目录创建）
            fopt.mkdirs();
        }

        for (File dex : loadedDex) {
            //创建自有的类加载器
            DexClassLoader classLoader = new DexClassLoader(dex.getAbsolutePath(),
                    optimizedDir, null, context.getClassLoader());

            //每循环一次，修复一次
            hotfix(classLoader, context);
        }
    }

    private static void hotfix(DexClassLoader classLoader, Context context) {
        try {
            // 获取系统的PathClassLoader
            PathClassLoader pathLoader = (PathClassLoader) context.getClassLoader();
            //获取自有的dexElements数组
            Object myElements = ReflectUtils.getDexElements(ReflectUtils.getPathList(classLoader));

            //获取系统的dexElements数组
            Object systemElements = ReflectUtils.getDexElements(ReflectUtils.getPathList(pathLoader));

            //合并数组，并且生成一个新的dexElements数组（包含排序工作）
            Object dexElements = ArrayUtils.combineArray(myElements, systemElements);

            // 获取系统的pathList属性（通过反射技术）
            Object systemPathList = ReflectUtils.getPathList(pathLoader);

            //通过反射的技术，将 新的dexElements数组 赋值给 系统的pathList属性
            ReflectUtils.setField(systemPathList, systemPathList.getClass(), dexElements);

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
