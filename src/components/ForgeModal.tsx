import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { AudioHelper } from '../audio/AudioSynthesizer';
import { Shield, Package, Flame, X } from 'lucide-react';

interface ForgeModalProps {
  game: GameEngine;
  onClose: () => void;
  onUpdate: () => void;
}

export const ForgeModal: React.FC<ForgeModalProps> = ({ game, onClose, onUpdate }) => {
  const handleUpgrade = (type: 'hp' | 'capacity' | 'damage') => {
    if (game.buyClanUpgrade(type)) {
      AudioHelper.playAnvil();
      onUpdate();
    }
  };

  const costHp = 10 + game.clanUpgradeHp * 8;
  const maxHpReached = game.clanUpgradeHp >= 3;
  const canHp = !maxHpReached && game.clanIron >= costHp;

  const costCapFe = 8 + game.clanUpgradeCapacity * 6;
  const costCapWood = 6 + game.clanUpgradeCapacity * 6;
  const maxCapReached = game.clanUpgradeCapacity >= 3;
  const canCap = !maxCapReached && game.clanIron >= costCapFe && game.clanWood >= costCapWood;

  const costDmgFe = 15 + game.clanUpgradeDamage * 10;
  const costDmgWood = 10 + game.clanUpgradeDamage * 10;
  const maxDmgReached = game.clanUpgradeDamage >= 2;
  const canDmg = !maxDmgReached && game.clanIron >= costDmgFe && game.clanWood >= costDmgWood;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-md bg-[#1B110B] border-2 border-[#D97706] rounded-xl p-5 shadow-2xl flex flex-col gap-4 text-amber-100">
        {/* Header */}
        <div className="flex justify-between items-start border-b border-[#78350F] pb-3">
          <div>
            <h2 className="text-xl font-black text-amber-400 font-dwarven tracking-wide">
              FUCINA DEI GRANDI FABBRI
            </h2>
            <p className="text-xs text-stone-300">
              Miglioramenti permanenti per tutte le spedizioni future
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded text-stone-400 hover:text-white hover:bg-stone-800 transition"
          >
            <X size={20} />
          </button>
        </div>

        {/* Clan vault summary */}
        <div className="bg-[#26150F] border border-[#B45309] rounded-lg py-2 px-3 text-center text-xs font-bold text-amber-300">
          Scorte Clan: <span className="text-amber-400">{game.clanIron} Ferro</span> ·{' '}
          <span className="text-emerald-400">{game.clanWood} Legname</span>
        </div>

        {/* 3 Upgrades */}
        <div className="flex flex-col gap-3">
          {/* 1. Tempra Scudo */}
          <button
            onClick={() => handleUpgrade('hp')}
            disabled={!canHp}
            className={`w-full text-left p-3 rounded-lg border flex items-center gap-3 transition active:scale-[0.98] ${
              canHp
                ? 'bg-[#2A150D] border-[#F59E0B] hover:bg-[#3D1E13] cursor-pointer'
                : 'bg-[#1C1917] border-[#52525B] opacity-60 cursor-not-allowed'
            }`}
          >
            <div className="p-2.5 bg-amber-950/80 rounded-md border border-amber-600/40 text-amber-400">
              <Shield size={24} />
            </div>
            <div className="flex-1">
              <div className="text-sm font-bold text-amber-300 font-dwarven">
                TEMPRA DELLO SCUDO (Liv. {game.clanUpgradeHp}/3)
              </div>
              <div className="text-xs text-stone-300">
                +1 Cuore vitale iniziale per la Rocca.
              </div>
              <div
                className={`text-xs font-bold mt-1 ${
                  maxHpReached ? 'text-emerald-400' : canHp ? 'text-amber-400' : 'text-red-400'
                }`}
              >
                {maxHpReached ? 'LIVELLO MASSIMO' : `Costo: ${costHp} Fe (Tocca per forgiare)`}
              </div>
            </div>
          </button>

          {/* 2. Stiva Runicata */}
          <button
            onClick={() => handleUpgrade('capacity')}
            disabled={!canCap}
            className={`w-full text-left p-3 rounded-lg border flex items-center gap-3 transition active:scale-[0.98] ${
              canCap
                ? 'bg-[#14291D] border-[#10B981] hover:bg-[#1C3A29] cursor-pointer'
                : 'bg-[#1C1917] border-[#52525B] opacity-60 cursor-not-allowed'
            }`}
          >
            <div className="p-2.5 bg-emerald-950/80 rounded-md border border-emerald-600/40 text-emerald-400">
              <Package size={24} />
            </div>
            <div className="flex-1">
              <div className="text-sm font-bold text-emerald-300 font-dwarven">
                STIVA RUNICATA (Liv. {game.clanUpgradeCapacity}/3)
              </div>
              <div className="text-xs text-stone-300">
                +1 Capienza di carico della sfera runica.
              </div>
              <div
                className={`text-xs font-bold mt-1 ${
                  maxCapReached ? 'text-emerald-400' : canCap ? 'text-amber-400' : 'text-red-400'
                }`}
              >
                {maxCapReached ? 'LIVELLO MASSIMO' : `Costo: ${costCapFe} Fe + ${costCapWood} Legno`}
              </div>
            </div>
          </button>

          {/* 3. Incudine d'Acciaio */}
          <button
            onClick={() => handleUpgrade('damage')}
            disabled={!canDmg}
            className={`w-full text-left p-3 rounded-lg border flex items-center gap-3 transition active:scale-[0.98] ${
              canDmg
                ? 'bg-[#2B1214] border-[#EF4444] hover:bg-[#3D1A1C] cursor-pointer'
                : 'bg-[#1C1917] border-[#52525B] opacity-60 cursor-not-allowed'
            }`}
          >
            <div className="p-2.5 bg-red-950/80 rounded-md border border-red-600/40 text-red-400">
              <Flame size={24} />
            </div>
            <div className="flex-1">
              <div className="text-sm font-bold text-red-300 font-dwarven">
                INCUDINE D'ACCIAIO (Liv. {game.clanUpgradeDamage}/2)
              </div>
              <div className="text-xs text-stone-300">
                +1 Danno base inflitto dalla sfera ai mostri.
              </div>
              <div
                className={`text-xs font-bold mt-1 ${
                  maxDmgReached ? 'text-emerald-400' : canDmg ? 'text-amber-400' : 'text-red-400'
                }`}
              >
                {maxDmgReached ? 'LIVELLO MASSIMO' : `Costo: ${costDmgFe} Fe + ${costDmgWood} Legno`}
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
