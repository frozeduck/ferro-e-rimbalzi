import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { AudioHelper } from '../audio/AudioSynthesizer';
import { Package, Shield, Flame } from 'lucide-react';

interface UpgradeModalProps {
  game: GameEngine;
  onSelectUpgrade: (type: 'cargo' | 'paddle' | 'damage') => void;
}

export const UpgradeModal: React.FC<UpgradeModalProps> = ({ game, onSelectUpgrade }) => {
  const handleChoose = (type: 'cargo' | 'paddle' | 'damage') => {
    AudioHelper.playAnvil();
    onSelectUpgrade(type);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-sm animate-fade-in select-none">
      <div className="relative w-full max-w-md bg-[#1B110B] border-2 border-[#D97706] rounded-xl p-5 shadow-2xl flex flex-col gap-4 text-amber-100">
        <div className="text-center border-b border-[#78350F] pb-3">
          <h2 className="text-2xl font-black text-amber-400 font-dwarven tracking-wide">
            ONDATA RESPINTA!
          </h2>
          <p className="text-xs text-stone-300 mt-1">
            I mastri fabbri nanici offrono un'artefatto per la spedizione:
          </p>
        </div>

        <div className="flex flex-col gap-3">
          {/* Card 1: Bisaccia */}
          <button
            onClick={() => handleChoose('cargo')}
            className="w-full text-left p-3.5 rounded-lg border-2 border-[#F59E0B] bg-[#2E1C12] hover:bg-[#3d2417] active:scale-[0.98] transition flex items-center gap-3.5 group cursor-pointer shadow-md"
          >
            <div className="p-3 bg-amber-950/80 rounded-lg border border-amber-500/40 text-amber-400 group-hover:scale-105 transition">
              <Package size={26} />
            </div>
            <div className="flex-1">
              <div className="text-sm font-black text-amber-300 font-dwarven">
                BISACCIA DA MINIERA RINFORZATA
              </div>
              <div className="text-xs font-bold text-amber-200 mt-0.5">
                +2 Capienza Carico Sfera
              </div>
              <div className="text-[11px] text-stone-300 mt-0.5">
                Permette di trasportare e fondere più minerale grezzo.
              </div>
            </div>
          </button>

          {/* Card 2: Scudo Torre */}
          <button
            onClick={() => handleChoose('paddle')}
            className="w-full text-left p-3.5 rounded-lg border-2 border-[#10B981] bg-[#14291D] hover:bg-[#1c3a29] active:scale-[0.98] transition flex items-center gap-3.5 group cursor-pointer shadow-md"
          >
            <div className="p-3 bg-emerald-950/80 rounded-lg border border-emerald-500/40 text-emerald-400 group-hover:scale-105 transition">
              <Shield size={26} />
            </div>
            <div className="flex-1">
              <div className="text-sm font-black text-emerald-300 font-dwarven">
                SCUDO TORRE MAGGIORATO
              </div>
              <div className="text-xs font-bold text-emerald-200 mt-0.5">
                +20px Larghezza Scudo & +1 Cuore
              </div>
              <div className="text-[11px] text-stone-300 mt-0.5">
                Allarga la difesa della rocca e ripara i danni subiti.
              </div>
            </div>
          </button>

          {/* Card 3: Incanto Runico */}
          <button
            onClick={() => handleChoose('damage')}
            className="w-full text-left p-3.5 rounded-lg border-2 border-[#EF4444] bg-[#2B1214] hover:bg-[#3d1a1c] active:scale-[0.98] transition flex items-center gap-3.5 group cursor-pointer shadow-md"
          >
            <div className="p-3 bg-red-950/80 rounded-lg border border-red-500/40 text-red-400 group-hover:scale-105 transition">
              <Flame size={26} />
            </div>
            <div className="flex-1">
              <div className="text-sm font-black text-red-300 font-dwarven">
                INCANTO RUNICO PESANTE
              </div>
              <div className="text-xs font-bold text-red-200 mt-0.5">
                +1 Danno Base Sfera
              </div>
              <div className="text-[11px] text-stone-300 mt-0.5">
                La sfera runica distrugge più rapidamente i goblin invasori.
              </div>
            </div>
          </button>
        </div>
      </div>
    </div>
  );
};
