import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { AudioHelper } from '../audio/AudioSynthesizer';
import { Wine, Heart, Zap, Coins, X } from 'lucide-react';

interface TavernModalProps {
  game: GameEngine;
  onClose: () => void;
  onUpdate: () => void;
}

export const TavernModal: React.FC<TavernModalProps> = ({ game, onClose, onUpdate }) => {
  const handleBuff = (type: 'heart' | 'speed' | 'iron_start') => {
    if (game.buyTavernBuff(type)) {
      AudioHelper.playButtonClick();
      onUpdate();
    }
  };

  const currentBuffLabel =
    game.clanBuff === 'heart'
      ? '+1 Cuore Extra'
      : game.clanBuff === 'speed'
      ? '+25px Scudo & Reattività'
      : game.clanBuff === 'iron_start'
      ? '+4 Oro di Partenza'
      : 'Nessuna';

  const canHeart = game.clanIron >= 3;
  const canSpeed = game.clanWood >= 3;
  const canCharge = game.clanIron >= 4;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-md bg-[#1B110B] border-2 border-[#D97706] rounded-xl p-5 shadow-2xl flex flex-col gap-4 text-amber-100">
        {/* Header */}
        <div className="flex justify-between items-start border-b border-[#78350F] pb-3">
          <div>
            <h2 className="text-xl font-black text-amber-400 font-dwarven tracking-wide flex items-center gap-2">
              <Wine className="text-amber-500" size={24} />
              TAVERNA DELLA BOTTE BOLLENTE
            </h2>
            <p className="text-xs text-stone-300">
              Offri una pinta per ricevere un buff per la prossima spedizione
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded text-stone-400 hover:text-white hover:bg-stone-800 transition"
          >
            <X size={20} />
          </button>
        </div>

        {/* Current buff status */}
        <div className="bg-[#26180F] border border-[#B45309] rounded-lg py-2 px-3 text-center text-xs font-bold">
          Benedizione attuale:{' '}
          <span className={game.clanBuff ? 'text-emerald-400 font-bold' : 'text-stone-400'}>
            {currentBuffLabel}
          </span>
        </div>

        {/* 3 Brews */}
        <div className="flex flex-col gap-3">
          {/* 1. Idromele della Roccia */}
          <button
            onClick={() => handleBuff('heart')}
            disabled={!canHeart}
            className={`w-full text-left p-3 rounded-lg border flex items-center gap-3 transition active:scale-[0.98] ${
              canHeart
                ? 'bg-[#2A150D] border-[#F59E0B] hover:bg-[#3D1E13] cursor-pointer'
                : 'bg-[#1C1917] border-[#52525B] opacity-60 cursor-not-allowed'
            }`}
          >
            <div className="p-2.5 bg-amber-950/80 rounded-md border border-amber-600/40 text-red-400">
              <Heart size={24} />
            </div>
            <div className="flex-1">
              <div className="text-sm font-bold text-amber-300 font-dwarven">
                IDROMELE DELLA ROCCIA (3 Fe)
              </div>
              <div className="text-xs text-stone-300">
                +1 Cuore vitale extra per l'intera durata della spedizione.
              </div>
              <div className={`text-xs font-bold mt-1 ${canHeart ? 'text-emerald-400' : 'text-red-400'}`}>
                {canHeart ? 'Tocca per bere l’idromele' : 'Richiede 3 Ferro'}
              </div>
            </div>
          </button>

          {/* 2. Birra di Fungo Silvano */}
          <button
            onClick={() => handleBuff('speed')}
            disabled={!canSpeed}
            className={`w-full text-left p-3 rounded-lg border flex items-center gap-3 transition active:scale-[0.98] ${
              canSpeed
                ? 'bg-[#14291D] border-[#10B981] hover:bg-[#1C3A29] cursor-pointer'
                : 'bg-[#1C1917] border-[#52525B] opacity-60 cursor-not-allowed'
            }`}
          >
            <div className="p-2.5 bg-emerald-950/80 rounded-md border border-emerald-600/40 text-emerald-400">
              <Zap size={24} />
            </div>
            <div className="flex-1">
              <div className="text-sm font-bold text-emerald-300 font-dwarven">
                BIRRA DI FUNGO SILVANO (3 Legno)
              </div>
              <div className="text-xs text-stone-300">
                +25px larghezza dello scudo per parate più agevoli.
              </div>
              <div className={`text-xs font-bold mt-1 ${canSpeed ? 'text-emerald-400' : 'text-red-400'}`}>
                {canSpeed ? 'Tocca per bere la birra' : 'Richiede 3 Legname'}
              </div>
            </div>
          </button>

          {/* 3. Distillato del Drago */}
          <button
            onClick={() => handleBuff('iron_start')}
            disabled={!canCharge}
            className={`w-full text-left p-3 rounded-lg border flex items-center gap-3 transition active:scale-[0.98] ${
              canCharge
                ? 'bg-[#2A150D] border-[#F59E0B] hover:bg-[#3D1E13] cursor-pointer'
                : 'bg-[#1C1917] border-[#52525B] opacity-60 cursor-not-allowed'
            }`}
          >
            <div className="p-2.5 bg-amber-950/80 rounded-md border border-amber-600/40 text-yellow-400">
              <Coins size={24} />
            </div>
            <div className="flex-1">
              <div className="text-sm font-bold text-amber-300 font-dwarven">
                DISTILLATO DEL DRAGO (4 Fe)
              </div>
              <div className="text-xs text-stone-300">
                +4 Oro spendibile già presente all'inizio della spedizione.
              </div>
              <div className={`text-xs font-bold mt-1 ${canCharge ? 'text-emerald-400' : 'text-red-400'}`}>
                {canCharge ? 'Tocca per bere il distillato' : 'Richiede 4 Ferro'}
              </div>
            </div>
          </button>
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
