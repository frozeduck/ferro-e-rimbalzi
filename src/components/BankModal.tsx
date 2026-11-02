import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { Landmark, Trophy, Pickaxe, Axe, Sparkles, Skull, X } from 'lucide-react';

interface BankModalProps {
  game: GameEngine;
  onClose: () => void;
}

export const BankModal: React.FC<BankModalProps> = ({ game, onClose }) => {
  const biomesUnlocked = 1 + (game.forestUnlocked ? 1 : 0) + (game.crystalUnlocked ? 1 : 0);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-md bg-[#1B110B] border-2 border-[#D97706] rounded-xl p-5 shadow-2xl flex flex-col gap-4 text-amber-100 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex justify-between items-start border-b border-[#78350F] pb-3">
          <div>
            <h2 className="text-xl font-black text-amber-400 font-dwarven tracking-wide flex items-center gap-2">
              <Landmark className="text-amber-500" size={24} />
              TESORERIA & ANNALI REALI
            </h2>
            <p className="text-xs text-stone-300">
              Registri storici delle conquiste sotterranee della Rocca
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded text-stone-400 hover:text-white hover:bg-stone-800 transition"
          >
            <X size={20} />
          </button>
        </div>

        {/* 1. Caveau del Regno */}
        <div className="bg-[#1E1712] border-2 border-[#B45309] rounded-xl p-4 flex flex-col gap-2">
          <div className="text-sm font-black text-amber-400 font-dwarven border-b border-amber-900/50 pb-1.5 flex items-center gap-2">
            <span>CAVEAU DEL REGNO</span>
          </div>
          <div className="grid grid-cols-2 gap-2 text-xs text-stone-200 mt-1">
            <div className="bg-[#291B14] p-2 rounded border border-[#78350F]">
              <span className="text-amber-400 font-bold">Ferro nel Tesoro:</span>
              <div className="text-lg font-arcade font-bold text-amber-300">{game.clanIron} Fe</div>
            </div>
            <div className="bg-[#291B14] p-2 rounded border border-[#78350F]">
              <span className="text-emerald-400 font-bold">Legname Pregiato:</span>
              <div className="text-lg font-arcade font-bold text-emerald-300">{game.clanWood} assi</div>
            </div>
            <div className="bg-[#291B14] p-2 rounded border border-[#78350F]">
              <span className="text-cyan-400 font-bold">Gemme Sfaccettate:</span>
              <div className="text-lg font-arcade font-bold text-cyan-300">{game.clanGems} gemme</div>
            </div>
            <div className="bg-[#291B14] p-2 rounded border border-[#78350F]">
              <span className="text-amber-300 font-bold">Biomi Esplorati:</span>
              <div className="text-lg font-arcade font-bold text-amber-200">{biomesUnlocked} / 3 biomi</div>
            </div>
          </div>
        </div>

        {/* 2. Annali delle Battaglie */}
        <div className="bg-[#19181E] border-2 border-[#6366F1] rounded-xl p-4 flex flex-col gap-2.5">
          <div className="text-sm font-black text-indigo-300 font-dwarven border-b border-indigo-900/50 pb-1.5 flex items-center gap-2">
            <Trophy size={16} />
            <span>ANNALI DELLE BATTAGLIE</span>
          </div>

          <div className="flex flex-col gap-2 text-xs text-stone-300">
            <div className="flex items-center justify-between border-b border-stone-800 pb-1">
              <span className="flex items-center gap-1.5">
                <Trophy size={14} className="text-yellow-400" /> Spedizioni concluse con vittoria:
              </span>
              <span className="font-arcade text-base text-yellow-300">{game.clanRunsWon}</span>
            </div>

            <div className="flex items-center justify-between border-b border-stone-800 pb-1">
              <span className="flex items-center gap-1.5">
                <Pickaxe size={14} className="text-amber-400" /> Minerale totale estratto con la sfera:
              </span>
              <span className="font-arcade text-base text-amber-300">{game.clanTotalMined}</span>
            </div>

            <div className="flex items-center justify-between border-b border-stone-800 pb-1">
              <span className="flex items-center gap-1.5">
                <Axe size={14} className="text-emerald-400" /> Legname ricavato dalla foresta:
              </span>
              <span className="font-arcade text-base text-emerald-300">{game.clanTotalWood}</span>
            </div>

            <div className="flex items-center justify-between border-b border-stone-800 pb-1">
              <span className="flex items-center gap-1.5">
                <Sparkles size={14} className="text-cyan-400" /> Gemme scavate nelle caverne:
              </span>
              <span className="font-arcade text-base text-cyan-300">{game.clanTotalGems}</span>
            </div>

            <div className="flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <Skull size={14} className="text-red-400" /> Mostri e invasori annientati:
              </span>
              <span className="font-arcade text-base text-red-300">{game.clanTotalKills}</span>
            </div>
          </div>
        </div>

        {/* Chiudi */}
        <button
          onClick={onClose}
          className="w-full py-2.5 rounded-lg bg-[#292524] border border-[#D97706] text-amber-200 font-bold hover:bg-[#3E3835] transition"
        >
          TORNA ALLA PIAZZA
        </button>
      </div>
    </div>
  );
};
