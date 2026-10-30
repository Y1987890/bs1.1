import { Navigate, Link } from 'react-router-dom';
import { ArrowRight, Briefcase, GraduationCap, ShieldCheck, Sparkles } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { ROUTES } from '../constants/routes';

interface Entrance {
  role: string;
  title: string;
  description: string;
  icon: LucideIcon;
  iconBg: string;
  ring: string;
  to: string;
}

const ENTRANCES: Entrance[] = [
  {
    role: '求职者',
    title: '我是求职者',
    description: '岗位探索 · 训练计划 · 模拟面试 · 成长档案',
    icon: GraduationCap,
    iconBg: 'from-blue-500 to-blue-600',
    ring: 'hover:border-blue-300 dark:hover:border-blue-700 hover:shadow-blue-500/10',
    to: ROUTES.loginCandidate,
  },
  {
    role: '面试官',
    title: '我是面试官 / 就业指导教师',
    description: '岗位能力模型 · 候选人面试记录 · 训练效果',
    icon: Briefcase,
    iconBg: 'from-emerald-500 to-emerald-600',
    ring: 'hover:border-emerald-300 dark:hover:border-emerald-700 hover:shadow-emerald-500/10',
    to: ROUTES.loginInterviewer,
  },
  {
    role: '管理员',
    title: '我是平台管理员',
    description: '用户与角色 · 岗位审核 · AI 服务配置 · 审计',
    icon: ShieldCheck,
    iconBg: 'from-amber-500 to-amber-600',
    ring: 'hover:border-amber-300 dark:hover:border-amber-700 hover:shadow-amber-500/10',
    to: ROUTES.loginAdmin,
  },
];

/**
 * 身份选择页（/login）：三个角色独立登录入口的导航中枢。
 * 未登录用户访问受保护页面时统一跳转到这里，再选择对应身份入口。
 */
export default function LoginPage() {
  const { user } = useAuth();

  // 已登录用户访问选择页：直接回到工作台
  if (user) {
    return <Navigate to={ROUTES.dashboard} replace />;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-white to-indigo-50 dark:from-slate-950 dark:via-slate-900 dark:to-slate-800 p-4">
      <div className="w-full max-w-lg">
        {/* 品牌 */}
        <div className="mb-8 flex justify-center">
          <div className="inline-flex items-center gap-3">
            <div className="w-12 h-12 bg-gradient-to-br from-primary-500 to-primary-600 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-primary-500/30">
              <Sparkles className="w-6 h-6" />
            </div>
            <div className="text-left">
              <h1 className="text-2xl font-bold text-slate-800 dark:text-white tracking-tight">职跃</h1>
              <p className="text-xs text-slate-400 dark:text-slate-500">校园求职训练与面试能力成长平台</p>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6">
          <div className="text-center mb-6">
            <h2 className="text-lg font-bold text-slate-900 dark:text-white">选择你的身份</h2>
            <p className="mt-1 text-xs text-slate-400 dark:text-slate-500"></p>
          </div>

          <div className="space-y-3">
            {ENTRANCES.map((entrance) => {
              const Icon = entrance.icon;
              return (
                <Link
                  key={entrance.role}
                  to={entrance.to}
                  className={`group flex items-center gap-4 p-4 rounded-xl border border-slate-200 dark:border-slate-700 hover:shadow-lg transition-all ${entrance.ring}`}
                >
                  <div className={`w-11 h-11 shrink-0 rounded-xl bg-gradient-to-br ${entrance.iconBg} flex items-center justify-center text-white shadow-md`}>
                    <Icon className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-slate-800 dark:text-white">{entrance.title}</p>
                    <p className="mt-0.5 text-xs text-slate-400 dark:text-slate-500 truncate">{entrance.description}</p>
                  </div>
                  <ArrowRight className="w-4 h-4 text-slate-300 dark:text-slate-600 group-hover:text-primary-500 transition-colors shrink-0" />
                </Link>
              );
            })}
          </div>

          <p className="mt-6 text-center text-[11px] leading-relaxed text-slate-400 dark:text-slate-500">
            还没有账号？前往 <Link to={ROUTES.loginCandidate} className="text-primary-500 hover:underline">求职者登录入口</Link> 注册（注册仅创建求职者账号）
          </p>
        </div>
      </div>
    </div>
  );
}
