import React from 'react';
import { Play, RotateCcw, Home } from 'lucide-react';

interface PauseModalProps {
  onResume: () => void;
  onRestart: () => void;
  onReturnHub: () => void;
}

export const PauseModal: React.FC<PauseModalProps> = ({ onResume, onRestart, onReturnHub }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fade-in select-none">
      <div className="relative w-full max-w-sm bg-[#1B110B] border-2 border-[#D97706] rounded-xl p-5 shadow-2xl flex flex-col gap-4 text-amber-100 text-center">
        <h2 className="text-2xl font-black text-amber-400 font-dwarven tracking-wide border-b border-[#78350F] pb-2">
          BATTAGLIA SOSPESA
        </h2>

        <div className="flex flex-col gap-3 mt-1">
          <button
            onClick={onResume}
            className="w-full py-3 rounded-lg bg-[#047857] border-2 border-[#6EE7B7] text-white font-bold text-sm hover:bg-[#059669] active:scale-[0.98] transition flex items-center justify-center gap-2 font-dwarven tracking-wide shadow-md"
          >
            <Play size={18} />
            <span>RIPRENDI</span>
          </button>

          <button
            onClick={onRestart}
            className="w-full py-2.5 rounded-lg bg-[#B91C1C] border border-[#FCA5A5] text-amber-100 font-bold text-xs hover:bg-[#dc2626] active:scale-[0.98] transition flex items-center justify-center gap-2 font-dwarven tracking-wide"
          >
            <RotateCcw size={16} />
            <span>RICOMINCIA SPEDIZIONE</span>
          </button>

          <button
            onClick={onReturnHub}
            className="w-full py-2.5 rounded-lg bg-[#292524] border border-[#78350F] text-stone-300 font-bold text-xs hover:bg-[#3E3835] active:scale-[0.98] transition flex items-center justify-center gap-2 font-dwarven tracking-wide"
          >
            <Home size={16} />
            <span>ABBANDONA ED ESCI ALL'HUB</span>
          </button>
        </div>
      </div>
    </div>
  );
};
