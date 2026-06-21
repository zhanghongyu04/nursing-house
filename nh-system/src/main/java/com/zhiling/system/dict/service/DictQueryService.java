package com.zhiling.system.dict.service;

import com.zhiling.model.vo.DictItemVo;

import java.util.List;

/**
 * 字典查询服务。
 *
 * @author zhanghongyu
 */
public interface DictQueryService {

    List<DictItemVo> listByDictTypeCode(String dictTypeCode);

    List<DictItemVo> listByDictTypeCodes(List<String> dictTypeCodes);
}
