package com.zhiling.system.infrastructure.persistence.query.impl;

import com.zhiling.model.vo.DictItemVo;
import com.zhiling.system.infrastructure.persistence.mapper.DictMapper;
import com.zhiling.system.infrastructure.persistence.query.DictQueryService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 字典查询服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class DictQueryServiceImpl implements DictQueryService {

    private final DictMapper dictMapper;

    /**
     * 构造器：DictQueryServiceImpl
     *
     * @author zhanghongyu
     */
    public DictQueryServiceImpl(DictMapper dictMapper) {
        this.dictMapper = dictMapper;
    }

    /**
     * 方法：listByDictTypeCode
     *
     * @author zhanghongyu
     */
    @Override
    public List<DictItemVo> listByDictTypeCode(String dictTypeCode) {
        if (dictTypeCode == null || dictTypeCode.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<DictItemVo> items = dictMapper.listByDictTypeCode(dictTypeCode);
        return items != null ? items : Collections.emptyList();
    }

    /**
     * 方法：listByDictTypeCodes
     *
     * @author zhanghongyu
     */
    @Override
    public List<DictItemVo> listByDictTypeCodes(List<String> dictTypeCodes) {
        if (dictTypeCodes == null || dictTypeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<DictItemVo> items = dictMapper.listByDictTypeCodes(dictTypeCodes);
        return items != null ? items : Collections.emptyList();
    }
}