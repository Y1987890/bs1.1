import { useEffect, useMemo, useState } from 'react';
import { Loader2, ScrollText } from 'lucide-react';
import { adminApi, type AuditLogView } from '../api/business';
import { formatDateTime } from '../utils/date';
import { getErrorMessage } from '../api/request';

export default function AdminAuditPage() {
  const [logs, setLogs] = useState<AuditLogView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadLogs = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await adminApi.listAuditLogs();
      setLogs(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const sortedLogs = useMemo(
    () => [...logs].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
    [logs],
  );

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-3 rounded-xl bg-gradient-to-r from-primary-500 to-primary-600 shadow-lg shadow-primary-500/25">
          <ScrollText className="w-6 h-6 text-white" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-white">审计日志</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">平台操作审计记录（时间倒序）</p>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20"><Loader2 className="w-8 h-8 text-primary-500 animate-spin" /></div>
      ) : error ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <p className="text-red-500 text-sm mb-3">{error}</p>
          <button onClick={loadLogs} className="px-4 py-2 text-sm font-medium rounded-lg bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-600">重试</button>
        </div>
      ) : sortedLogs.length === 0 ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <ScrollText className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
          <p className="text-slate-500 dark:text-slate-400 text-sm">暂无审计记录</p>
        </div>
      ) : (
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 dark:border-slate-700 text-left text-xs text-slate-400 dark:text-slate-500 uppercase">
                  <th className="px-5 py-3 font-medium">时间</th>
                  <th className="px-5 py-3 font-medium">操作人</th>
                  <th className="px-5 py-3 font-medium">操作</th>
                  <th className="px-5 py-3 font-medium">目标类型</th>
                  <th className="px-5 py-3 font-medium">目标 ID</th>
                  <th className="px-5 py-3 font-medium">详情</th>
                </tr>
              </thead>
              <tbody>
                {sortedLogs.map(log => (
                  <tr key={log.id} className="border-b border-slate-50 dark:border-slate-700/50 last:border-0 hover:bg-slate-50/50 dark:hover:bg-slate-700/30 transition-colors">
                    <td className="px-5 py-3 text-slate-500 dark:text-slate-400 whitespace-nowrap">{formatDateTime(log.createdAt)}</td>
                    <td className="px-5 py-3 font-medium text-slate-700 dark:text-slate-200">{log.actorUsername}</td>
                    <td className="px-5 py-3">
                      <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300">{log.action}</span>
                    </td>
                    <td className="px-5 py-3 text-slate-700 dark:text-slate-200">{log.targetType || '-'}</td>
                    <td className="px-5 py-3 text-slate-500 dark:text-slate-400">{log.targetId ?? '-'}</td>
                    <td className="px-5 py-3 text-slate-500 dark:text-slate-400 max-w-xs truncate">{log.detail || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}