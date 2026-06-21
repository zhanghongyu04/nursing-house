package com.zhiling.agent.application.repository;

import com.zhiling.agent.application.prompt.model.PromptTemplateSegment;
import com.zhiling.agent.application.prompt.model.PromptTemplateDetail;
import com.zhiling.agent.application.prompt.model.PromptTemplateSegmentEdit;
import com.zhiling.agent.application.prompt.model.PromptTemplateSummary;

import java.util.List;

/**

 * PromptTemplateRepository

 *

 * @author zhanghongyu

 */

public interface PromptTemplateRepository {

    Integer selectLatestActiveVersion(String promptName);

    List<PromptTemplateSegment> selectActiveByNameAndVersion(String promptName, Integer version);

    List<String> selectPromptNames();

    List<PromptTemplateSummary> selectVersionSummaries(String promptName);

    List<PromptTemplateDetail> selectDetailsByNameAndVersion(String promptName, Integer version);

    PromptTemplateDetail selectDetailById(Long id);

    Integer selectMaxVersion(String promptName);

    List<PromptTemplateDetail> insertVersion(String promptName, Integer version, List<PromptTemplateSegmentEdit> segments, Integer status);

    PromptTemplateDetail insertSegment(String promptName, Integer version, PromptTemplateSegmentEdit segment, Integer status);

    PromptTemplateDetail updateSegmentContent(Long id, String promptContent);

    List<PromptTemplateDetail> updateVersionStatus(String promptName, Integer version, Integer status);

    void updateOtherVersionsStatus(String promptName, Integer version, Integer status);

    PromptTemplateDetail updateSegmentStatus(Long id, Integer status);
}

