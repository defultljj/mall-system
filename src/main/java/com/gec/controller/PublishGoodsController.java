package com.gec.controller;

import com.gec.components.FileTemplate;
import com.gec.domain.entity.SpuDetail;
import com.gec.domain.vo.GoodsAttrValuesVO;
import com.gec.domain.vo.GoodsBaseInfoVO;
import com.gec.domain.vo.SkuValuesVO;
import com.gec.service.IGoodsDetailService;
import com.gec.service.ISpuDetailService;
import com.gec.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/*
 * 商品上架(第 1 步: 基本信息 + 图片上传/回显)。
 *   showImg           GET    /PublishGoods/showImg/{dir}/{imgName}   读本地图片字节流(失败输出 404 兜底图)
 *   upload            POST   /PublishGoods/upload (file + type)       图片上传: type=1 主图→goods, 其它→album
 *   saveGoodsBaseInfo POST   /PublishGoods/saveGoodsBaseInfo          第 1 步基本信息写 Redis 暂存(主流程)
 *   saveGoodsAttrValues POST /PublishGoods/saveGoodsAttrValues        第 2 步规格参数写 Redis 暂存(9.12 主流程)
 *   saveSaleAttrValues POST  /PublishGoods/saveSaleAttrValues         第 3 步销售属性写 Redis 暂存(9.12 补全)
 *   saveSkuValues     POST   /PublishGoods/saveSkuValues              第 4 步 SKU 列表写 Redis 暂存(9.12 补全)
 *   submitPublish     POST   /PublishGoods/submitPublish              第 5 步 保存完成: 四段缓存一次性落库并清缓存(9.12 补全)
 *   saveBaseInfo      POST   /PublishGoods/saveBaseInfo               第 1 步基本信息直接落库(旧接口, 保留备用)
 *   get               GET    /PublishGoods/get?id=                    第 1 步回显
 * 业务错误统一 @ExceptionHandler 以 R 形式写回; 图片读取直接写字节流, 不走 R。
 * 路由已被 WebConfig 的登录拦截器覆盖(/PublishGoods/** 需登录)。
 */
@RestController
@RequestMapping("/PublishGoods")
public class PublishGoodsController extends BaseController {

    /* 6.文件上传。*/
    @Autowired
    private FileTemplate fileTemplate;
    /* 第 1 步基本信息服务(落库/回显) */
    @Autowired
    private ISpuDetailService spuDetailService;
    /* 第 1 步基本信息服务(Redis 暂存; 9.11 资料新增, 发布主流程走这里) */
    @Autowired
    private IGoodsDetailService detailService;
    /* 7.1.声明商品主图片上传的目录。*/
    private String MAIN_DIR = "goods";
    /* 7.2.声明商品图集上传的目录。*/
    private String ALBUM_DIR = "album";

    @Override
    protected FileTemplate getFileTemplate() {
        return fileTemplate;
    }

    /*
    *   此方法用于读取本地的一张图片。
    *   把数据输出到请求端（浏览器）。
    *   浏览器即能展示出来。
    *   http://localhost:8080/PublishGoods/showImg/goods/xxx.png
    *   路径参数: {dir}=goods(主图)|album(图集), {imgName}=上传后返回的随机文件名。
    *   读不到文件(不存在/读盘失败) → 输出 404 兜底图(send404File 内含二次兜底),
    *   保证前端 <img> 不会因为一个坏地址而弹出 JSON 异常页。
    */
    @GetMapping("/showImg/{dir}/{imgName}")
    public void showImg(@PathVariable("dir") String dir,
                        @PathVariable("imgName") String imgName,
                        HttpServletResponse resp) {
        try {
            //读文件字节并写响应流(Content-Type 由扩展名映射, 见 BaseController.outFile)
            outFile(resp, dir, imgName);
        } catch (Exception e) {
            e.printStackTrace();
            send404File(resp);
        }
    }

    /*
    * upload() 文件上传的方法。
    * POST /PublishGoods/upload (multipart 参数名固定为 file)
    * type=1 → 主图, 存 upload/goods/ 目录; 其它(如 2=图集) → 存 upload/album/ 目录。
    * 文件名用 UUID + 原扩展名重命名(防重名覆盖/中文名乱码),
    * 返回: fileName=落盘随机名, logoUrl=浏览器可直接访问的回显地址。
    */
    @PostMapping("/upload")
    public R upload(@RequestParam("file") MultipartFile mFile,
                    @RequestParam("type") Integer type) {
        //1.空校验(前端没选文件/传了空文件, 直接拒绝)
        if (mFile == null || mFile.isEmpty()) {
            throw new RuntimeException("文件不能为空。");
        }
        //2.按 type 选目录(原实现 type.equals(1) 在 type 为 null 时会 NPE, 这里短路防御)
        String dir = (type != null && type == 1) ? MAIN_DIR : ALBUM_DIR;
        //3.上传管道: 先挂当前文件, 再 saveFile 落盘
        fileTemplate.setMultipartFile(mFile);
        //4.生成随机文件名, 拼出浏览器可直接访问的回显地址
        String newName = makeNewName(mFile);
        String logoUrl = "/PublishGoods/showImg/" + dir + "/" + newName;
        //5.落盘; 失败统一转成业务异常(交给 @ExceptionHandler 以 R 形式返回)
        try {
            fileTemplate.saveFile(dir, newName);
            return R.ok().put("logoUrl", logoUrl).put("fileName", newName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("上传文件失败。");
        }
    }

    /*
    * 第 1 步: 基本信息写 Redis 暂存(9.11 参考方案, 发布商品主流程走这里)。
    * POST /PublishGoods/saveGoodsBaseInfo  (请求体 JSON)
    * 入参 GoodsBaseInfoVO = {goodsDetail, pointRule, spuAlbumVO} 三块一起打包。
    * 返回: pubKey —— 本次发布的 16 位流水号, 后续 3 步(规格属性/销售属性/SKU)
    *       都带着它来, 以 "pubKey-后缀" 的 KEY 把同一商品的缓存片段串起来。
    * 注意: 只写 Redis, 不落库; 落库等发布流程最后一步统一做。
    */
    @PostMapping("/saveGoodsBaseInfo")
    public R saveGoodsBaseInfo(@RequestBody GoodsBaseInfoVO biVO) {
        String pubKey = detailService.saveBaseInfoCache(biVO);
        return R.ok().put("pubKey", pubKey);
    }

    /*
    * 第 1 步: 保存基本信息(表单提交, 直接落库)。
    * POST /PublishGoods/saveBaseInfo
    * 表单字段: id(可选)/goodsName/goodsDetails/categoryId/brandId/weight/mainImg/allowDeduct/rewardPoint。
    * id 为空 → 新增(自增 id 回填); id 有值 → 更新。
    * 返回实体: 新增时前端从 data.id 拿回填的主键, 供"下一步"继续提交。
    * [保留说明] 9.11 方案已改为 Redis 暂存(上面的 saveGoodsBaseInfo),
    *            本接口是"直接落库"的旧通道, 保留作本地调试/异常兜底, 不参与新主流程。
    */
    @PostMapping("/saveBaseInfo")
    public R saveBaseInfo(SpuDetail spu) {
        spuDetailService.saveBaseInfo(spu);
        return R.ok(spu);
    }

    /*
    * 第 1 步: 按 id 回显商品基本信息(编辑/下一步页面回填表单)。
    * GET /PublishGoods/get?id=1
    */
    @GetMapping("/get")
    public R get(Integer id) {
        return R.ok( spuDetailService.getById(id) );
    }

    /*
    * 第 2 步: 规格参数写 Redis 暂存(9.12 参考方案)。
    * POST /PublishGoods/saveGoodsAttrValues  (请求体 JSON)
    * 入参 GoodsAttrValuesVO = {pubKey, attrList}:
    *        pubKey   第 1 步返回的流水号, 前端原样带回来;
    *        attrList 用户在"规格参数"页勾选的属性值列表。
    * 返回: pubKey —— 原样带回, 供第 3 步(销售属性)继续使用。
    * 注意: 只写 Redis, 不落库; pubKey 缺失由 service 抛业务异常, 统一走 @ExceptionHandler。
    */
    @PostMapping("/saveGoodsAttrValues")
    public R saveGoodsAttrValues(@RequestBody GoodsAttrValuesVO gavVO) {
        String pubKey = detailService.saveGoodsAttrValCache(gavVO);
        return R.ok().put("pubKey", pubKey);
    }

    /*
    * 第 3 步: 销售属性写 Redis 暂存(9.12 补全, 请求契约与第 2 步相同)。
    * POST /PublishGoods/saveSaleAttrValues  (请求体 JSON)
    * 入参 GoodsAttrValuesVO = {pubKey, attrList}:
    *        pubKey   第 1 步返回的流水号, 前端原样带回来;
    *        attrList 用户在"销售属性"页勾选的属性值列表。
    * 返回: pubKey —— 原样带回, 供第 4 步(SKU 设置)继续使用。
    * 注意: 只写 Redis, 不落库; service 按 "pubKey-sale-attr" 落缓存;
    *       pubKey 缺失由 service 抛业务异常, 统一走 @ExceptionHandler。
    */
    @PostMapping("/saveSaleAttrValues")
    public R saveSaleAttrValues(@RequestBody GoodsAttrValuesVO gavVO) {
        String pubKey = detailService.saveSaleAttrValCache(gavVO);
        return R.ok().put("pubKey", pubKey);
    }

    /*
    * 第 4 步: SKU 列表写 Redis 暂存(9.12 补全)。
    * POST /PublishGoods/saveSkuValues  (请求体 JSON)
    * 入参 SkuValuesVO = {pubKey, skuList}:
    *        pubKey  第 1 步返回的流水号, 前端原样带回来;
    *        skuList 前端按销售属性组合生成的 SKU 行列表
    *                (skuName 组合串/price 售价/stock 库存/skuImg 图片/attrValues 属性明细)。
    * 返回: pubKey —— 原样带回(落库接口由下一批次补, 约定不变)。
    * 注意: 只写 Redis, 不落库; service 按 "pubKey-sku" 落缓存;
    *       pubKey 缺失由 service 抛业务异常, 统一走 @ExceptionHandler。
    */
    @PostMapping("/saveSkuValues")
    public R saveSkuValues(@RequestBody SkuValuesVO skuVO) {
        String pubKey = detailService.saveSkuValCache(skuVO);
        return R.ok().put("pubKey", pubKey);
    }

    /*
    * 第 5 步: 保存完成 —— 把四段 Redis 缓存一次性落库并清缓存(9.12 补全, 发布链路终点)。
    * POST /PublishGoods/submitPublish (表单参数 pubKey)
    * 入参: pubKey —— 第 1 步返回、前端一路带过来的发布流水号。
    * 返回: R.ok().put("spuId", 新商品 id) —— spuId 在响应顶层, 与前四步的 pubKey 同一风格。
    * 注意: 缓存过期/缺失由 service 抛业务异常, 统一走 @ExceptionHandler;
    *       落库失败时缓存不会被清, 前端保留 localStorage 三键可原地重试;
    *       整个落库过程在一个事务里(事务边界在 service, 与项目既有约定一致)。
    */
    @PostMapping("/submitPublish")
    public R submitPublish(@RequestParam("pubKey") String pubKey) {
        Integer spuId = detailService.submitPublish(pubKey);
        return R.ok().put("spuId", spuId);
    }

    /*
    * 此方法用于产生一个随机文件名。
    * 目的: 避免用户上传同名文件互相覆盖, 也避免中文名/特殊字符落盘
    * (与 BrandController.makeNewName 同一写法)。
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
}
