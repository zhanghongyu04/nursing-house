<script setup lang="ts">
import { computed, ref } from "vue";
import { useUserStore } from "@/stores/userStore";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import CaptchaInput from "@/components/CaptchaInput.vue";
import { User, Lock } from "@element-plus/icons-vue";

// 登录表单数据
const username = ref("");
const password = ref("");
const captchaCode = ref("");
const captchaKey = ref("");
const isSubmitting = ref(false);
const isLoadingSuccess = ref(false);

// 获取用户 Store 和路由对象
const userStore = useUserStore();
const router = useRouter();
const route = useRoute();
const currentYear = new Date().getFullYear();
const canSubmit = computed(() => username.value.trim() && password.value.trim() && captchaCode.value.trim());
const getSafeRedirectPath = () => {
  const redirect = route.query.redirect;
  if (typeof redirect === "string" && redirect.startsWith("/") && !redirect.startsWith("//")) {
    return redirect;
  }
  return "/";
};

// 验证码加载完成回调
const handleCaptchaLoaded = (key: string) => {
  captchaKey.value = key;
};

// 刷新验证码
const captchaRef = ref<InstanceType<typeof CaptchaInput> | null>(null);

const getLoginErrorMessage = (error: any) => {
  const backendMessage = error?.response?.data?.message;
  if (typeof backendMessage === "string" && backendMessage.trim()) {
    return backendMessage.trim();
  }
  return "登录失败，请稍后重试";
};

// 登录逻辑
const handleLogin = async () => {
  if (!username.value || !password.value || !captchaCode.value || isSubmitting.value) {
    ElMessage.warning("请输入账号、密码和验证码");
    return;
  }

  if (!captchaKey.value) {
    ElMessage.warning("请等待验证码加载完成");
    return;
  }

  try {
    isSubmitting.value = true;
    await userStore.login({
      username: username.value,
      password: password.value,
      captchaKey: captchaKey.value,
      captchaCode: captchaCode.value,
    });

    // 登录成功，显示加载动画
    isLoadingSuccess.value = true;

    // 延迟 1.5 秒后跳转
    setTimeout(() => {
      router.replace(getSafeRedirectPath());
    }, 1500);
  } catch (error: any) {
    const message = getLoginErrorMessage(error);
    console.warn("登录失败:", {
      status: error?.response?.status,
      message,
    });
    ElMessage.error({
      message,
      duration: 3000,
    });

    // 登录失败后刷新验证码
    captchaRef.value?.refreshCaptcha();
    captchaCode.value = "";
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="login-page">
    <div class="login-page__overlay"></div>
    <div class="login-container">
      <div class="login-shell">
        <!-- 左侧品牌区域 -->
        <section class="login-brand">
          <div class="brand-topline">
            <div class="brand-header">
              <img src="../../assets/logo/logo.png" alt="Logo" class="brand-logo" />
              <div class="brand-titles">
                <p class="brand-badge">政 · 院 · 护 协同平台</p>
                <h1 class="brand-title">智慧康养管理系统</h1>
              </div>
            </div>
            <span class="brand-chip">统一监管 · 机构运营 · 护理服务</span>
          </div>

          <div class="brand-content">
            <h2 class="brand-heading">统一监管、机构运营与护理服务入口</h2>
            <p class="brand-desc">
              面向政府监管端、养老机构端与护理执行端，提供机构概览、业务协同、智能问答与监测能力的一体化管理入口。
            </p>
            <div class="brand-highlight">
              <p class="highlight-label">核心场景</p>
              <p class="highlight-value">机构数据全局掌握，床位与老人状态联动，智能问答辅助业务执行。</p>
            </div>
          </div>

          <div class="brand-features">
            <div class="feature-item">
              <span class="feature-label">监管协同</span>
              <span class="feature-value">机构态势一屏掌握</span>
            </div>
            <div class="feature-item">
              <span class="feature-label">运营管理</span>
              <span class="feature-value">床位、老人、任务联动</span>
            </div>
            <div class="feature-item">
              <span class="feature-label">智能服务</span>
              <span class="feature-value">知识问答与业务辅助</span>
            </div>
          </div>

          <div class="brand-metrics">
            <div class="metric-item">
              <span class="metric-label">角色端统一入口</span>
              <strong>政院护协同</strong>
            </div>
            <div class="metric-item">
              <span class="metric-label">平台级协同闭环</span>
              <strong>业务联动</strong>
            </div>
            <div class="metric-item">
              <span class="metric-label">运行与监测支持</span>
              <strong>全天在线</strong>
            </div>
          </div>

          <div class="brand-footer">
            <span>Copyright © {{ currentYear }} 智慧康养. All Rights Reserved.</span>
          </div>
        </section>

        <!-- 右侧登录区域 -->
        <section class="login-panel">
          <div class="panel-header">
            <p class="panel-badge">用户登录</p>
            <h2 class="panel-title">登录系统</h2>
            <p class="panel-desc">请输入平台分配的账号和密码进行身份验证</p>
          </div>

          <form @submit.prevent="handleLogin" class="login-form">
            <div class="form-field">
              <label class="field-label" for="username">账号</label>
              <div class="input-group">
                <el-icon class="input-icon"><User /></el-icon>
                <input
                  id="username"
                  type="text"
                  v-model="username"
                  placeholder="请输入账号"
                  class="form-input"
                  autocomplete="username"
                />
              </div>
            </div>

            <div class="form-field">
              <label class="field-label" for="password">密码</label>
              <div class="input-group">
                <el-icon class="input-icon"><Lock /></el-icon>
                <input
                  id="password"
                  type="password"
                  v-model="password"
                  placeholder="请输入密码"
                  class="form-input"
                  autocomplete="current-password"
                />
              </div>
            </div>

            <div class="form-field">
              <label class="field-label" for="captcha">验证码</label>
              <CaptchaInput
                ref="captchaRef"
                v-model="captchaCode"
                name="captchaCode"
                placeholder="请输入验证码"
                @captcha-loaded="handleCaptchaLoaded"
              />
            </div>

            <button type="submit" class="login-btn" :disabled="!canSubmit || isSubmitting">
              {{ isSubmitting ? "登录中..." : "登录" }}
            </button>
          </form>

          <div class="panel-footer">
            <p>没有账号？请联系系统管理员或所属机构负责人开通。</p>
          </div>
        </section>
      </div>
    </div>

    <!-- 登录成功加载动画 -->
    <Transition name="fade">
      <div v-if="isLoadingSuccess" class="login-success-overlay">
        <div class="loading-content">
          <div class="loading-spinner">
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
          </div>
          <p class="loading-text">登录成功，正在进入系统...</p>
          <p class="loading-welcome">欢迎，{{ userStore.userInfo.username || "用户" }}</p>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* ========== 基础布局 ========== */
.login-page {
  position: relative;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-x: hidden;
  overflow-y: auto;
  background: url('@/assets/images/login-background/LoginBackground.png') center center / cover no-repeat;
  padding: 16px;
  box-sizing: border-box;
}

.login-page__overlay {
  display: none;
}

.login-container {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.login-shell {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 600px;
  max-height: 90vh;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  border: 1px solid rgba(170, 191, 220, 0.28);
  box-shadow: 0 2px 8px rgba(18, 43, 79, 0.12);
  backdrop-filter: blur(14px);
  overflow: hidden;
}

/* ========== 品牌区域 ========== */
.login-brand {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
  padding: 40px;
  color: #fff;
  background:
    linear-gradient(145deg, rgba(109, 140, 207, 0.96) 0%, rgba(125, 161, 231, 0.96) 52%, rgba(146, 180, 239, 0.95) 100%),
    url('@/assets/images/login-background/LoginBackground.png') center center / cover no-repeat;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 24px 24px;
  opacity: 0.06;
  pointer-events: none;
}

.login-brand > * {
  position: relative;
  z-index: 1;
}

.brand-topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-logo {
  width: 52px;
  height: 52px;
  display: block;
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: contain;
}

.brand-titles {
  flex: 1;
  min-width: 0;
}

.brand-badge {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 0.18em;
  color: rgba(245, 248, 255, 0.9);
}

.brand-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.25;
  font-weight: 700;
}

.brand-chip {
  flex-shrink: 0;
  margin-top: 2px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(243, 247, 255, 0.18);
  border: 1px solid rgba(242, 247, 255, 0.24);
  color: rgba(249, 251, 255, 0.98);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  backdrop-filter: blur(4px);
}

.brand-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 20px 0;
}

.brand-heading {
  margin: 0 0 16px;
  font-size: 30px;
  line-height: 1.3;
  font-weight: 700;
  letter-spacing: 0;
}

.brand-desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.75;
  color: rgba(247, 250, 255, 0.94);
  max-width: 28em;
}

.brand-highlight {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 4px;
  background: rgba(236, 242, 252, 0.2);
  border: 1px solid rgba(239, 245, 255, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.highlight-label {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: rgba(244, 248, 255, 0.88);
}

.highlight-value {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(250, 252, 255, 0.98);
}

.brand-features {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  padding: 14px 16px;
  border-radius: 2px;
  background: rgba(239, 244, 252, 0.18);
  border: 1px solid rgba(244, 248, 255, 0.2);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.feature-label {
  font-size: 13px;
  color: rgba(244, 248, 255, 0.88);
  margin-bottom: 4px;
}

.feature-value {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}

.brand-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  padding: 14px 16px;
  border-radius: 4px;
  background: rgba(236, 242, 252, 0.18);
  border: 1px solid rgba(244, 248, 255, 0.18);
  backdrop-filter: blur(4px);
}

.metric-item strong {
  display: block;
  font-size: 16px;
  line-height: 1.35;
  margin-top: 2px;
}

.metric-label {
  display: block;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(243, 247, 255, 0.88);
  margin-bottom: 6px;
}

.brand-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid rgba(244, 248, 255, 0.22);
  font-size: 13px;
  color: rgba(243, 247, 255, 0.88);
  gap: 12px;
}

/* ========== 登录区域 ========== */
.login-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px;
  background: rgba(255, 255, 255, 0.96);
  overflow-y: auto;
}

.panel-header {
  margin-bottom: 32px;
}

.panel-badge {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: #6a86ad;
}

.panel-title {
  margin: 0 0 12px;
  font-size: 28px;
  line-height: 1.2;
  color: #183761;
}

.panel-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  color: #60799e;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 14px;
  font-weight: 600;
  color: #2a4a7a;
  letter-spacing: 0.02em;
}

.input-group {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  font-size: 18px;
  color: #7a9ccb;
  z-index: 1;
  transition: color 0.2s ease;
}

.form-input {
  width: 100%;
  height: 48px;
  padding: 0 14px 0 44px;
  border: 1.5px solid #d0dfee;
  border-radius: 2px;
  font-size: 15px;
  color: #1e3557;
  background: #f8fbff;
  transition: all 0.25s ease;
  box-sizing: border-box;
}

.form-input::placeholder {
  color: #8ba3c7;
  font-size: 14px;
}

.form-input:hover {
  border-color: #a8c4e8;
  background: #ffffff;
}

.form-input:focus {
  outline: none;
  border-color: #4f83da;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(79, 131, 218, 0.12);
}

.input-group:focus-within .input-icon {
  color: #4f83da;
}

.login-btn {
  height: 50px;
  margin-top: 8px;
  border: none;
  border-radius: 2px;
  background: linear-gradient(180deg, #4f83da 0%, #3d6fc5 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 14px rgba(61, 111, 197, 0.2);
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(61, 111, 197, 0.32);
  background: linear-gradient(180deg, #5a91e3 0%, #477dd0 100%);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(61, 111, 197, 0.24);
}

.login-btn:disabled {
  cursor: not-allowed;
  opacity: 0.56;
  box-shadow: none;
  background: linear-gradient(180deg, #8fa8d1 0%, #7a9cc7 100%);
}

.panel-footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(202, 216, 235, 0.56);
}

.panel-footer p {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: #6c81a1;
}

/* ========== 响应式设计 ========== */

/* 平板尺寸 (1024px 以下) */
@media (max-width: 1024px) {
  .login-page {
    padding: 12px;
  }

  .login-shell {
    min-height: 550px;
    max-height: 85vh;
  }

  .login-brand {
    padding: 32px;
  }

  .brand-heading {
    font-size: 24px;
  }

  .brand-desc {
    font-size: 14px;
  }

  .login-panel {
    padding: 32px;
  }

  .panel-title {
    font-size: 26px;
  }
}

/* 小平板/大手机 (768px 以下) */
@media (max-width: 768px) {
  .login-page {
    padding: 0;
    align-items: stretch;
  }

  .login-container {
    max-width: 100%;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: auto;
    max-height: none;
    border-radius: 0;
    border: none;
    box-shadow: none;
  }

  .login-brand {
    padding: 32px 24px 24px;
    min-height: auto;
  }

  .brand-topline {
    flex-direction: column;
    align-items: flex-start;
  }

  .brand-header {
    gap: 12px;
  }

  .brand-logo {
    width: 44px;
    height: 44px;
  }

  .brand-title {
    font-size: 22px;
  }

  .brand-content {
    padding: 16px 0;
  }

  .brand-heading {
    font-size: 22px;
  }

  .brand-desc {
    font-size: 14px;
  }

  .brand-features {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .feature-item {
    padding: 12px;
  }

  .brand-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .feature-label {
    font-size: 12px;
  }

  .feature-value {
    font-size: 14px;
  }

  .brand-footer {
    flex-direction: column;
    align-items: flex-start;
    font-size: 12px;
  }

  .login-panel {
    padding: 24px;
  }

  .panel-header {
    margin-bottom: 24px;
  }

  .panel-title {
    font-size: 24px;
  }

  .login-form {
    gap: 18px;
  }
}

/* 手机 (480px 以下) */
@media (max-width: 480px) {
  .login-brand {
    padding: 24px 20px 20px;
  }

  .brand-header {
    gap: 10px;
  }

  .brand-logo {
    width: 40px;
    height: 40px;
  }

  .brand-title {
    font-size: 20px;
  }

  .brand-badge {
    font-size: 12px;
  }

  .brand-chip {
    white-space: normal;
  }

  .brand-heading {
    font-size: 20px;
    margin-bottom: 12px;
  }

  .brand-desc {
    font-size: 13px;
    line-height: 1.65;
  }

  .brand-features {
    gap: 6px;
    grid-template-columns: 1fr;
  }

  .feature-item {
    padding: 10px 12px;
  }

  .brand-metrics {
    grid-template-columns: 1fr;
  }

  .login-panel {
    padding: 20px;
  }

  .panel-badge {
    font-size: 12px;
  }

  .panel-title {
    font-size: 22px;
  }

  .panel-desc {
    font-size: 13px;
  }

  .login-form {
    gap: 16px;
  }

  .field-label {
    font-size: 13px;
  }

  .form-input {
    height: 46px;
    font-size: 14px;
  }

  .input-icon {
    font-size: 16px;
    left: 12px;
  }

  .form-input {
    padding-left: 40px;
  }

  .login-btn {
    height: 48px;
    font-size: 15px;
  }

  .panel-footer p {
    font-size: 12px;
  }
}

/* 超小屏幕 (360px 以下) */
@media (max-width: 360px) {
  .login-brand,
  .login-panel {
    padding: 20px 16px;
  }

  .brand-logo {
    width: 36px;
    height: 36px;
  }

  .brand-title {
    font-size: 18px;
  }

  .brand-heading {
    font-size: 18px;
  }

  .feature-item {
    padding: 8px 10px;
  }

  .feature-value {
    font-size: 13px;
  }

  .panel-title {
    font-size: 20px;
  }

  .form-input {
    height: 44px;
  }
}

/* 短屏幕适配 */
@media (max-height: 700px) {
  .login-page {
    padding: 8px;
  }

  .login-shell {
    min-height: auto;
    max-height: 95vh;
  }

  .login-brand {
    padding: 24px 32px;
  }

  .brand-content {
    padding: 12px 0;
  }

  .brand-desc {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .brand-features {
    gap: 8px;
  }

  .feature-item {
    padding: 10px 12px;
  }

  .login-panel {
    padding: 24px 32px;
  }

  .panel-header {
    margin-bottom: 20px;
  }

  .login-form {
    gap: 16px;
  }

  .login-btn {
    margin-top: 4px;
  }
}

@media (max-height: 600px) and (min-width: 769px) {
  .login-brand {
    padding: 20px 32px;
  }

  .brand-content {
    display: none;
  }

  .brand-features {
    flex-direction: row;
    gap: 8px;
  }

  .feature-item {
    flex: 1;
    padding: 10px;
  }

  .feature-label {
    font-size: 12px;
  }

  .feature-value {
    font-size: 13px;
  }

  .brand-footer {
    padding-top: 12px;
  }
}

/* ========== 登录成功加载动画 ========== */
.login-success-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
}

.loading-content {
  text-align: center;
}

.loading-spinner {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
}

.spinner-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 3px solid transparent;
  border-top-color: #4f83da;
  border-radius: 50%;
  animation: spin 1.2s linear infinite;
}

.spinner-ring:nth-child(1) {
  animation-delay: 0s;
}

.spinner-ring:nth-child(2) {
  animation-delay: -0.3s;
  width: calc(100% - 12px);
  height: calc(100% - 12px);
  top: 6px;
  left: 6px;
  border-top-color: #6b9df0;
}

.spinner-ring:nth-child(3) {
  animation-delay: -0.6s;
  width: calc(100% - 24px);
  height: calc(100% - 24px);
  top: 12px;
  left: 12px;
  border-top-color: #7aa8f0;
}

.spinner-ring:nth-child(4) {
  animation-delay: -0.9s;
  width: calc(100% - 36px);
  height: calc(100% - 36px);
  top: 18px;
  left: 18px;
  border-top-color: #a8c4f8;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-text {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #2a4a7a;
}

.loading-welcome {
  margin: 0;
  font-size: 14px;
  color: #6c81a1;
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
