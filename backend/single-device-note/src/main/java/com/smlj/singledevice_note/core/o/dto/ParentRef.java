package com.smlj.singledevice_note.core.o.dto;

// 我想给这种[{id: 1， parent_id: 0}] 这种数组设计一个通用的父类，比如class parentX<T> { T id; T parent_id;} 然后后续这类对象全部集成parentX, 然后利用parentX去构建部门组织架构，菜单组织架构这种树形组件， 不知道怎么去设计，而且我不满意parentX的命名， 不满意现在id和parent_id是定死的名字, 用java,js分别实现一下
public interface ParentRef<ID> {
    ID nodeId();

    ID parentNodeId();
}

//public class Dept implements ParentRef<Long> {
//    private Long deptId;
//    private Long parentDeptId;
//    private String name;
//
//    @Override
//    public Long nodeId() {
//        return deptId;
//    }
//
//    @Override
//    public Long parentNodeId() {
//        return parentDeptId;
//    }
//}
