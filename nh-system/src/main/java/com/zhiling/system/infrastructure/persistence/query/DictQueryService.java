package com.zhiling.system.infrastructure.persistence.query;

import com.zhiling.model.vo.DictItemVo;

import java.util.List;

/**
 * 字典查询服务接口。
 *
 * @author zhanghongyu
 */
public interface DictQueryService {

    /**
     * 根据字典类型编码查询字典项
     */
    List<DictItemVo> listByDictTypeCode(String dictTypeCode);

    /**
     * 批量查询字典项
     */
    List<DictItemVo> listByDictTypeCodes(List<String> dictTypeCodes);
}