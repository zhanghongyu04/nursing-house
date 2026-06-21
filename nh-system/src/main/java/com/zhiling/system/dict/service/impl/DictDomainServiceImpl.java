package com.zhiling.system.dict.service.impl;

import com.zhiling.model.vo.DictItemVo;
import com.zhiling.system.dict.service.DictDomainService;
import com.zhiling.system.dict.service.DictQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 字典域服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class DictDomainServiceImpl implements DictDomainService {

    private final DictQueryService dictQueryService;

    /**
     * 构造器：DictDomainServiceImpl
     *
     * @author zhanghongyu
     */
    public DictDomainServiceImpl(DictQueryService dictQueryService) {
        this.dictQueryService = dictQueryService;
    }

    /**
     * 方法：listByDictTypeCode
     *
     * @author zhanghongyu
     */
    @Override
    public List<DictItemVo> listByDictTypeCode(String dictTypeCode) {
        if (dictTypeCode == null || dictTypeCode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return dictQueryService.listByDictTypeCode(dictTypeCode.trim());
    }

    /**
     * 方法：listByDictTypeCodes
     *
     * @author zhanghongyu
     */
    @Override
    public Map<String, List<DictItemVo>> listByDictTypeCodes(List<String> dictTypeCodes) {
        Map<String, List<DictItemVo>> grouped = new LinkedHashMap<>();
        if (dictTypeCodes == null || dictTypeCodes.isEmpty()) {
            return grouped;
        }

        Set<String> uniqueCodes = new LinkedHashSet<>();
        for (String code : dictTypeCodes) {
            if (code != null && !code.trim().isEmpty()) {
                uniqueCodes.add(code.trim());
            }
        }
        if (uniqueCodes.isEmpty()) {
            return grouped;
        }

        List<DictItemVo> list = dictQueryService.listByDictTypeCodes(new ArrayList<>(uniqueCodes));
        for (String code : uniqueCodes) {
            grouped.put(code, new ArrayList<>());
        }
        for (DictItemVo item : list) {
            if (item == null || item.getDictTypeCode() == null) {
                continue;
            }
            grouped.computeIfAbsent(item.getDictTypeCode(), k -> new ArrayList<>()).add(item);
        }
        return grouped;
    }
}
