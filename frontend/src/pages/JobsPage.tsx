import { useEffect, useState, type FormEvent } from 'react';
import {
  Archive, Briefcase, Loader2, Plus, Send, Trash2, X,
} from 'lucide-react';
import { jobsApi, type CreateJobRequest, type PositionView } from '../api/business';
import { useAuth } from '../context/AuthContext';
import { formatDateTime } from '../utils/date';
import { getErrorMessage } from '../api/request';

interface ReqRow {
  dimension: string;
  weight: string;
  description: string;
}

const STATUS_BADGE: Record<string, string> = {
  DRAFT: 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300',
  PUBLISHED: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400',
  ARCHIVED: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400',
};

const STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档',
};

const inputClass = `w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700
  bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-white
  placeholder:text-slate-400 focus:outline-none focus:ring-2
  focus:ring-primary-500/50 focus:border-primary-400 transition-shadow`;

export default function JobsPage() {
  const { user } = useAuth();
  const role = user?.role ?? 'CANDIDATE';
  const canCreate = role === 'INTERVIEWER' || role === 'ADMIN';
  const canManage = role === 'ADMIN';

  const [jobs, setJobs] = useState<PositionView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionId, setActionId] = useState<number | null>(null);
  const [actionError, setActionError] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState('');
  const [company, setCompany] = useState('');
  const [description, setDescription] = useState('');
  const [requirements, setRequirements] = useState<ReqRow[]>([{ dimension: '', weight: '', description: '' }]);
  const [submitting, setSubmitting] = useState(false);

  const loadJobs = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await jobsApi.list();
      setJobs(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadJobs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const updateReq = (index: number, patch: Partial<ReqRow>) => {
    setRequirements(prev => prev.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  };

  const addReq = () => setRequirements(prev => [...prev, { dimension: '', weight: '', description: '' }]);

  const removeReq = (index: number) => setRequirements(prev => prev.filter((_, i) => i !== index));

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !company.trim() || !description.trim()) {
      setActionError('请填写岗位名称、公司和描述');
      return;
    }
    const reqs = requirements
      .filter(r => r.dimension.trim())
      .map(r => ({ dimension: r.dimension.trim(), weight: parseFloat(r.weight) || 0, description: r.description.trim() }));

    const payload: CreateJobRequest = {
      title: title.trim(),
      company: company.trim(),
      description: description.trim(),
      requirements: reqs,
    };
    setSubmitting(true);
    setActionError('');
    try {
      await jobsApi.create(payload);
      setShowForm(false);
      await loadJobs();
    } catch (err) {
      setActionError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const handlePublish = async (id: number) => {
    setActionId(id);
    setActionError('');
    try {
      await jobsApi.publish(id);
      await loadJobs();
    } catch (err) {
      setActionError(getErrorMessage(err));
    } finally {
      setActionId(null);
    }
  };

  const handleArchive = async (id: number) => {
    if (!window.confirm('确定要归档该岗位吗？归档后候选人将不再看到该岗位。')) return;
    setActionId(id);
    setActionError('');
    try {
      await jobsApi.archive(id);
      await loadJobs();
    } catch (err) {
      setActionError(getErrorMessage(err));
    } finally {
      setActionId(null);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('确定要删除该岗位吗？删除后无法恢复。')) return;
    setActionId(id);
    setActionError('');
    try {
      await jobsApi.remove(id);
      await loadJobs();
    } catch (err) {
      setActionError(getErrorMessage(err));
    } finally {
      setActionId(null);
    }
  };

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-r from-primary-500 to-primary-600 shadow-lg shadow-primary-500/25">
            <Briefcase className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-800 dark:text-white">岗位</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">{canCreate ? '管理与发布岗位' : '探索目标岗位'}</p>
          </div>
        </div>
        {canCreate && (
          <button
            onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl font-medium text-sm bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-lg shadow-primary-500/25 hover:from-primary-600 hover:to-primary-700 transition-all"
          >
            {showForm ? <X className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
            {showForm ? '取消' : '新增岗位'}
          </button>
        )}
      </div>

      {actionError && <p className="mb-4 text-sm text-red-500">{actionError}</p>}

      {showForm && canCreate && (
        <form onSubmit={handleCreate} className="mb-8 bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-6 space-y-4">
          <h2 className="text-lg font-bold text-slate-800 dark:text-white">新增岗位</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">岗位名称 <span className="text-red-500">*</span></label>
              <input className={inputClass} value={title} onChange={e => setTitle(e.target.value)} placeholder="如：Java 后端工程师" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">公司 <span className="text-red-500">*</span></label>
              <input className={inputClass} value={company} onChange={e => setCompany(e.target.value)} placeholder="如：示例科技" />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">职位描述 <span className="text-red-500">*</span></label>
            <textarea className={`${inputClass} resize-none`} rows={3} value={description} onChange={e => setDescription(e.target.value)} placeholder="岗位职责与要求描述" />
          </div>
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium text-slate-700 dark:text-slate-300">能力要求（维度 + 权重）</label>
              <button type="button" onClick={addReq} className="flex items-center gap-1.5 text-sm text-primary-500 hover:text-primary-600 font-medium">
                <Plus className="w-4 h-4" /> 添加维度
              </button>
            </div>
            <div className="space-y-2">
              {requirements.map((row, index) => (
                <div key={index} className="grid grid-cols-[1fr_90px_1.5fr_auto] gap-2 items-center">
                  <input className={inputClass} value={row.dimension} onChange={e => updateReq(index, { dimension: e.target.value })} placeholder="维度，如：沟通能力" />
                  <input className={inputClass} type="number" min="0" step="0.01" value={row.weight} onChange={e => updateReq(index, { weight: e.target.value })} placeholder="权重" />
                  <input className={inputClass} value={row.description} onChange={e => updateReq(index, { description: e.target.value })} placeholder="维度说明" />
                  <button type="button" onClick={() => removeReq(index)} className="p-2 rounded-lg text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-700 hover:text-red-500 transition-colors" title="移除">
                    <X className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          </div>
          <div className="flex justify-end">
            <button type="submit" disabled={submitting} className="flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-sm text-white bg-gradient-to-r from-primary-500 to-primary-600 hover:from-primary-600 hover:to-primary-700 shadow-lg shadow-primary-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              {submitting ? '创建中...' : '创建岗位'}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20"><Loader2 className="w-8 h-8 text-primary-500 animate-spin" /></div>
      ) : error ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <p className="text-red-500 text-sm mb-3">{error}</p>
          <button onClick={loadJobs} className="px-4 py-2 text-sm font-medium rounded-lg bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-600">重试</button>
        </div>
      ) : jobs.length === 0 ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <Briefcase className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
          <p className="text-slate-500 dark:text-slate-400 text-sm">暂无岗位</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {jobs.map(job => (
            <div key={job.id} className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 p-5 flex flex-col">
              <div className="flex items-start justify-between gap-3 mb-2">
                <div className="min-w-0">
                  <h3 className="text-base font-bold text-slate-800 dark:text-white truncate">{job.title}</h3>
                  <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">{job.company}</p>
                </div>
                <span className={`px-2.5 py-1 rounded-full text-xs font-semibold whitespace-nowrap ${STATUS_BADGE[job.status] ?? ''}`}>
                  {STATUS_LABEL[job.status] ?? job.status}
                </span>
              </div>
              <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed mb-3 line-clamp-3">{job.description}</p>
              {job.requirements.length > 0 && (
                <div className="flex flex-wrap gap-2 mb-3">
                  {job.requirements.map((req, i) => (
                    <span key={i} className="px-2.5 py-1 rounded-full text-xs font-medium bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300">
                      {req.dimension}{req.weight != null && req.weight !== 0 ? ` · ${req.weight}` : ''}
                    </span>
                  ))}
                </div>
              )}
              <div className="mt-auto pt-3 border-t border-slate-100 dark:border-slate-700 flex items-center justify-between">
                <span className="text-xs text-slate-400 dark:text-slate-500">{formatDateTime(job.createdAt)}</span>
                <div className="flex items-center gap-2">
                  {canCreate && job.status === 'DRAFT' && (
                    <button onClick={() => handlePublish(job.id)} disabled={actionId === job.id} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-emerald-600 hover:bg-emerald-50 dark:text-emerald-400 dark:hover:bg-emerald-900/20 transition-colors disabled:opacity-50">
                      {actionId === job.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                      发布
                    </button>
                  )}
                  {canManage && job.status === 'PUBLISHED' && (
                    <button onClick={() => handleArchive(job.id)} disabled={actionId === job.id} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-amber-600 hover:bg-amber-50 dark:text-amber-400 dark:hover:bg-amber-900/20 transition-colors disabled:opacity-50">
                      <Archive className="w-3.5 h-3.5" />
                      归档
                    </button>
                  )}
                  {canManage && (
                    <button onClick={() => handleDelete(job.id)} disabled={actionId === job.id} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:bg-red-50 hover:text-red-500 dark:text-slate-500 dark:hover:bg-red-900/20 dark:hover:text-red-300 transition-colors disabled:opacity-50">
                      <Trash2 className="w-3.5 h-3.5" />
                      删除
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}