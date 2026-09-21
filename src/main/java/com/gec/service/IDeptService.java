package com.gec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.domain.entity.Node;
import com.gec.domain.entity.Dept;

import java.util.List;

public interface IDeptService extends IService<Dept> {

    /** 部门树: 平铺数据转树形 List<Node>(根为顶层部门, 带 level/hasSub/children) */
    List<Node> listDept();

    /** 删除部门(部门下有人或有子部门时拒绝) */
    void deleteDept(Integer id);

    /** 新增部门(自动生成 p_ids) */
    void addDept(Dept dept);

    /** 修改部门(改了父部门时, p_ids 级联重算到整个分支) */
    void updateDept(Dept dept);

    /** 按 id 查部门详情 */
    Dept getDept(Integer id);

}