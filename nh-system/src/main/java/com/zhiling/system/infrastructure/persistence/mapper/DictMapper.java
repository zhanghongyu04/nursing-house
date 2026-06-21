package com.zhiling.system.infrastructure.persistence.mapper;

import com.zhiling.model.vo.DictItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 统一字典 Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface DictMapper {

    /**
     * 根据字典类型编码查询字典项
     */
    @Select("select dict_type_code as dictTypeCode, item_value as itemValue, item_label as itemLabel, item_desc as itemDesc, sort_no as sortNo " +
            "from sys_dict_item where dict_type_code = #{dictTypeCode} and status = 0 order by sort_no asc, id asc")
    List<DictItemVo> listByDictTypeCode(@Param("dictTypeCode") String dictTypeCode);

    /**
     * 批量查询字典项
     */
    @Select({
            "<script>",
            "select dict_type_code as dictTypeCode, item_value as itemValue, item_label as itemLabel, item_desc as itemDesc, sort_no as sortNo",
            "from sys_dict_item",
            "where status = 0 and dict_type_code in",
            "<foreach collection='dictTypeCodes' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "order by dict_type_code asc, sort_no asc, id asc",
            "</script>"
    })
    List<DictItemVo> listByDictTypeCodes(@Param("dictTypeCodes") List<String> dictTypeCodes);
}


