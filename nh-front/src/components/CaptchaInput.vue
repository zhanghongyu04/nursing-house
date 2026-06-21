<script setup lang="ts">
import { ref, onMounted } from "vue";
import { RefreshRight } from "@element-plus/icons-vue";
import { getCaptchaAPI } from "@/api/user";
import { ElMessage } from "element-plus";

const props = defineProps<{
  modelValue?: string;
  name?: string;
  placeholder?: string;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: string): void;
  (e: "captcha-loaded", key: string): void;
}>();

const captchaImage = ref<string>("");
const captchaKey = ref<string>("");
const isLoading = ref(false);

/**
 * 加载验证码
 */
const loadCaptcha = async () => {
  if (isLoading.value) return;

  try {
    isLoading.value = true;
    const { captchaKey: key, captchaImage: image } = await getCaptchaAPI();

    captchaImage.value = image;
    captchaKey.value = key;

    emit("captcha-loaded", key);
  } catch (error: any) {
    console.error("加载验证码失败:", error);
    ElMessage.error(error?.message || "验证码加载失败，请刷新重试");
  } finally {
    isLoading.value = false;
  }
};

/**
 * 刷新验证码
 */
const refreshCaptcha = () => {
  loadCaptcha();
};

/**
 * 输入事件处理
 */
const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  emit("update:modelValue", target.value);
};

// 组件挂载时自动加载验证码
onMounted(() => {
  loadCaptcha();
});

// 暴露给父组件的方法和数据
defineExpose({
  captchaKey,
  refreshCaptcha,
});
</script>

<template>
  <div class="captcha-input-wrapper">
    <div class="input-wrapper">
      <input
        type="text"
        :name="name || 'captchaCode'"
        :placeholder="placeholder || '请输入验证码'"
        :value="modelValue"
        @input="handleInput"
        class="form-input captcha-field"
        autocomplete="off"
      />
      <div class="captcha-image-wrapper" @click="refreshCaptcha" :class="{ loading: isLoading }">
        <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-image" />
        <div v-else class="captcha-placeholder">
          <span v-if="!isLoading">加载</span>
          <span v-else>...</span>
        </div>
        <div class="captcha-refresh-hint">
          <el-icon :class="{ rotating: isLoading }"><RefreshRight /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.captcha-input-wrapper {
  width: 100%;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-field {
  flex: 1;
  min-width: 0;
  height: clamp(46px, 5vw, 48px);
  padding: 0 14px;
  border: 1.5px solid #d0dfee;
  border-radius: 12px;
  font-size: 15px;
  color: #1e3557;
  background: #f8fbff;
  transition: all 0.25s ease;
  box-sizing: border-box;
}

.captcha-field::placeholder {
  color: #8ba3c7;
  font-size: 14px;
}

.captcha-field:hover {
  border-color: #a8c4e8;
  background: #ffffff;
}

.captcha-field:focus {
  outline: none;
  border-color: #4f83da;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(79, 131, 218, 0.12);
}

.captcha-image-wrapper {
  position: relative;
  flex-shrink: 0;
  width: 110px;
  height: clamp(46px, 5vw, 48px);
  border: 1.5px solid #d0dfee;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: #f8fbff;
  transition: all 0.25s ease;
}

.captcha-image-wrapper:hover {
  border-color: #a8c4e8;
  background: #ffffff;
}

.captcha-image-wrapper.loading {
  cursor: wait;
  opacity: 0.7;
}

.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #8ba3c7;
}

.captcha-refresh-hint {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0);
  transition: background 0.2s ease;
}

.captcha-image-wrapper:hover .captcha-refresh-hint {
  background: rgba(79, 131, 218, 0.15);
}

.captcha-refresh-hint .el-icon {
  font-size: 18px;
  color: #4f83da;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.captcha-image-wrapper:hover .captcha-refresh-hint .el-icon {
  opacity: 1;
}

.captcha-refresh-hint .el-icon.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .input-wrapper {
    flex-direction: column;
    gap: 10px;
  }

  .captcha-image-wrapper {
    width: 100%;
    height: 46px;
  }

  .captcha-field {
    width: 100%;
  }
}
</style>
