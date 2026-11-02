import React from 'react';
import { X, Play } from 'lucide-react';

interface TutorialModalProps {
  onClose: () => void;
}

export const TutorialModal: React.FC<TutorialModalProps> = ({ onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fade-in select-none">
      <div className="relative w-full max-w-md bg-[#26180F] border-2 border-[#D97706] rounded-xl p-5 shadow-2xl flex flex-col gap-3.5 text-amber-100 max-h-[92vh] overflow-y-auto">
        <div className="flex justify-between items-start border-b border-[#78350F] pb-2.5">
          <div>
            <h2 className="text-xl font-black text-amber-400 font-dwarven tracking-wide">
              GUIDA ALLA ROCCA DEI NANI
            </h2>
            <p className="text-xs text-stone-300">
              Come riconoscere le strutture e difendere la miniera
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded text-stone-400 hover:text-white hover:bg-stone-800 transition"
          >
            <X size={20} />
          </button>
        </div>

        {/* 4 Illustrated Steps */}
        <div className="flex flex-col gap-2.5">
          {/* 1. Blocchi Arkanoid */}
          <div className="bg-[#1B110B] border border-[#573E30] rounded-lg p-3 flex items-center gap-3">
            <img src="/assets/sprite_miniera.png" alt="Miniera" className="w-14 h-11 object-contain pixel-art rounded" />
            <div className="flex-1 text-xs">
              <div className="font-bold text-amber-400 font-dwarven">1. FILONI E ALBERI ARKANOID</div>
              <p className="text-stone-300 mt-0.5 leading-snug">
                Colpisci i blocchi in alto con la sfera per rimbalzare ed estrarre minerale o legname grezzo nella stiva.
              </p>
            </div>
          </div>

          {/* 2. Forgia dei Fabbri */}
          <div className="bg-[#1B110B] border border-[#573E30] rounded-lg p-3 flex items-center gap-3">
            <img src="/assets/sprite_forgia_aperta.png" alt="Forgia" className="w-14 h-11 object-contain pixel-art rounded" />
            <div className="flex-1 text-xs">
              <div className="font-bold text-amber-400 font-dwarven">2. FORGIA DEI FABBRI</div>
              <p className="text-stone-300 mt-0.5 leading-snug">
                Fai attraversare la sfera quando la fornace è APERTA per fondere il minerale in ferro. Chiusa fa da sponda.
              </p>
            </div>
          </div>

          {/* 3. Forziere Reale */}
          <div className="bg-[#1B110B] border border-[#573E30] rounded-lg p-3 flex items-center gap-3">
            <img src="/assets/sprite_forziere.png" alt="Forziere" className="w-14 h-11 object-contain pixel-art rounded" />
            <div className="flex-1 text-xs">
              <div className="font-bold text-amber-400 font-dwarven">3. FORZIERE REALE & ORO</div>
              <p className="text-stone-300 mt-0.5 leading-snug">
                Deposita il carico per vincere la missione (14 Fe). I mostri sconfitti rilasciano ORO per comprare difese!
              </p>
            </div>
          </div>

          {/* 4. Integrità della Rocca */}
          <div className="bg-[#1B110B] border border-[#573E30] rounded-lg p-3 flex items-center gap-3">
            <img src="/assets/sprite_scudo.png" alt="Scudo" className="w-14 h-11 object-contain pixel-art rounded" />
            <div className="flex-1 text-xs">
              <div className="font-bold text-amber-400 font-dwarven">4. INTEGRITÀ DELLA ROCCA</div>
              <p className="text-stone-300 mt-0.5 leading-snug">
                Perdi 1 Cuore se un nemico tocca la trincea rossa o se la sfera cade oltre la tua paletta difensiva!
              </p>
            </div>
          </div>
        </div>

        {/* Pulsante Ho capito */}
        <button
          onClick={onClose}
          className="w-full py-3 rounded-lg bg-[#B91C1C] border-2 border-[#FCA5A5] text-white font-bold text-sm hover:bg-[#dc2626] active:scale-[0.98] transition flex items-center justify-center gap-2 font-dwarven tracking-wide shadow-md mt-1"
        >
          <Play size={16} />
          <span>HO CAPITO ➔ ALLE ARMI!</span>
        </button>
      </div>
    </div>
  );
};
