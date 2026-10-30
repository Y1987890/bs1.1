import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, Briefcase, ChevronDown, ChevronUp, GraduationCap, Loader2, LogIn, ShieldCheck, Sparkles,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import type { Role } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../api/request';
import { ROUTES } from '../constants/routes';

interface RoleConfig {
  role: Role;
  title: string;
  subtitle: string;
  description: string;
  icon: LucideIcon;
  accentBadge: string;
  iconBg: string;
  demo: { username: string; password: string };
}

const ROLE_CONFIGS: Record<Role, RoleConfig> = {
  CANDIDATE: {
    role: 'CANDIDATE',
    title: '求职者登录',
    subtitle: '训练你的求职能力',
    description: '浏览岗位要求、制定训练计划、参加模拟面试、查看证据化评价与成长趋势。',
    icon: GraduationCap,
    accentBadge: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
    iconBg: 'from-blue-500 to-blue-600',
    demo: { username: 'candidate', password: 'candidate123' },
  },
  INTERVIEWER: {
    role: 'INTERVIEWER',
    title: '面试官 / 就业指导教师登录',
    subtitle: '管理与评价你负责的训练',
    description: '创建并发布岗位能力模型、查看候选人面试记录、跟进训练效果。',
    icon: Briefcase,
    accentBadge: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400',
    iconBg: 'from-emerald-500 to-emerald-600',
    demo: { username: 'interviewer', password: 'interviewer123' },
  },
  ADMIN: {
    role: 'ADMIN',
    title: '管理员登录',
    subtitle: '平台运营与治理',
    description: '管理用户与角色、审核岗位、维护知识库与模型服务配置、查看审计日志。',
    icon: ShieldCheck,
    accentBadge: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400',
    iconBg: 'from-amber-500 to-amber-600',
    demo: { username: 'admin', password: 'admin123' },
  },
};

const inputClass = `w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700
  bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-white
  placeholder:text-slate-400 focus:outline-none focus:ring-2
  focus:ring-primary-500/50 focus:border-primary-400 transition-shadow`;

export default function RoleLoginPage({ role }: { role: Role }) {
  const config = ROLE_CONFIGS[role];
  const { login, register, user } = useAuth();
  const navigate = useNavigate();
  const Icon = config.icon;

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [showRegister, setShowRegister] = useState(false);
  const [regUsername, setRegUsername] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regDisplayName, setRegDisplayName] = useState('');
  const [regError, setRegError] = useState('');
  const [regLoading, setRegLoading] = useState(false);

  // 已登录用户访问登录页：直接回到工作台
  if (user) {
    return <Navigate to={ROUTES.dashboard} replace />;
  }

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('请输入用户名和密码');
      return;
    }
    setError('');
    setLoading(true);
    try {
      // 入口角色由后端校验：角色不符会返回明确错误，不会签发 token
      await login(username.trim(), password, role);
      navigate(ROUTES.dashboard, { replace: true });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();
    if (!regUsername.trim() || !regPassword.trim()) {
      setRegError('请输入用户名和密码');
      return;
    }
    setRegError('');
    setRegLoading(true);
    try {
      // 注册仅创建 CANDIDATE 账号，注册后自动登录本入口
      await register(regUsername.trim(), regPassword, regDisplayName.trim() || undefined);
      navigate(ROUTES.dashboard, { replace: true });
    } catch (err) {
      setRegError(getErrorMessage(err));
    } finally {
      setRegLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-white to-indigo-50 dark:from-slate-950 dark:via-slate-900 dark:to-slate-800 p-4">
      <div className="w-full max-w-md">
        {/* 品牌与角色标识 */}
        <div className="mb-6 flex justify-center">
          <div className="inline-flex items-center gap-3">
            <div className="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-600 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-primary-500/30">
              <Sparkles className="w-6 h-6" />
            </div>
            <div className="text-left">
              <h1 className="text-xl font-bold text-slate-800 dark:text-white tracking-tight">职跃</h1>
              <p className="text-xs text-slate-400 dark:text-slate-500">校园求职训练与面试能力成长平台</p>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6">
          {/* 角色头部 */}
          <div className="mb-5 flex items-start gap-3">
            <div className={`w-11 h-11 shrink-0 rounded-xl bg-gradient-to-br ${config.iconBg} flex items-center justify-center text-white shadow-md`}>
              <Icon className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">{config.title}</h2>
              <p className="text-xs font-medium text-primary-600 dark:text-primary-400">{config.subtitle}</p>
              <p className="mt-1.5 text-xs leading-relaxed text-slate-500 dark:text-slate-400">{config.description}</p>
            </div>
          </div>

          {/* 登录表单 */}
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">用户名</label>
              <input className={inputClass} value={username} onChange={(e) => setUsername(e.target.value)} placeholder="请输入用户名" autoComplete="username" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">密码</label>
              <input className={inputClass} type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="请输入密码" autoComplete="current-password" />
            </div>
            {error && (
              <div className="rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-100 dark:border-red-900/40 px-3 py-2.5">
                <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
              </div>
            )}
            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl font-semibold text-sm text-white bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 shadow-lg shadow-primary-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <LogIn className="w-4 h-4" />}
              {loading ? '登录中...' : '登录'}
            </button>
          </form>

          {/* 演示账号（仅本地演示数据，由开发 profile 种子提供） */}
          <div className="mt-5 pt-4 border-t border-slate-100 dark:border-slate-700">
            <button
              onClick={() => { setUsername(config.demo.username); setPassword(config.demo.password); setError(''); }}
              className="w-full flex items-center justify-between gap-2 p-3 rounded-xl border border-slate-200 dark:border-slate-700 hover:border-primary-300 dark:hover:border-primary-600 hover:bg-primary-50/50 dark:hover:bg-primary-900/20 transition-colors"
            >
              <span className="flex items-center gap-2.5">
                <span className={`w-8 h-8 rounded-lg flex items-center justify-center ${config.accentBadge}`}>
                  <Icon className="w-4 h-4" />
                </span>
                <span className="text-left">
                  <span className="block text-xs font-medium text-slate-600 dark:text-slate-300">使用演示账号快速体验</span>
                  <span className="block text-[11px] text-slate-400 dark:text-slate-500">{config.demo.username} / {config.demo.password}</span>
                </span>
              </span>
              <span className="text-xs font-medium text-primary-500">填入</span>
            </button>
          </div>

          {/* 求职者注册（注册只创建 CANDIDATE） */}
          {role === 'CANDIDATE' && (
            <div className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-700">
              <button
                onClick={() => setShowRegister(!showRegister)}
                className="w-full flex items-center justify-center gap-1.5 text-sm text-slate-500 dark:text-slate-400 hover:text-primary-500 transition-colors"
              >
                {showRegister ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                没有账号？注册求职者账号
              </button>
              {showRegister && (
                <form onSubmit={handleRegister} className="space-y-3 mt-4">
                  <input className={inputClass} value={regUsername} onChange={(e) => setRegUsername(e.target.value)} placeholder="设置登录用户名" />
                  <input className={inputClass} type="password" value={regPassword} onChange={(e) => setRegPassword(e.target.value)} placeholder="设置密码" />
                  <input className={inputClass} value={regDisplayName} onChange={(e) => setRegDisplayName(e.target.value)} placeholder="昵称（可选）" />
                  {regError && <p className="text-sm text-red-500">{regError}</p>}
                  <button
                    type="submit"
                    disabled={regLoading}
                    className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl font-semibold text-sm text-white bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-600 hover:to-emerald-700 shadow-lg shadow-emerald-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {regLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <LogIn className="w-4 h-4" />}
                    {regLoading ? '注册中...' : '注册并登录'}
                  </button>
                </form>
              )}
            </div>
          )}
        </div>

        {/* 返回身份选择 */}
        <div className="mt-5 text-center">
          <Link to={ROUTES.login} className="inline-flex items-center gap-1.5 text-sm text-slate-400 dark:text-slate-500 hover:text-primary-500 transition-colors">
            <ArrowLeft className="w-4 h-4" />
            返回身份选择
          </Link>
        </div>
      </div>
    </div>
  );
}
