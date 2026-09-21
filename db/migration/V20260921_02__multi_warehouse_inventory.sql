-- Distribute the existing SKU inventory across five regional fulfillment warehouses.
-- One SKU still belongs to one warehouse, preserving the current order and stock-flow model.
UPDATE tbl_inventory
SET warehouse = CASE
  WHEN sku_code LIKE 'SKU-IP16-%' OR sku_code LIKE 'SKU-MIDEA-%' OR sku_code LIKE 'SKU-ANTA-%' THEN '广州中心仓'
  WHEN sku_code LIKE 'SKU-YOGA14-%' OR sku_code LIKE 'SKU-THERMOS-%' OR sku_code LIKE 'SKU-LOREAL-%' THEN '华东上海仓'
  WHEN sku_code LIKE 'SKU-MB14-%' OR sku_code LIKE 'SKU-LOCK-%' OR sku_code LIKE 'SKU-PROYA-%' THEN '华北北京仓'
  WHEN sku_code LIKE 'SKU-MATE70-%' OR sku_code LIKE 'SKU-SUPOR-%' THEN '西南成都仓'
  WHEN sku_code LIKE 'SKU-LINING-%' THEN '华中武汉仓'
  ELSE warehouse
END;
