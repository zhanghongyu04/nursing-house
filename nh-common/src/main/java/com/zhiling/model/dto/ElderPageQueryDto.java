package com.zhiling.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 老人信息分页查询DTO
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ElderPageQueryDto implements Serializable {
    private Integer page; //页码
    private Integer pageSize; //每页记录数
    private String elderName; //老人姓名
    private Integer occupiedBedType; //占用床位类型（0-普通床位,1-护理型床位）
    private String roomNumber; //房间号
    private String inpatientsTime; //入院时间（yyyy-MM-dd）
    private Integer familySituation; //入住类型
    private Integer selfCare; //自理能力
    private String sanaName; //所属养老院名称
    private Long sanaId; //所属养老院id
    private List<Long> elderIds; //选中的老人ID列表（导出使用）
    private Set<Long> sanaScopeIds; //所属养老院ID范围（多机构授权）
}
