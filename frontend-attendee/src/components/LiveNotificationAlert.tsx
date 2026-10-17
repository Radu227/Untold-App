import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface PerformanceChange {
  field: string;
  oldValue: string;
  newValue: string;
}

interface PerformanceUpdate {
  id: number;
  artistName: string;
  stageName: string;
  status: string;
  changes?: PerformanceChange[];
}

interface Props {
  performance: PerformanceUpdate;
  onClose: () => void;
}

const STATUS_COLORS: Record<string, string> = {
  LIVE:      'text-emerald-400',
  DELAYED:   'text-amber-400',
  CANCELLED: 'text-red-400',
  COMPLETED: 'text-sky-400',
  SCHEDULED: 'text-primary',
};

const STATUS_ICONS: Record<string, string> = {
  LIVE:      '🔴',
  DELAYED:   '⚠️',
  CANCELLED: '❌',
  COMPLETED: '✅',
  SCHEDULED: '📅',
};

export const LiveNotificationAlert: React.FC<Props> = ({ performance, onClose }) => {
  const navigate = useNavigate();
  const [modalOpen, setModalOpen] = useState(false);

  const hasChanges = performance.changes && performance.changes.length > 0;
  const statusIcon = STATUS_ICONS[performance.status] ?? '📢';
  const statusColor = STATUS_COLORS[performance.status] ?? 'text-primary';

  const handleGoToSchedule = () => {
    setModalOpen(false);
    onClose();
    navigate('/my-schedule');
  };

  const handleToastClick = () => {
    if (hasChanges) {
      setModalOpen(true);
    }
  };

  return (
    <>
      {/* ── Toast banner (bottom-left) ─────────────────────────────────── */}
      <div className="fixed bottom-8 left-8 z-[100] w-full max-w-sm animate-fade-in">
        <div
          className={`glass-panel border-2 border-primary/50 bg-background/95 rounded-2xl p-5 shadow-[0_0_30px_rgba(112,0,255,0.4)] flex flex-col gap-4 pointer-events-auto relative overflow-hidden ${hasChanges ? 'cursor-pointer hover:border-primary/80 transition-colors' : ''}`}
          onClick={handleToastClick}
          role={hasChanges ? 'button' : undefined}
          aria-label={hasChanges ? 'Click to see change details' : undefined}
        >
          {/* Glow */}
          <div className="absolute -inset-1 bg-gradient-to-tr from-primary/30 to-tertiary/10 blur-xl pointer-events-none" />

          <div className="flex items-start gap-4 relative z-10">
            <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center shrink-0 border border-primary/50">
              <span className="material-symbols-outlined text-primary text-2xl animate-pulse">campaign</span>
            </div>

            <div className="flex-1 min-w-0">
              <span className="text-[10px] uppercase font-black tracking-widest text-primary bg-primary/10 px-2 py-0.5 rounded border border-primary/30">
                Schedule Update
              </span>
              <h4 className="text-white font-bold text-sm mt-1.5 uppercase leading-tight truncate">
                {statusIcon} {performance.artistName}
              </h4>
              <p className="text-xs text-white/70 mt-0.5">
                {performance.stageName} · Status:{' '}
                <strong className={statusColor}>{performance.status}</strong>
              </p>
              {hasChanges && (
                <p className="text-[10px] text-primary/80 mt-1 font-semibold flex items-center gap-1">
                  <span className="material-symbols-outlined text-[12px]">info</span>
                  {performance.changes!.length} modificare{performance.changes!.length !== 1 ? 'i' : ''} · apasă pentru detalii
                </p>
              )}
            </div>

            <button
              onClick={(e) => { e.stopPropagation(); onClose(); }}
              className="w-8 h-8 rounded-full hover:bg-white/10 flex items-center justify-center text-white/50 hover:text-white transition-all shrink-0"
            >
              <span className="material-symbols-outlined text-lg">close</span>
            </button>
          </div>

          <div className="flex items-center gap-3 relative z-10 pt-2 border-t border-white/10">
            <button
              onClick={(e) => { e.stopPropagation(); handleGoToSchedule(); }}
              className="flex-1 bg-primary hover:bg-primary/80 text-white font-bold text-xs uppercase tracking-widest py-2 rounded-lg transition-all shadow-[0_0_15px_rgba(112,0,255,0.3)]"
            >
              View My Schedule
            </button>
          </div>
        </div>
      </div>

      {/* ── Detail modal ───────────────────────────────────────────────── */}
      {modalOpen && hasChanges && (
        <div
          className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in"
          onClick={() => setModalOpen(false)}
        >
          <div
            className="relative w-full max-w-md bg-[#0d0d1a] border border-primary/40 rounded-2xl shadow-[0_0_60px_rgba(112,0,255,0.35)] overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="relative bg-gradient-to-r from-primary/20 to-tertiary/10 px-6 pt-6 pb-4 border-b border-white/10">
              <div className="absolute -inset-0 bg-gradient-to-tr from-primary/10 to-transparent blur-2xl pointer-events-none" />
              <div className="relative flex items-start gap-3">
                <div className="w-10 h-10 rounded-full bg-primary/20 border border-primary/40 flex items-center justify-center shrink-0">
                  <span className="material-symbols-outlined text-primary text-xl">edit_calendar</span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-[10px] text-primary font-black uppercase tracking-widest mb-0.5">Performance Updated</p>
                  <h2 className="text-white font-bold text-base leading-tight">
                    {statusIcon} {performance.artistName}
                  </h2>
                  <p className="text-white/50 text-xs mt-0.5">{performance.stageName}</p>
                </div>
                <button
                  onClick={() => setModalOpen(false)}
                  className="w-8 h-8 rounded-full hover:bg-white/10 flex items-center justify-center text-white/40 hover:text-white transition-all shrink-0"
                >
                  <span className="material-symbols-outlined text-lg">close</span>
                </button>
              </div>
            </div>

            {/* Change list */}
            <div className="px-6 py-5 flex flex-col gap-3">
              <p className="text-white/50 text-xs uppercase tracking-widest font-bold mb-1">
                Modificări detectate
              </p>
              {performance.changes!.map((change, idx) => (
                <div
                  key={idx}
                  className="rounded-xl border border-white/10 bg-white/[0.03] px-4 py-3 grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 items-start"
                >
                  {/* Field label */}
                  <span className="text-[10px] font-black uppercase tracking-widest text-primary/80 col-span-2 mb-0.5">
                    {change.field}
                  </span>

                  {/* Old value */}
                  <span className="text-[10px] text-white/40 font-semibold uppercase self-center">Old</span>
                  <span className="text-sm text-red-400/90 font-mono bg-red-500/10 rounded px-2 py-0.5 line-through decoration-red-400/50">
                    {change.oldValue}
                  </span>

                  {/* Arrow */}
                  <span className="material-symbols-outlined text-white/20 text-sm self-center justify-self-center">arrow_downward</span>
                  <span />

                  {/* New value */}
                  <span className="text-[10px] text-white/40 font-semibold uppercase self-center">New</span>
                  <span className="text-sm text-emerald-400 font-mono bg-emerald-500/10 rounded px-2 py-0.5 font-bold">
                    {change.newValue}
                  </span>
                </div>
              ))}
            </div>

            {/* Footer actions */}
            <div className="px-6 pb-6 flex gap-3">
              <button
                onClick={handleGoToSchedule}
                className="flex-1 flex items-center justify-center gap-2 bg-primary hover:bg-primary/80 text-white font-bold text-xs uppercase tracking-widest py-3 rounded-xl transition-all shadow-[0_0_20px_rgba(112,0,255,0.4)] hover:shadow-[0_0_30px_rgba(112,0,255,0.6)]"
              >
                <span className="material-symbols-outlined text-base">calendar_month</span>
                My Schedule
              </button>
              <button
                onClick={() => { setModalOpen(false); onClose(); }}
                className="px-5 py-3 rounded-xl border border-white/10 text-white/50 hover:text-white hover:border-white/30 font-bold text-xs uppercase tracking-widest transition-all"
              >
                Închide
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
