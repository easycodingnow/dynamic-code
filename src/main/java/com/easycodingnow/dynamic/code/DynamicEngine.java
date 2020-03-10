package com.easycodingnow.dynamic.code;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.tools.*;
import java.io.File;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lihao
 * @since 2020-02-13
 */
@Component
public class DynamicEngine implements ApplicationContextAware {
    private static Map<String, AtomicLong> classReloadNum = new HashMap<>();
    private static long startTime = System.currentTimeMillis();
    private static MyLoader myClassLoader;

    private ApplicationContext applicationContext;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    private void init(){
        URL[] urls = new URL[1];
        try {
            urls[0] = new File(System.getProperty("user.dir")).toURL();
            myClassLoader = new MyLoader(urls, applicationContext.getClassLoader());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public  Class<?> compile(String className, String javaCodes) {

        if (classReloadNum.containsKey(className)) {
            classReloadNum.get(className).incrementAndGet();
        } else {
            classReloadNum.put(className, new AtomicLong(startTime));
        }

        String newClassName = className + "_" + classReloadNum.get(className);
        String[] classNameArr = className.split("\\.");
        String[] newClassNameArr = newClassName.split("\\.");

        String newJavaCode = javaCodes.replaceAll(classNameArr[classNameArr.length
                - 1], newClassNameArr[newClassNameArr.length -1]);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        StrSrcJavaObject srcObject = new StrSrcJavaObject(newClassName, newJavaCode);
        Iterable<? extends JavaFileObject> fileObjects = Collections.singletonList(srcObject);
        String flag = "-d";

        String outDir;
        File classPath;
        try {
            classPath = new File(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).toURI());
            outDir = classPath.getAbsolutePath() + File.separator;
            Iterable<String> options = Arrays.asList(flag, outDir);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, fileObjects);
            boolean result = task.call();
            if (result) {
                try {
                    return myClassLoader.loadClass(newClassName);
                } catch (ClassNotFoundException e) {
                    System.out.println("加载失败!");
                    e.printStackTrace();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }


    private static class StrSrcJavaObject extends SimpleJavaFileObject {
        private String content;
        StrSrcJavaObject(String name, String content) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.content = content;
        }
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

    public static class MyLoader extends URLClassLoader {
        public MyLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public Class loadClass(String name) throws ClassNotFoundException{
            return super.loadClass(name);
        }
    }


}
