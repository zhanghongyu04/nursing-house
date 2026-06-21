package com.zhiling.system.interfaces.system;

import com.zhiling.model.vo.DictItemVo;
import com.zhiling.system.dict.service.DictQueryService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典域查询适配器。
 *
 * 适配领域服务接口，委托持久化查询服务执行查询。
 *
 * @author zhanghongyu
 */
@Component
public class DictQueryServiceAdapter implements DictQueryService {

    private final com.zhiling.system.infrastructure.persistence.query.DictQueryService dictPersistenceQueryService;

    public DictQueryServiceAdapter(
            com.zhiling.system.infrastructure.persistence.query.DictQueryService dictPersistenceQueryService) {
        this.dictPersistenceQueryService = dictPersistenceQueryService;
    }

    /**
     * 方法：listByDictTypeCode
     *
     * @author zhanghongyu
     */
    @Override
    public List<DictItemVo> listByDictTypeCode(String dictTypeCode) {
        return dictPersistenceQueryService.listByDictTypeCode(dictTypeCode);
    }

    /**
     * 方法：listByDictTypeCodes
     *
     * @author zhanghongyu
     */
    @Override
    public List<DictItemVo> listByDictTypeCodes(List<String> dictTypeCodes) {
        return dictPersistenceQueryService.listByDictTypeCodes(dictTypeCodes);
    }
}
