<template>
  <div class="template-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">任务模板</h2>
        <p class="page-desc">配置任务生成规则，系统按规则自动生成周期性护理任务，也支持手动补充生成</p>
      </div>
      <el-button v-if="canCreateTemplate" type="primary" @click="openCreateDialog">新建模板</el-button>
    </div>

    <el-card shadow="never" class="query-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="任务标题">
          <el-input v-model.trim="queryForm.taskTitle" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.enabled" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="opt in enabledStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="taskTitle" label="任务标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="taskType" label="任务类型" width="110" align="center">
          <template #default="{ row }">{{ dictLabel(dictOptions.taskType, row.taskType) }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small">{{ dictLabel(dictOptions.priority, row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeUsername" label="默认执行人" width="120" show-overflow-tooltip />
        <el-table-column prop="scheduleDescription" label="生成规则" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="schedule-cell">
              <div class="schedule-cell__main">{{ formatScheduleDescription(row) }}</div>
              <div class="schedule-cell__sub">{{ row.timezone || DEFAULT_TIMEZONE }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="plannedDuration" label="单次预计时长" width="120" align="center">
          <template #default="{ row }">{{ row.plannedDuration }} 分钟</template>
        </el-table-column>
        <el-table-column prop="nextExecuteTime" label="下一次生成" width="190">
          <template #default="{ row }">{{ formatDisplayDateTime(row.nextExecuteTime) }}</template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-if="canToggleTemplate" :model-value="row.enabled === 1" size="small" @change="handleToggle(row)" />
            <el-tag v-else :type="row.enabled === 1 ? 'success' : 'info'" size="small">{{ row.enabled === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canUpdateTemplate" link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="canGenerateTemplate" link type="success" size="small" @click="handleGenerate(row)">手动生成</el-button>
            <el-button v-if="canRemoveTemplate" link type="danger" size="small" @click="handleRemove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageQuery.page"
          v-model:page-size="pageQuery.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close class="template-dialog">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <div class="form-section">
          <div class="section-head">
            <div class="section-title">基础信息</div>
            <div class="section-desc">定义模板会生成什么任务，以及自动生成后的默认分配对象。</div>
          </div>
          <el-form-item v-if="isMultiScope" label="目标机构" prop="targetSanaId">
            <el-select v-model="selectedSanaId" placeholder="请选择目标机构" filterable :disabled="dialogMode === 'edit'" style="width: 100%" @change="handleOrgChange">
              <el-option v-for="item in sanatoriumOptions" :key="item.id" :label="item.sanaName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务标题" prop="taskTitle">
            <el-input v-model.trim="formData.taskTitle" maxlength="200" />
          </el-form-item>
          <el-form-item label="任务内容">
            <el-input v-model="formData.taskContent" type="textarea" :rows="3" maxlength="2000" />
          </el-form-item>
          <el-form-item label="任务对象">
            <el-select v-model="formData.elderId" placeholder="请选择老人" clearable filterable style="width: 100%">
              <el-option v-for="e in elderOptions" :key="e.id" :label="e.displayLabel" :value="e.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务类型" prop="taskType">
            <el-select v-model="formData.taskType" style="width: 100%">
              <el-option v-for="opt in dictOptions.taskType" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="formData.priority" style="width: 100%">
              <el-option v-for="opt in dictOptions.priority" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="默认执行人" prop="assigneeUserId">
            <div class="field-stack">
              <el-select v-model="formData.assigneeUserId" placeholder="请选择默认执行人" clearable filterable style="width: 100%">
                <el-option v-for="u in nurseOptions" :key="u.id" :label="u.username" :value="u.id" />
              </el-select>
              <div class="field-help">系统自动生成的任务会默认分配给该护理人员。</div>
            </div>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="section-head">
            <div class="section-title">任务生成规则</div>
            <div class="section-desc">这里定义的是系统何时生成任务，不是护理人员何时执行任务。</div>
          </div>

          <div v-if="dialogMode === 'edit'" class="impact-panel">
            <div class="impact-row">
              <span>当前下一次生成时间</span>
              <strong>{{ currentNextExecuteText }}</strong>
            </div>
            <div class="impact-row">
              <span>保存后预计下一次生成时间</span>
              <strong>{{ estimatedNextExecuteText }}</strong>
            </div>
            <div class="impact-tip">修改生成规则只影响后续新生成的任务，不影响已生成任务。</div>
          </div>

          <el-form-item label="规则配置" prop="scheduleConfig">
            <div class="schedule-builder">
              <div class="builder-row">
                <div class="builder-label">生成周期</div>
                <div class="builder-control">
                  <el-radio-group v-model="formData.scheduleConfig.scheduleType" @change="handleScheduleTypeChange">
                    <el-radio-button v-for="item in scheduleTypeOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio-button>
                  </el-radio-group>
                </div>
              </div>

              <div v-if="formData.scheduleConfig.scheduleType === 'WEEKLY'" class="builder-row">
                <div class="builder-label">生成日期</div>
                <div class="builder-control">
                  <el-checkbox-group v-model="formData.scheduleConfig.weekdays">
                    <el-checkbox-button v-for="item in weekdayOptions" :key="item.value" :label="item.value">{{ item.label }}</el-checkbox-button>
                  </el-checkbox-group>
                </div>
              </div>

              <div v-if="formData.scheduleConfig.scheduleType === 'MONTHLY'" class="builder-row">
                <div class="builder-label">生成日期</div>
                <div class="builder-control">
                  <el-select v-model="formData.scheduleConfig.monthDays" multiple filterable collapse-tags collapse-tags-tooltip placeholder="请选择每月日期" style="width: 100%">
                    <el-option v-for="day in monthDayOptions" :key="day" :label="`${day}号`" :value="day" />
                  </el-select>
                </div>
              </div>

              <div class="builder-row">
                <div class="builder-label">生成方式</div>
                <div class="builder-control">
                  <el-radio-group v-model="formData.scheduleConfig.timeMode" @change="handleTimeModeChange">
                    <el-radio-button v-for="item in timeModeOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio-button>
                  </el-radio-group>
                </div>
              </div>

              <div v-if="formData.scheduleConfig.timeMode === 'POINT'" class="builder-row">
                <div class="builder-label">生成时间</div>
                <div class="builder-control">
                  <el-select v-model="formData.scheduleConfig.timePoints" multiple filterable collapse-tags collapse-tags-tooltip placeholder="可选择多个生成时间点" style="width: 100%">
                    <el-option v-for="time in timeOptions" :key="time" :label="time" :value="time" />
                  </el-select>
                  <div class="schedule-hint">系统会在所选时间点各生成 1 条任务。</div>
                </div>
              </div>

              <div v-else class="builder-row">
                <div class="builder-label">生成时间段</div>
                <div class="builder-control">
                  <div class="interval-fields">
                    <el-select v-model="formData.scheduleConfig.startTime" placeholder="开始时间" style="width: 160px">
                      <el-option v-for="time in timeOptions" :key="`start-${time}`" :label="time" :value="time" />
                    </el-select>
                    <span class="interval-separator">至</span>
                    <el-select v-model="formData.scheduleConfig.endTime" placeholder="结束时间" style="width: 160px">
                      <el-option v-for="time in timeOptions" :key="`end-${time}`" :label="time" :value="time" />
                    </el-select>
                    <el-select v-model="formData.scheduleConfig.intervalMinutes" placeholder="生成间隔" style="width: 180px">
                      <el-option v-for="item in intervalOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </div>
                  <div class="schedule-hint">系统会在该时间段内按固定间隔重复生成任务。</div>
                </div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="单次任务预计时长" prop="plannedDuration">
            <div class="field-stack">
              <div class="inline-field">
                <el-input-number v-model="formData.plannedDuration" :min="5" :max="1440" :step="15" />
                <span class="field-unit">分钟</span>
              </div>
              <div class="field-help">仅用于计算每条任务的计划结束时间，不影响生成频率。</div>
            </div>
          </el-form-item>

          <el-form-item label="模板生效日期">
            <div class="field-stack">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
              <div class="field-help">仅在生效日期范围内参与自动生成。</div>
            </div>
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="section-head">
            <div class="section-title">生成结果预览</div>
            <div class="section-desc">把规则翻译成生成结果，避免把“系统生成”误解成“人工执行”。</div>
          </div>
          <div class="preview-panel">
            <div class="preview-item">
              <span class="preview-item__label">规则说明</span>
              <span class="preview-item__value">{{ schedulePreview.ruleText }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-item__label">单个匹配日生成时间点</span>
              <span class="preview-item__value">{{ schedulePreview.batchTimesText }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-item__label">{{ dialogMode === 'edit' ? '保存后预计下一次生成时间' : '预计下一次生成时间' }}</span>
              <span class="preview-item__value">{{ schedulePreview.nextExecuteText }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-item__label">单次任务预计时长</span>
              <span class="preview-item__value">{{ plannedDurationText }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-item__label">模板生效日期</span>
              <span class="preview-item__value">{{ effectiveDateRangeText }}</span>
            </div>
            <div class="preview-tip">系统按 {{ formData.timezone || DEFAULT_TIMEZONE }} 时区计算生成时间。</div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createTaskTemplate,
  generateFromTemplate,
  getTaskTemplatePage,
  removeTaskTemplate,
  toggleTaskTemplate,
  updateTaskTemplate,
  type TaskTemplateCreate,
  type TaskTemplateRow,
  type TaskTemplateScheduleConfig,
  type TaskTemplateScheduleType,
  type TaskTemplateTimeMode,
} from '@/api/nursingTaskTemplate';
import { getElderPage } from '@/api/elder';
import { getUserPageAPI } from '@/api/admin';
import { getDictItemsBatch } from '@/api/dict';
import { getSanatoriumPage } from '@/api/sanatorium';
import { useUserStore } from '@/stores/userStore';
import { hasResourcePath } from '@/constants/authRoles';
import { formatDisplayDateTime } from '@/utils/dateTime';

interface DictOption { value: number; label: string }
interface StringOption { value: string; label: string; sortNo?: number }
interface ElderOption { id: number; elderName: string; displayLabel: string }
interface NurseOption { id: number; username: string }
interface SchedulePreviewSummary {
  ruleText: string;
  batchTimesText: string;
  nextExecuteText: string;
}

const DEFAULT_TIMEZONE = 'Asia/Shanghai';
const DEFAULT_POINT_TIME = '08:00';
const DEFAULT_INTERVAL_START = '06:00';
const DEFAULT_INTERVAL_END = '23:59';
const DEFAULT_INTERVAL_MINUTES = 60;

const scheduleTypeOptions: Array<{ value: TaskTemplateScheduleType; label: string }> = [
  { value: 'DAILY', label: '每天' },
  { value: 'WEEKLY', label: '每周' },
  { value: 'MONTHLY', label: '每月' },
];

const timeModeOptions: Array<{ value: TaskTemplateTimeMode; label: string }> = [
  { value: 'POINT', label: '固定时间生成' },
  { value: 'INTERVAL', label: '时间段内重复生成' },
];

const weekdayOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' },
];

const intervalOptions = [
  { value: 30, label: '每 30 分钟生成一次' },
  { value: 60, label: '每 1 小时生成一次' },
  { value: 120, label: '每 2 小时生成一次' },
];

const monthDayOptions = Array.from({ length: 31 }, (_, index) => index + 1);

const buildTimeOptions = () => {
  const options: string[] = [];
  for (let hour = 6; hour <= 23; hour += 1) {
    for (const minute of [0, 30]) {
      options.push(`${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`);
    }
  }
  options.push('23:59');
  return options;
};

const timeOptions = buildTimeOptions();
const padNumber = (value: number) => String(value).padStart(2, '0');

const buildElderLabel = (r: any): string => {
  const parts: string[] = [];
  if (r.sex != null) parts.push(r.sex === 1 ? '男' : '女');
  if (r.age != null) parts.push(`${r.age}岁`);
  return parts.length > 0 ? `${r.elderName || ''}（${parts.join(' / ')}）` : (r.elderName || '');
};

const toNumberOption = (item: any): DictOption | null => {
  const numericValue = Number(item?.itemValue);
  if (!Number.isFinite(numericValue)) return null;
  return { value: numericValue, label: String(item?.itemLabel || '') };
};

const toTagStyleOption = (item: any): StringOption | null => {
  const value = String(item?.itemValue || '').trim();
  const label = String(item?.itemLabel || '').trim();
  if (!value || !label) return null;
  const sortNo = Number(item?.sortNo);
  return { value, label, sortNo: Number.isFinite(sortNo) ? sortNo : 0 };
};

const createDefaultScheduleConfig = (): TaskTemplateScheduleConfig => ({
  scheduleType: 'DAILY',
  timeMode: 'POINT',
  weekdays: [],
  monthDays: [],
  timePoints: [DEFAULT_POINT_TIME],
  startTime: DEFAULT_INTERVAL_START,
  endTime: DEFAULT_INTERVAL_END,
  intervalMinutes: DEFAULT_INTERVAL_MINUTES,
});

const cloneScheduleConfig = (config?: TaskTemplateScheduleConfig): TaskTemplateScheduleConfig => ({
  scheduleType: config?.scheduleType || 'DAILY',
  timeMode: config?.timeMode || 'POINT',
  weekdays: Array.isArray(config?.weekdays) ? [...config.weekdays] : [],
  monthDays: Array.isArray(config?.monthDays) ? [...config.monthDays] : [],
  timePoints: Array.isArray(config?.timePoints) && config.timePoints.length > 0 ? [...config.timePoints] : [DEFAULT_POINT_TIME],
  startTime: config?.startTime || DEFAULT_INTERVAL_START,
  endTime: config?.endTime || DEFAULT_INTERVAL_END,
  intervalMinutes: config?.intervalMinutes || DEFAULT_INTERVAL_MINUTES,
});

const weekdayLabelMap = new Map(weekdayOptions.map((item) => [item.value, item.label]));

const parseTimeToMinutes = (value?: string) => {
  if (!value) return null;
  const [hour, minute] = value.split(':').map((item) => Number(item));
  if (!Number.isFinite(hour) || !Number.isFinite(minute)) return null;
  return hour * 60 + minute;
};

const normalizeNumberArray = (values?: number[]) => {
  if (!Array.isArray(values)) return [];
  return [...new Set(values.filter((value) => Number.isFinite(value)))].sort((a, b) => a - b);
};

const normalizeTimeArray = (values?: string[]) => {
  if (!Array.isArray(values)) return [];
  return [...new Set(values.filter((value) => typeof value === 'string' && value.trim()).map((value) => value.trim()))].sort();
};

const parseDateString = (value?: string) => {
  if (!value) return null;
  const [year, month, day] = value.split('-').map((item) => Number(item));
  if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(day)) return null;
  return new Date(year, month - 1, day);
};

const cloneDate = (value: Date) => new Date(value.getTime());

const startOfDay = (value: Date) => {
  const date = cloneDate(value);
  date.setHours(0, 0, 0, 0);
  return date;
};

const addDays = (value: Date, days: number) => {
  const date = cloneDate(value);
  date.setDate(date.getDate() + days);
  return date;
};

const toWeekdayValue = (value: Date) => {
  const day = value.getDay();
  return day === 0 ? 7 : day;
};

const buildTimeCandidateList = (config: TaskTemplateScheduleConfig) => {
  const normalized = normalizeScheduleConfigForSubmit(config);
  if (normalized.timeMode === 'POINT') {
    return normalizeTimeArray(normalized.timePoints);
  }

  const startMinutes = parseTimeToMinutes(normalized.startTime);
  const endMinutes = parseTimeToMinutes(normalized.endTime);
  const intervalMinutes = Number(normalized.intervalMinutes);
  if (startMinutes == null || endMinutes == null || !Number.isFinite(intervalMinutes) || intervalMinutes <= 0 || startMinutes > endMinutes) {
    return [];
  }

  const timeCandidates: string[] = [];
  for (let current = startMinutes; current <= endMinutes; current += intervalMinutes) {
    timeCandidates.push(`${padNumber(Math.floor(current / 60))}:${padNumber(current % 60)}`);
  }
  return timeCandidates;
};

const formatCompactTimeList = (times: string[]) => {
  if (times.length === 0) return '-';
  if (times.length <= 8) return times.join('、');
  return `${times.slice(0, 8).join('、')} 等 ${times.length} 个时间点`;
};

const resolveEffectiveDateRange = () => {
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
    return { startDate: dateRange.value[0], endDate: dateRange.value[1] };
  }
  return {
    startDate: formData.startDate,
    endDate: formData.endDate,
  };
};

const matchesScheduleDate = (config: TaskTemplateScheduleConfig, candidateDate: Date) => {
  const normalized = normalizeScheduleConfigForSubmit(config);
  if (normalized.scheduleType === 'DAILY') return true;
  if (normalized.scheduleType === 'WEEKLY') {
    return normalizeNumberArray(normalized.weekdays).includes(toWeekdayValue(candidateDate));
  }
  return normalizeNumberArray(normalized.monthDays).includes(candidateDate.getDate());
};

const calculateNextExecutePreview = (config: TaskTemplateScheduleConfig, startDate?: string, endDate?: string) => {
  const normalized = normalizeScheduleConfigForSubmit(config);
  const reference = new Date();
  const today = startOfDay(reference);
  const rangeStart = parseDateString(startDate);
  const rangeEnd = parseDateString(endDate);
  const cursor = rangeStart && rangeStart.getTime() > today.getTime() ? rangeStart : today;
  const timeCandidates = buildTimeCandidateList(normalized);

  for (let index = 0; index < 366 * 5; index += 1) {
    const candidateDate = addDays(cursor, index);
    if (rangeEnd && candidateDate.getTime() > startOfDay(rangeEnd).getTime()) {
      return null;
    }
    if (!matchesScheduleDate(normalized, candidateDate)) {
      continue;
    }
    for (const time of timeCandidates) {
      const [hour, minute] = time.split(':').map((item) => Number(item));
      const candidateDateTime = cloneDate(candidateDate);
      candidateDateTime.setHours(hour, minute, 0, 0);
      if (candidateDateTime.getTime() > reference.getTime()) {
        return candidateDateTime;
      }
    }
  }
  return null;
};

const formatIntervalLabel = (minutes?: number) => {
  if (minutes === 30) return '30 分钟';
  if (minutes === 60) return '1 小时';
  if (minutes === 120) return '2 小时';
  return `${minutes ?? '-'} 分钟`;
};

const normalizeScheduleConfigForSubmit = (config: TaskTemplateScheduleConfig): TaskTemplateScheduleConfig => {
  const normalized: TaskTemplateScheduleConfig = {
    scheduleType: config.scheduleType,
    timeMode: config.timeMode,
  };

  if (config.scheduleType === 'WEEKLY') {
    normalized.weekdays = normalizeNumberArray(config.weekdays);
  }
  if (config.scheduleType === 'MONTHLY') {
    normalized.monthDays = normalizeNumberArray(config.monthDays);
  }
  if (config.timeMode === 'POINT') {
    normalized.timePoints = normalizeTimeArray(config.timePoints);
  } else {
    normalized.startTime = config.startTime;
    normalized.endTime = config.endTime;
    normalized.intervalMinutes = config.intervalMinutes;
  }
  return normalized;
};

const resolveScheduleError = (config: TaskTemplateScheduleConfig) => {
  if (!config.scheduleType) return '请选择生成周期';
  if (!config.timeMode) return '请选择生成方式';
  if (config.scheduleType === 'WEEKLY' && normalizeNumberArray(config.weekdays).length === 0) return '请至少选择一个生成日期';
  if (config.scheduleType === 'MONTHLY' && normalizeNumberArray(config.monthDays).length === 0) return '请至少选择一个生成日期';
  if (config.timeMode === 'POINT' && normalizeTimeArray(config.timePoints).length === 0) return '请至少选择一个生成时间';
  if (config.timeMode === 'INTERVAL') {
    if (!config.startTime || !config.endTime) return '请完整配置开始时间和结束时间';
    if (![30, 60, 120].includes(Number(config.intervalMinutes))) return '生成间隔仅支持 30、60、120 分钟';
    const startMinutes = parseTimeToMinutes(config.startTime);
    const endMinutes = parseTimeToMinutes(config.endTime);
    if (startMinutes == null || endMinutes == null) return '时间格式不正确';
    if (startMinutes > endMinutes) return '开始时间不能晚于结束时间';
  }
  return '';
};

const buildScheduleDescription = (config?: TaskTemplateScheduleConfig) => {
  if (!config) return '-';
  const normalized = normalizeScheduleConfigForSubmit(config);
  const scopeLabel = (() => {
    if (normalized.scheduleType === 'DAILY') return '每天';
    if (normalized.scheduleType === 'WEEKLY') {
      const weekdays = normalizeNumberArray(normalized.weekdays).map((value) => weekdayLabelMap.get(value) || `周${value}`);
      return `每周 ${weekdays.join('、')}`;
    }
    const monthDays = normalizeNumberArray(normalized.monthDays).map((value) => `${value}号`);
    return `每月 ${monthDays.join('、')}`;
  })();

  if (normalized.timeMode === 'POINT') {
    return `${scopeLabel}，在 ${normalizeTimeArray(normalized.timePoints).join('、')} 固定时间生成`;
  }
  return `${scopeLabel}，${normalized.startTime} 至 ${normalized.endTime}，每 ${formatIntervalLabel(normalized.intervalMinutes)} 生成一次`;
};

const userStore = useUserStore();
const canCreateTemplate = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task-template/create'));
const canUpdateTemplate = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task-template/update'));
const canGenerateTemplate = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task-template/generate/**'));
const canRemoveTemplate = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task-template/remove/**'));
const canToggleTemplate = computed(() => hasResourcePath(userStore.userInfo.resourcePaths, '/web/nursing-task-template/toggle/**'));
const isMultiScope = computed(() => (userStore.userInfo.sanaScopeIds?.length ?? 0) > 1);
const selectedSanaId = ref<number | undefined>(undefined);
const sanatoriumOptions = ref<{ id: number; sanaName: string }[]>([]);
const effectiveSanaId = computed(() => (!isMultiScope.value ? userStore.userInfo.sanaId ?? userStore.userInfo.sanaScopeIds?.[0] : selectedSanaId.value));

const loading = ref(false);
const tableData = ref<TaskTemplateRow[]>([]);
const total = ref(0);

const queryForm = reactive({ taskTitle: '', enabled: undefined as number | undefined });
const pageQuery = reactive({ page: 1, pageSize: 10 });

const dictOptions = reactive<Record<string, DictOption[]>>({ priority: [], taskType: [] });
const enabledStatusOptions = ref<DictOption[]>([]);
const tagStyleOptions = ref<StringOption[]>([]);
const nurseRoleId = ref(4);

const elderOptions = ref<ElderOption[]>([]);
const nurseOptions = ref<NurseOption[]>([]);

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const submitting = ref(false);
const formRef = ref<FormInstance>();
const dateRange = ref<string[]>([]);
const editingCurrentNextExecuteTime = ref('');

const defaultForm = (): TaskTemplateCreate => ({
  taskTitle: '',
  taskContent: '',
  taskType: 0,
  priority: 2,
  elderId: undefined,
  assigneeUserId: undefined,
  timezone: DEFAULT_TIMEZONE,
  scheduleConfig: createDefaultScheduleConfig(),
  plannedDuration: 60,
  startDate: undefined,
  endDate: undefined,
  remark: '',
});

const formData = reactive<TaskTemplateCreate>(defaultForm());

const validateScheduleConfig = (_rule: any, _value: any, callback: (error?: Error) => void) => {
  const error = resolveScheduleError(formData.scheduleConfig);
  if (error) {
    callback(new Error(error));
    return;
  }
  callback();
};

const formRules: FormRules = {
  targetSanaId: [{
    validator: (_rule: any, _value: any, callback: (error?: Error) => void) => {
      if (isMultiScope.value && !selectedSanaId.value) {
        callback(new Error('请选择目标机构'));
        return;
      }
      callback();
    },
    trigger: 'change',
  }],
  taskTitle: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  scheduleConfig: [{ validator: validateScheduleConfig, trigger: 'change' }],
  plannedDuration: [{ required: true, message: '请输入单次任务预计时长', trigger: 'change' }],
};

const dictLabel = (opts: DictOption[], val: number) => opts.find((o) => o.value === val)?.label ?? '-';
const priorityTagType = (val: number) => {
  const getStyle = (key: string, fallback: string) => {
    const target = tagStyleOptions.value.find((o) => o.value === key);
    return (target?.label || fallback) as any;
  };
  if (val >= 4) return getStyle('PRIORITY_4', 'danger');
  if (val === 3) return getStyle('PRIORITY_3', 'warning');
  if (val === 2) return getStyle('PRIORITY_2', '');
  return getStyle('PRIORITY_1', 'info');
};

const dialogTitle = computed(() => dialogMode.value === 'create' ? '新建任务模板' : '编辑任务模板');

const plannedDurationText = computed(() => `${formData.plannedDuration || 0} 分钟`);

const effectiveDateRangeText = computed(() => {
  const { startDate, endDate } = resolveEffectiveDateRange();
  if (startDate && endDate) return `${startDate} 至 ${endDate}`;
  if (startDate) return `${startDate} 起`;
  if (endDate) return `截至 ${endDate}`;
  return '长期有效';
});

const schedulePreview = computed<SchedulePreviewSummary>(() => {
  const error = resolveScheduleError(formData.scheduleConfig);
  if (error) {
    return {
      ruleText: `请完善任务生成规则：${error}`,
      batchTimesText: '-',
      nextExecuteText: '-',
    };
  }
  const { startDate, endDate } = resolveEffectiveDateRange();
  return {
    ruleText: buildScheduleDescription(formData.scheduleConfig),
    batchTimesText: formatCompactTimeList(buildTimeCandidateList(formData.scheduleConfig)),
    nextExecuteText: formatDisplayDateTime(calculateNextExecutePreview(formData.scheduleConfig, startDate, endDate)),
  };
});

const currentNextExecuteText = computed(() => formatDisplayDateTime(editingCurrentNextExecuteTime.value));
const estimatedNextExecuteText = computed(() => schedulePreview.value.nextExecuteText);

const formatScheduleDescription = (row: TaskTemplateRow) => row.scheduleConfig ? buildScheduleDescription(row.scheduleConfig) : (row.scheduleDescription || '-');

const handleScheduleTypeChange = (type: TaskTemplateScheduleType) => {
  if (type === 'DAILY') {
    formData.scheduleConfig.weekdays = [];
    formData.scheduleConfig.monthDays = [];
  }
  if (type === 'WEEKLY') {
    formData.scheduleConfig.monthDays = [];
    if (!formData.scheduleConfig.weekdays?.length) formData.scheduleConfig.weekdays = [1];
  }
  if (type === 'MONTHLY') {
    formData.scheduleConfig.weekdays = [];
    if (!formData.scheduleConfig.monthDays?.length) formData.scheduleConfig.monthDays = [1];
  }
};

const handleTimeModeChange = (mode: TaskTemplateTimeMode) => {
  if (mode === 'POINT') {
    if (!formData.scheduleConfig.timePoints?.length) formData.scheduleConfig.timePoints = [DEFAULT_POINT_TIME];
    return;
  }
  formData.scheduleConfig.startTime = formData.scheduleConfig.startTime || DEFAULT_INTERVAL_START;
  formData.scheduleConfig.endTime = formData.scheduleConfig.endTime || DEFAULT_INTERVAL_END;
  formData.scheduleConfig.intervalMinutes = formData.scheduleConfig.intervalMinutes || DEFAULT_INTERVAL_MINUTES;
};

const loadDictOptions = async () => {
  try {
    const res: any = await getDictItemsBatch(['NURSING_TASK_PRIORITY', 'NURSING_TASK_TYPE', 'NURSING_TEMPLATE_ENABLED_STATUS', 'NURSING_TASK_TAG_STYLE', 'USER_ROLE_ID_MAP']);
    const map = res?.data || {};
    dictOptions.priority = Array.isArray(map.NURSING_TASK_PRIORITY) ? map.NURSING_TASK_PRIORITY.map(toNumberOption).filter(Boolean) as DictOption[] : [];
    dictOptions.taskType = Array.isArray(map.NURSING_TASK_TYPE) ? map.NURSING_TASK_TYPE.map(toNumberOption).filter(Boolean) as DictOption[] : [];
    enabledStatusOptions.value = Array.isArray(map.NURSING_TEMPLATE_ENABLED_STATUS) ? map.NURSING_TEMPLATE_ENABLED_STATUS.map(toNumberOption).filter(Boolean) as DictOption[] : [];
    tagStyleOptions.value = Array.isArray(map.NURSING_TASK_TAG_STYLE)
      ? (map.NURSING_TASK_TAG_STYLE.map(toTagStyleOption).filter(Boolean) as StringOption[]).sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
      : [];
    const roleOptions = Array.isArray(map.USER_ROLE_ID_MAP) ? map.USER_ROLE_ID_MAP : [];
    const nurse = roleOptions.find((o: any) => String(o?.itemLabel || '').trim() === 'NURSE');
    const parsedRoleId = Number(nurse?.itemValue);
    if (Number.isFinite(parsedRoleId)) nurseRoleId.value = parsedRoleId;
  } catch {
    /* ignore */
  }
};

const loadElderOptions = async () => {
  const targetSanaId = effectiveSanaId.value;
  if (isMultiScope.value && !targetSanaId) {
    elderOptions.value = [];
    return;
  }
  try {
    const params: any = { page: 1, pageSize: 999 };
    if (targetSanaId) params.sanaId = targetSanaId;
    const res: any = await getElderPage(params);
    if (res?.code === 200 && res?.data?.records) {
      elderOptions.value = res.data.records.map((r: any) => ({ id: r.id, elderName: r.elderName, displayLabel: buildElderLabel(r) }));
    }
  } catch {
    /* ignore */
  }
};

const loadNurseOptions = async () => {
  const targetSanaId = effectiveSanaId.value;
  if (isMultiScope.value && !targetSanaId) {
    nurseOptions.value = [];
    return;
  }
  try {
    const params: any = { page: 1, pageSize: 999, roleId: nurseRoleId.value };
    if (targetSanaId) params.sanaId = targetSanaId;
    const res: any = await getUserPageAPI(params);
    if (res?.code === 200 && res?.data?.records) {
      nurseOptions.value = res.data.records.map((r: any) => ({ id: r.id, username: r.username }));
    }
  } catch {
    /* ignore */
  }
};

const loadSanatoriumOptions = async () => {
  if (!isMultiScope.value) return;
  try {
    const pageSize = 200;
    let page = 1;
    let totalCount = 0;
    const list: { id: number; sanaName: string }[] = [];
    do {
      const res: any = await getSanatoriumPage({ page, pageSize });
      const records = res?.data?.records || [];
      totalCount = Number(res?.data?.total || 0);
      records.forEach((item: any) => {
        if (typeof item.id === 'number') list.push({ id: item.id, sanaName: item.sanaName || `机构#${item.id}` });
      });
      page += 1;
    } while (list.length < totalCount && totalCount > 0);
    sanatoriumOptions.value = list;
  } catch {
    /* ignore */
  }
};

const handleOrgChange = () => {
  formData.assigneeUserId = undefined;
  formData.elderId = undefined;
  loadNurseOptions();
  loadElderOptions();
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res: any = await getTaskTemplatePage({ ...pageQuery, ...queryForm });
    if (res?.code === 200 && res?.data) {
      tableData.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } catch {
    /* ignore */
  }
  loading.value = false;
};

const handleSearch = () => {
  pageQuery.page = 1;
  fetchData();
};

const handleReset = () => {
  queryForm.taskTitle = '';
  queryForm.enabled = undefined;
  pageQuery.page = 1;
  fetchData();
};

const openCreateDialog = () => {
  if (!canCreateTemplate.value) {
    ElMessage.warning('暂无新建模板权限');
    return;
  }
  dialogMode.value = 'create';
  selectedSanaId.value = undefined;
  editingCurrentNextExecuteTime.value = '';
  Object.assign(formData, defaultForm());
  dateRange.value = [];
  if (!isMultiScope.value) {
    loadNurseOptions();
    loadElderOptions();
  } else {
    nurseOptions.value = [];
    elderOptions.value = [];
  }
  dialogVisible.value = true;
};

const openEditDialog = (row: TaskTemplateRow) => {
  if (!canUpdateTemplate.value) {
    ElMessage.warning('暂无编辑模板权限');
    return;
  }
  dialogMode.value = 'edit';
  selectedSanaId.value = row.sanaId;
  editingCurrentNextExecuteTime.value = row.nextExecuteTime || '';
  Object.assign(formData, {
    id: row.id,
    taskTitle: row.taskTitle,
    taskContent: row.taskContent || '',
    elderId: row.elderId,
    taskType: row.taskType,
    priority: row.priority,
    assigneeUserId: row.assigneeUserId,
    timezone: row.timezone || DEFAULT_TIMEZONE,
    scheduleConfig: cloneScheduleConfig(row.scheduleConfig),
    plannedDuration: row.plannedDuration,
    startDate: row.startDate,
    endDate: row.endDate,
    remark: row.remark || '',
  } satisfies TaskTemplateCreate);
  handleScheduleTypeChange(formData.scheduleConfig.scheduleType);
  handleTimeModeChange(formData.scheduleConfig.timeMode);
  dateRange.value = row.startDate && row.endDate ? [row.startDate, row.endDate] : [];
  loadNurseOptions();
  loadElderOptions();
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (dialogMode.value === 'create' && !canCreateTemplate.value) {
    ElMessage.warning('暂无新建模板权限');
    return;
  }
  if (dialogMode.value === 'edit' && !canUpdateTemplate.value) {
    ElMessage.warning('暂无编辑模板权限');
    return;
  }
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  if (dateRange.value?.length === 2) {
    formData.startDate = dateRange.value[0];
    formData.endDate = dateRange.value[1];
  } else {
    formData.startDate = undefined;
    formData.endDate = undefined;
  }

  submitting.value = true;
  try {
    const payload: TaskTemplateCreate = {
      ...formData,
      sanaId: effectiveSanaId.value,
      timezone: formData.timezone || DEFAULT_TIMEZONE,
      scheduleConfig: normalizeScheduleConfigForSubmit(formData.scheduleConfig),
    };
    const res: any = dialogMode.value === 'create' ? await createTaskTemplate(payload) : await updateTaskTemplate(payload);
    if (res?.code === 200) {
      ElMessage.success(dialogMode.value === 'create' ? '创建成功' : '更新成功');
      dialogVisible.value = false;
      fetchData();
    } else {
      ElMessage.error(res?.message || '操作失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败');
  }
  submitting.value = false;
};

const handleToggle = async (row: TaskTemplateRow) => {
  if (!canToggleTemplate.value) {
    ElMessage.warning('暂无启禁用模板权限');
    return;
  }
  try {
    const res: any = await toggleTaskTemplate(row.id);
    if (res?.code === 200) {
      ElMessage.success(row.enabled === 1 ? '已禁用' : '已启用');
      fetchData();
    } else {
      ElMessage.error(res?.message || '操作失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败');
  }
};

const handleGenerate = async (row: TaskTemplateRow) => {
  if (!canGenerateTemplate.value) {
    ElMessage.warning('暂无手动生成权限');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定立即根据模板「${row.taskTitle}」生成一条任务吗？`, '手动生成', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    });
    const res: any = await generateFromTemplate(row.id);
    if (res?.code === 200) {
      ElMessage.success(`已生成 ${res.data || 0} 条任务`);
    } else {
      ElMessage.error(res?.message || '生成失败');
    }
  } catch {
    /* user cancel */
  }
};

const handleRemove = async (row: TaskTemplateRow) => {
  if (!canRemoveTemplate.value) {
    ElMessage.warning('暂无删除模板权限');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定删除模板「${row.taskTitle}」吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    const res: any = await removeTaskTemplate(row.id);
    if (res?.code === 200) {
      ElMessage.success('已删除');
      fetchData();
    } else {
      ElMessage.error(res?.message || '删除失败');
    }
  } catch {
    /* user cancel */
  }
};

onMounted(() => {
  loadDictOptions();
  loadSanatoriumOptions();
  fetchData();
});
</script>

<style scoped>
.template-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.page-desc { font-size: 13px; color: #909399; margin: 4px 0 0; }
.query-card { margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.schedule-cell { display: flex; flex-direction: column; gap: 4px; }
.schedule-cell__main { line-height: 1.5; color: #303133; }
.schedule-cell__sub { font-size: 12px; color: #909399; }
.form-section { padding: 18px 18px 4px; border: 1px solid #ebeef5; border-radius: 12px; background: #fff; }
.form-section + .form-section { margin-top: 16px; }
.section-head { margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 600; color: #303133; line-height: 1.4; }
.section-desc { margin-top: 4px; font-size: 12px; color: #909399; line-height: 1.6; }
.field-stack { width: 100%; }
.inline-field { display: inline-flex; align-items: center; }
.field-help { margin-top: 8px; font-size: 12px; color: #909399; line-height: 1.6; }
.impact-panel { margin-bottom: 18px; padding: 12px 14px; border-radius: 10px; background: #f8fafc; border: 1px solid #e4e7ed; }
.impact-row { display: flex; justify-content: space-between; gap: 16px; font-size: 13px; color: #606266; line-height: 1.8; }
.impact-row strong { color: #303133; font-weight: 600; }
.impact-tip { margin-top: 6px; font-size: 12px; color: #909399; line-height: 1.6; }
.schedule-builder { width: 100%; padding: 16px; border: 1px solid #e4e7ed; border-radius: 10px; background: #fafbfc; }
.builder-row { display: flex; align-items: flex-start; gap: 12px; }
.builder-row + .builder-row { margin-top: 14px; }
.builder-label { width: 88px; flex: 0 0 88px; line-height: 32px; color: #606266; }
.builder-control { flex: 1; min-width: 0; }
.interval-fields { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.interval-separator { color: #909399; }
.schedule-hint { margin-top: 8px; font-size: 12px; color: #909399; line-height: 1.5; }
.preview-panel { padding: 14px 16px; border-radius: 10px; border: 1px dashed #dcdfe6; background: #fafbfc; }
.preview-item { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; line-height: 1.7; }
.preview-item + .preview-item { margin-top: 8px; }
.preview-item__label { flex: 0 0 164px; color: #909399; font-size: 12px; }
.preview-item__value { flex: 1; color: #303133; font-size: 13px; text-align: right; word-break: break-word; }
.preview-tip { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #e4e7ed; color: #909399; font-size: 12px; line-height: 1.6; }
.field-unit { margin-left: 8px; color: #909399; }

.template-dialog :deep(.el-dialog__body) {
  max-height: 72vh;
  overflow-y: auto;
  scrollbar-width: none;
}

.template-dialog :deep(.el-dialog__body::-webkit-scrollbar) {
  width: 0;
  height: 0;
}

@media (max-width: 768px) {
  .page-header { flex-direction: column; align-items: flex-start; gap: 12px; }
  .builder-row,
  .preview-item,
  .impact-row { flex-direction: column; }
  .builder-label,
  .preview-item__label { width: auto; flex: none; }
  .preview-item__value { text-align: left; }
}
</style>
