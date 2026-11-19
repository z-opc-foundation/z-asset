package com.zifang.z.asset.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.asset.core.domain.entity.ZAssetDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业资产 Mapper。
 *
 * @author zifang
 * @since 1.1.0
 */
@Mapper
public interface ZAssetMapper extends BaseMapper<ZAssetDO> {
}
