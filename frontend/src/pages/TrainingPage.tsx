import { useEffect, useState, type FormEvent } from 'react';
import { CheckCircle2, Loader2, Plus, Target } from 'lucide-react';
import {
  jobsApi, trainingApi, type PositionView, type TrainingPlanView,
} from '../api/business';
import { formatDateTime } from '../utils/date';
import { getErrorMessage } from '../api/request';

const STATUS_BADGE: Record<string, string> = {
  ACTIVE: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
  COMPLETED: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400',
};

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: '进行中',
  COMPLETED: '已完成',
};

const inputClass = `w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700
  bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-white
  placeholder:text-slate-400 focus:outline-none focus:ring-2
  focus:ring-primary-500/50 focus:border-primary-400 transition-shadow`;

export default function TrainingPage() {
  const [plans, setPlans] = useState<TrainingPlanView[]>([]);
  const [publishedJobs, setPublishedJobs] = useState<PositionView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionId, setActionId] = useState<number | null>(null);
  const [actionError, setActionError] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState('');
  const [goal, setGoal] = useState('');
  const [positionId, setPositionId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [planData, jobData] = await Promise.all([
        trainingApi.list().catch(() => []),
        jobsApi.list().catch(() => []),
      ]);
      setPlans(planData);
      setPublishedJobs(jobData.filter(j => j.status === 'PUBLISHED'));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !goal.trim()) {
      setActionError('请填写计划标题和目标');
      return;
    }
    setSubmitting(true);
    setActionError('');
    try {
      await trainingApi.create({
        title: title.trim(),
        goal: goal.trim(),
        positionId: positionId ? Number(positionId) : null,
      });
      setTitle('');
      setGoal('');
      setPositionId('');
      setShowForm(false);
      await loadData();
    } catch (err) {
      setActionError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const handleComplete = async (id: number) => {
    setActionId(id);
    setActionError('');
    try {
      await trainingApi.complete(id);
      await loadData();
    } catch (err) {
      setActionError(getErrorMessage(err));
    } finally {
      setActionId(null);
    }
  };

  const jobTitle = (plan: TrainingPlanView) => {
    const job = publishedJobs.find(j => j.id === plan.positionId);
    return job ? `${job.title}（${job.company}）` : undefined;
  };

  return (
    <div className="max-w-3xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-r from-primary-500 to-primary-600 shadow-lg shadow-primary-500/25">
            <Target className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-800 dark:text-white">训练计划</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">围绕目标岗位制定并推进训练计划</p>
          </div>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl font-medium text-sm bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-lg shadow-primary-500/25 hover:from-primary-600 hover:to-primary-700 transition-all"
        >
          <Plus className="w-4 h-4" />
          {showForm ? '取消' : '新建计划'}
        </button>
      </div>

      {actionError && <p className="mb-4 text-sm text-red-500">{actionError}</p>}

      {showForm && (
        <form onSubmit={handleCreate} className="mb-6 bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6 space-y-4">
          <h2 className="text-lg font-bold text-slate-800 dark:text-white">新建训练计划</h2>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">计划标题 <span className="text-red-500">*</span></label>
            <input className={inputClass} value={title} onChange={e => setTitle(e.target.value)} placeholder="如：冲刺后端开发岗" />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">目标 <span className="text-red-500">*</span></label>
            <textarea className={`${inputClass} resize-none`} rows={3} value={goal} onChange={e => setGoal(e.target.value)} placeholder="描述训练目标" />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">关联岗位 <span className="text-slate-400 font-normal">(可选)</span></label>
            <select className={inputClass} value={positionId} onChange={e => setPositionId(e.target.value)}>
              <option value="">不关联岗位</option>
              {publishedJobs.map(j => (
                <option key={j.id} value={j.id}>{j.title}（{j.company}）</option>
              ))}
            </select>
          </div>
          <div className="flex justify-end">
            <button type="submit" disabled={submitting} className="flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-sm text-white bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 shadow-lg shadow-primary-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              {submitting ? '创建中...' : '创建计划'}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20"><Loader2 className="w-8 h-8 text-primary-500 animate-spin" /></div>
      ) : error ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <p className="text-red-500 text-sm mb-3">{error}</p>
          <button onClick={loadData} className="px-4 py-2 text-sm font-medium rounded-lg bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-600">重试</button>
        </div>
      ) : plans.length === 0 ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <Target className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
          <p className="text-slate-500 dark:text-slate-400 text-sm">暂无训练计划，点击右上角新建一个吧</p>
        </div>
      ) : (
        <div className="space-y-3">
          {plans.map(plan => {
            const relatedJob = jobTitle(plan);
            return (
              <div key={plan.id} className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="text-base font-bold text-slate-800 dark:text-white">{plan.title}</h3>
                      <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${STATUS_BADGE[plan.status] ?? ''}`}>
                        {STATUS_LABEL[plan.status] ?? plan.status}
                      </span>
                    </div>
                    {relatedJob && <p className="text-xs text-primary-500 dark:text-primary-400 mt-1">{relatedJob}</p>}
                  </div>
                  {plan.status === 'ACTIVE' && (
                    <button onClick={() => handleComplete(plan.id)} disabled={actionId === plan.id} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-emerald-600 hover:bg-emerald-50 dark:text-emerald-400 dark:hover:bg-emerald-900/20 transition-colors disabled:opacity-50 whitespace-nowrap">
                      {actionId === plan.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
                      完成
                    </button>
                  )}
                </div>
                <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed mt-3">{plan.goal}</p>
                <p className="text-xs text-slate-400 dark:text-slate-500 mt-3">创建于 {formatDateTime(plan.createdAt)}{plan.completedAt ? ` · 完成于 ${formatDateTime(plan.completedAt)}` : ''}</p>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}