package com.gec.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gec.components.FileTemplate;
import com.gec.dao.OptionMapper;
import com.gec.domain.entity.Brand;
import com.gec.domain.search.BrandSearch;
import com.gec.domain.vo.BrandCategoryVO;
import com.gec.domain.vo.BrandVO;
import com.gec.service.IBrandService;
import com.gec.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
 * 品牌管理(路由风格与 RoleController 一致, 本轮新增品牌 ↔ 类别关联 + logo 上传/回显):
 *   list             POST   /Brand/list?page=&limit=&brandName=&id=&categoryId=   品牌分页列表(搜索)
 *   setStatus        POST   /Brand/setStatus?id=&showStatus=                      设置显示状态
 *   save             POST   /Brand/save                                            新增品牌
 *   update           PUT    /Brand/update                                          修改品牌(必须带 id)
 *   get              GET    /Brand/get?id=                                         按 id 查品牌详情
 *   delete           DELETE /Brand/delete?id=                                      删除品牌(先清关联)
 *   associateCategory POST  /Brand/associateCategory (JSON body)                   品牌 ↔ 类别勾选保存
 *   upload           POST   /Brand/upload (multipart 参数名 file)                  品牌 logo 上传
 *   readImage        GET    /Brand/readImage?fileName=                             读 logo 图片字节流(兼容保留)
 *   showImg          GET    /Brand/showImg/{fileName}                              读 logo 图片字节流(路径参数路由)
 *   associatedCategories GET /Brand/associatedCategories?brandId=                  某品牌已关联的类别(回显)
 *   listByCategory   POST   /Brand/listByCategory?page=&limit=&categoryId=         按类别列品牌(带 isAssociate)
 *   brandOptions     GET    /Brand/brandOptions/{categoryId}                       按类别列品牌下拉(商品上架第1步)
 * 业务错误统一 @ExceptionHandler 以 R 形式写回; 图片读取直接写字节流不走 R。
 */
@RestController
@RequestMapping("/Brand")
public class BrandController extends BaseController {
    //品牌 CRUD + 关联服务
    @Autowired
    private IBrandService brandService;
    //下拉选项 Mapper(按类别列品牌下拉, 与 RoleController 用法一致)
    @Autowired
    private OptionMapper optionMapper;
    //文件操作模板(上传/读取共用; 本模块图片固定放 upload/brand 子目录)
    @Autowired
    private FileTemplate fileTemplate;
    //当前模块的图片子目录(相对 web.uploadDir)
    private String SUB_DIR = "brand";

    /* 1.品牌分页列表(原 GET 版已改为 POST, 前端同步):
     * POST /Brand/list?page=1&limit=10&brandName=xx&id=&categoryId=
     * brandName 模糊, id 精确; categoryId 有值 → 只查已关联该类别的品牌 */
    @PostMapping("/list")
    public R list(Integer page, Integer limit, BrandSearch brandSearch) {
        //分页查询(page/limit 非法时由 newPage 回退默认 1/10)
        IPage<Brand> ip = brandService.listBrand(newPage(page, limit), brandSearch);
        return R.convertPage(ip);
    }

    /* 2.设置显示状态.
     * POST /Brand/setStatus?id=1&showStatus=0 (1=商城主页显示 0=隐藏;
     * service 用 LambdaUpdateWrapper 只更新该列, 不误伤其它字段) */
    @PostMapping("/setStatus")
    public R setStatus(Integer id, Integer showStatus) {
        brandService.setStatus(id, showStatus);
        return R.ok();
    }

    /* 3.新增品牌-POST请求.
     * POST /Brand/save (表单/查询参数: brandName/logoName/brandDesc/showStatus) */
    @PostMapping("/save")
    public R save(Brand brand) {
        //品牌名称必填(前端列表页/商品选择都要按名称展示)
        if (brand.getBrandName() == null
                || brand.getBrandName().trim().isEmpty()) {
            throw new RuntimeException("品牌名称不能为空");
        }
        brandService.addBrand(brand);
        return R.ok();
    }

    /* 4.修改品牌-PUT请求.
     * PUT /Brand/update (必须带 id; 只更新提交的非空字段) */
    @PutMapping("/update")
    public R update(Brand brand) {
        if (brand.getId() == null) {
            throw new RuntimeException("缺少品牌 id, 无法更新");
        }
        brandService.updateBrand(brand);
        return R.ok();
    }

    /* 5.按 id 查品牌详情 GET /Brand/get?id=1 */
    @GetMapping("/get")
    public R get(Integer id) {
        return R.ok( brandService.getBrand(id) );
    }

    /* 6.删除品牌-DELETE请求.
     * DELETE /Brand/delete?id=1
     * service 会先清 tbl_brand_category 里该品牌的关联, 再删品牌本体(防孤儿) */
    @DeleteMapping("/delete")
    public R delete(Integer id) {
        if (id == null) {
            throw new RuntimeException("缺少品牌 id, 无法删除");
        }
        brandService.deleteBrand(id);
        return R.ok();
    }

    /* 7.品牌 ↔ 类别 关联(勾选保存).
     * POST /Brand/associateCategory
     * JSON body 结构见 BrandCategoryVO: {brandId, categories:[{id,categoryName}], deleteIds:[...]}
     * 批次语义: 撤销 deleteIds 中已勾掉的关联, 差集补插 categories —— 由 service 完成 */
    @PostMapping("/associateCategory")
    public R associateCategory(@RequestBody BrandCategoryVO bcVO) {
        brandService.associateCategory(bcVO);
        return R.ok();
    }

    /* 8.品牌 logo 上传.
     * POST /Brand/upload (multipart 参数名固定为 file)
     * 文件名用 UUID + 原扩展名重命名(防重名/防中文名乱码), 存到 upload/brand/ 下,
     * 返回 fileName(存进 logoName 字段) + logoUrl(浏览器直接可访问的回显地址) */
    @PostMapping("/upload")
    public R upload(MultipartFile file) throws IOException {
        //1.空文件直接拒绝
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要上传的图片");
        }
        //2.生成随机文件名(UUID + 扩展名)
        String newName = makeNewName(file);
        //3.上传管道: 模板类要先用当前文件 setMultipartFile, 再 saveFile 落盘
        fileTemplate.setMultipartFile(file);
        fileTemplate.saveFile(SUB_DIR, newName);
        //4.返回新文件名 + 可直接访问的图片地址(前端同步取 logoUrl 回显)
        return R.ok().put("fileName", newName)
                     .put("logoUrl", "/Brand/showImg/" + newName);
    }

    /* 9.读取品牌图片(旧地址, 兼容保留; 与下方 showImg 等价).
     * GET /Brand/readImage?fileName=xxx.png
     * 直接输出字节流(不走 R): 先读 upload/brand/ 下文件, 文件不存在 → resp 404;
     * 否则按扩展名从 BaseController 的 MIME 表取内容类型, 写进响应头后再写字节 */
    @GetMapping("/readImage")
    public void readImage(String fileName, HttpServletResponse resp)
            throws IOException {
        //1.参数不合法 → 404
        if (fileName == null || "".equals(fileName.trim())) {
            resp.sendError(404);
            return;
        }
        byte[] data;
        try {
            //2.读文件字节(文件不存在时 FileTemplate 会抛 IOException)
            data = fileTemplate.getFile(SUB_DIR, fileName);
        } catch (IOException e) {
            //3.文件不存在/读失败 → 404, 让 <img> 显示缺省样式
            resp.sendError(404);
            return;
        }
        if (data == null) {
            resp.sendError(404);
            return;
        }
        //4.按扩展名映射 MIME 设到响应头(没匹配到就给通用二进制类型兜底)
        String mime = getMIME(fileName);
        resp.setContentType(mime == null ? "application/octet-stream" : mime);
        //5.把图片字节写到响应缓冲区
        resp.getOutputStream().write(data);
    }

    /* 9.1 读取品牌图片(RESTful 风格路由): GET /Brand/showImg/d01.png
     * 与 readImage 等价, 只是参数风格不同(路径参数 vs 查询参数);
     * 直接复用 BaseController.outFile: 读 upload/brand/ 下文件(按扩展名设 MIME),
     * 读失败 → send404File 输出 404 兜底图, 保证 <img> 不裂 */
    @GetMapping("/showImg/{fileName}")
    public void showImg(@PathVariable("fileName") String fileName,
                        HttpServletResponse resp) {
        try {
            outFile(resp, SUB_DIR, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            send404File(resp);
        }
    }

    /* 10.某品牌已关联的类别列表(编辑回显: 前端据此回打勾).
     * GET /Brand/associatedCategories?brandId=1 → List<GoodsCategory>(id/categoryName) */
    @GetMapping("/associatedCategories")
    public R associatedCategories(Integer brandId) {
        return R.ok( brandService.getAssociatedCategories(brandId) );
    }

    /* 11.按类别列出品牌(带关联标记, 关联选择页用).
     * POST /Brand/listByCategory?page=&limit=&categoryId=
     * → IPage<BrandVO>, 每条带 isAssociate(1=已关联该类别); categoryId 为空 → 全 0
     * 列表接口与关联标记在两段式 Service 层完成(分页查 + 当前页批量查关联) */
    @PostMapping("/listByCategory")
    public R listByCategory(Integer page, Integer limit, BrandSearch brandSearch) {
        IPage<BrandVO> ip = brandService.getListByCategory(
                newPage(page, limit), brandSearch);
        return R.convertPage(ip);
    }

    /* 12.按类别列品牌下拉(商品上架第 1 步的"品牌"下拉框专用).
     * GET /Brand/brandOptions/{categoryId} → [{value,brandId, label:brandName}]
     * 只列"该类别已关联"的品牌, 数据源是中间表 tbl_brand_category(见 OptionMapper.brandOptions) */
    @GetMapping("/brandOptions/{categoryId}")
    public R brandOptions(@PathVariable("categoryId") Integer categoryId) {
        return R.ok( optionMapper.brandOptions(categoryId) );
    }

    /*
     * 生成一个随机文件名: UUID + 原扩展名(如 d01.png → UUID.png)。
     * 目的: 避免用户上传同名文件互相覆盖, 也避免中文名/特殊字符落盘。
     */
    private String makeNewName(MultipartFile mFile) {
        //1.生成一个 UUID
        String uuid = FileUtils.makeUUID();
        //2.拿到原始文件名(个别浏览器不传文件名 → 兜底不加扩展名)
        String fileName = mFile.getOriginalFilename();
        //3.取其扩展名(含点, 如 ".png")
        String extName = (fileName == null) ? ""
                : FileUtils.extName(fileName);
        //4.返回新文件名
        return uuid + extName;
    }

    /* 统一异常处理(与 DeptController 同一写法): 业务错误以 R 形式写回 */
    @ExceptionHandler
    public void exceptionHandler(Exception e, HttpServletResponse resp) {
        e.printStackTrace();
        R.err(e).write(resp);
    }

    /* 把注入的文件模板交给父类 outFile/send404File 使用(本模块图片回显必需) */
    @Override
    protected FileTemplate getFileTemplate() {
        return fileTemplate;
    }
}
