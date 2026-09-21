-- Phase 1 foundation. Apply once to the mall_sys database.
ALTER TABLE tbl_user
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用,0=禁用' AFTER dept_id;

CREATE INDEX idx_user_dept ON tbl_user(dept_id);
CREATE INDEX idx_attr_category_type ON tbl_goods_attr(category_id, attr_type);
CREATE INDEX idx_attr_group_category ON tbl_goods_attr_group(category_id);
CREATE INDEX idx_spu_category_brand ON tbl_spu_detail(category_id, brand_id);
