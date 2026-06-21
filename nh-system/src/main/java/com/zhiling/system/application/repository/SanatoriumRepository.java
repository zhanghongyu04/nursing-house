package com.zhiling.system.application.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.SanatoriumPageQueryDto;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.model.vo.RegionSanaCountVo;

import java.util.List;
import java.util.Set;

/**
 * 机构仓储接口。
 *
 * 定义 application 层所需的机构数据访问契约，
 * 由 infrastructure 层提供实现，遵循依赖倒置原则。
 *
 * @author zhanghongyu
 */
public interface SanatoriumRepository {

    // === 查询操作 ===

    List<Sanatorium> listAll();

    List<Sanatorium> listByIds(Set<Long> ids);

    Long selectIdByName(String sanaName);

    String getNameById(Long sanaId);

    Sanatorium selectById(Long id);

    Sanatorium selectByExactName(String sanaName);

    boolean existsByUscc(String uscc);

    List<Sanatorium> searchByKeyword(String keyword, Integer limit, Set<Long> scopeIds);

    Integer count();

    Integer countNurse();

    Integer countMedicine();

    Integer countAffiliation();

    Integer countBedInUse();

    Integer countBed();

    List<RegionSanaCountVo> listRegionSanaCount();

    List<RegionSanaCountVo> listRegionSanaCountBySanaId(Long sanaId);

    IPage<Sanatorium> selectPage(Page<Sanatorium> page, SanatoriumPageQueryDto dto);

    // === 命令操作 ===

    boolean insert(Sanatorium sanatorium);

    boolean deleteById(Long id);

    boolean updateById(Sanatorium sanatorium);
}