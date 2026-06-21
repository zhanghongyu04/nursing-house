package com.zhiling.system.dict.service;

import com.zhiling.model.vo.DictItemVo;

import java.util.List;
import java.util.Map;

/**
 * 字典域服务接口。
 *
 * @author zhanghongyu
 */
public interface DictDomainService {

    List<DictItemVo> listByDictTypeCode(String dictTypeCode);

    Map<String, List<DictItemVo>> listByDictTypeCodes(List<String> dictTypeCodes);
}