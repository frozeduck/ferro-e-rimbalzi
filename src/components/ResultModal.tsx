import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { Trophy, AlertTriangle, Skull, ArrowRight, RotateCcw } from 'lucide-react';

interface ResultModalProps {
  game: GameEngine;
  onReturnToHub: () => void;
  onRestart: () => void;
}

export const ResultModal: React.FC<ResultModalProps> = ({ game, onReturnToHub, onRestart }) => {
  const won = game.phase === 'won';
  const shortIron = game.phase === 'short';

  const title = won ? 'ROCCA SALVATA!' : shortIron ? 'FERRO INSUFFICIENTE' : 'LA ROCCA È CADUTA';
  const desc = won
    ? `Hai difeso le miniere e consegnato ${game.delivered}/${game.target} risorse al forziere del Regno!`
    : shortIron
    ? `Sopravvissuto alle 5 ondate, ma servivano ${game.target} risorse (consegnate: ${game.delivered}).`
    : 'I goblin e i mostri hanno sfondato la palizzata nanica.';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/90 backdrop-blur-md animate-fade-in select-none">
      <div className="relative w-full max-w-md bg-[#1B110B] border-2 border-[#D97706] rounded-xl p-5 shadow-2xl flex flex-col gap-4 text-amber-100">
        {/* Banner Titolo */}
        <div className="text-center flex flex-col items-center gap-1.5 border-b border-[#78350F] pb-3">
          <div className="p-3 rounded-full bg-[#2E1C12] border border-[#B45309]">
            {won ? (
              <Trophy size={36} className="text-yellow-400 animate-bounce" />
            ) : shortIron ? (
              <AlertTriangle size={36} className="text-amber-400" />
            ) : (
              <Skull size={36} className="text-red-500" />
            )}
          </div>
          <h2
            className={`text-2xl font-black font-dwarven tracking-wide ${
              won ? 'text-emerald-400' : shortIron ? 'text-amber-400' : 'text-red-500'
            }`}
          >
            {title}
          </h2>
          <p className="text-xs text-stone-300 max-w-xs">{desc}</p>
        </div>

        {/* Resoconto della Battaglia */}
        <div className="bg-[#2E1C12] border border-[#78350F] rounded-lg p-3.5 flex flex-col gap-2">
          <div className="text-xs font-black text-amber-400 font-dwarven border-b border-[#78350F] pb-1 uppercase tracking-wider text-center">
            Resoconto della Battaglia
          </div>
          <div className="flex flex-col gap-1.5 text-xs text-stone-200 mt-1">
            <div className="flex justify-between">
              <span className="text-stone-300">Minerale grezzo estratto:</span>
              <span className="font-arcade text-base text-amber-300">{game.stats.mined}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-stone-300">Risorse forgiate / affinate:</span>
              <span className="font-arcade text-base text-amber-300">{game.stats.smelted}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-stone-300">Goblin e mostri abbattuti:</span>
              <span className="font-arcade text-base text-red-400">{game.kills}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-stone-300">Mura e baliste edificate:</span>
              <span className="font-arcade text-base text-emerald-400">{game.stats.built}</span>
            </div>
          </div>
        </div>

        {/* Pulsanti Azione */}
        <div className="flex flex-col gap-2.5 mt-1">
          <button
            onClick={onReturnToHub}
            className="w-full py-3 rounded-lg bg-[#047857] border-2 border-[#6EE7B7] text-white font-bold text-sm hover:bg-[#059669] active:scale-[0.98] transition flex items-center justify-center gap-2 shadow-lg font-dwarven tracking-wide"
          >
            <span>TORNA ALL'AVAMPOSTO</span>
            <ArrowRight size={18} />
          </button>

          <button
            onClick={onRestart}
            className="w-full py-2.5 rounded-lg bg-[#B91C1C] border border-[#FCA5A5] text-amber-100 font-bold text-xs hover:bg-[#dc2626] active:scale-[0.98] transition flex items-center justify-center gap-2 font-dwarven tracking-wide"
          >
            <RotateCcw size={16} />
            <span>RIGIOCA SPEDIZIONE</span>
          </button>
        </div>
      </div>
    </div>
  );
};
