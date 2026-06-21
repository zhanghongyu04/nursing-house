<template>
  <!-- 通用任务详情弹窗，机构侧和护理端共用同一份展示结构。 -->
  <el-dialog v-model="visible" title="任务详情" :width="width">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="任务标题">{{ task?.taskTitle || '-' }}</el-descriptions-item>
      <el-descriptions-item label="任务类型">{{ task ? dictLabel(taskTypeOptions, task.taskType) : '-' }}</el-descriptions-item>
      <el-descriptions-item label="优先级">
        <el-tag v-if="task" :type="priorityTagType(task.priority)" size="small">
          {{ dictLabel(priorityOptions, task.priority) }}
        </el-tag>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag v-if="task" :type="statusTagType(task.status)" size="small">
          {{ dictLabel(statusOptions, task.status) }}
        </el-tag>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="关联老人">{{ task?.elderName || '-' }}</el-descriptions-item>
      <el-descriptions-item v-if="showAssignee" label="执行人">{{ task?.assigneeUsername || '-' }}</el-descriptions-item>
      <el-descriptions-item label="下发人">{{ task?.assignerUsername || '-' }}</el-descriptions-item>
      <el-descriptions-item label="所属机构">{{ task?.sanaName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="计划开始">{{ task?.plannedStartTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="计划结束">{{ task?.plannedEndTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="完成时间">{{ task?.completionTime || '-' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="任务内容">
        <div class="multiline-text">{{ task?.taskContent || '-' }}</div>
      </el-descriptions-item>
      <el-descriptions-item :span="2" label="备注">
        <div class="multiline-text">{{ task?.remark || '-' }}</div>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { NursingTaskRow } from '@/api/nursingTask';

// 详情弹窗内使用的字典值结构，与页面字典缓存保持一致。
interface DictOption {
  value: number;
  label: string;
}

const props = withDefaults(defineProps<{
  modelValue: boolean;
  task: NursingTaskRow | null;
  dictOptions: Record<string, DictOption[]>;
  priorityTagType?: (value: number) => string;
  statusTagType?: (value: number) => string;
  showAssignee?: boolean;
  width?: string;
}>(), {
  priorityTagType: () => '',
  statusTagType: () => 'info',
  showAssignee: true,
  width: '680px',
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
}>();

// 通过计算属性把 dialog 的双向绑定收敛到组件内部。
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
});

const taskTypeOptions = computed(() => props.dictOptions.taskType || []);
const priorityOptions = computed(() => props.dictOptions.priority || []);
const statusOptions = computed(() => props.dictOptions.status || []);

// 将数值型字典值转换成可读文本。
const dictLabel = (options: DictOption[], value: number) => {
  return options.find((item) => item.value === value)?.label ?? '-';
};
</script>

<style scoped>
.multiline-text {
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
