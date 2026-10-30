import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { ShieldAlert } from 'lucide-react';
import type { Role } from '../api/auth';
import { useAuth } from '../context/AuthContext';

interface ProtectedRouteProps {
  roles?: Role[];
  children: ReactNode;
}

export default function ProtectedRoute({ roles, children }: ProtectedRouteProps) {
  const { user, loading } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-10 w-10 animate-spin rounded-full border-3 border-slate-200 border-t-primary-500" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (roles && roles.length > 0 && !roles.includes(user.role)) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="max-w-sm text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-100 dark:bg-amber-900/30">
            <ShieldAlert className="h-8 w-8 text-amber-600 dark:text-amber-400" />
          </div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-white">无权限访问</h2>
          <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
            当前账号没有访问该页面的权限，请联系管理员或切换账号。
          </p>
          <button
            onClick={() => navigate(-1)}
            className="mt-6 inline-flex items-center gap-2 rounded-xl bg-primary-500 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-600"
          >
            返回
          </button>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}