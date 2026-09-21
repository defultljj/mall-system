# 数据库变更

开发库为 MySQL 8 的 `mall_sys`。迁移脚本按文件名顺序手工执行，每个脚本只执行一次。

当前阶段：

1. 执行 `migration/V20260917_01__phase1_foundation.sql`。
2. 启动 Redis 后运行应用测试。
3. 不删除现有业务数据；后续结构变更继续新增迁移脚本。

## 演示数据脚本（按顺序执行）

本目录下另有三个**可重复执行**的演示数据脚本，顺序不可颠倒：

| 顺序 | 脚本 | 作用 |
| --- | --- | --- |
| 1️⃣ | `mall_sys_full_demo.sql` | 基础演示数据：类目、品牌、商品、SKU、库存、会员、订单、行为、评价、客服等 |
| 2️⃣ | `mall_sys_expand_demo.sql` | 在基础数据上扩充到 85 商品 / 49 品牌 / 234 SKU / 300 订单 / 1007 行为等规模，并补齐规格参数与销售属性 |
| 3️⃣ | `mall_sys_local_analysis_seed.sql` | 模拟一次 ETL 批次产出（可选）：写入 `etl_batch` 与 `tbl_*_clean_detail` / `tbl_*_metric_daily`，让「数据中心 → 分析中心」的 5 个看板在**没有 Hadoop/Hive 环境**时也有数据可展示 |

```bash
mysql -uroot -p --default-character-set=utf8mb4 mall_sys < db/mall_sys_full_demo.sql
mysql -uroot -p --default-character-set=utf8mb4 mall_sys < db/mall_sys_expand_demo.sql
mysql -uroot -p --default-character-set=utf8mb4 mall_sys < db/mall_sys_local_analysis_seed.sql   # 可选
```

> - **必须先跑 `mall_sys_full_demo.sql`**：`expand` 依赖它建立的基础数据池。**直接跑 `expand` 不会报错、末尾对账也显示 6 项为 0，但会得到一个残缺的库**（只有脚本自己造的数据），务必按顺序执行。
> - 三个脚本重复执行后**数据内容一致**（仅 `tbl_inventory_flow` / `tbl_points_flow` 的自增主键会增长）；执行前仍建议备份数据库。
> - `expand` 的删除范围按脚本自身生成的数据特征判定（如 `sku_code REGEXP '^SKU-[0-9]{4}-'`、`user_id` 生成规则、固定 `created_at` 等），**不会影响你新增的会员、商品、SKU、库存、订单、行为日志、广告、活动、类目**。
>   两类例外需要知道：
>   1. 你在**脚本新建的那 14 个类目**上新增的属性 / 属性分组、以及给脚本那 73 个商品新增的属性值，重跑时会被一并重建 —— 建议重跑后再做这类维护；
>   2. 若你的数据恰好占用了脚本要用的 id（例如跑 `expand` 前就注册过会员、正好占了会员 id 25），脚本会**报 `Duplicate entry ... for key ...PRIMARY` 并中止**（**不会删除你的数据**，这是刻意的"显式失败"）；按报错行号清掉该行后重跑即可，实测可恢复。
> - 执行方式用 `mysql ... < 脚本文件`，**不要加 `--force`**：否则会跳过错误继续执行，留下难以排查的中间状态。
> - 图片资源见 `image_manifest.tsv`（文件名 / 类型 / 名称 / 搜索关键词），与 `upload/` 下的商品图、品牌 logo 对应（该目录还含应用运行时上传的图片，不在清单内）。
> - `mall_sys_local_analysis_seed.sql` 写入的是**模拟批次**；将来真跑通 Hive 链路后，新批次的 `published_at` 更晚，分析页面会自动切换到真实批次。
