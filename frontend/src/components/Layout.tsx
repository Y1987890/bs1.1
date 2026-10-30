import {Link, Outlet, useLocation, useNavigate} from 'react-router-dom';
import {motion} from 'framer-motion';
import {
  Briefcase, Calendar, ChevronRight, Database, FileStack, LayoutDashboard, LogOut,
  Moon, ScrollText, Settings, Sparkles, Sun, Target, TrendingUp, Upload, UserCog, Users,
} from 'lucide-react';
import {useTheme} from '../hooks/useTheme';
import {useState} from 'react';
import UnifiedInterviewModal, {UnifiedInterviewConfig} from './UnifiedInterviewModal';
import {useAuth} from '../context/AuthContext';
import type {Role} from '../api/auth';
import {ROUTES} from '../constants/routes';

interface NavItem {
  id: string;
  path: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  description?: string;
}

interface NavGroup {
  id: string;
  title: string;
  items: NavItem[];
}

const ROLE_LABEL: Record<Role, string> = {
  CANDIDATE: '求职者',
  INTERVIEWER: '面试官',
  ADMIN: '管理员',
};

const ROLE_BADGE: Record<Role, string> = {
  CANDIDATE: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
  INTERVIEWER: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400',
  ADMIN: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400',
};

const CANDIDATE_GROUPS: NavGroup[] = [
  {
    id: 'growth',
    title: '求职训练',
    items: [
      { id: 'dashboard', path: ROUTES.dashboard, label: '工作台', icon: LayoutDashboard, description: '训练概览' },
      { id: 'jobs', path: ROUTES.jobs, label: '岗位探索', icon: Briefcase, description: '浏览目标岗位' },
      { id: 'training', path: ROUTES.training, label: '训练计划', icon: Target, description: '制定训练计划' },
      { id: 'growth', path: ROUTES.growth, label: '成长档案', icon: TrendingUp, description: '记录成长轨迹' },
    ],
  },
  {
    id: 'interview',
    title: '求职实战',
    items: [
      { id: 'resumes', path: '/history', label: '简历管理', icon: FileStack, description: '简历与 AI 分析' },
      { id: 'upload', path: '/upload', label: '上传简历', icon: Upload, description: '上传并分析简历' },
      { id: 'interview-hub', path: '/interview-hub', label: '面试中心', icon: Sparkles, description: '文字/语音面试' },
      { id: 'interviews', path: '/interviews', label: '面试记录', icon: Users, description: '查看面试历史' },
      { id: 'interview-schedule', path: '/interview-schedule', label: '面试日程', icon: Calendar, description: '管理面试安排' },
    ],
  },
];

const INTERVIEWER_GROUPS: NavGroup[] = [
  {
    id: 'work',
    title: '面试工作',
    items: [
      { id: 'dashboard', path: ROUTES.dashboard, label: '面试工作台', icon: LayoutDashboard, description: '工作概览' },
      { id: 'jobs', path: ROUTES.jobs, label: '岗位管理', icon: Briefcase, description: '管理与发布岗位' },
      { id: 'interviews', path: '/interviews', label: '面试记录', icon: Users, description: '查看面试历史' },
      { id: 'interview-schedule', path: '/interview-schedule', label: '面试日程', icon: Calendar, description: '管理面试安排' },
    ],
  },
  {
    id: 'candidates',
    title: '候选人',
    items: [
      { id: 'resumes', path: '/history', label: '候选人简历', icon: FileStack, description: '浏览简历' },
    ],
  },
];

const ADMIN_GROUPS: NavGroup[] = [
  {
    id: 'platform',
    title: '平台管理',
    items: [
      { id: 'dashboard', path: ROUTES.dashboard, label: '平台总览', icon: LayoutDashboard, description: '平台数据概览' },
      { id: 'users', path: ROUTES.adminUsers, label: '用户与角色', icon: UserCog, description: '管理用户权限' },
      { id: 'audit', path: ROUTES.adminAudit, label: '审计日志', icon: ScrollText, description: '操作审计' },
    ],
  },
  {
    id: 'resources',
    title: '业务资源',
    items: [
      { id: 'jobs', path: ROUTES.jobs, label: '岗位审核', icon: Briefcase, description: '审核岗位' },
      { id: 'resumes', path: '/history', label: '简历库', icon: FileStack, description: '全量简历' },
      { id: 'kb', path: '/knowledgebase', label: '知识库', icon: Database, description: '知识文档' },
      { id: 'settings', path: '/settings', label: 'AI 服务设置', icon: Settings, description: '模型服务配置' },
    ],
  },
];

function getNavGroups(role: Role): NavGroup[] {
  if (role === 'CANDIDATE') return CANDIDATE_GROUPS;
  if (role === 'INTERVIEWER') return INTERVIEWER_GROUPS;
  return ADMIN_GROUPS;
}

export default function Layout() {
  const location = useLocation();
  const currentPath = location.pathname;
  const {theme, toggleTheme} = useTheme();
  const navigate = useNavigate();
  const {user, logout} = useAuth();
  const role: Role = user?.role ?? 'CANDIDATE';
  const [interviewModalPreset, setInterviewModalPreset] = useState<{
    defaultMode: 'text' | 'voice';
    defaultResumeId?: number;
    title: string;
    subtitle: string;
    startButtonText: string;
  } | null>(null);

  const openInterviewModalWithResume = (resumeId: number) => {
    setInterviewModalPreset({
      defaultMode: 'text',
      defaultResumeId: resumeId,
      title: '开始模拟面试',
      subtitle: '配置面试参数，开始练习',
      startButtonText: '开始面试',
    });
  };

  const handleInterviewStart = (config: UnifiedInterviewConfig) => {
    setInterviewModalPreset(null);
    if (config.mode === 'text') {
      navigate('/interview', {
        state: {
          resumeId: config.resumeId,
          interviewConfig: {
            skillId: config.skillId,
            difficulty: config.difficulty,
            questionCount: config.questionCount,
            llmProvider: config.llmProvider,
          },
        },
      });
      return;
    }

    const params = new URLSearchParams({
      skillId: config.skillId,
      difficulty: config.difficulty,
    });
    navigate(`/voice-interview?${params.toString()}`, {
      state: {
        voiceConfig: {
          skillId: config.skillId,
          difficulty: config.difficulty,
          techEnabled: true,
          projectEnabled: true,
          hrEnabled: true,
          plannedDuration: config.plannedDuration,
          resumeId: config.resumeId,
          llmProvider: config.llmProvider,
        },
      },
    });
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const navGroups = getNavGroups(role);

  // 判断当前页面是否匹配导航项
  const isActive = (path: string) => {
    if (path.startsWith('#')) return false;
    if (path === '/history') {
      return currentPath === '/history'
        || currentPath === '/'
        || currentPath.startsWith('/history/')
        || currentPath === '/upload';
    }
    if (path === '/interview-hub') {
      return currentPath === '/interview-hub'
        || currentPath === '/interview'
        || currentPath.startsWith('/interview/')
        || currentPath.startsWith('/voice-interview');
    }
    if (path === '/knowledgebase') {
      return currentPath === '/knowledgebase' || currentPath === '/knowledgebase/upload';
    }
    return currentPath.startsWith(path);
  };

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-slate-50 to-indigo-50 dark:from-slate-900 dark:to-slate-800">
      {/* 左侧边栏 */}
      <aside className="w-64 bg-white dark:bg-slate-900 border-r border-slate-100 dark:border-slate-700 fixed h-screen left-0 top-0 z-50 flex flex-col">
        {/* Logo */}
        <div className="p-6 border-b border-slate-100 dark:border-slate-700 flex items-center justify-between">
          <Link to={ROUTES.dashboard} className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-primary-500 to-primary-600 rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary-500/30">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <span className="text-lg font-bold text-slate-800 dark:text-white tracking-tight block">职跃</span>
              <span className="text-xs text-slate-400 dark:text-slate-500">校园求职训练平台</span>
            </div>
          </Link>
        </div>

        {/* 主题切换按钮 */}
        <div className="px-4 pb-2">
          <button
            onClick={toggleTheme}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
          >
            {theme === 'dark' ? (
              <>
                <Sun className="w-4 h-4" />
                <span className="text-sm font-medium">浅色模式</span>
              </>
            ) : (
              <>
                <Moon className="w-4 h-4" />
                <span className="text-sm font-medium">深色模式</span>
              </>
            )}
          </button>
        </div>

        {/* 导航菜单 */}
        <nav className="flex-1 p-4 overflow-y-auto">
          <div className="space-y-6">
            {navGroups.map((group) => (
              <div key={group.id}>
                <div className="px-3 mb-2">
                  <span className="text-xs font-semibold text-slate-400 dark:text-slate-500 uppercase tracking-wider">
                    {group.title}
                  </span>
                </div>
                <div className="space-y-1">
                  {group.items.map((item) => {
                    const active = isActive(item.path);

                    return (
                      <Link
                        key={item.id}
                        to={item.path}
                        className={`group relative flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200
                          ${active
                            ? 'bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400'
                            : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white'
                          }`}
                      >
                        <div className={`w-9 h-9 rounded-lg flex items-center justify-center transition-colors
                          ${active
                            ? 'bg-primary-100 dark:bg-primary-900/50 text-primary-600 dark:text-primary-400'
                            : 'bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 group-hover:bg-slate-200 dark:group-hover:bg-slate-700 group-hover:text-slate-700 dark:group-hover:text-white'
                          }`}
                        >
                          <item.icon className="w-5 h-5" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <span className={`text-sm block ${active ? 'font-semibold' : 'font-medium'}`}>
                            {item.label}
                          </span>
                          {item.description && (
                            <span className="text-xs text-slate-400 dark:text-slate-500 truncate block">
                              {item.description}
                            </span>
                          )}
                        </div>
                        {active && <ChevronRight className="w-4 h-4 text-primary-400" />}
                      </Link>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </nav>

        {/* 底部信息 */}
        <div className="p-4 border-t border-slate-100 dark:border-slate-700">
          <div className="px-3 py-2 bg-gradient-to-r from-primary-50 to-indigo-50 dark:from-primary-900/30 dark:to-slate-800 rounded-xl">
            <p className="text-xs text-primary-600 dark:text-primary-400 font-medium">职跃 · 求职训练平台</p>
            <p className="text-xs text-slate-400 dark:text-slate-500 mt-0.5">Powered by AI</p>
          </div>
        </div>
      </aside>

      {/* 主内容区 */}
      <main className="flex-1 ml-64 min-h-screen flex flex-col">
        {/* 顶部用户栏 */}
        <header className="sticky top-0 z-40 flex items-center justify-end gap-4 px-10 py-3.5 bg-white/70 dark:bg-slate-900/70 backdrop-blur border-b border-slate-100 dark:border-slate-700">
          {user && (
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-full bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center text-white text-sm font-bold">
                  {(user.username || 'U').slice(0, 1).toUpperCase()}
                </div>
                <div>
                  <p className="text-sm font-semibold text-slate-800 dark:text-white leading-tight">{user.username}</p>
                  <span className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-semibold ${ROLE_BADGE[user.role]}`}>
                    {ROLE_LABEL[user.role]}
                  </span>
                </div>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-red-500 dark:hover:text-red-400 transition-colors"
                title="退出登录"
              >
                <LogOut className="w-4 h-4" />
                退出
              </button>
            </div>
          )}
        </header>

        <div className="flex-1 p-10">
          <motion.div
            key={currentPath}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.3 }}
          >
            <Outlet context={{ openInterviewModalWithResume }} />
          </motion.div>
        </div>
      </main>

      {/* 统一面试弹窗 */}
      <UnifiedInterviewModal
        isOpen={interviewModalPreset !== null}
        onClose={() => setInterviewModalPreset(null)}
        onStart={handleInterviewStart}
        defaultMode={interviewModalPreset?.defaultMode || 'text'}
        defaultResumeId={interviewModalPreset?.defaultResumeId}
        hideModeSwitch={interviewModalPreset?.defaultResumeId == null}
        title={interviewModalPreset?.title || '开始模拟面试'}
        subtitle={interviewModalPreset?.subtitle || '选择面试模式和主题，快速开始'}
        startButtonText={interviewModalPreset?.startButtonText || '开始面试'}
      />
    </div>
  );
}