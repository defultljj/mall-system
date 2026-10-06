package com.gec.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.components.FileTemplate;
import com.gec.utils.FileUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/*
 * 所有 Controller 的父类:
 *  - newPage():     统一把前端页码/页大小转成 MyBatis-Plus 分页对象(非法参数回退默认值)
 *  - 文件输出相关(getFileTemplate/outFile/send404File/getMIME):
 *      outFile 从 upload 子目录读文件并直接写回响应流(图片回显);
 *      send404File 输出 404 兜底图(再失败降级为纯文本 404);
 *      getFileTemplate 由真正用文件的子类(Brand/PublishGoods)提供可用模板对象,
 *      用户/角色/部门模块不涉及文件输出, 其 getFileTemplate() 仍返回 null, 不受影响。
 */
public abstract class BaseController {
    static final int DEF_PAGE = 1;    //默认页码
    static final int DEF_LIMIT = 10;  //默认页大小。

    /* 补齐实现: 校验 page/limit 后真正构造 MyBatis-Plus 分页对象.
     * (资料里方法体为空; getInteger 已在下方做"为空/<=0 回退默认值"的校验) */
    public Page newPage(Integer page, Integer limit){
        return new Page<>( getInteger(page, DEF_PAGE), getInteger(limit, DEF_LIMIT) );
    }

    /* {2}做一个数据校验, 不能为空, 不能 <= 0 */
    private Integer getInteger(Integer value, int defVal) {
        if( value==null || value.intValue()<=0 ){
            return defVal;
        }
        return value;
    }
    protected abstract FileTemplate getFileTemplate();

    Map<String,String> MIMES = new HashMap();
    protected String getMIME(String fileName){
        String extName = FileUtils.extName(fileName);
        String mime = MIMES.get(extName);
        return mime;
    }
    //mime 是设置在响应头部, 再来告之浏览器, 当前这个文件的格式。
    public BaseController(){
        MIMES.put(".jpg","image/jpeg");
        MIMES.put(".jpeg","image/jpeg");
        MIMES.put(".png","image/png");
        MIMES.put(".html","text/html");
		MIMES.put(".css","text/css");
		MIMES.put(".js","text/javascript");
    }

    /*
    *  MIME: 媒体内容类型 (意指告诉浏览器当前的文件内型是什么)
    *  扩展名  MIME
    *  jpg    image/jpeg
    *  png    image/png
    *  html   text/html
    *  mp4    mpeg/mp4
    */

    //2.这个方法用来输出文件。
    //   用途: 子类(品牌/商品)把 upload 下的图片直接写回浏览器, 不走 R 的 JSON 包装。
    //   subDIR 传 "" 时读 upload 根目录(404_pic.png 就放在根目录下);
    //   文件不存在时这里直接抛异常, 由调用方决定是"兜底 404 图"还是"业务报错"。
    protected void outFile(HttpServletResponse resp,
        String subDIR, String fileName)
        throws IOException{
        /* 1.获取文件操作操作模板类。*/
        FileTemplate fileTemplate = getFileTemplate();
        /* 2.读取文件数据。*/
        //   文件不存在时 FileInputStream 构造会抛 IOException, 直接往外传;
        //   流打开但读成空字节(0 字节文件)同样视为"不存在", 统一抛业务异常。
        byte[] data = fileTemplate.getFile(subDIR, fileName);
        if (data == null || data.length == 0) {
            throw new RuntimeException("文件不存在: " + subDIR + "/" + fileName);
        }
        /* 3.获取 MIME 类型。*/
        String mime = getMIME(fileName);
        /* 4.设置 MIME 到响应头。*/
        //   未收录的扩展名(如 .webp)给通用二进制类型, 避免浏览器按文本去解析图片。
        resp.setContentType(
            mime == null ? "application/octet-stream" : mime);
        /* 5.把数据输出到响应的缓冲区。*/
        resp.getOutputStream().write(data);
        resp.getOutputStream().flush();
    }

    /*
    * 1.在目录下放置一个文件进来。
    *   位置: upload/ 根目录
    *   文件: 404_pic.png
    * 实现: 先尝试输出 upload 根目录的 404_pic.png(缺省图);
    *       二次兜底: 兜底图本身也读不到时绝不能再抛异常,
    *       降级为 404 状态 + 纯文本 "404", 保证调用方不会二次崩溃。
    */
    protected void send404File(HttpServletResponse resp) {
        try{
            outFile(resp, "", "404_pic.png");
        }catch (Exception e){
            e.printStackTrace();
            //兜底图不可用 → 既然已经知道文件不存在, 就明确回 404, 而不假装成功
            try {
                resp.setStatus(404);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getOutputStream().write(
                    "404".getBytes(StandardCharsets.UTF_8));
                resp.getOutputStream().flush();
            } catch (Exception ex) {
                //响应可能已提交/客户端已断开, 此处已无路可退, 只记录不抛
                ex.printStackTrace();
            }
        }
    }

}