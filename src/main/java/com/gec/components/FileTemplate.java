package com.gec.components;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * 文件操作模板类(资料原样保留)。
 * DIR 来自 application.properties 的 web.uploadDir(缺失该配置 → 启动直接失败)。
 * Windows 下 new File() 能同时识别 '/' 与 '\', 拼接处无需额外处理分隔符。
 * (用户/角色/部门模块未调用本组件的方法; 保存为 @Component 是为后续商品模块上传文件预留)
 */
@Component
public class FileTemplate {

    //1.打开这个注释。
    @Value("${web.uploadDir}")
    private String DIR;

    private MultipartFile mFile;

    public FileTemplate(){}
    public FileTemplate(MultipartFile mfile){
        this.mFile = mfile;
    }
    public FileTemplate(String DIR){
        this.DIR = DIR;
    }

    /*
    *         DIR        (DIR)最终输出
    * 情况1   d:\space    d:\space\
    * 情况2   d:\space\   d:\space\

    *         SUB        (SUB)最终输出
    * 情况1   upload      upload\
    * 情况1   upload\     upload\

    */
    private String getPrefixPath(String DIR, String SUB){
        //1.先判断 DIR 有没有 \, 没有就加上。
        if( !DIR.endsWith("\\") ){
            DIR = DIR +"\\";
        }
        if( SUB==null ) SUB = "";
        if( SUB.length()>0 ){
            if( !SUB.endsWith("\\") ){
                SUB = SUB +"\\";
            }
        }
        return DIR + SUB;
    }

    //1.此方法仅用于小文件的处理。
    public byte[] getFile(String subDir, String fileName)
        throws IOException {
        String prefix = getPrefixPath(DIR, subDir);
        String path = prefix + fileName;
        //1.创建 file 对象。
        File _file = new File(path);
        //2.创建文件输入流
        InputStream fis = new FileInputStream(_file);
        int len = fis.available();   //文件长度
        //3.创建与文件等大的数组。
        byte[] buff = new byte[ len ];
        //4.读满这个数组。
        fis.read( buff );
        fis.close();
        //5.返回文件字节数据。
        return buff;
    }

    public void setMultipartFile(MultipartFile mFile){
        this.mFile = mFile;
    }

    public boolean saveFile(String subDir, String fileName)
        throws IOException {
        String prefix = getPrefixPath(DIR, subDir);
        String path = prefix + fileName;
        File _target = new File(path);
        /*
        * mFile: 它是文件上传的封装类
        * (包含有文件上传的数据)
        * transferTo() 可以将里面数据写入本地磁盘。
        */
        this.mFile.transferTo( _target );
        return true;
    }

    public void printDir(){
        System.out.println("DIR:"+ DIR);
    }

}