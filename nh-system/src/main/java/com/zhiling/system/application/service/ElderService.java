package com.zhiling.system.application.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.ElderDto;
import com.zhiling.model.dto.ElderExcelDto;
import com.zhiling.model.dto.ElderImportExcelDto;
import com.zhiling.model.dto.ElderPageQueryDto;
import com.zhiling.model.entity.Elder;

import java.util.List;

/**
 * 老人信息服务接口
 *
 * @author zhanghongyu
 */
public interface ElderService {
    Boolean add(Elder elder);

    Boolean delete(Long id);

    Boolean update(ElderDto elder);

    PageResult page(ElderPageQueryDto elderPageQueryDto);

    List<Elder> list();

    List<ElderExcelDto> listForExport(ElderPageQueryDto queryDto);

    /**
     * 批量导入老人信息。
     */
    ImportResult importElders(List<ElderImportExcelDto> elders);

    /**
     * 导入结果
     */
    class ImportResult {
        private final int successCount;
        private final int failCount;
        private final List<String> failDetails;

        /**
         * 方法：ImportResult
         *
         * @author zhanghongyu
         */
        public ImportResult(int successCount, int failCount, List<String> failDetails) {
            this.successCount = successCount;
            this.failCount = failCount;
            this.failDetails = failDetails;
        }

        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public List<String> getFailDetails() { return failDetails; }
    }
}
