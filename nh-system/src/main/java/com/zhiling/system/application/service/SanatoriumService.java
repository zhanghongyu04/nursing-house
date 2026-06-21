package com.zhiling.system.application.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.SanatoriumDetailPageQueryDto;
import com.zhiling.model.dto.SanatoriumImportExcelDto;
import com.zhiling.model.dto.SanatoriumPageQueryDto;
import com.zhiling.model.entity.Sanatorium;

import java.util.List;
import java.util.Map;

/**
 * 养老院服务接口
 *
 * @author zhanghongyu
 */
public interface SanatoriumService {

    /**
     * 查询所有养老院信息
     * @return
     */
    List<Sanatorium> list();

    /**
     * 按筛选条件或选中 ID 查询导出数据。
     */
    List<Sanatorium> listForExport(SanatoriumPageQueryDto queryDto);

    /**
     * 批量插入养老院信息
     *
     * @param sanatoriumList
     * @return
     */
    boolean saveBatch(List<Sanatorium> sanatoriumList);

    /**
     * 批量导入康养机构信息。
     */
    ImportResult importSanatoriums(List<SanatoriumImportExcelDto> rows);

    /**
     * 分页查询养老院信息
     * @param sanatoriumPageQueryDto
     * @return
     */
    PageResult page(SanatoriumPageQueryDto sanatoriumPageQueryDto);

    /**
     * 删除养老院信息
     * @param id
     * @return
     */
    Boolean removeById(Long id);

    /**
     * 修改养老院信息
     * @param sanatorium
     * @return
     */
    boolean update(Sanatorium sanatorium);

    /**
     * 不同自理能力老人数量分布
     * @param sanaName
     * @return
     */
    Map<String, Integer> elderDistribution(String sanaName);


    /**
     * 养老院分页详情老人信息分页查询
     * @param sanatoriumDetailPageQueryDto
     * @return
     */
    PageResult pageSanaElderList(SanatoriumDetailPageQueryDto sanatoriumDetailPageQueryDto);

    /**
     * 导入结果。
     */
    class ImportResult {
        private final int totalCount;
        private final int successCount;
        private final int failCount;
        private final int skipCount;
        private final List<String> failDetails;

        public ImportResult(int totalCount, int successCount, int failCount, int skipCount, List<String> failDetails) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failCount = failCount;
            this.skipCount = skipCount;
            this.failDetails = failDetails;
        }

        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public int getSkipCount() { return skipCount; }
        public List<String> getFailDetails() { return failDetails; }
    }
}


