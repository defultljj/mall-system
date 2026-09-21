/**
 * ============================================================
 * 商城后台管理系统 mall-sys —— 页面逻辑（纯原生 JS）
 * 后端来源：SpringBoot 项目（src/main/resources/static）
 *
 * 统一返回格式 R（JSON）：
 *   成功: { result:"success", data:..., total:...n, status:200 }   total 仅分页接口有
 *   失败: { result:"failed",  cause:"提示文字", status:500 }
 *   重要：失败时 HTTP 状态码也可能就是 500，判断一律解析 JSON 看 result 字段。
 *
 * 会话（Cookie 同源自动携带，前端不存 token）：
 *   GET  /Auth/check     data 非空 = 已登录（页面启动先调它）；data=null = 未登录
 *   POST /Auth/login     account/password → data=登录用户 {id,account,nickName,roleName}
 *   POST /Auth/register  account/nickName/password/phone/sex/email/deptId（默认分配部门职员角色）
 *   POST /Auth/logout    退出
 *   重要：除 /Auth 外全部业务接口要求登录，未登录时后端返回 HTTP 401，
 *        api() 封装统一处理：清登录态、切登录视图、toast「请先登录」（见下方 api）。
 *
 * 接口契约（表单绑定，POST/PUT 必须发 application/x-www-form-urlencoded）：
 *   GET    /User/list     page/limit/account/nickName/phone/deptId   分页联表查询
 *   POST   /User/save     account/nickName/phone/sex/no/email/deptId + roleId   新增并分配角色
 *   PUT    /User/update   同 save 另加 id（roleId 缺省 = 清空角色）
 *   GET    /User/get?id   详情（User 实体，无 roleId —— 编辑回显一律用列表行数据）
 *   DELETE /User/delete?id
 *   GET    /Dept/list      树形数组 { id,deptName,deptDesc,parentId,level,hasSub,pids,children? }
 *   POST   /Dept/save      deptName/deptDesc/parentId（parentId 缺省或 0 = 顶层）
 *   PUT    /Dept/update    同 save 另加 id（改父部门后端会级联重算 pids）
 *   GET    /Dept/get?id    详情
 *   DELETE /Dept/delete?id  （有用户/子部门时后端失败，cause 直接展示）
 *   GET    /Role/list      page/limit/roleName    分页
 *   POST   /Role/save      roleName/descript
 *   PUT    /Role/update    同 save 另加 id
 *   DELETE /Role/delete?id
 *   GET    /Role/roleOptions   [{value,label},...]，不含 id=1 系统管理员
 *
 *   POST   /Brand/list      page/limit/brandName/categoryId(categoryId 缺省=全部类别)  分页
 *                         → data 行 { id,brandName,logoName,brandDesc,showStatus,createDate }，total 分页总数
 *   POST   /Brand/setStatus id/showStatus(1显示0隐藏)   状态行内开关调用
 *   POST   /Brand/save      brandName(必填)/logoName/brandDesc/showStatus
 *   PUT    /Brand/update    同 save 另加 id（编辑态“移除 LOGO 后保存”= 显式传空串 logoName，走 opts.noClean）
 *   DELETE /Brand/delete?id
 *   POST   /Brand/upload    multipart/form-data，字段名 file（uploadFile 封装）
 *                         → data={ fileName, logoUrl }（契约升级：原为“文件名字符串”）
 *   GET    /Brand/showImg/{logoName}   图片字节流（<img src> 同源直连带 cookie；404/失败时 onerror 换占位）
 *                         （原 GET /Brand/readImage?fileName= 已废弃，前端不再引用）
 *   GET    /Brand/brandOptions/{categoryId}   data=[{value,label},...] 发布商品「选择品牌」下拉数据源
 *                         （讲师逻辑：必须先确认三级类别，才可调用）
 *   GET    /Brand/associatedCategories?brandId=   data=该品牌已关联类别（树结构，含 children 嵌套）
 *   POST   /Brand/associateCategory  JSON body（apiJson 封装）：
 *                          { brandId, brandName, categories:[{id,categoryName}], deleteIds:[...] }
 *   POST   /Brand/listByCategory  page/limit/categoryId（行含 isAssociate，可选调用，本次未用）
 *
 *   GET    /Category/list   树形数组（结构与 /Dept/list 相同）
 *                           { id,categoryName,parentId,children?,level,hasSub,pids,showStatus,sort,icons,productUnit,productCount }
 *   POST   /Category/save   categoryName(必填)/parentId(缺省=顶层)/showStatus(1上架0下架,缺省1)/sort/icons/productUnit/productCount
 *   PUT    /Category/update 同 save 另加 id
 *   GET    /Category/get?id 详情
 *   DELETE /Category/delete?id  （有子类别时后端拒绝，cause 直接展示）
 *   GET    /Category/associatedBrands?categoryId=   data=已关联品牌 [{id,brandName},...]
 *   POST   /Category/associateBrand  JSON body（apiJson 封装）：
 *                          { categoryId, categoryName, brands:[{id,brandName}], deleteIds:[...] }
 *
 *   GET    /AttrGroup/list  page/limit/categoryId(categoryId 缺省=全部)  分页
 *                         → data 行 { id,groupName,categoryId,descript,icons,sort,createDate }
 *   POST   /AttrGroup/save  groupName(必填)/categoryId/descript/icons/sort
 *   PUT    /AttrGroup/update 同 save 另加 id
 *   DELETE /AttrGroup/delete?id
 *
 *   POST   /PublishGoods/upload   multipart/form-data：file + type（1=主图）
 *                         → data={ fileName, logoUrl }
 *   GET    /PublishGoods/showImg/goods/{fileName}   主图字节流（<img src> 直连；404 时后端返回 404 图）
 *   POST   /PublishGoods/saveGoodsBaseInfo   JSON body（apiJson 封装，讲师 Redis 暂存方案）：
 *                         { goodsDetail:{ goodsName, goodsDetails?, categoryId, mainImage?, brandId?, weight? },
 *                           pointRule:{ allowDeduct?, rewardPoint? }, spuAlbumVO:{ images:[] } }
 *                         → data={ pubKey }（16 位大写发布键）；前端存 localStorage.pubKey 供第 2 步续传
 *                         字段名注意：goodsDetail 单数（讲师实体 GoodsDetail）、mainImage（非 mainImg）；
 *                         weight/mainImage/brandId 可空；pointRule 为空传 null（后端取默认 150/100）
 *                         （原 POST /PublishGoods/saveBaseInfo 表单直落库：后端保留，前端不再调用）
 *   GET    /PublishGoods/get?id=   基础信息回显（本轮无入口，未接入）
 *
 * 商品上架第 2 步「设置商品属性 / 规格参数」（9.12 资料）：
 *   GET    /GoodsAttr/listByCategory/{categoryId}/{attrType}
 *                         按类别 + 属性类型取属性列表（attrType 固定 1 = 规格参数/基本属性）
 *                         → data=[{ id, attrName, categoryId, attrType, valueType, attrValue, ... }]
 *                         valueType：1=单值（input + datalist）| 2=多值（标签多选）
 *                         attrValue：候选项文本，多值型用 ';' 拼接（讲师答案：前端 split(';') 得 valueArr）
 *   POST   /PublishGoods/saveGoodsAttrValues   JSON body（apiJson 封装，讲师 Redis 暂存方案·第 2 步）：
 *                         { pubKey, attrList:[{ attrId, valueType, attrValue }] }
 *                         多值型 attrValue 由前端 valueArr.join(';') 回字符串；空值行前端已过滤
 *                         → R.ok().put("pubKey", pubKey)：**pubKey 在响应顶层**（与第 1 步一致）
 *
 * 商品上架第 3 步「销售属性」（本轮补齐：课程资料未覆盖，需求方自主决策，沿用第 2 步模式）：
 *   GET    /GoodsAttr/listByCategory/{categoryId}/2   同第 2 步接口，attrType 固定 2 = 销售属性
 *   POST   /PublishGoods/saveSaleAttrValues   JSON body：{ pubKey, attrList:[{ attrId, valueType, attrValue }] }
 *                         字段与第 2 步 saveGoodsAttrValues 完全一致（同一套前端引擎）；
 *                         成功后前端另存 localStorage.saleAttr（[{attrId,attrName,valueType,values:[...]}]）供第 4 步续用
 *
 * 商品上架第 4 步「SKU 设置」（本轮补齐，同上）：
 *   POST   /PublishGoods/saveSkuValues   JSON body：
 *                         { pubKey, skuList:[{ skuName, price, stock, skuImg, attrValues:[{ attrId, attrValue }] }] }
 *                         skuName = 该行各销售属性值按 ';' 连接（前端自动生成）；skuImg 本轮固定 null（图集属第 5 步）
 *                         price/stock 均由输入框取 Number（价格 >0、库存 ≥0，前端校验不通过不发请求）
 *                         数据源 = 第 3 步销售属性的值组合（笛卡尔积），一行一个 SKU
 *
 * 商品上架第 5 步「保存完成」（本轮补齐，同上）：
 *   POST   /PublishGoods/submitPublish   表单 pubKey：把 Redis 四段缓存（基本信息 / 规格参数 / 销售属性 / SKU）
 *                        一次性落库并清缓存 → R.ok().put("spuId", ...)：**spuId 在响应顶层**（同前四步的 pubKey）
 *                         成功后前端清 localStorage 的 pubKey/categoryId/saleAttr（发布完成）并切「商品管理」
 *
 * 商品管理（商品列表，本轮补齐：发布链路的最终闭环）：
 *   GET    /Goods/list    page/limit/goodsName   分页（后端 R.convertPage）
 *                         → data 行 { id, goodsName, goodsDetails, categoryId, categoryName, brandId,
 *                                    brandName, weight, mainImg, allowDeduct, rewardPoint, createDate }
 *                         （categoryName/brandName 后端已联表返回；前端仅在缺失时用类别树兜底映射）
 *   GET    /Goods/get?id= → data { spu:{ 同上行字段 },
 *                                  attrValues:[{ attrId, attrValue, valueType, attrName, attrType }],
 *                                  skus:[{ sku:{ id,spuId,skuName,price,stock,skuImg },
 *                                          attrs:[{ id,spuId,skuId,attrId,attrValue }] }] }
 *                         attrType：1=规格参数（基本属性）| 2=销售属性 —— 详情弹窗据此分两块展示
 *   DELETE /Goods/delete?id=   级联删除（后端一并清掉属性值与 SKU）
 *
 * 经营概览（Dashboard）数据来源：
 *   GET /Analysis/overview，统一从订单、会员、库存、商品表联合统计。
 *
 * 业务注意：
 *   - 用户 sex 列是 enum('男','女')，只允许这两个值
 *   - 编辑用户回显用列表行数据（行里有 roleId/deptId），不要用 /User/get
 *   - 部门/类别数据一次加载进内存，展开/收起只做 DOM 切换，不重复请求
 *   - 未登录时不加载任何业务数据；/Auth/check 成功（或登录成功）后才初次加载
 *   - 退出登录 / 会话失效（401）后清空视图数据，避免下次登录串台
 *   - 发布商品（第 1 步）：三级类别必须选满才算确认（categoryId = 第三级 id），
 *     确认后才加载品牌下拉（品牌可空）；主图未上传成功时「下一步」按钮禁用（讲师要求）
 *   - 发布商品（第 1 步提交）：整单 JSON POST /PublishGoods/saveGoodsBaseInfo 暂存 Redis（不落库），
 *     成功后把返回的 pubKey 与 categoryId 写入 localStorage（跨步骤续传），并自动切到第 2 步视图；
 *     「重置表单」不清 pubKey（讲师行为）
 *   - 发布商品（第 2 步）：类别 id 优先取内存 publishCategoryId、其次 localStorage.categoryId（兜底），
 *     二者都没有则提示「请先完成第 1 步」并禁用提交；属性列表扁平展示不分组
 *   - 发布商品（第 3 步）：与第 2 步共用同一套属性表单引擎（只换容器/前缀/attrType/文案）；
 *     保存成功后把「已填值」的销售属性写入 localStorage.saleAttr 并自动进入第 4 步
 *   - 发布商品（第 4 步）：SKU 行 = 销售属性值的笛卡尔积；价格为必填 >0、库存为必填 ≥0，校验不过不发请求；
 *     保存成功后自动进入第 5 步「保存完成」
 *   - 发布商品（第 5 步）：汇总卡片的数据源是 localStorage 三键（内存态不参与），进入视图确保 ensureCat
 *     以便把 categoryId 映射成类别名；发布成功才清 localStorage 三键（失败保留，可原地重试）；
 *     「返回上一步」回第 4 步（暂存键仍在，可改价格/库存后重新提交）
 *   - 商品管理：「查看」走 GET /Goods/get 实时取详情（不用列表行渲染，保证属性/SKU 最新）；
 *     删除是级联操作（后端连属性值与 SKU 一起删），故前端做 confirm 二次确认
 * ============================================================
 */
'use strict';

/* ==================== 常量与全局状态 ==================== */
const PAGE_SIZE = 10;                       // 分页每页条数（用户/角色/品牌共用）
const $ = (id) => document.getElementById(id);

// 当前登录用户：{ id, account, nickName, roleName }，null = 未登录
let currentUser = null;
// 401 会话失效已处理标记：置真后业务层 notifyError 不再重复弹提示，重新登录（enterApp）时复位
let sessionKicked = false;

let uPage = 1, uTotal = 0, uRows = [];      // 用户列表：当前页/总数/当前页行数据
let rPage = 1, rTotal = 0, rRows = [];      // 角色列表：同上
let bPage = 1, bTotal = 0, bRows = [];      // 品牌列表：同上
let agPage = 1, agTotal = 0, agRows = [];   // 属性组列表：当前页/总数/当前页行数据
let gPage = 1, gTotal = 0, gRows = [];      // 商品列表：当前页/总数/当前页行数据
let goodsDetailSkus = [];
let goodsEditData = null;
let goodsEditMainImg = '';
let deptCache = { raw: null, flat: [], byId: {} }; // 部门整树缓存（一次请求）
let roleOptionsCache = null;                // /Role/roleOptions 下拉缓存
let collapsedDeptIds = new Set();           // 已折叠的部门 id（空 = 全部展开）
let catCache = { raw: null, flat: [], byId: {} }; // 类别整树缓存（结构与部门一致，一次请求）
let collapsedCatIds = new Set();            // 已折叠的类别 id（空 = 全部展开）

// 关联弹窗状态（由 openBrandAssoc / openCatAssoc 填充，保存差分提交时读取）：
//   bAssoc: 品牌→类别 —— brandId/brandName/oldIds(原已勾选类别 id)
//   cAssoc: 类别→品牌 —— categoryId/categoryName/oldIds(原已勾选品牌 id)/all(全量品牌行)
let bAssoc = null;
let cAssoc = null;

// 品牌 LOGO 上传态：
//   brandOrigLogo: 打开弹窗时的原文件名（“编辑态移除后保存”= 显式空串提交，通知后端清空）
//   bmObjectUrl:   上传后本地预览 objectURL（再次上传/移除/重开弹窗时 revoke，防内存泄漏）
let brandOrigLogo = '';
let bmObjectUrl = null;

// 发布商品（第 1 步「基本信息」）状态：
//   publishCategoryId: 已确认的三级类别 id（'' = 未确认 → 品牌下拉禁用）
//   publishBrandFor:   品牌下拉当前已加载的 categoryId（相同则不重复请求）
//   publishBrandSeq:   品牌请求序号（级联快速切换时丢弃过期响应）
//   publishMainImg:    已上传主图文件名（回填隐藏字段 mainImg 随表单提交）
//   publishMainImgUrl: 主图本地预览 objectURL（替换/移除/清空时 revoke，防内存泄漏）
//   publishingImg:     主图上传中（禁用上传方块与「下一步」）
let publishCategoryId = '';
let publishBrandFor = '';
let publishBrandSeq = 0;
let publishMainImg = '';
let publishMainImgUrl = null;
let publishingImg = false;

// 发布商品（第 2/3 步「属性表单」）状态：见 createAttrFormCtx —— 第 2 步规格参数（attrType=1）与
// 第 3 步销售属性（attrType=2）各持一份上下文（goodsAttrForm / saleAttrForm），状态互不干扰

// 发布商品（第 3 步「销售属性」）状态：
//   saleAttrRows: 已填值的销售属性行 [{attrId,attrName,valueType,values:[...]}]（空值属性已排除）
//                 第 4 步 SKU 笛卡尔积的数据源；同时镜像到 localStorage.saleAttr（刷新兜底）
let saleAttrRows = [];

// 发布商品（第 4 步「SKU 设置」）状态：
//   skuDims:      当前 SKU 表格的销售属性维度 [{attrId,attrName,values}]（来自 saleAttrRows）
//   skuRows:      SKU 组合行 [{skuName,values,attrValues,price,stock}]（price/stock 为输入框字符串）
//   skuDimSig:    维度指纹：重进视图时与当前维度比对，一致则保留已填价格/库存、不重建组合
//   skuSubmitting: 提交中（禁用「保存 SKU」）
let skuDims = [];
let skuRows = [];
let skuDimSig = '';
let skuSubmitting = false;

// 发布商品（第 5 步「保存完成」）状态：
//   finishSubmitting: 发布请求进行中（禁用「确认发布」防重入）；
//   汇总卡片不走内存态 —— 发布键/类别/销售属性一律现读 localStorage（刷新后仍能正确回显）
let finishSubmitting = false;

// 数据概览（Dashboard）状态：
//   dashCharts: 图表容器 id → ECharts 实例（隐藏容器 init 会得到 0×0 尺寸，故首次切到本视图才 init）
let dashCharts = {};

/* ==================== 基础工具 ==================== */

// HTML 转义：所有来自后端或用户输入的值写入 innerHTML 前必须经过，防 XSS
function esc(v) {
  return String(v == null ? '' : v).replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
  ));
}

// 右上角浮动提示：成功绿色 / 失败红色，2.5 秒后自动消失
function toast(msg, isOk = true) {
  const box = $('toastBox');
  const t = document.createElement('div');
  t.className = 'toast ' + (isOk ? 'toast-ok' : 'toast-err');
  t.textContent = msg;
  box.appendChild(t);
  setTimeout(() => {
    t.classList.add('toast-out');
    setTimeout(() => t.remove(), 300);
  }, 2500);
}

// 业务层错误提示统一入口：会话已被 401 踢出时（界面已切登录页并提示过一次），静默避免刷屏
function notifyError(e) {
  if (sessionKicked) return;
  toast(e.message, false);
}

// 剔除空参数：空串 / null / undefined 不拼进请求
function cleanParams(obj) {
  const out = {};
  for (const k of Object.keys(obj)) {
    const v = obj[k];
    if (v !== '' && v != null) out[k] = v;
  }
  return out;
}

/**
 * 统一请求封装 api(url, method, params, opts)
 *  GET/DELETE：参数拼在 query（删除发 ?id=，不带 body）
 *  POST/PUT：URLSearchParams 作为 body + x-www-form-urlencoded，与后端表单绑定匹配
 *  opts.authRedirect=false：该请求允许未登录（401 时不切登录页，只抛错），
 *                           仅注册页拉取部门下拉使用（业务接口未登录会 401）
 *  opts.noClean=true：body 不剔除空串参数（默认 cleanParams 会剔除）。用途：
 *                     品牌“移除 LOGO 后保存”需显式把 logoName='' 提交给后端清空旧文件
 * 返回值：解析后的完整 R 对象（仅当 result==='success' 才正常返回）
 * 抛错：网络失败 / 响应非 JSON / HTTP 401 / result==='failed'（message 取 cause）
 * 401 处理：清当前登录态、关闭弹窗、切回登录视图、toast「请先登录」
 */
async function api(url, method = 'GET', params = {}, opts = {}) {
  const hasBody = (method === 'POST' || method === 'PUT');
  const fopts = { method, headers: {} };
  let full = url;
  if (hasBody) {
    fopts.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
    const p = opts.noClean ? params : cleanParams(params);
    fopts.body = new URLSearchParams(p).toString();
  } else {
    const q = new URLSearchParams(cleanParams(params)).toString();
    if (q) full += '?' + q;
  }
  let resp;
  try {
    resp = await fetch(full, fopts);
  } catch (e) {
    throw new Error('网络请求失败，请确认后端服务已启动');
  }
  let json = null;
  try {
    json = await resp.json();   // 即使 HTTP 500 也要尝试解析 JSON（以 result 字段为准）
  } catch (e) { /* 非 JSON 响应（如纯文本错误页），以状态码为准 */ }
  if (resp.status === 401) {                    // 会话失效 / 未登录：业务接口的 401 必须处理
    if (opts.authRedirect !== false) kickToLogin();
    throw new Error((json && (json.cause || json.message)) || '未登录或登录已过期，请重新登录');
  }
  if (!json) throw new Error('服务响应异常（HTTP ' + resp.status + '）');
  if (json && json.result === 'success') return json;
  throw new Error((json && json.cause) || '操作失败（HTTP ' + resp.status + '）');
}

/**
 * apiJson(url, params) —— 关联保存类接口专用（品牌/类别互相关联，body 必须是 JSON）
 * 与 api() 行为一致：result==='success' 判定、401 踢登录、错误信息取 cause；
 * 区别仅在 POST + application/json，不做空参剔除（deleteIds 空数组也要原样提交）。
 */
async function apiJson(url, params) {
  let resp;
  try {
    resp = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params)
    });
  } catch (e) {
    throw new Error('网络请求失败，请确认后端服务已启动');
  }
  let json = null;
  try {
    json = await resp.json();
  } catch (e) { /* 非 JSON 响应以状态码为准 */ }
  if (resp.status === 401) {
    kickToLogin();
    throw new Error((json && (json.cause || json.message)) || '未登录或登录已过期，请重新登录');
  }
  if (!json) throw new Error('服务响应异常（HTTP ' + resp.status + '）');
  if (json && json.result === 'success') return json;
  throw new Error((json && json.cause) || '操作失败（HTTP ' + resp.status + '）');
}

/**
 * uploadFile(url, file, extra) —— 图片上传专用（品牌 LOGO / 商品主图共用）
 * api() 只能发 URLSearchParams 表单，上传必须 multipart/form-data：
 *   fetch 直接传 FormData，Content-Type 交给浏览器自动带 boundary（不可手写）。
 * extra：附加表单字段（逐个 append），如商品主图 { type: 1 }；不传则只提交 file
 * 返回：整个响应 json —— 品牌/商品上传契约把 fileName/logoUrl 放在**顶层键**
 *   （后端 R.ok().put("fileName",...).put("logoUrl",...)，即 {result,fileName,logoUrl,status}），
 *   调用方取 resp.fileName 使用。（与 api()/apiJson() 返回整个 json 的约定一致）
 * 失败/401 处理与 api() 一致。
 */
async function uploadFile(url, file, extra) {
  const fd = new FormData();
  fd.append('file', file);                    // 后端契约：multipart 字段名固定为 file
  if (extra) {                                // 附加字段（如 type=1 表示商品主图）
    for (const k of Object.keys(extra)) {
      if (extra[k] != null) fd.append(k, extra[k]);
    }
  }
  let resp;
  try {
    resp = await fetch(url, { method: 'POST', body: fd });
  } catch (e) {
    throw new Error('网络请求失败，请确认后端服务已启动');
  }
  let json = null;
  try {
    json = await resp.json();
  } catch (e) { /* 非 JSON 响应以状态码为准 */ }
  if (resp.status === 401) {
    kickToLogin();
    throw new Error((json && (json.cause || json.message)) || '未登录或登录已过期，请重新登录');
  }
  if (!json) throw new Error('服务响应异常（HTTP ' + resp.status + '）');
  if (json && json.result === 'success') return json;   // 顶层键含 fileName/logoUrl（见函数注释）
  throw new Error((json && json.cause) || '上传失败（HTTP ' + resp.status + '）');
}

// 会话失效（任意业务接口返回 401）统一入口：只处理一次，切回登录视图
function kickToLogin() {
  if (sessionKicked) return;
  sessionKicked = true;
  showLoginView();
  toast('请先登录', false);
}

/* ==================== 弹窗通用 ==================== */

const MODAL_IDS = ['userModal', 'deptModal', 'roleModal', 'brandModal', 'categoryModal',
  'brandAssocModal', 'catAssocModal', 'attrGroupModal', 'goodsModal', 'goodsEditModal', 'orderDetailModal', 'operationModal'];

function openModal(id) { $(id).hidden = false; }
function closeModal(id) { $(id).hidden = true; }

function closeAllModals() { MODAL_IDS.forEach((id) => { const m = $(id); if (m) m.hidden = true; }); }

function closeTopModal() {
  const open = MODAL_IDS.find((id) => $(id) && !$(id).hidden);
  if (open) closeModal(open);
}

/* ==================== 登录 / 注册 与 会话状态机 ==================== */

// 顶部栏展示登录用户：昵称（缺省显示账号）+ 角色徽标
function setAuthUi(user) {
  const nick = user.nickName || user.account || '';
  $('loginUserNick').textContent = nick;
  $('loginUserRole').textContent = user.roleName || '—';
  $('loginUserNick').title = '账号：' + (user.account || '') +
    (user.id != null ? '（ID ' + user.id + '）' : '');
  $('loginUserRole').title = '当前角色';
}

// 清空各视图内存缓存 / 列表 / 搜索框，防止退出登录后残留数据串台
function clearViewData() {
  uPage = 1; uTotal = 0; uRows = [];
  rPage = 1; rTotal = 0; rRows = [];
  bPage = 1; bTotal = 0; bRows = [];
  agPage = 1; agTotal = 0; agRows = [];
  gPage = 1; gTotal = 0; gRows = [];
  deptCache = { raw: null, flat: [], byId: {} };
  roleOptionsCache = null;
  collapsedDeptIds = new Set();
  catCache = { raw: null, flat: [], byId: {} };
  collapsedCatIds = new Set();
  bAssoc = null;
  cAssoc = null;
  brandOrigLogo = '';
  if (bmObjectUrl) { URL.revokeObjectURL(bmObjectUrl); bmObjectUrl = null; }  // 释放预览内存
  if (typeof resetOrderState === 'function') resetOrderState();
  ['userTbody', 'deptTbody', 'roleTbody', 'brandTbody', 'catTbody', 'agTbody',
   'goodsTbody', 'goodsDetailBody'].forEach((id) => {
    const el = $(id);
    if (el) el.innerHTML = '';
  });
  ['uInpAccount', 'uInpNickName', 'rInpRoleName', 'bInpBrandName',
   'bSelCategoryFilter', 'agSelCategoryFilter', 'gInpGoodsName',
   'deptTreeSearch', 'catTreeSearch'].forEach((id) => {
    const el = $(id);
    if (el) el.value = '';
  });
  resetPublishForm(true);        // 发布商品：表单/三级联动/品牌/主图预览全部清空（静默）
  // 发布商品第 2/3 步：属性表单（规格参数/销售属性）状态与 DOM 复位 + 作废进行中的请求
  // 第 4 步：SKU 组合行一并清空（存储里的 pubKey/categoryId/saleAttr 按讲师行为保留）
  resetAttrFormState(goodsAttrForm);
  resetAttrFormState(saleAttrForm);
  syncSetGoodsAttrState();
  syncSaleAttrState();
  saleAttrRows = [];
  skuDims = [];
  skuRows = [];
  if (typeof resetPhase1Views === 'function') resetPhase1Views();
  skuDimSig = '';
  skuSubmitting = false;
  const skuBox = $('skuFormArea');
  if (skuBox) skuBox.innerHTML = '';
  syncSkuState();
  // 发布商品第 5 步：汇总卡片与按钮可用态按存储重算（此刻 catCache 已空，类别名兜底为 类别#id，
  // 重新登录后若仍停留在本视图，enterApp 会再走一次 initFinishView 补正类别名）
  finishSubmitting = false;
  renderFinishSummary();
  syncFinishState();
  // 数据概览：销毁图表实例（隐藏容器残留实例会在下次显示时尺寸错乱）
  disposeDashboardCharts();
}

// 登录/注册两屏输入框复位（密码一律清空）
function resetAuthForms() {
  ['lgAccount', 'lgPassword', 'rgAccount', 'rgNickName', 'rgPassword',
   'rgPassword2', 'rgPhone', 'rgDeptId'].forEach((id) => {
    const el = $(id);
    if (el) el.value = '';
  });
  const sex = $('rgSex');
  if (sex) sex.value = '男';
  const dept = $('rgDeptId');
  if (dept) dept.innerHTML = '<option value="">不分配部门</option>';
}

// 进入登录视图：登录/注册/退出/会话失效 共用收口（清数据、关弹窗、隐藏主界面）
function showLoginView() {
  closeAllModals();
  clearViewData();
  resetAuthForms();
  currentUser = null;
  $('register-view').hidden = true;
  $('app-view').hidden = true;
  $('login-view').hidden = false;
  setTimeout(() => { const el = $('lgAccount'); if (el) el.focus(); }, 0);
}

// 切到注册屏：两屏切换，进入时拉部门下拉（未登录 401 则静默降级）
function showRegisterView() {
  closeAllModals();
  $('login-view').hidden = true;
  $('app-view').hidden = true;
  $('register-view').hidden = false;
  const p1 = $('lgPassword'); if (p1) p1.value = '';
  loadRegDeptOptions();
}

// 注册页“所属部门”下拉：GET /Dept/list 平铺带“─ ”层级前缀。
// 注意：业务接口现在要求登录，未登录拉取会 401 —— 用 authRedirect:false 不触发切登录页，
// 失败时静默降级（下拉只剩“不分配部门”，可注册后再由管理员分配）。
async function loadRegDeptOptions() {
  const sel = $('rgDeptId');
  if (!sel) return;
  sel.innerHTML = '<option value="">不分配部门</option>';
  try {
    const res = await api('/Dept/list', 'GET', {}, { authRedirect: false });
    const opts = [];
    (function walk(list, depth) {
      for (const n of list) {
        opts.push({
          id: n.id,
          name: n.deptName,
          level: (n.level != null) ? n.level : depth
        });
        if (Array.isArray(n.children) && n.children.length) walk(n.children, depth + 1);
      }
    })(res.data || [], 1);
    for (const o of opts) {
      const prefix = o.level > 1 ? '─ '.repeat(o.level - 1) : '';
      sel.insertAdjacentHTML('beforeend',
        '<option value="' + o.id + '">' + esc(prefix + o.name) + '</option>');
    }
  } catch (e) {
    /* 未登录时 /Dept/list 会 401：静默，部门可不选 */
  }
}

// 登录：校验 → /Auth/login → 成功进主界面并初次加载各视图数据
async function doLogin() {
  const account = $('lgAccount').value.trim();
  const password = $('lgPassword').value;
  if (!account) { toast('请输入账号', false); return; }
  if (!password) { toast('请输入密码', false); return; }
  const btn = $('btnLogin');
  btn.disabled = true;
  try {
    const res = await api('/Auth/login', 'POST', { account: account, password: password });
    toast('登录成功，欢迎 ' + (res.data.nickName || res.data.account || ''));
    enterApp(res.data);
  } catch (e) { toast(e.message, false); }
  finally { btn.disabled = false; }
}

// 注册：前端校验（必填 / 密码≥4位 / 两次一致）→ /Auth/register → 回登录屏提示
async function doRegister() {
  const account = $('rgAccount').value.trim();
  const nickName = $('rgNickName').value.trim();
  const password = $('rgPassword').value;
  const password2 = $('rgPassword2').value;
  const phone = $('rgPhone').value.trim();
  const sex = $('rgSex').value;
  const deptId = $('rgDeptId').value;
  if (!account) { toast('请输入账号', false); return; }
  if (!nickName) { toast('请输入昵称', false); return; }
  if (!password || password.length < 4) { toast('密码至少 4 位', false); return; }
  if (password !== password2) { toast('两次输入的密码不一致', false); return; }
  const params = { account: account, nickName: nickName, password: password, sex: sex };
  if (phone) params.phone = phone;
  if (deptId) params.deptId = deptId;       // 部门选“不分配”则不提交
  const btn = $('btnRegister');
  btn.disabled = true;
  try {
    await api('/Auth/register', 'POST', params);
    toast('注册成功，请登录');
    const lg = $('lgAccount');
    lg.value = account;                     // 回填登录账号，方便直接登录
    showLoginView();
  } catch (e) { toast(e.message, false); }
  finally { btn.disabled = false; }
}

// 退出登录：调 /Auth/logout 后回登录视图（无论接口成败都切屏）
async function doLogout() {
  try {
    await api('/Auth/logout', 'POST');
    toast('已退出登录');
  } catch (e) {
    notifyError(e);                         // 失败也继续走退出收口
  }
  showLoginView();
}

// 进入主界面：记录登录用户 → 显示主界面 → 初次加载所有视图数据（未登录时不加载）
async function enterApp(user) {
  currentUser = user || null;
  sessionKicked = false;                    // 新会话开始，恢复业务层提示
  setAuthUi(currentUser);
  $('login-view').hidden = true;
  $('register-view').hidden = true;
  $('app-view').hidden = false;
  applyMenuPermissions(currentUser);
  initAppData();
  // 退出登录时以下视图被清空（属性行/SKU 行/商品列表/汇总卡片/图表实例）：重新登录后若仍停留在该视图，需要重建内容
  if (!$('view-setGoodsAttr').hidden) initSetGoodsAttrView();
  if (!$('view-saleAttr').hidden) initSaleAttrView();
  if (!$('view-setSku').hidden) initSkuView();
  if (!$('view-finish').hidden) initFinishView();
  if (!$('view-dashboard').hidden) initDashboard();
  // 商品管理无需在此重建：initAppData 里的 loadGoods(1) 已重新拉取并重绘（clearViewData 已把页码复位为 1）
}

async function applyMenuPermissions(user) {
  const nodes = document.querySelectorAll('#menuNav [data-permission]');
  nodes.forEach((node) => { node.hidden = false; });
  if (!user || user.roleId == null) return;
  try {
    const res = await api('/Role/permissions', 'GET', { id: user.roleId });
    const allowed = new Set((res.data || []).filter((p) => Number(p.selected) === 1).map((p) => p.path));
    nodes.forEach((node) => { node.hidden = !allowed.has(node.dataset.permission); });
  } catch (e) { /* 权限读取失败时保留菜单，后端认证仍然生效。 */ }
}

// 初次加载：部门树（用户页两个下拉依赖）→ 角色下拉 → 用户/角色/品牌/商品分页 → 类别树
// 说明：数据概览与上架第 2 步不在登录时预加载 —— 图表必须在容器可见时 init（隐藏容器尺寸为 0）、
//       属性列表要等第 1 步确认类别，二者均由 switchView 在进入视图时按需加载
function initAppData() {
  ensureDept()
    .then(() => { refillUserDeptSelects(); renderDept(); })
    .catch(notifyError);
  ensureRoleOptions().catch(notifyError);
  loadUsers(1);
  loadRoles(1);
  loadBrands(1);
  loadGoods(1);
  // 类别树加载完再拉属性组（其“所属类别”列与筛选下拉依赖类别树；loadCategories 内部已兜错）
  // 发布商品的三级联动同样依赖类别树：一并刷新选项（视图不可见时无副作用）
  loadCategories().then(() => { rebuildPubCascade(true); loadAttrGroups(1); });
}

/* ==================== 部门数据：整树一次加载进内存 ==================== */

// 深度优先把树拍平成平铺数组（下拉选项用），并建立 id -> 节点 索引（路径回溯用）
function buildDeptCache(raw) {
  const flat = [];
  const byId = {};
  (function walk(list, depth) {
    for (const n of list) {
      byId[n.id] = n;
      flat.push({
        id: n.id,
        name: n.deptName,
        desc: n.deptDesc,
        parentId: n.parentId,
        level: (n.level != null) ? n.level : depth,   // 层级以接口返回 level 为准
        node: n
      });
      if (Array.isArray(n.children) && n.children.length) {
        walk(n.children, depth + 1);                  // 无子部门时后端可能没有 children 键
      }
    }
  })(raw, 1);
  deptCache = { raw, flat, byId };
  collapsedDeptIds = new Set();                       // 重新加载后回到“全部展开”
}

// 拉取部门树；force=true 强制刷新（部门增删改后），否则优先用内存缓存
async function ensureDept(force = false) {
  if (!force && deptCache.raw) return deptCache;
  const res = await api('/Dept/list', 'GET');
  buildDeptCache(res.data || []);
  return deptCache;
}

// 部门下拉选项 HTML：按 level 加 "─ " 前缀体现层级，顶层不加
// excludeIds：编辑部门时排除自身 + 全部后代，防止把父级指向自己或自己的子孙（成环）
function deptOptionsHtml(excludeIds) {
  let h = '';
  for (const d of deptCache.flat) {
    if (excludeIds && excludeIds.has(d.id)) continue;
    const prefix = d.level > 1 ? '─ '.repeat(d.level - 1) : '';
    h += '<option value="' + d.id + '">' + esc(prefix + d.name) + '</option>';
  }
  return h;
}

// 收集 id 自身及其所有后代 id（DFS 原始树），用于父级下拉排除
function collectSubtreeIds(id) {
  const set = new Set([id]);
  (function walk(n) {
    if (Array.isArray(n.children)) {
      for (const c of n.children) { set.add(c.id); walk(c); }
    }
  })(deptCache.byId[id] || {});
  return set;
}

// 可见行 = 顶层起 DFS，跳过“已折叠集合”中节点的子树 —— 展开/收起只过滤内存，不重复请求
function visibleDeptNodes() {
  const out = [];
  (function walk(list) {
    for (const n of list) {
      out.push(n);
      if (Array.isArray(n.children) && n.children.length && !collapsedDeptIds.has(n.id)) {
        walk(n.children);
      }
    }
  })(deptCache.raw);
  return out;
}

// 层级描述：沿 parentId 链向上回溯拼“总部 / 研发部”式路径；顶层部门直接返回文案
function deptPathLabel(node) {
  const parts = [];
  let cur = node;
  while (cur && cur.parentId != null && cur.parentId != 0) {
    const p = deptCache.byId[cur.parentId];
    if (!p) break;                                  // 父级缺失（数据异常）停止回溯
    parts.unshift(p.deptName);
    cur = p;
  }
  return parts.length ? parts.join(' / ') : '顶层部门';
}

function renderDept() {
  const tbody = $('deptTbody');
  const keyword = ($('deptTreeSearch') ? $('deptTreeSearch').value : '').trim().toLowerCase();
  const nodes = keyword
    ? deptCache.flat.map((x) => deptCache.byId[x.id]).filter((n) => n && String(n.deptName || '').toLowerCase().includes(keyword))
    : visibleDeptNodes();
  if (!nodes.length) {
    tbody.innerHTML = '<tr><td colspan="3" class="empty">暂无数据，点击右上角「新增部门」</td></tr>';
    return;
  }
  let html = '';
  for (const n of nodes) {
    const level = (n.level != null) ? n.level : 1;
    const hasChild = Array.isArray(n.children) && n.children.length > 0;
    const indent = (level - 1) * 24;                 // 每行缩进 = (level-1)*24px
    const arrow = hasChild
      ? '<button type="button" class="tree-arrow" onclick="toggleDept(' + n.id + ')" title="' +
        (collapsedDeptIds.has(n.id) ? '展开' : '收起') + '">' +
        (collapsedDeptIds.has(n.id) ? '▸' : '▾') + '</button>'
      : '<span class="tree-arrow tree-leaf"></span>'; // 叶子节点：空白占位与箭头对齐
    html += '<tr>' +
      '<td><div class="tree-cell" style="padding-left:' + indent + 'px">' +
        arrow + '<button type="button" class="tree-name-btn" onclick="openDeptModal(\'edit\',' + n.id + ')">' + esc(n.deptName) + '</button>' +
      '</div></td>' +
      '<td title="pids: ' + esc(n.pids == null ? '' : n.pids) + '">' + esc(deptPathLabel(n)) + '</td>' +
      '<td class="op">' +
        '<button type="button" class="link-btn" onclick="openDeptModal(\'child\',' + n.id + ')"' + (level >= 4 ? ' disabled title="最多四级部门"' : '') + '>＋ 新增子部门</button>' +
        '<button type="button" class="link-btn" onclick="openDeptModal(\'edit\',' + n.id + ')">✎ 编辑</button>' +
        '<button type="button" class="link-btn danger" onclick="delDept(' + n.id + ')"' + (hasChild ? ' disabled title="请先处理子部门"' : '') + '>🗑 删除</button>' +
      '</td></tr>';
  }
  tbody.innerHTML = html;
}

// 展开/收起切换：只改折叠集合 + 重绘 DOM
function toggleDept(id) {
  if (collapsedDeptIds.has(id)) collapsedDeptIds.delete(id);
  else collapsedDeptIds.add(id);
  renderDept();
}

// 部门变更后统一刷新：重拉树 + 重绘部门视图 + 刷新用户页两个部门下拉
async function refreshDeptAll() {
  await ensureDept(true);
  renderDept();
  refillUserDeptSelects();
}

/* ==================== 部门管理视图 ==================== */

// mode: 'add' 新增顶层 / 'child' 新增子部门 / 'edit' 编辑
async function openDeptModal(mode, id) {
  try { await ensureDept(); } catch (e) { notifyError(e); return; }
  const node = (id != null) ? deptCache.byId[id] : null;
  const isEdit = (mode === 'edit');
  $('deptModalTitle').textContent = isEdit ? '编辑部门' : (mode === 'child' ? '新增子部门' : '新增部门');
  $('dmId').value = isEdit ? node.id : '';
  $('dmName').value = isEdit ? (node.deptName || '') : '';
  $('dmDesc').value = isEdit ? (node.deptDesc || '') : '';
  // 上级部门下拉：顶部“无（作为顶层部门）”；编辑时排除自身及后代防成环
  const exclude = isEdit ? collectSubtreeIds(node.id) : null;
  $('dmParent').innerHTML = '<option value="0">无（作为顶层部门）</option>' + deptOptionsHtml(exclude);
  // 父级回显: 顶层(库内 parentId 为 0/null)选中 value="0" 的顶层项; 真实父级选中其 id
  const preset = isEdit ? node.parentId : (mode === 'child' ? node.id : '');
  $('dmParent').value = (preset != null && preset != 0) ? String(preset) : '0';
  openModal('deptModal');
}

async function saveDept() {
  const id = $('dmId').value;
  const deptName = $('dmName').value.trim();
  const deptDesc = $('dmDesc').value.trim();
  const parentId = $('dmParent').value;
  if (!deptName) { toast('部门名称不能为空', false); return; }
  const params = { deptName };
  if (deptDesc) params.deptDesc = deptDesc;
  if (parentId !== '') params.parentId = parentId;   // "0" = 显式顶层; 空串不传 = 父级保持不变
  try {
    if (id) {
      params.id = id;
      await api('/Dept/update', 'PUT', params);
      toast('部门修改成功');
    } else {
      await api('/Dept/save', 'POST', params);
      toast('部门新增成功');
    }
    closeModal('deptModal');
    await refreshDeptAll();
  } catch (e) { notifyError(e); }
}

async function delDept(id) {
  const node = deptCache.byId[id];
  if (!node) return;
  if (!window.confirm('确定删除部门「' + (node.deptName || id) + '」吗？\n' +
      '（部门下存在子部门或用户时，后端会拒绝并给出原因）')) return;
  try {
    await api('/Dept/delete', 'DELETE', { id: id });
    toast('删除成功');
    await refreshDeptAll();
  } catch (e) { notifyError(e); }   // 后端 cause（中文提示）直接展示
}

/* ==================== 用户管理视图 ==================== */

async function loadUsers(page) {
  try {
    uPage = page || 1;
    const res = await api('/User/list', 'GET', {
      page: uPage,
      limit: PAGE_SIZE,
      account: $('uInpAccount').value.trim(),   // 与后端参数名严格对应
      nickName: $('uInpNickName').value.trim(),
      deptId: $('uSelDeptFilter').value
    });
    uRows = res.data || [];
    uTotal = (res.total != null) ? Number(res.total) : uRows.length;
    renderUserTable();
    drawPager('userPager', uPage, uTotal, (p) => loadUsers(p));
  } catch (e) { notifyError(e); }
}

function renderUserTable() {
  const tbody = $('userTbody');
  if (!uRows.length) {
    tbody.innerHTML = '<tr><td colspan="10" class="empty">暂无数据</td></tr>';
    return;
  }
  let html = '';
  uRows.forEach((r, i) => {
    html += '<tr>' +
      '<td>' + esc(r.id) + '</td>' +
      '<td>' + esc(r.account) + '</td>' +
      '<td>' + esc(r.nickName) + '</td>' +
      '<td>' + esc(r.phone) + '</td>' +
      '<td>' + esc(r.sex) + '</td>' +
      '<td>' + esc(r.deptName || '—') + '</td>' +
      '<td>' + esc(r.roleName || '—') + '</td>' +
      '<td>' + statusDot(r.status) + (Number(r.status) === 0 ? '停用' : '启用') + '</td>' +
      '<td>' + esc(r.createDate || '—') + '</td>' +
      '<td class="op">' +
        '<button type="button" class="link-btn" onclick="editUser(' + i + ')">✎ 编辑</button>' +
        '<button type="button" class="link-btn" onclick="toggleUserStatus(' + i + ')"' +
          (Number(r.id) === 1 || (currentUser && Number(currentUser.id) === Number(r.id)) ? ' disabled title="系统管理员或当前账号不能停用"' : '') + '>' +
          (Number(r.status) === 0 ? '启用' : '停用') + '</button>' +
        '<button type="button" class="link-btn danger" onclick="delUser(' + i + ')"' +
          (Number(r.id) === 1 || (currentUser && Number(currentUser.id) === Number(r.id)) ? ' disabled title="系统管理员或当前账号不能删除"' : '') + '>🗑 删除</button>' +
      '</td></tr>';
  });
  tbody.innerHTML = html;
}

function searchUsers() { loadUsers(1); }

async function toggleUserStatus(i) {
  const row = uRows[i];
  if (!row) return;
  const status = Number(row.status) === 0 ? 1 : 0;
  if (!window.confirm('确定' + (status === 1 ? '启用' : '停用') + '用户「' + (row.account || row.id) + '」吗？')) return;
  try {
    await api('/User/setStatus', 'POST', { id: row.id, status });
    toast(status === 1 ? '用户已启用' : '用户已停用');
    await loadUsers(uPage);
  } catch (e) { notifyError(e); }
}

function resetUserSearch() {
  $('uInpAccount').value = '';
  $('uInpNickName').value = '';
  $('uSelDeptFilter').value = '';
  loadUsers(1);
}

function editUser(i) {
  const row = uRows[i];
  if (row) openUserModal(row);
}

async function delUser(i) {
  const row = uRows[i];
  if (!row) return;
  if (!window.confirm('确定删除用户「' + (row.account || row.id) + '」吗？此操作不可恢复。')) return;
  try {
    await api('/User/delete', 'DELETE', { id: row.id });
    toast('删除成功');
    if (uRows.length === 1 && uPage > 1) uPage--;   // 删掉的是本页最后一条时回退一页
    loadUsers(uPage);
  } catch (e) { notifyError(e); }
}

/* ---------------- 用户新增/编辑弹窗 ---------------- */

// 角色下拉选项（来自 /Role/roleOptions，不含 id=1 系统管理员）
async function ensureRoleOptions(force) {
  if (!force && roleOptionsCache) return roleOptionsCache;
  const res = await api('/Role/roleOptions', 'GET');
  roleOptionsCache = res.data || [];
  return roleOptionsCache;
}

// 填充角色下拉的 HTML：顶部“不分配”空选项。
// 若编辑行的 roleId 不在 options 里（例如 id=1 系统管理员被后端过滤），
// 用行内的 roleName 动态补一项，保证回显与保存不丢角色。
function fillRoleSelectHtml(selectedId, fallbackName) {
  const opts = (roleOptionsCache || []).slice();
  if (selectedId != null && selectedId !== '' &&
      !opts.some((o) => String(o.value) === String(selectedId))) {
    opts.unshift({ value: selectedId, label: fallbackName || ('角色#' + selectedId) });
  }
  let h = '<option value="">不分配（无角色）</option>';
  for (const o of opts) {
    h += '<option value="' + esc(o.value) + '">' + esc(o.label) + '</option>';
  }
  return h;
}

// 重新填充用户页两个部门下拉（搜索条 + 弹窗），保留当前选中值（部门数据变更后调用）
function refillUserDeptSelects() {
  if (!deptCache.raw) return;
  const sel1 = $('uSelDeptFilter');
  const v1 = sel1.value;
  sel1.innerHTML = '<option value="">全部部门</option>' + deptOptionsHtml(null);
  sel1.value = v1;
  const sel2 = $('umDept');
  const v2 = sel2.value;
  sel2.innerHTML = '<option value="">不分配（无部门）</option>' + deptOptionsHtml(null);
  sel2.value = v2;
}

// 新增(null) / 编辑(行对象)。回显一律用列表行数据：
// 列表行含 roleId/deptId/roleName/deptName，而 /User/get 详情没有角色信息。
async function openUserModal(row) {
  try {
    await ensureDept();
    await ensureRoleOptions();
  } catch (e) { notifyError(e); return; }
  const isEdit = !!row;
  $('userModalTitle').textContent = isEdit ? '编辑用户' : '新增用户';
  $('umId').value = isEdit ? row.id : '';
  // 隐藏字段记录编辑行的 roleId 与 no/email（透传避免后端把已有值清空）
  $('umHiddenRoleId').value = isEdit ? (row.roleId == null ? '' : row.roleId) : '';
  $('umHiddenNo').value = isEdit ? (row.no || '') : '';
  $('umHiddenEmail').value = isEdit ? (row.email || '') : '';
  $('umAccount').value = isEdit ? (row.account || '') : '';
  $('umAccount').readOnly = isEdit;                 // 编辑时置灰只读（CSS 处理样式）
  $('umNickName').value = isEdit ? (row.nickName || '') : '';
  $('umPhone').value = isEdit ? (row.phone || '') : '';
  $('umSex').value = (isEdit && (row.sex === '男' || row.sex === '女')) ? row.sex : '男';
  $('umDept').innerHTML = '<option value="">不分配（无部门）</option>' + deptOptionsHtml(null);
  $('umDept').value = (isEdit && row.deptId != null) ? String(row.deptId) : '';
  $('umRole').innerHTML = fillRoleSelectHtml(isEdit ? row.roleId : '', isEdit ? row.roleName : '');
  $('umRole').value = (isEdit && row.roleId != null) ? String(row.roleId) : '';
  openModal('userModal');
}

async function saveUser() {
  const id = $('umId').value;
  const account = $('umAccount').value.trim();
  const nickName = $('umNickName').value.trim();
  const phone = $('umPhone').value.trim();
  const sex = $('umSex').value;
  if (!id && !account) { toast('账号不能为空', false); return; }
  if (!nickName) { toast('昵称不能为空', false); return; }
  if (sex !== '男' && sex !== '女') { toast('性别只允许 男/女（数据库 enum 约束）', false); return; }
  const params = { account: account, nickName: nickName, phone: phone, sex: sex };
  // 部门：选“不分配”则不提交 deptId
  const deptId = $('umDept').value;
  if (deptId) params.deptId = deptId;
  // 角色：不选时不带 roleId（后端更新时 roleId 缺省 = 清空角色，符合契约）
  const roleId = $('umRole').value;
  if (roleId) params.roleId = roleId;
  // 工号/邮箱不在表单展示，编辑时从行数据透传原值
  const no = $('umHiddenNo').value;
  const email = $('umHiddenEmail').value;
  if (no) params.no = no;
  if (email) params.email = email;
  try {
    if (id) {
      params.id = id;                              // 更新表单 = save 字段 + id
      await api('/User/update', 'PUT', params);
      toast('用户修改成功');
    } else {
      await api('/User/save', 'POST', params);
      toast('用户新增成功');
    }
    closeModal('userModal');
    loadUsers(uPage);
  } catch (e) { notifyError(e); }
}

/* ==================== 角色管理视图 ==================== */

async function loadRoles(page) {
  try {
    rPage = page || 1;
    const res = await api('/Role/list', 'GET', {
      page: rPage,
      limit: PAGE_SIZE,
      roleName: $('rInpRoleName').value.trim()
    });
    rRows = res.data || [];
    rTotal = (res.total != null) ? Number(res.total) : rRows.length;
    renderRoleTable();
    drawPager('rolePager', rPage, rTotal, (p) => loadRoles(p));
  } catch (e) { notifyError(e); }
}

function renderRoleTable() {
  const tbody = $('roleTbody');
  if (!rRows.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="empty">暂无数据</td></tr>';
    return;
  }
  let html = '';
  rRows.forEach((r, i) => {
    html += '<tr>' +
      '<td>' + esc(r.id) + '</td>' +
      '<td>' + esc(r.roleName) + '</td>' +
      '<td>' + esc(r.descript || '—') + '</td>' +
      '<td>' + esc(r.createDate || '—') + '</td>' +
      '<td class="op">' +
        '<button type="button" class="link-btn" onclick="editRole(' + i + ')">✎ 编辑</button>' +
        '<button type="button" class="link-btn danger" onclick="delRole(' + i + ')">🗑 删除</button>' +
      '</td></tr>';
  });
  tbody.innerHTML = html;
}

function searchRoles() { loadRoles(1); }

function resetRoleSearch() {
  $('rInpRoleName').value = '';
  loadRoles(1);
}

function editRole(i) {
  const row = rRows[i];
  if (row) openRoleModal(row);
}

async function delRole(i) {
  const row = rRows[i];
  if (!row) return;
  if (!window.confirm('确定删除角色「' + (row.roleName || row.id) + '」吗？')) return;
  try {
    await api('/Role/delete', 'DELETE', { id: row.id });
    toast('删除成功');
    if (rRows.length === 1 && rPage > 1) rPage--;
    loadRoles(rPage);
    refreshRoleOptions();        // 用户弹窗的角色下拉也同步刷新
  } catch (e) { notifyError(e); }
}

async function openRoleModal(row) {
  const isEdit = !!row;
  $('roleModalTitle').textContent = isEdit ? '编辑角色' : '新增角色';
  $('rmId').value = isEdit ? row.id : '';
  $('rmName').value = isEdit ? (row.roleName || '') : '';
  $('rmDesc').value = isEdit ? (row.descript || '') : '';
  $('rmPermissions').innerHTML = '<span class="hint">加载中…</span>';
  openModal('roleModal');
  try {
    const res = await api('/Role/permissions', 'GET', isEdit ? { id: row.id } : {});
    $('rmPermissions').innerHTML = (res.data || []).map((p) =>
      '<label class="permission-option"><input type="checkbox" value="' + esc(p.id) + '"' +
      (Number(p.selected) === 1 ? ' checked' : '') + '><span>' + esc(p.permission) + '</span><small>' + esc(p.path) + '</small></label>').join('');
  } catch (e) { notifyError(e); }
}

async function saveRole() {
  const id = $('rmId').value;
  const roleName = $('rmName').value.trim();
  const descript = $('rmDesc').value.trim();
  if (!roleName) { toast('角色名称不能为空', false); return; }
  const params = { roleName: roleName };
  if (descript) params.descript = descript;
  try {
    if (id) {
      params.id = id;
      await api('/Role/update', 'PUT', params);
      toast('角色修改成功');
    } else {
      const created = await api('/Role/save', 'POST', params);
      params.id = created.id;
      toast('角色新增成功');
    }
    const roleId = id || params.id;
    const permissionIds = Array.from(document.querySelectorAll('#rmPermissions input:checked')).map((el) => el.value).join(',');
    await api('/Role/savePermissions', 'POST', { roleId: roleId, permissionIds: permissionIds }, { noClean: true });
    closeModal('roleModal');
    loadRoles(rPage);
    refreshRoleOptions();        // 用户弹窗的角色下拉同步刷新
  } catch (e) { notifyError(e); }
}

// 刷新角色下拉缓存（角色增删改后调用，失败静默——只影响下拉可用性）
function refreshRoleOptions() {
  ensureRoleOptions(true).catch(notifyError);
}

/* ==================== 品牌管理视图 ==================== */

async function loadBrands(page) {
  try {
    bPage = page || 1;
    // 契约已升级：列表查询改为 POST 表单（原 GET /Brand/list 已废弃）
    const res = await api('/Brand/list', 'POST', {
      page: bPage,
      limit: PAGE_SIZE,
      brandName: $('bInpBrandName').value.trim(),
      categoryId: $('bSelCategoryFilter').value    // 空 = 全部类别（cleanParams 剔除后不传）
    });
    bRows = res.data || [];
    bTotal = (res.total != null) ? Number(res.total) : bRows.length;
    renderBrandTable();
    drawPager('brandPager', bPage, bTotal, (p) => loadBrands(p));
  } catch (e) { notifyError(e); }
}

// 状态点 HTML：showStatus=1 → 绿色实心；0 → 灰色。文案（显示/隐藏、上架/下架）由调用方决定
function statusDot(st) {
  return '<span class="st-dot' + (Number(st) === 1 ? ' on' : '') + '"></span>';
}

// 品牌图片地址（后端契约：GET /Brand/showImg/{logoName} 直连字节流）
function brandImgUrl(logoName) {
  return '/Brand/showImg/' + encodeURIComponent(logoName);
}

// LOGO 缩略图单元格 HTML：无文件名 → “无LOGO”占位；有 → 40px img 直连 /Brand/showImg/{logoName}，
// 404/加载失败由 imgLogoError 把图片节点就地替换成占位（不整表重绘）
function logoThumbHtml(fileName) {
  if (!fileName) return '<span class="no-logo">无LOGO</span>';
  return '<img class="brand-thumb" alt="" title="' + esc(fileName) + '" src="' + brandImgUrl(fileName) +
    '" onerror="imgLogoError(this)">';
}

// showImg 404（文件被删/未落盘）时：把出错 <img> 换成“无LOGO”占位
function imgLogoError(img) {
  const s = document.createElement('span');
  s.className = 'no-logo';
  s.textContent = '无LOGO';
  img.replaceWith(s);
}

function renderBrandTable() {
  const tbody = $('brandTbody');
  if (!bRows.length) {
    tbody.innerHTML = '<tr><td colspan="9" class="empty">暂无数据</td></tr>';
    return;
  }
  let html = '';
  bRows.forEach((r, i) => {
    const desc = r.brandDesc || '';
    const st = Number(r.showStatus);
    html += '<tr>' +
      '<td>' + esc(r.id) + '</td>' +
      '<td>' + logoThumbHtml(r.logoName) + '</td>' +
      '<td>' + esc(r.brandName) + '</td>' +
      '<td>' + (r.logoName
        ? '<span class="logo-fname" title="' + esc(r.logoName) + '">' + esc(r.logoName) + '</span>'
        : '—') + '</td>' +
      '<td>' + (desc
        ? '<span class="cell-ellip" title="' + esc(desc) + '">' + esc(desc) + '</span>'
        : '—') + '</td>' +
      // 状态列：行内开关（onchange 即调 setStatus，成功后本地重绘该表，不整页/不重新请求列表）
      '<td><span class="switch-wrap">' +
        '<label class="switch" title="点击切换显示状态">' +
          '<input type="checkbox" onchange="toggleBrandStatus(' + i + ')"' +
            (st === 1 ? ' checked' : '') + '>' +
          '<span class="slider"></span>' +
        '</label>' +
        '<span class="st-text">' + (st === 1 ? '显示' : '隐藏') + '</span>' +
      '</span></td>' +
      '<td>' + esc(r.createDate || '—') + '</td>' +
      '<td class="op">' +
        '<button type="button" class="link-btn" onclick="editBrand(' + i + ')">✎ 编辑</button>' +
        '<button type="button" class="link-btn" onclick="openBrandAssoc(' + i + ')">⇄ 关联类别</button>' +
        '<button type="button" class="link-btn danger" onclick="delBrand(' + i + ')">🗑 删除</button>' +
      '</td></tr>';
  });
  tbody.innerHTML = html;
}

// 行内开关切换状态：POST setStatus 成功后仅更新内存行 + 重绘表格（不重新请求列表）
async function toggleBrandStatus(i) {
  const row = bRows[i];
  if (!row) return;
  const target = (Number(row.showStatus) === 1) ? 0 : 1;
  try {
    await api('/Brand/setStatus', 'POST', { id: row.id, showStatus: target });
    row.showStatus = target;
    renderBrandTable();
  } catch (e) { notifyError(e); }
}

function searchBrands() { loadBrands(1); }

function resetBrandSearch() {
  $('bInpBrandName').value = '';
  $('bSelCategoryFilter').value = '';          // 类别筛选复位 = 全部类别
  loadBrands(1);
}

function editBrand(i) {
  const row = bRows[i];
  if (row) openBrandModal(row);
}

async function delBrand(i) {
  const row = bRows[i];
  if (!row) return;
  if (!window.confirm('确定删除品牌「' + (row.brandName || row.id) + '」吗？此操作不可恢复。')) return;
  try {
    await api('/Brand/delete', 'DELETE', { id: row.id });
    toast('删除成功');
    if (bRows.length === 1 && bPage > 1) bPage--;   // 删掉本页最后一条时回退一页
    loadBrands(bPage);
  } catch (e) { notifyError(e); }
}

/* ---------------- 品牌新增/编辑弹窗 ---------------- */

// 新增(null) / 编辑(行对象)。品牌列表行已含全部字段（logoName/brandDesc/showStatus），直接回显
// LOGO 区：记录原文件名 → resetLogoArea 回填预览（showImg 直连）/隐藏移除按钮
function openBrandModal(row) {
  const isEdit = !!row;
  $('brandModalTitle').textContent = isEdit ? '编辑品牌' : '新增品牌';
  $('bmId').value = isEdit ? row.id : '';
  $('bmBrandName').value = isEdit ? (row.brandName || '') : '';
  $('bmBrandDesc').value = isEdit ? (row.brandDesc || '') : '';
  $('bmShowStatus').value = isEdit ? String(row.showStatus == null ? 1 : row.showStatus) : '1';
  brandOrigLogo = isEdit ? (row.logoName || '') : '';   // 供“移除后保存=清空后端旧LOGO”判定
  resetLogoArea(brandOrigLogo);
  openModal('brandModal');
}

// 重置 LOGO 上传区：fileName 非空 → showImg 预览 + 显示“移除”；空 → “无LOGO”占位
function resetLogoArea(fileName) {
  if (bmObjectUrl) { URL.revokeObjectURL(bmObjectUrl); bmObjectUrl = null; }
  $('bmLogoName').value = fileName || '';
  $('bmLogoNameText').textContent = fileName || '';
  $('bmLogoNameText').title = fileName || '';
  $('bmRemoveLogoBtn').hidden = !fileName;
  const inp = $('bmLogoFile');
  if (inp) inp.value = '';                             // 清空 file input，保证再次选同一文件也能触发 change
  $('bmLogoPreview').innerHTML = fileName
    ? '<img src="' + brandImgUrl(fileName) + '" alt="LOGO 预览" onerror="imgLogoError(this)">'
    : '<span class="no-logo">无LOGO</span>';
}

// 触发隐藏的 file input（accept=image/*）
function pickLogoFile() { $('bmLogoFile').click(); }

// 选中图片：先 multipart 上传拿服务端文件名（回填 bmLogoName 随保存提交），
// 预览用 objectURL（仅本地展示；落盘文件名以上传返回值/服务端为准）
async function onLogoFileChange() {
  const inp = $('bmLogoFile');
  const file = inp.files && inp.files[0];
  if (!file) return;
  if (file.type && !file.type.startsWith('image/')) {
    toast('请选择图片文件（jpg/png 等）', false);
    inp.value = '';
    return;
  }
  try {
    const data = await uploadFile('/Brand/upload', file);
    // 上传接口把新文件名放在响应顶层：{ result, fileName, logoUrl, status }
    const fileName = data && data.fileName;
    if (!fileName) throw new Error('上传返回数据缺少 fileName（契约：顶层 fileName/logoUrl）');
    if (bmObjectUrl) URL.revokeObjectURL(bmObjectUrl);
    bmObjectUrl = URL.createObjectURL(file);
    $('bmLogoName').value = fileName;
    $('bmLogoNameText').textContent = fileName;
    $('bmLogoNameText').title = fileName;
    $('bmRemoveLogoBtn').hidden = false;
    $('bmLogoPreview').innerHTML = '<img src="' + bmObjectUrl + '" alt="LOGO 预览">';
    toast('LOGO 上传成功');
  } catch (e) {
    notifyError(e);
    inp.value = '';
  }
}

// 移除 LOGO：清空文件名与预览；编辑态原本有 LOGO 时，保存会以空串提交让后端清除
function removeLogoFile() {
  if (bmObjectUrl) { URL.revokeObjectURL(bmObjectUrl); bmObjectUrl = null; }
  $('bmLogoName').value = '';
  $('bmLogoNameText').textContent = '';
  $('bmLogoNameText').title = '';
  $('bmRemoveLogoBtn').hidden = true;
  $('bmLogoPreview').innerHTML = '<span class="no-logo">无LOGO</span>';
  const inp = $('bmLogoFile');
  if (inp) inp.value = '';
}

async function saveBrand() {
  const id = $('bmId').value;
  const brandName = $('bmBrandName').value.trim();
  const brandDesc = $('bmBrandDesc').value.trim();
  if (!brandName) { toast('品牌名称不能为空', false); return; }
  const logoName = $('bmLogoName').value;              // 上传成功回填 / 移除后为空串
  const params = { brandName: brandName, showStatus: $('bmShowStatus').value };
  let noClean = false;
  if (logoName) {
    params.logoName = logoName;
  } else if (id && brandOrigLogo) {
    // 编辑态原本有 LOGO 且被移除：显式提交空串让后端清空（cleanParams 会剔除空值，需 noClean）
    params.logoName = '';
    noClean = true;
  }
  if (brandDesc) params.brandDesc = brandDesc;
  try {
    if (id) {
      params.id = id;
      await api('/Brand/update', 'PUT', params, noClean ? { noClean: true } : {});
      toast('品牌修改成功');
    } else {
      await api('/Brand/save', 'POST', params);
      toast('品牌新增成功');
    }
    closeModal('brandModal');
    loadBrands(bPage);
  } catch (e) { notifyError(e); }
}

/* ==================== 类别管理视图（树形，模式同部门） ==================== */

// 类别整树拍平 + id 索引（下拉选项与父级路径回溯用）
function buildCatCache(raw) {
  const flat = [];
  const byId = {};
  (function walk(list, depth) {
    for (const n of list) {
      byId[n.id] = n;
      flat.push({
        id: n.id,
        name: n.categoryName,
        parentId: n.parentId,
        level: (n.level != null) ? n.level : depth,
        node: n
      });
      if (Array.isArray(n.children) && n.children.length) walk(n.children, depth + 1);
    }
  })(raw, 1);
  catCache = { raw, flat, byId };
  collapsedCatIds = new Set();                       // 重新加载后回到“全部展开”
}

// 拉取类别树；force=true 强制刷新（增删改后），否则优先用内存缓存
async function ensureCat(force = false) {
  if (!force && catCache.raw) return catCache;
  const res = await api('/Category/list', 'GET');
  buildCatCache(res.data || []);
  return catCache;
}

// 初次/强制加载后渲染列表（登录初次加载与类别增删改后共用）
async function loadCategories() {
  try {
    await ensureCat();
    renderCategory();
    refillCatDepSelects();           // 类别树就绪后填充依赖它的下拉（品牌/属性组搜索条与弹窗）
  } catch (e) { notifyError(e); }
}

// 重新填充“依赖类别树”的下拉（品牌搜索条类别 / 属性组搜索条类别 / 属性组弹窗所属类别），
// 保留各自当前选中值（类别增删改触发 refreshCategoryAll 后同步，避免选项过期）
function refillCatDepSelects() {
  if (!catCache.raw) return;
  const sync = (sel, firstHtml) => {
    if (!sel) return;
    const keep = sel.value;
    sel.innerHTML = firstHtml + catOptionsHtml(null);
    sel.value = keep;                 // 原值已被删除则回退到首项
  };
  sync($('bSelCategoryFilter'), '<option value="">全部类别</option>');
  sync($('agSelCategoryFilter'), '<option value="">全部类别</option>');
  sync($('gmCategory'), '<option value="">无（不关联类别）</option>');
}

// 上级类别下拉选项 HTML：带“─ ”层级前缀；excludeIds 排除自身及后代（防成环）
function catOptionsHtml(excludeIds) {
  let h = '';
  for (const c of catCache.flat) {
    if (excludeIds && excludeIds.has(c.id)) continue;
    const prefix = c.level > 1 ? '─ '.repeat(c.level - 1) : '';
    h += '<option value="' + c.id + '">' + esc(prefix + c.name) + '</option>';
  }
  return h;
}

// 收集类别 id 自身及其所有后代 id（DFS），编辑时排除，防止父级选到自己或子孙
function collectCatSubtreeIds(id) {
  const set = new Set([id]);
  (function walk(n) {
    if (Array.isArray(n.children)) {
      for (const c of n.children) { set.add(c.id); walk(c); }
    }
  })(catCache.byId[id] || {});
  return set;
}

// 可见行：顶层起 DFS，跳过已折叠节点的子树（展开/收起只过滤内存，不重复请求）
function visibleCatNodes() {
  const out = [];
  (function walk(list) {
    for (const n of list) {
      out.push(n);
      if (Array.isArray(n.children) && n.children.length && !collapsedCatIds.has(n.id)) {
        walk(n.children);
      }
    }
  })(catCache.raw);
  return out;
}

// 父级路径：沿 parentId 链回溯拼“手机/运营商/数码 / 手机通讯”式祖先路径；顶层显示“顶级类别”
function catPathLabel(node) {
  const parts = [];
  let cur = node;
  while (cur && cur.parentId != null && cur.parentId != 0) {
    const p = catCache.byId[cur.parentId];
    if (!p) break;                                  // 父级缺失（数据异常）停止回溯
    parts.unshift(p.categoryName);
    cur = p;
  }
  return parts.length ? parts.join(' / ') : '顶级类别';
}

function renderCategory() {
  const tbody = $('catTbody');
  const keyword = ($('catTreeSearch') ? $('catTreeSearch').value : '').trim().toLowerCase();
  const nodes = keyword
    ? catCache.flat.map((x) => catCache.byId[x.id]).filter((n) => n && String(n.categoryName || '').toLowerCase().includes(keyword))
    : visibleCatNodes();
  if (!nodes.length) {
    tbody.innerHTML = '<tr><td colspan="8" class="empty">暂无数据，点击右上角「新增类别」</td></tr>';
    return;
  }
  let html = '';
  for (const n of nodes) {
    const level = (n.level != null) ? n.level : 1;
    const hasChild = Array.isArray(n.children) && n.children.length > 0;
    const indent = (level - 1) * 24;
    const arrow = hasChild
      ? '<button type="button" class="tree-arrow" onclick="toggleCategory(' + n.id + ')" title="' +
        (collapsedCatIds.has(n.id) ? '展开' : '收起') + '">' +
        (collapsedCatIds.has(n.id) ? '▸' : '▾') + '</button>'
      : '<span class="tree-arrow tree-leaf"></span>';
    html += '<tr>' +
      '<td>' + esc(n.id) + '</td>' +
      '<td><div class="tree-cell" style="padding-left:' + indent + 'px">' +
        arrow + '<button type="button" class="tree-name-btn" onclick="openCategoryModal(\'edit\',' + n.id + ')">' + esc(n.categoryName) + '</button>' +
      '</div></td>' +
      '<td title="pids: ' + esc(n.pids == null ? '' : n.pids) + '">' + esc(catPathLabel(n)) + '</td>' +
      '<td>' + statusDot(n.showStatus) + (Number(n.showStatus) === 1 ? '上架' : '下架') + '</td>' +
      '<td>' + esc(n.sort == null ? '—' : n.sort) + '</td>' +
      '<td>' + esc(n.productUnit || '—') + '</td>' +
      '<td>' + esc(n.createDate || '—') + '</td>' +
      '<td class="op">' +
        '<button type="button" class="link-btn" onclick="openCategoryModal(\'child\',' + n.id + ')"' + (level >= 3 ? ' disabled title="最多三级类别"' : '') + '>＋ 新增子类别</button>' +
        '<button type="button" class="link-btn" onclick="openCategoryModal(\'edit\',' + n.id + ')">✎ 编辑</button>' +
        '<button type="button" class="link-btn" onclick="openCatAssoc(' + n.id + ')">⇄ 关联品牌</button>' +
        '<button type="button" class="link-btn danger" onclick="delCategory(' + n.id + ')"' + (hasChild ? ' disabled title="请先处理子类别"' : '') + '>🗑 删除</button>' +
      '</td></tr>';
  }
  tbody.innerHTML = html;
}

// 展开/收起切换：只改折叠集合 + 重绘 DOM
function toggleCategory(id) {
  if (collapsedCatIds.has(id)) collapsedCatIds.delete(id);
  else collapsedCatIds.add(id);
  renderCategory();
}

// 类别变更后统一刷新：重拉树（下拉选项随之更新）+ 重绘列表 + 同步依赖下拉
async function refreshCategoryAll() {
  await ensureCat(true);
  renderCategory();
  refillCatDepSelects();
  // 发布商品的三级联动选项同样依赖类别树：类别增删改后同步刷新，品牌选项强制重载
  rebuildPubCascade(true);
  syncPubCategory(true);
}

/* ---------------- 类别新增/编辑弹窗 ---------------- */

// mode: 'add' 新增顶层 / 'child' 新增子类别（预填父级）/ 'edit' 编辑（回显行数据）
async function openCategoryModal(mode, id) {
  try { await ensureCat(); } catch (e) { notifyError(e); return; }
  const node = (id != null) ? catCache.byId[id] : null;
  const isEdit = (mode === 'edit');
  $('categoryModalTitle').textContent = isEdit ? '编辑类别' : (mode === 'child' ? '新增子类别' : '新增类别');
  $('cmId').value = isEdit ? node.id : '';
  $('cmCategoryName').value = isEdit ? (node.categoryName || '') : '';
  // 上级类别下拉：顶部“无（作为顶层类别）”；编辑时排除自身及全部后代防成环
  const exclude = isEdit ? collectCatSubtreeIds(node.id) : null;
  $('cmParent').innerHTML = '<option value="0">无（作为顶层类别）</option>' + catOptionsHtml(exclude);
  // 父级回显: 顶层(库内 parentId 为 0 或 null)选中 value="0" 的顶层项; 真实父级选中其 id
  const preset = isEdit ? node.parentId : (mode === 'child' ? node.id : '');
  $('cmParent').value = (preset != null && preset != 0) ? String(preset) : '0';
  $('cmShowStatus').value = String(node && node.showStatus != null ? node.showStatus : 1);
  $('cmSort').value = (isEdit && node.sort != null && node.sort !== '') ? node.sort : '';
  $('cmIcons').value = isEdit ? (node.icons || '') : '';
  $('cmProductUnit').value = isEdit ? (node.productUnit || '') : '';
  openModal('categoryModal');
}

async function saveCategory() {
  const id = $('cmId').value;
  const categoryName = $('cmCategoryName').value.trim();
  const parentId = $('cmParent').value;
  const showStatus = $('cmShowStatus').value;
  const sort = $('cmSort').value;
  const icons = $('cmIcons').value.trim();
  const productUnit = $('cmProductUnit').value.trim();
  if (!categoryName) { toast('类别名称不能为空', false); return; }
  const params = { categoryName: categoryName, showStatus: showStatus };
  if (parentId !== '') params.parentId = parentId;   // "0" = 显式顶层(后端置 null 落库); 空串不传 = 父级保持不变
  if (sort !== '') params.sort = sort;
  if (icons) params.icons = icons;
  if (productUnit) params.productUnit = productUnit;
  try {
    if (id) {
      params.id = id;
      await api('/Category/update', 'PUT', params);
      toast('类别修改成功');
    } else {
      await api('/Category/save', 'POST', params);
      toast('类别新增成功');
    }
    closeModal('categoryModal');
    await refreshCategoryAll();
  } catch (e) { notifyError(e); }
}

async function delCategory(id) {
  const node = catCache.byId[id];
  if (!node) return;
  if (!window.confirm('确定删除类别「' + (node.categoryName || id) + '」吗？\n' +
      '（类别下存在子类别时，后端会拒绝并给出原因）')) return;
  try {
    await api('/Category/delete', 'DELETE', { id: id });
    toast('删除成功');
    await refreshCategoryAll();
  } catch (e) { notifyError(e); }   // 后端 cause（有子类别等中文提示）直接展示
}

/* ==================== 品牌 ⇄ 类别 / 类别 ⇄ 品牌 关联弹窗 ==================== */

// 把关联接口返回的数据拍平成 id 数组（兼容纯数组 [{id}] 与树结构 [{id,children}] 两种形态）
function collectTreeIds(list) {
  const out = [];
  (function walk(arr) {
    for (const n of arr) {
      if (n && n.id != null) out.push(Number(n.id));
      if (Array.isArray(n.children)) walk(n.children);
    }
  })(list || []);
  return out;
}

// 品牌页“关联类别”：GET 已关联类别取原勾选 → 类别整树平铺渲染 checkbox 多选列表
async function openBrandAssoc(i) {
  const row = bRows[i];
  if (!row) return;
  try { await ensureCat(); } catch (e) { notifyError(e); return; }
  bAssoc = { brandId: row.id, brandName: row.brandName || '', oldIds: [] };
  try {
    const res = await api('/Brand/associatedCategories', 'GET', { brandId: row.id });
    bAssoc.oldIds = collectTreeIds(res.data);
  } catch (e) { notifyError(e); return; }
  $('bamTip').textContent = '为品牌「' + row.brandName + '」勾选需要展示的类别（当前已关联 ' +
    bAssoc.oldIds.length + ' 个；可勾任意层级，无父子联动）';
  let h = '';
  for (const c of catCache.flat) {
    const indent = (c.level - 1) * 20;                 // 按层级缩进 (level-1)*20px
    const on = bAssoc.oldIds.indexOf(c.id) >= 0 ? ' checked' : '';
    h += '<label class="pick-item" style="padding-left:' + indent + 'px" title="' + esc(c.name) + '">' +
         '<input type="checkbox" value="' + c.id + '"' + on + '>' +
         '<span class="pick-name">' + esc(c.name) + '</span></label>';
  }
  $('bamList').innerHTML = h || '<div class="pick-empty">暂无类别数据</div>';
  openModal('brandAssocModal');
}

// 差分提交：当前勾选 → categories；原关联中本次未勾选 → deleteIds；JSON body 走 apiJson
async function saveBrandAssoc() {
  const categories = [];
  const checkedIds = new Set();
  document.querySelectorAll('#bamList input:checked').forEach((inp) => {
    const id = Number(inp.value);
    checkedIds.add(id);
    const node = catCache.byId[id];
    if (node) categories.push({ id: id, categoryName: node.categoryName });
  });
  const deleteIds = bAssoc.oldIds.filter((id) => !checkedIds.has(id));
  try {
    await apiJson('/Brand/associateCategory', {
      brandId: bAssoc.brandId,
      brandName: bAssoc.brandName,
      categories: categories,
      deleteIds: deleteIds
    });
    toast('品牌关联类别已保存');
    closeModal('brandAssocModal');
  } catch (e) { notifyError(e); }
}

// 类别页“关联品牌”：GET 已关联品牌取原勾选 + POST /Brand/list 全量品牌（500 条封顶）渲染多选
async function openCatAssoc(id) {
  const node = catCache.byId[id];
  if (!node) return;
  cAssoc = { categoryId: id, categoryName: node.categoryName || '', oldIds: [], all: [] };
  try {
    const res = await api('/Category/associatedBrands', 'GET', { categoryId: id });
    cAssoc.oldIds = collectTreeIds(res.data);
  } catch (e) { notifyError(e); return; }
  try {
    const res = await api('/Brand/list', 'POST', { page: 1, limit: 500 });
    cAssoc.all = res.data || [];
  } catch (e) { notifyError(e); return; }
  $('camTip').textContent = '为类别「' + node.categoryName + '」勾选需要展示的品牌（当前已关联 ' +
    cAssoc.oldIds.length + ' 个，共 ' + cAssoc.all.length + ' 个可选）';
  let h = '';
  for (const b of cAssoc.all) {
    const on = cAssoc.oldIds.indexOf(Number(b.id)) >= 0 ? ' checked' : '';
    h += '<label class="pick-item" title="' + esc(b.brandName) + '">' +
         '<input type="checkbox" value="' + b.id + '"' + on + '>' +
         '<span class="pick-name">' + esc(b.brandName) + '</span></label>';
  }
  $('camList').innerHTML = h || '<div class="pick-empty">暂无品牌数据</div>';
  openModal('catAssocModal');
}

// 差分提交：当前勾选 → brands；原关联中本次未勾选 → deleteIds
async function saveCatAssoc() {
  const brands = [];
  const checkedIds = new Set();
  document.querySelectorAll('#camList input:checked').forEach((inp) => {
    const id = Number(inp.value);
    checkedIds.add(id);
    const row = cAssoc.all.find((x) => Number(x.id) === id);
    if (row) brands.push({ id: id, brandName: row.brandName });
  });
  const deleteIds = cAssoc.oldIds.filter((id) => !checkedIds.has(id));
  try {
    await apiJson('/Category/associateBrand', {
      categoryId: cAssoc.categoryId,
      categoryName: cAssoc.categoryName,
      brands: brands,
      deleteIds: deleteIds
    });
    toast('类别关联品牌已保存');
    closeModal('catAssocModal');
  } catch (e) { notifyError(e); }
}

/* ==================== 属性组管理视图 ==================== */

async function loadAttrGroups(page) {
  // 所属类别列文案与筛选下拉依赖类别树；加载失败只影响文案展示，不阻塞列表（行内降级为 类别#id）
  try { await ensureCat(); } catch (e) { /* 静默，renderAttrGroupTable 内兜底 */ }
  try {
    agPage = page || 1;
    const res = await api('/AttrGroup/list', 'GET', {
      page: agPage,
      limit: PAGE_SIZE,
      categoryId: $('agSelCategoryFilter').value   // 空 = 全部类别（cleanParams 剔除后不传）
    });
    agRows = res.data || [];
    agTotal = (res.total != null) ? Number(res.total) : agRows.length;
    renderAttrGroupTable();
    drawPager('agPager', agPage, agTotal, (p) => loadAttrGroups(p));
  } catch (e) { notifyError(e); }
}

// categoryId → 平铺类别名（attr 行存 categoryId，列上显示类别名称）；树里找不到给 类别#id 兜底
function agCategoryLabel(id) {
  if (id == null || id === '') return '—';
  const n = catCache.byId[id];
  return n ? n.categoryName : ('类别#' + id);
}

function renderAttrGroupTable() {
  const tbody = $('agTbody');
  if (!agRows.length) {
    tbody.innerHTML = '<tr><td colspan="8" class="empty">暂无数据</td></tr>';
    return;
  }
  let html = '';
  agRows.forEach((r, i) => {
    const desc = r.descript || '';
    html += '<tr>' +
      '<td>' + esc(r.id) + '</td>' +
      '<td>' + esc(r.groupName) + '</td>' +
      '<td>' + esc(agCategoryLabel(r.categoryId)) + '</td>' +
      '<td>' + (desc
        ? '<span class="cell-ellip" title="' + esc(desc) + '">' + esc(desc) + '</span>'
        : '—') + '</td>' +
      '<td>' + (r.icons
        ? '<span class="cell-ellip" title="' + esc(r.icons) + '">' + esc(r.icons) + '</span>'
        : '—') + '</td>' +
      '<td>' + esc(r.sort == null ? '—' : r.sort) + '</td>' +
      '<td>' + esc(r.createDate || '—') + '</td>' +
      '<td class="op">' +
        '<button type="button" class="link-btn" onclick="editAttrGroup(' + i + ')">✎ 编辑</button>' +
        '<button type="button" class="link-btn danger" onclick="delAttrGroup(' + i + ')">🗑 删除</button>' +
      '</td></tr>';
  });
  tbody.innerHTML = html;
}

function searchAttrGroups() { loadAttrGroups(1); }

function resetAttrGroupSearch() {
  $('agSelCategoryFilter').value = '';
  loadAttrGroups(1);
}

function editAttrGroup(i) {
  const row = agRows[i];
  if (row) openAttrGroupModal(row);
}

async function delAttrGroup(i) {
  const row = agRows[i];
  if (!row) return;
  if (!window.confirm('确定删除属性组「' + (row.groupName || row.id) + '」吗？此操作不可恢复。')) return;
  try {
    await api('/AttrGroup/delete', 'DELETE', { id: row.id });
    toast('删除成功');
    if (agRows.length === 1 && agPage > 1) agPage--;   // 删掉本页最后一条时回退一页
    loadAttrGroups(agPage);
  } catch (e) { notifyError(e); }
}

/* ---------------- 属性组新增/编辑弹窗 ---------------- */

// 新增(null) / 编辑(行对象)：字段来自 /AttrGroup/list 行数据（groupName/categoryId/descript/icons/sort）
async function openAttrGroupModal(row) {
  try { await ensureCat(); } catch (e) { notifyError(e); return; }
  const isEdit = !!row;
  $('attrGroupModalTitle').textContent = isEdit ? '编辑属性组' : '新增属性组';
  $('gmId').value = isEdit ? row.id : '';
  $('gmName').value = isEdit ? (row.groupName || '') : '';
  $('gmDesc').value = isEdit ? (row.descript || '') : '';
  $('gmIcons').value = isEdit ? (row.icons || '') : '';
  $('gmSort').value = (isEdit && row.sort != null && row.sort !== '') ? row.sort : '';
  $('gmCategory').innerHTML = '<option value="">无（不关联类别）</option>' + catOptionsHtml(null);
  $('gmCategory').value = (isEdit && row.categoryId != null) ? String(row.categoryId) : '';
  openModal('attrGroupModal');
}

async function saveAttrGroup() {
  const id = $('gmId').value;
  const groupName = $('gmName').value.trim();
  const categoryId = $('gmCategory').value;
  const descript = $('gmDesc').value.trim();
  const icons = $('gmIcons').value.trim();
  const sort = $('gmSort').value;
  if (!groupName) { toast('分组名称不能为空', false); return; }
  const params = { groupName: groupName };
  if (categoryId !== '') params.categoryId = categoryId;   // “无（不关联）”则不提交
  if (descript) params.descript = descript;
  if (icons) params.icons = icons;
  if (sort !== '') params.sort = sort;
  try {
    if (id) {
      params.id = id;
      await api('/AttrGroup/update', 'PUT', params);
      toast('属性组修改成功');
    } else {
      await api('/AttrGroup/save', 'POST', params);
      toast('属性组新增成功');
    }
    closeModal('attrGroupModal');
    loadAttrGroups(agPage);
  } catch (e) { notifyError(e); }
}

/* ==================== 发布商品视图（商品上架第 1 步：基本信息） ==================== */
/*
 * 流程（还原讲师 PublishBaseInfo 模板）：
 *   三级类别联动（数据源 catCache）→ 选满三级才确认 categoryId 并加载品牌 →
 *   上传主图（type=1）→ POST /PublishGoods/saveGoodsBaseInfo（JSON，Redis 暂存）→ localStorage.pubKey
 * 讲师规则：必须确认三级类别才可选择品牌；主图没有上传之前不能提交
 */

// 类别节点 → 子级数组（后端无子级时可能不返回 children 键）
function catChildrenOf(node) {
  return (node && Array.isArray(node.children)) ? node.children : [];
}

// 三级联动下拉当前选中的类别节点（未选 / 缓存缺失返回 null）
function pubCatNode(id) {
  return id ? (catCache.byId[id] || null) : null;
}

// 用节点数组填充某个级联下拉：首项占位 + 各子级选项；selectedId 不在选项中时自动回落空值。
// 无选项（未选上级 / 该级无子级）时禁用该下拉
function fillPubCatSelect(sel, nodes, placeholder, selectedId) {
  let h = '<option value="">' + esc(placeholder) + '</option>';
  for (const n of nodes) {
    h += '<option value="' + esc(n.id) + '">' + esc(n.categoryName) + '</option>';
  }
  sel.innerHTML = h;
  sel.disabled = !nodes.length;
  sel.value = (selectedId == null) ? '' : String(selectedId);
}

// 重建三级联动（进入视图 / 类别树刷新 / 表单重置时调用）
// keepChain=true：尽量保留当前已选链路（选项已失效的层级自动截断）；false：全部清空
function rebuildPubCascade(keepChain) {
  const s1 = $('pubCat1'), s2 = $('pubCat2'), s3 = $('pubCat3');
  const v1 = keepChain ? s1.value : '';
  const v2 = keepChain ? s2.value : '';
  const v3 = keepChain ? s3.value : '';
  fillPubCatSelect(s1, catCache.raw || [], '请选择一级类别', v1);
  fillPubCatSelect(s2, catChildrenOf(pubCatNode(s1.value)), '请选择二级类别', v2);
  fillPubCatSelect(s3, catChildrenOf(pubCatNode(s2.value)), '请选择三级类别', v3);
}

// 级联变化（level: 1|2|3）：清空下游下拉与已选品牌 → 重新判定类别确认 → 必要时提示
function onPubCatChange(level) {
  const s2 = $('pubCat2'), s3 = $('pubCat3');
  if (level === 1) {
    fillPubCatSelect(s2, catChildrenOf(pubCatNode($('pubCat1').value)), '请选择二级类别', '');
    fillPubCatSelect(s3, [], '请选择三级类别', '');
  } else if (level === 2) {
    fillPubCatSelect(s3, catChildrenOf(pubCatNode(s2.value)), '请选择三级类别', '');
  }
  resetPubBrandSelect('请先选择类别');     // 类别一变，原品牌可能不属于新类别：一律清空
  syncPubCategory();
  // 选中的节点没有子级 → 永远走不到三级：提示（必须选满三级才算确认类别）
  const n1 = pubCatNode($('pubCat1').value);
  if (n1 && !s2.value && !catChildrenOf(n1).length) toast('请选择到三级类别', false);
  const n2 = pubCatNode(s2.value);
  if (n2 && !s3.value && !catChildrenOf(n2).length) toast('请选择到三级类别', false);
}

// 依据三级联动当前选择同步类别确认态：选满三级 → categoryId=第三级 id 并加载品牌；
// 否则清空 categoryId 且品牌下拉回到“请先选择类别”禁用态
// forceReload=true：强制重载品牌选项（类别树变更后选项可能过期）
function syncPubCategory(forceReload) {
  const v1 = $('pubCat1').value, v2 = $('pubCat2').value, v3 = $('pubCat3').value;
  if (v1 && v2 && v3) {                    // 讲师逻辑：arr.length == 3 才确认类别
    publishCategoryId = v3;
    if (forceReload) publishBrandFor = '';
    loadPubBrandOptions(v3);
  } else {
    publishCategoryId = '';
    resetPubBrandSelect('请先选择类别');
  }
}

// 品牌下拉复位为禁用占位态（未确认类别 / 重置表单 / 级联变化时调用）
function resetPubBrandSelect(placeholder) {
  const sel = $('pubBrandId');
  publishBrandFor = '';
  publishBrandSeq++;                       // 作废进行中的品牌请求
  sel.innerHTML = '<option value="">' + esc(placeholder) + '</option>';
  sel.value = '';
  sel.disabled = true;
}

// 加载品牌下拉：GET /Brand/brandOptions/{categoryId} → data [{value,label},...]
// 已加载过同一类别则直接复用（视图来回切换不重复请求）；保留当前已选品牌（选项仍存在才回显）
async function loadPubBrandOptions(categoryId) {
  const key = String(categoryId);
  if (publishBrandFor === key) return;
  const sel = $('pubBrandId');
  const keep = sel.value;
  const seq = ++publishBrandSeq;
  sel.disabled = true;
  sel.innerHTML = '<option value="">品牌加载中…</option>';
  try {
    const res = await api('/Brand/brandOptions/' + encodeURIComponent(key), 'GET');
    if (seq !== publishBrandSeq) return;   // 期间级联又变了：丢弃过期响应
    const opts = res.data || [];
    let h = opts.length ? '<option value="">请选择品牌（可不选）</option>'
                        : '<option value="">该类别下暂无可选品牌</option>';
    for (const o of opts) {
      h += '<option value="' + esc(o.value) + '">' + esc(o.label) + '</option>';
    }
    sel.innerHTML = h;
    sel.value = keep;
    sel.disabled = false;
    publishBrandFor = key;
  } catch (e) {
    if (seq !== publishBrandSeq) return;
    publishBrandFor = '';                  // 保持未加载：重进视图或重选类别会再试
    sel.innerHTML = '<option value="">品牌加载失败（可不选，重选类别可重试）</option>';
    sel.disabled = false;                  // 品牌可空，不阻塞流程
    notifyError(e);
  }
}

/* ---------------- 商品主图上传（105×105 方块） ---------------- */

// 主图直连地址（上传成功后预览用，与列表回显同一地址风格）
function pubGoodsImgUrl(fileName) {
  return '/PublishGoods/showImg/goods/' + encodeURIComponent(fileName);
}

// 按当前状态渲染主图预览：本地 objectURL（上传中即时反馈）> 已上传文件名（showImg）> 加号占位
function renderPubMainPreview() {
  const box = $('pubMainPreview');
  if (publishMainImgUrl) {
    box.innerHTML = '<img src="' + publishMainImgUrl + '" alt="商品主图">';
  } else if (publishMainImg) {
    box.innerHTML = '<img src="' + pubGoodsImgUrl(publishMainImg) + '" alt="商品主图">';
  } else {
    box.innerHTML = '<span class="pub-plus">+</span>';
  }
}

// 主图上传中开关：方块置灰禁点 + 两个按钮禁用 + 同步「下一步」可用态
function setPubMainUploading(on) {
  publishingImg = !!on;
  $('pubMainPreview').classList.toggle('uploading', publishingImg);
  $('pubPickMainBtn').disabled = publishingImg;
  $('pubRemoveMainBtn').disabled = publishingImg;
  $('pubMainImgFile').disabled = publishingImg;
  syncPublishSubmitState();
}

// 「下一步」可用态：主图已上传且不在上传中（讲师要求：文件没上传之前不能点提交）
function syncPublishSubmitState() {
  const ok = !!publishMainImg && !publishingImg;
  $('btnPublishNext').disabled = !ok;
  $('pubSubmitHint').textContent = ok ? '将暂存第 1 步基础信息'
    : (publishingImg ? '主图上传中，请稍候' : '主图未上传，不可提交');
}

// 触发隐藏的 file input（accept=image/*）
function pickPubMainImg() {
  if (publishingImg) return;
  $('pubMainImgFile').click();
}

// 选择主图：先本地预览（objectURL）→ multipart 上传（type=1）→ 成功回填主图文件名并改用 showImg 预览；
// 失败回滚到上一次已上传状态（publishMainImg 不变）
async function onPubMainImgChange() {
  const inp = $('pubMainImgFile');
  const file = inp.files && inp.files[0];
  if (!file) return;
  if (file.type && !file.type.startsWith('image/')) {
    toast('请选择图片文件（jpg/png 等）', false);
    inp.value = '';
    return;
  }
  if (publishMainImgUrl) URL.revokeObjectURL(publishMainImgUrl);
  publishMainImgUrl = URL.createObjectURL(file);
  renderPubMainPreview();                  // 上传期间先给本地预览
  setPubMainUploading(true);
  try {
    const data = await uploadFile('/PublishGoods/upload', file, { type: 1 });   // type=1 主图
    const fileName = data && data.fileName;
    if (!fileName) throw new Error('上传返回数据缺少 fileName（契约：顶层 fileName/logoUrl）');
    publishMainImg = fileName;
    $('pubMainImg').value = fileName;      // 隐藏字段随表单提交
    $('pubMainImgName').textContent = fileName;
    $('pubMainImgName').title = fileName;
    $('pubRemoveMainBtn').hidden = false;
    URL.revokeObjectURL(publishMainImgUrl); // 预览改用服务端地址，本地 URL 不再需要
    publishMainImgUrl = null;
    renderPubMainPreview();
    toast('主图上传成功');
  } catch (e) {
    if (publishMainImgUrl) { URL.revokeObjectURL(publishMainImgUrl); publishMainImgUrl = null; }
    renderPubMainPreview();                // 回滚：仍显示上一次已上传主图（或加号占位）
    notifyError(e);
  } finally {
    setPubMainUploading(false);
    inp.value = '';                        // 清空 file input，保证再次选同一文件也触发 change
  }
}

// 移除主图：清空文件名/隐藏字段/预览/按钮，并同步「下一步」可用态
function removePubMainImg() {
  if (publishingImg) return;
  if (publishMainImgUrl) { URL.revokeObjectURL(publishMainImgUrl); publishMainImgUrl = null; }
  publishMainImg = '';
  $('pubMainImg').value = '';
  $('pubMainImgName').textContent = '';
  $('pubMainImgName').title = '';
  $('pubRemoveMainBtn').hidden = true;
  $('pubMainImgFile').value = '';
  renderPubMainPreview();
  syncPublishSubmitState();
}

/* ---------------- 表单提交 / 重置 / 进入视图 ---------------- */

// 提交第 1 步（讲师 savePublishData 方案）：整单 JSON POST /PublishGoods/saveGoodsBaseInfo，
// 后端把第 1 步表单暂存 Redis 并返回 16 位大写发布键 pubKey → 前端写 localStorage.pubKey，后续步骤凭此键续传。
// 注意：本步不再落库（原 /PublishGoods/saveBaseInfo 表单直落库后端保留，前端不再调用）；隐藏字段
// pubGoodsId/pubMainImg 保留不动，仅作后续步骤衔接（pubGoodsId 不再由本函数写入）。
async function savePublishBaseInfo() {
  const goodsName = $('pubGoodsName').value.trim();
  if (!goodsName) { toast('商品名称不能为空', false); return; }
  if (!publishCategoryId) { toast('请选择到三级类别', false); return; }   // 必须选满三级（讲师规则）
  if (!publishMainImg) { toast('请先上传商品主图', false); return; }      // 主图未上传成功不可提交
  // goodsDetail：goodsName/categoryId 必给，其余可空（空则不带该键，后端按 null 处理）
  const goodsDetail = {
    goodsName: goodsName,
    categoryId: Number(publishCategoryId),   // 已确认的三级类别 id（JSON 数字）
    mainImage: publishMainImg                // 表单侧叫 mainImg，JSON 契约字段名为 mainImage
  };
  const goodsDetails = $('pubGoodsDetails').value.trim();
  if (goodsDetails) goodsDetail.goodsDetails = goodsDetails;
  const brandId = $('pubBrandId').value;     // 品牌可空：不选不传
  if (brandId) goodsDetail.brandId = Number(brandId);
  const weight = $('pubWeight').value;       // 重量可空：不填不传（数字输入，防御性判有限数）
  if (weight !== '' && Number.isFinite(Number(weight))) goodsDetail.weight = Number(weight);
  // pointRule：两项都留空 → 整个键传 null（后端取讲师模板默认 150/100）；单项留空 → 该项不传
  const pointRule = {};
  const allowDeduct = $('pubAllowDeduct').value;
  if (allowDeduct !== '' && Number.isFinite(Number(allowDeduct))) pointRule.allowDeduct = Number(allowDeduct);
  const rewardPoint = $('pubRewardPoint').value;
  if (rewardPoint !== '' && Number.isFinite(Number(rewardPoint))) pointRule.rewardPoint = Number(rewardPoint);
  const payload = {
    goodsDetail: goodsDetail,
    pointRule: Object.keys(pointRule).length ? pointRule : null,
    spuAlbumVO: { images: [] }               // 图集属于后续步骤，本轮固定空数组
  };
  $('btnPublishNext').disabled = true;
  try {
    const res = await apiJson('/PublishGoods/saveGoodsBaseInfo', payload);
    // 讲师答案：后端 R.ok().put("pubKey", pubKey) —— pubKey 在响应**顶层**（非 data 内）
    const pubKey = res && res.pubKey;
    if (!pubKey) throw new Error('返回数据缺少 pubKey（契约：顶层 pubKey，16 位大写发布键）');
    window.localStorage.setItem('pubKey', pubKey);   // 后续步骤凭此键继续（重置表单不清除）
    // 第 2 步要用类别 id 加载规格参数：讲师用路由参数传递，本项目无路由 → 内存全局 + localStorage 兜底
    window.localStorage.setItem('categoryId', publishCategoryId);
    console.log('【发布键】pubKey：' + pubKey);
    toast('基础信息已暂存（发布键：' + pubKey + '），后续步骤将凭此键继续');
    switchView('setGoodsAttr');              // 自动进入第 2 步（贴讲师 $router.push 语义）
  } catch (e) { notifyError(e); }
  finally { syncPublishSubmitState(); }
}

// 重置表单：清空所有字段 + 三级联动 + 品牌 + 主图预览
// 注意：不清 localStorage.pubKey —— 发布键跨步骤使用（讲师行为），仅本页表单字段复位
// silent=true：退出登录/会话失效时的静默清空（不弹 toast）
function resetPublishForm(silent) {
  $('pubGoodsId').value = '';
  $('pubGoodsName').value = '';
  $('pubGoodsDetails').value = '';
  $('pubWeight').value = '';
  $('pubAllowDeduct').value = '';
  $('pubRewardPoint').value = '';
  publishCategoryId = '';
  rebuildPubCascade(false);
  resetPubBrandSelect('请先选择类别');
  if (publishMainImgUrl) { URL.revokeObjectURL(publishMainImgUrl); publishMainImgUrl = null; }
  publishMainImg = '';
  $('pubMainImg').value = '';
  $('pubMainImgName').textContent = '';
  $('pubMainImgName').title = '';
  $('pubRemoveMainBtn').hidden = true;
  $('pubMainImgFile').value = '';
  setPubMainUploading(false);              // 复位上传中状态（内部会同步「下一步」可用态）
  renderPubMainPreview();
  syncPublishSubmitState();
  if (!silent) toast('表单已重置');
}

// 进入发布商品视图：确保类别树就绪 → 刷新三级联动选项（保留已选链路）→ 重新判定类别/品牌
// 并按当前状态重算「下一步」可用态（进入时 UI 一律由状态推导，不依赖上一次的 DOM 残留）
async function initPublishView() {
  try { await ensureCat(); } catch (e) { notifyError(e); return; }
  rebuildPubCascade(true);
  syncPubCategory();
  syncPublishSubmitState();
}

/* ==================== 发布商品视图（商品上架第 2/3 步：属性表单通用引擎） ==================== */
/*
 * 第 2 步「设置商品属性 / 规格参数」（attrType=1）与第 3 步「销售属性」（attrType=2）同构：
 *   categoryId 由第 1 步保存成功时写入（内存优先 publishCategoryId，兜底 localStorage.categoryId）
 *   → GET /GoodsAttr/listByCategory/{categoryId}/{attrType} 取属性列表
 *   → 逐行渲染：valueType=1 单值 → input + datalist（候选值 = attrValue 按 ';' 拆分，可自由输入）；
 *               valueType=2 多值 → 标签多选（已选值标签可 × 移除；输入框回车添加；候选项点击添加）
 *   → 「下一步」→ POST /PublishGoods/save{G|Sale}AttrValues（JSON：{pubKey, attrList}）
 * 两步的差异只有「容器与动态控件 id 前缀 / attrType / 按钮与文案」，全部收进 ctx 配置对象（createAttrFormCtx）；
 * 渲染与交互函数只操作 ctx，故第 3 步无需复制一份（本引擎即由第 2 步既有实现参数化而来，行为不变）。
 * 讲师规则：属性列表扁平展示（不分组）；空值行提交时过滤；属性全为空也可提交（无强制校验）
 */

// 创建一份属性表单上下文：containerId = 属性行容器；prefix = 动态控件 id 前缀（两个视图的 id 不能重复）
function createAttrFormCtx(cfg) {
  return {
    containerId: cfg.containerId,
    prefix: cfg.prefix,
    attrType: cfg.attrType,          // 1=规格参数（第 2 步）/ 2=销售属性（第 3 步）
    btnId: cfg.btnId,                // 「下一步」按钮 id
    hintId: cfg.hintId,              // 底部提示 id
    texts: cfg.texts,                // 文案：loading/empty/fail/noCat/busy/idle
    rows: [],                        // [{id,attrName,valueType,attrValue,cands,valueArr}]
    loadedFor: '',                   // 已加载属性列表的类别 id（'' = 未加载）
    seq: 0,                          // 请求序号（类别快速切换时丢弃过期响应）
    submitting: false                // 提交中
  };
}

// 第 2 步：规格参数（容器 #attrFormList；动态控件 id 前缀 attr → attrInp0 / attrTagBox0 …）
const goodsAttrForm = createAttrFormCtx({
  containerId: 'attrFormList',
  prefix: 'attr',
  attrType: 1,
  btnId: 'btnAttrNext',
  hintId: 'attrSubmitHint',
  texts: {
    loading: '规格参数加载中…',
    empty: '该类别下暂无规格参数，可直接进入下一步',
    fail: '规格参数加载失败：',
    noCat: '请先完成第 1 步「基本信息」',
    busy: '正在暂存规格参数…',
    idle: '空值不会被提交（无强制校验，可直接进入下一步）'
  }
});

// 第 3 步：销售属性（容器 #saleAttrFormList；动态控件 id 前缀 saleAttr → saleAttrInp0 / saleAttrTagBox0 …）
const saleAttrForm = createAttrFormCtx({
  containerId: 'saleAttrFormList',
  prefix: 'saleAttr',
  attrType: 2,
  btnId: 'btnSaleAttrNext',
  hintId: 'saleAttrSubmitHint',
  texts: {
    loading: '销售属性加载中…',
    empty: '该类别下暂无销售属性，可直接进入下一步',
    fail: '销售属性加载失败：',
    noCat: '请先完成第 1 步「基本信息」',
    busy: '正在暂存销售属性…',
    idle: '空值不会被提交（无强制校验，可直接进入下一步）'
  }
});

// 第 2/3 步的类别 id：内存全局优先（第 1 步刚保存过），其次 localStorage（刷新/重进兜底）；'' = 都没有
function resolveAttrCategoryId() {
  return String(publishCategoryId || window.localStorage.getItem('categoryId') || '');
}

// 属性行的候选项：接口 attrValue 按 ';' 拆成数组（讲师答案：多值型 valueArr = attrValue.split(';')）
// 只对接口原始值拆一次并缓存到 row.cands —— 单值行用户输入会覆盖 attrValue，不能边输入边重拆
function attrCandidates(attrValue) {
  if (!attrValue) return [];
  return String(attrValue).split(';').map((s) => s.trim()).filter((s) => s !== '');
}

// 拉取并渲染属性列表：GET /GoodsAttr/listByCategory/{categoryId}/{attrType}（第 2/3 步共用）
async function loadAttrRows(ctx, categoryId) {
  const box = $(ctx.containerId);
  if (!box) return;
  const seq = ++ctx.seq;                        // 请求序号：期间又切了类别则丢弃本次响应
  box.innerHTML = '<div class="attr-empty">' + ctx.texts.loading + '</div>';
  try {
    const res = await api('/GoodsAttr/listByCategory/' + encodeURIComponent(categoryId) + '/' + ctx.attrType, 'GET');
    if (seq !== ctx.seq) return;
    ctx.rows = (res.data || []).map((o) => {
      const row = {
        id: o.id,
        attrName: o.attrName || ('属性#' + o.id),
        valueType: o.valueType,
        attrValue: (o.attrValue == null) ? '' : String(o.attrValue),   // 单值行：当前值（初始 = 接口值）
        cands: [],
        valueArr: []
      };
      row.cands = attrCandidates(row.attrValue);
      // 讲师答案：多值型把 attrValue 拆成 valueArr 作为初始已选值（如 "256GB;512GB" → [256GB, 512GB]）
      if (row.valueType == 2) row.valueArr = row.cands.slice();
      return row;
    });
    ctx.loadedFor = String(categoryId);
    renderAttrRows(ctx);
  } catch (e) {
    if (seq !== ctx.seq) return;
    ctx.rows = [];
    ctx.loadedFor = '';                         // 保持未加载：重进视图会再试
    box.innerHTML = '<div class="attr-empty">' + ctx.texts.fail + esc(e.message) + '</div>';
    notifyError(e);
  }
  syncAttrFormState(ctx);
}

// 渲染全部属性行（label 复用 .pub-label；控件按 valueType 分派）
function renderAttrRows(ctx) {
  const box = $(ctx.containerId);
  if (!box) return;
  if (!ctx.rows.length) {
    box.innerHTML = '<div class="attr-empty">' + ctx.texts.empty + '</div>';
    return;
  }
  let h = '';
  ctx.rows.forEach((row, i) => {
    h += '<div class="pub-row top">' +
      '<label class="pub-label" for="' + ctx.prefix + ((row.valueType == 1) ? 'Inp' : 'TagInp') + i + '">' +
      esc(row.attrName) + '：</label>' +
      '<div class="attr-ctrl">' + attrCtrlHtml(ctx, row, i) + '</div>' +
      '</div>';
  });
  box.innerHTML = h;
}

// 单行控件 HTML：单值 → input(+datalist)；多值 → 标签框 + 候选值
function attrCtrlHtml(ctx, row, i) {
  const cands = row.cands || [];
  if (row.valueType == 1) {
    const dlId = ctx.prefix + 'Dl' + i;
    // datalist：既有候选可选，也允许自由输入（= 讲师 el-select filterable allow-create default-first-option）
    // 【bug 修复】补 data-row 行号：输入代理 onAttrFormInput 靠它把输入回写到对应 row.attrValue
    let h = '<input id="' + ctx.prefix + 'Inp' + i + '" class="form-input attr-value-input" type="text"' +
      ' data-row="' + i + '"' +
      (cands.length ? ' list="' + dlId + '"' : '') +
      ' placeholder="请选择或输入属性值（可自定义）" autocomplete="off"' +
      ' value="' + esc(row.attrValue) + '">';
    if (cands.length) {
      h += '<datalist id="' + dlId + '">';
      for (const c of cands) h += '<option value="' + esc(c) + '"></option>';
      h += '</datalist>';
    }
    return h;
  }
  let h = '<div class="tag-box" id="' + ctx.prefix + 'TagBox' + i + '">' +
    attrTagsHtml(row, i) + attrTagInputHtml(ctx, row, i) + '</div>';
  if (cands.length) {
    h += '<div class="tag-cands"><span>候选值：</span>' +
      cands.map((c, ci) => '<span class="cand" data-row="' + i + '" data-cand="' + ci + '">' +
        esc(c) + '</span>').join('') + '</div>';
  }
  return h;
}

// 已选值标签 HTML（标签 + × 移除按钮；data-* 交给容器点击代理处理）
function attrTagsHtml(row, i) {
  return (row.valueArr || []).map((v, ti) =>
    '<span class="tag">' + esc(v) + '<span class="tag-x" title="移除" data-row="' + i +
    '" data-tag-rm="' + ti + '">×</span></span>').join('');
}

// 标签输入框 HTML（回车添加自定义值）
function attrTagInputHtml(ctx, row, i) {
  return '<input id="' + ctx.prefix + 'TagInp' + i + '" class="tag-input" data-row="' + i + '" type="text"' +
    ' autocomplete="off" placeholder="' +
    ((row.cands || []).length ? '输入后回车添加，或点击下方候选值' : '输入后回车添加') + '">';
}

// 重渲染某行的标签区（保留输入框里未提交的草稿与焦点）
function renderAttrTagBox(ctx, i) {
  const box = $(ctx.prefix + 'TagBox' + i);
  const row = ctx.rows[i];
  if (!box || !row) return;
  const draft = (box.querySelector('.tag-input') || {}).value || '';
  box.innerHTML = attrTagsHtml(row, i) + attrTagInputHtml(ctx, row, i);
  const inp = box.querySelector('.tag-input');
  if (inp) { inp.value = draft; inp.focus(); }
}

// 多值行添加一个值（trim 后非空且不重复才加）
function addAttrTagValue(ctx, i, val) {
  const row = ctx.rows[i];
  const v = String(val == null ? '' : val).trim();
  if (!row || !v || row.valueArr.includes(v)) return;
  row.valueArr.push(v);
  renderAttrTagBox(ctx, i);
}

// 输入框回车：把当前输入内容加为标签，并清空输入框继续填
function addAttrTagFromInput(ctx, i) {
  const inp = $(ctx.prefix + 'TagInp' + i);
  if (!inp) return;
  addAttrTagValue(ctx, i, inp.value);
  inp.value = '';
  inp.focus();
}

// 点击候选值：按下标取（避免把候选字符串拼进 HTML 属性里）
function addAttrCand(ctx, i, ci) {
  const row = ctx.rows[i];
  if (row) addAttrTagValue(ctx, i, (row.cands || [])[ci]);
}

// 移除第 ti 个已选值
function removeAttrTag(ctx, i, ti) {
  const row = ctx.rows[i];
  if (!row) return;
  row.valueArr.splice(ti, 1);
  renderAttrTagBox(ctx, i);
}

// 属性区点击代理（每个容器只挂一次事件，行内 HTML 重渲染不影响；ctx 由绑定处闭包带入）：
//   标签 × → 移除；候选值 → 添加；点标签框空白 → 聚焦输入框
function onAttrFormClick(ctx, e) {
  const rm = e.target.closest('[data-tag-rm]');
  if (rm) { removeAttrTag(ctx, Number(rm.dataset.row), Number(rm.dataset.tagRm)); return; }
  const cd = e.target.closest('[data-cand]');
  if (cd) { addAttrCand(ctx, Number(cd.dataset.row), Number(cd.dataset.cand)); return; }
  const box = e.target.closest('.tag-box');
  if (box) { const inp = box.querySelector('.tag-input'); if (inp) inp.focus(); }
}

// 属性区回车代理：标签输入框内回车 = 添加标签（阻止默认行为）
function onAttrFormKeydown(ctx, e) {
  if (e.key !== 'Enter') return;
  const inp = e.target.closest('.tag-input');
  if (!inp) return;
  e.preventDefault();
  addAttrTagFromInput(ctx, Number(inp.dataset.row));
}

// 【bug 修复】属性区输入代理：单值输入框（.attr-value-input）输入即回写内存 row.attrValue。
// 原实现只渲染了 input 没绑事件，用户输入只改了 DOM 显示，buildAttrFormList() 提交时读的仍是
// 旧值（初始为空 → 空值行被过滤 → 第 3 步保存的是空列表 → 第 4 步无 SKU 可生成、保存按钮禁用）
function onAttrFormInput(ctx, e) {
  const inp = e.target.closest('.attr-value-input');
  if (!inp) return;
  const row = ctx.rows[Number(inp.dataset.row)];
  if (row) row.attrValue = inp.value;
}

// 「下一步」可用态：类别已确认且不在提交中（属性值为空也可提交——讲师无强制校验）
function syncAttrFormState(ctx) {
  const btn = $(ctx.btnId);
  if (!btn) return;
  const hasCat = !!resolveAttrCategoryId();
  btn.disabled = !hasCat || ctx.submitting;
  const hint = $(ctx.hintId);
  if (!hint) return;
  if (!hasCat) hint.textContent = ctx.texts.noCat;
  else if (ctx.submitting) hint.textContent = ctx.texts.busy;
  else hint.textContent = ctx.texts.idle;
}

// 第 2/3 步各自的可用态同步入口（视图侧只认这两个名字）
function syncSetGoodsAttrState() { syncAttrFormState(goodsAttrForm); }
function syncSaleAttrState() { syncAttrFormState(saleAttrForm); }

// 进入属性类视图（第 2/3 步）：解析类别 → 首次/换类别时加载属性 → 同步提交可用态
async function initAttrFormView(ctx) {
  const categoryId = resolveAttrCategoryId();
  if (!categoryId) {                            // 第 1 步没走完（无路由参数也没有兜底值）
    ctx.seq++;                                  // 作废进行中的请求
    ctx.rows = [];
    ctx.loadedFor = '';
    const box = $(ctx.containerId);
    if (box) box.innerHTML = '<div class="attr-empty">尚未确认商品类别，请先完成第 1 步「基本信息」</div>';
    syncAttrFormState(ctx);
    toast('请先完成第 1 步', false);
    return;
  }
  if (ctx.loadedFor !== String(categoryId)) {   // 同类别来回切换不重复请求（保留已填值）
    await loadAttrRows(ctx, categoryId);
  } else {
    renderAttrRows(ctx);
    syncAttrFormState(ctx);
  }
}

function initSetGoodsAttrView() { return initAttrFormView(goodsAttrForm); }   // 第 2 步入口（switchView 调用）
function initSaleAttrView() { return initAttrFormView(saleAttrForm); }       // 第 3 步入口

// 「重置表单」：清空该步骤所有已填值（属性行本身保留，候选项不丢，需重新填写）
function resetAttrForm(ctx) {
  ctx.rows.forEach((row) => {
    if (row.valueType == 2) row.valueArr = [];
    else row.attrValue = '';
  });
  renderAttrRows(ctx);
  syncAttrFormState(ctx);
  toast('表单已重置');
}

// 退出登录/会话失效时的复位（比重置更彻底：行数据与加载标记一起清，并作废进行中的请求）
function resetAttrFormState(ctx) {
  ctx.rows = [];
  ctx.loadedFor = '';
  ctx.seq++;
  ctx.submitting = false;
  const box = $(ctx.containerId);
  if (box) box.innerHTML = '';
}

// HTML 按钮入口（onclick）
function resetSetGoodsAttrForm() { resetAttrForm(goodsAttrForm); }
function resetSaleAttrForm() { resetAttrForm(saleAttrForm); }

// 汇总属性表单的提交列表：元素 = { attrId, valueType, attrValue }（第 2/3 步共用）
//   多值型 valueArr.join(';') 回字符串；空值行（单值未填 / 多值空数组）过滤 —— 讲师 pickForm 语义
function buildAttrFormList(ctx) {
  const list = [];
  for (let i = 0; i < ctx.rows.length; i++) {
    const row = ctx.rows[i];
    if (row.valueType == 2) {
      // 保存时自动接收输入框中尚未按回车确认的值，避免界面有内容但提交列表为空。
      const draftEl = $(ctx.prefix + 'TagInp' + i);
      const draft = String(draftEl ? draftEl.value : '').trim();
      if (draft && !row.valueArr.includes(draft)) row.valueArr.push(draft);
      const vals = (row.valueArr || []).filter((v) => String(v).trim() !== '');
      if (!vals.length) continue;                             // 空值行：不提交
      list.push({ attrId: row.id, valueType: row.valueType, attrValue: vals.join(';') });
    } else {
      const v = String(row.attrValue == null ? '' : row.attrValue).trim();
      if (!v) continue;                                       // 空值行：不提交
      list.push({ attrId: row.id, valueType: row.valueType, attrValue: v });
    }
  }
  return list;
}

// 提交第 2 步（讲师 savePublishData 方案）：POST /PublishGoods/saveGoodsAttrValues（JSON body）
// 成功后自动进入第 3 步「销售属性」（贴讲师 $router.push 语义）
async function saveSetGoodsAttrValues() {
  if (goodsAttrForm.submitting) return;
  const pubKey = window.localStorage.getItem('pubKey');
  if (!pubKey) { toast('请先完成第 1 步', false); return; }   // 没有发布键 = 第 1 步未暂存
  const list = buildAttrFormList(goodsAttrForm);
  goodsAttrForm.submitting = true;
  syncSetGoodsAttrState();
  try {
    const res = await apiJson('/PublishGoods/saveGoodsAttrValues', { pubKey: pubKey, attrList: list });
    // 讲师答案：后端 R.ok().put("pubKey", ...) —— 顶层 pubKey（与第 1 步一致）；缺失时沿用本地发布键
    const back = (res && res.pubKey) || pubKey;
    toast('规格参数已暂存（发布键：' + back + '）');
    switchView('saleAttr');                                   // 自动进入第 3 步
  } catch (e) { notifyError(e); }
  finally { goodsAttrForm.submitting = false; syncSetGoodsAttrState(); }
}

/* ==================== 发布商品视图（商品上架第 3 步：销售属性） ==================== */
/*
 * 与第 2 步共用属性表单引擎，仅换容器（#saleAttrFormList）/ 控件前缀（saleAttr）/ attrType=2；
 * 提交成功后：已填值的销售属性 → 内存 saleAttrRows + localStorage.saleAttr → 自动进入第 4 步「SKU 设置」
 */

// 提交第 3 步：POST /PublishGoods/saveSaleAttrValues（JSON body，契约与第 2 步 saveGoodsAttrValues 一致）
async function saveSaleAttrValues() {
  if (saleAttrForm.submitting) return;
  const pubKey = window.localStorage.getItem('pubKey');
  if (!pubKey) { toast('请先完成第 1 步', false); return; }   // 没有发布键 = 第 1 步未暂存
  const list = buildAttrFormList(saleAttrForm);
  if (saleAttrForm.rows.length && !list.length) {
    toast('请至少选择一个销售属性值', false);
    return;
  }
  saleAttrForm.submitting = true;
  syncSaleAttrState();
  try {
    const res = await apiJson('/PublishGoods/saveSaleAttrValues', { pubKey: pubKey, attrList: list });
    const back = (res && res.pubKey) || pubKey;
    // 第 4 步的 SKU 组合只认「已填值」的属性：多值取 valueArr、单值非空转单元素数组；空值属性排除
    // SKU 组合直接来自本次已成功提交的列表，避免与表单内存状态分叉。
    saleAttrRows = list.map((item) => {
      const row = saleAttrForm.rows.find((candidate) => String(candidate.id) === String(item.attrId));
      return {
        attrId: item.attrId,
        attrName: row ? row.attrName : ('属性#' + item.attrId),
        valueType: item.valueType,
        values: String(item.attrValue || '').split(';').map((value) => value.trim()).filter(Boolean)
      };
    }).filter((row) => row.values.length);
    // 刷新兜底：第 4 步直接读 localStorage.saleAttr 也能重建（讲师约定此键不清）
    window.localStorage.setItem('saleAttr', JSON.stringify(saleAttrRows));
    console.log('【发布键】pubKey：' + back + '；已填销售属性：', saleAttrRows);
    toast('销售属性已暂存（发布键：' + back + '）');
    switchView('setSku');                                     // 自动进入第 4 步
  } catch (e) { notifyError(e); }
  finally { saleAttrForm.submitting = false; syncSaleAttrState(); }
}

/* ==================== 发布商品视图（商品上架第 4 步：SKU 设置） ==================== */
/*
 * 流程：
 *   数据源 = 第 3 步的 saleAttrRows（内存优先，其次 localStorage.saleAttr —— 刷新/重新登录兜底）
 *   → 对各销售属性的 values 求笛卡尔积，一行一个 SKU 组合（例：存储[256GB,512GB] × 颜色[黑,白] → 4 行）
 *   → 逐行填写价格（必填，>0）与库存（必填，≥0）；SKU 名称由该行各值按 ';' 连接自动生成（只读展示）
 *   → 「保存 SKU」→ POST /PublishGoods/saveSkuValues（JSON：{pubKey, skuList}）
 * 保存成功后自动进入第 5 步「保存完成」（本轮补齐：POST /PublishGoods/submitPublish 一次性落库）。
 */

// 第 4 步数据源：内存 saleAttrRows 优先，其次 localStorage.saleAttr（刷新/重新登录兜底）
function readSaleAttrRows() {
  if (saleAttrRows.length) return saleAttrRows;
  try {
    const raw = window.localStorage.getItem('saleAttr');
    const arr = raw ? JSON.parse(raw) : [];
    return Array.isArray(arr) ? arr : [];
  } catch (e) {
    return [];      // 存储内容损坏：按「未选销售属性」处理（讲师约定存储不清，不做删除）
  }
}

// 维度指纹（attrId + 值列表）：重进视图时比对，一致则保留已填价格/库存，不重建组合行
function skuSignature(dims) {
  return dims.map((d) => d.attrId + ':' + (d.values || []).join(',')).join('|');
}

// 笛卡尔积：[[黑,白],[256GB,512GB]] → [[黑,256GB],[黑,512GB],[白,256GB],[白,512GB]]
// 任一维度为空数组 → 无合法组合（返回空数组，由调用方给出空态提示）
function cartesianProduct(dims) {
  if (!dims.length) return [];                 // 零维度：无组合（调用方已先行拦截空态）
  let out = [[]];                              // 从「零维组合」开始逐维展开
  for (const vals of dims) {
    if (!vals.length) return [];               // 某个属性一个值都没有 → 组合不存在
    const next = [];
    for (const pre of out) {
      for (const v of vals) next.push(pre.concat([v]));
    }
    out = next;
  }
  return out;
}

// 由销售属性维度重建 SKU 组合行（values 与 attrValues 一一对应，提交时按维度原样拆解）
function buildSkuRows(dims) {
  skuDims = dims;
  skuDimSig = skuSignature(dims);
  skuRows = cartesianProduct(dims.map((d) => d.values)).map((vals) => ({
    skuName: vals.join(';'),                                        // 自动生成，只读展示
    values: vals,
    attrValues: vals.map((v, k) => ({ attrId: dims[k].attrId, attrValue: v })),
    price: '',
    stock: '',
    skuImg: ''
  }));
}

// 渲染 SKU 表格：列 = 各销售属性值 + 价格 + 库存 + SKU 名称（行多时表格限高滚动，见 .sku-table-wrap）
function renderSkuRows() {
  const box = $('skuFormArea');
  if (!box) return;
  let h = '<p class="sku-tip">共 ' + skuRows.length + ' 个 SKU 组合（由销售属性值组合自动生成）：' +
    '价格为必填且须大于 0，库存为必填且不小于 0</p>' +
    '<div class="table-wrap sku-table-wrap"><table><thead><tr>';
  for (const d of skuDims) h += '<th>' + esc(d.attrName || ('属性#' + d.attrId)) + '</th>';
  h += '<th style="width:150px">价格（元）</th><th style="width:130px">库存</th><th style="width:170px">SKU 图片</th><th>SKU 名称</th>' +
    '</tr></thead><tbody>';
  skuRows.forEach((r, i) => {
    h += '<tr>';
    for (const v of r.values) h += '<td>' + esc(v) + '</td>';
    h += '<td><input id="skuPrice' + i + '" class="form-input sku-input" type="number" min="0" step="0.01"' +
      ' data-row="' + i + '" data-field="price" placeholder="必填" value="' + esc(r.price) + '"></td>';
    h += '<td><input id="skuStock' + i + '" class="form-input sku-input" type="number" min="0" step="1"' +
      ' data-row="' + i + '" data-field="stock" placeholder="必填" value="' + esc(r.stock) + '"></td>';
    h += '<td><input class="form-input sku-file" type="file" accept="image/*" data-sku-image-row="' + i + '">' +
      '<span class="order-sub" id="skuImageName' + i + '">' + esc(r.skuImg || '未上传') + '</span></td>';
    h += '<td class="sku-name">' + esc(r.skuName) + '</td>';
    h += '</tr>';
  });
  h += '</tbody></table></div>';
  box.innerHTML = h;
}

// SKU 输入实时回写内存：切走再回来（重渲染表格）时保留已填价格/库存
function onSkuFormInput(e) {
  const inp = e.target.closest('[data-field]');
  if (!inp) return;
  const row = skuRows[Number(inp.dataset.row)];
  if (row) row[inp.dataset.field] = inp.value;
}

async function onSkuImageChange(e) {
  const input = e.target.closest('[data-sku-image-row]');
  if (!input || !input.files || !input.files[0]) return;
  const index = Number(input.dataset.skuImageRow), row = skuRows[index];
  if (!row) return;
  try {
    input.disabled = true;
    const data = await uploadFile('/PublishGoods/upload', input.files[0], { type: 2 });
    row.skuImg = data.fileName || '';
    $('skuImageName' + index).textContent = row.skuImg || '未上传';
    toast('SKU 图片上传成功');
  } catch (err) { notifyError(err); }
  finally { input.disabled = false; }
}

// 「保存 SKU」可用态 + 底部提示（无 SKU 行 / 无发布键 / 提交中 → 禁用）
function syncSkuState() {
  const btn = $('btnSkuSave');
  if (!btn) return;
  const hasKey = !!window.localStorage.getItem('pubKey');
  btn.disabled = !skuRows.length || !hasKey || skuSubmitting;
  const hint = $('skuSubmitHint');
  if (!hint) return;
  if (!skuRows.length) hint.textContent = '请先完成第 3 步「销售属性」';
  else if (!hasKey) hint.textContent = '请先完成第 1 步「基本信息」';
  else if (skuSubmitting) hint.textContent = '正在暂存 SKU…';
  else hint.textContent = '共 ' + skuRows.length + ' 个 SKU 组合（价格/库存必填）';
}

// 进入第 4 步视图：读销售属性 → 维度变化才重建组合行 → 渲染表格
function initSkuView() {
  const box = $('skuFormArea');
  if (!box) return;
  const dims = (readSaleAttrRows() || []).filter((d) => d && d.attrId != null);
  if (!dims.length) {                          // 第 3 步未保存过（内存与 localStorage 都没有）
    skuDims = [];
    skuRows = [];
    skuDimSig = '';
    box.innerHTML = '<div class="attr-empty">请先在「销售属性」步骤选择属性值</div>';
    syncSkuState();
    return;
  }
  if (skuSignature(dims) !== skuDimSig) buildSkuRows(dims);     // 维度没变则保留已填价格/库存
  if (!skuRows.length) {                       // 某个销售属性没有值 → 笛卡尔积为空
    box.innerHTML = '<div class="attr-empty">销售属性值不完整，请返回「销售属性」步骤补充后重试</div>';
    syncSkuState();
    return;
  }
  renderSkuRows();
  syncSkuState();
}

// 第 4 步提交：POST /PublishGoods/saveSkuValues（JSON body）
//   skuList 元素 = { skuName, price, stock, skuImg, attrValues:[{attrId,attrValue}] }
//   price/stock 从输入框取 Number；任一行校验不过 → 提示并阻止提交（不发请求）
async function saveSkuValues() {
  if (skuSubmitting) return;
  const pubKey = window.localStorage.getItem('pubKey');
  if (!pubKey) { toast('请先完成第 1 步', false); return; }
  if (!skuRows.length) { toast('请先完成第 3 步「销售属性」', false); return; }
  const skuList = [];
  for (let i = 0; i < skuRows.length; i++) {
    const r = skuRows[i];
    const priceEl = $('skuPrice' + i);
    const stockEl = $('skuStock' + i);
    const priceRaw = priceEl ? String(priceEl.value).trim() : '';
    const stockRaw = stockEl ? String(stockEl.value).trim() : '';
    const price = Number(priceRaw);
    const stock = Number(stockRaw);
    // 必填 + 合法性校验：价格 > 0；库存 ≥ 0（注意空串 Number('') = 0，必须先判空串）
    if (priceRaw === '' || !Number.isFinite(price) || price <= 0) {
      toast('「' + r.skuName + '」价格未填或非法（须大于 0）', false);
      if (priceEl) priceEl.focus();
      return;
    }
    if (stockRaw === '' || !Number.isFinite(stock) || stock < 0) {
      toast('「' + r.skuName + '」库存未填或非法（须不小于 0）', false);
      if (stockEl) stockEl.focus();
      return;
    }
    skuList.push({
      skuName: r.skuName,
      price: price,
      stock: stock,
      skuImg: r.skuImg || null,
      attrValues: r.attrValues                 // 按维度拆解：每个属性的 attrId + 该行对应值
    });
  }
  skuSubmitting = true;
  syncSkuState();
  try {
    const res = await apiJson('/PublishGoods/saveSkuValues', { pubKey: pubKey, skuList: skuList });
    const back = (res && res.pubKey) || pubKey;
    toast('SKU 已暂存（发布键：' + back + '）');
    switchView('finish');                    // 自动进入第 5 步「保存完成」（衔接发布链路的最后一步）
  } catch (e) { notifyError(e); }
  finally { skuSubmitting = false; syncSkuState(); }
}

/* ==================== 发布商品视图（商品上架第 5 步：保存完成） ==================== */
/*
 * 流程：
 *   汇总本次暂存信息（发布键 / 所属类别 / 销售属性摘要，一律现读 localStorage —— 第 1、3 步保存时写入）
 *   → 「确认发布」POST /PublishGoods/submitPublish（表单 pubKey）→ 后端把 Redis 四段缓存一次性落库并清缓存
 *   → 成功：取响应**顶层 spuId** → 清 localStorage 的 pubKey/categoryId/saleAttr（发布完成）
 *     → toast「发布成功！商品 ID：xxx」→ 切「商品管理」视图并刷新列表
 *   → 失败：notifyError，三键保留（暂存还在 Redis，可原地重试）
 * 说明：清理时机严格限定在「发布成功之后」—— 失败就清会让用户丢掉整单暂存数据。
 */

// 进入第 5 步视图：确保类别树就绪（categoryId → 类别名映射要用）→ 渲染汇总卡片 + 同步按钮可用态
async function initFinishView() {
  try { await ensureCat(); } catch (e) { /* 静默：仅影响类别名展示，卡片内兜底为 类别#id */ }
  renderFinishSummary();
  syncFinishState();
}

// 渲染发布信息汇总卡片（进入视图 / 退出登录复位时调用）：三段内容全部由 localStorage 推导
function renderFinishSummary() {
  if (!$('finishPubKey')) return;
  const pubKey = window.localStorage.getItem('pubKey') || '';
  // 发布键：无 = 第 1 步未走完 → 空态提示（「确认发布」由 syncFinishState 置灰）
  $('finishPubKey').innerHTML = pubKey ? esc(pubKey)
    : '<span class="no-logo">请先完成第 1 步</span>';
  // 所属类别：categoryId（内存优先，刷新后走 localStorage 兜底）→ 类别树映射名称
  $('finishCategory').textContent = agCategoryLabel(resolveAttrCategoryId());
  // 销售属性摘要：内存镜像优先、localStorage.saleAttr 兜底（两者内容一致）→「属性名：值1 / 值2」
  const rows = readSaleAttrRows();
  const box = $('finishSaleAttr');
  if (!rows.length) {
    box.innerHTML = '未设置';
    return;
  }
  box.innerHTML = rows.map((r) => {
    const vals = Array.isArray(r.values) ? r.values : [];
    return '<div class="finish-attr">' +
      '<span class="finish-attr-name">' + esc(r.attrName || ('属性#' + r.attrId)) + '：</span>' +
      (vals.length ? esc(vals.join(' / ')) : '—') +
      '</div>';
  }).join('');
}

// 「确认发布」可用态 + 底部提示（无发布键 / 发布中 → 禁用；发布成功后键被清，按钮自动回到禁用态）
function syncFinishState() {
  const btn = $('btnFinishSubmit');
  if (!btn) return;
  const hasKey = !!window.localStorage.getItem('pubKey');
  btn.disabled = !hasKey || finishSubmitting;
  const hint = $('finishSubmitHint');
  if (!hint) return;
  if (!hasKey) hint.textContent = '请先完成第 1 步「基本信息」';
  else if (finishSubmitting) hint.textContent = '正在发布，请稍候…';
  else hint.textContent = '发布成功后将清除本次暂存（发布键 / 类别 / 销售属性）';
}

// 第 5 步提交：POST /PublishGoods/submitPublish（表单参数 pubKey，api() 默认 x-www-form-urlencoded）
async function submitPublishGoods() {
  if (finishSubmitting) return;
  const pubKey = window.localStorage.getItem('pubKey');
  if (!pubKey) { toast('请先完成第 1 步', false); return; }   // 没有发布键 = 第 1 步未暂存
  finishSubmitting = true;
  syncFinishState();
  try {
    const res = await api('/PublishGoods/submitPublish', 'POST', { pubKey: pubKey });
    // 讲师答案：后端 R.ok().put("spuId", ...) —— spuId 在响应**顶层**（与前四步的 pubKey 同一风格）
    const spuId = res && res.spuId;
    // 发布完成：清 localStorage 三键（只有成功才清 —— 失败保留三键，可原地重试）
    window.localStorage.removeItem('pubKey');
    window.localStorage.removeItem('categoryId');
    window.localStorage.removeItem('saleAttr');
    // 内存镜像同步清空：避免「已发布」的销售属性/SKU 被第 4 步视图残留显示
    saleAttrRows = [];
    skuDims = [];
    skuRows = [];
    skuDimSig = '';
    toast('发布成功！商品 ID：' + (spuId == null ? '—' : spuId));
    gPage = 1;                               // 回到第一页（从最新数据看起）
    switchView('goods');                     // 切「商品管理」：进入视图即重新拉取列表
  } catch (e) { notifyError(e); }
  finally { finishSubmitting = false; syncFinishState(); }
}

/* ==================== 商品管理视图（发布链路的最终闭环：列表 + 查看详情 + 删除） ==================== */
/*
 * 数据源：GET /Goods/list（page/limit/goodsName）分页；categoryName/brandName 后端已联表返回
 *   - 主图列复用品牌 LOGO 缩略图的处理模式（40px + onerror 就地换「无图」占位），
 *     地址 /PublishGoods/showImg/goods/{mainImg}（与第 1 步主图预览同一地址）
 *   - 「查看」→ GET /Goods/get?id= → 弹窗：基础信息（主图大图）+ 属性按 attrType 分两块 + SKU 表格
 *   - 「删除」→ confirm 二次确认 → DELETE /Goods/delete?id=（后端级联删除属性值与 SKU）→ 刷新列表
 */

async function loadGoods(page) {
  try {
    gPage = page || 1;
    const res = await api('/Goods/list', 'GET', {
      page: gPage,
      limit: PAGE_SIZE,
      goodsName: $('gInpGoodsName').value.trim()
    });
    gRows = res.data || [];
    gTotal = (res.total != null) ? Number(res.total) : gRows.length;
    renderGoodsTable();
    drawPager('goodsPager', gPage, gTotal, (p) => loadGoods(p));
  } catch (e) { notifyError(e); }
}

// 主图缩略图单元格 HTML：无文件名 → “无图”占位；有 → 40px img 直连 /PublishGoods/showImg/goods/{fileName}
// （.brand-thumb 为通用方图缩略图样式，商品侧直接复用；失败由 imgGoodsError 就地换占位）
function goodsThumbHtml(fileName) {
  if (!fileName) return '<span class="no-logo">无图</span>';
  return '<img class="brand-thumb" alt="" title="' + esc(fileName) + '" src="' + pubGoodsImgUrl(fileName) +
    '" onerror="imgGoodsError(this)">';
}

// showImg 404（文件被删/未落盘）时：把出错 <img> 就地换成“无图”占位（模式同品牌 imgLogoError）
function imgGoodsError(img) {
  const s = document.createElement('span');
  s.className = 'no-logo';
  s.textContent = '无图';
  img.replaceWith(s);
}

// 所属类别列文案：优先用后端联表返回的 categoryName，缺失时退回类别树映射（同属性组页 agCategoryLabel）
function goodsCategoryLabel(row) {
  return row.categoryName || agCategoryLabel(row.categoryId);
}

function goodsPriceText(row) {
  const min = row.minPrice == null ? null : Number(row.minPrice);
  const max = row.maxPrice == null ? null : Number(row.maxPrice);
  if (min == null || Number.isNaN(min)) return '—';
  const money = (n) => '¥' + Number(n).toFixed(2);
  return max != null && !Number.isNaN(max) && max !== min ? money(min) + '–' + money(max) : money(min);
}

function renderGoodsTable() {
  const tbody = $('goodsTbody');
  if (!gRows.length) {
    tbody.innerHTML = '<tr><td colspan="10" class="empty">暂无数据</td></tr>';
    return;
  }
  let html = '';
  gRows.forEach((r, i) => {
    html += '<tr>' +
      '<td>' + esc(r.id) + '</td>' +
      '<td>' + goodsThumbHtml(r.mainImg) + '</td>' +
      '<td>' + esc(r.goodsName) + '</td>' +
      '<td>' + esc(goodsCategoryLabel(r)) + '</td>' +
      '<td>' + esc(r.brandName || '—') + '</td>' +
      '<td class="goods-price">' + esc(goodsPriceText(r)) + '</td>' +
      '<td>' + ((r.weight == null || r.weight === '') ? '—' : esc(r.weight)) + '</td>' +
      '<td><span class="status-badge ' + (r.lifecycleStatus === '已上架' ? 'status-done' : 'status-cancelled') + '">' + esc(r.lifecycleStatus || '已上架') + '</span></td>' +
      '<td>' + esc(r.createDate || '—') + '</td>' +
      '<td class="op">' +
        '<button type="button" class="link-btn" onclick="viewGoods(' + i + ')">查看</button>' +
        '<button type="button" class="link-btn" onclick="editGoods(' + i + ')">编辑</button>' +
        '<button type="button" class="link-btn" onclick="toggleGoodsStatus(' + i + ')">' + (r.lifecycleStatus === '已上架' ? '下架' : '上架') + '</button>' +
        '<button type="button" class="link-btn danger" onclick="delGoods(' + i + ')">删除</button>' +
      '</td></tr>';
  });
  tbody.innerHTML = html;
}

function searchGoods() { loadGoods(1); }

function resetGoodsSearch() {
  $('gInpGoodsName').value = '';
  loadGoods(1);
}

// 进入商品管理视图：按当前条件重新拉取（发布成功 / 重新登录后列表可能已变）
function initGoodsView() { loadGoods(gPage); }

function viewGoods(i) {
  const row = gRows[i];
  if (row) openGoodsDetail(row.id);
}

async function delGoods(i) {
  const row = gRows[i];
  if (!row) return;
  if (!window.confirm('确定删除商品「' + (row.goodsName || row.id) + '」吗？\n' +
      '（将级联删除该商品的属性值与 SKU，操作不可恢复）')) return;
  try {
    await api('/Goods/delete', 'DELETE', { id: row.id });
    toast('删除成功');
    if (gRows.length === 1 && gPage > 1) gPage--;   // 删掉本页最后一条时回退一页
    loadGoods(gPage);
  } catch (e) { notifyError(e); }
}

/* ---------------- 商品完整编辑弹窗 ---------------- */

async function editGoods(i) {
  const row = gRows[i];
  if (!row) return;
  $('goodsEditBody').innerHTML = '<div class="attr-empty">商品数据加载中…</div>';
  openModal('goodsEditModal');
  try {
    const res = await api('/Goods/get', 'GET', { id: row.id });
    goodsEditData = res.data || {};
    goodsEditMainImg = (goodsEditData.spu || {}).mainImg || '';
    renderGoodsEdit();
    await loadGoodsEditBrandOptions((goodsEditData.spu || {}).categoryId, (goodsEditData.spu || {}).brandId);
  } catch (e) {
    $('goodsEditBody').innerHTML = '<div class="attr-empty">编辑数据加载失败：' + esc(e.message) + '</div>';
    notifyError(e);
  }
}

async function loadGoodsEditBrandOptions(categoryId, selectedId) {
  const sel = $('geBrandId');
  if (!sel || categoryId == null) return;
  try {
    const res = await api('/Brand/brandOptions/' + encodeURIComponent(categoryId), 'GET');
    const list = res.data || [];
    sel.innerHTML = list.map((o) => '<option value="' + esc(o.value) + '">' + esc(o.label) + '</option>').join('');
    sel.value = String(selectedId == null ? '' : selectedId);
  } catch (e) { notifyError(e); }
}

function renderGoodsEdit() {
  const data = goodsEditData || {};
  const spu = data.spu || {};
  const attrs = Array.isArray(data.attrValues) ? data.attrValues : [];
  const skus = Array.isArray(data.skus) ? data.skus : [];
  const nameById = {};
  attrs.forEach((a) => { nameById[a.attrId] = a.attrName || ('属性#' + a.attrId); });
  let h = '<section class="ge-section"><div class="ge-section-title">商品信息</div>' +
    '<div class="ge-base">' +
      '<div class="ge-image-panel"><div class="ge-image-preview" id="geImagePreview">' + goodsEditPreviewHtml() + '</div>' +
        '<input id="geImageFile" type="file" accept="image/*" hidden onchange="uploadGoodsEditImage(this)">' +
        '<button class="btn" type="button" onclick="$(\'geImageFile\').click()">更换主图</button>' +
        '<span class="hint">建议 1:1 方图，JPG/PNG/WebP</span></div>' +
      '<div class="ge-form-grid">' +
        geInput('geName', '商品名称', spu.goodsName, 'text') +
        '<label class="ge-field"><span>所属类别</span><input value="' + esc(spu.categoryName || agCategoryLabel(spu.categoryId)) + '" disabled><small>类别变更请重新发布商品，避免属性体系错配</small></label>' +
        '<label class="ge-field"><span>品牌</span><select id="geBrandId"><option value="">加载中…</option></select></label>' +
        geInput('geWeight', '重量（Kg）', spu.weight, 'number', '0.01') +
        geInput('geAllowDeduct', '最多抵扣积分', spu.allowDeduct, 'number', '1') +
        geInput('geRewardPoint', '返还积分', spu.rewardPoint, 'number', '1') +
        '<label class="ge-field ge-field-wide"><span>商品描述</span><textarea id="geDetails" rows="3">' + esc(spu.goodsDetails || '') + '</textarea></label>' +
      '</div></div></section>';

  h += '<section class="ge-section"><div class="ge-section-title">规格参数</div>';
  const basicAttrs = attrs.filter((a) => Number(a.attrType) === 1);
  h += basicAttrs.length ? '<div class="ge-attr-grid">' + basicAttrs.map((a, i) =>
    '<label class="ge-field"><span>' + esc(a.attrName || ('属性#' + a.attrId)) + '</span>' +
    '<input class="ge-attr-input" data-attr-index="' + attrs.indexOf(a) + '" value="' + esc(a.attrValue || '') + '"></label>').join('') + '</div>'
    : '<div class="attr-empty">暂无规格参数</div>';
  h += '</section>';

  h += '<section class="ge-section"><div class="ge-section-title">SKU 与销售属性</div>';
  if (!skus.length) h += '<div class="attr-empty">暂无 SKU</div>';
  else h += '<div class="ge-sku-list">' + skus.map((item, si) => {
    const sku = item.sku || {};
    const saleAttrs = Array.isArray(item.attrs) ? item.attrs : [];
    return '<article class="ge-sku-card" data-sku-index="' + si + '"><div class="ge-sku-head"><strong>' + esc(sku.skuName || ('SKU ' + (si + 1))) + '</strong><span class="data-code">' + esc(sku.skuCode || '') + '</span></div>' +
      '<div class="ge-sku-grid">' +
        geSkuInput(si, 'name', 'SKU 名称', sku.skuName, 'text') +
        '<label class="ge-field"><span>SKU 编码</span><input id="geSkuCode' + si + '" value="' + esc(sku.skuCode || '') + '" disabled><small>编码关联订单与库存，不允许在此变更</small></label>' +
        geSkuInput(si, 'price', '原价（元）', sku.price, 'number', '0.01') +
        geSkuInput(si, 'discount', '优惠价（元）', sku.discountPrice, 'number', '0.01') +
        geSkuInput(si, 'stock', '库存', sku.stock, 'number', '1') +
        '<label class="ge-field"><span>状态</span><select id="geSkuStatus' + si + '"><option value="启用"' + (sku.skuStatus !== '停用' ? ' selected' : '') + '>启用</option><option value="停用"' + (sku.skuStatus === '停用' ? ' selected' : '') + '>停用</option></select></label>' +
      '</div>' +
      (saleAttrs.length ? '<div class="ge-sale-attrs">' + saleAttrs.map((a, ai) => '<label class="ge-field"><span>' + esc(a.attrName || nameById[a.attrId] || ('销售属性#' + a.attrId)) + '</span><input id="geSkuAttr' + si + '_' + ai + '" value="' + esc(a.attrValue || '') + '"></label>').join('') + '</div>' : '') +
      '</article>';
  }).join('') + '</div>';
  h += '</section>';
  $('goodsEditBody').innerHTML = h;
}

function geInput(id, label, value, type, step) {
  return '<label class="ge-field"><span>' + label + '</span><input id="' + id + '" type="' + type + '"' + (step ? ' step="' + step + '"' : '') + ' value="' + esc(value == null ? '' : value) + '"></label>';
}

function geSkuInput(index, key, label, value, type, step) {
  return '<label class="ge-field"><span>' + label + '</span><input id="geSku' + key.charAt(0).toUpperCase() + key.slice(1) + index + '" type="' + type + '"' + (step ? ' step="' + step + '"' : '') + ' value="' + esc(value == null ? '' : value) + '"></label>';
}

function goodsEditPreviewHtml() {
  return goodsEditMainImg ? '<img src="' + pubGoodsImgUrl(goodsEditMainImg) + '" alt="商品主图">' : '<span class="no-logo">无图</span>';
}

async function uploadGoodsEditImage(input) {
  const file = input && input.files && input.files[0];
  if (!file) return;
  try {
    const res = await uploadFile('/PublishGoods/upload', file, { type: 1 });
    goodsEditMainImg = res.fileName || (res.data && res.data.fileName) || '';
    $('geImagePreview').innerHTML = goodsEditPreviewHtml();
    toast('主图上传成功，保存后生效');
  } catch (e) { notifyError(e); }
  finally { input.value = ''; }
}

async function saveGoodsEdit() {
  if (!goodsEditData || !goodsEditData.spu) return;
  const btn = $('goodsEditSaveBtn');
  const text = $('geName').value.trim();
  if (!text || !goodsEditMainImg) return toast('请填写商品名称并设置主图', false);
  const original = goodsEditData.spu;
  const attrs = (goodsEditData.attrValues || []).map((a, i) => ({
    attrId: a.attrId,
    valueType: a.valueType,
    attrValue: Number(a.attrType) === 1 && document.querySelector('.ge-attr-input[data-attr-index="' + i + '"]')
      ? document.querySelector('.ge-attr-input[data-attr-index="' + i + '"]').value.trim() : (a.attrValue || '')
  }));
  const skus = (goodsEditData.skus || []).map((item, si) => ({
    id: item.sku.id,
    skuName: $('geSkuName' + si).value.trim(),
    skuCode: $('geSkuCode' + si).value.trim(),
    price: Number($('geSkuPrice' + si).value),
    discountPrice: $('geSkuDiscount' + si).value === '' ? null : Number($('geSkuDiscount' + si).value),
    stock: Number($('geSkuStock' + si).value),
    skuStatus: $('geSkuStatus' + si).value,
    skuImg: goodsEditMainImg,
    attrs: (item.attrs || []).map((a, ai) => ({ attrId: a.attrId, attrValue: $('geSkuAttr' + si + '_' + ai).value.trim() }))
  }));
  if (skus.some((s) => !s.skuCode || !(s.price > 0) || s.stock < 0 || (s.discountPrice != null && (s.discountPrice <= 0 || s.discountPrice > s.price)))) {
    return toast('请检查 SKU 编码、价格、优惠价和库存', false);
  }
  const payload = {
    spu: {
      id: original.id,
      goodsName: text,
      goodsDetails: $('geDetails').value.trim(),
      categoryId: original.categoryId,
      brandId: Number($('geBrandId').value),
      weight: Number($('geWeight').value),
      mainImg: goodsEditMainImg,
      allowDeduct: Number($('geAllowDeduct').value || 0),
      rewardPoint: Number($('geRewardPoint').value || 0),
      lifecycleStatus: original.lifecycleStatus
    },
    attrValues: attrs,
    skus: skus
  };
  if (!payload.spu.brandId || !(payload.spu.weight > 0)) return toast('请选择品牌并填写正确重量', false);
  btn.disabled = true;
  btn.textContent = '保存中…';
  try {
    await apiJson('/Goods/update', payload);
    closeModal('goodsEditModal');
    toast('商品修改已保存');
    await loadGoods(gPage);
  } catch (e) { notifyError(e); }
  finally { btn.disabled = false; btn.textContent = '保存修改'; }
}

/* ---------------- 商品详情弹窗 ---------------- */

// 打开详情：先开弹窗显示加载态，再 GET /Goods/get?id=（实时取，不用列表行拼装）
async function openGoodsDetail(id) {
  const body = $('goodsDetailBody');
  if (!body) return;
  $('goodsModalTitle').textContent = '商品详情';
  body.innerHTML = '<div class="attr-empty">详情加载中…</div>';
  openModal('goodsModal');
  try {
    const res = await api('/Goods/get', 'GET', { id: id });
    renderGoodsDetail(res.data || {});
  } catch (e) {
    body.innerHTML = '<div class="attr-empty">详情加载失败：' + esc(e.message) + '</div>';
    notifyError(e);
  }
}

// 渲染详情弹窗：基础信息 + 属性两块（attrType 1=规格参数 2=销售属性）+ SKU 表格
function renderGoodsDetail(data) {
  const body = $('goodsDetailBody');
  if (!body) return;
  const spu = data.spu || {};
  const attrValues = Array.isArray(data.attrValues) ? data.attrValues : [];
  const skus = Array.isArray(data.skus) ? data.skus : [];
  goodsDetailSkus = skus;
  // attrId → attrName 映射：SKU 的 attrs 只带 attrId，用于把规格明细显示成「属性名：值」
  const attrNameById = {};
  for (const a of attrValues) attrNameById[a.attrId] = a.attrName || ('属性#' + a.attrId);

  let h = '';
  // ① 基础信息：左主图大图 + 右字段列表（描述附在下方，保留换行）
  h += '<div class="gd-section">' +
    '<div class="gd-title">基础信息</div>' +
    '<div class="gd-base">' +
      '<div class="gd-img">' + (spu.mainImg
        ? '<img src="' + pubGoodsImgUrl(spu.mainImg) + '" alt="商品主图" onerror="imgGoodsError(this)">'
        : '<span class="no-logo">无图</span>') + '</div>' +
      '<ul class="gd-fields">' +
        gdFieldHtml('商品名称', spu.goodsName) +
        gdFieldHtml('所属类别', spu.categoryName || agCategoryLabel(spu.categoryId)) +
        gdFieldHtml('品牌', spu.brandName) +
        gdFieldHtml('销售价格', goodsPriceText(spu)) +
        gdFieldHtml('重量(Kg)', spu.weight) +
        gdFieldHtml('允许抵扣积分', spu.allowDeduct) +
        gdFieldHtml('返还积分', spu.rewardPoint) +
        gdFieldHtml('创建日期', spu.createDate) +
      '</ul>' +
    '</div>' +
    (spu.goodsDetails ? '<div class="gd-desc">' + esc(spu.goodsDetails) + '</div>' : '') +
    '</div>';

  // ② 属性两块：按 attrType 分表（1=规格参数/基本属性，2=销售属性）
  h += '<div class="gd-section">' +
    '<div class="gd-title">规格参数与销售属性</div>' +
    gdAttrBlockHtml('规格参数（基本属性）', attrValues.filter((a) => Number(a.attrType) === 1)) +
    gdAttrBlockHtml('销售属性', attrValues.filter((a) => Number(a.attrType) === 2)) +
    '</div>';

  // ③ SKU 表格：名称 / 价格 / 库存 / 规格明细（明细按 attrId 映射属性名，映射不到直接显示 attrId）
  h += '<div class="gd-section"><div class="gd-title">SKU 列表（' + skus.length + '）</div>';
  if (!skus.length) {
    h += '<div class="attr-empty">暂无 SKU 数据</div>';
  } else {
    h += '<div class="table-wrap"><table><thead><tr>' +
      '<th>SKU 名称</th><th>编码</th><th style="width:110px">价格（元）</th><th style="width:90px">库存</th><th>状态</th><th>规格明细</th><th>操作</th>' +
      '</tr></thead><tbody>';
    for (const item of skus) {
      const sku = item.sku || {};
      const attrs = Array.isArray(item.attrs) ? item.attrs : [];
      const detail = attrs.map((at) => (at.attrName || attrNameById[at.attrId] || at.attrId) + '：' +
        (at.attrValue == null ? '' : at.attrValue)).join('；') || '—';
      h += '<tr>' +
        '<td class="sku-name">' + esc(sku.skuName || '—') + '</td>' +
        '<td class="data-code">' + esc(sku.skuCode || '—') + '</td>' +
        '<td>' + esc(sku.price == null ? '—' : sku.price) + '</td>' +
        '<td>' + esc(sku.stock == null ? '—' : sku.stock) + '</td>' +
        '<td>' + esc(sku.skuStatus || '启用') + '</td>' +
        '<td>' + esc(detail) + '</td>' +
        '<td><button class="btn" type="button" onclick="editSkuFromDetail(' + skus.indexOf(item) + ')">编辑</button></td>' +
        '</tr>';
    }
    h += '</tbody></table></div>';
  }
  h += '</div>';

  body.innerHTML = h;
}

// 属性小块：小标题 + 「属性名 | 值」两列表格（无数据给空态）
function gdAttrBlockHtml(title, list) {
  let h = '<div class="gd-sub">' + esc(title) + '</div>';
  if (!list.length) return h + '<div class="attr-empty">暂无</div>';
  h += '<div class="table-wrap"><table><thead><tr>' +
    '<th style="width:220px">属性名</th><th>值</th></tr></thead><tbody>';
  for (const a of list) {
    h += '<tr><td>' + esc(a.attrName || ('属性#' + a.attrId)) + '</td>' +
      '<td>' + esc((a.attrValue == null || a.attrValue === '') ? '—' : a.attrValue) + '</td></tr>';
  }
  return h + '</tbody></table></div>';
}

// 详情弹窗「字段名 + 值」行（空值统一显示 —）
function gdFieldHtml(label, val) {
  const v = (val == null || val === '') ? '—' : String(val);
  return '<li><span class="gd-label">' + esc(label) + '</span>' +
    '<span class="gd-value">' + esc(v) + '</span></li>';
}

/* ==================== 经营概览（统一业务数据） ==================== */
const DASH_CHART_IDS = ['deptBarChart', 'quarterBarChart', 'monthLineChart', 'productPieChart'];
// 图表使用一组克制但可区分的分类色；红色只留给真正的异常状态。
const DASH_BLUE = '#667a52';
const DASH_COLORS = ['#667a52', '#f18742', '#7397bd', '#a58a68', '#97a884', '#e0a65f', '#9a7fa0'];
const DASH_INK = '#60675c';          // 轴/图例文字（次级文字色）
const DASH_LINE = '#e8eae3';         // 网格线/轴线（弱化，与 .card 边框同色）

// echarts 是否可用（本地 js/vendor/echarts.min.js 未加载时降级提示，不影响其它视图）
function dashEchartsReady() { return typeof echarts !== 'undefined' && !!echarts; }

function dashMetricIcon(type) {
  const icons = {
    sales: '<path d="M4 7h16v12H4z"/><path d="M4 10h16M8 15h3"/>',
    order: '<path d="M6 3h12v18l-3-2-3 2-3-2-3 2z"/><path d="M9 8h6M9 12h6"/>',
    member: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
    warning: '<path d="M10.3 2.9 1.8 17a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 2.9a2 2 0 0 0-3.4 0z"/><path d="M12 9v4M12 17h.01"/>',
    sku: '<path d="m21 8-9-5-9 5 9 5z"/><path d="m3 8 9 5 9-5v8l-9 5-9-5z"/><path d="M12 13v8"/>',
    stock: '<path d="M3 10 12 4l9 6"/><path d="M5 9v11h14V9M8 13h8M8 17h8"/>',
    cost: '<circle cx="9" cy="8" r="5"/><path d="M7 8h4M9 6v4M14 12a5 5 0 1 1-5 5"/>',
    blocked: '<circle cx="12" cy="12" r="9"/><path d="m8 8 8 8"/>',
    vip: '<path d="M3 7h18l-3 11H6z"/><path d="m3 7 4-4 5 4 5-4 4 4M8 13h8"/>',
    gold: '<path d="m12 2 3 6 6.5 1-4.7 4.6 1.1 6.4-5.9-3.1L6.1 20l1.1-6.4L2.5 9 9 8z"/>',
    views: '<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z"/><circle cx="12" cy="12" r="2.5"/>',
    visitor: '<circle cx="9" cy="7" r="4"/><path d="M2 21v-2a7 7 0 0 1 14 0v2M19 8v6M16 11h6"/>',
    session: '<path d="M21 15a4 4 0 0 1-4 4H8l-5 3v-7a5 5 0 0 1-1-3V7a4 4 0 0 1 4-4h11a4 4 0 0 1 4 4z"/><path d="M7 9h10M7 13h6"/>',
    purchase: '<circle cx="9" cy="20" r="1"/><circle cx="18" cy="20" r="1"/><path d="M2 3h3l2.4 11.2a2 2 0 0 0 2 1.6h7.9a2 2 0 0 0 2-1.6L21 7H6"/>',
    average: '<path d="M5 20V10M12 20V4M19 20v-7"/><path d="M3 20h18"/>',
    refund: '<path d="M3 7v6h6"/><path d="M5.5 15a8 8 0 1 0 1-9L3 9"/><path d="M9 10h6M12 7v6"/>',
    review: '<path d="M21 15a4 4 0 0 1-4 4H8l-5 3v-7a5 5 0 0 1-1-3V7a4 4 0 0 1 4-4h11a4 4 0 0 1 4 4z"/><path d="m12 7 1.2 2.4 2.8.4-2 2 .5 2.8-2.5-1.3-2.5 1.3.5-2.8-2-2 2.8-.4z"/>',
    rating: '<path d="m12 2 3 6 6.5 1-4.7 4.6 1.1 6.4-5.9-3.1L6.1 20l1.1-6.4L2.5 9 9 8z"/>',
    positive: '<circle cx="12" cy="12" r="9"/><path d="M8 12l2.5 2.5L16 9"/>',
    response: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    message: '<path d="M21 15a4 4 0 0 1-4 4H8l-5 3v-7a5 5 0 0 1-1-3V7a4 4 0 0 1 4-4h11a4 4 0 0 1 4 4z"/><path d="M7 9h10M7 13h7"/>'
  };
  return '<span class="metric-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + (icons[type] || icons.sales) + '</svg></span>';
}

// 每日成交金额：面积折线更适合阅读连续日期的趋势变化。
function dashTrendOption(cats, vals) {
  return {
    grid: { top: 28, bottom: 48, left: 58, right: 20 },
    tooltip: { trigger: 'axis', valueFormatter: (value) => '¥ ' + Number(value || 0).toFixed(2) },
    xAxis: {
      type: 'category',
      data: cats,
      boundaryGap: false,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: DASH_LINE } },
      axisLabel: { color: DASH_INK, hideOverlap: true, rotate: 30 }
    },
    yAxis: {
      type: 'value',
      name: '元',
      nameTextStyle: { color: DASH_INK },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: DASH_INK },
      splitLine: { lineStyle: { color: DASH_LINE } }
    },
    series: [{
      name: '成交金额',
      type: 'line',
      data: vals,
      smooth: 0.32,
      symbol: 'circle',
      symbolSize: 7,
      showSymbol: false,
      lineStyle: { color: DASH_BLUE, width: 3 },
      itemStyle: { color: '#f18742', borderColor: '#fff', borderWidth: 2 },
      emphasis: { focus: 'series', scale: true },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
        { offset: 0, color: 'rgba(102,122,82,.34)' },
        { offset: 1, color: 'rgba(102,122,82,.03)' }
      ] } }
    }]
  };
}

// 订单状态：横向排行便于直接比较状态数量，并为每个状态保留稳定色彩。
function dashStatusOption(cats, vals) {
  return {
    grid: { top: 18, bottom: 30, left: 76, right: 38 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (value) => value + ' 单' },
    xAxis: { type: 'value', minInterval: 1, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: DASH_INK }, splitLine: { lineStyle: { color: DASH_LINE } } },
    yAxis: { type: 'category', inverse: true, data: cats, axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: DASH_INK } },
    series: [{ name: '订单数量', type: 'bar', barWidth: 16, showBackground: true,
      backgroundStyle: { color: '#f1f2ee', borderRadius: 8 },
      label: { show: true, position: 'right', color: DASH_INK, formatter: '{c}' },
      data: vals.map((value, index) => ({ value, itemStyle: { color: DASH_COLORS[index % DASH_COLORS.length], borderRadius: 8 } })) }]
  };
}

// 品类成交额：矩形树图同时表达金额大小与品类构成，避免重复使用柱状图。
function dashCategoryOption(rows) {
  return {
    color: DASH_COLORS,
    tooltip: { formatter: (p) => p.name + '<br/>成交额：¥ ' + Number(p.value || 0).toFixed(2) },
    series: [{ type: 'treemap', roam: false, nodeClick: false, breadcrumb: { show: false },
      top: 12, left: 12, right: 12, bottom: 12,
      label: { show: true, color: '#fff', fontSize: 12, lineHeight: 18,
        formatter: (p) => p.name + '\n¥ ' + Number(p.value || 0).toLocaleString('zh-CN') },
      upperLabel: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 3, gapWidth: 3, borderRadius: 7 },
      data: rows.map((x, index) => ({ name: x.name, value: Number(x.value || 0), itemStyle: { color: DASH_COLORS[index % DASH_COLORS.length] } })) }]
  };
}

// 窗口缩放：所有已建图表跟随 resize（未创建时为空操作）
function resizeDashboardCharts() {
  Object.keys(dashCharts).forEach((k) => {
    const c = dashCharts[k];
    if (c && c.resize) c.resize();
  });
}

// 销毁全部图表实例（退出登录 / 会话失效清空时调用，避免实例与监听残留）
function disposeDashboardCharts() {
  Object.keys(dashCharts).forEach((k) => {
    const c = dashCharts[k];
    if (c && c.dispose) c.dispose();
  });
  dashCharts = {};
}

// 经营概览只读取真实业务接口；图表容器首次进入时初始化，之后复用实例。
async function initDashboard() {
  if (!dashEchartsReady()) {
    toast('ECharts 未加载：缺少 js/vendor/echarts.min.js', false);
    return;
  }
  if (!dashCharts.deptBarChart) {
    DASH_CHART_IDS.forEach((id) => {
      const el = $(id);
      if (el) dashCharts[id] = echarts.init(el);
    });
  } else {
    resizeDashboardCharts();       // 视图隐藏期间容器尺寸可能变化：进入时校正
  }
  try {
    const data = (await api('/Analysis/overview')).data || {};
    const summary = data.summary || {};
    const metricNode = $('dashMetrics');
    if (metricNode) metricNode.innerHTML = [
      ['累计成交额', '¥ ' + Number(summary.salesAmount || 0).toFixed(2), '', 'sales'],
      ['订单总数', Number(summary.orderCount || 0), '单', 'order'],
      ['正常会员', Number(summary.memberCount || 0), '人', 'member'],
      ['库存预警', Number(summary.warningCount || 0), '个', 'warning']
    ].map((item) => '<div class="metric-item metric-item-with-icon"><span class="metric-label">' + esc(item[0]) +
      '</span><strong class="metric-value">' + esc(item[1]) +
      (item[2] ? '<span class="metric-unit">' + esc(item[2]) + '</span>' : '') + '</strong>' + dashMetricIcon(item[3]) + '</div>').join('');
    const trend = data.salesTrend || [], statuses = data.orderStatus || [];
    const categories = data.categorySales || [], products = data.topProducts || [];
    dashCharts.deptBarChart.setOption(dashTrendOption(trend.map((x) => x.name), trend.map((x) => Number(x.value || 0))), true);
    dashCharts.quarterBarChart.setOption(dashStatusOption(statuses.map((x) => x.name), statuses.map((x) => Number(x.value || 0))), true);
    dashCharts.monthLineChart.setOption(dashCategoryOption(categories), true);
    const productTotal = products.reduce((sum, x) => sum + Number(x.value || 0), 0);
    dashCharts.productPieChart.setOption({
      color: DASH_COLORS,
      tooltip: { trigger: 'item', formatter: '{b}<br/>销量：{c} 件（{d}%）' },
      legend: { orient: 'vertical', right: 14, top: 'middle', width: '47%', itemWidth: 10, itemHeight: 10, itemGap: 12,
        textStyle: { color: DASH_INK, fontSize: 11 },
        formatter: (name) => { const row = products.find((x) => x.name === name); const shortName = name.length > 12 ? name.slice(0, 12) + '…' : name; return shortName + '  ' + Number(row ? row.value : 0) + '件'; } },
      graphic: [{ type: 'group', left: '25%', top: '42%', children: [
        { type: 'text', left: 'center', style: { text: String(productTotal), fill: '#20241e', font: '700 22px Segoe UI, Microsoft YaHei' } },
        { type: 'text', left: 'center', top: 29, style: { text: '总销量（件）', fill: DASH_INK, font: '11px Segoe UI, Microsoft YaHei' } }
      ] }],
      series: [{ type: 'pie', radius: ['42%', '68%'], center: ['29%', '52%'], avoidLabelOverlap: true,
        label: { show: false }, labelLine: { show: false }, emphasis: { scaleSize: 6 },
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 5 },
        data: products.map((x) => ({ name: x.name, value: Number(x.value || 0) })) }]
    }, true);
  } catch (e) { notifyError(e); }
}

/* ==================== 分页条（用户/角色/品牌共用） ==================== */

// elId 容器、page 当前页、total 总数、onGo 跳转回调；显示 总数 + 上一页/下一页 + 第 x/y 页
function drawPager(elId, page, total, onGo) {
  const totalPage = total ? Math.max(1, Math.ceil(total / PAGE_SIZE)) : 1;
  const cur = Math.min(Math.max(1, page), totalPage);
  const el = $(elId);
  el.innerHTML =
    '<span class="pager-info">共 ' + total + ' 条</span>' +
    '<button type="button" class="btn" id="pgPrev"' + (cur <= 1 ? ' disabled' : '') + '>上一页</button>' +
    '<span class="pager-info">第 ' + cur + ' / ' + totalPage + ' 页</span>' +
    '<button type="button" class="btn" id="pgNext"' + (cur >= totalPage ? ' disabled' : '') + '>下一页</button>';
  el.querySelector('#pgPrev').onclick = function () { onGo(cur - 1); };
  el.querySelector('#pgNext').onclick = function () { onGo(cur + 1); };
}

/* ==================== 视图（菜单）切换 ==================== */

// 视图名 → 顶部栏页面标题（菜单一级项即视图；品牌/类别/发布商品/属性组/商品管理挂在「商品管理」分组下）
const VIEW_TITLES = {
  dashboard: '经营概览',
  user: '后台账号',
  dept: '部门管理',
  role: '角色权限',
  brand: '品牌管理',
  category: '类目管理',
  publish: '发布商品',
  attrGroup: '属性组管理',
  goods: '商品列表',                    // 商品列表（发布链路的最终闭环）
  order: '订单与售后',
  inventory: '库存管理',
  activity: '活动管理',
  member: '会员管理',
  advertisement: '广告管理',
  analysis: '分析中心',
  setGoodsAttr: '设置商品属性',         // 上架第 2 步：无独立菜单项，由第 1 步保存成功后进入
  saleAttr: '销售属性',                 // 上架第 3 步：无独立菜单项，由第 2 步保存成功后进入
  setSku: 'SKU设置',                    // 上架第 4 步：无独立菜单项，由第 3 步保存成功后进入
  finish: '保存完成'                    // 上架第 5 步：无独立菜单项，由第 4 步保存成功后进入
};

// 视图 → 菜单高亮项：第 2~5 步都属于发布商品流程，高亮其上级菜单
const MENU_VIEW_ALIAS = {
  setGoodsAttr: 'publish', saleAttr: 'publish', setSku: 'publish', finish: 'publish'
};

function switchView(name) {
  Object.keys(VIEW_TITLES).forEach((v) => { $('view-' + v).hidden = (v !== name); });
  const menuName = MENU_VIEW_ALIAS[name] || name;
  document.querySelectorAll('#menuNav .menu-item').forEach((m) => {
    m.classList.toggle('active', m.dataset.view === menuName);
  });
  const activeMenu = document.querySelector('#menuNav .menu-item.active');
  const activeSection = activeMenu && activeMenu.closest('details');
  if (activeSection) activeSection.open = true;
  $('pageTitle').textContent = VIEW_TITLES[name] || name;
  // 发布商品：进入时确保类别树已加载，并按最新类别树刷新三级联动选项
  if (name === 'publish') initPublishView();
  // 上架第 2/3 步：容器可见后才渲染属性行（期间可能弹「请先完成第 1 步」提示）
  if (name === 'setGoodsAttr') initSetGoodsAttrView();
  if (name === 'saleAttr') initSaleAttrView();
  // 上架第 4 步：容器可见后按销售属性重建/渲染 SKU 表格
  if (name === 'setSku') initSkuView();
  // 上架第 5 步：渲染发布信息汇总卡片（内部先确保类别树，用于 categoryId → 类别名映射）
  if (name === 'finish') initFinishView();
  // 商品管理：进入即按当前搜索条件重新拉取列表（发布成功后也会走到这里刷新）
  if (name === 'goods') initGoodsView();
  if (name === 'order') initOrderView();
  if (name === 'inventory') initInventoryView();
  if (name === 'activity') initActivityView();
  if (name === 'member') initMemberView();
  if (name === 'advertisement') initAdvertisementView();
  if (name === 'analysis') initAnalysisView();
  if (name === 'specAttr') initPhase1AttrView(1);
  if (name === 'saleAttrManage') initPhase1AttrView(2);
  // 数据概览：容器可见后才 init 图表（隐藏容器 init 会得到 0×0 尺寸）
  if (name === 'dashboard') initDashboard();
}

/* ==================== 启动流程 ==================== */

function init() {
  // 左侧菜单切换视图（原三页签逻辑迁移至此，品牌/类别为新视图）
  document.querySelectorAll('#menuNav .menu-item').forEach((m) => {
    m.addEventListener('click', () => switchView(m.dataset.view));
  });
  // 第 2/3 步属性行的交互统一用事件代理挂在各自容器上：行内 HTML 每次重渲染都不会丢事件
  // （闭包带入 ctx，两个容器的 data-row 行号互不干扰；动态控件 id 亦按前缀区分）
  [goodsAttrForm, saleAttrForm].forEach((ctx) => {
    const box = $(ctx.containerId);
    if (!box) return;
    box.addEventListener('click', (e) => onAttrFormClick(ctx, e));
    box.addEventListener('keydown', (e) => onAttrFormKeydown(ctx, e));
    // 【bug 修复】单值输入框输入即回写内存（与 SKU 表格 onSkuFormInput 同款事件代理）
    box.addEventListener('input', (e) => onAttrFormInput(ctx, e));
  });
  // 第 4 步 SKU 表格：输入即回写内存，切走再回来不丢已填值
  const skuBox = $('skuFormArea');
  if (skuBox) skuBox.addEventListener('input', onSkuFormInput);
  if (skuBox) skuBox.addEventListener('change', onSkuImageChange);
  // 数据概览图表跟随窗口缩放（未创建实例时为空操作）
  window.addEventListener('resize', resizeDashboardCharts);
  // 弹窗：右上角 ✕ / 遮罩点击 / Esc 均可关闭（登录前隐藏，无副作用）
  MODAL_IDS.forEach((id) => {
    const m = $(id);
    m.querySelector('.modal-close').onclick = () => closeModal(id);
    m.addEventListener('click', (e) => { if (e.target === m) closeModal(id); });
  });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeTopModal(); });
  boot();
}

// 启动：先 GET /Auth/check —— 已登录直接进主界面并初次加载数据；未登录显示登录视图
async function boot() {
  try {
    const res = await api('/Auth/check', 'GET');
    if (res.data) {
      enterApp(res.data);
    } else {
      showLoginView();          // 后端明确返回 data=null：未登录
    }
  } catch (e) {
    showLoginView();
    if (!sessionKicked) toast(e.message, false);   // 网络异常等：提示但不阻塞进入登录屏
  }
}

document.addEventListener('DOMContentLoaded', init);
