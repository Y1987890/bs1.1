import { useEffect, useState } from 'react';
import { Info, X } from 'lucide-react';
import { interviewApi } from '../api/interview';

/**
 * 本地演示模式横幅
 * 当后端启用演示模式（app.demo-mode.enabled=true）时展示，
 * 明确标注出题与评估结果来自本地确定性演示逻辑，而非真实模型推理。
 */
export default function DemoModeBanner() {
  const [label, setLabel] = useState('');
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    let cancelled = false;
    interviewApi.getDemoMode().then((status) => {
      if (!cancelled && status.enabled) {
        setLabel(status.label);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!label || !visible) {
    return null;
  }

  return (
    <div className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 px-4 py-2.5 text-amber-800 dark:border-amber-700/50 dark:bg-amber-900/20 dark:text-amber-200">
      <Info className="mt-0.5 h-4 w-4 shrink-0" />
      <p className="flex-1 text-sm leading-relaxed">
        {label || '本地演示模式：出题与评估结果来自确定性演示逻辑，非真实模型推理。'}
      </p>
      <button
        type="button"
        onClick={() => setVisible(false)}
        className="shrink-0 rounded p-0.5 text-amber-600 hover:bg-amber-100 hover:text-amber-800 dark:text-amber-300 dark:hover:bg-amber-900/40"
        aria-label="关闭演示模式提示"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}