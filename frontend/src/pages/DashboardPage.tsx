import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';
import {
  ArrowRight, Briefcase, FileStack, LayoutDashboard, Loader2, Server, ShieldCheck,
  Sparkles, Target, TrendingUp, Users,
} from 'lucide-react';
import {
  CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { useAuth } from '../context/AuthContext';
import { historyApi } from '../api/history';
import { interviewApi } from '../api/interview';
import { llmProviderApi } from '../api/llmProvider';
import {
  adminApi, growthApi, jobsApi, trainingApi,
  type GrowthTrendPoint, type PositionView, type AuditLogView,
} from '../api/business';
import { formatDateTime } from '../utils/date';

function StatCard({ icon: Icon, label, value, hint, accent }: {
  icon: LucideIcon;
  label: string;
  value: ReactNode;
  hint?: string;
  accent: string;
}) {
  return (
    <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-5">
      <div className="flex items-center gap-3 mb-3">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${accent}`}>
          <Icon className="w-5 h-5" />
        </div>
        <span className="text-sm text-slate-500 dark:text-slate-400">{label}</span>
      </div>
      <div className="text-3xl font-bold text-slate-800 dark:text-white tabular-nums">{value}</div>
      {hint && <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">{hint}</p>}
    </div>
  );
}

function LoadingBlock() {
  return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
    </div>
  );
}

function TrendChart({ data }: { data: { index: number; score: number }[] }) {
  if (data.length === 0) {
    return <p className="text-sm text-slate-400 dark:text-slate-500 text-center py-10">暂无分数趋势数据</p>;
  }
  return (
    <ResponsiveContainer width="100%" height={240}>
      <LineChart data={data} margin={{ top: 8, right: 16, left: -16, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#94a3b8" opacity={0.2} />
        <XAxis dataKey="index" tick={{ fontSize: 12 }} stroke="#94a3b8" />
        <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} stroke="#94a3b8" />
        <Tooltip
          formatter={(value) => [`${value} 分`, '综合得分']}
          labelFormatter={(label) => `第 ${label} 次`}
        />
        <Line type="monotone" dataKey="score" stroke="#6366f1" strokeWidth={2} dot={{ r: 3 }} />
      </LineChart>
    </ResponsiveContainer>
  );
}

function CandidateDashboard() {
  const [resumeCount, setResumeCount] = useState(0);
  const [activePlanCount, setActivePlanCount] = useState(0);
  const [openRecordCount, setOpenRecordCount] = useState(0);
  const [trend, setTrend] = useState<GrowthTrendPoint[]>([]);
  const [recommendedJobs, setRecommendedJobs] = useState<PositionView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [resumes, plans, records, trendData, jobs] = await Promise.all([
          historyApi.getResumes().catch(() => []),
          trainingApi.list().catch(() => []),
          growthApi.list().catch(() => []),
          growthApi.trend().catch(() => []),
          jobsApi.list().catch(() => []),
        ]);
        setResumeCount(resumes.length);
        setActivePlanCount(plans.filter((p) => p.status === 'ACTIVE').length);
        setOpenRecordCount(records.filter((r) => r.status === 'OPEN').length);
        setTrend(trendData);
        setRecommendedJobs(jobs.filter((j) => j.status === 'PUBLISHED').slice(0, 3));
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载失败');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) return <LoadingBlock />;
  if (error) {
    return <p className="text-sm text-red-500 text-center py-10">{error}</p>;
  }

  const trendData = trend.map((t, i) => ({ index: i + 1, score: t.overallScore }));

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard icon={FileStack} label="简历数" value={resumeCount} accent="bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400" />
        <StatCard icon={Target} label="进行中计划" value={activePlanCount} accent="bg-violet-100 dark:bg-violet-900/30 text-violet-600 dark:text-violet-400" />
        <StatCard icon={TrendingUp} label="未完成成长记录" value={openRecordCount} accent="bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400" />
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6">
        <h2 className="text-base font-bold text-slate-800 dark:text-white mb-4">最近分数趋势</h2>
        <TrendChart data={trendData} />
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-bold text-slate-800 dark:text-white">推荐岗位</h2>
          <Link to="/jobs" className="text-sm text-primary-500 hover:text-primary-600 font-medium">查看全部</Link>
        </div>
        {recommendedJobs.length === 0 ? (
          <p className="text-sm text-slate-400 dark:text-slate-500 text-center py-8">暂无已发布的推荐岗位</p>
        ) : (
          <div className="space-y-3">
            {recommendedJobs.map((job) => (
              <Link key={job.id} to="/jobs" className="flex items-center justify-between p-4 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors">
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-slate-800 dark:text-white truncate">{job.title}</p>
                  <p className="text-xs text-slate-400 dark:text-slate-500 mt-0.5">{job.company}</p>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-300 dark:text-slate-600 flex-shrink-0" />
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function InterviewerDashboard() {
  const [jobCount, setJobCount] = useState(0);
  const [resumeCount, setResumeCount] = useState(0);
  const [interviewCount, setInterviewCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [jobs, resumes, sessions] = await Promise.all([
          jobsApi.list().catch(() => []),
          historyApi.getResumes().catch(() => []),
          interviewApi.listSessions().catch(() => []),
        ]);
        setJobCount(jobs.filter((j) => j.status === 'DRAFT' || j.status === 'PUBLISHED').length);
        setResumeCount(resumes.length);
        setInterviewCount(sessions.length);
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载失败');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) return <LoadingBlock />;
  if (error) {
    return <p className="text-sm text-red-500 text-center py-10">{error}</p>;
  }

  const quickLinks = [
    { to: '/jobs', label: '岗位管理', icon: Briefcase },
    { to: '/history', label: '候选人简历', icon: FileStack },
    { to: '/interviews', label: '面试记录', icon: Users },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard icon={Briefcase} label="负责岗位数" value={jobCount} hint="草稿与已发布岗位" accent="bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400" />
        <StatCard icon={FileStack} label="平台简历数" value={resumeCount} accent="bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400" />
        <StatCard icon={Users} label="面试记录数" value={interviewCount} hint="候选人面试作答记录" accent="bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400" />
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6">
        <h2 className="text-base font-bold text-slate-800 dark:text-white mb-4">快捷入口</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {quickLinks.map((link) => {
            const Icon = link.icon;
            return (
              <Link key={link.to} to={link.to} className="flex items-center gap-3 p-4 rounded-xl border border-slate-200 dark:border-slate-700 hover:border-primary-300 dark:hover:border-primary-600 hover:bg-primary-50/50 dark:hover:bg-primary-900/20 transition-colors">
                <Icon className="w-5 h-5 text-primary-500" />
                <span className="text-sm font-medium text-slate-700 dark:text-slate-200">{link.label}</span>
              </Link>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function AdminDashboard() {
  const [userCount, setUserCount] = useState(0);
  const [jobCount, setJobCount] = useState(0);
  const [defaultProvider, setDefaultProvider] = useState('-');
  const [auditLogs, setAuditLogs] = useState<AuditLogView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [users, jobs, provider, logs] = await Promise.all([
          adminApi.listUsers().catch(() => []),
          jobsApi.list().catch(() => []),
          llmProviderApi.getDefaultProvider().catch(() => null),
          adminApi.listAuditLogs().catch(() => []),
        ]);
        setUserCount(users.length);
        setJobCount(jobs.length);
        setDefaultProvider(provider?.defaultProvider ?? '-');
        setAuditLogs(logs.slice(0, 5));
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载失败');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) return <LoadingBlock />;
  if (error) {
    return <p className="text-sm text-red-500 text-center py-10">{error}</p>;
  }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard icon={Users} label="用户数" value={userCount} accent="bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400" />
        <StatCard icon={Briefcase} label="岗位总数" value={jobCount} accent="bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400" />
        <StatCard icon={Server} label="默认 Provider" value={defaultProvider} accent="bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400" />
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-bold text-slate-800 dark:text-white">最近审计记录</h2>
          <Link to="/admin/audit" className="text-sm text-primary-500 hover:text-primary-600 font-medium">查看全部</Link>
        </div>
        {auditLogs.length === 0 ? (
          <p className="text-sm text-slate-400 dark:text-slate-500 text-center py-8">暂无审计记录</p>
        ) : (
          <div className="space-y-2">
            {auditLogs.map((log) => (
              <div key={log.id} className="flex items-center justify-between gap-3 p-3 rounded-lg bg-slate-50 dark:bg-slate-900/40">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{log.action}</p>
                  <p className="text-xs text-slate-400 dark:text-slate-500 truncate">{log.actorUsername} · {log.detail || log.targetType}</p>
                </div>
                <span className="text-xs text-slate-400 dark:text-slate-500 flex-shrink-0">{formatDateTime(log.createdAt)}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  if (!user) return null;

  const header = (() => {
    if (user.role === 'CANDIDATE') return { title: '工作台', subtitle: '求职训练概览', icon: LayoutDashboard };
    if (user.role === 'INTERVIEWER') return { title: '面试工作台', subtitle: '岗位与候选人概览', icon: Briefcase };
    return { title: '平台总览', subtitle: '平台运营数据', icon: ShieldCheck };
  })();

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-3 rounded-xl bg-gradient-to-r from-primary-500 to-primary-600 shadow-lg shadow-primary-500/25">
          <header.icon className="w-6 h-6 text-white" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-white flex items-center gap-2">
            {header.title}
            <Sparkles className="w-4 h-4 text-primary-500" />
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">{header.subtitle}</p>
        </div>
      </div>

      {user.role === 'CANDIDATE' && <CandidateDashboard />}
      {user.role === 'INTERVIEWER' && <InterviewerDashboard />}
      {user.role === 'ADMIN' && <AdminDashboard />}
    </div>
  );
}