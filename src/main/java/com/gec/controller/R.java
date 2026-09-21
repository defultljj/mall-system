package com.gec.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/*
 * 统一响应对象(资料原样保留): 键 result / status / data / total / cause。
 * 所有 Controller 都按此格式返回, 不要替换成别的 Result 类。
 */
public class R extends HashMap {
  final static int STATUS_OK = 200;
  final static int STATUS_ERR = 500;
  final static int STATUS_NOT_FOUND = 404;

  public static R ok(){
    R r = new R();
    r.put("result","success");
    r.put("status",STATUS_OK);
    return r;
  }
  public static R ok(Object obj){
    R r = new R();
    r.put("result","success");
    r.put("status",STATUS_OK);
    r.put("data",obj);
    return r;
  }
  public static R ok(Page page){
    R r = new R();
    r.put("result","success");
    r.put("status",STATUS_OK);
    r.put("data",page.getRecords());
    r.put("total",page.getTotal());
    return r;
  }

  public R put(String key, Object val){
    super.put( key , val);
    return this;
  }

  public static R err(Exception e) {
    R r = new R();
    r.put("result","failed");
    r.put("status",STATUS_ERR);
    r.put("cause",e.getMessage());
    return r;
  }

  public static R convertPage(IPage page) {
    R r = new R();
    r.put("result","success");
    r.put("status",STATUS_OK);
    r.put("data",page.getRecords());
    r.put("total",page.getTotal());
    return r;
  }

  public void write(HttpServletResponse resp) {
    Object status = get("status");
    resp.setStatus( (int)status );
    resp.setContentType("application/json;charset=UTF-8");
    try{
      resp.getWriter().write( toJson() );
    }catch (Exception e){
      e.printStackTrace();
    }
  }
  private String toJson(){
    return
        "{"+
        "\"result\":\""+ get("result") +"\","+
        "\"data\":\""+ get("data") +"\","+
        "\"cause\":\""+ get("cause") +"\""+
        "}";
  }

}