import { useEffect, useState, type FormEvent } from 'react';
import { CheckCircle2, Loader2, Plus, TrendingUp } from 'lucide-react';
import {
  CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import {
  growthApi, type GrowthRecordView, type GrowthTrendPoint,
} from '../api/business';
import { formatDateTime } from '../utils/date';
import { getErrorMessage } from '../api/request';

const STATUS_BADGE: Record<string, string> = {
  OPEN: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400',
  DONE: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400',
};

const STATUS_LABEL: Record<string, string> = {
  OPEN: '待完成',
  DONE: '已完成',
};

const inputClass = `w-full px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700
  bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-white
  placeholder:text-slate-400 focus:outline-none focus:ring-2
  focus:ring-primary-500/50 focus:border-primary-400 transition-shadow`;

export default function GrowthPage() {
  const [records, setRecords] = useState<GrowthRecordView[]>([]);
  const [trend, setTrend] = useState<GrowthTrendPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionId, setActionId] = useState<number | null>(null);
  const [actionError, setActionError] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [dimension, setDimension] = useState('');
  const [issue, setIssue] = useState('');
  const [action, setAction] = useState('');
  const [sourceSessionId, setSourceSessionId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [recordData, trendData] = await Promise.all([
        growthApi.list().catch(() => []),
        growthApi.trend().catch(() => []),
      ]);
      setRecords(recordData);
      setTrend(trendData);
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
    if (!dimension.trim() || !issue.trim() || !action.trim()) {
      setActionError('请填写维度、问题和行动');
      return;
    }
    setSubmitting(true);
    setActionError('');
    try {
      await growthApi.create({
        dimension: dimension.trim(),
        issue: issue.trim(),
        action: action.trim(),
        sourceSessionId: sourceSessionId.trim() || null,
      });
      setDimension('');
      setIssue('');
      setAction('');
      setSourceSessionId('');
      setShowForm(false);
      await loadData();
    } catch (err) {
      setActionError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDone = async (id: number) => {
    setActionId(id);
    setActionError('');
    try {
      await growthApi.done(id);
      await loadData();
    } catch (err) {
      setActionError(getErrorMessage(err));
    } finally {
      setActionId(null);
    }
  };

  const trendData = trend.map((t, i) => ({ index: i + 1, score: t.overallScore }));

  return (
    <div className="max-w-3xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-r from-primary-500 to-primary-600 shadow-lg shadow-primary-500/25">
            <TrendingUp className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-800 dark:text-white">成长档案</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">记录问题、行动与成长轨迹</p>
          </div>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl font-medium text-sm bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-lg shadow-primary-500/25 hover:from-primary-600 hover:to-primary-700 transition-all"
        >
          <Plus className="w-4 h-4" />
          {showForm ? '取消' : '新增记录'}
        </button>
      </div>

      {actionError && <p className="mb-4 text-sm text-red-500">{actionError}</p>}

      {showForm && (
        <form onSubmit={handleCreate} className="mb-6 bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6 space-y-4">
          <h2 className="text-lg font-bold text-slate-800 dark:text-white">新增成长记录</h2>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">维度 <span className="text-red-500">*</span></label>
            <input className={inputClass} value={dimension} onChange={e => setDimension(e.target.value)} placeholder="如：沟通表达、算法能力" />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">问题 <span className="text-red-500">*</span></label>
            <textarea className={`${inputClass} resize-none`} rows={2} value={issue} onChange={e => setIssue(e.target.value)} placeholder="遇到的问题" />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">行动 <span className="text-red-500">*</span></label>
            <input className={inputClass} value={action} onChange={e => setAction(e.target.value)} placeholder="采取的改进行动" />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">关联面试 <span className="text-slate-400 font-normal">(可选，sessionId)</span></label>
            <input className={inputClass} value={sourceSessionId} onChange={e => setSourceSessionId(e.target.value)} placeholder="面试会话 ID" />
          </div>
          <div className="flex justify-end">
            <button type="submit" disabled={submitting} className="flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-sm text-white bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 shadow-lg shadow-primary-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              {submitting ? '创建中...' : '创建记录'}
            </button>
          </div>
        </form>
      )}

      <div className="mb-6 bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6">
        <h2 className="text-base font-bold text-slate-800 dark:text-white mb-4">分数趋势</h2>
        {trend.length === 0 ? (
          <p className="text-sm text-slate-400 dark:text-slate-500 text-center py-8">暂无趋势数据，完成面试后会自动生成</p>
        ) : (
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={trendData} margin={{ top: 8, right: 16, left: -16, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#94a3b8" opacity={0.2} />
              <XAxis dataKey="index" tick={{ fontSize: 12 }} stroke="#94a3b8" />
              <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} stroke="#94a3b8" />
              <Tooltip formatter={(value) => [`${value} 分`, '综合得分']} labelFormatter={(label) => `第 ${label} 次`} />
              <Line type="monotone" dataKey="score" stroke="#6366f1" strokeWidth={2} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20"><Loader2 className="w-8 h-8 text-primary-500 animate-spin" /></div>
      ) : error ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <p className="text-red-500 text-sm mb-3">{error}</p>
          <button onClick={loadData} className="px-4 py-2 text-sm font-medium rounded-lg bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-600">重试</button>
        </div>
      ) : records.length === 0 ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <TrendingUp className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
          <p className="text-slate-500 dark:text-slate-400 text-sm">暂无成长记录，记录你的第一个进步吧</p>
        </div>
      ) : (
        <div className="space-y-3">
          {records.map(record => (
            <div key={record.id} className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-5">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300">{record.dimension}</span>
                    <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${STATUS_BADGE[record.status] ?? ''}`}>
                      {STATUS_LABEL[record.status] ?? record.status}
                    </span>
                  </div>
                  <h3 className="text-sm font-semibold text-slate-800 dark:text-white mt-3">问题</h3>
                  <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed">{record.issue}</p>
                  <h3 className="text-sm font-semibold text-slate-800 dark:text-white mt-3">行动</h3>
                  <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed">{record.action}</p>
                </div>
                {record.status === 'OPEN' && (
                  <button onClick={() => handleDone(record.id)} disabled={actionId === record.id} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-emerald-600 hover:bg-emerald-50 dark:text-emerald-400 dark:hover:bg-emerald-900/20 transition-colors disabled:opacity-50 whitespace-nowrap">
                    {actionId === record.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
                    完成
                  </button>
                )}
              </div>
              <p className="text-xs text-slate-400 dark:text-slate-500 mt-3">创建于 {formatDateTime(record.createdAt)}{record.doneAt ? ` · 完成于 ${formatDateTime(record.doneAt)}` : ''}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}