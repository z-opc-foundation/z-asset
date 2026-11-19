package com.zifang.z.asset.core;

import com.zifang.z.asset.api.enums.AssetLifecycle;
import com.zifang.z.asset.core.domain.entity.ZAssetDO;
import com.zifang.z.asset.core.service.AssetLifecycleService;
import com.zifang.z.asset.core.service.AssetService;
import com.zifang.z.meta.api.annotation.BizCause;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizMethod;
import com.zifang.z.meta.api.annotation.BizModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产模型口径写在代码里，不写在文档里 —— 自守卫。
 *
 * <p>注解是元数据的唯一来源：实体不标 {@code @BizModel}，宿主里的 z-meta 就采不到它，
 * 认知板块那边也就没有这台机器。因而「标了没有」得有人盯。</p>
 */
class AssetMetadataAnnotationTest {

    @Test
    @DisplayName("资产实体带 @BizModel，口径写在代码里")
    void entityIsAnnotated() {
        BizModel model = ZAssetDO.class.getAnnotation(BizModel.class);
        assertNotNull(model, "ZAssetDO 必须带 @BizModel，否则采不到");
        assertEquals("企业资产", model.name());
        assertEquals("asset", model.module());
    }

    @Test
    @DisplayName("每个字段都带 @BizField，没有裸字段")
    void everyFieldIsAnnotated() {
        int annotated = 0;
        for (Field field : ZAssetDO.class.getDeclaredFields()) {
            BizField bizField = field.getAnnotation(BizField.class);
            assertNotNull(bizField, "字段 " + field.getName() + " 缺 @BizField");
            assertTrue(!bizField.name().isEmpty(), "字段 " + field.getName() + " 的业务名不能为空");
            annotated++;
        }
        assertEquals(18, annotated, "字段数变了要同步 README 的主张");
    }

    @Test
    @DisplayName("状态机带业务语义，流转方法带 @BizMethod 与 @BizCause")
    void serviceIsAnnotated() throws Exception {
        assertNotNull(AssetService.class.getAnnotation(BizCause.class), "服务类应声明整体效果");

        Method transition = AssetService.class.getMethod("transition",
                String.class, AssetLifecycle.class, String.class, String.class);
        BizMethod method = transition.getAnnotation(BizMethod.class);
        assertNotNull(method, "流转方法应带 @BizMethod");
        assertEquals("asset", method.module());
        assertEquals(BizMethod.Operation.ACTION, method.operation());
        assertNotNull(transition.getAnnotation(BizCause.class), "流转方法应声明它的因果效果");
    }

    @Test
    @DisplayName("到期扫描是幂等只读")
    void queryMethodIsIdempotent() throws Exception {
        Method listExpiring = AssetService.class.getMethod("listExpiring", int.class);
        BizMethod method = listExpiring.getAnnotation(BizMethod.class);
        assertNotNull(method);
        assertEquals(BizMethod.Operation.QUERY, method.operation());
        assertTrue(method.idempotent(), "只读查询应标幂等");
    }
}
