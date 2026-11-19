package com.zifang.z.asset.web.api;

import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产 REST 面的业务契约自守卫：每个端点都要说清楚自己是干什么的。
 */
class AssetControllerContractTest {

    @Test
    @DisplayName("每个端点都带 @BizApi，且有摘要")
    void everyEndpointHasBizApi() {
        int count = 0;
        for (Method method : AssetController.class.getDeclaredMethods()) {
            if (method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class) == null
                    && method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class) == null) {
                continue;
            }
            BizApi api = method.getAnnotation(BizApi.class);
            assertNotNull(api, "端点 " + method.getName() + " 缺 @BizApi");
            assertTrue(!api.summary().isEmpty(), "端点 " + method.getName() + " 缺摘要");
            assertTrue(api.tags().length > 0, "端点 " + method.getName() + " 缺业务标签");
            count++;
        }
        assertEquals(5, count, "端点数变了要同步 README 的 API 表");
    }

    @Test
    @DisplayName("到期提醒端点的天数参数带业务名与位置")
    void expiringParamIsDescribed() throws Exception {
        Method expiring = AssetController.class.getMethod("expiring", int.class);
        BizParam[] params = expiring.getParameters()[0].getAnnotationsByType(BizParam.class);
        assertEquals(1, params.length);
        assertEquals("提前天数", params[0].name());
        assertEquals(BizParam.Position.QUERY, params[0].position());
    }
}
