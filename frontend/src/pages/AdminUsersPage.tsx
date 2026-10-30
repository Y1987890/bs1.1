import { useEffect, useState } from 'react';
import { Loader2, UserCog } from 'lucide-react';
import { adminApi, type AdminUserView } from '../api/business';
import { formatDateTime } from '../utils/date';
import { getErrorMessage } from '../api/request';

const ROLE_OPTIONS = [
  { value: 'CANDIDATE', label: '求职者' },
  { value: 'INTERVIEWER', label: '面试官' },
  { value: 'ADMIN', label: '管理员' },
];

const ROLE_BADGE: Record<string, string> = {
  CANDIDATE: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
  INTERVIEWER: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400',
  ADMIN: 'bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400',
};

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUserView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [savingId, setSavingId] = useState<number | null>(null);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const loadUsers = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await adminApi.listUsers();
      setUsers(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleRoleChange = async (id: number, role: string) => {
    setSavingId(id);
    setMessage(null);
    try {
      await adminApi.updateUserRole(id, role);
      setMessage({ type: 'success', text: '角色已更新' });
      await loadUsers();
    } catch (err) {
      setMessage({ type: 'error', text: getErrorMessage(err) });
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center gap-3 mb-2">
        <div className="p-3 rounded-xl bg-gradient-to-r from-primary-500 to-primary-600 shadow-lg shadow-primary-500/25">
          <UserCog className="w-6 h-6 text-white" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-800 dark:text-white">用户与角色</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">管理平台用户与角色权限</p>
        </div>
      </div>
      <p className="text-xs text-slate-400 dark:text-slate-500 mb-6">注册入口自动为用户分配「求职者」角色。</p>

      {message && (
        <p className={`mb-4 text-sm ${message.type === 'success' ? 'text-emerald-600' : 'text-red-500'}`}>{message.text}</p>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20"><Loader2 className="w-8 h-8 text-primary-500 animate-spin" /></div>
      ) : error ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <p className="text-red-500 text-sm mb-3">{error}</p>
          <button onClick={loadUsers} className="px-4 py-2 text-sm font-medium rounded-lg bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-600">重试</button>
        </div>
      ) : users.length === 0 ? (
        <div className="text-center py-16 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
          <p className="text-slate-500 dark:text-slate-400 text-sm">暂无用户</p>
        </div>
      ) : (
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 dark:border-slate-700 text-left text-xs text-slate-400 dark:text-slate-500 uppercase">
                  <th className="px-5 py-3 font-medium">ID</th>
                  <th className="px-5 py-3 font-medium">用户名</th>
                  <th className="px-5 py-3 font-medium">昵称</th>
                  <th className="px-5 py-3 font-medium">角色</th>
                  <th className="px-5 py-3 font-medium">状态</th>
                  <th className="px-5 py-3 font-medium">创建时间</th>
                </tr>
              </thead>
              <tbody>
                {users.map(user => (
                  <tr key={user.id} className="border-b border-slate-50 dark:border-slate-700/50 last:border-0 hover:bg-slate-50/50 dark:hover:bg-slate-700/30 transition-colors">
                    <td className="px-5 py-3 text-slate-500 dark:text-slate-400">{user.id}</td>
                    <td className="px-5 py-3 font-medium text-slate-700 dark:text-slate-200">{user.username}</td>
                    <td className="px-5 py-3 text-slate-700 dark:text-slate-200">{user.displayName ?? '-'}</td>
                    <td className="px-5 py-3">
                      {user.role === 'ADMIN' ? (
                        <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${ROLE_BADGE.ADMIN}`}>管理员</span>
                      ) : (
                        <select
                          value={user.role}
                          onChange={e => handleRoleChange(user.id, e.target.value)}
                          disabled={savingId === user.id}
                          className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-700 text-sm text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-primary-500/50 disabled:opacity-50"
                        >
                          {ROLE_OPTIONS.map(opt => (
                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                          ))}
                        </select>
                      )}
                    </td>
                    <td className="px-5 py-3">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${user.enabled ? 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400' : 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400'}`}>
                        {user.enabled ? '启用' : '停用'}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-slate-500 dark:text-slate-400">{formatDateTime(user.createdAt)}</td>
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